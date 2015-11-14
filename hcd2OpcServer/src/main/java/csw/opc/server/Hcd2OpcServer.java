package csw.opc.server;


import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.digitalpetri.opcua.sdk.server.OpcUaServer;
import com.digitalpetri.opcua.sdk.server.api.config.OpcUaServerConfig;
import com.digitalpetri.opcua.sdk.server.identity.UsernameIdentityValidator;
import com.digitalpetri.opcua.stack.core.Stack;
import com.digitalpetri.opcua.stack.core.application.CertificateManager;
import com.digitalpetri.opcua.stack.core.application.CertificateValidator;
import com.digitalpetri.opcua.stack.core.application.DefaultCertificateManager;
import com.digitalpetri.opcua.stack.core.application.DefaultCertificateValidator;
import com.digitalpetri.opcua.stack.core.types.builtin.LocalizedText;
import com.digitalpetri.opcua.stack.core.types.structured.UserTokenPolicy;

import static com.google.common.collect.Lists.newArrayList;

public class Hcd2OpcServer {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Hcd2OpcServer server = new Hcd2OpcServer();
        server.startup();

        server.shutdownFuture().get();
    }

    private final OpcUaServer server;

    public Hcd2OpcServer() {
        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
                true, // allow anonymous access
                authenticationChallenge ->
                        "user".equals(authenticationChallenge.getUsername()) &&
                                "password".equals(authenticationChallenge.getPassword())
        );

        List<UserTokenPolicy> userTokenPolicies = newArrayList(
                OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
                OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME
        );

        CertificateManager certificateManager = new DefaultCertificateManager();
        CertificateValidator certificateValidator = new DefaultCertificateValidator(new File("./security"));

        OpcUaServerConfig config = OpcUaServerConfig.builder()
                .setApplicationName(LocalizedText.english("HCD2 opc-ua server"))
                .setApplicationUri("urn:hcd2:opcua:server")
                .setCertificateManager(certificateManager)
                .setCertificateValidator(certificateValidator)
                .setIdentityValidator(identityValidator)
                .setUserTokenPolicies(userTokenPolicies)
                .setProductUri("urn:hcd2:opcua:sdk")
                .setServerName("")
                .build();

        server = new OpcUaServer(config);

        // register a CttNamespace so we have some nodes to play with
        server.getNamespaceManager().registerAndAdd(
                Hcd2Namespace.NAMESPACE_URI,
                idx -> new Hcd2Namespace(server, idx));

//        // XXX
//        Namespace ns = server.getNamespaceManager().getNamespace(2);
    }

    public void startup() {
        server.startup();
    }

    private CompletableFuture<Void> shutdownFuture() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            Stack.releaseSharedResources();
            future.complete(null);
        }));

        return future;
    }

}
