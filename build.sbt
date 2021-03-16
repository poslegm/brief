import Dependencies._

inThisBuild(
  List(
    organization := "chugunkov.dev",
    homepage := Some(url("https://github.com/macro-validated/")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "poslegm",
        "Mikhail Chugunkov",
        "poslegm@gmail.com",
        url("https://chugunkov.dev")
      )
    )
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "macro-validated",
    libraryDependencies ++= Seq(cats, refined, munit, circeDerivation),
    testFrameworks += new TestFramework("munit.Framework"),
    scalaVersion := "2.13.5",
    scalacOptions ++= Seq("-Ymacro-annotations", "-Ymacro-debug-lite")
  )
