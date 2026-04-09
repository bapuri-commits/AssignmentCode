# Prompt A2 — 핵심 구현 (Drawer · ViewController · 그래프 계층 · TextRenderer)

> MakeAGraph 프로젝트 **2/3**.
> **필수**: A1에서 생성한 모든 Java 파일을 컨텍스트로 첨부.

## 생성할 파일

```
src/main/java/makeagraph/
├── drawer/
│   ├── ScatterPlotDrawer.java
│   └── BarGraphDrawer.java
├── graph/
│   ├── ViewController.java
│   ├── AbstractGraph.java
│   ├── AbstractAxisGraph.java
│   ├── ScatterPlot.java
│   └── BarGraph.java
└── renderer/
    └── TextRenderer.java
```

총 8개 파일 생성.

---

## ScatterPlotDrawer (makeagraph.drawer)

```java
public class ScatterPlotDrawer implements IScatterDrawer {
    private final ScaleCalculator scaleCalc;
    private final int gridWidth, gridHeight;
    private static final int DEFAULT_WIDTH = 60, DEFAULT_HEIGHT = 20;

    public ScatterPlotDrawer(ScaleCalculator sc) { this(sc, DEFAULT_WIDTH, DEFAULT_HEIGHT); }
    public ScatterPlotDrawer(ScaleCalculator sc, int gridWidth, int gridHeight) { ... }

    @Override
    public List<String> drawTitle(GraphMetadata metadata) {
        // 제목 문자열 + 빈 줄 반환
    }

    @Override
    public List<String> drawAxis(ScatterPlotData data, GraphMetadata metadata, int[] axisMapping) {
        // 축 레이블 매핑 규칙 (axisMapping 기반):
        //   가로축 레이블: axisMapping[0]==0 → getXLabel(), ==1 → getYLabel(), >=2 → "축"+axisMapping[0]
        //   세로축 레이블: axisMapping[1]==0 → getXLabel(), ==1 → getYLabel(), >=2 → "축"+axisMapping[1]
        //   전치({1,0}) 시 레이블 교차는 의도된 동작
        // 눈금: scaleCalc.getTickLabels() 사용
    }

    @Override
    public List<String> drawPlot(ScatterPlotData data, int[] axisMapping) {
        // 1. xValues = points.stream().map(p -> p.get(axisMapping[0])).collect(toList())
        //    yValues = points.stream().map(p -> p.get(axisMapping[1])).collect(toList())
        // 2. xRange = scaleCalc.calcRange(xValues)
        //    yRange = scaleCalc.calcRange(yValues)
        // 3. char[gridHeight][gridWidth] grid, fill ' '
        // 4. for each Point: col = toCol(...), row = toRow(...)
        //    grid[row][col] = '*'   ← 중복 좌표는 덮어쓰기 (의도된 동작)
        // 5. grid 행을 List<String>으로 반환
    }
}
```

---

## BarGraphDrawer (makeagraph.drawer)

```java
public class BarGraphDrawer implements IBarDrawer {
    private final ScaleCalculator scaleCalc;
    private final int gridWidth, gridHeight;
    private static final int DEFAULT_WIDTH = 60, DEFAULT_HEIGHT = 20;

    public BarGraphDrawer(ScaleCalculator sc) { this(sc, DEFAULT_WIDTH, DEFAULT_HEIGHT); }
    public BarGraphDrawer(ScaleCalculator sc, int gridWidth, int gridHeight) { ... }

    @Override
    public List<String> drawTitle(GraphMetadata metadata) { /* 제목 + 빈 줄 */ }

    @Override
    public List<String> drawAxis(BarGraphData data, GraphMetadata metadata, int[] axisMapping) {
        // axisMapping 무시. direction 기반으로 축 레이블 결정:
        //   VERTICAL:   가로축(하단) = 카테고리 이름 나열, 세로축(좌측) = 값 눈금
        //   HORIZONTAL: 가로축(하단) = 값 눈금,            세로축(좌측) = 카테고리 이름 나열
        // metadata.getXLabel() → 가로축 레이블, metadata.getYLabel() → 세로축 레이블
    }

    @Override
    public List<String> drawPlot(BarGraphData data, int[] axisMapping) {
        // axisMapping 무시. direction으로 렌더링 방향 결정.
        //
        // 1. values = bars.stream().map(Pair::getSecond).collect(toList())
        // 2. values.add(0.0)  ← 기준선(0)이 항상 grid 범위 안에 위치하도록 보장
        //    (모든 값이 양수일 때 calcRange가 0을 포함하지 않으면 toRow(0,...)가 음수 인덱스 → 예외)
        // 3. valueRange = scaleCalc.calcRange(values)
        // 4. char[gridHeight][gridWidth] grid, fill ' '
        //
        // 5a. VERTICAL:
        //     barWidth = gridWidth / bars.size()
        //     for each bar i:
        //       col = i * barWidth + barWidth / 2   ← 막대 중심 열
        //       topRow = toRow(val, valueRange.min, valueRange.max, gridHeight)
        //       baseRow = toRow(0, valueRange.min, valueRange.max, gridHeight)
        //       for r in [min(topRow,baseRow)..max(topRow,baseRow)]: grid[r][col] = '*'
        //
        // 5b. HORIZONTAL:
        //     barHeight = gridHeight / bars.size()
        //     for each bar i:
        //       row = i * barHeight + barHeight / 2
        //       endCol = toCol(val, valueRange.min, valueRange.max, gridWidth)
        //       baseCol = toCol(0, valueRange.min, valueRange.max, gridWidth)
        //       for c in [min(baseCol,endCol)..max(baseCol,endCol)]: grid[row][c] = '*'
        //
        // 6. grid 행을 List<String>으로 반환
    }
}
```

---

