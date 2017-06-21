import sbt._

//noinspection TypeAnnotation
object Dependencies {
  val Version = "0.7"
  val ScalaVersion = "2.12.2"
  val OpcVersion = "0.1.0"

  val pkg = "org.tmt" %% "pkg" % Version
  val ccs = "org.tmt" %% "ccs" % Version
  val log = "org.tmt" %% "log" % Version
  val containerCmd = "org.tmt" %% "containercmd" % Version

  val uaServer = "org.eclipse.milo" % "sdk-server" % OpcVersion
  val uaClient = "org.eclipse.milo" % "sdk-client" % OpcVersion
}

