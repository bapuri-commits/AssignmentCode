# teammade 설계 리뷰 및 개선 방안

---

## 1. 현재 클래스 구조

```
Text (추상)                     Product (추상)           Order (추상)
  └── NameText                    └── Card                 └── CardOrder

CardStencil (추상)              Input (추상)             POS (추상)
  └── CardStencil1                ├── UserInput            └── VendingMachinePOS
                                  └── TestInput
                                Output (추상)
                                  ├── UserOutput
                                  └── TestOutput
```

9개 헤더, 14개 클래스 (추상 7 + 구체 7)

---

## 2. 설계 문제 4가지

### 문제 1: 가격 책임이 두 클래스에 분산됨

| 위치 | 담당 내용 |
|---|---|
| `Card::getPrice()` | 장당 가격 (이름 길이 기준 40/50원) |
| `CardOrder::getTotalPrice()` | 총액 + 할인 계산 |
| 없음 | 할인 메시지 ("10% discount applied") |

가격이라는 하나의 관심사가 Card와 CardOrder에 나뉘어 있고,
할인 메시지는 아예 출력할 수 없다.

**위반 원칙:** SRP — 가격 정책이 응집되지 않음

---

### 문제 2: Order가 불변 객체로 설계됨

```cpp
CardOrder(std::shared_ptr<Card> c, int q);  // 생성 시 수량 확정
// setQuantity() 없음
```

명세의 흐름은 카드 미리보기 → 테두리 변경 → 수량 입력 순서인데,
Order 생성 시점에 수량이 필요하므로 이 흐름을 구현할 수 없다.

**위반 원칙:** 유스케이스와 객체 생명주기 불일치

---

### 문제 3: DI만 있고 팩토리가 없음

Card 하나를 만들려면 외부에서 3개 객체를 조립해야 한다:

```cpp
auto stencil = std::make_shared<CardStencil1>();
auto text = std::make_shared<NameText>(fullName);
auto card = std::make_shared<Card>(stencil, text, 0);  // 더미 가격 파라미터
```

조립 로직이 캡슐화되지 않아 사용하는 곳마다 반복된다.

**위반 원칙:** 생성 책임 캡슐화 부족

---

### 문제 4: Product 인터페이스의 모호함

```cpp
class Product {
    virtual std::string getName() const = 0;   // 상품명? 사람이름?
    virtual int getPrice() const = 0;          // 가격 정책이 Product에 있어야 하나?
    virtual std::string toString() const = 0;
};
```

`getName()`의 의미가 불분명하고 (`Card`에서는 `"Custom Card"` 하드코딩),
`getPrice()`는 Product의 본질적 책임이 아니다.

**위반 원칙:** ISP — 인터페이스가 도메인 역할에 맞지 않음

---

## 3. 해결 방안: Calculator 클래스 도입

가격 관련 책임을 전담하는 Calculator를 신설하고,
그에 따라 연쇄적으로 다른 클래스들을 정리한다.

### 3-1. Calculator 신설

```cpp
class Calculator {
public:
    int calcPricePerCard(int displayNameLen) {
        return (displayNameLen <= 12) ? 40 : 50;
    }

    int calcTotal(int displayNameLen, int quantity) {
        int total = calcPricePerCard(displayNameLen) * quantity;
        if (quantity >= 200) {
            total = (int)(total * 0.9);
        }
        return total;
    }

    bool hasDiscount(int quantity) {
        return quantity >= 200;
    }

    std::string getDiscountMsg(int quantity) {
        if (quantity >= 200) return "10% discount applied";
        return "No discount given";
    }
};
```

→ 문제 1 해결: 장당 가격, 총액, 할인, 메시지가 한 클래스에 응집

---

### 3-2. Product 인터페이스 정리

```
변경 전                        변경 후
─────────────────              ─────────────────
Product                        Product
  getName()   ← 모호             toString()
  getPrice()  ← Calculator로
  toString()
```

Product는 "만들어서 보여주는 것"이라는 역할만 남긴다.
가격은 Calculator의 책임이므로 Product에서 제거한다.

→ 문제 4 해결: Product 인터페이스가 렌더링 역할에 집중

---

### 3-3. Card 변경

```
변경 전                        변경 후
─────────────────              ─────────────────
Card : Product                 Card : Product
  getPrice()  ← 제거             toString()
  getName()   ← "Custom Card"   getDisplayNameLength()
  Card(s, t, p=0)               Card(s, t)
                                 static create(fullName)
```

- `getPrice()` 제거 — Calculator로 이관
- `getName()` 제거 — Product에서 사라졌으므로
- 더미 파라미터 `int p = 0` 제거
- `getDisplayNameLength()` 추가 — Calculator가 가격 계산 시 사용
- `static create()` 팩토리 메서드 추가 — 조립 캡슐화

