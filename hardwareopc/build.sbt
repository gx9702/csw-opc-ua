

val Version = "0.1-SNAPSHOT"

lazy val settings = Seq(
  organization := "org.tmt",
  version := Version,
  scalaVersion := "2.11.5"
)
lazy val packageSettings = settings ++ packagerSettings ++ packageArchetype.java_application

val pkg = "org.tmt" %% "pkg" % Version

// Prosys OPC UI jars non-free and are in ../lib: These are the additional dependencies
val bcpkix         = "org.bouncycastle"             % "bcpkix-jdk15on"          % "1.47"
val bcprov         = "org.bouncycastle"             % "bcprov-jdk15on"          % "1.47"
val commonsLogging = "commons-logging"              % "commons-logging"         % "1.1.1"
val httpclient     = "org.apache.httpcomponents"    % "httpclient"              % "4.2.5"
val httpcore       = "org.apache.httpcomponents"    % "httpcore"                % "4.2.4"
val httpcoreNio    = "org.apache.httpcomponents"    % "httpcore-nio"            % "4.2.4"
val log4jOverSlf4j = "org.slf4j"                  % "log4j-over-slf4j"          % "1.7.7"

val scalaTest      = "org.scalatest"                 %% "scalatest"             % "2.1.5" % "test"

lazy val hardwareopc = (project in file("."))
  .settings(packageSettings: _*)
  .settings(
    libraryDependencies ++= Seq(pkg, bcpkix, bcprov, commonsLogging, httpclient, httpcore, httpcoreNio,
      log4jOverSlf4j, scalaTest)
  )
