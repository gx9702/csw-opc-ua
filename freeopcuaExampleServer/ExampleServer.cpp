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

#include "ExampleServer.h"
#include "SubClient.h"

using namespace OpcUa;

void ExampleServer::start() {
    std::ostringstream uri;
    uri << "opc.tcp://" << host << ":" << port << "/OPCUA/SampleConsoleServer";
    server.SetEndpoint(uri.str());
    std::cout << "Server endpoint is: " << uri.str() << std::endl;

//    server.SetURI("http://www.tmt.org/opcua/demoAddressSpace"); // XXX how to set namespace URI?

    server.Start();
    createNamespace();
}

Event ExampleServer::createEvent() {
    Event ev(ObjectID::BaseEventType); // XXX TODO should create own event type
    ev.Severity = 2;
    ev.SourceNode = ObjectID::Server;
    ev.SourceName = "Event from FreeOpcUA";
    ev.Time = CurrentDateTime();
    std::string msg(eventSize, 'a');
    ev.Message = LocalizedText(msg);
    return ev;

}

void ExampleServer::startPerfTest(Event ev, Node perfTestVar, Node staticArrayNode, std::vector<int32_t> ar) {
    for (int i = 0; true /*|| i < count*/; i++) { // XXX loop forever, since we can't start this via a method
        switch (testNo) {
            case 0:
                // send events
                server.TriggerEvent(ev);
                break;
            case 1:
                // set a scalar variable
                perfTestVar.SetValue(Variant(i)); // will change value and trigger datachange event
                break;
            default:
                // array tests
                ar[0] = i;
                staticArrayNode.SetValue(Variant(ar));
                break;
        }
        std::this_thread::sleep_for(std::chrono::microseconds(delay));
    }
}


void ExampleServer::createNamespace() {
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

//    // OPC UA methods to set the filter and disperser
//    createMethodNode(filter);
//    createMethodNode(disperser);

    // Add a plain int test var
    Node perfTestVar = device.AddVariable(NodeID("perfTestVar", ns), QualifiedName("perfTestVar", ns), Variant(0));

    // Add analog and static array vars
    std::vector<int32_t> ar(100,0); // 100 ints with value 0
    Node staticArrayNode = device.AddVariable(NodeID("StaticInt32Array", ns), QualifiedName("StaticInt32Array", ns), Variant(ar));

    // subscribe to datachange events inside server
    SubClient clt;
    std::unique_ptr<Subscription> sub = server.CreateSubscription(100, clt);
    sub->SubscribeDataChange(filter);
    sub->SubscribeDataChange(disperser);

    //Create event
    server.EnableEventNotification();
    Event ev = createEvent();

    std::cout << "Ctrl-C to exit" << std::endl;
    startPerfTest(ev, perfTestVar, staticArrayNode, ar);

//    server.Stop();
}

//void addPerfTest(uint16_t ns, Node folderNode, int eventSize, int delay) {
////
////    // Add a plain int test var
////    Node perfTestVar = folderNode.AddVariable(NodeID("perfTestVar", ns), QualifiedName("perfTestVar", ns), Variant(-1));
////
////    // Add analog and static array vars
////    int *ar = new int[10000];
////    Node staticArrayNode = folderNode.AddVariable(NodeID("StaticInt32Array", ns), QualifiedName("StaticInt32Array", ns), Variant(ar));
////
////    // Add method that starts the timer (XXX methods are not implemented yet in freeopcua!)
////    createPerfTestMethodNode(perfTestVar, analogArrayNode, staticArrayNode, methodFolder, eventSize);
//}
//
//void createMethodNode(Node variableNode) {
//    // XXX methods are not implemented yet in freeopcua!
//}

