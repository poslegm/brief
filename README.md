# brief

Validation things are useless until someone uses them. Tools should work as simple as possible for mass usage.

**Proof of concept**

Automated constructor generation for case classes with refined fields.

```scala
import brief.annotations.Validation
import cats.data.Validated
import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._

// write a lot of boilerplate without macro

case class Test(a: Int, b: Int Refined Positive, c: Int Refined Negative)
object Test {
  def create(a: Int, b: Int, c: Int): ValidatedNec[String, Test] =
    (
      refineV[Positive](b).toValidatedNec,
      refineV[Negative](c).toValidatedNec
    ).mapN { (validB, validC) =>
      Test(a, validB, validC)
    }
}

// just one annotation with macro

@Validation
case class Test(a: Int, b: Int Refined Positive, c: Int Refined Negative)
Test.create(1, 2, -3) // Validated.Valid(Test(1, 2, -3))
Test.create(1, -2, 3) // Validated.Invalid(NonEmptyChain("Predicate failed: (-2 > 0).", "Predicate failed: (3 < 0)."))
```
