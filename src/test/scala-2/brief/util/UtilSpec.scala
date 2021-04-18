package brief.util

import either._
import either.syntax._

class UtilSpec extends munit.FunSuite {
  test("concat non empty lists") {
    val nel1 = refineNonEmptyList(1, 2)
    val nel2 = refineNonEmptyList(3)
    assertEquals(concatNonEmptyLists(nel1, nel2).value, List(1, 2, 3))
  }

  test("product eithers with error accumulation") {
    val e1: Either[String, Int] = Right(42)
    val e2: Either[String, Int] = Right(43)
    val e3: Either[String, Int] = Left("error1")
    val e4: Either[String, Int] = Left("error2")

    val succ = e1.liftErrors.product(e2.liftErrors)
    assertEquals(succ, Right(42, 43))

    val fail = succ.product(e3.liftErrors).product(e4.liftErrors)
    assertEquals(fail.left.map(_.value), Left(List("error1", "error2")))
  }
}
