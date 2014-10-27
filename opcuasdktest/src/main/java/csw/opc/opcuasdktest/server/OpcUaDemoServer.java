/*
 * Copyright 2014 Inductive Automation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csw.opc.opcuasdktest.server;

import com.inductiveautomation.opcua.sdk.server.OpcUaServer;
import com.inductiveautomation.opcua.sdk.server.api.OpcUaServerConfig;
import com.inductiveautomation.opcua.stack.core.UaException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class OpcUaDemoServer {

    public static void main(String[] args) throws Exception {
        int eventSize = 256;
        if (args.length > 0) eventSize = Integer.valueOf(args[0]);

        int delay = 100; // delay in microseconds (should be same as client arg for performance test)
        if (args.length > 1) delay = Integer.valueOf(args[1]);

        OpcUaDemoServer server = new OpcUaDemoServer(new OpcUaDemoServerConfig(), eventSize, delay);

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
