import sbt._

// Dependencies

object Dependencies {

  val scalaVersion = "2.11.2"
  val akkaVersion = "2.3.6"

  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val akkaActor      = "com.typesafe.akka"             %% "akka-actor"            % akkaVersion
  val akkaKernel     = "com.typesafe.akka"             %% "akka-kernel"           % akkaVersion
  val akkaRemote     = "com.typesafe.akka"             %% "akka-remote"           % akkaVersion

  val scalaTest      = "org.scalatest"                 %% "scalatest"             % "2.1.5"

  // Prosys OPC UI jars non-free and are in ../lib: These are the additional dependencies
  val bcpkix         = "org.bouncycastle"             % "bcpkix-jdk15on"          % "1.47"
  val bcprov         = "org.bouncycastle"             % "bcprov-jdk15on"          % "1.47"
  val commonsLogging = "commons-logging"              % "commons-logging"         % "1.1.1"
  val httpclient     = "org.apache.httpcomponents"    % "httpclient"              % "4.2.5"
  val httpcore       = "org.apache.httpcomponents"    % "httpcore"                % "4.2.4"
  val httpcoreNio    = "org.apache.httpcomponents"    % "httpcore-nio"            % "4.2.4"
//  val log4j          = "log4j"                        % "log4j"                   % "1.2.17"
  val log4jOverSlf4j = "org.slf4j"                  % "log4j-over-slf4j"        % "1.7.7"

  // log4j+logstash
  val jsoneventLayout = "net.logstash.log4j"          % "jsonevent-layout"        % "1.6"

  // csw packages (installed with sbt publish-local)
  val pkg            = "org.tmt"                       %% "pkg"                     % Settings.Version
  val log            = "org.tmt"                       %% "log"                     % Settings.Version

}

