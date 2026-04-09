# Prompt 05 — 입력 구현체 · 조립(Factory/Director) · 세션 · Main

> 이 프롬프트는 MakeAGraph 프로젝트의 **5단계(최종): 입력 구현체, 객체 조립, 세션, Main** 구현을 요청합니다.
> **1~4단계 코드가 모두 완성된 상태**에서 진행합니다.

---

## 사전 컨텍스트

1~4단계에서 생성된 **모든 파일**을 컨텍스트로 첨부하세요. 이 단계에서 전체가 조립되므로 모든 타입이 필요합니다.

---

## 지시

아래 명세에 따라 입력 구현체, 팩토리, 디렉터, 세션, Main을 구현해 주세요.
**콘솔 대화형 모드만 구현 대상**입니다.

패키지 구조:

```
src/main/java/makeagraph/
├── input/
│   ├── ConsoleInput.java
│   ├── ConsoleTypeSelector.java
│   ├── ScatterConsoleInputHandler.java
│   └── BarConsoleInputHandler.java
├── assembly/
│   ├── GraphFactory.java
│   ├── BuildResult.java
│   └── GraphDirector.java
├── session/
│   └── GraphSession.java
└── Main.java
```

---

## 구현 대상 (9개 파일)

### 입력 구현체 (4개)

#### 1. `ConsoleInput`

```java
package makeagraph.input;

import java.util.Scanner;

public class ConsoleInput implements IInputSource {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public Object readObject() {
        return readObject("");
    }

    @Override
    public Object readObject(String prompt) {
        if (prompt != null && !prompt.isEmpty()) {
            System.out.print(prompt);
        }
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return null;
    }

    @Override
    public void close() {
        scanner.close();
    }
}
```

- `readObject()` → `readObject("")` 위임 (편의 메서드).
- `readObject(prompt)`: prompt가 비어있지 않으면 `System.out.print(prompt)` 출력 후 입력 읽기.
- **안내 출력은 source에 위임** — handler가 `System.out.print()`를 직접 호출하지 않고 `source.readObject("안내문")` 사용.

#### 2. `ConsoleTypeSelector`

```java
package makeagraph.input;

public class ConsoleTypeSelector implements ITypeSelector {

    @Override
    public String selectType(IInputSource source) {
        // 루프:
        //   source.readObject("그래프 타입을 선택하세요 (scatter / bar): ")로 입력 받기
        //   "scatter" 또는 "bar"이면 반환
        //   그 외 → source.readObject("scatter 또는 bar를 입력하세요: ")로 재입력
    }
}
```

- **안내 출력은 `source.readObject(prompt)`를 통해 위임** — handler/selector가 `System.out.print()`를 직접 호출하지 않는다.

#### 3. `ScatterConsoleInputHandler`

```java
package makeagraph.input;

import makeagraph.data.*;
import makeagraph.util.*;

public class ScatterConsoleInputHandler
        implements IGraphDataInputHandler<ScatterPlotData, Point> {

    private int dim;  // readData()에서 설정, readMetadata()에서 참조

    @Override
    public ScatterPlotData readData(IInputSource source) {
        // 1. dim 입력 (정수 검증, 1 이상)
        //    source.readObject("몇 차원 데이터입니까? ")
        //    정수가 아닌 값 → source.readObject("정수를 입력하세요: ") 재입력
        //    0 이하 → source.readObject("1 이상의 값을 입력하세요: ") 재입력
        //    → dim 필드에 저장
        //
        // 2. 데이터 반복 입력
        //    source.readObject("데이터를 입력하세요 (v1, v2, ..., vN). 빈 줄로 종료:\n")
        //    반복:
        //      source.readObject() → 입력 (프롬프트 없이 줄 읽기)
        //      빈 줄 → 1건 이상이면 종료, 0건이면 "최소 1건 필요" 안내
        //      파싱 실패 (dim 불일치, 숫자 아님) → 에러 출력, 무시
        //      성공 → Point.of(v1, ..., vdim) 생성, 리스트에 추가
        //
        // 3. ScatterPlotData 생성, 수집된 Point들을 append
        //    → 반환
    }

    @Override
    public GraphMetadata readMetadata(IInputSource source) {
        // 1. source.readObject("그래프 제목: ") → title
        // 2. dim개의 축 레이블 입력
        //    source.readObject("축 1 레이블: ") → xLabel
        //    source.readObject("축 2 레이블: ") → yLabel
        //    (dim>2일 때 추가 레이블은 현재 GraphMetadata가 xLabel/yLabel만 지원하므로 무시)
        // → GraphMetadata(title, xLabel, yLabel) 반환
    }

    @Override
    public IDataParser<Point> createParser() {
        // dim이 설정된 상태에서 호출 (readData() 이후)
        // raw 문자열 → Point.of(...)로 변환하는 파서 반환
        return raw -> {
            // "v1, v2, ..., vdim" 파싱 → Point.of(values)
            // dim 불일치 / 숫자 아닌 값 → 예외 throw
        };
    }

    @Override
    public Point parseData(IInputSource source) {
        // createParser()에 위임하여 파싱 로직 중복 방지
        String raw = (String) source.readObject("데이터를 입력하세요 (예: 1.0, 2.0): ");
        return createParser().parse(raw);
    }
}
```

