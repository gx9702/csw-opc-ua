

val Version = "0.1-SNAPSHOT"

lazy val settings = Seq(
  organization := "org.tmt",
  version := Version,
  scalaVersion := "2.11.5"
)
lazy val packageSettings = settings ++ packagerSettings ++ packageArchetype.java_application

val pkg = "org.tmt" %% "pkg" % Version
val hardwareopc = "org.tmt" %% "hardwareopc" % Version

lazy val hcd2opc = (project in file("."))
  .settings(packageSettings: _*)
  .settings(
    libraryDependencies ++= Seq(pkg, hardwareopc)
  )
