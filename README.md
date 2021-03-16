# brief

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

// without macro

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

// with macro

@Validation
case class Test(a: Int, b: Int Refined Positive, c: Int Refined Negative)
Test.create(1, 2, -3) // Validated.Valid(Test(1, 2, -3))
Test.create(1, -2, 3) // Validated.Invalid(NonEmptyChain("Predicate failed: (-2 > 0).", "Predicate failed: (3 < 0)."))
```

## Next steps

-   Get rid of `cats.data.Validated`
-   Add field name to error
-   Support case classes with generic fields
-   Receive `NonEmptyChain[String] => E` in annotation for custom exceptions
-   Cross build for Scala 2.11 and 2.12
-   Support multiple predicates for refined types
-   Support Scala 3
