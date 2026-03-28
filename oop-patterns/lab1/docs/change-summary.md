# 변경 사항 요약

---

## 1. 코딩/로직 수정 (이미 적용됨)

### Text.h

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| 멤버 변수 | private 블록 2개, 변수 중복 선언 | private 블록 1개로 통합 |
| parse() | 2번 정의, 두 번째는 잘려있음 | 1개로 통합, 완전한 구현 |
| 2단어 이름 | tokens.size() != 3이면 무조건 invalid | tokens.size() == 2도 valid 처리 |

### CardOrder.h

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| getTotalPrice() | price * quantity만 반환 | 200장 이상일 때 10% 할인 적용 |

---

## 2. 설계 개선 (다이어그램에 반영됨)

### 신규: Calculator 클래스

```
Calculator
+ calcPricePerCard(len: int): int
+ calcTotal(len: int, qty: int): int
+ hasDiscount(qty: int): bool
+ getDiscountMsg(qty: int): string
```

가격 정책(장당 가격, 총액, 할인, 할인 메시지)을 한 클래스에 응집시킴.

### 수정: Product

```
변경 전                변경 후
───────────            ───────────
+ getName(): string    삭제
+ getPrice(): int      삭제
+ toString(): string   유지
```

렌더링(toString) 역할만 남기고 가격 관련 메서드 제거.

### 수정: Card

```
변경 전                          변경 후
───────────────────              ───────────────────
+ Card(s, t, int p=0)           + Card(s, t)              더미 파라미터 제거
+ getName(): string             삭제                       Product에서 제거됨
+ getPrice(): int               삭제                       Calculator로 이관
                                + static create(name)      팩토리 메서드 추가
                                + getDisplayNameLength()   가격 계산용 길이 제공
```

### 수정: CardOrder

```
변경 전                          변경 후
───────────────────              ───────────────────
  CardOrder(card, qty)             CardOrder(card)        수량 없이 생성
  quantity 불변                    + setQuantity()         나중에 수량 설정
  getTotalPrice() 직접 계산        getTotalPrice()         Calculator에 위임
  할인 메시지 없음                 + getDiscountMsg()      Calculator에 위임
                                   - calculator: Calculator 필드 추가
```

### 수정: NameText

```
변경 전              변경 후
───────────          ───────────
(기존 메서드 유지)   + getDisplayNameLength(): int  추가
```

---

## 3. 변경 이유 요약

| 설계 문제 | 해결 방법 |
|---|---|
| 가격 책임이 Card와 CardOrder에 분산 | Calculator로 응집 |
| Order 생성 시 수량 필수 (대화형 흐름 불가) | setQuantity() 추가, 수량 없이 생성 |
| Card 생성에 3단계 조립 필요 (팩토리 없음) | Card::create() 정적 팩토리 추가 |
| Product의 getName()/getPrice()가 역할에 안 맞음 | 제거, 렌더링에 집중 |

---

## 4. 관련 파일

- `teammade-design-review.md` — 설계 문제 상세 분석 및 해결 방안
- `teammade-revised.drawio` — 수정 반영된 UML 클래스 다이어그램
- `generate-diagram.ps1` — 다이어그램 생성 스크립트
