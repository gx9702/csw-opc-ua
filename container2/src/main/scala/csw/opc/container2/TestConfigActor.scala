package csw.opc.container2

import akka.actor._
import csw.opc.client.OpcDemoClient
import csw.services.cmd.akka.{CommandStatus, ConfigActor, RunId}
import csw.services.cmd.akka.CommandQueueActor.SubmitWithRunId
import org.opcfoundation.ua.common.ServiceResultException
import org.opcfoundation.ua.transport.ResultListener
import scala.util.Success
import csw.services.cmd.akka.ConfigActor._
import csw.util.cfg.Configurations._


object TestConfigActor {
  def props(commandStatusActor: ActorRef, configKey: String): Props =
    Props(classOf[TestConfigActor], commandStatusActor, configKey)
}

/**
 * A test config actor (simulates an actor that does the work of executing a configuration).
 *
 * @param commandStatusActor actor that receives the command status messages
 * @param configKey set to "filter" or "disperser"
 */
class TestConfigActor(override val commandStatusActor: ActorRef, configKey: String) extends ConfigActor {
  assert(configKey == "filter" || configKey == "disperser")

  // Note: The listener here listens for changes in the OPC variables for filter and disperser
  log.info("XXX create OPC client: start")
  val opcClient = new OpcDemoClient(new OpcDemoClient.Listener {
    override def filterChanged(value: String): Unit = {
      log.info(s"filter set to $value")
    }

    override def disperserChanged(value: String): Unit = {
      log.info(s"disperser set to $value")
    }
  })
  log.info("XXX create OPC client: done")

  // Receive
  override def receive: Receive = receiveConfigs

  /**
   * Called when a configuration is submitted
   */
  override def submit(submit: SubmitWithRunId): Unit = {
    log.info("XXX submit to OPC")
    val config = submit.config.head.asInstanceOf[SetupConfig]
    val value = config("value").elems.head.toString
    val result = configKey match {
      case "filter" => opcClient.setFilterAsync(value)
      case "disperser" => opcClient.setDisperserAsync(value)
    }
    // The listener here is called when the method completes.
    result.setListener(new ResultListener() {
      override def onCompleted(o: Any): Unit = {
        returnStatus(CommandStatus.Completed(submit.runId), submit.submitter)
      }

      override def onError(e: ServiceResultException): Unit = {
        returnStatus(CommandStatus.Error(submit.runId, e.getMessage), submit.submitter)
      }
    })
  }

  /**
   * Work on the config matching the given runId should be paused
   */
  override def pause(runId: RunId): Unit = {
  }

  /**
   * Work on the config matching the given runId should be resumed
   */
  override def resume(runId: RunId): Unit = {
  }

  /**
   * Work on the config matching the given runId should be canceled
   */
  override def cancel(runId: RunId): Unit = {
  }

  /**
   * Work on the config matching the given runId should be aborted
   */
  override def abort(runId: RunId): Unit = {
  }

  /**
   * Query the current state of a device and reply to the sender with a ConfigResponse object.
   * A config is passed in (the values are ignored) and the reply will be sent containing the
   * same config with the current values filled out.
   *
   * @param configs used to specify the keys for the values that should be returned
   * @param replyTo reply to this actor with the config response
   *
   */
  override def query(configs: SetupConfigList, replyTo: ActorRef): Unit = {
    log.info("XXX query OPC")
    val confs = for (config <- configs) yield configKey match {
      case "filter" => config.withValues("value" -> opcClient.getFilter)
      case "disperser" => config.withValues("value" -> opcClient.getDisperser)
    }

    sender() ! ConfigResponse(Success(confs))
  }
}

