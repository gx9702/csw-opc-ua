package csw.opcDemo.hcd2

import akka.actor._
import csw.services.kvs.{ StateVariableStore, KvsSettings }
import csw.util.cfg.Configurations.StateVariable.CurrentState
import csw.util.cfg.Configurations._
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

  val svs = StateVariableStore(KvsSettings(context.system))
  val opcKey = prefix.split('.').last
  val opcClient = new Hcd2OpcUaClient()
  opcClient.subscribe(opcKey)

  override def receive: Receive = {
    case s: SetupConfig ⇒ submit(s)
    case x              ⇒ log.error(s"Unexpected message $x")
  }

  /**
   * Called when a configuration is submitted
   */
  def submit(setupConfig: SetupConfig): Unit = {

    // Note: We could just send the JSON and let the C code parse it, but for now, keep it simple
    // and extract the value here
    val key = if (opcKey == "filter") StandardKeys.filter else StandardKeys.disperser
    setupConfig.get(key).foreach { value ⇒
      opcClient.setValue(opcKey, value)
      // XXX TEMP FIXME
      svs.set(CurrentState(setupConfig))

      //      val zmqMsg = ByteString(s"$opcKey=$value")

      //      ask(zmqClient, ZmqClient.Command(zmqMsg))(6 seconds) onComplete {
      //        case Success(reply: ByteString) ⇒
      //          val msg = reply.decodeString(ZMQ.CHARSET.name())
      //          log.info(s"ZMQ Message: $msg")
      //          // For this test, assume setupConfig is the current state of the device...
      //          svs.set(CurrentState(setupConfig))
      //
      //        case Success(m) ⇒ // should not happen
      //          log.error(s"Unexpected reply from zmq: $m")
      //
      //        case Failure(ex) ⇒
      //          log.error("Error talking to zmq", ex)
      //      }

    }

  }
}

