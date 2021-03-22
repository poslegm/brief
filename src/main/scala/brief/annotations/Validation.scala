package brief.annotations

import brief.ValidationMacros

import scala.language.experimental.macros

/** Macro annotation generates smart constructor for case classes with refined fields.
  * Generated method accumulates all field validation errors to `List[String]`.
  *
  * Example:
  * {{{
  * @Validation case class Test(a: Int, b: String Refined NonEmpty, c: Int Refined Positive)
  *
  * // MACRO GENERATED CODE START
  * object Test {
  *   def create(a: Int, b: String, c: Int): Either[List[String] Refined NonEmpty, Test] =
  *     brief.util.either.product(
  *       brief.util.either.liftErrors(refineV[NonEmpty](b)),
  *       brief.util.either.liftErrors(refineV[Positive](c))
  *     ).map { case (b, c) =>
  *       new Test(a = a, b = b, c = c)
  *     }
  * }
  * // MACRO GENERATED CODE END
  *
  * Test.create(1, "", -1) // <- List("For field Test.b: Predicate failed...", "For field Test.c: Predicate failed...")
  * }}}
  *
  *  Optionally accepts custom error type with contract:
  *  1. Should be `class` or `case class`
  *  2. Constructor should accept `List[String]` or `List[String] Refined NonEmpty`
  *
  *  Generates `create` method in companion object with return type:
  *  - if `E` specified: `Either[E, ClassName]`
  *  - if `E` not specified `Either[List[String] Refined NonEmpty, ClassName]`
  *
  *  Example with custom error:
  *  {{{
  *  final case class TestValidationError(msgs: List[String])
  *      extends Exception(s"Test validation errors: ${msgs.mkString(", ")}")
  *      with NoStackTrace
  *
  *  @Validation[TestValidationError]
  *  case class Test(a: Int, b: String Refined NonEmpty, c: Int Refined Positive)
  *
  * Test.create(1, "", -1) // <- TestValidationError
  *  }}}
  */
@scala.annotation.compileTimeOnly("enable macro paradise to expand macro annotations")
class Validation[E]() extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ValidationMacros.apply
}
