package csw.opcDemo.client

import com.prosysopc.ua._
import com.prosysopc.ua.client._
import com.prosysopc.ua.nodes._
import csw.opcDemo.server.OpcDemoNodeManager
import org.apache.log4j.{PropertyConfigurator, Logger}
import org.opcfoundation.ua.builtintypes._
import org.opcfoundation.ua.core._
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy
import org.opcfoundation.ua.transport.security.SecurityMode
import java.io.File
import java.net.InetAddress
import java.util._

import scala.concurrent.Future

class OpcDemoClient {
  val APP_NAME = "OpcDemoClient"
  val log = Logger.getLogger(classOf[OpcDemoClient])

  val serverStatusListener: ServerStatusListener = new ServerStatusListener {
    def onShutdown(uaClient: UaClient, secondsTillShutdown: Long, shutdownReason: LocalizedText) {
      log.info(s"Server shutdown in $secondsTillShutdown seconds. Reason: ${shutdownReason.getText}")
    }

    def onStateChange(uaClient: UaClient, oldState: ServerState, newState: ServerState) {
      log.info(s"ServerState changed from $oldState to $newState")
      if (newState == ServerState.Unknown) log.info("ServerStatusError: " + uaClient.getServerStatusError)
    }

    def onStatusChange(uaClient: UaClient, status: ServerStatusDataType) {
    }
  }

  val subscriptionAliveListener: SubscriptionAliveListener = new SubscriptionAliveListener {
    def onAlive(s: Subscription) {
    }

    def onTimeout(s: Subscription) {
      log.info(s"Subscription timeout")
    }
  }

  val subscriptionListener: SubscriptionNotificationListener = new SubscriptionNotificationListener {
    def onBufferOverflow(subscription: Subscription, sequenceNumber: UnsignedInteger, notificationData: Array[ExtensionObject]) {
      log.error("Subscription buffer overflow")
    }

    def onDataChange(subscription: Subscription, item: MonitoredDataItem, newValue: DataValue) {
    }

    def onError(subscription: Subscription, notification: AnyRef, e: Exception) {
      log.error("Subscription error", e)
    }

    def onEvent(subscription: Subscription, item: MonitoredEventItem, eventFields: Array[Variant]) {
    }

    def onMissingData(lastSequenceNumber: UnsignedInteger, sequenceNumber: Long, newSequenceNumber: Long, serviceResult: StatusCode): Long = {
      log.warn(s"Data missed: lastSequenceNumber: $lastSequenceNumber, newSequenceNumber: $newSequenceNumber")
      newSequenceNumber
    }

    def onNotificationData(subscription: Subscription, notification: NotificationData) {
    }

    def onStatusChange(subscription: Subscription, oldStatus: StatusCode, newStatus: StatusCode, diagnosticInfo: DiagnosticInfo) {
    }
  }

  val client: UaClient = initialize()
  connect()

  // XXX FIXME do this in a handler whenever connected
  val uri = OpcDemoNodeManager.NAMESPACE
  val ns = client.getAddressSpace.getNamespaceTable.getIndex(uri)
  val filterNodeId = new NodeId(ns, "FilterIndex")
  val disperserNodeId = new NodeId(ns, "DisperserIndex")
  subscribe(filterNodeId)
  subscribe(disperserNodeId)


  /**
   * Initializes the object, must be called after constructor
   */
  private def initialize(): UaClient = {
    val serverUri: String = "opc.tcp://localhost:52520/OPCUA/OpcDemoServer"
    println("Connecting to " + serverUri)
    val uaClient = new UaClient(serverUri)
    val validator: PkiFileBasedCertificateValidator = new PkiFileBasedCertificateValidator
    uaClient.setCertificateValidator(validator)
    val appDescription: ApplicationDescription = new ApplicationDescription
    appDescription.setApplicationName(new LocalizedText(APP_NAME, Locale.ENGLISH))
    appDescription.setApplicationUri("urn:localhost:UA:OpcDemoClient")
    appDescription.setProductUri("urn:prosysopc.com:UA:OpcDemoClient")
    appDescription.setApplicationType(ApplicationType.Client)
    val privatePath: File = new File(validator.getBaseDir, "private")
    val identity: ApplicationIdentity = ApplicationIdentity.loadOrCreateCertificate(appDescription,
      "Sample Organisation", "opcua", privatePath, null, null, true)
    val hostName: String = InetAddress.getLocalHost.getHostName
    identity.setHttpsCertificate(ApplicationIdentity.loadOrCreateHttpsCertificate(appDescription,
      hostName, "opcua", null, privatePath, true))
    uaClient.setApplicationIdentity(identity)
    uaClient.setLocale(Locale.ENGLISH)
    uaClient.setTimeout(30000)
    uaClient.setStatusCheckTimeout(10000)
    uaClient.addServerStatusListener(serverStatusListener)
    uaClient.setSecurityMode(SecurityMode.NONE)
    uaClient.getHttpsSettings.setHttpsSecurityPolicies(HttpsSecurityPolicy.TLS_1_0, HttpsSecurityPolicy.TLS_1_1)
    uaClient.getHttpsSettings.setCertificateValidator(validator)
    uaClient.setUserIdentity(new UserIdentity)
    uaClient.getEndpointConfiguration.setMaxByteStringLength(Integer.MAX_VALUE)
    uaClient.getEndpointConfiguration.setMaxArrayLength(Integer.MAX_VALUE)
    uaClient
  }

