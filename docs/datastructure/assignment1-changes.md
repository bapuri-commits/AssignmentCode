# multPoly.c 수정 내역

## 원본 코드 구조

원래 작성했던 코드는 다음과 같은 구조였다:
- `polynomial` 구조체 (coef, xexp, yexp, zexp)
- `Teamgroup` 구조체 (factors 배열 + factorsnum)
- 4개 test case 데이터 (A1~A4, B1~B4)
- `compare_y` — qsort용 비교 함수
- `attach` — terms 배열에 항 추가
- `Preprocess` — Teamgroup의 팩터들을 하나의 polynomial로 합침
- `multPoly` — 미완성, 덧셈 로직으로 작성됨
- `main` — test case 1만 준비, multPoly 호출 없음

---

## 수정 1: Teamgroup 팩터 분리 (채점 기준 3번)

### 문제
`15x^4 * y^7` 같은 항을 `{15, 4, 7, 0}` 하나의 팩터로 저장했다.
이미 합쳐진 형태이므로 "계수를 앞으로 빼지 않고 안에서 계산" 조건을 위반한다.

### 원본
```c
// 15x^4 * y^7 → 팩터 1개로 이미 합쳐짐
{ {{15, 4, 7, 0}, {1, 0, 0, 0}}, 2 }
```

### 수정
```c
// 15x^4 * y^7 → 15x^4와 y^7을 별도 팩터로 분리
{ {{15, 4, 0, 0}, {1, 0, 7, 0}}, 2 }
```

### 영향받는 데이터
| 항 | 원본 팩터 | 수정 팩터 |
|---|---|---|
| `15x^4 * y^7` (A3) | `{15,4,7,0}, {1,0,0,0}` | `{15,4,0,0}, {1,0,7,0}` |
| `-12x^4 * y^2` (B3) | `{-12,4,2,0}, {1,0,0,0}` | `{-12,4,0,0}, {1,0,2,0}` |
| `15x^4 * y^7 * 6z^3` (A4) | `{15,4,7,0}, {6,0,0,3}` | `{15,4,0,0}, {1,0,7,0}, {6,0,0,3}` |
| `-12x^4 * y^2 * z^2` (B4) | `{-12,4,2,0}, {1,0,0,2}` | `{-12,4,0,0}, {1,0,2,0}, {1,0,0,2}` |

수학적 결과는 동일하지만, 팩터가 문제에 쓰인 형태 그대로 분리되어 있어
곱셈 과정에서 자연스럽게 합쳐진다.

---

## 수정 2: Preprocess 함수 제거

### 문제
`Preprocess`는 multPoly 호출 전에 Teamgroup 내부 팩터들을 미리 곱해서
하나의 polynomial로 만들었다. 이것이 바로 "계수를 앞으로 빼는" 행위다.

### 원본
```c
int Preprocess(Teamgroup a[], int size) {
    int start = avail;
    polynomial result[size];
    for (int i = 0; i < size; i++) {
        Teamgroup cur = a[i];
        polynomial processing = { 1, 0, 0, 0 };
        for (int j = 0; j < cur.factorsnum; j++) {
            processing.coef *= cur.factors[j].coef;
            processing.xexp += cur.factors[j].xexp;
            processing.yexp += cur.factors[j].yexp;
            processing.zexp += cur.factors[j].zexp;
        }
        result[i] = processing;
    }
    qsort(result, size, sizeof(polynomial), compare_y);
    for (int i = 0; i < size; i++)
        attach(result + i);
    return start;
}
```

### 수정
함수 자체를 삭제했다. 팩터 결합은 multPoly 안에서 A*B 곱셈과 동시에 수행된다.

---

## 수정 3: multPoly 함수 전면 재작성

### 문제
원본 multPoly는 **덧셈** 로직(교재 poly_add2)을 그대로 가져왔고,
변수명 불일치(As/as, Bs/bs 혼용), 미완성 상태였다.

