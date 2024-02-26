import sbt._

object Dependencies {
  private val refinedVersion = "0.9.29"
  private val munitVersion   = "0.7.29"
  private val circeVersion   = "0.13.0-M5"

  lazy val refined         = "eu.timepit"    %% "refined"                      % refinedVersion
  lazy val munit           = "org.scalameta" %% "munit"                        % munitVersion % Test
  lazy val circeDerivation = "io.circe"      %% "circe-derivation-annotations" % circeVersion % Test

  // for old Scala versions
  lazy val macroParadise = compilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
  )
}
