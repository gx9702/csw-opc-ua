package csw.opcDemo.container2

import akka.actor._
import csw.services.cmd.akka.{CommandStatus, ConfigActor, RunId}
import csw.services.cmd.akka.CommandQueueActor.SubmitWithRunId
import akka.util.ByteString
import scala.util.{Failure, Success}
import csw.services.cmd.akka.ConfigActor._
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import scala.concurrent.duration._
import csw.util.cfg.ConfigJsonFormats
import csw.util.cfg.Configurations._
import spray.json._


object TestConfigActor {
  def props(commandStatusActor: ActorRef, configKey: String, numberOfSecondsToRun: Int = 2): Props =
    Props(classOf[TestConfigActor], commandStatusActor, configKey, numberOfSecondsToRun)

  val config = ConfigFactory.load("TestConfigActor")
}

/**
 * A test config actor (simulates an actor that does the work of executing a configuration).
 *
 * @param commandStatusActor actor that receives the command status messages
 * @param numberOfSecondsToRun the number of seconds to run the simulated work
 * @param configKey set to the last component of a key, for example: "filter" or "disperser" in this test
 */
class TestConfigActor(override val commandStatusActor: ActorRef, configKey: String,
                      numberOfSecondsToRun: Int) extends ConfigActor {


  val url = TestConfigActor.config.getString(s"TestConfigActor.$configKey.url")
  log.info(s"For $configKey: using ZMQ URL = $url")

  val zmqClient = context.actorOf(OpcUaClient.props(url))

  // XXX temp: change to get values over ZMQ from hardware simulation
  var savedConfig: Option[SetupConfigList] = None


  // Receive
  override def receive: Receive = receiveConfigs

  /**
   * Called when a configuration is submitted
   * (XXX Fix this so the ZMQ C code saves the value)
   */
  override def submit(submit: SubmitWithRunId): Unit = {
    // Save the config for this test, so that query can return it later
    savedConfig = Some(submit.config.asInstanceOf[SetupConfigList])
    log.info("Sending dummy message to ZMQ hardware simulation")

    // Note: We could just send the JSON and let the C code parse it, but for now, keep it simple
    // and extract the value here
    val config = submit.config.head.asInstanceOf[SetupConfig]
    val value = configKey match {
      case "filter" | "disperser" => config("value").elems.head
      case "pos" | "one" =>
        val c1 = config("c1").elems.head
        val c2 = config("c2").elems.head
        val equinox = config("equinox").elems.head
        s"$c1 $c2 $equinox"
      case _ => "error"
    }

    // For this test, a timestamp value is inserted by assembly1 (Later the JSON can be just passed on to ZMQ)
    val zmqMsg = configKey match {
      case "filter" =>
        val timestamp = config("timestamp").elems.head
        ByteString(s"$configKey=$value, timestamp=$timestamp", "UTF-8")
      case _ =>
        ByteString(s"$configKey=$value", "UTF-8")
    }

    implicit val dispatcher = context.system.dispatcher
    ask(zmqClient, OpcUaClient.Command(zmqMsg))(6 seconds) onComplete {
      case Success(reply: ByteString) =>
        val msg = reply.decodeString("UTF-8")
        log.info(s"ZMQ Message: $msg")
        val status = if (msg == "OK") {
          CommandStatus.Completed(submit.runId)
        } else {
          CommandStatus.Error(submit.runId, msg)
        }
        returnStatus(status, submit.submitter)

      case Success(m) => // should not happen
        val status = CommandStatus.Error(submit.runId, s"Unexpected ZMQ Message: $m")
        returnStatus(status, submit.submitter)

      case Failure(ex) =>
        val status = CommandStatus.Error(submit.runId, ex.getMessage)
        returnStatus(status, submit.submitter)
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
    // XXX TODO: replace savedConfig and get values over ZMQ from hardware simulation
    val confs = savedConfig match {
      case Some(c) => c
      case None =>
        for(config <- configs) yield
        if (configKey == "filter") {
          config.withValues("value" -> "None")
        } else if (configKey == "disperser") {
          config.withValues("value" -> "Mirror")
        } else if (configKey == "pos") {
          config.withValues("posName" -> "m653", "c1" -> "03:19:34.2", "c2" -> "31:23:21.5", "equinox" -> "J2000")
        } else if (configKey == "one") {
          config.withValues("c1" -> "03:20:29.2", "c2" -> "31:24:02.1", "equinox" -> "J2000")
        } else config
    }

    sender() ! ConfigResponse(Success(confs))
  }
}

