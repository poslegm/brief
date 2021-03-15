import sbt._

object Dependencies {
  private val catsVersion    = "2.4.2"
  private val refinedVersion = "0.9.21"
  private val munitVersion   = "0.7.22"
  private val circeVersion   = "0.13.0-M5"

  lazy val munit           = "org.scalameta" %% "munit"                        % munitVersion % Test
  lazy val cats            = "org.typelevel" %% "cats-core"                    % catsVersion
  lazy val refined         = "eu.timepit"    %% "refined"                      % refinedVersion
  lazy val circeDerivation = "io.circe"      %% "circe-derivation-annotations" % circeVersion % Test
}
