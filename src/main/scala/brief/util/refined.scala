package brief.util

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV

trait refined {
  type NEL[T] = List[T] Refined NonEmpty

  def refineNonEmptyList[T](head: T, tail: T*): NEL[T] =
    refineV[NonEmpty](head :: tail.toList).fold(
      e => throw new IllegalStateException(s"non empty list check failed [$e]; it's a bug"),
      lst => lst
    )

  def concatNonEmptyLists[T](xs: NEL[T], ys: NEL[T]): NEL[T] =
    refineNonEmptyList(xs.value.head, (xs.value.tail ::: ys.value): _*)
}

object refined extends refined
