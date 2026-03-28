#ifndef ORDER_H
#define ORDER_H

#include <string>
#include <iostream>
#include "Product.h"
#include "Calculator.h"
using namespace std;

// 개별 명함 주문 1건
class Order {
private:
    Card card;
    Calculator calculator;
    int quantity;

public:
    Order(string fullName)
        : card(fullName), calculator() {
        quantity = 0;
    }

    Card& getCard() { return card; }
    int getQuantity() { return quantity; }
    void setQuantity(int n) { quantity = n; }

    void setBorderChar(char c) {
        card.setBorderChar(c);
    }

    void printSampleCard() {
        cout << "Here is a sample card:" << endl << endl;
        cout << card.generate() << endl;
    }

    void printResult() {
        int nameLen = card.getNameLength();
        int price = calculator.calcTotal(nameLen, quantity);
        cout << "The price of " << quantity << " cards is " << price << " won." << endl;
        cout << calculator.getDiscountMsg(quantity) << endl;
    }

    bool isValidName() {
        return card.isValidName();
    }
};

#endif