## ViewController (makeagraph.graph)

```java
public class ViewController {
    private final int dim;
    private int[] axisMapping; // 항상 길이 2

    public ViewController(int dim) {
        this.dim = dim;
        this.axisMapping = new int[]{0, 1};
    }

    public void swapAxes(int a, int b) { /* axisMapping 내 a, b 위치 교환 */ }

    public void setView(int[] axes) {
        if (axes.length != 2) throw new IllegalArgumentException("axes must be length 2");
        this.axisMapping = axes.clone();
    }

    public int[] getAxes() { return axisMapping.clone(); }

    public List<int[]> getAvailableViews() {
        // dim에서 2개를 골라 순서 쌍(ordered pair, 순열) 생성
        // dim=2 → {0,1}, {1,0}
        // dim=3 → {0,1}, {1,0}, {0,2}, {2,0}, {1,2}, {2,1}
    }
}
```

---

## AbstractGraph\<T, D extends ITitle\> (makeagraph.graph) — Template Method

```java
public abstract class AbstractGraph<T, D extends ITitle>
        implements IGraph<T>, IGraphObserver {

    protected final T data;
    protected final GraphMetadata metadata;
    protected final IRenderer renderer;
    protected final D drawer;

    protected AbstractGraph(T data, GraphMetadata metadata, IRenderer renderer, D drawer) { ... }

    @Override
    public final void draw() {
        List<String> lines = new ArrayList<>();
        lines.addAll(drawer.drawTitle(metadata));
        lines.addAll(drawBody());
        renderer.print(lines);
    }

    @Override
    public void onDataChanged() { draw(); }

    protected abstract List<String> drawBody();
}
```

---

## AbstractAxisGraph\<T, D extends ITitle & IAxis\<T\> & IAxisPlot\<T\>\> (makeagraph.graph)

```java
public abstract class AbstractAxisGraph<T, D extends ITitle & IAxis<T> & IAxisPlot<T>>
        extends AbstractGraph<T, D>
        implements IViewControllable {

    protected final ViewController viewController;

    protected AbstractAxisGraph(T data, GraphMetadata metadata,
                                IRenderer renderer, D drawer, ViewController vc) {
        super(data, metadata, renderer, drawer);
        this.viewController = vc;
    }

    @Override
    protected List<String> drawBody() {
        int[] axes = viewController.getAxes();
        List<String> body = new ArrayList<>();
        body.addAll(drawer.drawAxis(data, metadata, axes));
        body.addAll(drawer.drawPlot(data, axes));
        return body;
    }

    @Override
    public void swapAxes(int a, int b) { viewController.swapAxes(a, b); draw(); }

    @Override
    public void setView(int[] axes) { viewController.setView(axes); draw(); }

    @Override
    public List<int[]> getAvailableViews() { return viewController.getAvailableViews(); }
}
```

---

## ScatterPlot (makeagraph.graph)

```java
public class ScatterPlot extends AbstractAxisGraph<ScatterPlotData, IScatterDrawer>
        implements IBatchAppendable<Point> {

    public ScatterPlot(ScatterPlotData data, GraphMetadata metadata,
                       IRenderer renderer, IScatterDrawer drawer, ViewController vc) {
        super(data, metadata, renderer, drawer, vc);
    }

    @Override public void appendData(Point point) { data.append(point); }
    @Override public void suspendObservers() { data.suspendObservers(); }
    @Override public void resumeObservers() { data.resumeObservers(); }
}
```

---

## BarGraph (makeagraph.graph)

```java
public class BarGraph extends AbstractAxisGraph<BarGraphData, IBarDrawer>
        implements IBatchAppendable<Pair<String, Double>> {

    public BarGraph(BarGraphData data, GraphMetadata metadata,
                    IRenderer renderer, IBarDrawer drawer, ViewController vc) {
        super(data, metadata, renderer, drawer, vc);
    }

    @Override public void appendData(Pair<String, Double> bar) { data.append(bar); }
    @Override public void suspendObservers() { data.suspendObservers(); }
    @Override public void resumeObservers() { data.resumeObservers(); }

    @Override
    public List<int[]> getAvailableViews() { return Collections.emptyList(); }
    // direction으로만 렌더링하므로 뷰 전환 무의미.
    // 빈 목록 → GraphSession에서 "전환할 뷰가 없습니다" 안내.
    // swapAxes()/setView()는 상속 잔존이나 BarGraphDrawer가 axisMapping을 무시하므로 출력 무영향.
}
```

---

## TextRenderer (makeagraph.renderer)

```java
public class TextRenderer implements IRenderer {
    @Override
    public void print(List<String> lines) {
        System.out.println();
        for (String line : lines) System.out.println(line);
    }
}
```

---

## 주의사항

1. `AbstractGraph.draw()`는 **final** — 하위 클래스 오버라이드 불가
2. `BarGraph.getAvailableViews()` → `Collections.emptyList()` 오버라이드 필수
3. `suspend/resume`는 각 구체 클래스에서 data에 직접 위임 — AbstractGraph에서 공통화 불가 (T에 suspendObservers()의 타입 바운드를 걸 공통 상위 타입이 없음)
4. `BarGraphDrawer`: axisMapping 파라미터는 계약상 받지만 **무시**, `data.getDirection()`으로 렌더링 방향 결정
5. `BarGraphDrawer.drawPlot()`: values에 `0.0` 추가 — 기준선이 항상 grid 범위 내 위치
6. `ScatterPlotDrawer.drawAxis()`: axisMapping 기반 레이블 매핑 (전치 시 레이블 교차는 의도된 동작)
7. `ScatterPlotDrawer.drawPlot()`: 중복 좌표는 마지막 `*`로 덮어쓰기
8. `Pair` import는 `makeagraph.util.Pair` 사용
