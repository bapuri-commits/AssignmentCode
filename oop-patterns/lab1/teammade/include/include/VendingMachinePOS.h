#ifndef VENDING_MACHINE_POS_H
#define VENDING_MACHINE_POS_H

#include "POS.h"
#include "Card.h"
#include "CardOrder.h"

class VendingMachinePOS : public POS {
private:
    std::vector<std::shared_ptr<Order>> orders;

public:
    VendingMachinePOS(std::shared_ptr<Input> i, std::shared_ptr<Output> o);

    void addOrder(std::shared_ptr<Order> order) override;
    int calculateTotal() override;
    const std::vector<std::shared_ptr<Order>>& getOrders() const override;
    void runScenario() override;
};

#endif // VENDING_MACHINE_POS_H
