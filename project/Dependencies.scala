import sbt._

//noinspection TypeAnnotation
object Dependencies {
  val Version = "0.3-SNAPSHOT"
  val ScalaVersion = "2.11.8"
  val OpcVersion = "0.1.0"

  val pkg = "org.tmt" %% "pkg" % Version
  val ccs = "org.tmt" %% "ccs" % Version
  val log = "org.tmt" %% "log" % Version
  val containerCmd = "org.tmt" %% "containercmd" % Version

//  val uaServer = "com.digitalpetri.opcua" % "ua-server" % "0.4.2"
//  val uaClient = "com.digitalpetri.opcua" % "ua-client" % "1.0.2"

  val uaServer = "org.eclipse.milo" % "sdk-server" % OpcVersion
  val uaClient = "org.eclipse.milo" % "sdk-client" % OpcVersion
}

