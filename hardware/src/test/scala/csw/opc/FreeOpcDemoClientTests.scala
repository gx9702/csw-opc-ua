package csw.opc

import csw.opc.client.FreeOpcDemoClient
import org.apache.log4j.Logger
import org.scalatest.DoNotDiscover

// This test requires that OpcDemoServer server is running
@DoNotDiscover
object FreeOpcDemoClientTests extends App {
  val log = Logger.getLogger(classOf[FreeOpcDemoClient])

  val client = new FreeOpcDemoClient(new FreeOpcDemoClient.Listener() {
    def filterChanged(value: String): Unit = {
      log.info(s"filter changed to: $value")
    }

    def disperserChanged(value: String): Unit = {
      log.info(s"disperser changed to: $value")
    }
  })

  client.setFilter("newFilter")
  client.setDisperser("newDisperser")

  client.setFilter("newFilter2")
}
