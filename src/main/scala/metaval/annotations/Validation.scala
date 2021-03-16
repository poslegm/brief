package metaval.annotations

import metaval.ValidationMacros

import scala.language.experimental.macros

@scala.annotation.compileTimeOnly("enable macro paradise to expand macro annotations")
class Validation() extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ValidationMacros.apply
}
