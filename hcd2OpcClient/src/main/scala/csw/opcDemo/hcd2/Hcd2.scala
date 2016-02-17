package csw.opcDemo.hcd2

import akka.actor.Props
import com.typesafe.config.{ConfigFactory, Config}
import csw.services.ccs.PeriodicHcdController
import csw.services.pkg.{LifecycleHandler, Hcd}
import csw.util.cfg.{StandardKeys, Key}

// A test HCD that is configured with the given name and config path
object Hcd2 {
  /**
   * Used to create the actor
   * @param name the HCD's name
   * @param config optional settings for the HCD
   */
  def props(name: String, config: Config = ConfigFactory.empty()): Props = Props(classOf[Hcd2], name, config)
}

/**
 * Test HCD
 * @param name the name of the HCD
 * @param config config file with settings for the HCD
 */
case class Hcd2(name: String, config: Config) extends Hcd with PeriodicHcdController with LifecycleHandler {
  val prefix = if (name == "HCD-2A") StandardKeys.filterPrefix else StandardKeys.disperserPrefix
  val worker = context.actorOf(Hcd2Worker.props(prefix))

  // Reads the "rate" from the config file and starts the periodic processing
  // (process() method will be called at the given rate)
  startProcessing(config)

  override def process(): Unit = {
    // Process all configs in the queue
    // (Note: How to deal with multiple configs in the queue? Maybe depends on device?)
    nextConfig.foreach { config ⇒
      worker ! config
    }
  }

}
