package brief

import annotations.Validation
import cats.data.{Validated, NonEmptyChain}
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._
import eu.timepit.refined.boolean._

class ValidationMacroSpec extends munit.FunSuite {
  test("generate object") {
    @Validation
    case class Test(a: Int)
    assertEquals(Test.create(1), Validated.valid(Test(1)))
  }

  test("generate object for final case class") {
    @Validation
    final case class Test()
    assertEquals(Test.create(), Validated.valid(Test()))
  }

  test("generate object for class with companion") {
    @Validation
    case class Test()
    object Test {
      def wow = "such function"
    }
    assertEquals(Test.create(), Validated.valid(Test()))
  }

  test("generate object for class with aliased fields") {
    type Alias = String
    @Validation
    case class Test(a: Int, b: Alias)
    assertEquals(Test.create(1, "a"), Validated.valid(Test(1, "a")))
  }

  test("generate object for class with another macro annotation") {
    import io.circe.derivation.annotations.JsonCodec
    import io.circe.syntax._

    @Validation @JsonCodec
    case class Test(a: Int)
    assertEquals(Test.create(1), Validated.valid(Test(1)))
    assertEquals(Test(1).asJson.noSpaces, """{"a":1}""")
  }

  test("fail on applying annotation to traits") {
    assertNoDiff(
      compileErrors("@Validation trait Ok"),
      """|error: @Validation macro failure - @Validation macro can only be applied to case classes
         |@Validation trait Ok
         | ^
         |""".stripMargin
    )
  }

  test("fail on applying annotation to non-case classes") {
    assertNoDiff(
      compileErrors("@Validation class Ok"),
      """|error: @Validation macro failure - @Validation macro can only be applied to case classes
         |@Validation class Ok
         | ^
         |""".stripMargin
    )
  }

  test("fail on applying annotation to objects") {
    assertNoDiff(
      compileErrors("@Validation object Ok"),
      """|error: @Validation macro failure - @Validation macro can only be applied to case classes
         |@Validation object Ok
         | ^
         |""".stripMargin
    )
  }

  test("validate refined fields") {
    @Validation
    case class Test(a: Int, ref: Int Refined Positive)
    assertEquals(Test.create(1, 2), Validated.valid(Test(1, 2)))
    assertEquals(
      Test.create(1, -2),
      Validated.invalidNec[String, Test]("For field Test.ref: Predicate failed: (-2 > 0).")
    )
  }

  test("validate refined fields with complex type") {
    @Validation
    case class Test(a: Int, ref: Int Refined Interval.Closed[0, 10])
    assertEquals(Test.create(1, 2), Validated.valid(Test(1, 2)))
    assertEquals(
      Test.create(1, -2),
      Validated.invalidNec[String, Test](
        "For field Test.ref: Left predicate of (!(-2 < 0) && !(-2 > 10)) failed: Predicate (-2 < 0) did not fail."
      )
    )
  }

  test("validate multiple predicates") {
    val x: Refined[String, IPv6 Or IPv4 And Not[EndsWith["1"]]] =
      refineMV[IPv6 Or IPv4 And Not[EndsWith["1"]]]("0.0.0.0")
    @Validation
    case class Test(a: Int, ref: Refined[String, IPv6 Or IPv4 And Not[EndsWith["1"]]])
    assertEquals(Test.create(1, "0.0.0.0"), Validated.valid(Test(1, x)))
    assertEquals(
      Test.create(1, "0.0.0.1"),
      Validated.invalidNec[String, Test](
        "For field Test.ref: Right predicate of ((0.0.0.1 is a valid IPv6 || 0.0.0.1 is a valid IPv4) && !\"0.0.0.1\".endsWith(\"1\")) failed: Predicate \"0.0.0.1\".endsWith(\"1\") did not fail."
      )
    )
  }
}
