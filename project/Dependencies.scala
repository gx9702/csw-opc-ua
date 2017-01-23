import sbt._

//noinspection TypeAnnotation
object Dependencies {
  val Version = "0.3-SNAPSHOT"
  val ScalaVersion = "2.11.8"

  val pkg = "org.tmt" %% "pkg" % Version
  val ccs = "org.tmt" %% "ccs" % Version
  val log = "org.tmt" %% "log" % Version
  val containerCmd = "org.tmt" %% "containercmd" % Version
  val uaServer = "com.digitalpetri.opcua" % "ua-server" % "0.4.2"
  val uaClient = "com.digitalpetri.opcua" % "ua-client" % "1.0.2"
}

