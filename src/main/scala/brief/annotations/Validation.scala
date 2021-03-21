package brief.annotations

import brief.ValidationMacros

import scala.language.experimental.macros

/** Optionally accepts custom error type with contract:
  *  1. Should be `class` or `case class`
  *  2. Constructor should be `List[String] => E` or `List[String] Refined NonEmpty => E`
  *
  *  Generates `create` method in companion object with return type:
  *  - if `E` specified: `Either[E, ClassName]`
  *  - if `E` not specified `Either[List[String] Refined NonEmpty, ClassName]`
  */
@scala.annotation.compileTimeOnly("enable macro paradise to expand macro annotations")
class Validation[E]() extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ValidationMacros.apply
}
