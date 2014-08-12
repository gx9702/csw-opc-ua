import sbt._
import Keys._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.archetypes.ServerLoader
import NativePackagerKeys._


// This is the top level build object used by sbt.
object Build extends Build {

  import Settings._
  import Dependencies._

  lazy val hardware = project
    .settings(packageSettings: _*)
    .settings(mappings in Universal <+= (packageBin in Compile, sourceDirectory) map { (_, src) =>
    val conf = src / "main" / "resources" / "log.properties"
    conf -> "bin/log.properties"
  }).settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(bcpkix, bcprov, commonsLogging, httpclient, httpcore, httpcoreNio, log4j) ++
      test(scalaTest)
    )

  lazy val testclient = project
    .settings(packageSettings: _*)
    .settings(mappings in Universal <+= (packageBin in Compile, sourceDirectory) map { (_, src) =>
    val conf = src / "main" / "resources" / "log.properties"
    conf -> "bin/log.properties"
  }).dependsOn(hardware)

  lazy val container1 = project
    .settings(packageSettings: _*)
    .settings(bashScriptExtraDefines ++= Seq(s"addJava -Dcsw.extjs.root=" + file("../csw-extjs").absolutePath))
    .settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(akkaKernel, akkaRemote, pkg)
    )

  lazy val container2 = project
    .settings(packageSettings: _*)
    .settings(mappings in Universal <+= (packageBin in Compile, sourceDirectory) map { (_, src) =>
    val conf = src / "main" / "resources" / "log.properties"
    conf -> "bin/log.properties"
  }).settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(akkaKernel, akkaRemote, pkg)
    ).dependsOn(hardware)
}
