#include <iostream>
#include <sstream>
#include <string>
#include <iostream>
#include <algorithm>
#include <time.h>

#include <thread>         
#include <chrono>   

#include <opc/ua/node.h>
#include <opc/ua/subscription.h>
#include <opc/ua/server/opcuaserver.h>


using namespace OpcUa;

class SubClient : public SubscriptionClient {
    void DataChange(uint32_t handle, const Node &node, const Variant &val, AttributeID attr) const override {
        std::cout << "Received DataChange event for Node " << node << std::endl;
    }
};

void createMethodNode(Node node);

void addPerfTest(uint16_t ns, Node node);


// optional args: (Note: more args than the java version, since OPC methods are not implemented yet)
//
// host - hostname to bind to (default: localhost)
// port - port to listen on: default (localhost 52520)
// count - number of times to set the OPC variable or fire the event (default: 100000))
// delay - sleep time in microsec between settings or events (default: 100)
// testNo: 0 to 4 to indicate the performance test to run (default: 1, scalar var):
//      - 0: send events,
//      - 1: set a scalar variable,
//      - 2: set an analog array value,
//      - 3: set a static array value
// eventSize - size of the event payload (default: 512 bytes)
int main(int argc, const char **argv) {
    const bool debug = false;
    OpcUa::OPCUAServer server(debug);

    const char *host = (argc > 1) ? argv[1] : "localhost";
    int port = (argc > 2) ? atoi(argv[2]) : 52520;
    int count = (argc > 3) ? atoi(argv[3]) : 1000000;
    int delay = (argc > 4) ? atoi(argv[4]) : 10;
    int testNo = (argc > 5) ? atoi(argv[5]) : 1;
    int eventSize = (argc > 6) ? atoi(argv[6]) : 512;

    std::ostringstream uri;
    uri << "opc.tcp://" << host << ":" << port << "/OPCUA/SampleConsoleServer";
    server.SetEndpoint(uri.str());
    std::cout << "Server endpoint is: " << uri.str() << std::endl;

//    server.SetURI("http://www.tmt.org/opcua/demoAddressSpace"); // XXX how to set namespace URI?

    server.Start();
    Node root = server.GetRootNode();
    std::cout << "Root node is: " << root << std::endl;
    std::cout << "Childs are: " << std::endl;


    Node objects = server.GetObjectsNode();
    uint16_t ns = 2;

    // Device
    NodeID deviceId("OpcDemoDevice", ns);
    Node device = objects.AddObject(deviceId, QualifiedName("OpcDemoDevice", ns));

    // OPC UA Variables to hold the current filter and disperser values
    Node filter = device.AddVariable(NodeID("Filter", ns), QualifiedName("Filter", ns), Variant(std::string("None")));
    Node disperser = device.AddVariable(NodeID("Disperser", ns), QualifiedName("Disperser", ns), Variant(std::string("Mirror")));

    // OPC UA methods to set the filter and disperser
    createMethodNode(filter);
    createMethodNode(disperser);

    // Add a plain int test var
    Node perfTestVar = device.AddVariable(NodeID("perfTestVar", ns), QualifiedName("perfTestVar", ns), Variant(0));

    // Add analog and static array vars
    std::vector<int32_t> ar(100,0); // 100 ints with value 0
    Node staticArrayNode = device.AddVariable(NodeID("StaticInt32Array", ns), QualifiedName("StaticInt32Array", ns), Variant(ar));


    //Uncomment following to subscribe to datachange events inside server
    /*
    SubClient clt;
    std::unique_ptr<Subscription> sub = server.CreateSubscription(100, clt);
    sub->SubscribeDataChange(myvar);
    */

    //Create event
    server.EnableEventNotification();
    Event ev(ObjectID::BaseEventType); //you should create your own type
    ev.Severity = 2;
    ev.SourceNode = ObjectID::Server;
    ev.SourceName = "Event from FreeOpcUA";
    ev.Time = CurrentDateTime();
    std::string msg(eventSize, 'a');
    ev.Message = LocalizedText(msg);

    std::cout << "Ctrl-C to exit" << std::endl;

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wmissing-noreturn"
    for (int i = 0; true || i < count; i++) {
        switch (testNo) {
            case 0:
                // send events
//                std::stringstream ss;
//                ss << "This is event number: " << counter;
//                ev.Message = LocalizedText(ss.str());
                server.TriggerEvent(ev);
                break;
            case 1:
                // set a scalar variable
                perfTestVar.SetValue(Variant(i)); //will change value and trigger datachange event
                break;
            default:
                // array tests
                ar[0] = i;
                staticArrayNode.SetValue(Variant(ar));
                break;
        }

        std::this_thread::sleep_for(std::chrono::microseconds(delay));
    }
#pragma clang diagnostic pop

    server.Stop();

    return 0;
}

// void addPerfTest(UaNode folderNode, UaNode methodFolder, int eventSize, int delay)
void addPerfTest(uint16_t ns, Node folderNode, int eventSize, int delay) {
//
//    // Add a plain int test var
//    Node perfTestVar = folderNode.AddVariable(NodeID("perfTestVar", ns), QualifiedName("perfTestVar", ns), Variant(-1));
//
//    // Add analog and static array vars
//    int *ar = new int[10000];
//    Node staticArrayNode = folderNode.AddVariable(NodeID("StaticInt32Array", ns), QualifiedName("StaticInt32Array", ns), Variant(ar));
//
//    // Add method that starts the timer (XXX methods are not implemented yet in freeopcua!)
//    createPerfTestMethodNode(perfTestVar, analogArrayNode, staticArrayNode, methodFolder, eventSize);
}

void createMethodNode(Node variableNode) {
    // XXX methods are not implemented yet in freeopcua!
}

