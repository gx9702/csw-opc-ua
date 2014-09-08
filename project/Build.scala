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
    .settings(bashScriptExtraDefines ++= Seq("addJava -Dapplication-name=hardware"))
    .settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(log, bcpkix, bcprov, commonsLogging, httpclient, httpcore, httpcoreNio, log4jOverSlf4j, jsoneventLayout) ++
      test(scalaTest)
    )

  lazy val testclient = project
    .settings(packageSettings: _*)
    .settings(bashScriptExtraDefines ++= Seq("addJava -Dapplication-name=testclient"))
    .dependsOn(hardware)

  lazy val container1 = project
    .settings(packageSettings: _*)
    .settings(bashScriptExtraDefines ++= Seq(s"addJava -Dapplication-name=container1 -Dcsw.extjs.root="
      + file("../csw-extjs").absolutePath))
    .settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(akkaKernel, akkaRemote, pkg, log)
    )

  lazy val container2 = project
    .settings(packageSettings: _*)
    .settings(bashScriptExtraDefines ++= Seq("addJava -Dapplication-name=container2"))
    .settings(libraryDependencies ++=
    provided(akkaActor) ++
      compile(akkaKernel, akkaRemote, pkg, log)
    ).dependsOn(hardware)
}
