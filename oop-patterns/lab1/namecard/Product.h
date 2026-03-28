#ifndef PRODUCT_H
#define PRODUCT_H

#include <string>
#include "NameText.h"
#include "CardStencil.h"
using namespace std;

// 상품 추상 클래스
class Product {
public:
    virtual string generate() = 0;
    virtual int getNameLength() = 0;
    virtual bool isValidName() = 0;
    virtual void setBorderChar(char c) = 0;
    virtual ~Product() {}
};

// 명함 카드
class Card : public Product {
private:
    NameText nameText;
    CardStencil1 stencil;

public:
    Card(string fullName)
        : nameText(fullName), stencil() {}

    NameText& getNameText() { return nameText; }
    CardStencil1& getStencil() { return stencil; }

    string generate() override {
        string initials = nameText.getInitials();
        string displayName = nameText.getDisplayName();

        string top = stencil.buildTopLine(initials);
        string empty = stencil.buildEmptyLine();
        string nameLine = stencil.buildNameLine(displayName);

        return top + "\n"
             + empty + "\n"
             + nameLine + "\n"
             + empty + "\n"
             + top;
    }

    int getNameLength() override {
        return nameText.getDisplayLength();
    }

    bool isValidName() override {
        return nameText.isValid();
    }

    void setBorderChar(char c) override {
        stencil.setBorderChar(c);
    }
};

#endif
