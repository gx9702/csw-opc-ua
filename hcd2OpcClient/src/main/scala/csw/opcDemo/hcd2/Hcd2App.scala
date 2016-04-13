package csw.opcDemo.hcd2

import java.io.File

import csw.services.loc.ConnectionType.AkkaType
import csw.services.loc._
import csw.services.pkg.Supervisor
import csw.services.pkg.Component.{HcdInfo, RegisterOnly}
import scala.concurrent.duration._

/**
 * Starts Hcd2 as a standalone application.
 * Args: HCD-name: one of (HCD-2A, HCD-2B)
 */
object Hcd2App extends App {
  if (args.length != 1) {
    println("Expected one argument: the HCD name")
    System.exit(1)
  }
  LocationService.initInterface()
  val hcdName = args(0)
  val prefix = if (hcdName == "HCD-2A") "tcs.mobie.blue.filter" else "tcs.mobie.blue.disperser"
  val className = "csw.opcDemo.hcd2.Hcd2"
  val componentId = ComponentId(hcdName, ComponentType.HCD)
  val hcdInfo = HcdInfo(hcdName, prefix, className, RegisterOnly, Set(AkkaType), 1.second)
  val supervisor = Supervisor(hcdInfo)

}