### 원본
```c
void multPoly(int as, int ae, int be, int* cs, int* ce) {
    float tempcoef;
    polynomial add;
    *cs = avail;
    while (As <= Ae && Bs <= Be) {
        if (compare_y(terms[As], terms[Bs]) > 0) {
            attach(terms[as]); As++;
        }
        else if (compare_y(terms[As], terms[Bs]) == 0) {
            if (terms[As].xexp == terms[Bs].xexp && ...) {
                add.coef = tempcoef;
            }
            tempcoef = terms[As].coef + terms[Bs].coef;
            if (tempcoef) {
                // 미완성
            }
        }
    }
}
```

문제점:
- 시그니처에 `bs` 파라미터 누락
- `As`/`as` 대소문자 혼용 (컴파일 에러)
- `compare_y`에 포인터가 아닌 값 전달 (타입 불일치)
- `attach`에 포인터가 아닌 값 전달 (타입 불일치)
- 덧셈 로직이지 곱셈이 아님
- 동류항 결합 없음

### 수정
```c
void multPoly(Teamgroup A[], int sizeA, Teamgroup B[], int sizeB, int *cs, int *ce)
```

새 로직은 3단계:

**1단계 — 항별 곱셈**
A의 각 Teamgroup × B의 각 Teamgroup. 양쪽의 모든 팩터를 순회하며
계수는 곱하고 지수는 더한다. 결과를 지역 배열 temp[]에 저장.

**2단계 — 동류항 결합**
temp[]에서 (xexp, yexp, zexp)가 같은 항들의 계수를 합산.
합산 결과가 0이 아닌 항만 attach()로 전역 terms[]에 저장.

**3단계 — qsort 정렬**
결과 구간 terms[cs..ce]를 y 내림차순으로 정렬.

---

## 수정 4: attach 함수 시그니처 변경

### 원본
```c
void attach(polynomial* d)
{
    terms[avail].coef = d->coef;
    terms[avail].xexp = d->xexp;
    terms[avail].yexp = d->yexp;
    terms[avail].zexp = d->zexp;
    avail++;
}
```

### 수정
```c
void attach(float coef, int xexp, int yexp, int zexp)
{
    terms[avail].coef = coef;
    terms[avail].xexp = xexp;
    terms[avail].yexp = yexp;
    terms[avail].zexp = zexp;
    avail++;
}
```

동류항 결합 후 합산된 계수를 바로 전달하기 위해 개별 값으로 받도록 변경.
경계 검사도 `> MAX_TERMS`에서 `>= MAX_TERMS`로 수정 (off-by-one 방지).

---

## 수정 5: print_poly 함수 추가

원본에는 출력 함수가 없었다. 다변수 다항식 출력을 위해 새로 작성:

- 부호 처리: 첫 항은 그대로, 이후 양수면 ` + `, 음수면 ` - ` 구분자
- 지수 0인 변수는 출력 생략 → 상수항 자동 처리 (채점 기준 5번)
- 지수 1이면 `x`, 2 이상이면 `x^n` 형식
- 결과가 비어 있으면 `0` 출력

---

## 수정 6: main 함수 완성

### 원본
```c
int main(void) {
    int As = Preprocess(A1, sizeof(A1) / sizeof(A1[0]));
    int Ae = avail - 1;
    int Bs = Preprocess(B1, sizeof(B1) / sizeof(B1[0]));
    int Be = avail - 1;
    int Cs, Ce;
    return 0;
}
```

test case 1의 Preprocess만 호출하고, multPoly도 호출하지 않고, 출력도 없었다.

### 수정
- 4개 test case 모두 실행
- 각 test case마다 `avail = 0`으로 초기화 (이전 결과 간섭 방지)
- A/B 입력 다항식과 곱셈 결과를 printf로 출력

---

## 변경되지 않은 부분

- `polynomial` 구조체 정의 (coef, xexp, yexp, zexp)
- `Teamgroup` 구조체 정의 (factors[10], factorsnum)
- `compare_y` 비교 함수 (y 내림차순)
- test case 1, 2의 데이터 (단일 팩터라 변경 불필요)
- 전역 변수 `terms[MAX_TERMS]`, `avail`
