package csw.opc.hcd2

import csw.services.ls.LocationService.RegInfo
import csw.services.ls.LocationServiceActor.{ServiceId, ServiceType}
import csw.services.pkg.Component
import csw.services.pkg.LifecycleManager.Startup

/**
 * Starts Hcd2 as a standalone application.
 * Args: name, configPath
 */
object Hcd2App extends App {
  if (args.length != 2) {
    println("Expected two args: the HCD name and the config path")
    System.exit(1)
  }
  val name = args(0)
  val configPath = args(1)
  val props = Hcd2.props(name, configPath)
  val serviceId = ServiceId(name, ServiceType.HCD)
  val httpUri = None
  val regInfo = RegInfo(serviceId, Some(configPath), httpUri)
  val services = Nil
  val info = Component.create(props, regInfo, services)
  info.lifecycleManager ! Startup
}
