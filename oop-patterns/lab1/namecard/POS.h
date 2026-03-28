#ifndef POS_H
#define POS_H

#include <vector>
#include <string>
#include <iostream>
#include "Order.h"
using namespace std;

// 주문 목록 관리
class POS {
private:
    vector<Order> orders;

public:
    POS() {}

    void addOrder(string fullName) {
        orders.push_back(Order(fullName));
    }

    Order& getLastOrder() {
        return orders.back();
    }

    int getOrderCount() {
        return orders.size();
    }

    void printAllResults() {
        for (int i = 0; i < orders.size(); i++) {
            cout << "=== Order " << (i + 1) << " ===" << endl;
            orders[i].printSampleCard();
            orders[i].printResult();
            cout << endl;
        }
    }
};

#endif
