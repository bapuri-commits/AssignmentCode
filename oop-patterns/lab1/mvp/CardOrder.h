#ifndef CARDORDER_H
#define CARDORDER_H

#include <string>
#include <iostream>
#include "Name.h"
#include "Stencil.h"
#include "Calculator.h"
using namespace std;

class CardOrder {
private:
    Name name;
    Stencil stencil;
    Calculator calculator;
    int numCards;

public:
    CardOrder(string fullName)
        : name(fullName), stencil(), calculator() {
        numCards = 0;
    }

    Name& getName() { return name; }
    Stencil& getStencil() { return stencil; }
    int getNumCards() { return numCards; }
    void setNumCards(int n) { numCards = n; }
    void setBorderChar(char c) { stencil.setBorderChar(c); }

    string getSampleCard() {
        string initials = name.getInitials();
        string displayName = name.getDisplayName();

        string top = stencil.buildTopLine(initials);
        string empty = stencil.buildEmptyLine();
        string nameLine = stencil.buildNameLine(displayName);

        return "Here is a sample card:\n\n"
             + top + "\n"
             + empty + "\n"
             + nameLine + "\n"
             + empty + "\n"
             + top;
    }

    void printResult() {
        int nameLen = name.getDisplayLength();
        int price = calculator.calcTotal(nameLen, numCards);
        cout << "The price of " << numCards << " cards is " << price << " won." << endl;
        cout << calculator.getDiscountMsg(numCards) << endl;
    }
};

#endif
