# Prompt A1 — 기반 계층 (데이터 · 유틸 · 인터페이스)

> MakeAGraph — Java 콘솔 대화형 모드 구현. 3개 프롬프트 중 **1/3**.
> 이 프롬프트는 단독 실행. 이전 코드 첨부 불필요.

## 패키지 구조

```
src/main/java/makeagraph/
├── data/         Point, ScatterPlotData, BarGraphData, GraphMetadata, ObserverSupport
├── util/         Direction, Range, Pair, ScaleCalculator
├── observer/     IGraphObserver
├── graph/        IGraph, IDataAppendable, IBatchAppendable, IViewControllable
├── drawer/       ITitle, IAxis, IAxisPlot, IScatterDrawer, IBarDrawer
├── input/        IInputSource, ITypeSelector, IGraphDataInputHandler, IDataParser
├── renderer/     IRenderer
└── session/      ISession
```

총 25개 파일 생성.

---

## 유틸 — 4 files (makeagraph.util)

### Direction

```java
public enum Direction { VERTICAL, HORIZONTAL }
```

### Range — 불변

```java
public class Range {
    private final double min, max;
    public Range(double min, double max) { ... }
    public double getMin() { ... }
    public double getMax() { ... }
}
```

### Pair\<A, B\> — 불변

```java
public class Pair<A, B> {
    private final A first;
    private final B second;
    public Pair(A first, B second) { ... }
    public A getFirst() { ... }
    public B getSecond() { ... }
}
```

### ScaleCalculator

```java
public class ScaleCalculator {

    /**
     * nice interval 기반 축 범위 계산.
     *  1) dataMin/Max 추출
     *  2) calcInterval()로 눈금 간격 결정
     *  3) axisMin = floor(dataMin / interval) * interval
     *     axisMax = ceil(dataMax / interval) * interval
     */
    public Range calcRange(List<Double> values) { ... }

    /**
     * nice interval 계산.
     *  roughInterval = rawRange / 5
     *  magnitude = 10^floor(log10(roughInterval))
     *  1, 2, 5, 10 중 roughInterval에 가장 가까운 step 선택
     */
    public double calcInterval(Range range) { ... }

    /** range 내 눈금 위치를 문자열 목록으로 반환 */
    public List<String> getTickLabels(Range range) { ... }

    /** col = (int)((x - xMin) / (xMax - xMin) * (gridWidth - 1)) */
    public int toCol(double x, double xMin, double xMax, int gridWidth) { ... }

    /** row = gridHeight - 1 - (int)((y - yMin) / (yMax - yMin) * (gridHeight - 1)) ← y축 반전 */
    public int toRow(double y, double yMin, double yMax, int gridHeight) { ... }
}
```

---

## 데이터 · 옵저버 — 6 files

### IGraphObserver (makeagraph.observer)

```java
public interface IGraphObserver {
    void onDataChanged();
}
```

### ObserverSupport (makeagraph.data) — has-a 헬퍼

```java
public class ObserverSupport {
    private final List<IGraphObserver> observers = new ArrayList<>();
    private boolean suspended = false;
    private boolean dirty = false;

    public void addObserver(IGraphObserver o) { observers.add(o); }

    public void notifyObservers() {
        if (suspended) { dirty = true; return; }
        for (IGraphObserver o : observers) o.onDataChanged();
    }

    public void suspend() { suspended = true; dirty = false; }

    public void resume() {
        suspended = false;
        if (dirty) { dirty = false; notifyObservers(); }
    }
}
```

### Point (makeagraph.data) — 불변, 팩토리

```java
public class Point {
    private final List<Double> coords; // Collections.unmodifiableList

    private Point(List<Double> coords) {
        this.coords = Collections.unmodifiableList(new ArrayList<>(coords));
    }

    public double get(int index) { return coords.get(index); }
    public int size() { return coords.size(); }

    public static Point of(double... values) {
        List<Double> list = new ArrayList<>();
        for (double v : values) list.add(v);
        return new Point(list);
    }
}
```

### ScatterPlotData (makeagraph.data) — Subject

```java
public class ScatterPlotData {
    private final List<Point> points = new ArrayList<>();
    private final ObserverSupport observerSupport = new ObserverSupport();

    public void append(Point point) { points.add(point); observerSupport.notifyObservers(); }
    public List<Point> getPoints() { return Collections.unmodifiableList(points); }
    public void addObserver(IGraphObserver o) { observerSupport.addObserver(o); }
    public void suspendObservers() { observerSupport.suspend(); }
    public void resumeObservers() { observerSupport.resume(); }
}
```

