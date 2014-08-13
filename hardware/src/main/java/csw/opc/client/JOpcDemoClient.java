package csw.opc.client;

import com.prosysopc.ua.*;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.client.*;
import com.prosysopc.ua.nodes.*;
import csw.opc.server.OpcDemoEventType;
import csw.opc.server.OpcDemoNodeManager;
import csw.opc.server.OpcDemoServer;
import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.*;
import org.opcfoundation.ua.core.*;
import org.opcfoundation.ua.transport.AsyncResult;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.SecurityMode;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.*;

/**
 * A demo Java OPC UA client
 */
public class JOpcDemoClient {

    private static String APP_NAME = "OpcDemoClient";
    private static Logger log = Logger.getLogger(JOpcDemoClient.class);
//    String NAMESPACE = "http://www.tmt.org/opcua/demoAddressSpace"; // OpcDemoNodeManager.NAMESPACE
    private static String NAMESPACE = OpcDemoNodeManager.NAMESPACE;

    // Listen for changes in the filter or disperser variable values after calling one of the set* OPC methods
    public interface Listener {
        void filterChanged(String value);

        void disperserChanged(String value);

        void perfTestVarChanged(int value);

        void analogArrayVarChanged(Integer[] value);

        void staticArrayVarChanged(Integer[] value);

        void onEvent(int value);
    }

    private static void printException(Exception e) {
        log.info(e.toString());
        if (e instanceof MethodCallStatusException) {
            MethodCallStatusException me = (MethodCallStatusException) e;
            final StatusCode[] results = me.getInputArgumentResults();
            if (results != null)
                for (int i = 0; i < results.length; i++) {
                    StatusCode s = results[i];
                    if (s.isBad()) {
                        log.info("Status for Input #" + i + ": " + s);
                        DiagnosticInfo d = me.getInputArgumentDiagnosticInfos()[i];
                        if (d != null)
                            log.info("  DiagnosticInfo:" + i + ": " + d);
                    }
                }
        }
        if (e.getCause() != null)
            log.info("Caused by: " + e.getCause());
    }

    private UaClient client;

    private ServerStatusListener serverStatusListener = new ServerStatusListener() {
        @Override
        public void onShutdown(UaClient uaClient, long secondsTillShutdown, LocalizedText shutdownReason) {
            log.info("Server shutdown in " + secondsTillShutdown + " seconds. Reason: " + shutdownReason.getText());
        }

        @Override
        public void onStateChange(UaClient uaClient, ServerState oldState, ServerState newState) {
            log.info("ServerState changed from " + oldState + " to " + newState);
            if (newState.equals(ServerState.Unknown))
                log.info("ServerStatusError: " + uaClient.getServerStatusError());
        }

        @Override
        public void onStatusChange(UaClient uaClient, ServerStatusDataType status) {
        }
    };

    private int sessionCount = 0;

    private SubscriptionAliveListener subscriptionAliveListener = new SubscriptionAliveListener() {

        @Override
        public void onAlive(Subscription s) {
        }

        @Override
        public void onTimeout(Subscription s) {
            log.info(String.format(
                    "%tc Subscription timeout: ID=%d lastAlive=%tc",
                    Calendar.getInstance(), s.getSubscriptionId().getValue(),
                    s.getLastAlive()));
        }
    };

    private SubscriptionNotificationListener subscriptionListener = new SubscriptionNotificationListener() {

        @Override
        public void onBufferOverflow(Subscription subscription, UnsignedInteger sequenceNumber, ExtensionObject[] notificationData) {
            log.info("*** SUBCRIPTION BUFFER OVERFLOW ***");
        }

        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem item, DataValue newValue) {
//            log.info("XXX SubscriptionNotificationListener.onDataChange");
        }

        @Override
        public void onError(Subscription subscription, Object notification,
                            Exception e) {
            log.info("XXX SubscriptionNotificationListener.onError");
            e.printStackTrace();
        }

        @Override
        public void onEvent(Subscription subscription, MonitoredEventItem item, Variant[] eventFields) {
//            DateTime t = (DateTime)eventFields[3].getValue();
//            long ms = DateTime.currentTime().getMilliSeconds() - t.getMilliSeconds();
//            log.info("Server event to client time in ms: " + ms);
            listener.onEvent(eventFields[eventFieldNames.length - 2].intValue());
        }

