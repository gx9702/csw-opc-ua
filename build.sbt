import sbt.Keys._
import sbt._

import Dependencies._
import Settings._

lazy val hcd2OpcServer = project
  .enablePlugins(JavaAppPackaging)
  .settings(packageSettings("hcd2OpcServer", "Demo OPC UA Server", "Demo OPC UA Server"): _*)
  .settings(mainClass in Compile := Some("csw.opc.server.Hcd2OpcServer"))
  .settings(libraryDependencies ++= Seq(uaServer, log))

lazy val hcd2OpcClient = project
  .enablePlugins(JavaAppPackaging)
  .settings(packageSettings("hcd2OpcClient", "HCD OPC UA demo", "HCD demo"): _*)
  .settings(libraryDependencies ++= Seq(pkg, uaClient))
  .dependsOn(hcd2OpcServer)

lazy val container2Opc = project
  .enablePlugins(JavaAppPackaging)
  .settings(packageSettings("container2Opc", "Container OPC UA demo", "Example container"): _*)
  .settings(libraryDependencies ++= Seq(containerCmd)) dependsOn hcd2OpcClient