### BarGraphData (makeagraph.data) — Subject

```java
public class BarGraphData {
    private final List<Pair<String, Double>> bars = new ArrayList<>();
    private final Direction direction; // 생성자 주입, 불변
    private final ObserverSupport observerSupport = new ObserverSupport();

    public BarGraphData(Direction direction) { this.direction = direction; }

    public void append(Pair<String, Double> bar) { bars.add(bar); observerSupport.notifyObservers(); }
    public List<Pair<String, Double>> getBars() { return Collections.unmodifiableList(bars); }
    public Direction getDirection() { return direction; }
    public void addObserver(IGraphObserver o) { observerSupport.addObserver(o); }
    public void suspendObservers() { observerSupport.suspend(); }
    public void resumeObservers() { observerSupport.resume(); }
}
```

### GraphMetadata (makeagraph.data) — 불변

```java
public class GraphMetadata {
    private final String title, xLabel, yLabel;
    public GraphMetadata(String title, String xLabel, String yLabel) { ... }
    public String getTitle() { ... }
    public String getXLabel() { ... }
    public String getYLabel() { ... }
}
```

---

## 인터페이스 — 그래프 계층 (4 files, makeagraph.graph)

```java
public interface IGraph<T> {
    void draw();
}
```

```java
public interface IDataAppendable<E> {
    void appendData(E element);
}
```

```java
public interface IBatchAppendable<E> extends IDataAppendable<E> {
    void suspendObservers();
    void resumeObservers();
}
```

```java
public interface IViewControllable {
    void swapAxes(int a, int b);
    void setView(int[] axes);
    List<int[]> getAvailableViews();
}
```

---

## 인터페이스 — Drawer 계층 (5 files, makeagraph.drawer)

```java
public interface ITitle {
    List<String> drawTitle(GraphMetadata metadata);
}
```

```java
public interface IAxis<T> {
    List<String> drawAxis(T data, GraphMetadata metadata, int[] axisMapping);
}
```

```java
public interface IAxisPlot<T> {
    List<String> drawPlot(T data, int[] axisMapping);
}
```

```java
// 교차 인터페이스
public interface IScatterDrawer extends ITitle, IAxis<ScatterPlotData>, IAxisPlot<ScatterPlotData> {}
```

```java
public interface IBarDrawer extends ITitle, IAxis<BarGraphData>, IAxisPlot<BarGraphData> {}
```

---

## 인터페이스 — 입력 계층 (4 files, makeagraph.input)

```java
public interface IInputSource {
    Object readObject();              // readObject("")에 위임하는 편의 메서드
    Object readObject(String prompt); // prompt 출력 후 입력 읽기
    void close();
}
```

```java
public interface ITypeSelector {
    String selectType(IInputSource source);
}
```

```java
@FunctionalInterface
public interface IDataParser<E> {
    E parse(String raw);
}
```

```java
public interface IGraphDataInputHandler<T, E> {
    T readData(IInputSource source);
    GraphMetadata readMetadata(IInputSource source);
    E parseData(IInputSource source);
    IDataParser<E> createParser();
}
```

- **호출 순서 계약**: `readData()` → `readMetadata()`. `createParser()`는 `readData()` 이후 호출 가능 (내부 상태 의존).

---

## 인터페이스 — 출력 · 세션 (2 files)

```java
// makeagraph.renderer
public interface IRenderer {
    void print(List<String> lines);
}
```

```java
// makeagraph.session
public interface ISession {
    void start(); // 논블로킹 — 내부 스레드 생성 후 즉시 반환
    void stop();
    void await(); // 내부 스레드 join
}
```

---

## 주의사항

1. 모든 클래스/인터페이스에 올바른 `package` 선언과 필요한 `import` 포함
2. `Pair`는 **`makeagraph.util.Pair`** 사용 — `javafx.util.Pair` 사용 금지
3. `ObserverSupport.resume()`: dirty였을 때만 `notifyObservers()` 1회 호출
4. `ScaleCalculator.toRow()`에서 **y축 반전** (`gridHeight - 1 - ...`)
5. `IGraphDataInputHandler` 호출 순서: `readData()` → `readMetadata()` (createParser는 readData 이후)
6. `BarGraphData.direction`은 생성자 주입, 이후 불변
7. `GraphMetadata`도 불변 (setter 없음)
8. `IInputSource.readObject()`는 `readObject("")`에 위임하는 편의 메서드
