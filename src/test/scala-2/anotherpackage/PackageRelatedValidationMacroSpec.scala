package anotherpackage

import brief.annotations.Validation
import brief.util.either._

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._

package fields {
  package fields2 {
    case class NestedField(a: String)
  }
}

package errors {
  package custom {
    class CustomErr(errors: List[String]) extends Exception(s"errs is ${errors.mkString(",")}")
  }
}

class PackageRelatedValidationMacroSpec extends munit.FunSuite {
  test("generate object for case class from any package") {
    @Validation
    case class Test(a: Int, ref: Int Refined Positive)
    assertEquals(Test.create(1, 2), Right(Test(1, 2)))
    assertEquals(
      Test.create(1, -2),
      Left(refineNonEmptyList("For field Test.ref: Predicate failed: (-2 > 0)."))
    )
  }

  test("generate object for case class with fields from any package") {
    @Validation
    case class Test(a: fields.fields2.NestedField, ref: Int Refined Positive)
    assertEquals(
      Test.create(fields.fields2.NestedField("a"), 2),
      Right(Test(fields.fields2.NestedField("a"), 2))
    )
  }

  test("generate object for case class with fields imported from any package") {
    import fields.fields2._
    @Validation
    case class Test(a: NestedField, ref: Int Refined Positive)
    assertEquals(
      Test.create(fields.fields2.NestedField("a"), 2),
      Right(Test(fields.fields2.NestedField("a"), 2))
    )
  }

  test("use custom exception from any package") {
    import errors.custom.CustomErr

    @Validation[CustomErr]
    case class Test(ref: Int Refined Positive)
    assertEquals(
      Test.create(-1).left.map(_.getMessage),
      Left("errs is For field Test.ref: Predicate failed: (-1 > 0).")
    )
  }

}
