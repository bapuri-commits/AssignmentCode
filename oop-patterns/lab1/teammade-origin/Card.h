#ifndef CARD_H
#define CARD_H

#include "Product.h"
#include "CardStencil.h"
#include "Text.h"
#include <memory>

class Card : public Product {
private:
    std::shared_ptr<CardStencil> stencil;
    std::shared_ptr<NameText> cardText;

public:
    Card(std::shared_ptr<CardStencil> s, std::shared_ptr<NameText> t, int p = 0) 
        : stencil(s), cardText(t) {
        (void)p; // Ignore dummy price
    }

    std::string getName() const override {
        return "Custom Card";
    }

    int getPrice() const override {
        // 이름이 12글자 이내면 40원, 12자가 넘어가면 50원
        if (cardText->getRawInputLength() <= 12) {
            return 40;
        } else {
            return 50;
        }
    }

    std::string toString() const override {
        return stencil->render(cardText->getContent(), cardText->getInitials());
    }

    void setBorder(char c) {
        stencil->setBorderChar(c);
    }
};

#endif // CARD_H
