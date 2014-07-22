package csw.opcDemo.container2

import akka.actor.{Props, Actor, ActorLogging}
import akka.util.ByteString
import scala.concurrent.Future

object OpcUaClient {
  def props(url: String): Props = Props(classOf[OpcUaClient], url)

  // Type of a command this actor receives from clients
  case class Command(m: ByteString)
}

class OpcUaClient(url: String) extends Actor with ActorLogging {
  import context.dispatcher

  override def receive: Receive = {
    case OpcUaClient.Command(byteString) ⇒
//      val replyTo = sender()
//      Future {
//        socket.send(byteString.toArray, 0)
//        val reply = socket.recv(0)
//        replyTo ! ByteString(reply)
//      }

    case x ⇒ log.info(s"Unexpected Message from OPC UA: $x")
  }
}
