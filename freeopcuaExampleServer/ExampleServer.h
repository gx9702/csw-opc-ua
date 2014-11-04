#pragma once

#include <opc/ua/node.h>
#include <opc/ua/subscription.h>
#include <opc/ua/server/opcuaserver.h>

using namespace OpcUa;

class ExampleServer {
private:
    const char *host;
    int port;
    int count;
    int delay;
    int testNo;
    int eventSize;

    OPCUAServer server;

    void createNamespace();
    Event createEvent();
    void startPerfTest(Event ev, Node perfTestVar, Node staticArrayNode, std::vector<int32_t> ar);


public:
    ExampleServer(char const *host, int port, int count, int delay, int testNo, int eventSize)
            : host(host), port(port), count(count), delay(delay), testNo(testNo), eventSize(eventSize), server(false) {
    }

    void start();
};