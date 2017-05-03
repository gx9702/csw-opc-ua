package csw.opcUaDemo.opcUaHcd

import java.util.function.Consumer

import akka.actor._
import csw.services.log.PrefixedActorLogging
import csw.util.config.Configurations._

import scala.concurrent.duration._
import csw.util.config.ConfigDSL._
import csw.util.config.StringKey
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue

import scala.language.postfixOps

object OpcUaHcdWorker {
  def props(prefix: String): Props = Props(classOf[OpcUaHcdWorker], prefix)

  // Message used to try/retry to connect to the OPC server
  case object TryOpcConnection

  // Message requesting current state of HCD values
  case object RequestCurrentState
}

/**
 * An actor that does the work of matching a configuration
 */
class OpcUaHcdWorker(override val prefix: String) extends Actor with PrefixedActorLogging {
  import context.dispatcher
  import OpcUaHcd._
  import OpcUaHcdWorker._

  log.info(s"Started worker for $prefix")

  private val name = prefix.split('.').last
  private val key = StringKey("CylceCounter")

  // We can't do anything until the OPC UA server is available
  context.become(waitingForOpcUaServer)
  tryOpcConnection()

  override def receive: Receive = Actor.emptyBehavior

  // State while waiting for a connection to the OPC UA server
  private def waitingForOpcUaServer: Receive = {
    case TryOpcConnection => tryOpcConnection()
    case _: SetupConfig   => log.error("Not connected to OPC UA server")
    case x                => log.error(s"Unexpected message $x")
  }

  // State while connected to the OPC UA server
  private def connected(opcClient: OpcUaHcdClient, currentPos: String): Receive = {
    case s: SetupConfig => submit(s, opcClient)

    // Send the parent the current state
    case RequestCurrentState =>
      context.parent ! cs(prefix, key -> currentPos)

    case x => log.error(s"Unexpected message $x")
  }

  private def tryOpcConnection(): Unit = {
    try {
      val opcUaClient = new OpcUaHcdClient()

      // Subscribe to changes in the filter or disperser opcua variable and then update the state variable
      opcUaClient.subscribe(name, new Consumer[DataValue] {
        override def accept(v: DataValue): Unit = {

          val s = v.getValue.getValue.toString
          log.info(s"HCD subscriber: value for $name received: $s")
        }
      })

      log.info(s"$name: Connected to OPC UA server")
      context.become(connected(opcUaClient, "0"))
    } catch {
      case ex: Exception =>
        // Retry the connection in a second
        log.warning(s"$name: Failed to connect to OPC server (${ex.getMessage}). Will retry in 1 sec.")
        context.system.scheduler.scheduleOnce(1.second, self, TryOpcConnection)
    }
  }

  /**
   * Called when a configuration is submitted
   */
  def submit(setupConfig: SetupConfig, opcUaClient: OpcUaHcdClient): Unit = {
    setupConfig.get(key).foreach { value =>
      opcUaClient.setValue(name, value.head)
    }
  }
}

