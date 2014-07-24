import sbt._
import Keys._
import com.typesafe.sbt.packager.Keys._


// This is the top level build object used by sbt.
object Build extends Build {

  import Settings._
  import Dependencies._

  lazy val hardware = project
    .settings(packageSettings: _*)
    .settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(bcpkix, bcprov, commonsLogging, httpclient, httpcore, httpcoreNio, log4j) ++
      test(scalaTest)
    )

  lazy val container1 = project
    .settings(packageSettings: _*)
    .settings(bashScriptExtraDefines ++= Seq(s"addJava -Dcsw.extjs.root=" + file("../csw-extjs").absolutePath))
    .settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(akkaKernel, akkaRemote, pkg)
    )

  lazy val container2 = project
    .settings(packageSettings: _*)
    .settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(akkaKernel, akkaRemote, pkg)
    ).dependsOn(hardware)
}
