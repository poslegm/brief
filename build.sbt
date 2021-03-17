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

def scala211 = "2.11.12"
def scala212 = "2.12.13"
def scala213 = "2.13.5"

commands += Command.command("ci-test") { s =>
  val scalaVersion = sys.env.get("TEST") match {
    case Some("2.11") => scala211
    case Some("2.12") => scala212
    case _            => scala213
  }
  s"++$scalaVersion" ::
    "test" ::
    "publishLocal" ::
    s
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "brief",
    libraryDependencies ++= Seq(cats, refined, munit, circeDerivation),
    testFrameworks += new TestFramework("munit.Framework"),
    scalaVersion := "2.13.5",
    scalacOptions ++= Seq(
      "-Ymacro-annotations",
      //"-Ymacro-debug-lite",
      "-deprecation",
      "-unchecked",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"
    )
  )
