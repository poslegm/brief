package metaval

import annotations.Validation

class ValidationMacroSpec extends munit.FunSuite {
  test("generate object") {
    @Validation
    case class Test(a: Int)
    assertEquals(Test.ok, "boomer")
    assertEquals(Test.create(1), Test(1))
  }

  test("generate object for final case class") {
    @Validation
    final case class Test()
    assertEquals(Test.ok, "boomer")
    assertEquals(Test.create(), Test())
  }

  test("generate object for class with companion") {
    @Validation
    case class Test()
    object Test {
      def neok = "zoomer"
    }
    assertEquals(Test.ok, "boomer")
  }

  test("generate object for class with another macro annotation") {
    import io.circe.derivation.annotations.JsonCodec
    import io.circe.syntax._

    @Validation @JsonCodec
    case class Test(a: Int)
    assertEquals(Test.ok, "boomer")
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
    import eu.timepit.refined._
    import eu.timepit.refined.api.Refined
    import eu.timepit.refined.auto._
    import eu.timepit.refined.numeric._

    @Validation
    case class Test(a: Int, ref: Int Refined Positive)
    assertEquals(Test.ok, "boomer")
    assertEquals(Test.create(1, 2), Test(1, 2))
  }
}
