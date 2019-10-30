package csw.opcUaDemo.opcUaHcd

import java.util.function.Consumer

import akka.actor._
import csw.services.log.PrefixedActorLogging
import csw.util.config.Configurations._

import scala.concurrent.duration._
import csw.util.config.ConfigDSL._
import csw.util.config.StringKey
import org.eclipse.milo.opcua.sdk.core.NumericRange
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong

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
  private val key = StringKey(name)

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
      name match {
        case "CycleCounter" => opcUaClient.subscribe (name, new CycleCounterConsumer () )
        case "arrPublishedCounts" => opcUaClient.subscribe (name, new ArrayCountsConsumer () )
      }

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

  class ArrayCountsConsumer extends Consumer[DataValue] {
    var counter: Option[ULong] = None

    override def accept(v: DataValue): Unit = {

      if (v.getValue.getDataType.get.equals(Identifiers.UInt64)) {
        val newValue = v.getValue.getValue
        log.debug(s"New value = $newValue")
        val arr = NumericRange.readFromValueAtRange(v.getValue, NumericRange.parse("0:99")) match {
          case x:Array[ULong] => {
            if (counter.isEmpty) {
              log.debug(s"Setting first value as ${x(0)}")
              counter = Some(x(0))
            }
            for (i <- x) {
              if (i != counter.get) {
                log.error(s"Value is $i, expected ${counter.get}")
              }
              counter = Some(counter.get.add(1))
            }
          }
          case _ => log.debug("not array?")
        }
      }
    }
  }

  class CycleCounterConsumer extends Consumer[DataValue] {
    var counter: Option[Long] = None

    def accept(v: DataValue): Unit = {
      if (v.getValue.getDataType.get.equals(Identifiers.UInt64)) {
        val newValue = v.getValue.getValue.toString.toLong
        if (counter.isDefined) {
          /*
        v.getValue.getDataType.get match {
          case Identifiers.UInt16 => log.info("Value is a UInt16")
          case Identifiers.UInt32 => log.info("Value is a UInt32")
          case Identifiers.UInt64 => log.info("Value is a UInt64")
          case other => log.info(s"Unknown Type $other")
        }
        */
          val expectedValue = counter.get + 1
          if (newValue != expectedValue) {
            log.info(s"New value for $name is $newValue.  Expected $expectedValue")
          }
        }
        counter = Some(newValue)
      }
      //val s = v.getValue.getValue.toString

      //log.info(s"HCD subscriber: value for $name received: $s")
    }
  }

}

