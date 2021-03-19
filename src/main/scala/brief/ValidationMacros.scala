package brief

import cats.data.NonEmptyList
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

    private[this] def create(clsDef: ClassDef): Tree = {
      val name         = clsDef.name
      val fields       = extractCaseClassFields(clsDef)
      val refinedMetas = fields.flatMap(findRefinedMeta).map(m => m.fieldName -> m).toMap
      val arguments    = fields.map(fieldWithoriginalType(refinedMetas))
      val constructor  =
        q"""
          new $name(
            ..${fields.map(fieldToConstructorArgument)}
          )
        """
      val validation   = fields.flatMap(validateRefinedFields(refinedMetas, name))
      val body         =
        NonEmptyList
          .fromList(validation)
          .map(validatedConstructor(_, constructor))
          .getOrElse(defaultValidConstructor(constructor, name))
      q"""
        def create(
          ..$arguments
        ): _root_.cats.data.Validated[_root_.cats.data.NonEmptyChain[String], $name] = {
          ..$body
        }
      """
    }

    private[this] def extractCaseClassFields(clsDef: ClassDef): List[ValDef] =
      clsDef.impl.body.collect {
        case field: ValDef if field.mods.hasFlag(Flag.CASEACCESSOR | Flag.PARAMACCESSOR) => field
      }

    private[this] def fieldWithoriginalType(
        refinedMetas: Map[TermName, RefinedMeta]
    )(field: ValDef): Tree =
      refinedMetas
        .get(field.name)
        .map(m => q"${field.name}: ${m.original}")
        // for non-refined fields just use its type as is
        .getOrElse(q"${field.name}: ${field.tpt}")

    private[this] case class RefinedFieldValidator(validator: Tree, field: ValDef)
    private[this] def validateRefinedFields(
        refinedMetas: Map[TermName, RefinedMeta],
        className: TypeName
    )(field: ValDef): Option[RefinedFieldValidator] =
      refinedMetas.get(field.name).map { refinedMeta =>
        val validator =
          q"""
            (_root_.eu.timepit.refined.refineV[${refinedMeta.predicate}](${field.name}) match {
              case Left(err) => _root_.cats.data.Validated.invalidNec(
                "For field " + ${className.toString} + "." + ${field.name.toString} + ": " + err
              )
              case Right(res) => _root_.cats.data.Validated.valid(res)
            })
          """
        RefinedFieldValidator(validator, field)
      }

    /** x: Int Refined Positive Or Negative
      * ^  ^ original  ^ predicate
      * |- fieldName
      */
    private[this] case class RefinedMeta(
        fieldName: TermName,
        original: TypeName,
        predicate: Tree
    )

    /** returns None for non-refined fields
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
            original -> q"${name.toTermName}[..${predicates}]"

          case Ident(name: TypeName)                        => None -> q"${name.toTermName}"

          case _ =>
            abort(s"Unsupported predicate type ${tree}; it's a bug in the brief library")
          //case SingletonTypeTree(singleton)                 => None -> singleton
        }

      val (original, predicates) = go(field.tpt)
      original.map(o => RefinedMeta(field.name, o, predicates))
    }

    private[this] def validatedConstructor(
        validators: NonEmptyList[RefinedFieldValidator],
        ctor: Tree
    ): Tree =
      validators match {
        case NonEmptyList(head, Nil) =>
          q"""
              ${head.validator}.map { ${head.field} =>
                ..$ctor
              }
            """

        case list =>
          q"""
            import _root_.cats.syntax.apply._
            (
              ..${list.map(_.validator).toList}
            ).mapN { (..${list.map(_.field).toList}) =>
              ..$ctor
            }
          """
      }

    private[this] def defaultValidConstructor(constructor: Tree, className: TypeName): Tree =
      q"""
        _root_.cats.data.Validated.validNec[String, $className](..$constructor)
      """

    private[this] def fieldToConstructorArgument(field: ValDef): Tree =
      q"${field.name} = ${field.name}"

  }
}
