# Prompt A3 — 조립 · 세션 · Main

> MakeAGraph 프로젝트 **3/3**.
> **필수**: A1 + A2에서 생성한 모든 Java 파일을 컨텍스트로 첨부.

## 생성할 파일

```
src/main/java/makeagraph/
├── input/
│   ├── ConsoleInput.java
│   ├── ConsoleTypeSelector.java
│   ├── ScatterConsoleInputHandler.java
│   └── BarConsoleInputHandler.java
├── assembly/
│   ├── BuildResult.java
│   ├── GraphFactory.java
│   └── GraphDirector.java
├── session/
│   └── GraphSession.java
└── Main.java
```

총 9개 파일 생성.

---

## ConsoleInput (makeagraph.input)

```java
public class ConsoleInput implements IInputSource {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public Object readObject() { return readObject(""); }

    @Override
    public Object readObject(String prompt) {
        if (prompt != null && !prompt.isEmpty()) System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }

    @Override
    public void close() { scanner.close(); }
}
```

---

## ConsoleTypeSelector (makeagraph.input)

```java
public class ConsoleTypeSelector implements ITypeSelector {
    @Override
    public String selectType(IInputSource source) {
        while (true) {
            String input = (String) source.readObject("그래프 타입을 선택하세요 (scatter / bar): ");
            if ("scatter".equalsIgnoreCase(input) || "bar".equalsIgnoreCase(input))
                return input.toLowerCase();
            System.out.println("scatter 또는 bar를 입력하세요.");
        }
    }
}
```

---

## ScatterConsoleInputHandler (makeagraph.input)

```java
public class ScatterConsoleInputHandler
        implements IGraphDataInputHandler<ScatterPlotData, Point> {

    private int dim; // readData()에서 설정, createParser()/readMetadata()에서 참조

    @Override
    public ScatterPlotData readData(IInputSource source) {
        // 1. dim 입력:
        //    source.readObject("몇 차원 데이터입니까? ")
        //    정수가 아니면 재입력, 0 이하면 재입력
        //    dim을 필드에 저장
        //
        // 2. 안내 + 첫 줄 읽기:
        //    source.readObject("데이터를 입력하세요 (v1, v2, ...). 빈 줄로 종료:\n")
        //
        // 3. 반복: source.readObject("") → 한 줄 읽기
        //    빈 줄 → 종료 (단, 0건이면 "최소 1건 필요" 안내 후 계속)
        //    파싱 실패 (dim 불일치, 숫자 아닌 값) → 에러 출력, 무시, 루프 계속
        //
        // 4. ScatterPlotData 생성, 수집한 모든 Point를 append
    }

    @Override
    public GraphMetadata readMetadata(IInputSource source) {
        // 제목:   source.readObject("그래프 제목: ")
        // xLabel: source.readObject("X축 레이블: ")
        // yLabel: source.readObject("Y축 레이블: ")
        // dim>2인 경우 추가 레이블은 현재 무시 (GraphMetadata가 2개만 지원)
    }

    @Override
    public IDataParser<Point> createParser() {
        return raw -> {
            // "v1, v2, ..., vdim" 파싱 → Point.of(v1, ..., vdim)
            // dim 필드 참조하여 개수 검증
        };
    }

    @Override
    public Point parseData(IInputSource source) {
        String raw = (String) source.readObject("데이터를 입력하세요 (예: 1.0, 2.0): ");
        return createParser().parse(raw);
    }
}
```

---

## BarConsoleInputHandler (makeagraph.input)

