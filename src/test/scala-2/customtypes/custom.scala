package brief.customtypes

import eu.timepit.refined.api._
import eu.timepit.refined._

object custom {
  case class CustomPositive()
  implicit def customPositive: Validate.Plain[Int, CustomPositive] =
    Validate.fromPredicate(x => x > 0, x => s"($x is positive)", CustomPositive())
}
