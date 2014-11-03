/// @author Alexander Rykovanov 2013
/// @email rykovanov.as@gmail.com
/// @brief Remote Computer implementaion.
/// @license GNU GPL
///
/// Distributed under the GNU GPL License
/// (See accompanying file LICENSE or copy at
/// http://www.gnu.org/licenses/gpl.html)
///



#include <opc/ua/client/client.h>
#include <opc/ua/node.h>
#include <opc/ua/subscription.h>
#include <opc/ua/event.h>

#include <iostream>
#include <stdexcept>
#include <unistd.h>


using namespace OpcUa;

static int receivedVarUpdates = 0;

class SubClient : public SubscriptionClient
{
private:
    int testNo = 1;
    DateTime startTime= OpcUa::CurrentDateTime();
public:
    virtual void Event(uint32_t handle, OpcUa::Event const &event) const override {
        receivedUpdate();
    }

    void DataChange(uint32_t handle, const Node& node, const Variant& val, AttributeID attr) const override
    {
//        receivedVarUpdates++;
////        int value = val.As<int>();
////        std::cout << "Received DataChange event, value of Node " << node << " is now: " << value << std::endl;
//        if (receivedVarUpdates % 1000 == 0) {
//            logResults(receivedVarUpdates, testNo);
//        }
        receivedUpdate();
    }

    void receivedUpdate() const {
        receivedVarUpdates++;
        if (receivedVarUpdates % 1000 == 0) {
            logResults(receivedVarUpdates, testNo);
        }
    }

    // Log results of performance test
    void logResults(int count, int testNo) const {
        double secs = OpcUa::ToTimeT(OpcUa::CurrentDateTime()) - OpcUa::ToTimeT(startTime);
        double rate = count / secs;
        std::cout << "Received " << count << " updates in " << secs << " seconds, rate = " << rate << "/sec" << std::endl;
    }

public:
    SubClient(int testNo) :testNo(testNo) {
    }
};


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
int main(int argc, const char** argv)
{
    try
    {
        const char* host = (argc > 1) ? argv[1] : "localhost";
        int port = (argc > 2) ? atoi(argv[2]) : 52520;
        int count = (argc > 3) ? atoi(argv[3]) : 1000000;
        int delay = (argc > 4) ? atoi(argv[4]) : 10;
        int testNo = (argc > 5) ? atoi(argv[5]) : 1;
        int eventSize = (argc > 6) ? atoi(argv[6]) : 512;

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

        // Subscription: perfTestVar
        std::vector<std::string> perfTestVarPath({"Objects", "2:OpcDemoDevice", "2:perfTestVar"});
        OpcUa::Node perfTestVar = root.GetChild(perfTestVarPath);
        std::cout << "got perfTestVar node: " << perfTestVar << std::endl;

        // Subscription: StaticInt32Array
        std::vector<std::string> staticArrayVarPath({"Objects", "2:OpcDemoDevice", "2:StaticInt32Array"});
        OpcUa::Node staticArrayVar = root.GetChild(staticArrayVarPath);
        std::cout << "got staticArrayVar node: " << staticArrayVar << std::endl;

        SubClient sclt(testNo);
        std::unique_ptr<Subscription> sub = client.CreateSubscription(1, sclt);
        uint32_t handle1 = sub->SubscribeDataChange(perfTestVar);
        uint32_t handle2 = sub->SubscribeDataChange(staticArrayVar);
        sub->SubscribeEvents(); // XXX TODO FIXME: Add filter for events
//        std::cout << "Got sub handle: " << handle1 << ", sleeping..." << std::endl;

        sleep(10000000); // XXX secs
        std::cout << "Disconnecting" << std::endl;
        return 0;
    }
    catch (const std::exception& exc)
    {
        std::cout << exc.what() << std::endl;
    }
    catch (...)
    {
        std::cout << "Unknown error." << std::endl;
    }
    return -1;
}

