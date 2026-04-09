# Prompt 01 — 데이터 · 유틸 계층

> 이 프롬프트는 MakeAGraph 프로젝트의 **1단계: 데이터/유틸 계층** 구현을 요청합니다.
> 설계 문서: `docs/MakeAGraph_설계정리_Final_v4_9_3_.md`

---

## 지시

아래 명세에 따라 Java 클래스를 구현해 주세요.
**콘솔 대화형 모드만 구현 대상**입니다 (실시간 스트림, GUI 관련 코드는 제외).

패키지 구조는 다음을 따릅니다:

```
src/main/java/makeagraph/
├── data/
│   ├── Point.java
│   ├── ScatterPlotData.java
│   ├── BarGraphData.java
│   ├── GraphMetadata.java
│   └── ObserverSupport.java
├── util/
│   ├── Range.java
│   ├── ScaleCalculator.java
│   └── Direction.java
└── observer/
    └── IGraphObserver.java
```

---

## 구현 대상 (8개 파일)

### 1. `Direction` (열거형)

```java
package makeagraph.util;

public enum Direction {
    VERTICAL, HORIZONTAL
}
```

### 2. `Range` (유틸)

```java
package makeagraph.util;

public class Range {
    private final double min;
    private final double max;

    public Range(double min, double max) { ... }
    public double getMin() { ... }
    public double getMax() { ... }
}
```

- 불변 클래스.

### 3. `Point` (데이터)

```java
package makeagraph.data;

import java.util.List;

public class Point {
    private final List<Double> coords;

    private Point(List<Double> coords) { ... }

    public double get(int index) { ... }
    public int size() { ... }

    public static Point of(double... values) {
        // values를 List<Double>로 변환하여 불변 리스트로 저장
    }
}
```

- n차원 좌표. `coords`는 불변 리스트로 저장.
- `of()` 팩토리 메서드로만 생성.

### 4. `IGraphObserver` (인터페이스)

```java
package makeagraph.observer;

public interface IGraphObserver {
    void onDataChanged();
}
```

### 5. `ObserverSupport` (옵저버 관리 헬퍼)

```java
package makeagraph.data;

import makeagraph.observer.IGraphObserver;
import java.util.List;
import java.util.ArrayList;

public class ObserverSupport {
    private final List<IGraphObserver> observers = new ArrayList<>();
    private boolean suspended = false;
    private boolean dirty = false;

    public void addObserver(IGraphObserver observer) { ... }

    public void notifyObservers() {
        // suspended 상태면 dirty=true로만 기록
        // 아니면 모든 옵저버의 onDataChanged() 호출
    }

    public void suspend() {
        // suspended=true, dirty=false
    }

    public void resume() {
        // suspended=false
        // dirty였으면 notifyObservers() 1회
        // dirty 아니면 무호출
    }
}
```

- `removeObserver()`는 의도적으로 생략 (YAGNI).
- 상속이 아닌 has-a 구성으로 Data 클래스에서 사용.

### 6. `ScatterPlotData` (Subject)

```java
package makeagraph.data;

import makeagraph.observer.IGraphObserver;

public class ScatterPlotData {
    private final List<Point> points = new ArrayList<>();
    private final ObserverSupport observerSupport = new ObserverSupport();

    public void append(Point point) {
        points.add(point);
        observerSupport.notifyObservers();
    }

    public List<Point> getPoints() {
        // 외부 변경 방지를 위해 unmodifiableList 반환 권장
    }

    public void addObserver(IGraphObserver o) {
        observerSupport.addObserver(o);
    }

    public void suspendObservers() {
        observerSupport.suspend();
    }

    public void resumeObservers() {
        observerSupport.resume();
    }
}
```

### 7. `BarGraphData` (Subject)

