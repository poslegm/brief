# brief

Validation things are useless while programmers ignore them. We need to reduce
boilerplate to adopt typesafe validations to mass usage.

_breif_ is an automated constructor generation for case classes with refined
fields. The purpose of this micro-library is reducing adoption cost of
[refined](https://github.com/fthomas/refined) types.

It solves problems:

1. I want typesafe fields validation for case classes, but I don't want to
   write boilerplate to its validation manually;
2. If something went wrong I don't want to "fail fast" on first invalid field.
   I want to get all validation errors immediatly;
3. I want to have failed field name in every error message.

## Usage

Public API of _brief_ consists of the only one macro annotation `@Validation`!
It creates constructor for case class with refined fields and accumulate all
validation errors to `List[String]`.

### Example

```scala
import brief.annotations.Validation
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._

@Validation case class Test(a: Int, b: Int Refined Positive, c: Int Refined Negative)
// MACRO GENERATED CODE START
object Test {
  def create(a: Int, b: Int, c: Int): Either[List[String] Refined NonEmpty, Test] =
    brief.util.either.product(
      brief.util.either.liftErrors(refineV[Positive](b)),
      brief.util.either.liftErrors(refineV[Negative](c))
    ).map { case (b, c) =>
      new Test(a = a, b = b, c = c)
    }
}
// MACRO GENERATED CODE END

Test.create(1, 2, -3) // Right(Test(1, 2, -3))
Test.create(1, -2, 3) // Left(List("For field Test.b: Predicate failed: (-2 > 0).", "For field Test.c: Predicate failed: (3 < 0)."))
```

### Custom predicate example

```scala
import brief.annotations.Validation
import eu.timepit.refined._
import eu.timepit.refined.api.{Refined, Validate}

val russianPhoneNumberRegex = "\\+7[0-9]{10}".r
final case class RussianPhoneNumber()
implicit def phoneNumber: Validate.Plain[String, RussianPhoneNumber] =
  Validate.fromPredicate(
    x => russianPhoneNumberRegex.matches(x),
    x => s"($x has format +7XXXXXXXXXX)",
    RussianPhoneNumber()
  )

@Validation case class Call(
  source: String Refined RussianPhoneNumber,
  target: String Refined RussianPhoneNumber
)

Call.create("+71234567890", "+71112223344") // Right(Call(...))
```

## Custom errors

It's possible to return from `create` method your own exception instead of raw
`List[String]` with errors. Just define your error datatype with contract:

1. It should be a `class` or a `case class`
2. It should receive `List[String]` or `List[String] Refined NonEmpty` to
   constructor
   And pass it to annotation type parameter as `@Validation[CustomError]`.

### Example

```scala
final case class CallValidationError(msgs: List[String])
    extends Exception(s"call validation errors: ${msgs.mkString("; ")}")
    with NoStackTrace

@Validation[CallValidationError] case class Call(
  source: String Refined RussianPhoneNumber,
  target: String Refined RussianPhoneNumber
)

Call.create("+71234567890", "+71112") // Left(CallValidationError(...))
```

## Constraints

### Generic case classes

`case class` with type parameters will not compile. Feel free to
[fix](https://github.com/poslegm/brief/issues/8) it!

```scala
@Validation case class Test[T](a: T) // <- doesn't compile
```

### Type aliases

There are constraints about type aliases support. Feel free to
[fix](https://github.com/poslegm/brief/issues/14) it!

```scala
// predicate aliases are OK
type PaE = Positive And Even
@Validation case class Test(x: Int Refined PaE) // <- works good :)

// full type aliases aren't OK
type Pos = Int Refined Positive
@Validation case class Test(x: Pos) // <- will not work :(
```

## Supported Scala versions

Currently supported versions are 2.12 and 2.13. [Scala 3
support](https://github.com/poslegm/brief/issues/7) not ready yet.
