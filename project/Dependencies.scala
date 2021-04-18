import sbt._

object Dependencies {
  private val refinedVersion = "0.9.23"
  private val munitVersion   = "0.7.23"

  lazy val refined = "eu.timepit"    %% "refined" % refinedVersion
  lazy val munit   = "org.scalameta" %% "munit"   % munitVersion % Test

  // for old Scala versions
  lazy val macroParadise = compilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
  )
}
