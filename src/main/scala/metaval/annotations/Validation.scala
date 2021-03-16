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
// TODO find refined fields and original type of its fields
// TODO take NonEmptyChain[String] => E in constructor
// TODO generic fields
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
        case _                                             =>
          abort(s"@Validation macro can only be applied to case classes")
      }

    private[this] def create(clsDef: ClassDef): Tree = {
      val (name, fields) = extractCaseClassFields(clsDef)
      q"""
        def create(..$fields): $name = new $name(..${fields.map(_.name)})
      """
    }

    private[this] def extractCaseClassFields(clsDef: ClassDef): (TypeName, List[ValDef]) =
      clsDef match {
        case q"..$mods class $className(..$fields) extends ..$parents { ..$body }" =>
          className -> fields
      }
  }
}
