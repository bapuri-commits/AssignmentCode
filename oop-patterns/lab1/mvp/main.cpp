#include <iostream>
#include <string>
#include "Name.h"
#include "Stencil.h"
#include "Calculator.h"
#include "CardOrder.h"
using namespace std;

void testName() {
    cout << "========== Name Test ==========" << endl;

    Name n1("Jonathan Peter Macdonald");
    cout << "[3 words] Jonathan Peter Macdonald" << endl;
    cout << "  getInitials()     = " << n1.getInitials() << endl;
    cout << "  getDisplayName()  = " << n1.getDisplayName() << endl;
    cout << "  getDisplayLength()= " << n1.getDisplayLength() << endl;
    cout << "  isValid()         = " << n1.isValid() << endl;
    cout << endl;

    Name n2("Barbara Thomson");
    cout << "[2 words] Barbara Thomson" << endl;
    cout << "  getInitials()     = " << n2.getInitials() << endl;
    cout << "  getDisplayName()  = " << n2.getDisplayName() << endl;
    cout << "  getDisplayLength()= " << n2.getDisplayLength() << endl;
    cout << endl;

    Name n3("Eun Man Choi");
    cout << "[short] Eun Man Choi" << endl;
    cout << "  getDisplayName()  = " << n3.getDisplayName() << endl;
    cout << "  getDisplayLength()= " << n3.getDisplayLength() << endl;
    cout << endl;

    Name n4("Madonna");
    cout << "[no space] Madonna -> isValid() = " << n4.isValid() << endl;
    cout << endl;
}

void testStencil() {
    cout << "========== Stencil Test ==========" << endl;

    Stencil s;
    cout << "default borderChar = " << s.getBorderChar() << endl;
    cout << "buildTopLine(JPM)   = " << s.buildTopLine("JPM") << endl;
    cout << "buildEmptyLine()    = " << s.buildEmptyLine() << endl;
    cout << "buildNameLine(test) = " << s.buildNameLine("Jonathan P Macdonald") << endl;
    cout << endl;

    s.setBorderChar('+');
    cout << "changed borderChar  = " << s.getBorderChar() << endl;
    cout << "buildTopLine(JPM)   = " << s.buildTopLine("JPM") << endl;
    cout << endl;
}

void testCalculator() {
    cout << "========== Calculator Test ==========" << endl;

    Calculator calc;

    cout << "[long + 200] calcTotal(21, 200) = " << calc.calcTotal(21, 200) << endl;
    cout << "  hasDiscount(200)    = " << calc.hasDiscount(200) << endl;
    cout << "  getDiscountMsg(200) = " << calc.getDiscountMsg(200) << endl;
    cout << endl;

    cout << "[short + 100] calcTotal(10, 100) = " << calc.calcTotal(10, 100) << endl;
    cout << "  hasDiscount(100)    = " << calc.hasDiscount(100) << endl;
    cout << "  getDiscountMsg(100) = " << calc.getDiscountMsg(100) << endl;
    cout << endl;

    cout << "[boundary] calcTotal(12, 199) = " << calc.calcTotal(12, 199) << endl;
    cout << "  calcTotal(13, 200)        = " << calc.calcTotal(13, 200) << endl;
    cout << endl;
}

void testCardOrder() {
    cout << "========== CardOrder Test ==========" << endl;

    cout << "[A] long name + 200" << endl;
    CardOrder orderA("Jonathan Peter Macdonald");
    orderA.setNumCards(200);
    cout << orderA.getSampleCard() << endl;
    orderA.printResult();
    cout << endl;

    cout << "[B] short name + 100" << endl;
    CardOrder orderB("Eun Man Choi");
    orderB.setNumCards(100);
    cout << orderB.getSampleCard() << endl;
    orderB.printResult();
    cout << endl;

    cout << "[C] border change -> +" << endl;
    orderA.setBorderChar('+');
    cout << orderA.getSampleCard() << endl;
    cout << endl;

    cout << "[D] 2 words + 200" << endl;
    CardOrder orderD("Barbara Thomson");
    orderD.setNumCards(200);
    cout << orderD.getSampleCard() << endl;
    orderD.printResult();
    cout << endl;
}

string inputName() {
    string fullName;
    while (true) {
        cout << "Enter name: ";
        getline(cin, fullName);
        Name temp(fullName);
        if (temp.isValid()) {
            return fullName;
        }
        cout << "Error: Name must contain a space and be 28 characters or less." << endl;
    }
}

void inputBorder(CardOrder& order) {
    string input;
    while (true) {
        cout << order.getSampleCard() << endl;
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

    string fullName = inputName();
    CardOrder order(fullName);
    inputBorder(order);
    int quantity = inputQuantity();
    order.setNumCards(quantity);
    cout << endl;
    order.printResult();
}

int main() {
    testName();
    testStencil();
    testCalculator();
    testCardOrder();

    // runProgram();

    return 0;
}
