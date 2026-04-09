# Prompt 02 — 인터페이스 계층

> 이 프롬프트는 MakeAGraph 프로젝트의 **2단계: 인터페이스 계층** 구현을 요청합니다.
> **1단계(데이터/유틸 계층) 코드가 이미 완성된 상태**에서 진행합니다.

---

## 사전 컨텍스트

1단계에서 생성된 파일들을 컨텍스트로 첨부하세요:
- `data/Point.java`, `data/ScatterPlotData.java`, `data/BarGraphData.java`, `data/GraphMetadata.java`, `data/ObserverSupport.java`
- `util/Range.java`, `util/ScaleCalculator.java`, `util/Direction.java`, `util/Pair.java`
- `observer/IGraphObserver.java`

---

## 지시

아래 명세에 따라 Java 인터페이스를 구현해 주세요.
**콘솔 대화형 모드만 구현 대상**입니다 (실시간 관련 인터페이스는 최소한으로).

패키지 구조:

```
src/main/java/makeagraph/
├── graph/
│   ├── IGraph.java
│   ├── IDataAppendable.java
│   ├── IBatchAppendable.java
│   └── IViewControllable.java
├── drawer/
│   ├── ITitle.java
│   ├── IAxis.java
│   ├── IAxisPlot.java
│   ├── IScatterDrawer.java
│   └── IBarDrawer.java
├── input/
│   ├── IInputSource.java
│   ├── ITypeSelector.java
│   ├── IGraphDataInputHandler.java
│   └── IDataParser.java
├── renderer/
│   └── IRenderer.java
└── session/
    └── ISession.java
```

---

## 구현 대상 (15개 파일)

### 그래프 계층 인터페이스

#### 1. `IGraph<T>`

```java
package makeagraph.graph;

public interface IGraph<T> {
    void draw();
}
```

#### 2. `IDataAppendable<E>`

```java
package makeagraph.graph;

public interface IDataAppendable<E> {
    void appendData(E element);
}
```

- `IGraph`에서 분리 (SRP). E = 원소 타입 (Point, Pair 등).

#### 3. `IBatchAppendable<E>`

```java
package makeagraph.graph;

public interface IBatchAppendable<E> extends IDataAppendable<E> {
    void suspendObservers();
    void resumeObservers();
}
```

- 배치 처리 전용. 실시간 세션에서 사용하지만, 그래프 구체 클래스가 구현해야 하므로 이 단계에서 정의.

#### 4. `IViewControllable`

```java
package makeagraph.graph;

import java.util.List;

public interface IViewControllable {
    void swapAxes(int a, int b);
    void setView(int[] axes);
    List<int[]> getAvailableViews();
}
```

- 축 있는 그래프만 구현 (ISP). PieChart 등은 미구현.

### Drawer 인터페이스

#### 5. `ITitle`

```java
package makeagraph.drawer;

import makeagraph.data.GraphMetadata;
import java.util.List;

public interface ITitle {
    List<String> drawTitle(GraphMetadata metadata);
}
```

#### 6. `IAxis<T>`

```java
package makeagraph.drawer;

import makeagraph.data.GraphMetadata;
import java.util.List;

public interface IAxis<T> {
    List<String> drawAxis(T data, GraphMetadata metadata, int[] axisMapping);
}
```

#### 7. `IAxisPlot<T>`

```java
package makeagraph.drawer;

import java.util.List;

public interface IAxisPlot<T> {
    List<String> drawPlot(T data, int[] axisMapping);
}
```

- 축 매핑을 받아 데이터를 그리는 계약. ScatterPlot/BarGraph의 Drawer가 구현.

#### 8. `IScatterDrawer` (교차 인터페이스)

```java
package makeagraph.drawer;

import makeagraph.data.ScatterPlotData;

public interface IScatterDrawer extends ITitle, IAxis<ScatterPlotData>, IAxisPlot<ScatterPlotData> {
}
```

