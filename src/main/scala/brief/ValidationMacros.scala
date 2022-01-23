package brief

import eu.timepit.refined.refineV
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import scala.reflect.macros.whitebox

private[brief] final class ValidationMacros(val c: whitebox.Context) {
  import c.universe._

  final def apply(annottees: c.Tree*): c.Tree = macroApply(annottees)()

  final def macroApply(annottees: Seq[c.Tree]): MacroApply = new MacroApply(annottees)

  private[this] def abort(msg: String): Nothing =
    c.abort(c.enclosingPosition, s"@Validation macro failure - $msg")

  private[this] def isCaseClass(clsDef: ClassDef): Boolean = clsDef.mods.hasFlag(Flag.CASE)

  final class MacroApply(annottees: Seq[c.Tree]) {
    final def apply(): c.Tree =
      annottees match {
        case List(clsDef: ClassDef) if isCaseClass(clsDef) =>
          // generate companion if there is no one
          q"""
            $clsDef
            object ${clsDef.name.toTermName} {
              ..${create(clsDef)}
            }
          """

        case List(
              clsDef: ClassDef,
              q"object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf => ..$objDefs }"
            ) if isCaseClass(clsDef) =>
          // reuse existing companion
          q"""
            $clsDef
            object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf =>
              ..$objDefs
              ..${create(clsDef)}
            }
          """

        case _ =>
          abort(s"@Validation macro can only be applied to case classes")
      }

    /** User arguments and type parameters should be parsed from annotation
      *
      * @returns
      *   `Some(E)` if annotation used as `@Validation[E] case class ...` `None` if annotation used
      *   as `@Validation case class ...`
      */
    private[this] def parseUserErrorType: Option[Tree] =
      c.prefix.tree match {
        case q"new $annotion[$exceptionType]" => Some(exceptionType)
        case _                                => None
      }

    /** Produces `create` method for class companion
      */
    private[this] def create(clsDef: ClassDef): Tree = {
      // all examples for `@Validation[CustomError] case class Test(a: Int, b: String Refined NonEmpty)`
      // name = Test
      val name          = clsDef.name
      // fields = List(a, b)
      val fields        = extractCaseClassFields(clsDef)
      // refinedMetas = Map(b -> RefinedMeta(b, String, NonEmpty))
      val refinedMetas  = fields.flatMap(findRefinedMeta).map(m => m.fieldName -> m).toMap
      // arguments = List("a: Int", "b: String")
      val arguments     = fields.map(fieldWithOriginalType(refinedMetas))
      // constructor = new Test(a = a, b = b)
      val constructor   =
        q"""
          new $name(
            ..${fields.map(fieldToConstructorArgument)}
          )
        """
      // validation = List(
      //   RefinedFieldValidator(
      //     validator = q"""liftErrors(refineV[NonEmpty](b)).left.map(e => "For field Test.b: " + e)""",
      //     field = b))
      val validation    = fields.flatMap(validateRefinedFields(refinedMetas, name))
      // body = liftErrors(refineV[NonEmpty](b)).left.map(e => "For field Test.b: " + e).map { b =>
      //   new Test(a = a, b = b)
      // }
      val body          =
        refineV[NonEmpty](validation).toOption
          .map(validatedConstructor(_, constructor))
          .getOrElse(defaultValidConstructor(constructor, name))
      // userErrorType = Some(CustomError)
      val userErrorType = parseUserErrorType
      // errorType = CustomError
      val errorType     = userErrorType.getOrElse(
        tq"""
          _root_.eu.timepit.refined.api.Refined[
            _root_.scala.List[_root_.scala.Predef.String],
            _root_.eu.timepit.refined.collection.NonEmpty
          ]
        """
      )
      // def create(b: String): Either[CustomError, Test] = {
      //   liftErrors(refineV[NonEmpty](b)).left.map(e => "For field Test.b: " + e).map { b =>
      //     new Test(a = a, b = b)
      //   }.left.map(errors => new CustomError(errors))
      // }
      q"""
        def create(
          ..$arguments
        ): _root_.scala.Either[$errorType, $name] = {
          ..${mapErrors(body, userErrorType)}
        }
      """
    }

    private[this] def extractCaseClassFields(clsDef: ClassDef): List[ValDef] =
      clsDef.impl.body.collect {
        case field: ValDef if field.mods.hasFlag(Flag.CASEACCESSOR | Flag.PARAMACCESSOR) => field
      }

    /** Generates arguments for `create` method. All fields should have its original type to be
      * validated inside the method.
      *
      * @param refinedMetas
      *   meta information about refined fields
      * @param field
      *   case class field to handle
      * @return
      *   tree with argument: type for received field
      */
    private[this] def fieldWithOriginalType(
        refinedMetas: Map[TermName, RefinedMeta]
    )(field: ValDef): Tree =
      refinedMetas
        .get(field.name)
        .map(m => q"${field.name}: ${m.original}")
        // for non-refined fields just use its type as is
        .getOrElse(q"${field.name}: ${field.tpt}")

    private[this] case class RefinedFieldValidator(validator: Tree, field: ValDef)

    /** Generates validation code with error handling for refined fields. Adds class/field name to
      * error string. Lifts error to non-empty list for accumulation.
      *
      * @param refinedMetas
      *   meta information about refined fields
      * @param className
      *   name of validated case class
      * @param field
      *   case class field to handle
      * @returns
      *   `None` for non-refined fields `Some[RefinedFieldValidator]` for refined fields
      */
    private[this] def validateRefinedFields(
        refinedMetas: Map[TermName, RefinedMeta],
        className: TypeName
    )(field: ValDef): Option[RefinedFieldValidator] =
      refinedMetas.get(field.name).map { refinedMeta =>
        val validator =
          q"""
            _root_.brief.util.either.liftErrors {
              _root_.eu.timepit.refined.refineV[${refinedMeta.predicate}](${field.name}).left.map { err =>
                "For field " + ${className.toString} + "." + ${field.name.toString} + ": " + err
              }
            }
          """
        RefinedFieldValidator(validator, field)
      }

    /** x: Int Refined Positive Or Negative ^ ^ original ^ predicate
      * \|- fieldName
      */
    private[this] case class RefinedMeta(
        fieldName: TermName,
        original: TypeName,
        predicate: Tree
    )

    /** Checks is field refined. For refined fields parses its type for original type and refined
      * predicate.
      *
      * @param field
      *   - case class field
      * @returns
      *   `None` for non-refined fields `Some[RefinedMeta]` for refined fields
      */
    private[this] def findRefinedMeta(field: ValDef): Option[RefinedMeta] = {

      def go(tree: Tree): (Option[TypeName], Tree) =
        tree match {
          case AppliedTypeTree(
                Ident(TypeName("Refined")),
                Ident(original: TypeName) :: predicate :: Nil
              ) =>
            Some(original) -> go(predicate)._2

          case AppliedTypeTree(Ident(name: TypeName), args) =>
            val successors = args.map(go)
            val original   = successors.collectFirst { case (Some(original), _) => original }
            val predicates = successors.map(_._2)
            original -> AppliedTypeTree(Ident(name), predicates)

          case other => None -> other
        }

      val (original, predicates) = go(field.tpt)
      original.map(o => RefinedMeta(field.name, o, predicates))
    }

    /** Maps validated fields to class constructor. Uses applicative semantics for `Either`
      * composition.
      *
      * @param validators
      *   list of validation code snippets for refined fields
      * @param ctor
      *   class constructor
      * @returns
      *   tree with mapping from validated fields to constructor
      */
    private[this] def validatedConstructor(
        validators: List[RefinedFieldValidator] Refined NonEmpty,
        ctor: Tree
    ): Tree =
      validators.value match {
        case head :: Nil =>
          // one refined field can be mapped as is
          q"""
            ${head.validator}.map { ${head.field} =>
              ..$ctor
            }
          """

        case list =>
          // two or more validators should be composed with `product` to accumulate errors
          val validatorsProduct = list.tail.foldLeft(q"${list.head.validator}") { (acc, v) =>
            q"_root_.brief.util.either.product($acc, ${v.validator})"
          }

          // pattern match nested tuples `case (((a, b), c), d) => `
          val tupledArgs = list.tail.tail.foldLeft(
            q"_root_.scala.Tuple2(${toPatMatArg(list.head.field)}, ${toPatMatArg(list.tail.head.field)})"
          )((acc, v) => q"_root_.scala.Tuple2($acc, ${toPatMatArg(v.field)})")

          q"""
            $validatorsProduct.map { case (..$tupledArgs) =>
              ..$ctor
            }
          """
      }

    /** Always valid constructor for classes without refined fields
      */
    private[this] def defaultValidConstructor(constructor: Tree, className: TypeName): Tree =
      q"""
        _root_.scala.Right(..$constructor)
      """

    /** Maps validation errors to custom exception if provided
      *
      * @param body
      *   validation code returning `Either[NEL[String], ClassName]`
      * @param customException
      *   exception type. Should be a class with constructor receives `List[String]`
      * @return
      *   tree with body wrapped to error mapping
      */
    private[this] def mapErrors(body: Tree, customException: Option[Tree]): Tree =
      customException.fold(body)(e => q"$body.left.map(errors => new $e(errors))")

    /** Generates field propagation to constructor as named arguments.
      * i.e. `new Test(a = a, b = b)`
      * @param field
      *   case class field
      * @return
      *   tree with received field application
      */
    private[this] def fieldToConstructorArgument(field: ValDef): Tree =
      q"${field.name} = ${field.name}"

    private[this] def toPatMatArg(field: ValDef): Bind = Bind(field.name, Ident(termNames.WILDCARD))
  }
}
