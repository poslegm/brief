package anotherpackage

import cats.data.Validated
import metaval.annotations.Validation

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._

class PackageRelatedValidationMacroSpec extends munit.FunSuite {
  test("generate object for case class from any package") {
    @Validation
    case class Test(a: Int, ref: Int Refined Positive)
    assertEquals(Test.create(1, 2), Validated.valid(Test(1, 2)))
    assertEquals(
      Test.create(1, -2),
      Validated.invalidNec[String, Test]("Predicate failed: (-2 > 0).")
    )
  }

}
