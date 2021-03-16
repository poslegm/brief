package metaval.annotations

import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import cats.data.NonEmptyList

@scala.annotation.compileTimeOnly("enable macro paradise to expand macro annotations")
class Validation() extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ValidationMacros.apply
}

// TODO check is it case class - DONE
// TODO check if companion object exists already - DONE
// TODO check if there is another macro annotations - DONE
// TODO find refined fields, original and targiet types of its fields `eu.timepit.refined.refineV[RRR](value)` - DONE
// TODO take NonEmptyChain[String] => E in constructor
// TODO generic fields
// TODO test usage in another package
// TODO multiple predicates: String Refined IPv4 Or IPv6
private[annotations] final class ValidationMacros(val c: whitebox.Context) {
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
      val name        = clsDef.name
      val fields      = extractCaseClassFields(clsDef)
      val arguments   = fields.map { x =>
        q"${x.name}: ${originalFieldType(x)}"
      }
      val constructor =
        q"""
          new $name(
            ..${fields.map(fieldToConstructorArgument)}
          )
        """
      val validation  = validateRefinedFields(fields)
      val body        =
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

    /** case class Example(a: String, b: Int Refined Positive)
      * will return String for a and Int for b
      */
    private[this] def originalFieldType(field: ValDef): TypeName =
      field.tpt match {
        case AppliedTypeTree(Ident(TypeName("Refined")), Ident(original: TypeName) :: _) => original
        case Ident(original: TypeName) /* non-refined field */                           => original

        case _ =>
          abort(
            "Unsupported field type in MacroApply.originalFieldType; it's a bug in @Validation macros"
          )
      }

    private[this] case class RefinedFieldValidator(validator: Tree, field: ValDef)
    private[this] def validateRefinedFields(fields: List[ValDef]): List[RefinedFieldValidator] =
      fields.flatMap { field =>
        field.tpt match {
          case AppliedTypeTree(
                Ident(TypeName("Refined")),
                Ident(_) :: Ident(predicate: TypeName) :: Nil
              ) =>
            val validator =
              q"""
                (_root_.eu.timepit.refined.refineV[${predicate}](${field.name}) match {
                  case Left(err) => _root_.cats.data.Validated.invalidNec(err)
                  case Right(res) => _root_.cats.data.Validated.valid(res)
                })
              """
            Some(RefinedFieldValidator(validator, field))

          case _ /* non-refined field */ => None
        }
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
