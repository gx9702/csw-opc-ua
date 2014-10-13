package csw.opc.client

import com.prosysopc.ua._
import com.prosysopc.ua.client._
import com.prosysopc.ua.nodes.{UaVariable, UaDataType}
import csw.opc.server.{OpcDemoServer, OpcDemoNodeManager}
import org.apache.log4j.Logger
import org.opcfoundation.ua.builtintypes._
import org.opcfoundation.ua.core._
import org.opcfoundation.ua.transport.AsyncResult
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy
import org.opcfoundation.ua.transport.security.SecurityMode
import java.io.File
import java.net.InetAddress
import java.util._

// Scala version of the OPC UA client:
// XXX FIXME: Needs to be updated to include the perfTest and event features that are in the java version
case class FreeOpcDemoClient(listener: FreeOpcDemoClient.Listener) {
  val APP_NAME = "FreeOpcDemoClient"
  val log = Logger.getLogger(classOf[FreeOpcDemoClient])

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

  val subscriptionAliveListener = new SubscriptionAliveListener {
    def onAlive(s: Subscription) {
    }

    def onTimeout(s: Subscription) {
      log.info(s"Subscription timeout")
    }
  }

  val subscriptionListener = new SubscriptionNotificationListener {
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

  val client = initialize()
  connect()

  // XXX FIXME do this in a handler whenever connected
  val uri = OpcDemoNodeManager.NAMESPACE
  val addressSpace = client.getAddressSpace
  val namespaceTable = addressSpace.getNamespaceTable
//  val ns = namespaceTable.getIndex(uri)
  val ns = 2 // XXX above doesn't work with freeopcua server
  val deviceNodeId = new NodeId(ns, "OpcDemoDevice")
  val filterNodeId = new NodeId(ns, "Filter")
  val disperserNodeId = new NodeId(ns, "Disperser")
  val setFilterNodeId = new NodeId(ns, "setFilter")
  val setDisperserNodeId = new NodeId(ns, "setDisperser")
  subscribe(filterNodeId)
  subscribe(disperserNodeId)


  /**
   * Initializes the object, must be called after constructor
   */
  private def initialize(): UaClient = {
    // XXX CHANGE IP ADDRESS to that of OPC UA server
    val serverUri = "opc.tcp://192.168.2.101:52520/OPCUA/" + OpcDemoServer.APP_NAME

    log.info("Connecting to " + serverUri)
    val uaClient = new UaClient(serverUri)
    val validator = new PkiFileBasedCertificateValidator
    uaClient.setCertificateValidator(validator)
    val appDescription = new ApplicationDescription
    appDescription.setApplicationName(new LocalizedText(APP_NAME, Locale.ENGLISH))
    appDescription.setApplicationUri("urn:localhost:UA:FreeOpcDemoClient")
    appDescription.setProductUri("urn:prosysopc.com:UA:FreeOpcDemoClient")
    appDescription.setApplicationType(ApplicationType.Client)
    val privatePath = new File(validator.getBaseDir, "private")
    val identity: ApplicationIdentity = ApplicationIdentity.loadOrCreateCertificate(appDescription,
      "Sample Organisation", "opcua", privatePath, null, null, true)
    val hostName = InetAddress.getLocalHost.getHostName
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
    uaClient.setUserIdentity(new UserIdentity())
    uaClient.getEndpointConfiguration.setMaxByteStringLength(Integer.MAX_VALUE)
    uaClient.getEndpointConfiguration.setMaxArrayLength(Integer.MAX_VALUE)

    uaClient
  }

  /**
   * Connect to the server.
   */
  private def connect(): Unit =
    if (!client.isConnected) try {
      client.setSessionName(APP_NAME)
      client.connect()
      log.info("ServerStatus: " + client.getServerStatus)
    } catch {
      case ex: Exception => log.error("connect", ex)
    }


  private def callMethod(methodId: NodeId, value: String): Unit =
    client.call(deviceNodeId, methodId, new Variant(value))

  private def callMethodAsync(methodId: NodeId, value: String): AsyncResult =
    client.callAsync(new CallMethodRequest(deviceNodeId, methodId, Array(new Variant(value))))


  private def subscribe(nodeId: NodeId): Unit =
    try {
      val subscription = createSubscription
      createMonitoredItem(subscription, nodeId, Attributes.Value)
    }
    catch {
      case e: Exception =>
        log.error("subscribe", e)
    }


  private def createSubscription: Subscription = {
    val subscription = new Subscription
    subscription.addAliveListener(subscriptionAliveListener)
    subscription.addNotificationListener(subscriptionListener)
    if (!client.hasSubscription(subscription.getSubscriptionId)) client.addSubscription(subscription)
    subscription
  }

  private def createMonitoredItem(sub: Subscription, nodeId: NodeId, attributeId: UnsignedInteger) {
    var monitoredItemId: UnsignedInteger = null
    if (!sub.hasItem(nodeId, attributeId)) {
      val dataItem = createMonitoredDataItem(nodeId, attributeId)
      dataItem.setDataChangeFilter(null)
      sub.addItem(dataItem)
      monitoredItemId = dataItem.getMonitoredItemId
    }
  }

  private def createMonitoredDataItem(nodeId: NodeId, attributeId: UnsignedInteger): MonitoredDataItem = {
    val dataItem = new MonitoredDataItem(nodeId, attributeId, MonitoringMode.Reporting)
    dataItem.setDataChangeListener(new MonitoredDataItemListener {
      def onDataChange(sender: MonitoredDataItem, prevValue: DataValue, value: DataValue) {
        if (nodeId == filterNodeId) {
          listener.filterChanged(value.getValue.toString)
        } else if (nodeId == disperserNodeId) {
          listener.disperserChanged(value.getValue.toString)
        }
      }
    })
    dataItem
  }

  private def read(nodeId: NodeId): String = {
    val value = client.readAttribute(nodeId, Attributes.Value)
    value.getValue.toString
  }

  private def write(nodeId: NodeId, value: String) {
    val attributeId = Attributes.Value
    val node = client.getAddressSpace.getNode(nodeId)
    println("Writing to node " + nodeId + " - " + node.getDisplayName.getText)
    if (attributeId == Attributes.Value && node.isInstanceOf[UaVariable]) {
      val v = node.asInstanceOf[UaVariable]
      if (v.getDataType == null) v.setDataType(client.getAddressSpace.getType(v.getDataTypeId))
      val dataType = v.getDataType.asInstanceOf[UaDataType]
      log.info("DataType: " + dataType.getDisplayName.getText)
    }
    try {
      val status = client.writeAttribute(nodeId, attributeId, value)
      if (status) log.info("OK")
      else log.info("OK (completes asynchronously)")
    }
    catch {
      case e: Any =>
        log.error("write", e)
    }
  }

  def getFilter: String = read(filterNodeId)

  def getDisperser: String = read(disperserNodeId)

  def setFilter(filter: String): Unit = {
//    callMethod(setFilterNodeId, filter)
    write(filterNodeId, filter)
  }

  def setDisperser(disperser: String): Unit = {
//    callMethod(setDisperserNodeId, disperser)
    write(disperserNodeId, disperser)
  }
}

object FreeOpcDemoClient {
  val log = Logger.getLogger(classOf[FreeOpcDemoClient])

  // Listen for changes in the filter or disperser variable values after calling one of the set* OPC methods
  trait Listener {
    def filterChanged(value: String): Unit

    def disperserChanged(value: String): Unit
  }
}