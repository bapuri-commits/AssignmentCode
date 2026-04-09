# Prompt 04 — 그래프 계층 (AbstractGraph · AbstractAxisGraph · 구체 클래스 · TextRenderer)

> 이 프롬프트는 MakeAGraph 프로젝트의 **4단계: 그래프 추상/구체 클래스 + TextRenderer** 구현을 요청합니다.
> **1~3단계 코드가 이미 완성된 상태**에서 진행합니다.

---

## 사전 컨텍스트

1~3단계에서 생성된 파일들을 컨텍스트로 첨부하세요. 특히:
- `graph/IGraph.java`, `graph/IDataAppendable.java`, `graph/IBatchAppendable.java`, `graph/IViewControllable.java`
- `graph/ViewController.java`
- `observer/IGraphObserver.java`
- `drawer/ITitle.java`, `drawer/IAxis.java`, `drawer/IAxisPlot.java`
- `drawer/IScatterDrawer.java`, `drawer/IBarDrawer.java`
- `drawer/ScatterPlotDrawer.java`, `drawer/BarGraphDrawer.java`
- `data/ScatterPlotData.java`, `data/BarGraphData.java`, `data/GraphMetadata.java`, `data/Point.java`
- `renderer/IRenderer.java`
- `util/Pair.java`

---

## 지시

아래 명세에 따라 그래프 추상 클래스, 구체 클래스, TextRenderer를 구현해 주세요.

패키지 구조:

```
src/main/java/makeagraph/
├── graph/
│   ├── AbstractGraph.java
│   ├── AbstractAxisGraph.java
│   ├── ScatterPlot.java
│   └── BarGraph.java
└── renderer/
    └── TextRenderer.java
```

---

## 구현 대상 (5개 파일)

### 1. `AbstractGraph<T, D extends ITitle>` — Template Method 패턴

```java
package makeagraph.graph;

import makeagraph.data.GraphMetadata;
import makeagraph.drawer.ITitle;
import makeagraph.observer.IGraphObserver;
import makeagraph.renderer.IRenderer;
import java.util.List;
import java.util.ArrayList;

public abstract class AbstractGraph<T, D extends ITitle>
        implements IGraph<T>, IGraphObserver {

    protected final T data;
    protected final GraphMetadata metadata;
    protected final IRenderer renderer;
    protected final D drawer;

    protected AbstractGraph(T data, GraphMetadata metadata,
                            IRenderer renderer, D drawer) {
        this.data = data;
        this.metadata = metadata;
        this.renderer = renderer;
        this.drawer = drawer;
    }

    @Override
    public final void draw() {
        // Template Method (final 권장)
        List<String> lines = new ArrayList<>();
        lines.addAll(drawer.drawTitle(metadata));
        lines.addAll(drawBody());
        renderer.print(lines);
    }

    @Override
    public void onDataChanged() {
        draw();  // 패스스루
    }

    protected abstract List<String> drawBody();
}
```

- `draw()` 골격: 제목 → 본문(`drawBody()`) → 출력.
- `onDataChanged() → draw()` 패스스루는 모든 그래프에 공통.
- D의 타입 바운드는 `ITitle`만 요구 — `drawBody()`에서 하위 클래스가 Drawer의 나머지 메서드 호출.

### 2. `AbstractAxisGraph<T, D>` — 축 있는 그래프 공통

```java
package makeagraph.graph;

import makeagraph.data.GraphMetadata;
import makeagraph.drawer.ITitle;
import makeagraph.drawer.IAxis;
import makeagraph.drawer.IAxisPlot;
import makeagraph.renderer.IRenderer;
import java.util.List;
import java.util.ArrayList;

public abstract class AbstractAxisGraph<T, D extends ITitle & IAxis<T> & IAxisPlot<T>>
        extends AbstractGraph<T, D>
        implements IViewControllable {

    protected final ViewController viewController;

    protected AbstractAxisGraph(T data, GraphMetadata metadata,
                                IRenderer renderer, D drawer,
                                ViewController viewController) {
        super(data, metadata, renderer, drawer);
        this.viewController = viewController;
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
    public void swapAxes(int a, int b) {
        viewController.swapAxes(a, b);
        draw();
    }

    @Override
    public void setView(int[] axes) {
        viewController.setView(axes);
        draw();
    }

    @Override
    public List<int[]> getAvailableViews() {
        return viewController.getAvailableViews();
    }
}
```

- D의 타입 바운드에 `IAxis<T>`, `IAxisPlot<T>` 추가 → Drawer가 축/플롯 그리기를 구현함이 컴파일 타임 보장.
- `drawBody()`, `swapAxes()`, `setView()`, `getAvailableViews()` 모두 상위에서 한 번만 구현.