```java
package makeagraph.data;

import makeagraph.observer.IGraphObserver;
import makeagraph.util.Direction;
import makeagraph.util.Pair;

public class BarGraphData {
    private final List<Pair<String, Double>> bars = new ArrayList<>();
    private final Direction direction;  // 생성자 주입, 불변
    private final ObserverSupport observerSupport = new ObserverSupport();

    public BarGraphData(Direction direction) {
        this.direction = direction;
    }

    public void append(Pair<String, Double> bar) {
        bars.add(bar);
        observerSupport.notifyObservers();
    }

    public List<Pair<String, Double>> getBars() { ... }
    public Direction getDirection() { ... }

    public void addObserver(IGraphObserver o) { ... }
    public void suspendObservers() { ... }
    public void resumeObservers() { ... }
}
```

- `direction`은 생성자에서 주입하고 이후 변경하지 않음.
- `Pair<String, Double>`: Java 표준에 `Pair`가 없으므로 **간단한 Pair 클래스를 직접 구현**하거나, `Map.Entry`를 사용하거나, `record Pair<A, B>(A first, B second) {}`로 정의.

### 8. `GraphMetadata` (불변)

```java
package makeagraph.data;

public class GraphMetadata {
    private final String title;
    private final String xLabel;
    private final String yLabel;

    public GraphMetadata(String title, String xLabel, String yLabel) { ... }
    public String getTitle() { ... }
    public String getXLabel() { ... }
    public String getYLabel() { ... }
}
```

- 불변. setter 없음.

### 9. `ScaleCalculator` (스케일 계산)

```java
package makeagraph.util;

import java.util.List;

public class ScaleCalculator {

    public Range calcRange(List<Double> values) {
        // 1. dataMin, dataMax 추출
        // 2. calcInterval()로 nice interval 계산
        // 3. axisMin = floor(dataMin / interval) * interval
        //    axisMax = ceil(dataMax / interval) * interval
        // 반환: Range(axisMin, axisMax)
    }

    public double calcInterval(Range range) {
        // rawRange = max - min
        // targetTickCount = 5
        // roughInterval = rawRange / targetTickCount
        // magnitude = 10 ^ floor(log10(roughInterval))
        // nice step: 1, 2, 5, 10 중 roughInterval에 가장 가까운 값
        // return interval
    }

    public List<String> getTickLabels(Range range) {
        // range와 interval을 기반으로 눈금 라벨 생성
    }

    public int toCol(double x, double xMin, double xMax, int gridWidth) {
        // col = (int)((x - xMin) / (xMax - xMin) * (gridWidth - 1))
    }

    public int toRow(double y, double yMin, double yMax, int gridHeight) {
        // row = gridHeight - 1 - (int)((y - yMin) / (yMax - yMin) * (gridHeight - 1))
        // (y축은 위로 갈수록 값이 커지므로 반전)
    }
}
```

- nice interval 알고리즘 참고:
  - 데이터 범위 0~36, targetTickCount=5
  - roughInterval=7.2, magnitude=1, nice step → interval=10
  - axisMin=0, axisMax=40

---

## Pair 클래스 (선택)

Java 표준에 Pair가 없으므로, 간단하게 정의:

```java
package makeagraph.util;

public record Pair<A, B>(A first, B second) {}
```

또는 Java 16 미만이면:

```java
package makeagraph.util;

public class Pair<A, B> {
    private final A first;
    private final B second;

    public Pair(A first, B second) { this.first = first; this.second = second; }
    public A getFirst() { return first; }
    public B getSecond() { return second; }
}
```

---

## 주의사항

1. 모든 클래스에 `package` 선언 포함
2. 타입 힌팅/제네릭 정확히 적용
3. `ObserverSupport`의 `suspend()/resume()` 로직을 정확히 구현 — `resume()` 시 dirty 플래그 확인 후 조건부 `notifyObservers()`
4. `ScaleCalculator`의 좌표 변환에서 **y축 반전** 주의 (`toRow`에서 `gridHeight - 1 - ...`)
5. 이 단계에서는 인터페이스(`IGraph`, `IDataAppendable` 등)를 아직 구현하지 않음 — 2단계에서 진행
