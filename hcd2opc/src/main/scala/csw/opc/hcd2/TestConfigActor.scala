package csw.opc.hcd2

import akka.actor._
import csw.opc.client.JOpcDemoClient
import csw.services.cmd.akka.CommandQueueActor.SubmitWithRunId
import csw.services.cmd.akka.ConfigActor._
import csw.services.cmd.akka.{CommandStatus, ConfigActor, RunId}
import csw.util.cfg.Configurations._

import scala.util.Success


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
  assert(configKey == "filter" || configKey == "disperser" || configKey == "pos" || configKey == "one")

  // The last submit (the one currently being worked on)
  var lastSubmit: Option[SubmitWithRunId] = None

  // The listener argument listens for changes in the OPC variables for filter and disperser
  val opcClient = new JOpcDemoClient("localhost", new JOpcDemoClient.Listener {
    override def filterChanged(value: String): Unit = {
      if (configKey == "filter") returnStatus(value)
    }

    override def disperserChanged(value: String): Unit = {
      if (configKey == "disperser") returnStatus(value)
    }

    override def perfTestVarChanged(value: Int): Unit = {
      log.info(s"Perf test var set to $value")
    }

    override def analogArrayVarChanged(value: Array[Integer]): Unit = {}

    override def onEvent(value: Int): Unit = {}

    override def staticArrayVarChanged(value: Array[Integer]): Unit = {}
  })

  // Returns the command status to the submitter after the command has completed
  def returnStatus(value: String): Unit = {
    log.info(s"$configKey set to $value")
    lastSubmit match {
      case Some(submit) =>
        returnStatus(CommandStatus.Completed(submit.runId), submit.submitter)
        lastSubmit = None
      case _ =>
    }
  }

  // Receive
  override def receive: Receive = receiveConfigs

  /**
   * Called when a configuration is submitted
   */
  override def submit(submit: SubmitWithRunId): Unit = {
    log.info("submit to OPC")
    if (lastSubmit.isDefined) {
      // XXX The server will interrupt the currently running submit thread, but there might be race conditions
      // that would need to be taken care of in the real implementation
      returnStatus(CommandStatus.Canceled(submit.runId), submit.submitter)
    }
    lastSubmit = Some(submit)
    val config = submit.config.head.asInstanceOf[SetupConfig]
    val value = config("value").elems.head.toString
    // The listener passed to the OpcClient constructor will be notified when the filter or disperser
    // setting has completed
    configKey match {
      case "filter" => opcClient.setFilter(value)
      case "disperser" => opcClient.setDisperser(value)
    }
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
    val confs = for (config <- configs) yield configKey match {
      case "filter" => config.withValues("value" -> opcClient.getFilter)
      case "disperser" => config.withValues("value" -> opcClient.getDisperser)
    }

    sender() ! ConfigResponse(Success(confs))
  }
}