```java
public class BarConsoleInputHandler
        implements IGraphDataInputHandler<BarGraphData, Pair<String, Double>> {

    @Override
    public BarGraphData readData(IInputSource source) {
        // 1. direction 입력:
        //    source.readObject("방향 (VERTICAL / HORIZONTAL): ")
        //    VERTICAL/HORIZONTAL 외 → 재입력
        //
        // 2. 안내 + 첫 줄 읽기:
        //    source.readObject("데이터를 입력하세요 (카테고리, 값). 빈 줄로 종료:\n")
        //
        // 3. 반복: source.readObject("") → 한 줄 읽기
        //    빈 줄 → 종료 (0건이면 "최소 1건 필요" 안내 후 계속)
        //    파싱 실패 (값이 숫자 아닌 경우) → 에러 출력, 무시, 루프 계속
        //
        // 4. BarGraphData(direction) 생성, 수집한 모든 Pair를 append
    }

    @Override
    public GraphMetadata readMetadata(IInputSource source) {
        // 제목, X축 레이블, Y축 레이블 입력
    }

    @Override
    public IDataParser<Pair<String, Double>> createParser() {
        return raw -> {
            // "카테고리, 값" 파싱 → new Pair<>(category.trim(), Double.parseDouble(value.trim()))
        };
    }

    @Override
    public Pair<String, Double> parseData(IInputSource source) {
        String raw = (String) source.readObject("데이터를 입력하세요 (예: 사과, 15.0): ");
        return createParser().parse(raw);
    }
}
```

---

## BuildResult\<T, E\> (makeagraph.assembly) — 내부 전달 객체

```java
public class BuildResult<T, E> {
    private final IGraph<T> graph;
    private final IBatchAppendable<E> appendable;
    private final IGraphDataInputHandler<T, E> handler;

    public BuildResult(IGraph<T> graph, IBatchAppendable<E> appendable,
                       IGraphDataInputHandler<T, E> handler) { ... }

    public IGraph<T> graph() { ... }
    public IBatchAppendable<E> appendable() { ... }
    public IGraphDataInputHandler<T, E> handler() { ... }
}
```

---

## GraphFactory (makeagraph.assembly) — static 유틸리티

```java
public class GraphFactory {
    private GraphFactory() {}

    public static ScatterPlot createScatter(ScatterPlotData data, GraphMetadata meta,
                                            IRenderer renderer) {
        ScaleCalculator sc = new ScaleCalculator();
        IScatterDrawer drawer = new ScatterPlotDrawer(sc); // gridWidth/Height 기본값
        int dim = data.getPoints().get(0).size();
        ViewController vc = new ViewController(dim);
        return new ScatterPlot(data, meta, renderer, drawer, vc);
    }

    public static BarGraph createBar(BarGraphData data, GraphMetadata meta,
                                     IRenderer renderer) {
        ScaleCalculator sc = new ScaleCalculator();
        IBarDrawer drawer = new BarGraphDrawer(sc); // gridWidth/Height 기본값
        ViewController vc = new ViewController(2); // 카테고리/값 2축 고정
        return new BarGraph(data, meta, renderer, drawer, vc);
    }
}
```

---

## GraphDirector (makeagraph.assembly)

```java
public class GraphDirector {
    private final ITypeSelector typeSelector;

    public GraphDirector(ITypeSelector typeSelector) { this.typeSelector = typeSelector; }

    public ISession construct(IInputSource source, IRenderer renderer) {
        return assembleSession(constructGraph(source, renderer), source);
    }

    // 와일드카드 캡처 헬퍼 — BuildResult<?, ?>의 T, E를 바인딩
    private <T, E> ISession assembleSession(BuildResult<T, E> result, IInputSource source) {
        return new GraphSession<>(source, result.graph(), result.appendable(), result.handler());
    }

    private BuildResult<?, ?> constructGraph(IInputSource source, IRenderer renderer) {
        String type = typeSelector.selectType(source);

        if ("scatter".equals(type)) {
            var handler = new ScatterConsoleInputHandler();
            ScatterPlotData data = handler.readData(source);
            GraphMetadata meta = handler.readMetadata(source);
            ScatterPlot graph = GraphFactory.createScatter(data, meta, renderer);
            data.addObserver(graph);
            return new BuildResult<>(graph, graph, handler);
            // ScatterPlot이 IGraph<ScatterPlotData>와 IBatchAppendable<Point>를 모두 구현
            // → graph와 appendable은 같은 인스턴스

        } else if ("bar".equals(type)) {
            var handler = new BarConsoleInputHandler();
            BarGraphData data = handler.readData(source);
            GraphMetadata meta = handler.readMetadata(source);
            BarGraph graph = GraphFactory.createBar(data, meta, renderer);
            data.addObserver(graph);
            return new BuildResult<>(graph, graph, handler);
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }
}
```

