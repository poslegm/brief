package metaval.annotations

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

@scala.annotation.compileTimeOnly("enable macro paradise to expand macro annotations")
class Validation() extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ValidationMacros.apply
}

// TODO check is it case class - DONE
// TODO check if companion object exists already - DONE
// TODO check if there is another macro annotations - DONE
// TODO find refined fields, original and targiet types of its fields `eu.timepit.refined.refineV[RRR](value)`
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
              def ok: String = "boomer"
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
              def ok: String = "boomer"
            }
          """

        case _ =>
          abort(s"@Validation macro can only be applied to case classes")
      }

    private[this] def create(clsDef: ClassDef): Tree = {
      val name         = clsDef.name
      val fields       = extractCaseClassFields(clsDef)
      val arguments    = fields.map { x =>
        q"${x.name}: ${originalFieldType(x)}"
      }
      val applications = fields.map(fieldToConstructorArgument)
      q"""
        def create(
          ..$arguments
        ): $name =
          new $name(
            ..${applications}
          )
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

    private[this] def fieldToConstructorArgument(field: ValDef): Tree =
      field.tpt match {
        case AppliedTypeTree(
              Ident(TypeName("Refined")),
              Ident(_) :: Ident(predicate: TypeName) :: Nil
            ) =>
          q"_root_.eu.timepit.refined.refineV[${predicate}](${field.name}).fold(_root_.scala.sys.error, identity)"

        case Ident(_) /* non-refined field */ =>
          q"${field.name}"

        case _ =>
          abort(
            "Unsupported field type in MacroApply.fieldToConstructorArgument; it's a bug in @Validation macros"
          )
      }

  }
}