- **호출 순서 계약**: `readData()` → `readMetadata()` 순서 필수. `dim`이 `readData()`에서 설정됨.

#### 4. `BarConsoleInputHandler`

```java
package makeagraph.input;

import makeagraph.data.*;
import makeagraph.util.*;

public class BarConsoleInputHandler
        implements IGraphDataInputHandler<BarGraphData, Pair<String, Double>> {

    @Override
    public BarGraphData readData(IInputSource source) {
        // 1. direction 입력
        //    source.readObject("방향 (VERTICAL / HORIZONTAL): ")
        //    VERTICAL/HORIZONTAL 외 → source.readObject("VERTICAL 또는 HORIZONTAL을 입력하세요: ") 재입력
        //
        // 2. 데이터 반복 입력
        //    source.readObject("데이터를 입력하세요 (카테고리, 값). 빈 줄로 종료:\n")
        //    반복:
        //      source.readObject() → 입력 (프롬프트 없이)
        //      빈 줄 → 1건 이상이면 종료, 0건이면 "최소 1건 필요" 안내
        //      파싱 실패 → 에러 출력, 무시
        //      성공 → Pair<String, Double> 생성
        //
        // 3. BarGraphData(direction) 생성, 수집된 Pair들을 append
        //    → 반환
    }

    @Override
    public GraphMetadata readMetadata(IInputSource source) {
        // source.readObject("그래프 제목: ") → title
        // source.readObject("X축 레이블: ") → xLabel
        // source.readObject("Y축 레이블: ") → yLabel
        // → GraphMetadata(title, xLabel, yLabel) 반환
    }

    @Override
    public IDataParser<Pair<String, Double>> createParser() {
        // raw 문자열 → Pair<String, Double>로 변환하는 파서 반환
        return raw -> {
            // "카테고리, 값" 파싱 → new Pair<>(category, value)
            // 값이 숫자 아닌 경우 → 예외 throw
        };
    }

    @Override
    public Pair<String, Double> parseData(IInputSource source) {
        // createParser()에 위임하여 파싱 로직 중복 방지
        String raw = (String) source.readObject("데이터를 입력하세요 (예: 사과, 15.0): ");
        return createParser().parse(raw);
    }
}
```

---

### 조립 계층 (3개)

#### 5. `BuildResult<T, E>` — 내부 전달 객체

```java
package makeagraph.assembly;

import makeagraph.graph.IGraph;
import makeagraph.graph.IBatchAppendable;
import makeagraph.input.IGraphDataInputHandler;

public class BuildResult<T, E> {
    private final IGraph<T> graph;
    private final IBatchAppendable<E> appendable;
    private final IGraphDataInputHandler<T, E> handler;

    public BuildResult(IGraph<T> graph, IBatchAppendable<E> appendable,
                       IGraphDataInputHandler<T, E> handler) {
        this.graph = graph;
        this.appendable = appendable;
        this.handler = handler;
    }

    public IGraph<T> graph() { return graph; }
    public IBatchAppendable<E> appendable() { return appendable; }
    public IGraphDataInputHandler<T, E> handler() { return handler; }
}
```

#### 6. `GraphFactory` — static 유틸리티 클래스

```java
package makeagraph.assembly;

import makeagraph.data.*;
import makeagraph.drawer.*;
import makeagraph.graph.*;
import makeagraph.renderer.IRenderer;
import makeagraph.util.ScaleCalculator;

public class GraphFactory {

    private GraphFactory() {} // 인스턴스 생성 방지

    public static ScatterPlot createScatter(
            ScatterPlotData data, GraphMetadata metadata, IRenderer renderer) {
        ScaleCalculator scaleCalc = new ScaleCalculator();
        IScatterDrawer drawer = new ScatterPlotDrawer(scaleCalc);
        int dim = data.getPoints().get(0).size();  // readData() 최소 1건 보장
        ViewController vc = new ViewController(dim);
        return new ScatterPlot(data, metadata, renderer, drawer, vc);
    }

    public static BarGraph createBar(
            BarGraphData data, GraphMetadata metadata, IRenderer renderer) {
        ScaleCalculator scaleCalc = new ScaleCalculator();
        IBarDrawer drawer = new BarGraphDrawer(scaleCalc);
        ViewController vc = new ViewController(2);  // 카테고리/값 2축 고정
        return new BarGraph(data, metadata, renderer, drawer, vc);
    }
}
```

