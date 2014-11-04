#pragma once

#include <iostream>
#include <iostream>

#include <opc/ua/node.h>
#include <opc/ua/subscription.h>
#include <opc/ua/server/opcuaserver.h>

// Holds callback methods for variable updates
class SubClient : public SubscriptionClient {
    void DataChange(uint32_t handle, const Node &node, const Variant &val, AttributeID attr) const override {
        std::cout << "Received DataChange event for Node " << node << std::endl;
    }
};