        @Override
        public long onMissingData(UnsignedInteger lastSequenceNumber, long sequenceNumber, long newSequenceNumber, StatusCode serviceResult) {
            log.info("Data missed: lastSequenceNumber=" + lastSequenceNumber + " newSequenceNumber=" + newSequenceNumber);
            return newSequenceNumber; // Accept the default
        }

        @Override
        public void onNotificationData(Subscription subscription, NotificationData notification) {
//            log.info("XXX SubscriptionNotificationListener.onNotificationData");
        }

        @Override
        public void onStatusChange(Subscription subscription, StatusCode oldStatus, StatusCode newStatus, DiagnosticInfo diagnosticInfo) {
            log.info("XXX SubscriptionNotificationListener.onStatusChange");
        }
    };

    private final QualifiedName[] eventFieldNames = {
            new QualifiedName("EventType"), new QualifiedName("Message"),
            new QualifiedName("SourceName"), new QualifiedName("Time"),
            new QualifiedName("Severity"), new QualifiedName("ActiveState/Id"),
            null, null
    };

//    private final MonitoredEventItemListener eventListener = new MonitoredEventItemListener() {
//        @Override
//        public void onEvent(MonitoredEventItem sender, Variant[] eventFields) {
//            listener.onEvent(eventFields[eventFieldNames.length - 2].intValue());
//        }
//    };

    private NodeId deviceNodeId = null;
    private NodeId filterNodeId = null;
    private NodeId disperserNodeId = null;
    private NodeId setFilterNodeId = null;
    private NodeId setDisperserNodeId = null;

    private NodeId perfTestVarNodeId = null;
    private NodeId perfTestNodeId = null;
    private NodeId analogArrayVarNodeId = null;
    private NodeId staticArrayVarNodeId = null;

    private final Listener listener;


    public JOpcDemoClient(String host, Listener listener)
            throws SecureIdentityException, ServerListException, IOException, SessionActivationException, URISyntaxException {
        this.listener = listener;
        client = initialize(host);

        connect();

        // XXX FIXME do this in a handler whenever connected
        int ns = client.getAddressSpace().getNamespaceTable().getIndex(NAMESPACE);
        deviceNodeId = new NodeId(ns, "OpcDemoDevice");
        filterNodeId = new NodeId(ns, "Filter");
        disperserNodeId = new NodeId(ns, "Disperser");
        setFilterNodeId = new NodeId(ns, "setFilter");
        setDisperserNodeId = new NodeId(ns, "setDisperser");

        perfTestVarNodeId = new NodeId(ns, "perfTestVar");
        perfTestNodeId = new NodeId(ns, "perfTest");
        analogArrayVarNodeId = new NodeId(ns, "Int32ArrayAnalogItem");
        staticArrayVarNodeId = new NodeId(ns, "StaticInt32Array");

        subscribe(filterNodeId, Attributes.Value);
        subscribe(disperserNodeId, Attributes.Value);
        subscribe(perfTestVarNodeId, Attributes.Value);
        subscribe(analogArrayVarNodeId, Attributes.Value);
        subscribe(staticArrayVarNodeId, Attributes.Value);

        //Subscribe to events
        subscribe(Identifiers.Server, Attributes.EventNotifier);
    }

    /**
     * Connect to the server.
     *
     * @throws ServerConnectionException
     */
    private void connect() throws ServerConnectionException {
        if (!client.isConnected())
            try {
                if (client.getProtocol() == Protocol.Https)
                    log.info("Using HttpsSecurityPolicies "
                            + Arrays.toString(client.getHttpsSettings()
                            .getHttpsSecurityPolicies()));
                else {
                    String securityPolicy = client.getEndpoint() == null ? client
                            .getSecurityMode().getSecurityPolicy()
                            .getPolicyUri()
                            : client.getEndpoint().getSecurityPolicyUri();
                    log.info("Using SecurityPolicy " + securityPolicy);
                }

                // We can define the session name that is visible in the server as well
                client.setSessionName(APP_NAME + " Session" + ++sessionCount);

                client.connect();
                try {
                    log.info("ServerStatus: " + client.getServerStatus());
                } catch (StatusException ex) {
                    printException(ex);
                }
            } catch (ServerConnectionException | ServiceException e) {
                printException(e);
            }
    }

    public void disconnect() {
        client.disconnect();
    }

    /**
     * Initializes the object, must be called after constructor
     * @param host the host where the server is running
     */
    private UaClient initialize(String host) throws URISyntaxException,
            SecureIdentityException, IOException, SessionActivationException,
            ServerListException {

        String serverUri = "opc.tcp://" + host + ":52520/OPCUA/" + OpcDemoServer.APP_NAME;
        log.info("Connecting to " + serverUri);

        // *** Create the UaClient
        UaClient uaClient = new UaClient(serverUri);

        // Use PKI files to keep track of the trusted and rejected server certificates...
        final PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator();
        uaClient.setCertificateValidator(validator);

        // *** Application Description is sent to the server
        ApplicationDescription appDescription = new ApplicationDescription();
        appDescription.setApplicationName(new LocalizedText(APP_NAME, Locale.ENGLISH));
        // 'localhost' (all lower case) in the URI is converted to the actual
        // host name of the computer in which the application is run
        appDescription.setApplicationUri("urn:localhost:UA:OpcDemoClient");
        appDescription.setProductUri("urn:prosysopc.com:UA:OpcDemoClient");
        appDescription.setApplicationType(ApplicationType.Client);

        // *** Certificates

        File privatePath = new File(validator.getBaseDir(), "private");

        // *** Application Identity
        // Define the client application identity, including the security
        // certificate
        final ApplicationIdentity identity = ApplicationIdentity
                .loadOrCreateCertificate(appDescription, "Sample Organisation",
                /* Private Key Password */"opcua",
                /* Key File Path */privatePath,
                /* CA certificate & private key */null,
				/* Key Sizes for instance certificates to create */null,
				/* Enable renewing the certificate */true);

        // Create the HTTPS certificate.
        // The HTTPS certificate must be created, if you enable HTTPS.
        String hostName = InetAddress.getLocalHost().getHostName();
        identity.setHttpsCertificate(ApplicationIdentity
                .loadOrCreateHttpsCertificate(appDescription, hostName,
                        "opcua", null, privatePath, true));

        uaClient.setApplicationIdentity(identity);

        // Define our user locale - the default is Locale.getDefault()
        uaClient.setLocale(Locale.ENGLISH);

        // Define the call timeout in milliseconds. Default is 0 - to
        // use the value of UaClient.getEndpointConfiguration() which is
        // 120000 (2 min) by default
        uaClient.setTimeout(30000);

        // StatusCheckTimeout is used to detect communication
        // problems and start automatic reconnection.
        // These are the default values:
        uaClient.setStatusCheckTimeout(10000);
        // client.setAutoReconnect(true);

        uaClient.addServerStatusListener(serverStatusListener);
        uaClient.setSecurityMode(SecurityMode.NONE);

        // Define the security policies for HTTPS; ALL is the default
        uaClient.getHttpsSettings().setHttpsSecurityPolicies(HttpsSecurityPolicy.ALL);

        // Define a custom certificate validator for the HTTPS certificates
        uaClient.getHttpsSettings().setCertificateValidator(validator);
        uaClient.setUserIdentity(new UserIdentity());

        // Set endpoint configuration parameters
        uaClient.getEndpointConfiguration().setMaxByteStringLength(Integer.MAX_VALUE);
        uaClient.getEndpointConfiguration().setMaxArrayLength(Integer.MAX_VALUE);

        return uaClient;
    }


    protected void callMethod(NodeId methodId, String value)
            throws ServiceException, ServerConnectionException,
            AddressSpaceException, MethodArgumentException, StatusException {
        Variant[] inputs = new Variant[]{new Variant(value)};
        client.call(deviceNodeId, methodId, inputs);
    }

    protected AsyncResult callMethodAsync(NodeId methodId, String value)
            throws ServiceException, ServerConnectionException, AddressSpaceException, MethodArgumentException, StatusException {
        Variant[] inputs = new Variant[]{new Variant(value)};
        return client.callAsync(new CallMethodRequest(deviceNodeId, methodId, inputs));
    }


    private void subscribe(NodeId nodeId, UnsignedInteger attributeId) {
        try {
            // Create the subscription
            Subscription subscription = createSubscription();
            // Create the monitored item
            createMonitoredItem(subscription, nodeId, attributeId);
        } catch (ServiceException | StatusException e) {
            printException(e);
        }
    }

    private Subscription createSubscription() throws ServiceException,
            StatusException {
        Subscription subscription = new Subscription();
        // Listen to the alive and timeout events of the subscription
        subscription.addAliveListener(subscriptionAliveListener);
        // Listen to notifications - the data changes and events are
        // handled using the item listeners (see below), but in many
        // occasions, it may be best to use the subscription
        // listener also to handle those notifications
        subscription.addNotificationListener(subscriptionListener);
        // Add it to the client, if it wasn't there already
        if (!client.hasSubscription(subscription.getSubscriptionId()))
            client.addSubscription(subscription);
        return subscription;
    }

    private void createMonitoredItem(Subscription sub, NodeId nodeId,
                                     UnsignedInteger attributeId) throws ServiceException, StatusException {
        // Create the monitored item, if it is not already in the subscription
        if (!sub.hasItem(nodeId, attributeId)) {
            if (Objects.equals(attributeId, Attributes.EventNotifier)) {
                MonitoredEventItem eventItem = createMonitoredEventItem(nodeId);
                sub.addItem(eventItem);
            } else {
                MonitoredDataItem dataItem = createMonitoredDataItem(nodeId, attributeId);
                dataItem.setSamplingInterval(0.0); // 0 means use fastest rate
                dataItem.setQueueSize(100);
                // Set the filter if you want to limit data changes
                dataItem.setDataChangeFilter(null);
                sub.addItem(dataItem);
            }
        }
    }

    private MonitoredDataItem createMonitoredDataItem(final NodeId nodeId, UnsignedInteger attributeId) {
        MonitoredDataItem dataItem = new MonitoredDataItem(nodeId, attributeId, MonitoringMode.Reporting);
        dataItem.setDataChangeListener(new MonitoredDataItemListener() {
            @Override
            public void onDataChange(MonitoredDataItem sender, DataValue prevValue, DataValue value) {
//                long ms = DateTime.currentTime().getMilliSeconds() - value.getSourceTimestamp().getMilliSeconds();
//                log.info("Server to client time in ms: " + ms);
                if (nodeId == filterNodeId) {
                    listener.filterChanged(value.getValue().toString());
                } else if (nodeId == disperserNodeId) {
                    listener.disperserChanged(value.getValue().toString());
                } else if (nodeId == perfTestVarNodeId) {
                    listener.perfTestVarChanged(value.getValue().intValue());
                } else if (nodeId == analogArrayVarNodeId) {
                    listener.analogArrayVarChanged((Integer[]) value.getValue().getValue());
                } else if (nodeId == staticArrayVarNodeId) {
                    listener.staticArrayVarChanged((Integer[]) value.getValue().getValue());
                }
            }
        });
        return dataItem;
    }

    private MonitoredEventItem createMonitoredEventItem(NodeId nodeId) throws StatusException {
        initEventFieldNames();
        EventFilter filter = createEventFilter(eventFieldNames);

        // Create the item
        return new MonitoredEventItem(nodeId, filter);
    }

