#ifndef POS_H
#define POS_H

#include "Order.h"
#include "IOInterface.h"
#include <vector>
#include <memory>
#include <iostream>

class POS {
protected:
    std::shared_ptr<Input> input;
    std::shared_ptr<Output> output;

public:
    POS(std::shared_ptr<Input> i, std::shared_ptr<Output> o) : input(i), output(o) {}
    virtual ~POS() = default;
    virtual void addOrder(std::shared_ptr<Order> order) = 0;
    virtual int calculateTotal() = 0;
    virtual const std::vector<std::shared_ptr<Order>>& getOrders() const = 0;
    virtual void runScenario() = 0;
};

#endif // POS_H
