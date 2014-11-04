#pragma once

#include <opc/ua/client/client.h>
#include <opc/ua/node.h>
#include <opc/ua/subscription.h>
#include <opc/ua/event.h>

#include <iostream>
#include <stdexcept>
#include <unistd.h>


using namespace OpcUa;

static int receivedVarUpdates = 0;

// Contains callback methods for variable updates and events
class SubClient : public SubscriptionClient {

private:
    int testNo;
    DateTime startTime;

public:
    virtual void Event(uint32_t handle, OpcUa::Event const &event) const override {
        receivedUpdate();
    }

    void DataChange(uint32_t handle, const Node& node, const Variant& val, AttributeID attr) const override {
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
    SubClient(int testNo) :testNo(testNo), startTime(OpcUa::CurrentDateTime()) {
    }
};
