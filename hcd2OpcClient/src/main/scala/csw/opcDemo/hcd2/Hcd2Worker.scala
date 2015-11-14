package csw.opcDemo.hcd2

import java.util.function.Consumer

import akka.actor._
import com.digitalpetri.opcua.stack.core.types.builtin.DataValue
import csw.opc.server.Hcd2Namespace
import csw.services.kvs.{ TelemetryService, StateVariableStore, KvsSettings }
import csw.util.cfg.Configurations.StateVariable.CurrentState
import csw.util.cfg.Configurations._
import csw.util.cfg.Events.StatusEvent
import csw.util.cfg.StandardKeys

import scala.language.postfixOps

object Hcd2Worker {
  def props(prefix: String): Props = Props(classOf[Hcd2Worker], prefix)

}

/**
 * An actor that does the work of matching a configuration
 */
class Hcd2Worker(prefix: String) extends Actor with ActorLogging {
  import context.dispatcher

  log.info(s"Started worker for $prefix")

  val settings = KvsSettings(context.system)
  val svs = StateVariableStore(settings)
  val telemetryService = TelemetryService(settings)
  val name = prefix.split('.').last
  val choices = if (name == "filter") Hcd2Namespace.FILTERS else Hcd2Namespace.DISPERSERS
  val key = if (prefix == StandardKeys.filterPrefix) StandardKeys.filter else StandardKeys.disperser
  val opcClient = new Hcd2OpcUaClient()

  // Subscribe to changes in the filter or disperser opcua variable and then update the state variable
  opcClient.subscribe(name, new Consumer[DataValue] {
    override def accept(v: DataValue): Unit = {
      val s = v.getValue.getValue.toString
      log.info(s"HCD subscriber: value for $name received: $s")

      svs.set(CurrentState(prefix).set(key, s))
    }
  })

  // Subscribe to changes in the opcua filterPos or disperserPos opcua variable and then set the telemetry value
  // (These values are generated by the HCD2 OPC UA server to simulate a wheel turning through different values)
  opcClient.subscribe(s"${name}Pos", new Consumer[DataValue] {
    override def accept(v: DataValue): Unit = {
      val pos = v.getValue.getValue.asInstanceOf[Int]
      val choice = choices(pos)
      log.info(s"HCD subscriber: value for ${name}Pos received: $choice")

      // Note: Could alternatively use a different key or data type for the telemetry,
      // here we use the filter or disperser keys
      telemetryService.set(StatusEvent(prefix).set(key, choice))
    }
  })

  override def receive: Receive = {
    case s: SetupConfig ⇒ submit(s)
    case x              ⇒ log.error(s"Unexpected message $x")
  }

  /**
   * Called when a configuration is submitted
   */
  def submit(setupConfig: SetupConfig): Unit = {
    setupConfig.get(key).foreach { value ⇒
      opcClient.setValue(name, value)
    }
  }
}