```cpp
class Card : public Product {
private:
    std::shared_ptr<CardStencil> stencil;
    std::shared_ptr<NameText> cardText;

public:
    Card(std::shared_ptr<CardStencil> s, std::shared_ptr<NameText> t)
        : stencil(s), cardText(t) {}

    static std::shared_ptr<Card> create(const std::string& fullName) {
        auto stencil = std::make_shared<CardStencil1>();
        auto text = std::make_shared<NameText>(fullName);
        return std::make_shared<Card>(stencil, text);
    }

    std::string toString() const override {
        return stencil->render(cardText->getContent(), cardText->getInitials());
    }

    int getDisplayNameLength() const {
        return cardText->getContent().length();
    }

    void setBorder(char c) {
        stencil->setBorderChar(c);
    }
};
```

→ 문제 3 해결: `Card::create("Jonathan Peter Macdonald")` 한 줄로 생성 가능
→ 문제 4 해결: Card가 렌더링 + 명함 고유 정보만 제공

---

### 3-4. CardOrder 변경

```
변경 전                        변경 후
─────────────────              ─────────────────
CardOrder : Order              CardOrder : Order
  CardOrder(card, quantity)      CardOrder(card)
  quantity 불변                   setQuantity() 추가
  getTotalPrice() 직접 계산       getTotalPrice() → calculator 위임
  할인 메시지 없음                getDiscountMsg() → calculator 위임
```

```cpp
class CardOrder : public Order {
private:
    std::shared_ptr<Card> card;
    Calculator calculator;
    int quantity;

public:
    CardOrder(std::shared_ptr<Card> c)
        : card(c), calculator(), quantity(0) {}

    void setQuantity(int q) { quantity = q; }

    std::shared_ptr<Product> getProduct() const override {
        return card;
    }

    int getQuantity() const override {
        return quantity;
    }

    int getTotalPrice() const override {
        int nameLen = card->getDisplayNameLength();
        return calculator.calcTotal(nameLen, quantity);
    }

    std::string getDiscountMsg() const {
        return calculator.getDiscountMsg(quantity);
    }

    std::shared_ptr<Card> getCard() const { return card; }
};
```

→ 문제 1 해결: 가격 로직이 CardOrder에서 사라지고 Calculator에 위임
→ 문제 2 해결: 수량 없이 생성 가능, setQuantity()로 나중에 설정

---

## 4. 변경 후 사용 흐름

명세의 대화형 흐름과 자연스럽게 일치한다:

```
1. auto card = Card::create("Jonathan Peter Macdonald");
   auto order = std::make_shared<CardOrder>(card);

2. card->toString();             // 샘플 카드 미리보기
   card->setBorder('+');         // 테두리 변경
   card->toString();             // 다시 미리보기

3. order->setQuantity(200);      // 수량 나중에 설정

4. order->getTotalPrice();       // → Calculator가 계산: 9000
   order->getDiscountMsg();      // → "10% discount applied"
```

---

## 5. 변경 전후 구조 비교

```
변경 전                                    변경 후
──────────────────────────                ──────────────────────────
Product                                   Product
  getName()    → 모호                       toString()
  getPrice()   → Card에 가격 혼재
  toString()

Card : Product                            Card : Product
  getPrice()   → 장당 가격                   toString()
  getName()    → "Custom Card"              getDisplayNameLength()
  Card(s,t,p=0)→ 더미 파라미터               create(fullName) ← 팩토리
                                            Card(s, t)

CardOrder : Order                         CardOrder : Order
  CardOrder(card, qty)                      CardOrder(card) ← 수량 없이
  getTotalPrice() → 직접 할인 계산            setQuantity()
  할인 메시지 없음                            getTotalPrice()  → Calculator
                                            getDiscountMsg() → Calculator

없음                                       Calculator ← 신설
                                            calcPricePerCard()
                                            calcTotal()
                                            hasDiscount()
                                            getDiscountMsg()
```

---

## 6. 요약

| 설계 문제 | 원인 | 해결 |
|---|---|---|
| 가격 책임 분산 | Card와 CardOrder에 산재 | Calculator로 응집 |
| Order 불변 | 생성자에서 수량 확정 | setQuantity() 추가, Calculator가 on-demand 계산 |
| 팩토리 부재 | Card 생성에 3단계 조립 필요 | Card::create() 정적 팩토리 + 더미 파라미터 제거 |
| Product 인터페이스 모호 | getPrice/getName이 역할에 안 맞음 | 가격 제거, 렌더링에 집중 |

Calculator 하나를 도입하면서 4가지 문제가 연쇄적으로 해결된다.
