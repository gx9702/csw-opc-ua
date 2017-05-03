package csw.opcUaDemo.opcUaHcd

import csw.services.loc.ConnectionType.AkkaType
import csw.services.loc._
import csw.services.pkg.Supervisor
import csw.services.pkg.Component.{HcdInfo, RegisterOnly}

import scala.concurrent.duration._

/**
 * Starts Hcd2 as a standalone application.
 * Args: HCD-name: one of (HCD-2A, HCD-2B)
 */
object OpcUaHcdApp extends App {
  if (args.length != 1) {
    println("Expected one argument: the HCD name")
    System.exit(1)
  }
  LocationService.initInterface()
  private val hcdName = args(0)
  private val prefix = "MAIN.CycleCounter"
  private val className = "csw.opcUaDemo.opcUaHcd.OpcUaHcd"
  private val componentId = ComponentId(hcdName, ComponentType.HCD)
  private val hcdInfo = HcdInfo(hcdName, prefix, className, RegisterOnly, Set(AkkaType), 1.second)
  Supervisor(hcdInfo)
}
