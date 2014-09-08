package csw.opc

import csw.opc.client.OpcDemoClient
import org.apache.log4j.Logger
import org.scalatest.DoNotDiscover

// This test requires that OpcDemoServer server is running
@DoNotDiscover
object OpcDemoClientTests extends App {
  val log = Logger.getLogger(classOf[OpcDemoClient])

  val client = new OpcDemoClient(new OpcDemoClient.Listener() {
    def filterChanged(value: String): Unit = {
      log.info(s"filter changed to: $value")
    }

    def disperserChanged(value: String): Unit = {
      log.info(s"disperser changed to: $value")
    }
  })

  // setFilter and setDisperser call the OPC UA methods and return when done.
  // When the operation is done, the listener passed to the OpcDemoClient constructor is
  // notified
  client.setFilter("newFilter")
  client.setDisperser("newDisperser")

  // The async versions call the OPC UA methods asynchronously and notify
  // the given listener when done.
  client.setFilter("newFilter2")
}
