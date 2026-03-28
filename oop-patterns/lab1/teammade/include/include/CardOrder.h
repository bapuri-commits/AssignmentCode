#ifndef CARD_ORDER_H
#define CARD_ORDER_H

#include "Order.h"
#include "Card.h"

class CardOrder : public Order {
private:
    std::shared_ptr<Card> card;
    int quantity;

public:
    CardOrder(std::shared_ptr<Card> c, int q) : card(c), quantity(q) {}

    std::shared_ptr<Product> getProduct() const override {
        return card;
    }

    int getQuantity() const override {
        return quantity;
    }

    int getTotalPrice() const override {
        int total = card->getPrice() * quantity;
        if (quantity >= 200) { // fix: 원래 price*quantity만 반환 → 200장 이상 10% 할인 로직 추가
            total = (int)(total * 0.9);
        }
        return total;
    }
    
    // 카드 전용 정보가 필요할 경우를 위해 캐스팅 없이 접근 가능하도록
    std::shared_ptr<Card> getCard() const { return card; }
};

#endif // CARD_ORDER_H
