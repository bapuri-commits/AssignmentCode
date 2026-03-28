#include <iostream>
#include <string>
#include "NameText.h"
#include "CardStencil.h"
#include "Calculator.h"
#include "Product.h"
#include "Order.h"
#include "POS.h"
using namespace std;


void testNameText() {
    cout << "========== NameText TEST ==========" << endl;

    NameText n1("Jonathan Peter Macdonald");
    cout << "[3 words] Jonathan Peter Macdonald" << endl;
    cout << "  getInitials()     = " << n1.getInitials() << endl;
    cout << "  getDisplayName()  = " << n1.getDisplayName() << endl;
    cout << "  getDisplayLength()= " << n1.getDisplayLength() << endl;
    cout << "  isValid()         = " << n1.isValid() << endl;
    cout << endl;

    NameText n2("Barbara Thomson");
    cout << "[2words] Barbara Thomson" << endl;
    cout << "  getInitials()     = " << n2.getInitials() << endl;
    cout << "  getDisplayName()  = " << n2.getDisplayName() << endl;
    cout << "  getDisplayLength()= " << n2.getDisplayLength() << endl;
    cout << endl;

    NameText n3("Madonna");
    cout << "[no space] Madonna -> isValid() = " << n3.isValid() << endl;
    cout << endl;
}

void testCardStencil() {
    cout << "========== CardStencil1 TEST ==========" << endl;

    CardStencil1 s;
    cout << "basic borderChar = " << s.getBorderChar() << endl;
    cout << "buildTopLine(JPM)   = " << s.buildTopLine("JPM") << endl;
    cout << "buildEmptyLine()    = " << s.buildEmptyLine() << endl;
    cout << "buildNameLine(test) = " << s.buildNameLine("Jonathan P Macdonald") << endl;
    cout << endl;

    s.setBorderChar('+');
    cout << "change borderChar  = " << s.getBorderChar() << endl;
    cout << "buildTopLine(JPM)   = " << s.buildTopLine("JPM") << endl;
    cout << endl;
}

void testCalculator() {
    cout << "========== Calculator TEST ==========" << endl;

    Calculator calc;

    cout << "[long name + 200 cards] calcTotal(20, 200) = " << calc.calcTotal(20, 200) << endl;
    cout << "  hasDiscount(200)    = " << calc.hasDiscount(200) << endl;
    cout << "  getDiscountMsg(200) = " << calc.getDiscountMsg(200) << endl;
    cout << endl;

    cout << "[short name + 100 cards] calcTotal(10, 100) = " << calc.calcTotal(10, 100) << endl;
    cout << "  hasDiscount(100)    = " << calc.hasDiscount(100) << endl;
    cout << "  getDiscountMsg(100) = " << calc.getDiscountMsg(100) << endl;
    cout << endl;

    cout << "[boundary] calcTotal(12, 199) = " << calc.calcTotal(12, 199) << endl;
    cout << "  calcTotal(13, 200)        = " << calc.calcTotal(13, 200) << endl;
    cout << endl;
}

void testProduct() {
    cout << "========== Product TEST ==========" << endl;

    Card p("Jonathan Peter Macdonald");
    cout << "[Card Generate]" << endl;
    cout << p.generate() << endl;
    cout << "  nameLength = " << p.getNameLength() << endl;
    cout << endl;

    p.getStencil().setBorderChar('+');
    cout << "[border change -> +]" << endl;
    cout << p.generate() << endl;
    cout << endl;
}

void testOrder() {
    cout << "========== Order all TEST ==========" << endl;

    cout << "[A] long name + 200 cards" << endl;
    Order orderA("Jonathan Peter Macdonald");
    orderA.setQuantity(200);
    orderA.printSampleCard();
    orderA.printResult();
    cout << endl;

    cout << "[B] short name + 100 cards" << endl;
    Order orderB("Eun Man Choi");
    orderB.setQuantity(100);
    orderB.printSampleCard();
    orderB.printResult();
    cout << endl;

    cout << "[C] border change -> +" << endl;
    orderA.setBorderChar('+');
    orderA.printSampleCard();
    cout << endl;
}

void testPOS() {
    cout << "========== POS all TEST ==========" << endl;

    POS pos;
    pos.addOrder("Jonathan Peter Macdonald");
    pos.getLastOrder().setQuantity(200);

    pos.addOrder("Eun Man Choi");
    pos.getLastOrder().setQuantity(100);

    cout << "Order num: " << pos.getOrderCount() << endl;
    pos.printAllResults();
}



string inputName() {
    string fullName;
    while (true) {
        cout << "Enter name: ";
        getline(cin, fullName);
        NameText temp(fullName);
        if (temp.isValid()) return fullName;
        cout << "Error: Name must contain a space and be 28 characters or less." << endl;
    }
}

void inputBorder(Order& order) {
    string input;
    while (true) {
        order.printSampleCard();
        cout << "\nEnter \"OK\" if this card is ok, otherwise enter an alternative border" << endl;
        cout << "character: ";
        getline(cin, input);
        if (input == "OK") break;
        order.setBorderChar(input[0]);
        cout << endl;
    }
}

int inputQuantity() {
    int num;
    while (true) {
        cout << "\nHow many cards would you like? ";
        cin >> num;
        if (num >= 1 && num <= 1000) return num;
        cout << "Error: Quantity must be between 1 and 1000." << endl;
    }
}

void runProgram() {
    cout << "========== Card Print Program ==========" << endl;

    POS pos;
    string fullName = inputName();
    pos.addOrder(fullName);
    Order& order = pos.getLastOrder();

    inputBorder(order);
    int qty = inputQuantity();
    order.setQuantity(qty);

    cout << endl;
    order.printResult();
}


int main() {
    testNameText();
    testCardStencil();
    testCalculator();
    testProduct();
    testOrder();
    testPOS();

    // runProgram();

    return 0;
}
