#ifndef CALCULATOR_H
#define CALCULATOR_H

#include <string>
using namespace std;

// 가격 계산 및 할인 판단
class Calculator {
public:
    int calcTotal(int nameLen, int numCards) {
        int pricePerCard = (nameLen <= 12) ? 40 : 50;
        int total = pricePerCard * numCards;
        if (numCards >= 200) {
            total = (int)(total * 0.9);
        }
        return total;
    }

    bool hasDiscount(int numCards) {
        return numCards >= 200;
    }

    string getDiscountMsg(int numCards) {
        if (numCards >= 200) {
            return "10% discount applied";
        }
        return "No discount given";
    }
};

#endif
