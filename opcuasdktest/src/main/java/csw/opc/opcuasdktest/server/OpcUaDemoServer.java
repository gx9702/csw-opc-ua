
package csw.opc.opcuasdktest.server;

import com.inductiveautomation.opcua.sdk.server.OpcUaServer;
import com.inductiveautomation.opcua.sdk.server.api.OpcUaServerConfig;
import com.inductiveautomation.opcua.stack.core.UaException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class OpcUaDemoServer {

    private static void usage() {
        System.out.println("Usage: Optional args: -host host -port port -delay delayInMicroSecs -eventSize eventSizeInBytes");
    }

    public static void main(String[] args) throws Exception {
        if (args.length % 2 != 0) usage();

        String host = "localhost";
        int port = 52520;
        int eventSize = 256;
        int delay = 100; // delay in microseconds (should be same as client arg for performance test)
        for(int i = 0; i < args.length; i += 2) {
            String option = args[i];
            String value = args[i+1];
            switch (option) {
                case "-host":
                    host = value;
                    break;
                case "-port":
                    port = Integer.valueOf(value);
                    break;
                case "-delay":
                    delay = Integer.valueOf(value);
                    break;
                case "-eventSize":
                    eventSize = Integer.valueOf(value);
                    break;
            }
        }

        OpcUaDemoServer server = new OpcUaDemoServer(new OpcUaDemoServerConfig(host, port), eventSize, delay);

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        server.startup();
        server.shutdownFuture().get();
    }

    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();

    private final OpcUaServer server;

    public OpcUaDemoServer(OpcUaServerConfig config, int eventSize, int delay) {
        server = new OpcUaServer(config);

        server.getNamespaceManager().getNamespaceTable().putUri(
                OpcUaDemoNamespace.NamespaceUri,
                OpcUaDemoNamespace.NamespaceIndex
        );

        server.getNamespaceManager().addNamespace(new OpcUaDemoNamespace(server, eventSize, delay));
    }

    public void startup() throws UaException {
        server.startup();
    }

    public void shutdown() {
        server.shutdown();

        shutdownFuture.complete(null);
    }

    public Future<Void> shutdownFuture() {
        return shutdownFuture;
    }

}
