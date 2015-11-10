package csw.opcDemo.hcd2

import java.io.File

import com.typesafe.config.ConfigFactory
import csw.services.loc.{ ServiceType, ServiceId }
import csw.services.pkg.Component

/**
 * Starts Hcd2 as a standalone application.
 * Args: name ("HCD-2A" or "HCD-2B"),
 * optional config file (defines rate, prefix, other component-specific keys)
 */
object Hcd2App extends App {
  if (args.length != 1 && args.length != 2) {
    println("Expected one or two args: the HCD name and an optional config file with HCD settings")
    System.exit(1)
  }
  val name = args(0)
  val config = if (args.length == 1) ConfigFactory.empty() else ConfigFactory.parseFile(new File(args(1)))
  val prefix = if (config.hasPath("prefix")) config.getString("prefix")
  else {
    // Normally the HCD would know its prefix, but for the test we are reusing the same class for two HCDs
    if (name == "HCD-2A") "tcs.mobie.blue.filter" else "tcs.mobie.blue.disperser"
  }
  val props = Hcd2.props(name, config)
  val serviceId = ServiceId(name, ServiceType.HCD)
  val services = Nil
  Component.create(props, serviceId, prefix, services)
}
