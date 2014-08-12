import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scala.Some
import com.typesafe.sbt.SbtNativePackager._

// Defines the global build settings so they don't need to be edited everywhere
object Settings {
  val Version = "1.0"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := "org.tmt",
    organizationName := "TMT",
    organizationHomepage := Some(url("http://www.tmt.org")),
    version := Version,
    scalaVersion := Dependencies.scalaVersion,
    crossPaths := true,
    parallelExecution in Test := false,
    resolvers += Resolver.typesafeRepo("releases"),
    resolvers += "Akka Releases" at "http://repo.typesafe.com/typesafe/akka-releases",
//    resolvers += "Akka Snapshots" at "http://repo.typesafe.com/typesafe/akka-snapshots",
    resolvers += "Spray repo" at "http://repo.spray.io",
//    resolvers += "Spray nightlies" at "http://nightlies.spray.io",
    resolvers += Resolver.sonatypeRepo("releases"),
//    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "mDialog releases" at "http://mdialog.github.io/releases/"
    // local maven repo
//    resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
  )

  lazy val defaultSettings = buildSettings ++ Seq(
    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked")
//    javacOptions in Compile ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation")
  )

  // For standalone applications
  lazy val packageSettings = defaultSettings ++ packagerSettings ++ packageArchetype.java_application

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  import scalariform.formatter.preferences._
  def formattingPreferences: FormattingPreferences =
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
}
