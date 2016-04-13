package csw.opcDemo.hcd2

import akka.actor.Props
import csw.services.ccs.PeriodicHcdController
import csw.services.pkg.Component.HcdInfo
import csw.services.pkg.Supervisor._
import csw.services.pkg.{Hcd, LifecycleHandler}

// A test HCD that is configured with the given name and config path
object Hcd2 {
  /**
   * Used to create the actor
   */
  def props(info: HcdInfo): Props = Props(classOf[Hcd2], info)
}

/**
 * Test HCD
 */
case class Hcd2(info: HcdInfo) extends Hcd with PeriodicHcdController with LifecycleHandler {
  val worker = context.actorOf(Hcd2Worker.props(info.prefix))

  lifecycle(supervisor)
  processAt(info.rate)

  def receive = controllerReceive orElse lifecycleHandlerReceive

  override def process(): Unit = {
    // Process all configs in the queue
    // (Note: How to deal with multiple configs in the queue? Maybe depends on device?)
    nextConfig.foreach { config ⇒
      worker ! config
    }
  }

}
