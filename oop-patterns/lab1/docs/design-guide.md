# LAB1 수정 설계 가이드

> **대상**: 팀원 전원  
> **목적**: `teammade-revised.drawio`의 클래스 구조와 실행 흐름을 이해하기 위한 설명서  
> **기준 설계**: 수정된 팀 설계 (Revised)

---

## 목차

1. [전체 클래스 구조 한눈에 보기](#1-전체-클래스-구조-한눈에-보기)
2. [레이어별 클래스 설명](#2-레이어별-클래스-설명)
   - 이름 처리 레이어: Text / NameText
   - 카드 렌더링 레이어: CardStencil / CardStencil1
   - 상품 레이어: Product / Card
   - 가격 계산 레이어: Calculator
   - 주문 레이어: Order / CardOrder
   - I/O 레이어: Input / Output / UserInput / UserOutput / TestInput / TestOutput
   - POS 레이어: POS / VendingMachinePOS
3. [클래스 간 관계 정리](#3-클래스-간-관계-정리)
4. [실행 흐름 (runScenario 기준)](#4-실행-흐름-runscenario-기준)
5. [원설계 대비 핵심 변경점](#5-원설계-대비-핵심-변경점)
6. [자주 헷갈리는 포인트 Q&A](#6-자주-헷갈리는-포인트-qa)

---

## 1. 전체 클래스 구조 한눈에 보기

```
[ 이름 처리 ]           [ 카드 렌더링 ]
Text (추상)             CardStencil (추상)
  └── NameText            └── CardStencil1

[ 상품 ]                [ 가격 계산 ]
Product (추상)          Calculator
  └── Card

[ 주문 ]
Order (인터페이스)
  └── CardOrder ──── Calculator (포함, by value)
                └─── Card (포함, shared_ptr)

[ I/O ]
Input (추상)            Output (추상)
  ├── UserInput           ├── UserOutput
  └── TestInput           └── TestOutput

[ POS ]
POS (추상)  ──── Input (포함)
              └── Output (포함)
  └── VendingMachinePOS ──── Order목록 (vector)
```

**클래스 총 15개** (추상/인터페이스 7 + 구체 클래스 8)

---

## 2. 레이어별 클래스 설명

---

### 2-1. 이름 처리 레이어

#### `Text` — 추상 클래스

```cpp
class Text {
public:
    virtual ~Text() = default;
    virtual std::string getContent() const = 0;  // 순수 가상
};
```

**역할**: "텍스트를 반환하는 것"의 추상적 계약. 나중에 다른 종류의 텍스트(주소, 직함 등)가 생기면 이 인터페이스를 구현하면 된다.

---

#### `NameText` — 이름 파싱·포맷 담당

```
필드:
  - formattedName : string    → 표시용 이름 (예: "Jonathan P Macdonald")
  - initials      : string    → 이니셜 (예: "JPM")
  - rawLength     : size_t    → 원본 입력 문자 수
  - valid         : bool      → 유효 여부

메서드:
  + NameText(rawInput)        → 생성과 동시에 parse() 호출
  + parse(input)              → 이름을 단어별로 분리, 포맷 생성
  + checkValid()              → 유효 여부 반환
  + getContent()              → formattedName 반환 (Text 인터페이스 구현)
  + getInitials()             → 이니셜 반환
  + getRawInputLength()       → 원본 길이 반환
  + getDisplayNameLength()    → formattedName 길이 반환 (가격 계산에 사용)
```

**이름 처리 규칙**:

| 입력 단어 수 | 결과 | 예시 |
|---|---|---|
| 2단어 | first + " " + last | "Barbara Thomson" → `BT` |
| 3단어 | first + " " + 중간이니셜 + " " + last | "Jonathan Peter Macdonald" → `JPM`, "Jonathan P Macdonald" |
| 1단어 또는 4단어 이상 | valid = false | |
| 28자 초과 | valid = false | |

> **주의**: `getDisplayNameLength()`는 원본 입력 길이가 아니라 **포맷된 이름(formattedName)** 의 길이를 반환한다. 이것이 가격 계산 기준이 된다.

---

### 2-2. 카드 렌더링 레이어

#### `CardStencil` — 추상 클래스

```cpp
class CardStencil {
protected:
    char borderChar = '*';      // 자식 클래스가 사용
public:
    virtual ~CardStencil() = default;
    virtual std::string getPattern() const = 0;
    virtual std::string render(name, initials) const = 0;
    void setBorderChar(char c);  // 비가상 — 모든 자식이 동일하게 사용
};
```

**역할**: "카드를 그리는 방법"의 추상 계약. 나중에 CardStencil2, CardStencil3 등 다른 레이아웃을 추가할 수 있다.

---

#### `CardStencil1` — 표준 명함 레이아웃 구현

```
render("Jonathan P Macdonald", "JPM") 결과:

JPM**************************JPM
*                              *
*     Jonathan P Macdonald    *
*                              *
JPM**************************JPM
```

렌더링 로직 (전체 너비 32):
1. **상단/하단**: `이니셜 + borderChar * (32 - 이니셜길이*2) + 이니셜`
2. **빈 줄**: `borderChar + 공백 30개 + borderChar`
3. **이름 줄**: 이름을 가운데 정렬, 홀수 공백이면 왼쪽에 1칸 더

---

### 2-3. 상품 레이어

#### `Product` — 추상 클래스 (인터페이스 역할)

```cpp
class Product {
public:
    virtual ~Product() = default;
    virtual std::string toString() const = 0;  // 유일한 책임
};
```

**역할**: "화면에 보여줄 수 있는 상품"이라는 계약. 수정 설계에서는 `toString()` 하나만 남긴다.

> **원설계와의 차이**: 원설계에는 `getName()`, `getPrice()`가 있었다. 수정 설계에서는 가격 책임을 Calculator로, 이름 관련 메서드는 Card 내부로 이동시키고 인터페이스를 단순화했다.

---

#### `Card` — 명함 상품 (Product 구현체)

```
필드:
  - stencil  : shared_ptr<CardStencil>   → 렌더링 전략 (교체 가능)
  - cardText : shared_ptr<NameText>      → 이름 데이터

메서드:
  + Card(s, t)                           → 생성자 (더미 파라미터 제거됨)
  + static create(fullName: string)      → 팩토리 메서드 ★
  + toString()                           → stencil->render() 위임
  + setBorder(c: char)                   → stencil->setBorderChar() 위임
  + getDisplayNameLength()               → cardText->getDisplayNameLength() 위임
```

**팩토리 메서드 `create()`의 역할**:

```cpp
// 사용 전 (원설계) — 사용자가 3단계 조립 필요
auto stencil = std::make_shared<CardStencil1>();
auto text    = std::make_shared<NameText>("Jonathan Peter Macdonald");
auto card    = std::make_shared<Card>(stencil, text, 0);  // 더미 0

// 사용 후 (수정 설계) — 한 줄로 완료
auto card = Card::create("Jonathan Peter Macdonald");
```

`create()` 내부에서 CardStencil1과 NameText를 조립한다. Card를 만드는 방법이 바뀌어도 `create()` 안만 수정하면 된다.

---

### 2-4. 가격 계산 레이어

#### `Calculator` — 가격 정책 전담 (신규 클래스)

```
메서드:
  + calcPricePerCard(len: int): int    → 이름 길이로 장당 가격 결정
  + calcTotal(len: int, qty: int): int → 총액 계산 (할인 포함)
  + hasDiscount(qty: int): bool        → 200장 이상 여부
  + getDiscountMsg(qty: int): string   → 할인 메시지 반환
```

**가격 정책 상세**:

```
장당 가격:
  formattedName 길이 ≤ 12  →  40원
  formattedName 길이 > 12  →  50원

총액:
  기본 = 장당가격 × 수량
  수량 ≥ 200 이면 → 기본 × 0.9 (10% 할인)

메시지:
  수량 ≥ 200  →  "10% discount applied"
  수량 < 200  →  "No discount given"
```

**예시 계산**:
- "Jonathan P Macdonald" (20자) × 200장 = 50 × 200 × 0.9 = **9,000원**
- "Eun Man Choi" (12자) × 100장 = 40 × 100 = **4,000원**
- "Barbara Thomson" (15자) × 199장 = 50 × 199 = **9,950원** (할인 없음)

---

### 2-5. 주문 레이어

#### `Order` — 인터페이스

```cpp
class Order {
public:
    virtual ~Order() = default;
    virtual std::shared_ptr<Product> getProduct() const = 0;
    virtual int getQuantity() const = 0;
    virtual int getTotalPrice() const = 0;
};
```

**역할**: POS가 주문을 다루는 방법을 정의하는 계약. CardOrder 외에 다른 주문 유형도 추가 가능하다.

---

#### `CardOrder` — 명함 주문 (Order 구현체)

```
필드:
  - card       : shared_ptr<Card>   → 주문한 카드
  - calculator : Calculator         → 가격 계산기 (by value, 직접 포함)
  - quantity   : int                → 주문 수량

메서드:
  + CardOrder(card)         → 수량 없이 생성 (quantity = 0)
  + setQuantity(q)          → 수량 나중에 설정 가능 ★
  + getProduct()            → card 반환
  + getQuantity()           → quantity 반환
  + getTotalPrice()         → calculator.calcTotal() 에 위임
  + getDiscountMsg()        → calculator.getDiscountMsg() 에 위임
  + getCard()               → 캐스팅 없이 Card 직접 접근
```

> **왜 수량 없이 생성하나?**  
> 명세의 시나리오 순서: `이름 입력 → 미리보기 → 테두리 변경 → 수량 입력`  
> CardOrder를 만들 때 아직 수량을 모른다. 수량은 나중에 `setQuantity()`로 설정한다.

---

### 2-6. I/O 레이어

#### `Input` / `Output` — 추상 클래스

```cpp
class Input {
    virtual std::string readLine() = 0;   // 한 줄 읽기
};

class Output {
    virtual void writeLine(const std::string& msg) = 0;  // 한 줄 쓰기
};
```

**역할**: POS가 입출력 방식에 의존하지 않도록 추상화. 같은 POS 로직으로 실제 사용자 입력과 테스트 입력을 모두 처리할 수 있다.

---

#### 구현체 4가지

| 클래스 | 용도 | 구현 방식 |
|---|---|---|
| `UserInput` | 실제 사용 시 | `std::getline(std::cin, line)` |
| `UserOutput` | 실제 사용 시 | `std::cout << msg` |
| `TestInput` | 자동 테스트 시 | `queue<string>`에서 순서대로 꺼냄 |
| `TestOutput` | 자동 테스트 시 | `vector<string>`에 누적 후 확인 |

**TestInput/TestOutput 사용 예시**:

```cpp
auto in  = std::make_shared<TestInput>();
auto out = std::make_shared<TestOutput>();

in->pushInput("Jonathan Peter Macdonald");  // 이름 입력 미리 세팅
in->pushInput("OK");                        // 테두리 확인 입력
in->pushInput("200");                       // 수량 입력

auto pos = std::make_shared<VendingMachinePOS>(in, out);
pos->runScenario();

// 결과 검증
auto& lines = out->getOutputs();
assert(lines.back() == "9000");
```

---

### 2-7. POS 레이어

#### `POS` — 추상 클래스

```
필드 (protected):
  - input  : shared_ptr<Input>
  - output : shared_ptr<Output>

메서드:
  + POS(i, o)              → Input/Output 주입 (DI)
  + addOrder(order)  = 0   → 주문 추가
  + calculateTotal() = 0   → 전체 합계 계산
  + getOrders()      = 0   → 주문 목록 조회
  + runScenario()    = 0   → 전체 시나리오 실행
```

**역할**: 자판기 POS의 추상적 골격. 다른 종류의 POS(키오스크, 웹 주문 등)를 만들 때 이 클래스를 상속한다.

---

#### `VendingMachinePOS` — 자판기 POS 구현체

```
필드:
  - orders : vector<shared_ptr<Order>>   → 주문 목록

메서드:
  + VendingMachinePOS(i, o)
  + addOrder(order)     → orders에 추가
  + calculateTotal()    → 모든 order의 getTotalPrice() 합산
  + getOrders()         → orders 반환
  + runScenario()       → 전체 대화형 시나리오 실행 ★
```

`runScenario()`는 input/output을 통해 사용자와 대화하며 전체 흐름을 진행한다.

---

## 3. 클래스 간 관계 정리

### 상속 관계 (실선 삼각형 화살표)

```
Text            ←──── NameText
CardStencil     ←──── CardStencil1
Product         ←──── Card
Order           ←──── CardOrder
POS             ←──── VendingMachinePOS
Input           ←──── UserInput, TestInput
Output          ←──── UserOutput, TestOutput
```

### 집합/포함 관계 (다이아몬드 화살표)

| 소유자 | 포함 대상 | 종류 | 기호 |
|---|---|---|---|
| Card | CardStencil | 집약 (shared_ptr) | ◇── |
| Card | NameText | 집약 (shared_ptr) | ◇── |
| CardOrder | Card | 집약 (shared_ptr) | ◇── |
| CardOrder | Calculator | 합성 (by value) | ◆── |
| POS | Input | 집약 (shared_ptr) | ◇── |
| POS | Output | 집약 (shared_ptr) | ◇── |
| VendingMachinePOS | Order 목록 | 집약 (vector) | ◇── |

> **집약(◇) vs 합성(◆)**  
> - 집약: 소유자가 사라져도 대상이 살아있을 수 있다 (shared_ptr로 공유 가능)  
> - 합성: 소유자가 사라지면 대상도 함께 사라진다 (CardOrder 안에 Calculator가 by value로 박혀 있음)

---

## 4. 실행 흐름 (runScenario 기준)

`VendingMachinePOS::runScenario()`가 호출될 때 실제로 어떤 순서로 객체들이 동작하는지 추적한다.

### Step 1 — 이름 입력 및 유효성 검사

```
사용자: "Jonathan Peter Macdonald"
         ↓
VendingMachinePOS::runScenario()
  → output->writeLine("Enter name:")
  → name = input->readLine()              // "Jonathan Peter Macdonald"
  → NameText text(name)
      parse() 실행:
        tokens = ["Jonathan", "Peter", "Macdonald"]
        formattedName = "Jonathan P Macdonald"
        initials      = "JPM"
        valid         = true
  → text.checkValid() == true  →  계속
```

### Step 2 — Card 생성 (팩토리)

```
Card::create("Jonathan Peter Macdonald")
  내부:
    1. auto stencil = make_shared<CardStencil1>()
    2. auto text    = make_shared<NameText>("Jonathan Peter Macdonald")
    3. return make_shared<Card>(stencil, text)
```

### Step 3 — 샘플 카드 미리보기

```
card->toString()
  → stencil->render("Jonathan P Macdonald", "JPM")
      CardStencil1::render() 실행:
        line1    = "JPM" + "**"×26 + "JPM"
        empty    = "*" + " "×30 + "*"
        nameLine = "*" + " "×5 + "Jonathan P Macdonald" + " "×5 + "*"
        return line1 + empty + nameLine + empty + line1

output->writeLine(카드 미리보기)
```

### Step 4 — 테두리 변경 (반복 가능)

```
output->writeLine("Enter OK or border char:")
border = input->readLine()

if border == "OK":
    continue
else:
    card->setBorder(border[0])
        → stencil->setBorderChar('+')
    다시 미리보기 출력
    반복
```

### Step 5 — 주문 생성 및 수량 입력

```
auto order = make_shared<CardOrder>(card)
    quantity = 0  (수량은 아직 모름)

output->writeLine("How many cards?")
qty = input->readLine()    // "200"
order->setQuantity(200)
```

### Step 6 — 가격 계산 및 출력

```
order->getTotalPrice()
  → calculator.calcTotal(
        card->getDisplayNameLength(),   // "Jonathan P Macdonald".length() = 20
        200                             // quantity
    )
    → calcPricePerCard(20) = 50    (20 > 12)
    → 50 × 200 = 10000
    → 200 ≥ 200  →  10000 × 0.9 = 9000
    → return 9000

order->getDiscountMsg()
  → calculator.getDiscountMsg(200)
  → return "10% discount applied"

output->writeLine("Total: 9000")
output->writeLine("10% discount applied")
```

### Step 7 — 주문 등록 및 다음 주문 or 종료

```
pos->addOrder(order)
    → orders.push_back(order)

사용자에게 추가 주문 여부 확인
  "Y" → Step 1로 돌아가 반복
  "N" → 전체 합계 출력 후 종료
```

### 전체 흐름 요약 다이어그램

```
[사용자]                    [VendingMachinePOS]          [Card]          [CardOrder]      [Calculator]
   |                                |                        |                  |               |
   |── 이름 입력 ──────────────────>|                        |                  |               |
   |                        create(name) ──────────────────>|                  |               |
   |                                |<── card ──────────────|                  |               |
   |<── 샘플 카드 출력 ─────────────|── toString() ─────────>|                  |               |
   |                                |<── 카드 문자열 ────────|                  |               |
   |── "OK" 또는 테두리 문자 ───────>|                        |                  |               |
   |                                |── setBorder() ─────────>|                  |               |
   |── 수량 입력 ──────────────────>|                        |                  |               |
   |                         new CardOrder(card) ──────────────────────────────>|               |
   |                                |── setQuantity(200) ────────────────────────>|               |
   |                                |── getTotalPrice() ─────────────────────────>|               |
   |                                |                        |        calcTotal() ──────────────>|
   |                                |                        |<─────────────────── 9000 ─────────|
   |<── "Total: 9000" ─────────────|                        |                  |               |
   |<── "10% discount applied" ────|                        |                  |               |
```

---

## 5. 원설계 대비 핵심 변경점

### 변경점 요약표

| 항목 | 원설계 | 수정 설계 | 이유 |
|---|---|---|---|
| Product 인터페이스 | `getName()`, `getPrice()`, `toString()` | `toString()` 만 | 가격/이름은 Product 책임이 아님 |
| Card 생성 | 3단계 수동 조립 | `Card::create(name)` | 생성 책임 캡슐화 |
| Card 생성자 | `Card(s, t, int p=0)` | `Card(s, t)` | 더미 파라미터 제거 |
| 가격 계산 위치 | Card.getPrice() + CardOrder.getTotalPrice() | Calculator 전담 | 단일 책임 원칙 |
| 할인 메시지 | 없음 | `CardOrder.getDiscountMsg()` | 명세 요구사항 반영 |
| 수량 설정 | 생성자에서만 가능 | `setQuantity()`로 나중에 가능 | 대화형 흐름 지원 |
| NameText | 3단어만 유효 | 2단어/3단어 모두 유효 | 버그 수정 |
| getDisplayNameLength() | 없음 | NameText에 추가 | Calculator 가격 계산 연결 |

### 설계 원칙 적용

| 원칙 | 적용 내용 |
|---|---|
| **SRP** (단일 책임) | Calculator가 가격 정책 전담, Product는 렌더링만 |
| **OCP** (개방-폐쇄) | CardStencil, Input/Output의 추상화로 구현체 교체 가능 |
| **DIP** (의존성 역전) | VendingMachinePOS가 UserInput이 아닌 Input 추상에 의존 |
| **Factory 패턴** | `Card::create()`로 생성 캡슐화 |

---

## 6. 자주 헷갈리는 포인트 Q&A

**Q. `getDisplayNameLength()`와 `getRawInputLength()`의 차이는?**

> `getRawInputLength()`: 사용자가 타이핑한 원본 문자열 길이  
> (예: "Jonathan Peter Macdonald" → 24)  
> `getDisplayNameLength()`: 실제 카드에 찍히는 포맷된 이름 길이  
> (예: "Jonathan P Macdonald" → 20)  
> **가격 계산은 `getDisplayNameLength()` 기준**이다.

---

**Q. Calculator는 왜 CardOrder에 shared_ptr가 아닌 by value로 들어가나?**

> Calculator는 상태가 없는 순수 계산 클래스다. 상속이나 교체가 필요 없고, 공유할 이유도 없다. by value로 직접 포함하는 게 더 단순하고 명확하다.

---

**Q. 왜 Card도 추상 클래스가 아닌가?**

> 현재 명함 종류가 하나(CardStencil1 기반)뿐이다. 다른 종류의 명함이 생기면 CardStencil을 교체하는 것으로 해결한다. Card 자체를 상속하는 구조는 지금 단계에서 불필요하다.

---

**Q. TestInput/TestOutput은 왜 필요한가?**

> `std::cin`/`std::cout`을 직접 쓰면 자동화 테스트를 작성할 수 없다. Input/Output을 주입받는 구조이기 때문에, 테스트 시에는 TestInput/TestOutput으로 교체해 실제 키보드 입력 없이도 시나리오 전체를 검증할 수 있다.

---

**Q. `Card::create()`가 항상 CardStencil1을 쓰는데 유연성이 없지 않나?**

> 현재는 CardStencil1 한 종류만 있으므로 이것으로 충분하다. 나중에 `create(name, stencilType)` 같은 오버로드를 추가하거나, 팩토리를 별도 클래스로 분리할 수 있다. 지금은 최소한의 캡슐화가 목표다.

---

*문서 기준: `teammade-revised.drawio` 및 수정 설계 헤더*

---

---

# 흐름으로 읽는 설계 요약

UML을 처음 보는 팀원을 위한 가벼운 설명. 코드 디테일보다 "왜 이 구조인가"에 집중한다.

---

## 클래스를 역할별로 묶으면

```
이름을 파싱한다       →  NameText
카드를 그린다         →  CardStencil1
카드를 만든다         →  Card
가격을 계산한다       →  Calculator
주문을 관리한다       →  CardOrder
사용자와 대화한다     →  VendingMachinePOS
입출력을 추상화한다   →  Input / Output
```

추상 클래스(Text, CardStencil, Product, Order, POS, Input, Output)는 각 역할의 **계약**을 정의하고, 구체 클래스가 그 계약을 **구현**한다.

---

## 실행 흐름을 한 문장씩

1. 사용자가 이름을 입력하면 **NameText**가 파싱해서 포맷된 이름(`"Jonathan P Macdonald"`)과 이니셜(`"JPM"`)을 만든다.

2. **Card::create()** 를 호출하면 내부에서 NameText와 CardStencil1을 조립해 Card를 반환한다. 외부에서 직접 조립할 필요 없다.

3. **CardStencil1::render()** 가 이름과 이니셜을 받아 명함 모양 문자열을 그린다. 테두리 문자를 바꾸고 싶으면 `setBorder()`를 호출하면 된다.

4. **CardOrder** 는 카드를 받아 주문을 만든다. 수량은 나중에 `setQuantity()`로 설정한다. 생성 시점에 수량을 몰라도 된다.

5. 가격이 필요할 때 CardOrder가 **Calculator**에게 물어본다. Calculator가 이름 길이와 수량으로 장당 가격, 총액, 할인 여부를 한 곳에서 계산한다.

6. **VendingMachinePOS** 가 이 모든 흐름을 `runScenario()` 안에서 순서대로 진행한다. 입출력은 Input/Output 인터페이스를 통해서만 하기 때문에, 테스트할 때는 TestInput/TestOutput으로 교체하면 된다.

---

## 핵심 설계 결정 3가지

**1. Calculator를 따로 뺀 이유**
가격 계산 로직이 Card와 CardOrder에 흩어져 있었다. 한 곳으로 모으면 정책이 바뀌어도 Calculator만 수정하면 된다.

**2. Card::create()를 만든 이유**
Card를 만들려면 CardStencil1과 NameText를 직접 조립해야 했다. 팩토리 메서드가 이 조립을 숨겨준다. 사용하는 쪽은 이름 문자열만 넘기면 된다.

**3. Input/Output을 인터페이스로 뺀 이유**
`std::cin`/`std::cout`을 직접 쓰면 자동 테스트를 작성할 수 없다. 인터페이스로 분리하면 테스트 시 Mock으로 교체할 수 있다.