//    private String eventFieldsToString(QualifiedName[] fieldNames, Variant[] fieldValues) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < fieldValues.length; i++) {
//            Object fieldValue = fieldValues[i] == null ? null : fieldValues[i].getValue();
//            // Find the BrowseName of the node corresponding to NodeId values
//            try {
//                UaNode node = null;
//                if (fieldValue instanceof NodeId)
//                    node = client.getAddressSpace().getNode((NodeId) fieldValue);
//                else if (fieldValue instanceof ExpandedNodeId)
//                    node = client.getAddressSpace().getNode((ExpandedNodeId) fieldValue);
//                if (node != null)
//                    fieldValue = String.format("%s {%s}", node.getBrowseName(), fieldValue);
//            } catch (Exception e) {
//                // Node not found, just use fieldValue
//            }
//            if (i < fieldNames.length) {
//                QualifiedName fieldName = fieldNames[i];
//                sb.append(fieldName.getName()).append("=").append(fieldValue).append("; ");
//            } else
//                sb.append("Node=").append(fieldValue).append("; ");
//        }
//        return sb.toString();
//    }


//    private String eventToString(NodeId nodeId, QualifiedName[] fieldNames, Variant[] fieldValues) {
//        return String.format("Received Event: Node: %s Fields: %s", nodeId, eventFieldsToString(fieldNames, fieldValues));
//    }

    private void initEventFieldNames() throws StatusException {
        if (eventFieldNames[eventFieldNames.length - 1] == null) {
            int ns = client.getNamespaceTable().getIndex(NAMESPACE);
            eventFieldNames[eventFieldNames.length - 2] = new QualifiedName(ns, OpcDemoEventType.DEMO_VARIABLE_NAME);
            eventFieldNames[eventFieldNames.length - 1] = new QualifiedName(ns, OpcDemoEventType.DEMO_PROPERTY_NAME);
        }
    }

    private QualifiedName[] createBrowsePath(QualifiedName qualifiedName) {
        if (!qualifiedName.getName().contains("/"))
            return new QualifiedName[]{qualifiedName};
        int namespaceIndex = qualifiedName.getNamespaceIndex();
        String[] names = qualifiedName.getName().split("/");
        QualifiedName[] result = new QualifiedName[names.length];
        for (int i = 0; i < names.length; i++)
            result[i] = new QualifiedName(namespaceIndex, names[i]);
        return result;
    }

    protected EventFilter createEventFilter(QualifiedName[] eventFields) {
        NodeId eventTypeId = Identifiers.BaseEventType;
        UnsignedInteger eventAttributeId = Attributes.Value;
        SimpleAttributeOperand[] selectClauses = new SimpleAttributeOperand[eventFields.length + 1];
        for (int i = 0; i < eventFields.length; i++) {
            QualifiedName[] browsePath = createBrowsePath(eventFields[i]);
            selectClauses[i] = new SimpleAttributeOperand(eventTypeId, browsePath, eventAttributeId, null);
        }
        // Add a field to get the NodeId of the event source
        selectClauses[eventFields.length] = new SimpleAttributeOperand(eventTypeId, null, Attributes.NodeId, null);
        EventFilter filter = new EventFilter();
        // Event field selection
        filter.setSelectClauses(selectClauses);

        ContentFilterBuilder fb = new ContentFilterBuilder(client.getEncoderContext());

        fb.add(FilterOperator.Not,
                new ElementOperand(UnsignedInteger.valueOf(1)));
        final LiteralOperand filteredType = new LiteralOperand(new Variant(
                Identifiers.GeneralModelChangeEventType));
        fb.add(FilterOperator.OfType, filteredType);

        // Apply the filter to Where-clause
        filter.setWhereClause(fb.getContentFilter());
        return filter;
    }

    private String read(NodeId nodeId) throws ServiceException, StatusException {
        return client.readAttribute(nodeId, Attributes.Value).getValue().toString();
    }

    public String getFilter() throws ServiceException, StatusException {
        return read(filterNodeId);
    }

    public String getDisperser() throws ServiceException, StatusException {
        return read(disperserNodeId);
    }

    public void setFilter(String filter) throws ServiceException, MethodArgumentException, StatusException, AddressSpaceException {
        callMethod(setFilterNodeId, filter);
    }

    public void setDisperser(String disperser) throws ServiceException, MethodArgumentException, StatusException, AddressSpaceException {
        callMethod(setDisperserNodeId, disperser);
    }

    /**
     * Starts a performance test, setting an OPC variable count times, with the given
     * delay in μs between settings.
     * @param count number of times to set the OPC variable
     * @param delay sleep time in μs between settings
     * @param testNo The variable to set: 1: scalar value, 2: analog array, 3: static array
     */
    public void startPerfTest(int count, int delay, int testNo)
            throws ServiceException, MethodArgumentException, StatusException, AddressSpaceException {
        Variant[] inputs = new Variant[]{new Variant(count), new Variant(delay), new Variant(testNo)};

        client.call(deviceNodeId, perfTestNodeId, inputs);
    }

//    /**
//     * Returns the current value of the perfTestVar OPC variable
//     */
//    public int getPerfTestVar() throws ServiceException, StatusException {
//        return client.readAttribute(perfTestVarNodeId, Attributes.Value).getValue().intValue();
//    }
//
//
//    public Integer[] getAnalogArrayVarValue() throws ServiceException, StatusException {
//        return (Integer[])client.readAttribute(analogArrayVarNodeId, Attributes.Value).getValue().getValue();
//    }
//
//    public Integer[] getStaticArrayVarValue() throws ServiceException, StatusException {
//        return (Integer[])client.readAttribute(staticArrayVarNodeId, Attributes.Value).getValue().getValue();
//    }
}
