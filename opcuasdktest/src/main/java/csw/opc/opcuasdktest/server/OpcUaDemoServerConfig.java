package csw.opc.opcuasdktest.server;

import java.io.File;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.inductiveautomation.opcua.sdk.server.api.OpcUaServerConfig;
import com.inductiveautomation.opcua.sdk.server.identity.IdentityValidator;
import com.inductiveautomation.opcua.sdk.server.identity.UsernameIdentityValidator;
import com.inductiveautomation.opcua.stack.core.application.CertificateManager;
import com.inductiveautomation.opcua.stack.core.application.DirectoryCertificateManager;
import com.inductiveautomation.opcua.stack.core.security.SecurityPolicy;
import com.inductiveautomation.opcua.stack.core.types.builtin.DateTime;
import com.inductiveautomation.opcua.stack.core.types.builtin.LocalizedText;
import com.inductiveautomation.opcua.stack.core.types.structured.BuildInfo;
import com.inductiveautomation.opcua.stack.core.types.structured.UserTokenPolicy;
import org.slf4j.LoggerFactory;

public class OpcUaDemoServerConfig implements OpcUaServerConfig {

    private static final BuildInfo BUILD_INFO = new BuildInfo(
            "http://www.tmt.org/csw-opc-ua",
            "TMT", "CSW-OPC-UA",
            "dev", "dev", DateTime.now()
    );

    private static final String SERVER_ALIAS = "csw-opc-ua-demo-server";
    private static final char[] PASSWORD = "test".toCharArray();

    private volatile CertificateManager certificateManager;
    private volatile X509Certificate certificate;
    private volatile KeyPair keyPair;

    private String host;
    private int port;

    public OpcUaDemoServerConfig(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            keyStore.load(getClass().getClassLoader().getResourceAsStream("ctt-server-keystore.pfx"), PASSWORD);

            Key serverPrivateKey = keyStore.getKey(SERVER_ALIAS, PASSWORD);

            if (serverPrivateKey instanceof PrivateKey) {
                certificate = (X509Certificate) keyStore.getCertificate(SERVER_ALIAS);
                PublicKey serverPublicKey = certificate.getPublicKey();
                keyPair = new KeyPair(serverPublicKey, (PrivateKey) serverPrivateKey);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error loading server certificate: {}.", e.getMessage(), e);

            certificate = null;
            keyPair = null;
        }

        certificateManager = new DirectoryCertificateManager(keyPair, certificate, new File("./security"));
    }

    @Override
    public int getBindPort() { return port; }

    @Override
    public List<String> getBindAddresses() {
        return Lists.newArrayList(host);
    }

    @Override
    public LocalizedText getApplicationName() {
        return LocalizedText.english("OPC-UA Demo Server");
    }

    @Override
    public String getApplicationUri() {
        return "urn:tmt:opc-demo-server";
    }

    @Override
    public String getServerName() {
        return "OPCUA/SampleConsoleServer";
    }

    @Override
    public String getProductUri() {
        return "http://www.tmt.org/csw-opc-ua/demo";
    }

    @Override
    public CertificateManager getCertificateManager() {
        return certificateManager;
    }

    @Override
    public EnumSet<SecurityPolicy> getSecurityPolicies() {
        return EnumSet.of(SecurityPolicy.None, SecurityPolicy.Basic128Rsa15, SecurityPolicy.Basic256);
    }

    @Override
    public List<UserTokenPolicy> getUserTokenPolicies() {
        return Lists.newArrayList(USER_TOKEN_POLICY_ANONYMOUS, USER_TOKEN_POLICY_USERNAME);
    }

    @Override
    public IdentityValidator getIdentityValidator() {
        return new UsernameIdentityValidator(true, challenge -> {
            String username = challenge.getUsername();
            String password = challenge.getPassword();

            return ("user1".equals(username) && "password1".equals(password)) ||
                    ("user2".equals(username) && "password2".equals(password));
        });
    }

    @Override
    public BuildInfo getBuildInfo() {
        return BUILD_INFO;
    }
}
