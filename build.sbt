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

def scala212 = "2.12.13"
def scala213 = "2.13.5"

commands += Command.command("ci-test") { s =>
  val scalaVersion = sys.env.get("TEST") match {
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
    libraryDependencies ++= Seq(refined, munit, circeDerivation),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Seq.empty
        case _             => Seq(macroParadise)
      }
    },
    testFrameworks += new TestFramework("munit.Framework"),
    scalaVersion := scala213,
    crossScalaVersions := List(scala213, scala212),
    scalacOptions ++= Seq(
      //"-Ymacro-debug-lite",
      "-deprecation",
      "-unchecked",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"
    ),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Seq("-Ymacro-annotations")
        case _             => Seq.empty
      }
    }
  )
