package csw.opc.client;

import com.prosysopc.ua.*;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.client.*;
import com.prosysopc.ua.nodes.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.opcfoundation.ua.builtintypes.*;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.*;
import org.opcfoundation.ua.transport.AsyncResult;
import org.opcfoundation.ua.transport.ResultListener;
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
                        DiagnosticInfo d = me
                                .getInputArgumentDiagnosticInfos()[i];
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
            System.out.printf("Server shutdown in %d seconds. Reason: %s\n",
                    secondsTillShutdown, shutdownReason.getText());
        }

        @Override
        public void onStateChange(UaClient uaClient, ServerState oldState, ServerState newState) {
            System.out.printf("ServerState changed from %s to %s\n", oldState, newState);
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
            log.info(String.format(
                    "%tc Subscription alive: ID=%d lastAlive=%tc",
                    Calendar.getInstance(), s.getSubscriptionId().getValue(),
                    s.getLastAlive()));
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
        }

        @Override
        public void onError(Subscription subscription, Object notification,
                            Exception e) {
            e.printStackTrace();
        }

        @Override
        public void onEvent(Subscription subscription, MonitoredEventItem item, Variant[] eventFields) {
        }

        @Override
        public long onMissingData(UnsignedInteger lastSequenceNumber, long sequenceNumber, long newSequenceNumber, StatusCode serviceResult) {
            log.info("Data missed: lastSequenceNumber=" + lastSequenceNumber + " newSequenceNumber=" + newSequenceNumber);
            return newSequenceNumber; // Accept the default
        }

        @Override
        public void onNotificationData(Subscription subscription, NotificationData notification) {
        }

        @Override
        public void onStatusChange(Subscription subscription, StatusCode oldStatus, StatusCode newStatus, DiagnosticInfo diagnosticInfo) {
        }
    };

    private NodeId deviceNodeId = null;
    private NodeId filterNodeId = null;
    private NodeId disperserNodeId = null;
    private NodeId setFilterNodeId = null;
    private NodeId setDisperserNodeId = null;

    public JOpcDemoClient() {
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

    /**
     * Initializes the object, must be called after constructor
     */
    private void initialize() throws URISyntaxException,
            SecureIdentityException, IOException, SessionActivationException,
            ServerListException {

        String serverUri = "opc.tcp://localhost:52520/OPCUA/OpcDemoServer";
        log.info("Connecting to " + serverUri);

        // *** Create the UaClient
        client = new UaClient(serverUri);

        // Use PKI files to keep track of the trusted and rejected server certificates...
        final PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator();
        client.setCertificateValidator(validator);

        // *** Application Description is sent to the server
        ApplicationDescription appDescription = new ApplicationDescription();
        appDescription.setApplicationName(new LocalizedText(APP_NAME, Locale.ENGLISH));
        // 'localhost' (all lower case) in the URI is converted to the actual
        // host name of the computer in which the application is run
        appDescription
                .setApplicationUri("urn:localhost:UA:OpcDemoClient");
        appDescription
                .setProductUri("urn:prosysopc.com:UA:OpcDemoClient");
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

        client.setApplicationIdentity(identity);

        // Define our user locale - the default is Locale.getDefault()
        client.setLocale(Locale.ENGLISH);

        // Define the call timeout in milliseconds. Default is 0 - to
        // use the value of UaClient.getEndpointConfiguration() which is
        // 120000 (2 min) by default
        client.setTimeout(30000);

        // StatusCheckTimeout is used to detect communication
        // problems and start automatic reconnection.
        // These are the default values:
        client.setStatusCheckTimeout(10000);
        // client.setAutoReconnect(true);

        client.addServerStatusListener(serverStatusListener);
        client.setSecurityMode(SecurityMode.NONE);

        // Define the security policies for HTTPS; ALL is the default
        client.getHttpsSettings().setHttpsSecurityPolicies(
                HttpsSecurityPolicy.ALL);

        // Define a custom certificate validator for the HTTPS certificates
        client.getHttpsSettings().setCertificateValidator(validator);
        client.setUserIdentity(new UserIdentity());

        // Set endpoint configuration parameters
        client.getEndpointConfiguration().setMaxByteStringLength(Integer.MAX_VALUE);
        client.getEndpointConfiguration().setMaxArrayLength(Integer.MAX_VALUE);

        connect();

        String namespace = "http://www.tmt.org/opcua/demoAddressSpace"; // OpcDemoNodeManager.NAMESPACE
        int ns = client.getAddressSpace().getNamespaceTable().getIndex(namespace);
        deviceNodeId = new NodeId(ns, "OpcDemoDevice");
        filterNodeId = new NodeId(ns, "Filter");
        disperserNodeId = new NodeId(ns, "Disperser");
        setFilterNodeId = new NodeId(ns, "setFilter");
        setDisperserNodeId = new NodeId(ns, "setDisperser");

        subscribe(filterNodeId);
        subscribe(disperserNodeId);
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


    private void subscribe(NodeId nodeId) {
        log.info("*** Subscribing to node: " + nodeId);
        try {
            // Create the subscription
            Subscription subscription = createSubscription();
            // Create the monitored item
            createMonitoredItem(subscription, nodeId, Attributes.Value);
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
        UnsignedInteger monitoredItemId = null;
        // Create the monitored item, if it is not already in the subscription
        if (!sub.hasItem(nodeId, attributeId)) {
            MonitoredDataItem dataItem = createMonitoredDataItem(nodeId, attributeId);
            // Set the filter if you want to limit data changes
            dataItem.setDataChangeFilter(null);
            sub.addItem(dataItem);
            monitoredItemId = dataItem.getMonitoredItemId();
        }
        log.info("Subscription: Id=" + sub.getSubscriptionId() + " ItemId=" + monitoredItemId);
    }

    private MonitoredDataItem createMonitoredDataItem(final NodeId nodeId, UnsignedInteger attributeId) {
        MonitoredDataItem dataItem = new MonitoredDataItem(nodeId, attributeId, MonitoringMode.Reporting);
        dataItem.setDataChangeListener(new MonitoredDataItemListener() {
            @Override
            public void onDataChange(MonitoredDataItem sender, DataValue prevValue, DataValue value) {
                String s = (nodeId == filterNodeId) ? "filter" : "disperser";
                log.info("onDataChange " + s + " changed to " + value.getValue());
            }
        });
        return dataItem;
    }

    private String read(NodeId nodeId) throws ServiceException, StatusException {
        UnsignedInteger attributeId = Attributes.Value;
        DataValue value = client.readAttribute(nodeId, attributeId);
        return value.getValue().toString();
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

    public AsyncResult setFilterAsync(String filter) throws ServiceException, MethodArgumentException, StatusException, AddressSpaceException {
        return callMethodAsync(setFilterNodeId, filter);
    }

    public AsyncResult setDisperserAsync(String disperser) throws ServiceException, MethodArgumentException, StatusException, AddressSpaceException {
        return callMethodAsync(setDisperserNodeId, disperser);
    }

    // Demo main
    public static void demoMain(String[] args) throws Exception {
        // Load Log4j configurations from external file
        PropertyConfigurator.configureAndWatch(JOpcDemoClient.class.getResource("/log.properties").getFile(), 5000);
        final JOpcDemoClient client = new JOpcDemoClient();
        client.initialize();

        client.setFilter("NewFilter");
        client.setDisperser("NewDisperser");

        AsyncResult result = client.setFilterAsync("NewFilter2");
        result.setListener(new ResultListener() {
            @Override
            public void onCompleted(Object o) {
                try {
                    log.info("filter changed (async) to: " + client.getFilter());
                } catch (ServiceException | StatusException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ServiceResultException e) {
                log.error("Error setting filter (async)", e);
            }
        });
    }
}