- 반환 타입이 구체 클래스 → `IGraph<T>`와 `IDataAppendable<E>` 모두 시그니처에서 보장.

#### 7. `GraphDirector` — 조립 순서 제어

```java
package makeagraph.assembly;

import makeagraph.data.*;
import makeagraph.graph.*;
import makeagraph.input.*;
import makeagraph.renderer.IRenderer;
import makeagraph.session.*;
import makeagraph.util.Pair;

public class GraphDirector {

    private final ITypeSelector typeSelector;

    public GraphDirector(ITypeSelector typeSelector) {
        this.typeSelector = typeSelector;
    }

    public ISession construct(IInputSource source, IRenderer renderer) {
        BuildResult<?, ?> result = constructGraph(source, renderer);
        return new GraphSession<>(source, result.graph(), result.appendable(),
                                  result.handler());
    }

    private BuildResult<?, ?> constructGraph(IInputSource source, IRenderer renderer) {
        String type = typeSelector.selectType(source);

        if ("scatter".equals(type)) {
            IGraphDataInputHandler<ScatterPlotData, Point> handler
                = new ScatterConsoleInputHandler();
            ScatterPlotData data = handler.readData(source);
            GraphMetadata metadata = handler.readMetadata(source);
            ScatterPlot graph = GraphFactory.createScatter(data, metadata, renderer);
            data.addObserver(graph);
            return new BuildResult<>(graph, graph, handler);

        } else if ("bar".equals(type)) {
            IGraphDataInputHandler<BarGraphData, Pair<String, Double>> handler
                = new BarConsoleInputHandler();
            BarGraphData data = handler.readData(source);
            GraphMetadata metadata = handler.readMetadata(source);
            BarGraph graph = GraphFactory.createBar(data, metadata, renderer);
            data.addObserver(graph);
            return new BuildResult<>(graph, graph, handler);
        }

        throw new IllegalArgumentException("Unknown graph type: " + type);
    }
}
```

- `constructGraph()` 내부의 `if/else if` 분기 — 그래프 종류 추가 시 분기 추가 (현재 규모에서 감수).
- `BuildResult`에 `graph`와 `appendable`에 같은 인스턴스(`ScatterPlot`/`BarGraph`)를 넣음 — 두 인터페이스를 모두 구현하므로 가능.

---

### 세션 (1개)

#### 8. `GraphSession<T, E>` — 대화형 세션

```java
package makeagraph.session;

import makeagraph.graph.*;
import makeagraph.input.*;

public class GraphSession<T, E> implements ISession {

    private final IInputSource source;
    private final IGraph<T> graph;
    private final IDataAppendable<E> appendable;
    private final IGraphDataInputHandler<T, E> handler;
    private Thread workerThread;

    public GraphSession(IInputSource source, IGraph<T> graph,
                        IDataAppendable<E> appendable,
                        IGraphDataInputHandler<T, E> handler) {
        this.source = source;
        this.graph = graph;
        this.appendable = appendable;
        this.handler = handler;
    }

    @Override
    public void start() {
        workerThread = new Thread(this::run);
        workerThread.start();
    }

    @Override
    public void stop() {
        source.close();
    }

    @Override
    public void await() {
        try {
            if (workerThread != null) {
                workerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void run() {
        try {
            graph.draw();  // 최초 출력

            // 명령어 루프
            while (true) {
                Object input = source.readObject("\n명령어 (add / view / exit): ");
                if (input == null) break;
                String cmd = ((String) input).trim().toLowerCase();

                switch (cmd) {
                    case "add":
                        try {
                            E element = handler.parseData(source);
                            appendable.appendData(element);
                        } catch (Exception e) {
                            System.err.println("입력 오류: " + e.getMessage());
                        }
                        break;

                    case "view":
                        if (graph instanceof IViewControllable) {
                            IViewControllable vc = (IViewControllable) graph;
                            var views = vc.getAvailableViews();
                            if (views.isEmpty()) {
                                System.out.println("전환할 뷰가 없습니다.");
                                break;
                            }
                            System.out.println("사용 가능한 뷰:");
                            for (int i = 0; i < views.size(); i++) {
                                int[] axes = views.get(i);
                                System.out.printf("  %d. 축%d - 축%d%n",
                                    i + 1, axes[0], axes[1]);
                            }
                            try {
                                String numStr = (String) source.readObject("번호 입력: ");
                                int idx = Integer.parseInt(numStr.trim()) - 1;
                                if (idx >= 0 && idx < views.size()) {
                                    vc.setView(views.get(idx));
                                } else {
                                    System.out.println("유효한 번호를 입력하세요.");
                                }
                            } catch (Exception e) {
                                System.out.println("유효한 번호를 입력하세요.");
                            }
                        } else {
                            System.out.println("이 그래프는 뷰 전환을 지원하지 않습니다.");
                        }
                        break;

                    case "exit":
                        return;  // 루프 종료

                    default:
                        System.out.println("add, view, exit 중 입력하세요.");
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("세션 오류: " + e.getMessage());
        } finally {
            source.close();
        }
    }
}
```

