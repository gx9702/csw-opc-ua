val Version = "0.1-SNAPSHOT"

lazy val settings = Seq(
  organization := "org.tmt",
  version := Version,
  scalaVersion := "2.11.5"
)
lazy val packageSettings = settings ++ packagerSettings ++ packageArchetype.java_application

val containerCmd = "org.tmt" %% "containercmd" % Version
val hcd2opc = "org.tmt" %% "hcd2opc" % Version

lazy val container2opc = (project in file("."))
  .settings(packageSettings: _*)
  .settings(
    libraryDependencies ++= Seq(containerCmd, hcd2opc)
  )
