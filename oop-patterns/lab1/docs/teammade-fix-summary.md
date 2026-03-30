# teammade 원본 수정 요약

## 상황

팀 설계(teammade)로 받은 코드가 요구 명세를 충족하지 못하는 상태.
원본은 `teammade-origin/`에 보관, `teammade/`에서 수정 작업 진행 중.

---

## 원본이 명세를 못 맞추는 이유

요구 명세 (`OO Design and Patterns - Lab1`)의 핵심 요구사항과 원본의 불일치:

### 1. 2단어 이름 거부 — 명세 위반

- 명세: `"Barbara Thomson"` (2단어) 입력 허용
- 원본: `tokens.size() != 3`이면 무조건 invalid → 2단어 이름 처리 불가

### 2. 할인 미적용 — 명세 위반

- 명세: 200장 이상 10% 할인, "10% discount applied" 출력
- 원본 `CardOrder::getTotalPrice()`: `card->getPrice() * quantity`만 반환, 할인 로직 없음
- 할인 메시지를 출력할 메서드 자체가 없음

### 3. 대화형 흐름 구현 불가

- 명세 흐름: 이름 입력 → 카드 미리보기 → 테두리 변경 → 수량 입력
- 원본 `CardOrder(card, quantity)`: 생성 시점에 수량 필수 → 위 흐름대로 코딩할 수 없음

---

## 수정 내용

### 코딩 버그 수정

| 파일 | 수정 |
|---|---|
| Text.h | 2단어 이름 처리 추가 |
| CardOrder.h | 200장 이상 10% 할인 로직 추가 |

### 설계 개선

| 변경 | 요약 |
|---|---|
| **Calculator 신설** | 가격 정책(장당 가격, 총액, 할인, 할인 메시지)을 한 클래스에 응집 |
| **CardOrder 생성자** | `CardOrder(card)` — 수량 없이 생성, `setQuantity()`로 나중에 설정 |
| **Card::create()** | 정적 팩토리 메서드 추가, 3단계 조립 → 1줄 생성 |
| **Product 정리** | `getName()`/`getPrice()` 제거, `toString()` 렌더링만 남김 |

---

## 관련 파일

- `teammade-origin/` — 팀메이드 원본 코드 (수정 전)
- `teammade/` — 수정된 코드
- `teammade-design-review.md` — 설계 문제 상세 분석 및 해결 방안
- `teammade-revised.drawio` — 수정 반영 UML 다이어그램
