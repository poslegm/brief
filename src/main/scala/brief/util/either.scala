package brief.util

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV

trait either extends refined {
  def liftErrors[E, A](e: Either[E, A]): Either[NEL[E], A] = e.left.map(refineNonEmptyList(_))

  def product[E, A, B](a: Either[NEL[E], A], b: Either[NEL[E], B]): Either[NEL[E], (A, B)] =
    (a, b) match {
      case (Right(a), Right(b)) => Right(a -> b)
      case (Left(e1), Left(e2)) => Left(concatNonEmptyLists(e1, e2))
      case (Left(e1), Right(_)) => Left(e1)
      case (Right(_), Left(e2)) => Left(e2)
    }
}

object either extends either {
  object syntax {
    final implicit class RichEither[E, A](private val raw: Either[E, A])            extends AnyVal {
      def liftErrors: Either[NEL[E], A] = either.liftErrors(raw)
    }

    final implicit class RichErrorsEither[E, A](private val raw: Either[NEL[E], A]) extends AnyVal {
      def product[B](other: Either[NEL[E], B]): Either[NEL[E], (A, B)] = either.product(raw, other)
    }
  }
}