  /**
   * Connect to the server.
   */
  private def connect(): Unit = {
    if (!client.isConnected) try {
      client.setSessionName(APP_NAME)
      client.connect()
      log.info("ServerStatus: " + client.getServerStatus)
    } catch {
      case ex: Exception => log.error("connect", ex)
    }
  }

  private def subscribe(nodeId: NodeId) {
    println("*** Subscribing to node: " + nodeId)
    try {
      val subscription: Subscription = createSubscription
      createMonitoredItem(subscription, nodeId, Attributes.Value)
    }
    catch {
      case e: Exception =>
        log.error("subscribe", e)
    }
  }

  private def createSubscription: Subscription = {
    val subscription: Subscription = new Subscription
    subscription.addAliveListener(subscriptionAliveListener)
    subscription.addNotificationListener(subscriptionListener)
    if (!client.hasSubscription(subscription.getSubscriptionId)) client.addSubscription(subscription)
    subscription
  }

  private def createMonitoredItem(sub: Subscription, nodeId: NodeId, attributeId: UnsignedInteger) {
    var monitoredItemId: UnsignedInteger = null
    if (!sub.hasItem(nodeId, attributeId)) {
      val dataItem: MonitoredDataItem = createMonitoredDataItem(nodeId, attributeId)
      dataItem.setDataChangeFilter(null)
      sub.addItem(dataItem)
      monitoredItemId = dataItem.getMonitoredItemId
    }
    println("Subscription: Id=" + sub.getSubscriptionId + " ItemId=" + monitoredItemId)
  }

  private def createMonitoredDataItem(nodeId: NodeId, attributeId: UnsignedInteger): MonitoredDataItem = {
    val dataItem: MonitoredDataItem = new MonitoredDataItem(nodeId, attributeId, MonitoringMode.Reporting)
    dataItem.setDataChangeListener(new MonitoredDataItemListener {
      def onDataChange(sender: MonitoredDataItem, prevValue: DataValue, value: DataValue) {
        val s: String = if (nodeId eq filterNodeId) "filter" else "disperser"
        log.info(s"$s changed to ${value.getValue}")
      }
    })
    dataItem
  }

  private def read(nodeId: NodeId): Int = {
    val attributeId: UnsignedInteger = Attributes.Value
    val value: DataValue = client.readAttribute(nodeId, attributeId)
    value.getValue.intValue
  }

  private def write(nodeId: NodeId, value: Integer) {
    val attributeId: UnsignedInteger = Attributes.Value
    val node: UaNode = client.getAddressSpace.getNode(nodeId)
    println("Writing to node " + nodeId + " - " + node.getDisplayName.getText)
    var dataType: UaDataType = null
    if (attributeId == Attributes.Value && node.isInstanceOf[UaVariable]) {
      val v: UaVariable = node.asInstanceOf[UaVariable]
      if (v.getDataType == null) v.setDataType(client.getAddressSpace.getType(v.getDataTypeId))
      dataType = v.getDataType.asInstanceOf[UaDataType]
      log.info("DataType: " + dataType.getDisplayName.getText)
    }
    try {
      val status: Boolean = client.writeAttribute(nodeId, attributeId, value)
      if (status) log.info("OK")
      else log.info("OK (completes asynchronously)")
    }
    catch {
      case e: Any =>
        log.error("write", e)
    }
  }

  def readFilter: Int = {
    read(filterNodeId)
  }

  def writeFilter(value: Int) {
    write(filterNodeId, value)
  }

  def readDisperser: Int = {
    read(disperserNodeId)
  }

  def writeDisperser(value: Int) {
    write(disperserNodeId, value)
  }

}

object OpcDemoClient {
  // Demo main
  def main(args: Array[String]) {
    import scala.concurrent.ExecutionContext.Implicits.global

    PropertyConfigurator.configureAndWatch(classOf[OpcDemoClient].getResource("../log.properties").getFile, 5000)
    val client = new OpcDemoClient
    client.log.info("filter initial value: " + client.readFilter)
    Future {
      Thread.sleep(500)
      client.writeFilter(99)
    }
    client.writeFilter(2)
    assert(client.readFilter == 2)
    client.log.info("disperser initial value: " + client.readDisperser)
    client.writeDisperser(3)
    assert(client.readDisperser == 3)
  }
}