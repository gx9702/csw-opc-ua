package csw.opcDemo.hcd2

import csw.services.ccs.HcdController
import csw.services.pkg.Component.HcdInfo
import csw.services.pkg.Supervisor._
import csw.services.pkg.{Hcd, LifecycleHandler}
import csw.util.config.Configurations.SetupConfig
import csw.util.config.StringKey

/**
 * Test HCD
 */
case class Hcd2(info: HcdInfo) extends Hcd with HcdController with LifecycleHandler {
  val worker = context.actorOf(Hcd2Worker.props(info.prefix))

  lifecycle(supervisor)

  def receive = controllerReceive orElse lifecycleHandlerReceive

  // Send the config to the worker for processing
  override protected def process(config: SetupConfig): Unit = {
    worker ! config
  }

  // Ask the worker actor to send us the current state (handled by parent trait)
  override protected def requestCurrent(): Unit = {
    worker ! Hcd2Worker.RequestCurrentState
  }
}

object Hcd2 {
  /**
   * The prefix for filter configs
   */
  val filterPrefix = "tcs.mobie.blue.filter"

  /**
   * The prefix for disperser configs
   */
  val disperserPrefix = "tcs.mobie.blue.disperser"

  /**
   * The key for filter values
   */
  val filterKey = StringKey("filter")

  /**
   * The key for disperser values
   */
  val disperserKey = StringKey("disperser")

  /**
   * The available filters
   */
  val FILTERS = Array[String]("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308")

  /**
   * The available dispersers
   */
  val DISPERSERS = Array[String]("Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306")
}

