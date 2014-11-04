#include <opc/ua/client/client.h>

#include "ExampleClient.h"
#include "SubClient.h"

using namespace OpcUa;

void ExampleClient::start() {
    try {
        std::ostringstream uri;
        uri << "opc.tcp://" << host << ":" << port << "/OPCUA/SampleConsoleServer";
        std::string endpoint = uri.str();

        std::cout << "Connecting to: " << endpoint << std::endl;
        OpcUa::RemoteClient client(endpoint);
        client.Connect();

        OpcUa::Node root = client.GetRootNode();
        std::cout << "Root node is: " << root << std::endl;
        std::vector<std::string> path({"Objects", "Server"});
        OpcUa::Node server = root.GetChild(path);
        std::cout << "Server node obtained by path: " << server << std::endl;

        std::cout << "Child of objects node are: " << std::endl;
        for (OpcUa::Node node : client.GetObjectsNode().GetChildren())
            std::cout << "    " << node << std::endl;

        // perfTestVar
        std::vector<std::string> perfTestVarPath({"Objects", "2:OpcDemoDevice", "2:perfTestVar"});
        OpcUa::Node perfTestVar = root.GetChild(perfTestVarPath);
        std::cout << "got perfTestVar node: " << perfTestVar << std::endl;

        // StaticInt32Array
        std::vector<std::string> staticArrayVarPath({"Objects", "2:OpcDemoDevice", "2:StaticInt32Array"});
        OpcUa::Node staticArrayVar = root.GetChild(staticArrayVarPath);
        std::cout << "got staticArrayVar node: " << staticArrayVar << std::endl;

        // Subscribe to changes in OPC variables and OPC events
        SubClient sclt(testNo);
        std::unique_ptr<Subscription> sub = client.CreateSubscription(1, sclt);
        sub->SubscribeDataChange(perfTestVar);
        sub->SubscribeDataChange(staticArrayVar);
        sub->SubscribeEvents(); // XXX TODO FIXME: Add filter for events

        sleep(10000000); // or loop forever
        std::cout << "Disconnecting" << std::endl;
    }
    catch (const std::exception &exc) {
        std::cout << exc.what() << std::endl;
    }
    catch (...) {
        std::cout << "Unknown error." << std::endl;
    }

}