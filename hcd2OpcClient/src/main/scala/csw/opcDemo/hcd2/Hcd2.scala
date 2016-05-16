package csw.opcDemo.hcd2

import akka.actor.Props
import csw.services.ccs.HcdController
import csw.services.pkg.Component.HcdInfo
import csw.services.pkg.Supervisor._
import csw.services.pkg.{Hcd, LifecycleHandler}
import csw.util.cfg.Configurations.SetupConfig

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