- `IViewControllable`로의 런타임 캐스팅 — ISP에 의한 유일한 잔존 캐스팅.
- `parseData()` 실패 시 에러 메시지 출력 후 무시, 루프 계속.
- `view` 명령어 시 뷰 목록 번호 출력 → 사용자 선택 → `setView()`.

---

### Main (1개)

#### 9. `Main`

```java
package makeagraph;

import makeagraph.input.*;
import makeagraph.renderer.*;
import makeagraph.assembly.*;
import makeagraph.session.*;

public class Main {

    public static void main(String[] args) {
        IInputSource source = new ConsoleInput();
        IRenderer renderer = new TextRenderer();
        ITypeSelector typeSelector = new ConsoleTypeSelector();
        GraphDirector director = new GraphDirector(typeSelector);

        ISession session = director.construct(source, renderer);
        session.start();
        session.await();
    }
}
```

- Main이 아는 것: `IInputSource`, `IRenderer`, `ITypeSelector`, `GraphDirector`, `ISession`.
- Main이 모르는 것: `IGraph`, `data`, `handler`, `Drawer`, `ViewController`, `ScaleCalculator`.

---

## 전체 사용자 인터랙션 예시

```
그래프 타입을 선택하세요 (scatter / bar): scatter
몇 차원 데이터입니까? 2
데이터를 입력하세요 (v1, v2). 빈 줄로 종료:
10, 20
15, 35
20, 25
30, 55
35, 55

그래프 제목: My Scatter Plot
축 1 레이블: X
축 2 레이블: Y

[그래프 출력]

명령어 (add / view / exit): add
30, 40

[그래프 갱신 출력]

명령어 (add / view / exit): view
사용 가능한 뷰:
  1. 축0 - 축1
  2. 축1 - 축0
번호 입력: 2

[축 전치된 그래프 출력]

명령어 (add / view / exit): exit
```

---

## 에러 처리 요약

| 상황 | 처리 |
|------|------|
| 미지원 그래프 타입 | "scatter 또는 bar를 입력하세요" → 재입력 |
| dim에 음수/-1 | "1 이상의 값을 입력하세요" → 재입력 |
| direction에 잘못된 값 | "VERTICAL 또는 HORIZONTAL을 입력하세요" → 재입력 |
| 데이터 파싱 실패 | 에러 메시지 → 해당 줄 무시 → 루프 계속 |
| 빈 줄인데 0건 | "최소 1건 필요" → 재입력 |
| 미지원 명령어 | "add, view, exit 중 입력하세요" → 무시 |
| add 후 parseData 실패 | 에러 메시지 → 무시 → 루프 계속 |
| view 후 범위 초과 번호 | "유효한 번호를 입력하세요" → 뷰 전환 취소 |

---

## 주의사항

1. `GraphDirector.constructGraph()`에서 `data.addObserver(graph)` 호출을 빠뜨리지 않기.
2. `BuildResult`에 graph와 appendable은 같은 인스턴스 — `ScatterPlot`/`BarGraph`가 `IGraph`와 `IBatchAppendable`을 모두 구현.
3. **콘솔 안내 메시지는 `source.readObject(prompt)`를 통해 위임** — handler/selector가 `System.out.print()`를 직접 호출하지 않는다. 이 설계로 `ConsoleInput`을 `GUIInput`으로 교체하면 대화 상자 레이블로 자동 전환.
4. `GraphSession`의 `run()`은 `start()`가 별도 스레드로 실행 → `await()`에서 join 대기.
5. `Main`은 최소한의 코드 — 모든 복잡성은 `GraphDirector`가 흡수.
