package csw.opcUaDemo.opcUaHcd

import akka.actor.ActorRef
import csw.services.ccs.HcdController
import csw.services.pkg.Component.HcdInfo
import csw.services.pkg.Supervisor.Initialized
import csw.services.pkg.Hcd
import csw.util.config.Configurations.SetupConfig
import csw.util.config.StringKey

/**
 * Test HCD
 */
case class OpcUaHcd(info: HcdInfo, supervisor: ActorRef) extends Hcd with HcdController {
  private val worker = context.actorOf(OpcUaHcdWorker.props(info.prefix))

  supervisor ! Initialized

  override def receive: Receive = controllerReceive

  // Send the config to the worker for processing
  override protected def process(config: SetupConfig): Unit = {
    worker ! config
  }

  // Ask the worker actor to send us the current state (handled by parent trait)
  override protected def requestCurrent(): Unit = {
    worker ! OpcUaHcdWorker.RequestCurrentState
  }
}

object OpcUaHcd {
}