---

## GraphSession\<T, E\> (makeagraph.session)

```java
public class GraphSession<T, E> implements ISession {
    private final IInputSource source;
    private final IGraph<T> graph;
    private final IDataAppendable<E> appendable; // IBatchAppendable에서 업캐스팅
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
    public void start() { workerThread = new Thread(this::run); workerThread.start(); }

    @Override
    public void stop() { source.close(); }

    @Override
    public void await() {
        try { if (workerThread != null) workerThread.join(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void run() {
        try {
            graph.draw(); // 최초 출력 (readData()로 채워진 초기 데이터 반영)

            while (true) {
                Object input = source.readObject("\n명령어 (add / view / exit): ");
                if (input == null) break;
                String cmd = ((String) input).trim().toLowerCase();

                switch (cmd) {
                    case "add" -> {
                        try {
                            E data = handler.parseData(source);
                            appendable.appendData(data);
                        } catch (Exception e) {
                            System.err.println("입력 오류: " + e.getMessage());
                        }
                    }
                    case "view" -> handleView();
                    case "exit" -> { return; }
                    default -> System.out.println("add, view, exit 중 입력하세요.");
                }
            }
        } catch (Exception e) {
            System.err.println("세션 오류: " + e.getMessage());
        } finally {
            source.close();
        }
    }

    private void handleView() {
        // 1. instanceof IViewControllable 캐스팅 시도
        if (!(graph instanceof IViewControllable vc)) {
            System.out.println("이 그래프는 뷰 전환을 지원하지 않습니다.");
            return;
        }

        // 2. 뷰 목록 확인
        List<int[]> views = vc.getAvailableViews();
        if (views.isEmpty()) {
            System.out.println("전환할 뷰가 없습니다.");
            return;
        }

        // 3. 뷰 목록 번호와 함께 출력
        for (int i = 0; i < views.size(); i++) {
            System.out.printf("  %d. 축%d - 축%d%n", i + 1, views.get(i)[0], views.get(i)[1]);
        }

        // 4. 번호 입력 → setView
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
    }
}
```

---

## Main (makeagraph)

```java
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

---

## 에러 처리 요약

| 상황 | 처리 |
|------|------|
| 미지원 그래프 타입 | 재입력 요구 |
| dim 비정수 / 0 이하 | 재입력 요구 |
| direction 잘못된 값 | 재입력 요구 |
| readData 파싱 실패 | 에러 출력, 해당 줄 무시, 루프 계속 |
| readData 0건에서 빈 줄 | "최소 1건 필요" 안내, 재입력 |
| 미지원 명령어 | 안내 후 무시 |
| add 후 parseData 실패 | 에러 출력, 무시, 루프 계속 |
| view 잘못된 번호 | 안내, 명령어 루프 복귀 |

---

## 핵심 계약

1. 안내 메시지는 `source.readObject(prompt)`로 위임 — handler가 `System.out` 직접 호출 금지
2. `parseData()`는 `createParser().parse()`에 위임 (DRY)
3. `constructGraph()`에서 `data.addObserver(graph)` 필수
4. `BuildResult`에 graph와 appendable은 같은 인스턴스
5. `GraphSession`의 `appendable` 필드는 `IDataAppendable<E>` (IBatchAppendable에서 업캐스팅)
6. `GraphDirector.construct()`는 `assembleSession()` 헬퍼로 와일드카드 타입을 캡처
7. `GraphSession.start()`는 논블로킹 — 내부 스레드 생성 후 즉시 반환