### 3. `ScatterPlot` — 산포도 구체 클래스

```java
package makeagraph.graph;

import makeagraph.data.ScatterPlotData;
import makeagraph.data.GraphMetadata;
import makeagraph.data.Point;
import makeagraph.drawer.IScatterDrawer;
import makeagraph.renderer.IRenderer;

public class ScatterPlot
        extends AbstractAxisGraph<ScatterPlotData, IScatterDrawer>
        implements IBatchAppendable<Point> {

    public ScatterPlot(ScatterPlotData data, GraphMetadata metadata,
                       IRenderer renderer, IScatterDrawer drawer,
                       ViewController viewController) {
        super(data, metadata, renderer, drawer, viewController);
    }

    @Override
    public void appendData(Point point) {
        data.append(point);
    }

    @Override
    public void suspendObservers() {
        data.suspendObservers();
    }

    @Override
    public void resumeObservers() {
        data.resumeObservers();
    }
}
```

- `appendData()` → `data.append()` → `notifyObservers()` → `onDataChanged()` → `draw()` 자동 호출.

### 4. `BarGraph` — 막대그래프 구체 클래스

```java
package makeagraph.graph;

import makeagraph.data.BarGraphData;
import makeagraph.data.GraphMetadata;
import makeagraph.drawer.IBarDrawer;
import makeagraph.renderer.IRenderer;
import makeagraph.util.Pair;
import java.util.Collections;
import java.util.List;

public class BarGraph
        extends AbstractAxisGraph<BarGraphData, IBarDrawer>
        implements IBatchAppendable<Pair<String, Double>> {

    public BarGraph(BarGraphData data, GraphMetadata metadata,
                    IRenderer renderer, IBarDrawer drawer,
                    ViewController viewController) {
        super(data, metadata, renderer, drawer, viewController);
    }

    @Override
    public void appendData(Pair<String, Double> bar) {
        data.append(bar);
    }

    @Override
    public void suspendObservers() {
        data.suspendObservers();
    }

    @Override
    public void resumeObservers() {
        data.resumeObservers();
    }

    @Override
    public List<int[]> getAvailableViews() {
        // BarGraph는 렌더링 방향이 Direction으로 고정되므로
        // 축 전환(뷰)이 의미 없음 → 빈 목록 반환
        return Collections.emptyList();
    }
}
```

### 5. `TextRenderer` — 콘솔 텍스트 출력

```java
package makeagraph.renderer;

import java.util.List;

public class TextRenderer implements IRenderer {

    @Override
    public void print(List<String> lines) {
        // 화면 지우기 (선택 — 콘솔 환경에 따라 조정)
        System.out.println();
        for (String line : lines) {
            System.out.println(line);
        }
    }
}
```

---

## appendData() 경유 흐름 (확인용)

```
[GraphSession]
graph.appendData(point)              ← IDataAppendable<Point> 경유
 ↓ [ScatterPlot 내부]
data.append(point)                   ← 캐스팅 없음
 └── observerSupport.notifyObservers()
      └── onDataChanged()            ← AbstractGraph에서 구현
           └── draw()                ← AbstractGraph의 Template Method
```

## setView() / swapAxes() 흐름 (확인용)

```
setView(axes)                        ← AbstractAxisGraph에서 구현
 ├── viewController.setView(axes)
 └── draw()                          ← AbstractGraph의 Template Method
```

---

## 주의사항

1. `AbstractGraph.draw()`는 `final` — 하위 클래스에서 오버라이드 불가.
2. `AbstractAxisGraph`의 D 타입 바운드: `D extends ITitle & IAxis<T> & IAxisPlot<T>` — 교차 타입 바운드.
3. `ScatterPlot`과 `BarGraph`는 매우 얇은 클래스 — `appendData()`와 `suspendObservers()/resumeObservers()` 위임만 담당.
6. **`suspendObservers()/resumeObservers()` 비공통화 이유**: `AbstractGraph<T, D>`의 T가 raw data 타입인데, `suspendObservers()`/`resumeObservers()`는 data 객체의 `ObserverSupport` 메서드이다. `T`에 대해 해당 메서드의 존재를 보장할 타입 바운드가 없으므로(ScatterPlotData/BarGraphData는 공통 상위를 갖지 않음), `AbstractGraph`에서 끌어올릴 수 없다. 각 구체 클래스에서 `data.suspendObservers()`를 직접 호출하는 것이 의도된 설계.
4. `TextRenderer`는 단순 `System.out.println()` 루프. 스레드 안전성 고려 불필요 (콘솔 모드).
5. 모든 생성자 파라미터를 `protected final` 필드에 저장 (불변 의도).