- ScatterPlot의 drawer 필드 타입. 컴파일 타임에 `ScatterPlotData` 보장.

#### 9. `IBarDrawer` (교차 인터페이스)

```java
package makeagraph.drawer;

import makeagraph.data.BarGraphData;

public interface IBarDrawer extends ITitle, IAxis<BarGraphData>, IAxisPlot<BarGraphData> {
}
```

### 입력 계층 인터페이스

#### 10. `IInputSource`

```java
package makeagraph.input;

public interface IInputSource {
    Object readObject();
    Object readObject(String prompt);
    void close();
}
```

- 범용 입력 추상화. 반환 타입 `Object`는 설계 의도 (향후 제네릭화 가능).
- `readObject(prompt)`: 안내 메시지를 source에 위임하는 오버로드. `ConsoleInput`은 prompt를 `System.out`에 출력한 뒤 입력을 받고, `GUIInput`은 대화 상자 레이블로 사용.
- `readObject()`는 `readObject("")`를 호출하는 편의 메서드로 구현.
- handler는 `source.readObject("dim을 입력하세요")`처럼 프롬프트를 인자로 전달하여 안내 출력을 source에 위임한다.

#### 11. `ITypeSelector`

```java
package makeagraph.input;

public interface ITypeSelector {
    String selectType(IInputSource source);
}
```

- 그래프 타입 판단 계약. 안내 출력은 구현체 책임.

#### 12. `IGraphDataInputHandler<T, E>`

```java
package makeagraph.input;

import makeagraph.data.GraphMetadata;

public interface IGraphDataInputHandler<T, E> {
    T readData(IInputSource source);
    GraphMetadata readMetadata(IInputSource source);
    E parseData(IInputSource source);
    IDataParser<E> createParser();
}
```

- `T` = Data 전체 (ScatterPlotData, BarGraphData)
- `E` = 원소 타입 (Point, Pair<String, Double>)
- **호출 순서 계약**: `readData()`를 먼저 호출해야 함. `readMetadata()`와 `createParser()`는 `readData()`에서 설정된 내부 상태(dim 등)에 의존.
- `createParser()`: 실시간 모드에서 raw 문자열을 E로 변환하는 파서를 반환. `parseData()`와 동일한 파싱 로직을 공유 — `parseData()`는 입력 읽기 + 파싱, `createParser()`는 파싱만 추출. 비동기 프롬프트에서 사용될 예정.

> **참고**: `IDataParser<E>`는 함수형 인터페이스로 같은 패키지에 정의:
> ```java
> package makeagraph.input;
> 
> public interface IDataParser<E> {
>     E parse(String raw);
> }
> ```

### 출력 계층 인터페이스

#### 13. `IRenderer`

```java
package makeagraph.renderer;

import java.util.List;

public interface IRenderer {
    void print(List<String> lines);
}
```

### 세션 인터페이스

#### 14. `ISession`

```java
package makeagraph.session;

public interface ISession {
    void start();
    void stop();
    void await();
}
```

- `start()`: 논블로킹 — 내부 스레드 생성 후 즉시 반환.
- `stop()`: 세션 종료 신호.
- `await()`: 세션 완료 대기 (내부 스레드 join).

---

## 주의사항

1. 인터페이스만 정의. 구현체는 이후 단계에서 작성.
2. 제네릭 타입 파라미터를 정확히 적용 — `IAxis<T>`, `IAxisPlot<T>`, `IGraphDataInputHandler<T, E>`.
3. 교차 인터페이스(`IScatterDrawer`, `IBarDrawer`)가 제네릭을 구체 타입으로 바인딩하는 점 확인.
4. `IGraphObserver`는 1단계에서 이미 생성되었으므로 여기서는 생략.
5. 패키지 간 import가 정확한지 확인 (특히 `data` 패키지의 타입을 `drawer`, `input` 패키지에서 참조).
