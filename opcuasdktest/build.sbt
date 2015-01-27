

val Version = "0.1-SNAPSHOT"

lazy val settings = Seq(
  organization := "org.tmt",
  version := Version,
  scalaVersion := "2.11.5",
  resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
)
lazy val packageSettings = settings ++ packagerSettings ++ packageArchetype.java_application

val log            = "org.tmt"                       %% "log"                     % Version

// opc-ua-stack, opc-ua-sdk
// See sbt/maven parent pom bug: https://github.com/sbt/sbt/issues/1616
val serverSdk       = "com.inductiveautomation.opcua" % "server-sdk"             % "0.2.0-SNAPSHOT"

val scalaTest      = "org.scalatest"                 %% "scalatest"             % "2.1.5" % "test"

lazy val opcuasdktest = (project in file("."))
  .settings(packageSettings: _*)
  .settings(
    libraryDependencies ++= Seq(log, serverSdk, scalaTest)
  )
