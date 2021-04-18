import Dependencies._

inThisBuild(
  List(
    organization := "com.github.poslegm",
    homepage := Some(url("https://github.com/poslegm/brief/")),
    licenses := List("MIT" -> url("http://opensource.org/licenses/MIT")),
    developers := List(
      Developer(
        "poslegm",
        "Mikhail Chugunkov",
        "poslegm@gmail.com",
        url("https://github.com/poslegm")
      )
    )
  )
)

def scala212 = "2.12.13"
def scala213 = "2.13.5"
def scala3   = "3.0.0-RC2"

commands += Command.command("ci-test") { s =>
  val scalaVersion = sys.env.get("TEST") match {
    case Some("2.12") => scala212
    case Some("2.13") => scala213
    case _            => scala3
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
    libraryDependencies ++= Seq(refined, munit),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => Seq(macroParadise)
        case _             => Seq.empty
      }
    },
    testFrameworks += new TestFramework("munit.Framework"),
    scalaVersion := scala3,
    crossScalaVersions := List(scala213, scala212, scala3),
    scalacOptions ++= Seq(
      //"-Ymacro-debug-lite",
      "-deprecation",
      "-unchecked"
    ),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Seq("-Ymacro-annotations")
        case _             => Seq.empty
      }
    },
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          Seq(
            "-explain",
            "-indent",
            "-new-syntax",
            "-print-lines",
            "-Xcheck-macros",
            "-source:3.0-migration"
          )
        case _            => Seq("-Ywarn-dead-code", "-Ywarn-numeric-widen")
      }
    }
  )
