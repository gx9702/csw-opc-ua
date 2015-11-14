import com.typesafe.sbt.SbtNativePackager.packageArchetype
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.packager.Keys._
import sbt.Keys._
import sbt._

val Version = "0.2-SNAPSHOT"
val ScalaVersion = "2.11.7"

def formattingPreferences: FormattingPreferences =
  FormattingPreferences()
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)

lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
  ScalariformKeys.preferences in Compile := formattingPreferences,
  ScalariformKeys.preferences in Test := formattingPreferences
)

val buildSettings = Seq(
  organization := "org.tmt",
  organizationName := "TMT",
  organizationHomepage := Some(url("http://www.tmt.org")),
  version := Version,
  scalaVersion := ScalaVersion,
  crossPaths := true,
  parallelExecution in Test := false,
  fork := true,
  resolvers += Resolver.typesafeRepo("releases"),
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += sbtResolver.value
)

lazy val defaultSettings = buildSettings ++ formatSettings ++ Seq(
  scalacOptions ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-feature", "-deprecation", "-unchecked"),
//  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation")
  javacOptions in Compile ++= Seq("-source", "1.8"),
  javacOptions in (Compile, compile) ++= Seq("-target", "1.8", "-Xlint:all", "-Xlint:-options", "-Werror")
)

// For standalone applications
def packageSettings(summary: String, desc: String) = defaultSettings ++
  packagerSettings ++ packageArchetype.java_application ++ Seq(
  version in Rpm := Version,
  rpmRelease := "0",
  rpmVendor := "TMT Common Software",
  rpmUrl := Some("http://www.tmt.org"),
  rpmLicense := Some("MIT"),
  rpmGroup := Some("CSW"),
  packageSummary := summary,
  packageDescription := desc,
  bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=$Version -Dakka.loglevel=DEBUG")
)

// dependencies
val pkg = "org.tmt" %% "pkg" % Version
val log = "org.tmt" %% "log" % Version
val containerCmd = "org.tmt" %% "containercmd" % Version
val uaServer = "com.digitalpetri.opcua" % "ua-server" % "0.4.2"
val uaClient = "com.digitalpetri.opcua" % "ua-client" % "1.0.2"

lazy val hcd2OpcServer = project
  .settings(packageSettings("Demo OPC UA Server", "Demo OPC UA Server"): _*)
  .settings(libraryDependencies ++= Seq(uaServer, log))

lazy val hcd2OpcClient = project
  .settings(packageSettings("HCD OPC UA demo", "HCD demo"): _*)
  .settings(libraryDependencies ++= Seq(pkg, uaClient))
  .dependsOn(hcd2OpcServer)

lazy val container2Opc = project
  .settings(packageSettings("Container OPC UA demo", "Example container"): _*)
  .settings(libraryDependencies ++= Seq(containerCmd)) dependsOn hcd2OpcClient
