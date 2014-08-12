package csw.opc.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.UserTokenPolicy;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.SecurityMode;
import org.opcfoundation.ua.utils.EndpointUtil;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.CertificateValidationListener;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.server.NodeBuilderException;
import com.prosysopc.ua.server.NodeManagerListener;
import com.prosysopc.ua.server.UaInstantiationException;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.UaServerException;
import com.prosysopc.ua.server.UserValidator;
import com.prosysopc.ua.types.opcua.server.BuildInfoTypeNode;

/**
 * A sample OPC UA server application.
 */
public class OpcDemoServer {

    private static Logger logger = Logger.getLogger(OpcDemoServer.class);
    
//    public static final String APP_NAME = "OpcDemoServer";
    public static final String APP_NAME = "SampleConsoleServer";

    public static void main(String[] args) throws Exception {
        // Initialize log4j logging
        PropertyConfigurator.configureAndWatch(OpcDemoServer.class.getResource("/log.properties").getFile(), 5000);

        // *** Initialization and Start Up
        OpcDemoServer opcDemoServer = new OpcDemoServer();

        // Initialize the server
        opcDemoServer.initialize(52520, 52443, APP_NAME);

        // Create the address space
        opcDemoServer.createAddressSpace();

        opcDemoServer.run(false);
    }

    protected OpcDemoNodeManager opcDemoNodeManager;
    protected NodeManagerListener myNodeManagerListener = new OpcDemoNodeManagerListener();
    protected UaServer server;
    protected final UserValidator userValidator = new OpcDemoUserValidator();

    protected final CertificateValidationListener validationListener = new OpcDemoCertificateValidationListener();

    protected void createAddressSpace() throws StatusException,
            UaInstantiationException, NodeBuilderException {

        // My Node Manager
        opcDemoNodeManager = new OpcDemoNodeManager(server, OpcDemoNodeManager.NAMESPACE);

        opcDemoNodeManager.addListener(myNodeManagerListener);

        // My I/O Manager Listener
        opcDemoNodeManager.getIoManager().addListeners(new OpcDemoIoManagerListener());

        logger.info("Address space created.");
    }

    protected void initBuildInfo() {
        // Initialize BuildInfo - using the version info from the SDK
        // XXX TODO You should replace this with your own build information

        final BuildInfoTypeNode buildInfo = server.getNodeManagerRoot()
                .getServerData().getServerStatusNode().getBuildInfoNode();

        // Fetch version information from the package manifest
        final Package sdkPackage = UaServer.class.getPackage();
        final String implementationVersion = sdkPackage
                .getImplementationVersion();
        if (implementationVersion != null) {
            int splitIndex = implementationVersion.lastIndexOf(".");
            final String softwareVersion = implementationVersion.substring(0,
                    splitIndex);
            String buildNumber = implementationVersion
                    .substring(splitIndex + 1);

            buildInfo.setManufacturerName(sdkPackage.getImplementationVendor());
            buildInfo.setSoftwareVersion(softwareVersion);
            buildInfo.setBuildNumber(buildNumber);

        }

        final URL classFile = UaServer.class.getResource("/com/prosysopc/ua/samples/server/OpcDemoServer.class");
        if (classFile != null) {
            final File mfFile = new File(classFile.getFile());
            GregorianCalendar c = new GregorianCalendar();
            c.setTimeInMillis(mfFile.lastModified());
            buildInfo.setBuildDate(new DateTime(c));
        }
    }

    protected void initialize(int port, int httpsPort, String applicationName)
            throws SecureIdentityException, IOException, UaServerException {

        // *** Create the server
        server = new UaServer();

        // Use PKI files to keep track of the trusted and rejected client
        // certificates...
        final PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator(System.getProperty("user.home"));
        server.setCertificateValidator(validator);
        // ...and react to validation results with a custom handler
        validator.setValidationListener(validationListener);

        // *** Application Description is sent to the clients
        ApplicationDescription appDescription = new ApplicationDescription();
        appDescription.setApplicationName(new LocalizedText(applicationName,
                Locale.ENGLISH));
        // 'localhost' (all lower case) in the URI is converted to the actual
        // host name of the computer in which the application is run
        appDescription.setApplicationUri("urn:localhost:OPCUA:"
                + applicationName);
        appDescription.setProductUri("urn:prosysopc.com:OPCUA:"
                + applicationName);

        // *** Server Endpoints
        // TCP Port number for the UA Binary protocol
        server.setPort(Protocol.OpcTcp, port);
        // TCP Port for the HTTPS protocol
        server.setPort(Protocol.Https, httpsPort);

        // optional server name part of the URI (default for all protocols)
        server.setServerName("OPCUA/" + applicationName);

        server.setBindAddresses(EndpointUtil.getInetAddresses());

        // *** Certificates

        File privatePath = new File(validator.getBaseDir(), "private");

        KeyPair issuerCertificate = ApplicationIdentity
                .loadOrCreateIssuerCertificate("ProsysSampleCA", privatePath,
                        "opcua", 3650, false);

        // *** Application Identity

        // Define the Server application identity, including the Application
        // Instance Certificate (but don't sign it with the issuerCertificate as
        // explained above).
        final ApplicationIdentity identity = ApplicationIdentity
                .loadOrCreateCertificate(appDescription, "Sample Organisation",
                        /* Private Key Password */"opcua",
						/* Key File Path */privatePath,
						/* Issuer Certificate & Private Key */null,
						/* Key Sizes for instance certificates to create */null,
						/* Enable renewing the certificate */true);

        // Create the HTTPS certificate bound to the hostname.
        // The HTTPS certificate must be created, if you enable HTTPS.
        String hostName = ApplicationIdentity.getActualHostName();
        identity.setHttpsCertificate(ApplicationIdentity
                .loadOrCreateHttpsCertificate(appDescription, hostName,
                        "opcua", issuerCertificate, privatePath, true));

        server.setApplicationIdentity(identity);

        // *** Security settings
        // Define the security modes to support for the Binary protocol -
        // ALL is the default
        server.setSecurityModes(SecurityMode.ALL);
        // The TLS security policies to use for HTTPS
        server.getHttpsSettings().setHttpsSecurityPolicies(
                HttpsSecurityPolicy.ALL);

        // Define a custom certificate validator for the HTTPS certificates
        server.getHttpsSettings().setCertificateValidator(validator);

        // Define the supported user Token policies
        server.addUserTokenPolicy(UserTokenPolicy.ANONYMOUS);
        server.addUserTokenPolicy(UserTokenPolicy.SECURE_USERNAME_PASSWORD);
        server.addUserTokenPolicy(UserTokenPolicy.SECURE_CERTIFICATE);
        // Define a validator for checking the user accounts
        server.setUserValidator(userValidator);

        // *** init() creates the service handlers and the default endpoints
        // according to the above settings
        server.init();

        initBuildInfo();

        // "Safety limits" for ill-behaving clients
        server.getSessionManager().setMaxSessionCount(500);
        server.getSessionManager().setMaxSessionTimeout(3600000); // one hour
        server.getSubscriptionManager().setMaxSubscriptionCount(500);

        // You can do your own additions to server initializations here
    }


    /**
     * Run the server.
     *
     * @throws UaServerException
     * @throws StatusException
     */
    protected void run(boolean enableSessionDiagnostics)
            throws UaServerException, StatusException {
        server.start();
        if (enableSessionDiagnostics)
            server.getNodeManagerRoot().getServerData()
                    .getServerDiagnosticsNode().setEnabled(true);
    }
}
