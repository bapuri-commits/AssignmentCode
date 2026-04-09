# MakeAGraph 설계 정리 Final (v4.9.3)

> v4.9.2 대비 변경사항:
> - **[명세 수정]** ScatterPlotDrawer.drawAxis() 레이블 매핑 — 존재하지 않는 getLabel(int) 제거, 현재 getXLabel()/getYLabel() 기반 index 분기로 교체. U1 확정 시 getLabel(i)로 통일 예정 명시
> - **[흐름도 보완]** run() view 명령어 처리에 빈 목록 분기(BarGraph → "전환할 뷰가 없습니다") 추가
> - **[표현 교정]** "axisMapping 전환 자체를 차단" → "콘솔 view 명령어를 통한 뷰 목록 비노출"로 정정. swapAxes()/setView() 프로그래밍 API는 여전히 호출 가능함 명시
> - **[예시 보완]** parseData() 코드 예시에 readObject(prompt) 사용 형태 반영. readData() 내부 prompt 전달 패턴 예시 추가
> - **[경미]** PieChartDrawer의 gridHeight/gridWidth 미추가 이유 명시 (렌더링 방식 미확정)

---

## 전체 클래스 목록

### 인터페이스

| 인터페이스 | 역할 | 구현체 |
|-----------|------|--------|
| `IInputSource` | 동기 블로킹 입력 소스. readObject(prompt) 오버로드로 안내 출력 위임 지원 | `ConsoleInput`, `FileInput`(미래), `GUIInput`(미래) |
| `IRealtimeSource<E>` | 비동기 push 스트림 소스 (제네릭) | `SocketRealtimeSource<E>` |
| `IDataParser<E>` | raw 문자열 → 원소 타입 변환 (함수형) | handler의 `createParser()` 반환 |
| `RealtimeSourceFactory` | 실시간 소스 생성 팩토리 | `SocketRealtimeSourceFactory` |
| `ITypeSelector` | 그래프 타입 판단 | `ConsoleTypeSelector` |
| `IGraphDataInputHandler<T, E>` | 타입별 데이터/메타 입력, 파싱, 파서 생성 | `ScatterConsoleInputHandler`, `BarConsoleInputHandler` |
| `IGraph<T>` | 그래프 그리기 | `AbstractGraph`(추상), `ScatterPlot`, `BarGraph`, `PieChart`(미래) |
| `IDataAppendable<E>` | 데이터 원소 추가 전용 | `ScatterPlot`, `BarGraph`, `PieChart`(미래) |
| `IBatchAppendable<E>` | 배치 처리 전용 (suspend/resume). IDataAppendable 확장 | `ScatterPlot`, `BarGraph`, `PieChart`(미래) |
| `IViewControllable` | 축/뷰 전환 (축 있는 그래프만) | `AbstractAxisGraph`(추상), `ScatterPlot`, `BarGraph` |
| `IGraphObserver` | 데이터 변경 통지 수신 | `AbstractGraph`(추상), `ScatterPlot`, `BarGraph`, `PieChart`(미래) |
| `IRenderer` | 출력 추상화 | `TextRenderer`, `GUIRenderer`(미래) |
| `ISession` | 세션 시작/종료 통일 | `GraphSession<T, E>`, `RealtimeGraphSession<T, E>` |
| `ITitle` | 제목 그리기 | `ScatterPlotDrawer`, `BarGraphDrawer` |
| `IAxis<T>` | 축 그리기 (제네릭) | `ScatterPlotDrawer`, `BarGraphDrawer` |
| `IPlot<T>` | 데이터 영역 그리기 (축 없는 그래프용) | `PieChartDrawer`(미래) |
| `IAxisPlot<T>` | 데이터 영역 그리기 (축 매핑 포함) | `ScatterPlotDrawer`, `BarGraphDrawer` |
| `IScatterDrawer` | 산포도 Drawer 교차 인터페이스 | `ScatterPlotDrawer` |
| `IBarDrawer` | 막대그래프 Drawer 교차 인터페이스 | `BarGraphDrawer` |
| `IPieDrawer` | 파이차트 Drawer 교차 인터페이스 (미래) | `PieChartDrawer`(미래) |

### 추상 클래스

| 클래스 | 역할 | 계층 |
|--------|------|------|
| `AbstractGraph<T, D>` | draw() 골격 (Template Method), onDataChanged() 공통화 | 그래프 |
| `AbstractAxisGraph<T, D>` | 축 있는 그래프 공통 — drawBody(), IViewControllable 구현 | 그래프 |

### 구체 클래스

| 클래스 | 역할 | 계층 |
|--------|------|------|
| `ConsoleInput` | 콘솔 입력 구현 | 입력 |
| `ConsoleTypeSelector` | 콘솔에서 그래프 타입 질의 | 입력 |
| `ScatterConsoleInputHandler` | 산포도 데이터/메타 입력 | 입력 |
| `BarConsoleInputHandler` | 막대그래프 데이터/메타 입력 | 입력 |
| `SocketRealtimeSource<E>` | 소켓 기반 실시간 스트림 | 입력 |
| `SocketRealtimeSourceFactory` | 소켓 스트림 소스 팩토리 | 입력 |
| `GraphDirector` | 조립 순서 제어 (Director) | 조립 |
| `GraphFactory` | 그래프 객체 생성 (static Factory) | 조립 |
| `BuildResult<T, E>` | 조립 결과 전달 객체 — 내부용 (graph + appendable + handler) | 조립 |
| `GraphSession<T, E>` | 대화형 세션 관리 | 세션 |
| `RealtimeGraphSession<T, E>` | 실시간 세션 관리 | 세션 |
| `ScatterPlot` | 산포도 (AbstractAxisGraph 상속) | 그래프 |
| `BarGraph` | 막대그래프 (AbstractAxisGraph 상속) | 그래프 |
| `ScatterPlotData` | 산포도 데이터 (Subject) | 데이터 |
| `BarGraphData` | 막대그래프 데이터 (Subject) | 데이터 |
| `GraphMetadata` | 제목/레이블 메타정보 | 데이터 |
| `Point` | n차원 좌표 | 데이터 |
| `ObserverSupport` | 옵저버 관리 헬퍼 | 데이터 |
| `DataQueue<E>` | 스레드 간 데이터 버퍼 (원소 타입) | 실시간 |
| `ScatterPlotDrawer` | 산포도 그리기 로직 | 그리기 |
| `BarGraphDrawer` | 막대그래프 그리기 로직 | 그리기 |
| `ScaleCalculator` | 축 범위/눈금/좌표 변환 | 그리기 |
| `ViewController` | 축 매핑/뷰 전환 | 그리기 |
| `TextRenderer` | 콘솔 텍스트 출력 | 출력 |
| `Range` | min/max 값 쌍 | 유틸 |

### 열거형

| 열거형 | 값 |
|--------|-----|
| `Direction` | `VERTICAL`, `HORIZONTAL` |

---

## 전체 흐름

> 아래는 **콘솔 대화형** 기준 흐름. 실시간 스트림 흐름은 `RealtimeGraphSession` 섹션 참조.

```
Main
 ├── 1. ConsoleInput 생성 (IInputSource)
 ├── 2. TextRenderer 생성 (IRenderer)
 ├── 3. ConsoleTypeSelector 생성 (ITypeSelector)
 ├── 4. GraphDirector(typeSelector) 생성
 ├── 5. GraphDirector.construct(source, renderer) → GraphSession<T, E> 반환
 ├── 6. session.start()
 └── 7. session.await()

GraphDirector.construct(source, renderer)
 ├── typeSelector.selectType(source)                     → type 판단
 ├── type에 따라 IGraphDataInputHandler<T, E> 구현체 선택
 ├── handler.readData(source)                            → T data 생성 (최소 1건 보장)
 ├── handler.readMetadata(source)                        → GraphMetadata 생성
 ├── GraphFactory.createXxx(data, metadata, renderer)
 │     ├── Drawer, ViewController 생성
 │     └── → IGraph<T> 생성 (모두 주입)
 ├── data.addObserver(graph)                             → 옵저버 등록
 └── return GraphSession<T, E>(source, graph, appendable, handler)

GraphSession.run()
 ├── graph.draw()                ← 최초 출력 (data 반드시 비어있지 않음)
 ├── 명령어 루프
 │     ├── source.readObject() → 명령어 읽기
 │     ├── "add"
 │     │     ├── handler.parseData(source) : E     ← 다음 줄에서 데이터 읽기
 │     │     └── appendable.appendData(point)
 │     │              ↓ [ScatterPlot.appendData() → data.append()]
 │     │              └── data.append(point)          ← 제네릭으로 캐스팅 불필요
 │     │                   └── notifyObservers() [private]
 │     │                        └── graph.onDataChanged()       ← AbstractGraph 구현
 │     │                             └── graph.draw()           ← AbstractGraph Template Method
 │     ├── "view"
 │     │     ├── 뷰 목록 출력 (getAvailableViews())
 │     │     ├── 번호 입력 대기
 │     │     └── setView(선택된 축 조합) → draw()
 │     ├── "exit" → 루프 종료
 │     └── 기타   → "add, view, exit 중 입력하세요" 안내, 무시
 └── source.close()
```

> 아래는 **콘솔 실시간** 기준 흐름. 대화형과의 핵심 차이: 명령어 루프 없음, 2스레드 구조(receiverThread + workerThread), 종료는 스트림 종료 또는 외부 stop()으로만.

```
Main (실시간)
 ├── 1. ConsoleInput 생성 (초기 설정용)
 ├── 2. TextRenderer 생성
 ├── 3. ConsoleTypeSelector 생성
 ├── 4. GraphDirector(typeSelector) 생성
 ├── 5. SocketRealtimeSourceFactory(address) 생성
 ├── 6. director.constructRealtimeSession(source, renderer, factory) → ISession 반환
 │     ├── constructGraph(source, renderer) → BuildResult
 │     │     ├── typeSelector.selectType(source)          → type 판단
 │     │     ├── handler.readData(source)                 → 초기 데이터 수집 (콘솔 블로킹)
 │     │     ├── handler.readMetadata(source)             → 메타데이터 수집
 │     │     ├── GraphFactory.createXxx(data, metadata, renderer) → graph 생성
 │     │     └── data.addObserver(graph)
 │     └── assembleRealtimeSession(result, factory)
 │           ├── handler.createParser()                   → IDataParser<E>
 │           ├── new DataQueue<E>(capacity)
 │           ├── factory.create(parser)                   → IRealtimeSource<E>
 │           ├── stream.stream(queue)                     → receiverThread 시작 (소켓 → parse → enqueue)
 │           └── return RealtimeGraphSession(queue, graph, appendable, () -> stream.stop())
 ├── 7. session.start()                                   → workerThread에서 run() 실행
 └── 8. session.await()

RealtimeGraphSession.run()                                 [workerThread]
 ├── graph.draw()                    ← 최초 출력 (초기 데이터만. 스트림 데이터는 queue에 대기 중)
 └── dequeue 루프
       ├── data = queue.dequeue()    ← 블로킹 대기
       ├── data == null → 루프 종료  ← poison pill
       ├── appendable.suspendObservers()
       ├── appendable.appendData(data)   ← 첫 건
       ├── drain: tryDequeue()로 추가 건 배치 소비 (appendable.appendData 반복)
       ├── appendable.resumeObservers()  → draw() 1회 (배치 전체 반영)       └── (반복)

종료 경로 A — 스트림 자체 종료:
 receiverThread 내부 → queue.close() → poison pill → workerThread 루프 종료
 → onStop.run() (멱등) → session.await() 해제

종료 경로 B — 외부에서 session.stop():
 session.stop() → onStop.run() → stream.stop() (멱등) → queue.close() → poison pill
 → workerThread 루프 종료 → workerThread.join()

스레드 구조 비교:
 대화형:  [workerThread] 명령어 루프 (readObject → add/view/exit)
 실시간:  [receiverThread] 소켓→parse→enqueue  |  [workerThread] dequeue→append→draw
```

---

## 클래스 구조


### 입력 계층 — 브리지 패턴 + 단계 분리

세 가지 독립적인 변경 축:
- **입력 방식 축**: 콘솔 / 파일 / GUI → `IInputSource`
- **타입 선택 축**: 그래프 종류 판단 → `ITypeSelector`
- **데이터 입력 축**: 그래프 종류별 data 생성 및 파싱 → `IGraphDataInputHandler<T, E>`

```
IInputSource                   ITypeSelector
+ readObject() : Object        + selectType(source) : String
+ readObject(prompt : String) : Object   ← 안내 출력 포함 오버로드. readObject()는 readObject("")를 호출하는 편의 메서드
+ close() : void
      ↑                               ↑
ConsoleInput                   ConsoleTypeSelector  ← 안내 출력은 구현체 책임
FileInput (미래)
GUIInput  (미래)

IDataParser<E>                                ← 함수형 인터페이스. raw 문자열 → 원소 타입 변환
+ parse(raw : String) : E

IRealtimeSource<E>                            ← IInputSource와 별개. push 방식. 제네릭
+ stream(queue : DataQueue<E>) : void
+ stop() : void

RealtimeSourceFactory                         ← Main이 주입. 스트림 소스 생성 팩토리
+ <E> create(parser : IDataParser<E>) : IRealtimeSource<E>
      ↑
SocketRealtimeSourceFactory(address)          ← 소켓 주소를 보유, create() 시 SocketRealtimeSource<E> 생성

IGraphDataInputHandler<T, E>
+ readData(source : IInputSource)     : T
+ readMetadata(source : IInputSource) : GraphMetadata
+ parseData(source : IInputSource)    : E               ← 원소 타입 (Point, Pair 등)
+ createParser()                      : IDataParser<E>   ← 실시간 모드용 파서 생성
      ↑
ScatterConsoleInputHandler implements IGraphDataInputHandler<ScatterPlotData, Point>
BarConsoleInputHandler     implements IGraphDataInputHandler<BarGraphData, Pair<String, Double>>
```

- `IInputSource`: 날것의 스트림 읽기만 담당. **동기 블로킹 방식** — GUI 전환 시 `GUIInput` 구현체 추가로 대응. 블로킹 루프 문제는 `GraphSession` 교체로 해결 (GUI 전환 경로 섹션 참조)
- `readObject(prompt : String)` 오버로드: handler가 "dim을 입력하세요" 같은 안내를 source에 위임할 수 있도록 추가한다. `ConsoleInput`은 프롬프트를 `System.out`에 출력한 뒤 입력을 받고, `GUIInput`은 프롬프트를 대화 상자 제목/레이블로 사용한다. 기존 `readObject()`는 `readObject("")`를 호출하는 편의 메서드로 유지하여 호출자 변경 없이 기존 코드와 호환된다
- `IInputSource.readObject()`의 반환 타입이 `Object`인 이유: `IInputSource`는 콘솔·파일·GUI 등 다양한 입력을 추상화하는 범용 인터페이스이다. 현재 모든 사용처가 `String`으로 캐스팅하므로 `String`으로 고정하면 캐스팅을 제거할 수 있다는 주장이 있으나, `IInputSource`를 향후 제네릭화(`IInputSource<T>`)하면 `String`과 바이너리를 모두 수용할 수 있다. 현재는 `Object`로 두되, 캐스팅 지점이 명령어 루프 1곳(`GraphSession.run()`)으로 한정되어 있으므로 실질적 위험은 낮다. 이 캐스팅은 A안의 "데이터 타입 캐스팅 제거"와 다른 성격이다 — 입력 인터페이스의 범용성 유지를 위한 설계 트레이드오프이다
- `ITypeSelector`: 타입 읽기 계약만 정의. 안내 출력은 구현체 담당
- `IGraphDataInputHandler<T, E>`: `readData()`는 `T`(Data 전체) 반환, `parseData()`는 `E`(원소 타입) 반환, `createParser()`는 실시간 모드에서 raw 문자열을 E로 변환하는 파서를 반환
- **호출 순서 계약**: `readData()`를 먼저 호출해야 한다. `readMetadata()`와 `createParser()`는 `readData()`에서 설정된 내부 상태(dim 등)에 의존한다. `constructGraph()`가 이 순서를 강제하므로 실사용에서 위반되지 않지만, 구현체 작성 시 이 계약을 인지해야 한다
- **`createParser()`가 이 인터페이스에 있는 근거**: `parseData()`와 `createParser()`는 동일한 파싱 로직을 공유한다 — `parseData()`는 입력 읽기 + 파싱, `createParser()`는 파싱만 추출. "타입별 데이터 파싱"이라는 하나의 책임으로 응집되므로 별도 인터페이스 분리(ISP) 없이 현행 유지한다
- 입력 방식 추가 → `IInputSource` 구현체만 추가 (OCP)
- 그래프 종류 추가 → `IGraphDataInputHandler<T, E>` 구현체만 추가 (OCP)

**handler와 UI 모드의 관계:**

`ScatterConsoleInputHandler`/`BarConsoleInputHandler`는 이름에 "Console"이 붙어 있지만, 핵심 로직은 **파싱**(문자열 → Point/Pair 변환)이다. 안내 메시지 출력("dim을 입력하세요" 등)은 handler 내부에서 `System.out.println()`으로 직접 하지 않고, `IInputSource` 구현체에 위임하는 것이 원칙이다 — "안내 출력은 구현체 책임"이라는 기존 원칙과 일관. handler가 `source.readObject("dim을 입력하세요")`처럼 프롬프트를 인자로 전달하면, `ConsoleInput`은 프롬프트를 `System.out`에 출력하고 입력을 받고, `GUIInput`은 대화 상자를 띄우고 입력을 받는다. handler 자체는 UI 모드에 무관하게 재사용 가능하다. 다만 handler 내부에서 콘솔 전용 출력을 직접 하는 코드가 있다면 `IInputSource.readObject(prompt)` 호출로 이동해야 한다. handler 이름의 "Console" 접두어는 최초 구현 환경을 반영한 것이며, handler가 진정으로 UI 무관하다면 GUI 도입 시 이름 변경(`ScatterInputHandler` 등)을 고려한다

**readData() 종료 조건:**

`readData()`는 반복 입력을 수집하여 T(Data 객체)를 생성한다. 종료 조건은 다음과 같다:

```
[공통 정책]
- 빈 줄 입력 (Enter만 입력) → 데이터 입력 종료
- 최소 1건 보장: 빈 줄이 첫 입력이면 "최소 1건 필요" 안내 후 재입력 요구
- 잘못된 형식 입력 → 에러 메시지 출력 후 해당 줄 무시, 루프 계속

[ScatterConsoleInputHandler.readData()]
1. dim 입력 → 정수 검증
2. 반복: "(v1, v2, ..., vdim)" 형태 입력
   - 빈 줄 → 종료 (1건 이상 입력된 경우)
   - dim 불일치 → 에러 출력, 무시
   - 숫자 아닌 값 → 에러 출력, 무시

[BarConsoleInputHandler.readData()]
1. direction 입력 → VERTICAL/HORIZONTAL 검증
2. 반복: "(카테고리, 값)" 형태 입력
   - 빈 줄 → 종료 (1건 이상 입력된 경우)
   - 값이 숫자 아닌 경우 → 에러 출력, 무시
```

**`ScatterConsoleInputHandler.readData()` 입력 순서:**
```
1. dim(차원 수) 입력     ← "몇 차원 데이터입니까?" 먼저 물음
   → dim을 구현체 필드로 저장 (readMetadata()에서 레이블 개수 결정에 사용)
2. (v1, v2, ..., vdim) 형태로 데이터 반복 입력
   → 빈 줄 입력 시 종료 (최소 1건 보장)
→ Point.of(v1, v2, ..., vdim) 생성
현재 요구사항은 2D이므로 dim=2를 입력. 3D 이상도 동일한 경로로 처리.
```

```
// ScatterConsoleInputHandler 필드
- dim : int    ← readData()에서 입력받아 설정, readMetadata()에서 참조
```

**`ScatterConsoleInputHandler.readMetadata()` 입력 순서:**
```
1. 그래프 제목 입력
2. dim개의 축 레이블 입력   ← dim 필드 참조
→ GraphMetadata 생성
```

**`BarConsoleInputHandler.readData()` 입력 순서:**
```
1. direction(방향) 입력   ← VERTICAL / HORIZONTAL 먼저 물음
2. (카테고리, 값) 쌍 반복 입력
   → 빈 줄 입력 시 종료 (최소 1건 보장)
→ BarGraphData(direction) 생성                ← direction은 생성자 주입. 불변
```

---

### 데이터 계층 — 옵저버 패턴 (동기) + ObserverSupport

**ObserverSupport — 옵저버 관리 헬퍼 클래스:**

`ScatterPlotData`와 `BarGraphData`의 옵저버 관리 코드(`addObserver`, `notifyObservers`)가 완전히 동일하므로, 별도 헬퍼 클래스로 분리하여 중복을 제거한다. 상속이 아닌 **has-a 구성**으로, Data 클래스의 필드 구조를 침범하지 않는다.

```
ObserverSupport
- observers  : List<IGraphObserver>
- suspended  : boolean                          ← 배치 처리 중 알림 일시 중단
- dirty      : boolean                          ← suspend 중 변경 발생 여부 추적
+ addObserver(observer : IGraphObserver) : void
+ notifyObservers() : void                      ← suspended 상태면 dirty=true로만 기록
+ suspend() : void                              ← suspended=true, dirty=false
+ resume() : void                               ← suspended=false. dirty면 notifyObservers() 1회. dirty 아니면 무호출
```

> **`removeObserver()`는 의도적으로 생략한다.** 현재 구조에서 graph의 생명주기는 data와 동일하다 — 둘 다 `GraphFactory`에서 생성되고 `GraphSession`이 함께 보유한다. 옵저버 해제가 필요한 시점(GUI에서 그래프 교체 등)이 발생하면 그 시점에 추가한다 (YAGNI).

**suspend()/resume() — 배치 처리 지원:**

실시간 모드에서 데이터가 고빈도로 유입될 때, 매 `append()`마다 `notifyObservers() → draw()`가 호출되면 성능 문제가 발생한다. `suspend()`로 알림을 일시 중단한 뒤 여러 건을 한꺼번에 추가하고, `resume()`으로 알림을 재개하면 draw()가 배치당 1회만 호출된다. `append()` 메서드 자체는 변경하지 않는다 — 항상 `notifyObservers()`를 호출하되, suspended 상태면 내부적으로 무시한다. 대화형 모드에서는 suspend/resume을 호출하지 않으므로 기존 동작에 영향 없음.

```
ScatterPlotData (Subject)
- points          : List<Point>
- observerSupport : ObserverSupport         ← has-a
+ append(point : Point)                      ← 내부에서 observerSupport.notifyObservers()
+ getPoints() : List<Point>
+ addObserver(o : IGraphObserver)            ← observerSupport.addObserver(o) 위임
+ suspendObservers() : void                  ← observerSupport.suspend() 위임
+ resumeObservers() : void                   ← observerSupport.resume() 위임

BarGraphData (Subject)
- bars            : List<Pair<String, Double>>
- direction       : Direction                ← 생성자 주입. 불변
- observerSupport : ObserverSupport         ← has-a
+ BarGraphData(direction : Direction)        ← 생성자에서 direction 설정
+ append(bar : Pair<String, Double>)         ← 내부에서 observerSupport.notifyObservers()
+ getBars() : List<Pair<String, Double>>
+ getDirection() : Direction
+ addObserver(o : IGraphObserver)            ← observerSupport.addObserver(o) 위임
+ suspendObservers() : void                  ← observerSupport.suspend() 위임
+ resumeObservers() : void                   ← observerSupport.resume() 위임

GraphMetadata                                ← 불변. 생성자에서만 설정
- title  : String
- xLabel : String
- yLabel : String
+ GraphMetadata(title, xLabel, yLabel)
+ getTitle() : String
+ getXLabel() : String
+ getYLabel() : String

enum Direction { VERTICAL, HORIZONTAL }
```

- `append()` → 내부에서 `observerSupport.notifyObservers()` 자동 호출
- `suspendObservers()/resumeObservers()` → `observerSupport.suspend()/resume()` 위임. `IBatchAppendable<E>` 계약 이행
- 옵저버 패턴은 **동기 구조**로 확정
- `Main`은 `data` 직접 참조 없음
- `ObserverSupport`는 순수 위임 클래스 — 상속 없이 중복 제거
- `BarGraphData.direction`은 생성자에서 주입하고 이후 변경하지 않는다. `readData()` 시점에 결정되며 런타임 변경 시 draw 트리거 없이 상태가 불일치하는 문제를 원천 차단
- `GraphMetadata`는 불변이다. `readMetadata()` 시점에 생성되며 이후 변경하지 않는다. 런타임 변경 시 draw 트리거가 없으므로 setter를 제공하면 상태 불일치가 발생한다. 메타데이터 변경이 필요해지면 옵저버 패턴을 도입하는 시점에 setter를 추가한다

**[참고] 실시간/비동기 확장 시 고려사항**

동기 구조의 한계: `data.append()` → `notifyObservers()` → `draw()`가 순차 실행되므로, `draw()`가 완료되기 전에 다음 데이터가 들어오면 유실될 수 있다.

**대화형 모드에서는 문제없음** — 사람이 타이핑하는 속도가 `draw()` 완료보다 느리므로 실질적 위험 없음.

**실시간 스트림에서는 `RealtimeGraphSession`으로 해결** — receiverThread가 queue에 버퍼링하고 workerThread가 배치 소비하므로 옵저버 패턴 자체를 바꿀 필요 없음. 옵저버는 workerThread 컨텍스트에서만 실행됨.
→ 실시간 확장 설계는 `RealtimeGraphSession` / `DataQueue` / `IRealtimeSource` 섹션 참조.

**[참고] GraphMetadata 처리 방향 — 현행 유지**

`GraphMetadata`는 `title`, `xLabel`, `yLabel`만 보유. 그래프별 전용 속성은 각 Data 클래스에 위치. 그래프 종류 증가로 Metadata 분기가 복잡해지는 시점에 `BarGraphMetadata extends GraphMetadata` 도입.

---

### 그래프 계층 — 인터페이스

```
<<interface>>
IGraph<T>
+ draw() : void

<<interface>>
IDataAppendable<E>                               ← IGraph에서 분리 (SRP). E = 원소 타입
+ appendData(point : E) : void

<<interface>>
IBatchAppendable<E> extends IDataAppendable<E>   ← 배치 처리 전용. 실시간 세션만 사용 (ISP)
+ suspendObservers() : void                      ← 옵저버 알림 일시 중단
+ resumeObservers() : void                       ← 알림 재개. suspend 중 변경이 있었으면 notifyObservers() 1회

<<interface>>
IViewControllable                                ← 축 있는 그래프만 구현 (ISP)
+ swapAxes(a : int, b : int) : void
+ setView(axes : int[]) : void
+ getAvailableViews() : List<int[]>

<<interface>>
IGraphObserver
+ onDataChanged() : void

<<interface>>    <<interface>>         <<interface>>           <<interface>>
ITitle           IAxis<T>              IPlot<T>                IAxisPlot<T>
+ drawTitle(     + drawAxis(           + drawPlot(             + drawPlot(
    metadata         data : T,             data : T                data : T,
  : GraphMetadata    metadata             ) : List<String>         axisMapping)
) : List<String>   : GraphMetadata,                             : List<String>
                     axisMapping)
                   : List<String>
```

- `IPlot<T>`: 축 매핑 없이 데이터를 그리는 계약. PieChart 등 축 없는 그래프의 Drawer가 구현
- `IAxisPlot<T>`: 축 매핑을 받아 데이터를 그리는 계약. ScatterPlot/BarGraph 등 축 있는 그래프의 Drawer가 구현
- 두 인터페이스는 상속 관계 없이 독립 — "축 매핑 필요 여부"가 다른 변경의 이유이므로 ISP에 따라 분리

- `IAxis<T>`, `IPlot<T>`, `IAxisPlot<T>` 제네릭화 — 구현체에서 캐스팅 불필요 (A안 전환)
- `IAxis<T>.drawAxis()`는 `metadata`를 파라미터로 받아 축 레이블을 그릴 수 있다. **단, U1 미해결로 `GraphMetadata`는 `xLabel`/`yLabel` 2개 필드만 보유하므로, dim≥3에서는 3번째 이후 축 레이블을 표시할 수 없다.** 시그니처는 n차원 대응이 완료되었으나 데이터 구조가 아직 미달인 상태이다
- `IDataAppendable<E>` 분리 — `IGraph`는 "그리기"만, 데이터 추가는 별도 계약

**IDataAppendable 분리 근거:**

`IGraph`의 핵심 책임은 "그래프를 그리는 것"이다. 데이터 추가(`appendData`)는 다른 변경의 이유를 갖는다 — 실시간 모드에서는 `RealtimeGraphSession`이, GUI 동기 모드에서는 이벤트 핸들러가, 대화형에서는 `GraphSession`이 호출한다. 호출자가 다양하고 호출 맥락도 다르므로 별도 인터페이스로 분리한다.

```
GraphSession<T, E>가 아는 것:       IGraph<T> + IDataAppendable<E>
RealtimeGraphSession<T, E>가 아는 것: IGraph<T> + IBatchAppendable<E>
GUI 이벤트 핸들러(동기)가 아는 것:   IDataAppendable<E> (draw는 옵저버가 자동 호출)
```

**원소 타입 제네릭 — A안 확정:**

`IGraph<T>`의 T는 Data 클래스 전체(`ScatterPlotData`)를, `IDataAppendable<E>`의 E는 원소 타입(`Point`)을 의미한다. 두 제네릭은 서로 다른 인터페이스에 독립적으로 존재하므로, `IGraph<T, E>` 같은 복합 파라미터는 불필요하다.

```
ScatterPlot implements IGraph<ScatterPlotData>, IBatchAppendable<Point>, ...
BarGraph    implements IGraph<BarGraphData>,    IBatchAppendable<Pair<String, Double>>, ...
```

이로써 `appendData(E point)` 시그니처에서 컴파일 타임 타입 보장이 완성된다. 기존 B안 4개 지점이 모두 해소됨.

**그래프별 Drawer 인터페이스 — 교차 인터페이스 (제네릭화):**

```
<<interface>>
IScatterDrawer extends ITitle, IAxis<ScatterPlotData>, IAxisPlot<ScatterPlotData>
← ScatterPlot의 drawer 필드 타입. 컴파일 타임에 ScatterPlotData 보장

<<interface>>
IBarDrawer extends ITitle, IAxis<BarGraphData>, IAxisPlot<BarGraphData>
← BarGraph의 drawer 필드 타입

<<interface>>
IPieDrawer extends ITitle, IPlot<PieChartData>       (미래, IAxis/IAxisPlot 없음)
← PieChart의 drawer 필드 타입. 파이차트는 축이 없으므로 IAxis, IAxisPlot 제외
```

교차 인터페이스를 사용하면 "이 drawer는 title, axis, plot을 모두 구현한다"를 컴파일 타임에 단일 타입으로 표현할 수 있다. `IPieDrawer`에 `IAxis`/`IAxisPlot`이 없는 것도 "파이차트 Drawer는 축 그리기와 축 매핑 기반 그리기가 없다"를 타입으로 강제하는 효과.

**그래프 상속 계층:**
```
AbstractGraph<T, D extends ITitle>
  implements IGraph<T>, IGraphObserver
  │
  ├── AbstractAxisGraph<T, D extends ITitle & IAxis<T> & IAxisPlot<T>>
  │     implements IViewControllable
  │     │
  │     ├── ScatterPlot extends AbstractAxisGraph<ScatterPlotData, IScatterDrawer>
  │     │     implements IBatchAppendable<Point>
  │     │
  │     ├── BarGraph extends AbstractAxisGraph<BarGraphData, IBarDrawer>
  │     │     implements IBatchAppendable<Pair<String, Double>>
  │     │
  │     └── LineGraph (미래) extends AbstractAxisGraph<LineGraphData, ILineDrawer>
  │           implements IBatchAppendable<Point>
  │
  └── PieChart (미래) extends AbstractGraph<PieChartData, IPieDrawer>
        implements IBatchAppendable<PieChartEntry>
```

---

### 그래프 계층 — Drawer (그리기 로직 분리)

그리기 로직은 자주 바뀌고 그래프마다 완전히 다르므로 별도 클래스로 분리. `ScaleCalculator`를 필드로 보유하여 좌표 변환을 내부에서 처리.

```
ScatterPlotDrawer implements IScatterDrawer          ← IAxisPlot<ScatterPlotData> 경유
- scaleCalc  : ScaleCalculator
- gridHeight : int                                   ← 기본값 상수 (예: 20). 생성자에서 주입 가능
- gridWidth  : int                                   ← 기본값 상수 (예: 60). 생성자에서 주입 가능
+ drawTitle(metadata : GraphMetadata) : List<String>
+ drawAxis(data : ScatterPlotData, metadata : GraphMetadata, axisMapping : int[]) : List<String>
+ drawPlot(data : ScatterPlotData, axisMapping : int[]) : List<String>   ← IAxisPlot 계약

BarGraphDrawer implements IBarDrawer                 ← IAxisPlot<BarGraphData> 경유
- scaleCalc  : ScaleCalculator
- gridHeight : int                                   ← 기본값 상수 (예: 20). 생성자에서 주입 가능
- gridWidth  : int                                   ← 기본값 상수 (예: 60). 생성자에서 주입 가능
+ drawTitle(metadata : GraphMetadata) : List<String>
+ drawAxis(data : BarGraphData, metadata : GraphMetadata, axisMapping : int[]) : List<String>
+ drawPlot(data : BarGraphData, axisMapping : int[]) : List<String>      ← IAxisPlot 계약

PieChartDrawer implements IPieDrawer    (미래)        ← IPlot<PieChartData> 경유
- scaleCalc : ScaleCalculator
+ drawTitle(metadata : GraphMetadata) : List<String>
+ drawPlot(data : PieChartData) : List<String>                           ← IPlot 계약 (axisMapping 없음)
  ※ gridHeight/gridWidth: PieChart의 텍스트 렌더링 방식이 미확정이므로 현 시점에서 필드를 추가하지 않는다.
     grid 방식을 채택하면 ScatterPlotDrawer·BarGraphDrawer와 동일하게 추가한다.
```

- A안 전환으로 Drawer 내부 캐스팅 완전 제거 — `drawAxis(data : ScatterPlotData, ...)`처럼 구체 타입이 시그니처에 명시
- `ScaleCalculator`는 Drawer 필드로 보유. 조립자는 직접 알 필요 없음
- 독립적으로 테스트 가능

**ScatterPlotDrawer.drawAxis()의 축 레이블 매핑:**

현재 `GraphMetadata`는 `getXLabel()`과 `getYLabel()` 두 개의 레이블 접근자만 보유한다(U1 미해결). 따라서 `drawAxis()`는 `axisMapping[0]==0`이면 `metadata.getXLabel()`을 가로축 레이블로, `axisMapping[0]==1`이면 `metadata.getYLabel()`을 가로축 레이블로 분기하여 접근해야 한다 — 세로축도 동일하게 `axisMapping[1]`로 분기. `axisMapping={0,1}` 기본값이면 x→가로, y→세로가 자연스럽게 대응되고, `axisMapping={1,0}` 전치이면 y 레이블이 가로축, x 레이블이 세로축에 표시된다. 전치 시 레이블이 교차하는 것은 의도된 동작이다.

U1(F안)이 확정되어 `GraphMetadata.labels`가 `List<String>`으로 변경되면, `metadata.getLabel(axisMapping[0])` 형태의 단일 접근으로 통일할 수 있다. 현재는 index→레이블 분기 로직이 `drawAxis()` 구현에 남는다. BarGraph에서는 getAvailableViews() 빈 목록 반환으로 view 명령어 경유 전치가 불가하므로 이 분기가 실행되지 않는다.

---

### 그래프 계층 — ViewController (뷰/축 제어 분리)

`axisMapping` 관련 로직이 `ScatterPlot`과 `BarGraph`에서 완전히 동일하므로 공유 클래스로 분리.

```
ViewController
- dim         : int                                       ← 생성자에서 주입. 불변
- axisMapping : int[]                                     ← 항상 길이 2. 불변 조건
+ ViewController(dim : int)                               ← 생성자. dim 저장, axisMapping = {0, 1}
+ swapAxes(a : int, b : int) : void
+ setView(axes : int[]) : void                            ← axes 길이 반드시 2
+ getAxes() : int[]
+ getAvailableViews() : List<int[]>                       ← 저장된 dim 사용. 파라미터 불필요
```

- `dim`은 생성 시 결정되며 이후 변하지 않음. `GraphFactory`가 Data에서 dim을 추출하여 주입
  - ScatterPlot: `data.getPoints().get(0).size()`
  - BarGraph: `2` (카테고리/값 2축 고정)
- `getAvailableViews()`는 저장된 dim으로 뷰 목록을 생성 → `IViewControllable.getAvailableViews()`와 시그니처 일치
- `axisMapping`은 항상 길이 2 — "n차원 데이터에서 어떤 두 축을 2D로 투영할지"를 나타냄. 3D 이상 데이터도 한 번에 두 축만 선택해서 2D 평면으로 출력하는 구조
- `setView(axes)`에서 `axes.length != 2`이면 구현체가 예외 처리

- `AbstractAxisGraph`가 인스턴스를 보유 (has-a) → ScatterPlot/BarGraph가 각자 보유하던 것을 상위로 이동
- `PieChart`는 보유하지 않음 → `IViewControllable` 미구현과 일치
- `swapAxes()` / `setView()` 후 redraw는 `AbstractAxisGraph`가 담당 (옵저버 경로 아님)

**BarGraph의 IViewControllable — 설계 판단:**

v4.9.0에서 `getAvailableViews()`가 순서 쌍(ordered pair)을 반환하도록 변경되어, BarGraph는 dim=2 고정이므로 `{0,1}`과 `{1,0}` 두 개의 뷰를 반환하게 된다. 그러나 `BarGraphDrawer`는 `axisMapping`을 무시하고 `direction`으로만 렌더링하므로, 두 뷰를 선택해도 출력이 동일하다 — "전환은 되지만 결과가 같다"는 어색한 UX가 발생한다. 따라서 **BarGraph는 `getAvailableViews()`를 오버라이드하여 빈 목록을 반환**하고, `GraphSession`은 "전환할 뷰가 없습니다"를 안내한다. `AbstractAxisGraph`의 기본 구현(`ViewController`에 위임)은 ScatterPlot 등에서 그대로 사용된다. `swapAxes()`·`setView()`는 상속된 상태로 남아 있으므로 프로그래밍 API로는 여전히 호출 가능하지만, `BarGraphDrawer`가 axisMapping을 무시하므로 출력에 영향이 없다 — 이 오버라이드가 막는 것은 axisMapping **전환 호출** 자체가 아니라 콘솔 view 명령어를 통한 **뷰 목록 제공**이다. Direction(VERTICAL/HORIZONTAL) 전환은 축 매핑과 다른 개념(데이터 배치 방향)이므로 `IViewControllable`에 포함시키면 SRP 위반이다.

**IViewControllable 런타임 캐스팅 — 유일한 잔존 지점:**

뷰 출력 패턴에서 `graph`를 `IViewControllable`로 런타임 캐스팅한다. 이것은 A안에서 해소한 "데이터 타입 캐스팅"(Object → Point 등)과는 다른 종류이다. A안의 "캐스팅 제거"는 **데이터 흐름의 타입 안전성** — `appendData(Object)` → `appendData(E)`, `drawAxis(Object)` → `drawAxis(T)` 등 — 을 의미하며, 기능 분기를 위한 인터페이스 캐스팅(`IViewControllable`)은 ISP의 자연스러운 결과이다. PieChart처럼 축이 없는 그래프에서 캐스팅 실패 시 안내 메시지로 처리한다.

**뷰 출력 패턴:**

`setView()`는 내부에서 `draw()`를 직접 호출하므로, 호출 즉시 해당 뷰로 화면이 갱신된다.

```java
// IViewControllable로 캐스팅 필요 — getAvailableViews()는 IGraph가 아닌 IViewControllable에 있음
IViewControllable vc = (IViewControllable) graph;

// 단일 뷰 출력 — 특정 축 조합 하나만 출력
vc.setView(new int[]{0, 2});   // x축과 z축으로 보기 → 즉시 draw()

// 전체 뷰 순회 출력 — 모든 가능한 축 조합을 순서대로 출력
for (int[] axes : vc.getAvailableViews()) {
    vc.setView(axes);          // 각 조합으로 draw() 호출
    // 필요 시 사용자 입력 대기 or 자동 전환
}
```

`getAvailableViews()`가 반환하는 목록 예시 (3차원 데이터, dim=3):
```
{0,1} → x-y 평면
{1,0} → y-x 평면 (전치)
{0,2} → x-z 평면
{2,0} → z-x 평면 (전치)
{1,2} → y-z 평면
{2,1} → z-y 평면 (전치)
```
`getAvailableViews()`는 **순서 쌍(ordered pair)**을 반환한다 — `{0,1}`과 `{1,0}`은 다른 뷰이다(가로축/세로축이 뒤바뀜). n차원 데이터에서 임의의 두 축을 골라 2D로 투영하되, 축 순서도 선택 가능한 구조.

---

### 그래프 계층 — AbstractGraph (Template Method)

그래프 종류가 4종 이상(산포도, 막대, 파이, 꺾은선 + α)으로 확장될 때 draw()/onDataChanged()의 중복을 방지하기 위해 Template Method 패턴을 도입한다.

```
abstract class AbstractGraph<T, D extends ITitle>
    implements IGraph<T>, IGraphObserver

# data       : T                              ← protected. 하위 클래스 접근
# metadata   : GraphMetadata                  ← protected
# renderer   : IRenderer                      ← protected
# drawer     : D                              ← protected. 제네릭 바운드로 타입 보장

+ AbstractGraph(data : T, metadata : GraphMetadata,
                renderer : IRenderer, drawer : D)

+ draw() : void {                              ← Template Method (final 권장)
    List<String> lines = new ArrayList<>();
    lines.addAll(drawer.drawTitle(metadata));
    lines.addAll(drawBody());                   // hook — 하위 클래스 구현
    renderer.print(lines);
  }

+ onDataChanged() : void { draw(); }           ← 공통. 패스스루

# abstract drawBody() : List<String>            ← 하위 클래스가 구현하는 hook
```

- `draw()`의 골격(제목 → 본문 → 출력)을 상위에서 고정하고, 본문 생성(`drawBody()`)만 하위에서 결정
- `onDataChanged() → draw()` 패스스루가 모든 그래프에 공통이므로 상위에서 한 번만 구현
- D의 타입 바운드는 `ITitle`만 요구 — `draw()`에서 `drawer.drawTitle()`만 직접 호출하고, `drawPlot()`/`drawAxis()` 호출은 `drawBody()` 내부에서 하위 클래스가 담당하므로 상위에서 `IPlot`/`IAxisPlot` 바운드가 불필요

---

### 그래프 계층 — AbstractAxisGraph (축 있는 그래프 공통)

산포도, 막대그래프, 꺾은선그래프 등 **축이 있는 그래프**의 공통 로직을 담는다. `drawBody()`와 `IViewControllable` 구현을 상위에서 처리하여, 하위 클래스는 `appendData()`와 그래프 고유 로직만 구현한다.

```
abstract class AbstractAxisGraph<T, D extends ITitle & IAxis<T> & IAxisPlot<T>>
    extends AbstractGraph<T, D>
    implements IViewControllable

# viewController : ViewController              ← 생성자 주입

+ AbstractAxisGraph(data : T, metadata : GraphMetadata,
                    renderer : IRenderer, drawer : D,
                    viewController : ViewController)

# drawBody() : List<String> {                  ← AbstractGraph의 hook 구현
    int[] axes = viewController.getAxes();
    List<String> body = new ArrayList<>();
    body.addAll(drawer.drawAxis(data, metadata, axes));
    body.addAll(drawer.drawPlot(data, axes));
    return body;
  }

+ swapAxes(a : int, b : int) : void {          ← IViewControllable
    viewController.swapAxes(a, b);
    draw();
  }

+ setView(axes : int[]) : void {               ← IViewControllable
    viewController.setView(axes);
    draw();
  }

+ getAvailableViews() : List<int[]> {           ← IViewControllable
    return viewController.getAvailableViews();   // ← ViewController가 dim을 이미 보유
  }
```

- D의 타입 바운드에 `IAxis<T>`와 `IAxisPlot<T>`가 추가됨 → Drawer가 축 그리기와 축 매핑 기반 데이터 그리기를 구현함이 컴파일 타임에 보장
- `viewController`를 상위에서 보유하므로 ScatterPlot/BarGraph가 각각 중복 보유할 필요 없음
- `swapAxes()` / `setView()` / `getAvailableViews()` 구현이 완전히 동일하므로 상위에서 한 번만 구현
- `getAvailableViews()`는 ViewController가 생성 시 dim을 받아 저장하므로 파라미터 없이 호출 가능

---

### 그래프 계층 — 구체 클래스

AbstractAxisGraph가 draw()/drawBody()/onDataChanged()/swapAxes()/setView()/getAvailableViews()를 모두 처리하므로, 하위 클래스는 `appendData()`와 `suspendObservers()/resumeObservers()` 위임만 구현한다.

```
ScatterPlot extends AbstractAxisGraph<ScatterPlotData, IScatterDrawer>
    implements IBatchAppendable<Point>

+ ScatterPlot(data, metadata, renderer, drawer, viewController)
    → super(data, metadata, renderer, drawer, viewController)

+ appendData(point : Point) : void { data.append(point); }
+ suspendObservers() : void { data.suspendObservers(); }
+ resumeObservers() : void { data.resumeObservers(); }
```

```
BarGraph extends AbstractAxisGraph<BarGraphData, IBarDrawer>
    implements IBatchAppendable<Pair<String, Double>>

+ BarGraph(data, metadata, renderer, drawer, viewController)
    → super(data, metadata, renderer, drawer, viewController)

+ appendData(bar : Pair<String, Double>) : void { data.append(bar); }
+ suspendObservers() : void { data.suspendObservers(); }
+ resumeObservers() : void { data.resumeObservers(); }
+ getAvailableViews() : List<int[]> { return Collections.emptyList(); }
    ← AbstractAxisGraph 기본 구현(ViewController 위임)을 오버라이드.
       axisMapping을 무시하고 direction으로만 렌더링하므로 뷰 목록 제공이 무의미.
       빈 목록 반환 → GraphSession에서 "전환할 뷰가 없습니다" 안내.
       swapAxes()/setView()는 상속 그대로이나 BarGraphDrawer가 axisMapping을 무시하므로 출력 무영향
```

```
PieChart extends AbstractGraph<PieChartData, IPieDrawer>           (미래)
    implements IBatchAppendable<PieChartEntry>

+ PieChart(data, metadata, renderer, drawer)
    → super(data, metadata, renderer, drawer)

# drawBody() : List<String> {
    return drawer.drawPlot(data);     ← 축 없음. drawAxis() 호출 없음
  }

+ appendData(entry : PieChartEntry) : void { data.append(entry); }
+ suspendObservers() : void { data.suspendObservers(); }
+ resumeObservers() : void { data.resumeObservers(); }
```

**계층 구조 효과:**
- 새 "축 있는 그래프"(LineGraph 등) 추가 시 `AbstractAxisGraph` 상속 + `appendData()` 구현만으로 완성
- 새 "축 없는 그래프" 추가 시 `AbstractGraph` 상속 + `drawBody()` + `appendData()` 구현
- draw()/onDataChanged()/swapAxes()/setView() 중복이 완전 제거됨

**suspend/resume 공통화를 하지 않은 이유:**

`suspend/resume`의 위임 패턴(`data.suspendObservers()` / `data.resumeObservers()`)이 `ScatterPlot`, `BarGraph`, `PieChart` 세 클래스에서 동일하게 반복된다. `AbstractGraph`에서 공통화하려면 `T`가 `suspendObservers()`를 갖는다는 타입 바운드(예: `T extends ISuspendable`)가 필요하다. 그러나 현재 `Data` 클래스들(`ScatterPlotData`, `BarGraphData`, `PieChartData`)은 공통 상위 타입이 없으므로 바운드를 걸 수 없다. 공통 인터페이스(예: `ISuspendable`)를 도입하면 해결되지만, 3줄 위임 중복이 인터페이스 추가보다 단순하므로 현행 유지한다.

**appendData() 경유 흐름:**
```
[GraphSession]
graph.appendData(point)              ← IDataAppendable<Point> 경유. 컴파일 타임 타입 보장
 ↓ [ScatterPlot 내부]
data.append(point)                   ← 캐스팅 없음
 └── observerSupport.notifyObservers()
      └── onDataChanged()            ← AbstractGraph에서 구현
           └── draw()                ← AbstractGraph의 Template Method
```

**swapAxes() / setView() 흐름:**
```
swapAxes(a, b)                       ← AbstractAxisGraph에서 구현. 프로그래밍 API (GUI용)
 ├── viewController.swapAxes(a, b)
 └── draw()   ← AbstractGraph의 Template Method

setView(axes)                        ← AbstractAxisGraph에서 구현. 콘솔 view 명령어가 호출
 ├── viewController.setView(axes)
 └── draw()
```

콘솔에서는 setView()만 사용된다. swapAxes()는 setView()의 특수 케이스(축 순서 뒤집기)이므로 별도 명령어 없이 view 명령어에서 해당 축 조합을 선택하면 동일 효과.

---

### 출력 계층 — 브리지 패턴

```
IRenderer
+ print(lines : List<String>)
      ↑
TextRenderer       GUIRenderer (미래)
```

---

### 객체 생성 — GraphFactory

A안 전환으로 `instanceof` 분기 제거. 타입별 전용 팩토리 메서드로 분리.
반환 타입은 구체 클래스(`ScatterPlot`, `BarGraph`)로 선언하여 `IGraph<T>`와 `IDataAppendable<E>`를 모두 시그니처에서 보장한다.

```
GraphFactory                                        ← static 유틸리티 클래스. 인스턴스 생성 불필요
+ static createScatter(data : ScatterPlotData, metadata : GraphMetadata, renderer : IRenderer)
    : ScatterPlot
+ static createBar(data : BarGraphData, metadata : GraphMetadata, renderer : IRenderer)
    : BarGraph
```

```java
// GraphFactory.createScatter()
public static ScatterPlot createScatter(
        ScatterPlotData data, GraphMetadata metadata, IRenderer renderer) {
    ScaleCalculator scaleCalc = new ScaleCalculator();
    IScatterDrawer drawer = new ScatterPlotDrawer(scaleCalc);  // gridHeight/gridWidth 기본값 사용
    int dim = data.getPoints().get(0).size();   // readData() 최소 1건 보장
    ViewController vc = new ViewController(dim);
    return new ScatterPlot(data, metadata, renderer, drawer, vc);
}

// GraphFactory.createBar()
public static BarGraph createBar(
        BarGraphData data, GraphMetadata metadata, IRenderer renderer) {
    ScaleCalculator scaleCalc = new ScaleCalculator();
    IBarDrawer drawer = new BarGraphDrawer(scaleCalc);         // gridHeight/gridWidth 기본값 사용
    ViewController vc = new ViewController(2);  // 카테고리/값 2축 고정
    return new BarGraph(data, metadata, renderer, drawer, vc);
}
```

**반환 타입이 구체 클래스인 이유:**

`ScatterPlot`은 `IGraph<ScatterPlotData>`와 `IDataAppendable<Point>`를 모두 구현한다. 반환 타입이 `IGraph<ScatterPlotData>`였다면 `IDataAppendable<Point>`라는 정보가 시그니처에서 사라지고, 호출자(`GraphDirector`)가 캐스팅에 의존해야 한다. 구체 클래스를 반환하면 두 인터페이스 모두 컴파일 타임에 보장된다. `GraphDirector.constructGraph()` 안에서 `BuildResult`에 `graph`와 `appendable`을 동일 인스턴스로 넣는 패턴이 타입 안전하게 성립한다.

**기존 B안 대비 변경:**

기존: `create(Object data, ...)` → `instanceof` 분기 → 런타임 캐스팅.
변경: 타입별 전용 메서드 → 컴파일 타임에 타입 보장. `instanceof` 완전 제거.

`GraphDirector.constructGraph()` 내부에서 타입 판단이 이미 완료되어 있으므로, 해당하는 팩토리 메서드를 직접 호출한다. 팩토리가 타입 분기를 할 필요가 없어짐.

**OCP 관점:** 그래프 종류 추가 시 `createPie()` 등 새 메서드를 추가한다. 기존 메서드 수정 없음. `GraphDirector`의 분기와 함께 추가되므로 변경 지점이 명확하다.

---

### GraphDirector — 빌더 패턴의 Director 역할과 유사

```
GraphDirector
- typeSelector : ITypeSelector                          ← 생성자 주입 (DIP)
+ GraphDirector(typeSelector : ITypeSelector)
+ construct(source : IInputSource, renderer : IRenderer) : ISession
+ constructRealtimeSession(source : IInputSource, renderer : IRenderer,
                           factory : RealtimeSourceFactory) : ISession
+ constructGraphOnly(source : IInputSource, renderer : IRenderer) : BuildResult<?, ?>   ← GUI 동기용
- constructGraph(source : IInputSource, renderer : IRenderer) : BuildResult<?, ?>   ← private
- <T, E> assembleRealtimeSession(result : BuildResult<T, E>,
                                  factory : RealtimeSourceFactory) : ISession        ← private. 와일드카드 캡처
```

- GoF 빌더 패턴의 완전한 구조는 아님. 조립 순서 제어라는 Director 역할만 차용
- `ITypeSelector`를 생성자 주입으로 받음 → 구체 클래스(`ConsoleTypeSelector`) 몰라도 됨 (DIP)
- `construct()`: 대화형 세션 조립 — 내부에서 `constructGraph()`를 호출한 뒤 `GraphSession`으로 감싸서 반환
- `constructRealtimeSession()`: 실시간 세션 조립 — 내부에서 `constructGraph()` 호출 후 `assembleRealtimeSession()`으로 위임. `RealtimeSourceFactory`를 받아 E가 결정된 후 소스와 큐를 생성
- `constructGraphOnly()`: GUI 동기 모드용 — 세션으로 감싸지 않고 `BuildResult<?, ?>`를 그대로 반환. 내부적으로 `constructGraph()`를 호출하는 단순 위임. **와일드카드가 외부에 노출되므로 GUI 진입점에서 unchecked cast가 1회 발생한다.** 대안으로 콜백 패턴(`<T, E> void constructAndAccept(source, renderer, BiConsumer<IGraph<T>, IDataAppendable<E>>)`)이 있으나 현재 규모에서는 과도하다
- `constructGraph()`: **private** — 두 public 메서드의 공통 조립 로직
- `assembleRealtimeSession()`: **private 제네릭 헬퍼** — `BuildResult<T, E>`의 와일드카드를 `<T, E>`로 캡처하여 타입 안전하게 `DataQueue<E>`, `IRealtimeSource<E>`, `RealtimeGraphSession<T, E>`를 생성
- `Main`은 모드에 따라 `construct()` 또는 `constructRealtimeSession()`을 호출

**construct()와 constructRealtimeSession()의 관계:**

두 메서드 모두 내부에서 private `constructGraph()`를 호출하여 그래프를 조립한 뒤, 각각 다른 세션으로 감싸서 반환한다. 조립 로직 중복이 없고, `GraphDirector`는 상태(필드)를 갖지 않는다.

```java
// GraphDirector.construct() — 대화형 모드
ISession construct(IInputSource source, IRenderer renderer) {
    BuildResult<?, ?> result = constructGraph(source, renderer);
    return new GraphSession<>(source, result.graph(), result.appendable(), result.handler());
}

// GraphDirector.constructRealtimeSession() — 실시간 모드
ISession constructRealtimeSession(IInputSource source, IRenderer renderer,
                                  RealtimeSourceFactory factory) {
    BuildResult<?, ?> result = constructGraph(source, renderer);
    return assembleRealtimeSession(result, factory);
}

// 와일드카드 캡처 헬퍼 — BuildResult<?, ?>의 T, E를 바인딩
private <T, E> ISession assembleRealtimeSession(BuildResult<T, E> result,
                                                 RealtimeSourceFactory factory) {
    IDataParser<E> parser = result.handler().createParser();
    DataQueue<E> queue = new DataQueue<>(1024);  // capacity는 설정으로 조절 가능
    IRealtimeSource<E> stream = factory.create(parser);
    stream.stream(queue);
    return new RealtimeGraphSession<>(queue, result.graph(), result.appendable(),
                                      () -> stream.stop());
}
```

**와일드카드 타입 갭 해소:**

기존 설계에서는 `DataQueue<?>`가 Main에서 생성되어 `constructRealtimeSession()`에 전달되었다. 이 경우 `DataQueue`의 E와 `BuildResult`의 E가 같다는 보장이 컴파일 타임에 없었고, `RealtimeGraphSession<T, E>` 생성자에 `DataQueue<?>`를 넘기면 타입 불일치가 발생했다.

변경 후에는 `assembleRealtimeSession()`이 `BuildResult<T, E>`를 제네릭으로 받아 T와 E를 캡처한다. 이 메서드 안에서 `DataQueue<E>`, `IDataParser<E>`, `IRealtimeSource<E>`가 모두 동일한 E로 생성되므로 타입 안전성이 완전히 보장된다. `RealtimeSourceFactory.create()`가 제네릭 메서드 `<E> create(IDataParser<E>)`이므로, 팩토리 자체는 E를 몰라도 호출 시점에 E가 결정된다.

**실시간 초기 데이터 수집 — 의도 명시:**

`constructRealtimeSession()`은 `IInputSource`를 받아 `constructGraph()` 내부에서 그래프 타입 선택, 초기 데이터 수집, 메타데이터 입력을 수행한다. 실시간 모드라도 "어떤 그래프를 그릴지, 축 설정은 뭔지"를 먼저 정해야 스트림 데이터를 의미 있게 시각화할 수 있다. 센서 모니터링 프로그램에서 "먼저 그래프 설정 → 스트림 연결" 순서가 자연스러운 것과 같은 맥락이다.

**BuildResult — 조립 결과 전달 객체 (내부용):**

```
BuildResult<T, E>
- graph      : IGraph<T>
- appendable : IBatchAppendable<E>              ← IBatchAppendable는 IDataAppendable를 확장
- handler    : IGraphDataInputHandler<T, E>
```

`constructGraph()`가 graph, appendable, handler 세 가지를 함께 반환한다. `construct()`는 이 결과에서 graph/appendable/handler를 꺼내 `GraphSession`을 생성하고(GraphSession은 `IDataAppendable`만 필요하므로 업캐스팅), `assembleRealtimeSession()`은 handler에서 파서를 추출한 뒤 graph/appendable로 `RealtimeGraphSession`을 생성한다(IBatchAppendable로 전달). `BuildResult`는 외부에 노출되지 않는 내부 전달 객체이다. 단, `constructGraphOnly()`에서 외부에 노출될 때는 `BuildResult<?, ?>`로 와일드카드가 발생한다.

**constructGraph() 완전한 구현 명세 (private):**

```java
// GraphDirector.constructGraph() — private
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
        // ScatterPlot이 IGraph<ScatterPlotData>와 IDataAppendable<Point>를 모두 구현

    } else if ("bar".equals(type)) {
        IGraphDataInputHandler<BarGraphData, Pair<String, Double>> handler
            = new BarConsoleInputHandler();
        BarGraphData data = handler.readData(source);
        GraphMetadata metadata = handler.readMetadata(source);
        BarGraph graph = GraphFactory.createBar(data, metadata, renderer);
        data.addObserver(graph);
        return new BuildResult<>(graph, graph, handler);
    }

    throw new IllegalArgumentException("Unknown type: " + type);
}
```

**handler 선택 분기 — OCP 트레이드오프:**

`constructGraph()` 내부의 `if/else if` 분기는 `GraphFactory`의 기존 `instanceof` 분기와 동일한 구조적 트레이드오프다. 그래프 종류 추가 시 분기를 추가해야 한다.

현재 4종 이하에서는 분기가 명확하고 안전하므로 감수한다. 5종 이상으로 확장 시 `Map<String, Supplier<IGraphDataInputHandler<?>>>` 등록 방식으로 전환을 고려한다. 이 시점은 `GraphFactory`의 전환 시점과 동일하다.

---

### ISession — 세션 통일 인터페이스

모든 세션(`GraphSession<T, E>`, `RealtimeGraphSession<T, E>`)이 구현하는 공통 계약.
`Main`이 모드에 관계없이 `session.start()` 한 줄로 세션을 시작할 수 있도록 통일.

```
<<interface>>
ISession
+ start() : void   ← 세션 시작
+ stop()  : void   ← 세션 종료. 스트림 중단, 스레드 정리
+ await() : void   ← 세션 완료 대기. 내부 스레드가 종료될 때까지 블로킹
```

**start()의 계약:**
- `start()`는 항상 **논블로킹** — 내부적으로 스레드를 생성하고 즉시 반환
- `Main`은 `session.start()` 후 `session.await()`를 호출하여 세션 종료를 대기

**await()를 도입한 이유:**

`start()` 후 main 스레드가 할 일이 없으면 프로세스 종료 여부가 언어에 따라 다르다.
Java에서는 비데몬 스레드가 살아있으면 프로세스가 유지되지만, C++에서는 main 함수가 반환하면 프로세스가 종료된다 (`std::thread`를 detach한 경우). `await()`를 명시적으로 호출하도록 하면 언어에 관계없이 동작이 보장된다.

```
// Main 코드
session.start();
session.await();  // ← 세션 스레드가 종료될 때까지 대기
```

**콘솔 대화형 `GraphSession.start()` 내부:**
```java
// start()는 새 스레드에서 run()을 실행 → main 스레드 미점유
new Thread(this::run).start();
```

**`GraphSession.await()` 내부:**
```java
// 내부 스레드가 종료될 때까지 대기
this.workerThread.join();
```

**왜 통일이 필요한가:**
- 콘솔: `run()` 블로킹 → main 스레드 점유 허용
- GUI: `run()` 블로킹 → GUI 이벤트 루프 차단 (화면 동결)
- `start()`로 통일하면 `Main`은 모드를 몰라도 됨. 내부 구현이 차이를 흡수
- `await()`로 종료 대기를 명시하면 C++ 전환 시에도 안전

---

### GraphSession — 세션 전체 관리

```
GraphSession<T, E> implements ISession
- source       : IInputSource
- graph        : IGraph<T>
- appendable   : IDataAppendable<E>
- handler      : IGraphDataInputHandler<T, E>
- workerThread : Thread
+ start()  : void   ← 새 스레드에서 run() 실행
+ stop()   : void   ← source.close() 후 루프 종료
+ await()  : void   ← workerThread.join()
+ run()    : void   ← 블로킹 루프 (내부용)
```

- `GraphDirector.construct()`가 생성하여 반환. `Main`은 `session.start()` 한 줄만 호출
- 최초 출력, 입력 루프, 자원 정리까지 세션 전체 흐름 담당
- `stop()`은 `source.close()`를 호출하여 입력 스트림을 닫고 루프를 종료한다. 명령어 루프가 `source.readObject()`에서 블로킹 중일 때 `close()`가 블로킹을 해제하는 방식은 `IInputSource` 구현체의 책임이다 — `stop()` 시 블로킹 해제를 위해 `ConsoleInput`은 내부적으로 입력 스레드를 분리할 수 있다: 별도 스레드에서 `System.in`을 읽어 내부 `BlockingQueue`에 넣고, `readObject()`는 이 큐에서 꺼내는 구조이다. `close()` 시 큐를 닫으면 `readObject()` 블로킹이 해제된다. Java의 `System.in`은 `Thread.interrupt()`로 중단되지 않으므로 이 우회가 필요하다. 구체적 구현은 열어둔다. `GUIInput`은 내부 `BlockingQueue` 닫기로 대응한다

**run() 내부:**
```
run()
 ├── try
 │     ├── graph.draw()                      ← 최초 출력. 이 시점에 data는 readData()로 채워진 상태
 │     ├── 명령어 루프
 │     │     ├── source.readObject() → 명령어 읽기 (한 줄)
 │     │     ├── "add"
 │     │     │     ├── handler.parseData(source) → appendable.appendData(point)
 │     │     │     └── parseData() 실패 시 에러 메시지 출력, 무시, 루프 계속
 │     │     ├── "view"
 │     │     │     ├── graph를 IViewControllable로 캐스팅 시도
 │     │     │     │     ← 캐스팅 실패 시 (PieChart 등) "이 그래프는 뷰 전환을 지원하지 않습니다" 안내 → 명령어 루프로 복귀
 │     │     │     ├── views = vc.getAvailableViews()
 │     │     │     │     ← views가 빈 목록이면 (BarGraph) "전환할 뷰가 없습니다" 안내 → 명령어 루프로 복귀
 │     │     │     ├── 뷰 목록 번호와 함께 출력
 │     │     │     │     예: "1. x-y  2. x-z  3. y-z"
 │     │     │     ├── source.readObject() → 번호 입력 대기
 │     │     │     ├── 유효한 번호 → setView(views[번호]) → draw()
 │     │     │     └── 무효한 번호 → "유효한 번호를 입력하세요" 안내, 명령어 루프로 복귀
 │     │     ├── "exit" → 루프 종료
 │     │     └── 기타   → "add, view, exit 중 입력하세요" 안내, 무시
 │     catch(Exception)
 │           └── 에러 메시지 출력 (stderr)
 └── finally
       └── source.close()                   ← 자원 정리 보장
```

**명령어 기반 전환 근거:**

기존 설계에서 `source.readObject()`로 한 줄을 읽어 "Enter/Q"를 판단한 뒤, `parseData(source)`가 다시 한 줄을 읽는 구조였다. 이러면 첫 번째 readObject()에서 읽은 줄이 판단용으로만 사용되고 버려지는 모호성이 있었다.

명령어 기반으로 전환하면 readObject()는 **명령어**를 읽고, parseData()는 **데이터**를 읽는다. 역할이 명확히 분리된다.

**뷰 전환 — 방식 B (뷰 네비게이션):**

`view` 명령어 입력 시 사용 가능한 뷰 목록을 번호와 함께 출력하고, 사용자가 번호를 선택하면 해당 뷰로 전환한다. 축 전치(x-y → y-x)도 별도 명령어 없이 뷰 목록에 포함하여 setView()로 처리한다.

```
// 사용자 인터랙션 예시 (dim=3 산포도)
> view
사용 가능한 뷰:
  1. x - y
  2. y - x
  3. x - z
  4. z - x
  5. y - z
  6. z - y
번호 입력: 3
→ setView({0, 2}) → draw()   ← x-z 평면으로 출력

> view
사용 가능한 뷰:
  1. x - y
  2. y - x
  3. x - z  [현재]
  4. z - x
  5. y - z
  6. z - y
번호 입력: 4
→ setView({2, 0}) → draw()   ← z-x 평면으로 출력 (전치)
```

dim=2일 때는 `{0,1}`과 `{1,0}` 두 개 뷰가 있으므로 가로축/세로축 전치 전환이 가능하다.

**swapAxes()와 view 명령어의 관계:**

`swapAxes(a, b)`는 IViewControllable의 프로그래밍 API로 제공된다. 콘솔 대화형에서는 별도의 "swap" 명령어를 두지 않는다 — `setView()`로 임의의 축 조합을 지정할 수 있으므로, 축 전치({0,1} → {1,0})도 view 명령어에서 해당 조합을 선택하면 동일한 효과를 달성한다. GUI에서는 버튼이나 드래그로 swapAxes()를 직접 호출할 수 있다.

**`parseData()` 책임:**

`IGraphDataInputHandler<T, E>.parseData(source)`는 현재 그래프 타입에 맞는 형식으로 데이터 1건을 입력받아 `E` 타입으로 반환한다.
- `ScatterConsoleInputHandler.parseData()` → `(v1, v2, ..., vdim)` 형태 입력 → `Point` 반환
- `BarConsoleInputHandler.parseData()` → `(카테고리, 값)` 형태 입력 → `Pair<String, Double>` 반환

`GraphSession<T, E>`은 제네릭 `E`를 통해 컴파일 타임에 원소 타입이 보장된다.

---

### 데이터 구조 — Point

```
Point
- coords : List<Double>
+ get(index : int) : double
+ size() : int
+ of(values : double...) : Point
```

**dim은 동적으로 결정.** `ScatterConsoleInputHandler.readData()`에서 사용자가 입력한 dim 값으로 `Point.of(v1, ..., vdim)`을 생성한다. dim은 `Point.size()`로 언제든 추출 가능하므로 별도 필드로 보관 불필요. 현재 요구사항은 2D이므로 dim=2를 입력하면 되고, n차원도 동일한 경로로 처리된다.

---

### 스케일 계층

```
Range
- min : double
- max : double

ScaleCalculator
+ calcRange(values : List<Double>) : Range          ← Data 타입 의존 제거
+ calcInterval(range : Range) : double
+ getTickLabels(range : Range) : List<String>
+ toCol(x : double, xMin : double, xMax : double, gridWidth : int)  : int
+ toRow(y : double, yMin : double, yMax : double, gridHeight : int) : int
```

- `Drawer`가 필드로 보유. 조립자는 직접 알 필요 없음
- `calcRange(values)`: 이미 추출된 값 목록을 받아 min/max 계산. **Data 타입에 의존하지 않음** — 값 추출은 Drawer가 담당
- `calcInterval(range)`: `calcRange()` 결과를 받아 nice interval을 계산하는 전용 메서드. 아래 알고리즘의 2단계를 담당
- 좌표 변환 공식: `col = (x - xMin) / (xMax - xMin) * (gridWidth - 1)`
- 상태 캐싱 없음. `drawAxis()`와 `drawPlot()`이 각각 `calcRange()`를 독립 호출하므로 매 `draw()`마다 range 계산이 2회 수행된다. 현재 콘솔 텍스트 렌더링에서는 성능 문제가 되지 않으나, 데이터가 대량이면 Drawer 내부에서 range를 캐싱하거나 `drawBody()`에서 range를 선계산하여 공유하는 최적화를 고려한다
- 그래프 종류 추가 시 `ScaleCalculator` 수정 불필요 (OCP) — Drawer가 자기 Data에서 값을 추출하여 `List<Double>`로 넘기면 됨

**기존 대비 변경 — `calcRange()` 시그니처 변경 근거:**

기존: `calcRange(data : ScatterPlotData, axisIndex)` / `calcRange(data : BarGraphData, axisIndex)` — 그래프 종류마다 오버로딩 추가 필요 (OCP 위반).
변경: `calcRange(values : List<Double>)` — 순수 수학 연산만 담당. 값 추출은 각 Drawer 구현체가 담당.

실제 호출:
```java
// ScatterPlotDrawer 내부
List<Double> xValues = data.getPoints().stream()
    .map(p -> p.get(axisMapping[0])).collect(toList());
Range xRange = scaleCalc.calcRange(xValues);

// BarGraphDrawer 내부
List<Double> values = data.getBars().stream()
    .map(Pair::getSecond).collect(toList());
Range valueRange = scaleCalc.calcRange(values);
```

**`calcRange()` 알고리즘 — nice interval 방식**

교재 샘플 출력 기준: 데이터 x 범위 0~36인데 축이 -10~35로 표시됨.
→ "데이터 min/max를 눈금 단위로 내림/올림"하는 방식.

```
// 1. 데이터에서 raw min/max 추출
dataMin = min(values)
dataMax = max(values)
rawRange = dataMax - dataMin

// 2. 눈금 간격 결정 — calcInterval(range) 내부 로직
//    range = Range(dataMin, dataMax)
targetTickCount = 5  // 눈금 개수 목표 (조정 가능)
roughInterval   = rawRange / targetTickCount
magnitude       = 10 ^ floor(log10(roughInterval))
// nice step: 1, 2, 5, 10 중 roughInterval에 가장 가까운 값 선택
interval = niceCeil(roughInterval, magnitude)   ← calcInterval() 반환값

// 3. 축 범위 결정 — calcRange() 최종 반환값
axisMin = floor(dataMin / interval) * interval
axisMax = ceil(dataMax  / interval) * interval
```

예시: 데이터 범위 0~36, targetTickCount=5
→ roughInterval=7.2, magnitude=1, nice step → interval=10
→ axisMin=floor(0/10)*10=0, axisMax=ceil(36/10)*10=40
(교재 출력이 -10 시작인 것은 margin 1칸 추가 여부로, 구현자 재량)

---

### 버킷 방식 — drawPlot() 내부 (ScatterPlotDrawer)

**[정책] 중복 좌표 처리 — 덮어쓰기**

같은 (x, y) 값이 여러 번 입력되어 동일한 grid 셀에 매핑되는 경우,
마지막으로 처리된 포인트의 `*`가 그대로 유지됨 (덮어쓰기).
별도 기호나 카운트 표시 없음.
근거: 교재 샘플 출력에서 `(35,55)` 중복 데이터가 `*` 하나로만 표시됨.

```java
// ScatterPlotDrawer.drawPlot()
List<Double> xValues = data.getPoints().stream()
    .map(p -> p.get(axisMapping[0])).collect(toList());
List<Double> yValues = data.getPoints().stream()
    .map(p -> p.get(axisMapping[1])).collect(toList());

Range xRange = scaleCalc.calcRange(xValues);
Range yRange = scaleCalc.calcRange(yValues);

char[][] grid = new char[gridHeight][gridWidth];
for (char[] row : grid) Arrays.fill(row, ' ');

for (Point p : data.getPoints()) {
    int col = scaleCalc.toCol(p.get(axisMapping[0]), xRange.min, xRange.max, gridWidth);
    int row = scaleCalc.toRow(p.get(axisMapping[1]), yRange.min, yRange.max, gridHeight);
    grid[row][col] = '*';
}

List<String> lines = new ArrayList<>();
for (char[] row : grid) lines.add(new String(row));
return lines;
```

---

### 버킷 방식 — drawPlot() 내부 (BarGraphDrawer)

**막대 그리기 방식:**

BarGraphDrawer는 `BarGraphData`의 `direction`에 따라 수직 또는 수평 막대를 grid에 배치한다. 기본 전략은 ScatterPlotDrawer와 동일한 char grid 방식이되, 데이터 포인트 하나가 아니라 **연속된 셀을 채우는 막대**를 그린다.

```java
// BarGraphDrawer.drawPlot()
List<Pair<String, Double>> bars = data.getBars();
List<Double> values = new ArrayList<>(bars.stream().map(Pair::getSecond).collect(toList()));

// [기준선 보장] 막대그래프는 0을 기준선으로 사용하여 막대를 그린다.
// 모든 값이 양수(예: 10, 20, 30)일 때 calcRange()가 axisMin=10을 반환하면
// 0이 축 범위 밖이 되어 toRow(0, 10, 30, gridHeight)가 음수 인덱스를 반환,
// grid[음수][col] 접근으로 ArrayIndexOutOfBoundsException이 발생한다.
// 값 목록에 0.0을 추가하면 calcRange()가 0을 포함하는 범위를 계산하므로
// 기준선이 항상 grid 안에 위치하게 된다.
// ScatterPlot은 데이터 포인트만 찍으므로 이 문제가 없다.
values.add(0.0);

Range valueRange = scaleCalc.calcRange(values);

char[][] grid = new char[gridHeight][gridWidth];
for (char[] row : grid) Arrays.fill(row, ' ');

if (data.getDirection() == Direction.VERTICAL) {
    // 수직 막대: 각 카테고리가 x축 위치, 값이 y축 높이
    int barWidth = gridWidth / bars.size();        // 균등 분할
    for (int i = 0; i < bars.size(); i++) {
        double val = bars.get(i).getSecond();
        int col = i * barWidth + barWidth / 2;     // 막대 중심 열
        int topRow = scaleCalc.toRow(val, valueRange.min, valueRange.max, gridHeight);
        int baseRow = scaleCalc.toRow(0, valueRange.min, valueRange.max, gridHeight);
        // 기준선(0)부터 값까지 채우기
        for (int r = Math.min(topRow, baseRow); r <= Math.max(topRow, baseRow); r++) {
            grid[r][col] = '*';
        }
    }
} else {
    // 수평 막대: grid 전치 — 카테고리가 y축, 값이 x축 길이
    int barHeight = gridHeight / bars.size();
    for (int i = 0; i < bars.size(); i++) {
        double val = bars.get(i).getSecond();
        int row = i * barHeight + barHeight / 2;
        int endCol = scaleCalc.toCol(val, valueRange.min, valueRange.max, gridWidth);
        int baseCol = scaleCalc.toCol(0, valueRange.min, valueRange.max, gridWidth);
        for (int c = Math.min(baseCol, endCol); c <= Math.max(baseCol, endCol); c++) {
            grid[row][c] = '*';
        }
    }
}

List<String> lines = new ArrayList<>();
for (char[] row : grid) lines.add(new String(row));
return lines;
```

**수평 막대의 grid 전치 방식:**

수평 막대는 "수직 막대를 90도 회전"한 것이 아니다. grid 자체는 동일한 `char[gridHeight][gridWidth]` 구조이며, 카테고리 축과 값 축의 방향이 바뀔 뿐이다. 수직은 카테고리→x축/값→y축, 수평은 카테고리→y축/값→x축. `axisMapping`을 바꾸는 것이 아니라 `drawPlot()` 내부에서 direction에 따라 채우기 방향을 분기한다.

**BarGraphDrawer의 axisMapping 무시:** `drawPlot(data, axisMapping)`과 `drawAxis(data, metadata, axisMapping)` 모두 IAxisPlot/IAxis 계약상 axisMapping을 파라미터로 받지만, BarGraphDrawer는 이를 사용하지 않고 `data.getDirection()`으로 렌더링 방향을 결정한다. `drawAxis()`에서 축 레이블은 direction에 따라 결정된다 — VERTICAL이면 x축=카테고리, y축=값, HORIZONTAL이면 x축=값, y축=카테고리. dim=2 고정이므로 axisMapping이 `{0,1}`이든 `{1,0}`이든 출력이 동일하다. `setView()`로 전치를 선택해도 BarGraph의 출력은 변하지 않는다 — 카테고리/값 축의 배치는 direction이 결정하며, 이는 생성 시 고정된다. #1의 getAvailableViews() 빈 목록 반환으로 콘솔 view 명령어를 통한 전치 선택이 불가하므로, 실제로 이 분기가 사용자에 의해 트리거될 경로가 없다.

---

### 실시간 확장 — DataQueue

실시간 스트림에서 데이터 수신 스레드와 소비 스레드를 분리하기 위한 버퍼.

```
DataQueue<E>
- queue    : BlockingQueue<E>
- capacity : int                      ← 생성자에서 주입
+ DataQueue(capacity : int)           ← bounded queue 생성
+ enqueue(data : E) : void            ← 데이터 스레드(IRealtimeSource)가 호출
+ dequeue() : E                       ← 소비 스레드가 호출. 블로킹. null 반환 시 종료
+ tryDequeue() : E                    ← 소비 스레드가 호출. 논블로킹. 큐 비어있으면 즉시 null
+ close() : void                      ← poison pill 삽입. 소비 스레드 종료 신호
```

- `BlockingQueue` 기반 — `dequeue()`는 데이터가 없으면 스레드를 재워서 대기. 데이터가 들어오는 순간 OS가 스레드를 깨움. CPU를 낭비하며 루프를 돌리는 방식(busy-wait)과 달리 CPU 사용 없이 효율적으로 대기
- `tryDequeue()`는 큐가 비어있으면 대기하지 않고 즉시 null을 반환하는 **논블로킹** 메서드. 배치 소비에 사용 — `dequeue()`로 첫 건을 기다린 뒤, `tryDequeue()`로 추가로 쌓인 데이터를 모두 꺼내고 draw()를 한 번만 호출
- `close()`가 호출되면 queue에 종료 신호(poison pill)를 삽입 → `dequeue()`가 null 반환 → 소비 스레드 루프 종료. **멱등성 보장** — 이미 닫힌 큐에 close()를 재호출해도 안전
- **제네릭 `<E>`** — 원소 타입(Point, Pair 등)이 컴파일 타임에 보장됨
- **bounded queue** — 생성 시 `capacity`를 지정. 용량 초과 시 정책(drop-oldest, drop-newest, 블로킹 등)은 실시간 데이터의 성격에 따라 구현체가 결정한다. 실시간 시각화에서는 최신 데이터를 우선하는 drop-oldest가 일반적이지만, 데이터 유실이 허용되지 않는 경우 블로킹이 적합할 수 있다

---

### 실시간 확장 — IDataParser

실시간 스트림에서 raw 문자열을 원소 타입으로 변환하는 함수형 인터페이스.

```
<<interface>>
IDataParser<E>
+ parse(raw : String) : E             ← raw 문자열 → 원소 타입 변환
```

- `IGraphDataInputHandler<T, E>.createParser()`가 반환
- `ScatterConsoleInputHandler.createParser()` → `"(v1,v2,...,vdim)"` → `Point` 변환 파서 반환
- `BarConsoleInputHandler.createParser()` → `"(카테고리,값)"` → `Pair<String, Double>` 변환 파서 반환
- `parseData(IInputSource)`는 대화형 모드에서 콘솔 입력을 읽어 파싱하는 메서드이고, `createParser()`는 실시간 모드에서 raw 문자열을 변환하는 순수 파서를 생성하는 메서드이다. 입력 읽기와 파싱 로직을 분리하여 두 모드에서 파싱 로직을 재사용한다

**createParser()와 parseData()의 관계:**

`parseData(source)`는 내부적으로 `source.readObject()`로 한 줄을 읽은 뒤 파싱한다. `createParser()`가 반환하는 파서는 이 파싱 로직만 추출한 것이다. 구현체에서 중복이 발생하지 않도록, `parseData()`가 내부적으로 `createParser().parse()`를 호출하는 구조로 작성할 수 있다.

```java
// ScatterConsoleInputHandler 예시
public IDataParser<Point> createParser() {
    return raw -> {
        // "(v1, v2, ..., vdim)" 파싱 → Point.of(v1, v2, ..., vdim)
        // dim은 readData()에서 설정된 필드 참조
    };
}

public Point parseData(IInputSource source) {
    String raw = (String) source.readObject("데이터를 입력하세요 (예: 1.0, 2.0)");
    return createParser().parse(raw);
}
```

> `readData()` 내부에서도 동일하게 프롬프트를 인자로 전달한다 — 예: `source.readObject("몇 차원 데이터입니까?")`, `source.readObject("(v1, v2, ...) 형태로 입력 (빈 줄로 종료)")`. handler가 UI를 직접 알지 않고도 안내를 전달하는 구조가 완성된다.

---

### 실시간 확장 — IRealtimeSource

실시간 스트림 소스의 계약. `IInputSource`(pull, 블로킹)와 독립된 push 방식 인터페이스.

```
<<interface>>
IRealtimeSource<E>                              ← 제네릭. 원소 타입 E
+ stream(queue : DataQueue<E>) : void   ← 스트림 시작. 데이터를 queue에 push
+ stop() : void                         ← 스트림 종료 신호
```

- `IInputSource`와 **별개 계약** — pull(IInputSource)과 push(IRealtimeSource)는 제어 흐름이 반대. 동일 인터페이스로 묶으면 LSP 위반
- **제네릭 `<E>`** — `DataQueue<E>.enqueue(E)`를 호출하려면 구체 타입이 필요하므로 `IRealtimeSource` 자체가 E를 알아야 한다. 비제네릭 `IRealtimeSource`에 `DataQueue<?>`를 넘기면 Java에서 `enqueue()`에 null 외의 값을 넣을 수 없어 컴파일 에러가 발생한다
- `stream(queue)`에서 받은 `DataQueue`를 구현체 필드로 저장. `stop()` 호출 시 저장된 queue로 `queue.close()` 호출
- 즉 `stop()`의 `queue.close()` 호출 책임은 **IRealtimeSource 구현체**에 있음. `RealtimeGraphSession`은 `source.stop()`만 호출하면 됨
- **`stop()`은 멱등이어야 한다** — 이미 종료된 스트림에서 `stop()`을 재호출해도 안전해야 한다. 경로 A(스트림 자체 종료) 후 onStop 콜백에서 `stop()`이 다시 호출될 수 있으므로, 내부에 `if (already stopped) return;` 가드가 필요하다. `DataQueue.close()` 멱등과 함께 종료 경로의 안전성을 보장한다
- `stream()`은 별도 스레드에서 실행되거나, 내부적으로 스레드를 생성해 실행
- 구현체(`SocketRealtimeSource<E>`)는 생성 시 `IDataParser<E>`를 주입받아, 소켓에서 읽은 raw 문자열을 `parser.parse(raw)`로 E 타입으로 변환한 뒤 `queue.enqueue(E)`를 호출한다

```
SocketRealtimeSource<E> implements IRealtimeSource<E>
- address : String
- parser  : IDataParser<E>                   ← 생성자 주입
- queue   : DataQueue<E>                     ← stream()에서 저장
- stopped : boolean                          ← 멱등성 가드
+ SocketRealtimeSource(address, parser)
+ stream(queue : DataQueue<E>) : void        ← 내부 스레드에서 소켓 읽기 → parser.parse() → queue.enqueue()
+ stop() : void                              ← if (stopped) return; stopped=true; queue.close() + 소켓 닫기
```

**stream() 내부 루프 — 파싱 에러 처리:**

```java
// SocketRealtimeSource.stream() 내부
new Thread(() -> {
    try {
        while (connected) {
            String raw = readFromSocket();
            try {
                E data = parser.parse(raw);
                queue.enqueue(data);
            } catch (Exception e) {
                System.err.println("파싱 실패, 무시: " + raw);  // 해당 건 무시, 루프 계속
            }
        }
    } catch (Exception e) {
        System.err.println("스트림 에러: " + e.getMessage());  // 연결 끊김 등
    } finally {
        queue.close();  // 정상 종료든 에러든 poison pill 전달
    }
}).start();
```

- `parser.parse()` 실패 시 해당 데이터만 무시하고 루프를 계속한다
- 소켓 레벨 예외(연결 끊김 등)는 외부 try-catch에서 잡아 루프를 종료하고 `queue.close()`로 소비 스레드에 종료를 알린다

---

### 실시간 확장 — RealtimeSourceFactory

Main이 스트림 소스의 연결 정보(주소 등)를 설정하되, 원소 타입 E는 모르는 상태에서 팩토리를 주입하는 구조.

```
<<interface>>
RealtimeSourceFactory
+ <E> create(parser : IDataParser<E>) : IRealtimeSource<E>

SocketRealtimeSourceFactory implements RealtimeSourceFactory
- address : String
+ SocketRealtimeSourceFactory(address : String)
+ <E> create(parser : IDataParser<E>) : IRealtimeSource<E>
    → return new SocketRealtimeSource<>(address, parser)
```

- Main은 `new SocketRealtimeSourceFactory(address)`로 팩토리를 생성하여 `constructRealtimeSession()`에 전달
- `constructRealtimeSession()` 내부에서 그래프 타입이 결정된 후, handler에서 `createParser()`로 파서를 얻고, `factory.create(parser)`로 타입이 맞는 `IRealtimeSource<E>`를 생성
- Main은 E를 몰라도 되고, `DataQueue<E>` 생성도 `constructRealtimeSession()` 내부에서 이루어지므로 와일드카드 타입 갭이 발생하지 않음

---

### 실시간 확장 — RealtimeGraphSession

콘솔 실시간용 세션. `GraphSession` 코드를 수정하지 않고 새 클래스로 추가 (OCP). 실시간 모드에서 `Main`은 `GraphSession` 대신 `RealtimeGraphSession`을 사용한다.

```
RealtimeGraphSession<T, E> implements ISession
- graph           : IGraph<T>
- appendable      : IBatchAppendable<E>      ← 배치 처리용. IDataAppendable 확장
- queue           : DataQueue<E>              ← 외부에서 주입. 원소 타입 E
- onStop          : Runnable                  ← 종료 시 호출할 콜백
- workerThread    : Thread                    ← dequeue 루프 실행
+ RealtimeGraphSession(queue : DataQueue<E>, graph : IGraph<T>,
                       appendable : IBatchAppendable<E>, onStop : Runnable)
+ start() : void   ← workerThread에서 run() 실행. 논블로킹, 즉시 반환
+ stop()  : void   ← onStop.run() 호출 후 workerThread.join()
+ await() : void   ← workerThread.join() 대기
+ run()   : void   ← dequeue 루프 (내부용. start()가 스레드로 실행)
```

- `IRealtimeSource`를 생성자에서 받지 않음 — queue를 채우는 주체가 `IRealtimeSource`든 GUI 이벤트 핸들러든 세션이 알 필요 없음 (C안)
- `onStop` 콜백으로 종료 책임을 주입 — 세션은 "종료 시 뭔가를 호출해야 한다"만 알고, 대상이 스트림인지 GUI인지 모름. C안의 핵심 원칙 보존
- `renderer`는 필드로 보유하지 않음 — `GraphFactory.createXxx()` 시점에 이미 `graph` 내부에 주입되어 있음
- 생성 주체는 `GraphDirector.assembleRealtimeSession()` — queue, graph, appendable, onStop을 타입 안전하게 조립하여 주입

**start()/run() 관계 — GraphSession과의 대칭:**

두 세션 모두 `start()`가 `run()`을 새 스레드(workerThread)로 실행하고, `await()`가 그 스레드를 join한다. ISession 인터페이스의 일관성을 유지하면서, `run()` 내부만 모드에 따라 달라진다.

```
GraphSession:         start() → workerThread에서 run() → 명령어 루프 (동기 입력)
RealtimeGraphSession: start() → workerThread에서 run() → dequeue 루프 (실시간 소비)
```

`RealtimeGraphSession`에는 "rendererThread"라는 별도 스레드가 존재하지 않는다. workerThread 자체가 dequeue → appendData → draw를 수행하는 소비 스레드이다. 데이터 수신은 `IRealtimeSource` 구현체 내부의 receiverThread가 담당하며, 이 스레드는 세션이 아닌 `stream.stream(queue)` 호출 시점에 생성된다.

**종료 단순화 — 스트림 종료 = 세션 종료:**

기존 설계의 "수동 종료 대기 모드"(스트림 종료 후 Q 입력 대기)를 제거한다. 스트림이 종료되면 세션도 즉시 종료된다. 콘솔에서는 출력이 화면에 남아 있으므로 별도 대기가 불필요하다.

**그래프 타입 선택 경로:**

실시간 모드에서도 그래프 타입은 런타임에서 `ITypeSelector`로 받는다.
private `constructGraph()`가 타입 선택 → 데이터/메타 수집 → `GraphFactory` 조립까지 처리하고 `BuildResult`를 반환. 이 로직은 `construct()`와 `constructRealtimeSession()` 모두에서 공유된다.

```
// GraphDirector.constructGraph() 흐름 (private)
constructGraph(source, renderer)
 ├── typeSelector.selectType(source)        → type 판단
 ├── type에 따라 IGraphDataInputHandler<T, E> 선택
 ├── handler.readData(source)               → Data 생성
 ├── handler.readMetadata(source)           → GraphMetadata 생성
 ├── GraphFactory.createXxx(data, metadata, renderer) → IGraph<T> (+ IDataAppendable<E>)
 ├── data.addObserver(graph)
 └── return BuildResult(graph, appendable, handler)   ← 내부 전달 객체
```

**사용자 인터랙션 제약:**

콘솔 실시간 모드에서는 사용자 명령어(add/view/exit)를 지원하지 않는다. `RealtimeGraphSession`에는 명령어 루프가 없고, 데이터는 스트림에서만 유입된다. 종료는 스트림 종료(경로 A) 또는 외부 `stop()`(경로 B)으로만 이루어진다. 사용자 인터랙션이 필요한 경우 GUI 모드에서 이벤트 핸들러로 대응한다.

**run() 내부 — 배치 소비 + suspend/resume:**
```
run()
 ├── graph.draw()                              ← 최초 출력 (초기 데이터만 반영. 주석 참조)
 └── dequeue 루프
       ├── data = queue.dequeue()              ← 블로킹 대기. 데이터 올 때까지 잠듦
       ├── data == null → 루프 종료            ← poison pill 수신
       ├── try
       │     ├── appendable.suspendObservers()   ← 옵저버 알림 일시 중단
       │     ├── appendable.appendData(data)   ← 첫 건 추가 (알림 무시됨)
       │     ├── while ((next = queue.tryDequeue()) != null)
       │     │     └── appendable.appendData(next)  ← 추가 건 배치 소비 (알림 무시됨)
       │     └── appendable.resumeObservers()  ← 알림 재개 → notifyObservers() → draw() 1회
       │     catch(Exception)
       │           ├── 로그 출력 (stderr)
       │           └── onStop.run()            ← 스트림 정리
       └── (루프 반복)
```

> **최초 draw() 시점:** `graph.draw()`는 `readData()`로 수집한 초기 데이터만 반영한다. 이 시점에 스트림 데이터는 `assembleRealtimeSession()`에서 `stream.stream(queue)`로 시작된 receiverThread에 의해 queue에 버퍼링되어 있을 수 있으며, 이후 dequeue 루프에서 순차 반영된다.

> **배치 소비 근거:** 데이터가 고빈도로 유입될 때 매 `append()`마다 `draw()`가 호출되면 성능 문제가 발생한다. `dequeue()`로 첫 건을 기다린 뒤, `tryDequeue()`로 추가로 쌓인 데이터를 모두 꺼내고, `resume()`으로 draw()를 배치당 1회만 호출한다. `suspend()/resume()`은 ObserverSupport에서 제어하므로 append() 메서드 자체는 변경하지 않는다.

> **observerSupport 접근:** `RealtimeGraphSession`이 `observerSupport`를 직접 참조하는 것이 아니라, Data 클래스가 `suspend()/resume()`을 위임 메서드로 노출한다. 즉 `data.suspendObservers()` / `data.resumeObservers()`를 호출하는 구조이다. 이를 위해 `IDataAppendable<E>` 대신 Data 참조가 필요한데, `BuildResult`에서 data를 추가로 전달하거나 `IDataAppendable`에 `suspendObservers()/resumeObservers()`를 추가하는 방안이 있다. 현재는 후자(IDataAppendable 확장)를 채택한다 — 배치 처리가 appendData와 동일한 호출자에서 필요하므로 같은 인터페이스에 위치하는 것이 자연스럽다.

**stop() 흐름:**
```
session.stop()
 ├── onStop.run()              ← 종료 콜백 (예: stream.stop() (멱등) → queue.close())
 │     → workerThread poison pill 수신 → 루프 종료
 └── workerThread.join()       ← 스레드 완전 종료 대기
```

외부에서 `session.stop()`을 호출하면 `onStop` 콜백이 실행되어 queue가 닫히고, workerThread가 poison pill을 받아 종료된다. 스트림이 자체적으로 끝난 경우(연결 끊김 등)에는 `IRealtimeSource` 구현체가 내부에서 `queue.close()`를 호출하므로, `onStop`에서 `stream.stop()`과 `queue.close()`를 중복 호출해도 안전하도록 **둘 다 멱등성을 보장**한다.

**스레드 생명주기:**

```
[assembleRealtimeSession() 내부에서]
1. stream.stream(queue) 호출 → receiverThread 시작
   → 외부 스트림에서 데이터를 받아 parser.parse() → queue.enqueue() 반복
   → 파싱 실패 시 해당 건 무시, 로그 출력 (stream() 내부 try-catch)

[session.start() 호출 후]
2. workerThread에서 run() 실행
3. graph.draw() → 최초 출력 (초기 데이터만)
4. dequeue 루프
   → queue.dequeue() 블로킹 대기
   → 데이터 수신 시 appendable.suspendObservers() → 배치 소비(tryDequeue) → appendable.resumeObservers() → draw() 1회

[종료 — 경로 A: 스트림 자체 종료]
5. IRealtimeSource 구현체가 내부에서 queue.close() 호출
6. workerThread poison pill 수신 → 루프 종료
7. onStop.run() 호출 (stream.stop() 멱등 + queue.close() 멱등)
8. session.await() 해제

[종료 — 경로 B: 외부에서 session.stop() 호출]
5. onStop.run() → stream.stop() (멱등) → queue.close()
6. workerThread poison pill 수신 → 루프 종료
7. workerThread.join() → 세션 종료
```

**종료 시퀀스:**
```
1. onStop.run()            → stream.stop() (멱등) + queue.close() (멱등, poison pill)
2. workerThread.join()     → queue 잔여 소진 후 종료 대기 (poison pill로 루프 탈출)
3. 세션 종료
```

**옵저버 경로와 스레드 정책:**

`appendable.appendData()` → `data.append()` → `notifyObservers()` → `draw()`는 workerThread에서 실행된다. 배치 처리 시 `suspend()/resume()`에 의해 draw()는 배치당 1회만 호출된다.
`receiverThread`는 queue에 넣기만 하고 `appendData()`를 직접 호출하지 않는다.
따라서 두 스레드가 `draw()`를 동시 호출하는 상황은 발생하지 않는다.

**기존 설계와의 관계:**
```
GraphSession<T, E>            ← 콘솔 대화형. 현행 유지
RealtimeGraphSession<T, E>    ← 콘솔 실시간. 신규 추가
(GUI 동기)                    ← 세션 불필요. 이벤트 핸들러가 appendable.appendData() 직접 호출
(GUI 비동기)                  ← RealtimeGraphSession.start() 재사용. DataQueue 동일 경로
```

- `appendable.appendData()`, 옵저버, `draw()`, `GraphFactory`, `Drawer`, `ViewController` — 전부 그대로
- 변경 범위가 세션 계층에만 집중 (OCP)

**생성 주체 — GraphDirector가 통일 조립:**

콘솔 대화형은 `GraphDirector.construct()`가, 실시간은 `GraphDirector.constructRealtimeSession()`이 세션을 조립하여 반환한다. Main은 어느 모드에서든 Director에게 조립을 위임하고 `ISession`만 받는다.

```java
// Main — 실시간 분기 (콘솔)
IInputSource initSource   = new ConsoleInput();       // 초기 설정용
IRenderer renderer        = new TextRenderer();
ITypeSelector typeSelector = new ConsoleTypeSelector();
GraphDirector director    = new GraphDirector(typeSelector);

RealtimeSourceFactory factory = new SocketRealtimeSourceFactory(address);
session = director.constructRealtimeSession(initSource, renderer, factory);
session.start();
session.await();
```

> ※ `constructRealtimeSession()` 내부에서 `constructGraph()`로 그래프를 조립한 뒤, handler에서 파서를 추출하고, factory로 `IRealtimeSource<E>`와 `DataQueue<E>`를 생성하여 `RealtimeGraphSession`을 조립한다. Main은 E를 몰라도 된다.

---

### 스타일 계층 — GraphStyle (GUI 도입 시 추가 예정)

명세 §4에서 요구하는 GUI 변경 항목: **배경색**, **그래프 색상**, **데이터 포인트의 그래픽 표현**.

**현재(콘솔 모드):** 스타일 정보 없음. `*` 기호는 Drawer 내부에 하드코딩.
콘솔에서 배경색/그래프 색상을 입력받거나 처리하는 것은 의미가 없으므로,
`GraphStyle` 계층은 **GUI 도입 시점에 추가**한다.

**GUI 도입 시 추가할 구조:**

```
GraphStyle  (공통 기반)                    ← GUI 도입 시 추가
- backgroundColor : Color    ← 구현 시 타입 결정 (RGB 구조체 or String 등)
- plotColor       : Color    ← 구현 시 타입 결정
+ getters / setters
+ GraphStyle()               ← 기본 생성자: colors=null

      ↑
ScatterPlotStyle
- plotSymbol : String        ← 기본값 '*' (콘솔 하드코딩에서 이동)
+ ScatterPlotStyle()

BarGraphStyle
- barSymbol  : String        ← 기본값 '*'
+ BarGraphStyle()

PieChartStyle  (미래)
- sectorColors : List<Color>
+ PieChartStyle()
```

`ScatterPlotData` / `BarGraphData`, `ScatterConsoleInputHandler` / `BarConsoleInputHandler`,
`ScatterPlot` / `BarGraph`가 그래프 타입마다 별도 클래스로 분기되듯, Style도 동일 패턴을 따른다.

GUI 도입 시: `GraphFactory`가 타입에 맞는 Style을 생성해서 `Drawer`에 주입.
`Drawer`는 `style.getPlotSymbol()` 등으로 기호/색상을 참조.

---

### GUI 전환 경로

**CLI와 GUI는 동일 코드베이스, Main이 진입점 분기**

런타임 분기가 아니라 빌드/진입점 수준에서 결정.
`Main`이 인자에 따라 어떤 Session 조합을 띄울지 선택한다.
`ISession.start()`로 통일되어 `Main`은 모드를 몰라도 됨.

---

**main 스레드 점유 문제**

GUI 프레임워크(Swing, JavaFX 등)는 main 스레드에서 자신의 이벤트 루프를 실행한다.
이 이벤트 루프가 버튼 클릭, 창 리사이즈, 화면 갱신 등을 처리한다.

```
// 콘솔 — main 스레드가 세션 루프를 점유해도 문제 없음
main 스레드 → session.run() → while(true) { readObject()... }

// GUI — main 스레드가 GUI 이벤트 루프를 돌려야 함
main 스레드 → GUI 이벤트 루프 → 버튼 클릭 감지 → 핸들러 호출
              ↑ 여기서 session.run()을 호출하면 이벤트 루프가 막혀 화면이 동결됨
```

해결: `ISession.start()`가 내부에서 별도 스레드를 생성해 실행 → main 스레드 반환.

---

**GUI 초기 데이터 수집 — 블로킹 문제와 해결**

`constructGraph()` 내부에서 `handler.readData(source)`가 `IInputSource.readObject()`를 반복 호출하는 블로킹 루프이다. GUI 이벤트 스레드에서 이걸 직접 호출하면 화면이 동결된다. 이 문제는 GUI 동기와 GUI 실시간 모두에 해당한다 — 둘 다 `constructGraph()`를 거치기 때문이다.

해결: `GUIInput`이 `IInputSource`를 구현하되, `readObject()` 내부에서 대화 상자를 이벤트 스레드에 띄우고 사용자 입력을 `BlockingQueue`로 받아 반환한다. `readObject()`는 블로킹이지만 **workerThread에서 호출**되므로 이벤트 스레드는 자유롭다.

```
[workerThread]                          [이벤트 스레드]
constructGraph(guiInput, renderer)
  → handler.readData(guiInput)
    → guiInput.readObject()             → 대화 상자 표시 (invokeLater)
      ← BlockingQueue.take() 대기         사용자 입력
                                         → BlockingQueue.put(입력값)
      ← 입력값 반환
```

GUI 동기에서는 `constructGraphOnly()`를 workerThread에서 호출하고, GUI 실시간에서는 `constructRealtimeSession()` 자체를 workerThread에서 호출하는 것으로 해결한다. `GUIInput`의 구체적 구현은 열어두되, "readObject() 내부에서 이벤트 스레드 위임으로 블로킹 문제를 해결한다"는 방향을 설계에서 확정한다.

---

**GUIRenderer — 텍스트 기반 렌더링 전제**

현재 설계의 Drawer 계층(ScatterPlotDrawer, BarGraphDrawer)은 `char[][] grid`를 만들어 `List<String>`으로 반환한다. `IRenderer.print(List<String>)`은 이 텍스트 줄 목록을 출력하는 계약이다.

GUIRenderer는 이 `List<String>`을 받아 **모노스페이스 폰트 캔버스에 텍스트로 그린다.** 벡터 좌표 기반 그래픽 렌더링이 필요해지면 `IRenderer` 시그니처가 아니라 Drawer 계층부터 재설계해야 한다 — Drawer가 `List<String>` 대신 좌표 데이터나 그리기 명령을 반환해야 하므로 `IAxis`, `IAxisPlot`, `IPlot` 인터페이스 전체가 변경된다. 현재 범위에서는 텍스트 기반 렌더링으로 한정하며, 벡터 그래픽은 Drawer 계층 재설계와 함께 미래 과제로 둔다.

```java
// GUIRenderer.print() — 텍스트 기반
public void print(List<String> lines) {
    SwingUtilities.invokeLater(() -> {
        canvas.setLines(lines);   // 모노스페이스 캔버스에 텍스트 배치
        canvas.repaint();
    });
}
```

`IRenderer.print()` 계약: **구현체는 스레드 안전을 보장해야 한다.**
`TextRenderer`는 stdout에 쓰므로 스레드 무관. `GUIRenderer`는 이벤트 스레드 위임으로 보장.

명세 §4의 GUI 요구사항(배경색, 그래프 색상, 데이터 포인트 그래픽 표현)은 `GraphStyle` 계층에서 기호/색상을 주입받아 `List<String>` 안에서 처리하거나, `GUIRenderer`가 스타일 정보를 참조하여 캔버스 렌더링 시 반영한다.

---

**시나리오 1 — GUI 동기 (버튼 클릭 → 즉시 갱신)**

가장 단순한 케이스. `RealtimeGraphSession` 불필요.

```
[초기화 — workerThread에서 실행]
BuildResult<?, ?> result = director.constructGraphOnly(guiInput, guiRenderer);
IGraph<?> graph = result.graph();
IDataAppendable appendable = result.appendable();                 ← unchecked cast
IViewControllable vc = (IViewControllable) result.graph();        ← 축 있는 그래프만
graph.draw();                                                     ← 최초 출력

[이후 — 이벤트 스레드에서 실행]
"추가" 버튼 클릭
  → appendable.appendData(point)   ← IDataAppendable<E> 경유
      └── data.append()
           └── observerSupport.notifyObservers()
                └── draw()
                     └── GUIRenderer.print()   ← invokeLater(repaint)

"뷰 전환" 드롭다운 변경
  → vc.setView(axes)   ← IViewControllable 경유
      └── draw()

"창 닫기"
  → 자원 정리. 세션이 없으므로 별도 종료 절차 불필요
```

- `DataQueue`, `RealtimeGraphSession` 없이 기존 `IGraph` + 옵저버 패턴만으로 동작
- draw()가 짧을 때 적합. draw()가 이벤트 루프의 응답성을 해칠 정도로 오래 걸리면(데이터가 많아 drawBody()의 grid 생성이 느려지는 경우 등) GUI 비동기(시나리오 2)로 전환한다. 구체적 임계치는 데이터 크기·하드웨어·프레임워크에 따라 다르므로 구현/프로파일링 단계에서 결정한다
- 이벤트 핸들러가 `IDataAppendable`과 `IViewControllable` 참조를 `constructGraphOnly()` 반환 직후에 획득
- **타입 안전성 제한:** `BuildResult<?, ?>`에서 appendable을 꺼낼 때 unchecked cast가 발생한다. **컴파일 타임 타입 보장이 아닌 프로그래머 규약에 의존한다** — 산포도 그래프에 `Pair<String, Double>`을 넣는 실수를 컴파일러가 잡을 수 없으므로, GUI 진입점 코드 작성 시 그래프 타입과 원소 타입의 일치를 프로그래머가 보장해야 한다. 대안으로 콜백 패턴(`<T, E> void constructAndAccept(source, renderer, BiConsumer<IGraph<T>, IDataAppendable<E>>)`)이 있으나 현재 규모에서는 과도하다

---

**시나리오 2 — GUI 비동기 (외부 스트림 또는 무거운 draw)**

draw()가 오래 걸리거나 외부 실시간 스트림을 받을 때. `RealtimeGraphSession.start()` 재사용.

```
[초기화 — workerThread에서 실행]
// 외부 스트림이 있는 경우
session = director.constructRealtimeSession(guiInput, guiRenderer, factory);
session.start();

// 외부 스트림 없이 버튼 클릭만 쓰는 경우
BuildResult<?, ?> result = director.constructGraphOnly(guiInput, guiRenderer);
DataQueue queue = new DataQueue(capacity);            ← unchecked cast
session = new RealtimeGraphSession(queue, result.graph(), result.appendable(),
                                    () -> queue.close());
session.start();

[이후 — 이벤트 스레드에서 실행]
"추가" 버튼 클릭 or 스트림 수신
  → queue.enqueue(point)

                              [workerThread]
                              queue.dequeue()
                               → suspendObservers() → 배치 소비 → resumeObservers()
                                    └── draw()
                                         └── GUIRenderer.print()
                                              └── invokeLater(repaint)

"창 닫기"
  → session.stop()   ← onStop 실행 → 스트림/큐 정리 → workerThread 종료
```

- `DataQueue` + `RealtimeGraphSession.start()` 재사용
- `GUIRenderer.print()`가 내부적으로 이벤트 스레드로 그리기 위임 (스레드 안전 계약)
- 버튼 클릭과 외부 스트림 모두 같은 queue에 넣을 수 있음
- **타입 안전성 제한 (버튼 클릭만 경로):** 외부 스트림이 있는 경우 `constructRealtimeSession()` 내부의 `assembleRealtimeSession()`이 타입을 캡처하므로 안전하다. 그러나 버튼 클릭만 쓰는 경우 `constructGraphOnly()`를 거치므로 시나리오 1과 동일한 타입 갭이 발생한다 — DataQueue와 appendable의 E가 컴파일 타임에 일치하지 않으며, 프로그래머 규약에 의존한다

---

**onStop 콜백 — 시나리오별 구체 내용:**

onStop은 "세션 종료 시 정리해야 할 것"을 주입하는 콜백이다. 스트림 자체 종료든 외부 stop 호출이든 관계없이 실행된다.

| 시나리오 | onStop 내용 | 종료 트리거 |
|---------|------------|-----------|
| 콘솔 실시간 | `() -> stream.stop()` | 스트림 끊김(경로 A) 또는 외부 stop(경로 B) |
| GUI + 외부 스트림 | `() -> stream.stop()` | 스트림 끊김 또는 창 닫기 → `session.stop()` |
| GUI + 버튼 클릭만 | `() -> queue.close()` | 창 닫기 → `session.stop()` |
| GUI 동기 | 세션 없음. 해당 없음 | 창 닫기 → 자원 정리 |

---

**재사용 가능한 계층 — GUI 도입 시에도 변경 없음**

| 계층 | 클래스 | 근거 |
|------|--------|------|
| 그래프 추상 | `AbstractGraph`, `AbstractAxisGraph` | draw()/drawBody()/onDataChanged()/swapAxes()/setView() 공통화. UI 무관 |
| 그래프 구체 | `ScatterPlot`, `BarGraph` | `IDataAppendable<E>` 구현만 담당. UI 무관 |
| 데이터 | `ScatterPlotData`, `BarGraphData` | 순수 데이터, UI 무관 |
| 그리기 | `ScatterPlotDrawer`, `BarGraphDrawer` | 그리기 로직 UI 무관 |
| 입력 핸들러 | `IGraphDataInputHandler<T, E>` 구현체 | 초기 데이터 수집 단계 재사용 |
| 출력 인터페이스 | `IRenderer` | 브리지 패턴. `GUIRenderer` 추가만으로 대응 |
| 입력 인터페이스 | `IInputSource` | 브리지 패턴. `GUIInput` 추가 가능 |
| DataQueue | `DataQueue<E>` | GUI 비동기에서 그대로 재사용 |
| 실시간 세션 | `RealtimeGraphSession<T, E>` | GUI 비동기에서 `start()` 방식으로 재사용 |

---

**GUI 도입 시 추가/교체 계층**

| 항목 | 시나리오 | 내용 |
|------|---------|------|
| `GUIRenderer` | 동기 + 비동기 | `IRenderer` 구현체. 텍스트 기반. `print()` 내부에서 이벤트 스레드 위임(invokeLater) 보장 |
| `GUIInput` | 모든 GUI 모드 | `IInputSource` 구현체. `readObject()` 내부에서 대화 상자 + BlockingQueue로 블로킹 해결 |
| `GUITypeSelector` | 모든 GUI 모드 | `ITypeSelector` 구현체. 콤보박스/라디오 버튼으로 그래프 타입 선택 |
| 이벤트 핸들러 | 동기 | `appendable.appendData()`, `vc.setView()` 직접 호출 |
| `DataQueue` 연결 | 비동기 | 이벤트 핸들러 또는 스트림이 `queue.enqueue()` 호출 |
| `GraphStyle` 계층 | 동기 + 비동기 | `ScatterPlotStyle` / `BarGraphStyle`. GUI 도입 시 추가 |

---

**IInputSource 블로킹 문제의 실체**

`IInputSource` 자체가 문제가 아니라, GUI에서 `readObject()`를 호출하는 스레드가 문제이다.
콘솔에서는 main 스레드(또는 workerThread)가 블로킹되어도 괜찮지만, GUI에서는 이벤트 스레드가 블로킹되면 화면이 동결된다.
`GUIInput.readObject()`가 내부적으로 이벤트 스레드 위임을 하면, 호출 스레드(workerThread)만 블로킹되고 이벤트 스레드는 자유롭다.

```
// 콘솔 대화형 — GraphSession.start() → workerThread에서 run() → 명령어 루프
// 콘솔 실시간 — RealtimeGraphSession.start() → workerThread에서 run() → dequeue 루프
// GUI 동기    — constructGraphOnly()를 workerThread에서 호출 → 이후 이벤트 핸들러가 직접 호출
// GUI 비동기  — constructRealtimeSession()을 workerThread에서 호출 → RealtimeGraphSession.start() 재사용
```

---

**GraphStyle — GUI 도입 시점에 추가**

현재 콘솔 모드에서는 Style 주입 없음. `*` 기호 Drawer 내부 하드코딩 유지.
GUI 도입 시 `GraphStyle` 계층을 추가하고 `GraphFactory`에서 Drawer에 주입하는 구조로 확장.
(스타일 계층 섹션 참조)

---

### Main — 흐름 제어

```
Main
+ main(args) : void
```

**외부 옵션 구조:**

| 옵션 | 값 | 의미 |
|------|----|------|
| `--mode` | `interactive` (기본) / `realtime` | 실행 모드 |
| `--source` | `host:port` 등 | 실시간 데이터 경로. `--mode=realtime` 시 필수 |
| `--ui` | `console` (기본) / `gui` | 출력 방식 |

그래프 타입(`scatter` / `bar`)은 옵션이 아닌 런타임에서 `ITypeSelector`로 받음.
"실행 환경 설정"과 "무엇을 그릴지"를 분리.

**분기 구조:**

```java
// Main.main(args)
String mode   = args.getOrDefault("--mode",   "interactive");
String ui     = args.getOrDefault("--ui",     "console");
String source = args.getOrDefault("--source", null);

IRenderer renderer = ui.equals("gui") ? new GUIRenderer() : new TextRenderer();
ISession session;

if (mode.equals("realtime")) {
    if (source == null) throw new IllegalArgumentException("--source 필수");

    // 그래프 타입은 런타임에서 받음
    IInputSource initSource    = new ConsoleInput();
    ITypeSelector typeSelector = new ConsoleTypeSelector();
    GraphDirector director     = new GraphDirector(typeSelector);

    // 실시간 세션 조립 — Director가 전담. Main은 E를 몰라도 됨
    RealtimeSourceFactory factory = new SocketRealtimeSourceFactory(source);
    session = director.constructRealtimeSession(initSource, renderer, factory);

} else {
    // 대화형 (기본)
    IInputSource source_       = new ConsoleInput();
    ITypeSelector typeSelector = new ConsoleTypeSelector();
    GraphDirector director     = new GraphDirector(typeSelector);
    session = director.construct(source_, renderer);
}

session.start();  // ← 모든 모드 공통. ISession 통일
session.await();  // ← 세션 완료 대기
```

- `Main`이 아는 것: `IInputSource`, `IRenderer`, `ITypeSelector`, `GraphDirector`, `ISession`, `RealtimeSourceFactory`
- `Main`이 모르는 것: `IGraph` 내부 구조, `data`, `handler`, `Drawer`, `ViewController`, `ScaleCalculator`, `BuildResult`, `DataQueue`, `IRealtimeSource`, `IDataParser`
- 모든 모드: `session.start()` + `session.await()` 두 줄로 통일 (`ISession` 인터페이스)

---

### 에러 처리 정책

**원칙**: 콘솔 대화형에서는 프로그램 종료 대신 **재입력 유도**. 에러 메시지는 `IInputSource`가 아닌 InputHandler 구현체가 출력 (안내 출력은 구현체 책임 원칙과 일관).

```
[입력 단계별 에러 처리]

1. 타입 선택 (ConsoleTypeSelector.selectType())
   - 미지원 타입 → "scatter 또는 bar를 입력하세요" 안내 후 재입력
   - 빈 입력 → 동일 안내 후 재입력

2. dim 입력 (ScatterConsoleInputHandler)
   - 정수가 아닌 값 → "정수를 입력하세요" 안내 후 재입력
   - 0 이하 → "1 이상의 값을 입력하세요" 안내 후 재입력

3. direction 입력 (BarConsoleInputHandler)
   - VERTICAL/HORIZONTAL 외 → "VERTICAL 또는 HORIZONTAL을 입력하세요" 안내 후 재입력

4. 데이터 입력 (readData 루프)
   - 파싱 실패 (숫자 아닌 값, 형식 불일치 등) → 에러 메시지 출력, 해당 줄 무시, 루프 계속
   - dim 불일치 (산포도) → "dim개의 값을 입력하세요" 안내, 무시

5. 명령어 입력 (GraphSession.run() 명령어 루프)
   - 미지원 명령어 → "add, view, exit 중 입력하세요" 안내, 무시
   - "add" 후 parseData 실패 → 에러 메시지 출력, 무시, 루프 계속

6. 뷰 전환 입력 ("view" 명령어 후)
   - 범위 초과 번호 → "유효한 번호를 입력하세요" 안내, 뷰 전환 취소 (명령어 루프로 복귀)
   - 숫자가 아닌 값 → 동일 안내, 뷰 전환 취소

[실시간 모드]
- 수신 데이터 파싱 실패 → `SocketRealtimeSource` 내부에서 `parser.parse(raw)` 예외 발생 시 해당 데이터 무시, 로그 출력 (stderr). 루프는 계속 (SocketRealtimeSource 섹션의 stream() 내부 루프 참조)
- 연결 끊김 → `SocketRealtimeSource` 외부 try-catch에서 잡아 `queue.close()` 호출 → 소비 스레드 정상 종료

[공통]
- 예상하지 못한 예외 → 두 계층 try-catch로 처리:
  1. Session.run() 내부: 세션 수준 예외를 잡아 자원 정리(source.close() 등) 보장 후 세션 종료
  2. Main: 세션 생성/조립 실패(constructGraph 예외 등)를 잡아 에러 메시지 출력 후 프로세스 종료
- 실시간 모드에서는 receiverThread와 workerThread 각각의 진입점에 try-catch 배치
  - 한 스레드의 예외가 다른 스레드에 전파되지 않으므로 각 스레드가 독립적으로 예외를 잡아야 함
  - receiverThread 예외 시: 로그 출력 후 queue.close()로 workerThread에 종료 신호 전달
  - workerThread 예외 시: 로그 출력 후 onStop.run()으로 스트림 정리
```

---

## 타입 안전성 — A안 전환 완료

v4.6.x까지 존재했던 4개의 타입 안전성 취약 지점이 A안(제네릭) 전환으로 모두 해소되었다.

| 기존 지점 | 위치 | 기존 문제 | A안 해결 |
|----------|------|----------|---------|
| 1 | `GraphSession.handler` | `IGraphDataInputHandler<?>` 와일드카드 | `GraphSession<T, E>` — handler 타입 보존 |
| 2 | `IGraph.appendData(Object)` | Object 캐스팅 | `IDataAppendable<E>.appendData(E)` — 원소 타입 제네릭으로 컴파일 타임 보장 |
| 3 | `IAxis.drawAxis(/* Data */)` | 구현체 내부 캐스팅 | `IAxis<T>.drawAxis(T data, ...)` — 인터페이스에 구체 타입 명시 |
| 4 | `GraphFactory.create(Object)` | instanceof 분기 | `createScatter()` / `createBar()` 전용 메서드 — instanceof 제거 |

**기존 `DataQueue.enqueue(Object)` 경로** 역시 `DataQueue<E>.enqueue(E)` 제네릭화로 해소.

**A안 전환에 따른 구조 변경 요약:**

```
[제네릭 전파 경로]
IGraphDataInputHandler<T, E>
 → readData() : T
 → parseData() : E
 → createParser() : IDataParser<E>            ← 실시간 모드 파싱용
 → GraphFactory.createXxx(T data, ...) : IGraph<T> (+ IDataAppendable<E>)
 → GraphSession<T, E>(source, graph, appendable, handler)
 → IDataAppendable<E>.appendData(E)

[실시간 제네릭 전파 경로]
assembleRealtimeSession(BuildResult<T, E>, factory)
 → handler.createParser() : IDataParser<E>
 → factory.create(parser) : IRealtimeSource<E>
 → new DataQueue<E>()
 → new RealtimeGraphSession<T, E>(queue, graph, appendable, onStop)

[Drawer 제네릭]
IAxis<T>, IAxisPlot<T>, IPlot<T>
 → IScatterDrawer extends ITitle, IAxis<ScatterPlotData>, IAxisPlot<ScatterPlotData>
 → IPieDrawer extends ITitle, IPlot<PieChartData>
 → 구현체 시그니처에 구체 타입 명시, 캐스팅 불필요
 → 축 매핑 유무에 따른 drawPlot 시그니처 불일치 해소 (ISP)

[DataQueue 제네릭]
DataQueue<E>.enqueue(E) / dequeue() : E
```

**GraphDirector 내부 분기는 유일한 "런타임 의존" 지점으로 남아 있다.** `typeSelector.selectType()` 결과에 따라 handler를 선택하는 `if/else if` 분기는 컴파일 타임에 제거할 수 없다. 그러나 이 분기 이후의 모든 흐름은 제네릭으로 타입이 보장되므로, 분기가 올바르기만 하면 이후 `ClassCastException`은 발생하지 않는다.

**A안 "캐스팅 제거"의 범위:** A안이 해소한 것은 **데이터 흐름의 타입 캐스팅** — `Object` → 구체 타입 변환 — 이다. `GraphSession`에서 `IViewControllable`로의 런타임 캐스팅은 ISP에 따른 기능 분기이며, A안의 대상이 아니다. 이 캐스팅은 설계 전체에서 유일한 잔존 런타임 캐스팅이다 (ViewController 섹션 참조).

---

## C++ 전환 고려사항

설계 구조 자체는 언어와 무관하게 동일하게 적용 가능하다.
아래는 C++ 구현 시 주의할 차이점을 정리한 것이다.

### 언어 매핑

| Java 개념 | C++ 대응 | 비고 |
|-----------|---------|------|
| 인터페이스 | 순수 가상 클래스 (`virtual f() = 0`) | 반드시 `virtual ~I() = default;` 소멸자 추가 |
| 제네릭 (`IGraph<T>`) | 템플릿 (`template<typename T>`) | 컴파일 타임 타입 보장. Java보다 타입 안전성 강함 |
| `BlockingQueue` | `std::queue` + `std::mutex` + `std::condition_variable` | 표준 라이브러리에 없음. 직접 구현 필요 |
| `Thread` | `std::thread` (C++11) | detach 시 main 종료로 프로세스 종료 — `await()`/join 필수 |
| GC | `shared_ptr` / `unique_ptr` | 소유권 명시 필요 |
| `instanceof` | `dynamic_cast` | A안 전환으로 사용 지점 없음 |
| 교차 인터페이스 | 다중 상속 | C++ 다중 상속으로 자연스럽게 구현 |

### 소유권 그래프

```
[unique_ptr — 단독 소유]
GraphSession<T, E>       owns → workerThread
AbstractAxisGraph        owns → ViewController          ← 상위 클래스에서 소유
AbstractGraph            owns → Drawer (D 타입 파라미터)
Drawer                   owns → ScaleCalculator

[shared_ptr — 공유 소유]
GraphSession<T, E>     shares → IGraph<T>         ← 세션과 옵저버 모두 참조
                       shares → IDataAppendable<E> ← 세션이 appendData() 호출
                       shares → IInputSource       ← 세션과 Main 모두 참조 가능
RealtimeGraphSession   shares → IBatchAppendable<E> ← 배치 처리용
IGraph(ScatterPlot 등) shares → IRenderer          ← Main이 생성, 여러 graph가 공유 가능

[weak_ptr 또는 raw pointer — 비소유 참조]
ObserverSupport        holds → List<IGraphObserver*>  ← 순환 참조 방지
```

**IBatchAppendable → IDataAppendable 업캐스팅 주의 (C++):**

Java에서는 `IBatchAppendable<E>`를 `IDataAppendable<E>`로 자동 업캐스팅하지만, C++에서 `std::shared_ptr<IBatchAppendable<E>>`를 `std::shared_ptr<IDataAppendable<E>>`로 변환하려면 `std::static_pointer_cast`가 필요하다. `BuildResult`가 `IBatchAppendable`을 보유하고 `construct()`가 `GraphSession`에 `IDataAppendable`로 넘길 때 이 변환이 발생한다.

**옵저버 순환 참조 문제:**

`ScatterPlotData`가 `ObserverSupport`를 통해 `ScatterPlot`(IGraphObserver)을 참조하고, `ScatterPlot`이 `ScatterPlotData`를 필드로 보유한다. Java에서는 GC가 처리하지만 C++에서 둘 다 `shared_ptr`이면 메모리 누수가 발생한다.

해결: `ObserverSupport`의 옵저버 목록을 `weak_ptr` 또는 raw pointer로 보유한다. 옵저버(Graph)의 생명주기는 항상 Data와 같거나 더 길므로 dangling pointer 위험이 없다 — 둘 다 `GraphFactory`에서 생성되고 `GraphSession`이 함께 보유하기 때문이다.

### DataQueue 직접 구현

```cpp
// C++ DataQueue<T> 구현 골격
template<typename T>
class DataQueue {
    std::queue<std::optional<T>> queue_;
    std::mutex mutex_;
    std::condition_variable cv_;
    bool closed_ = false;
    size_t capacity_;

public:
    explicit DataQueue(size_t capacity) : capacity_(capacity) {}

    void enqueue(T data) {
        {
            std::lock_guard lock(mutex_);
            // 용량 초과 정책은 구현체가 결정 (예: drop-oldest)
            if (queue_.size() >= capacity_) {
                queue_.pop();  // drop-oldest 예시
            }
            queue_.push(std::optional<T>(std::move(data)));
        }
        cv_.notify_one();
    }

    std::optional<T> dequeue() {   // nullopt = poison pill
        std::unique_lock lock(mutex_);
        cv_.wait(lock, [&] { return !queue_.empty() || closed_; });
        if (queue_.empty()) return std::nullopt;
        auto val = std::move(queue_.front());
        queue_.pop();
        return val;
    }

    std::optional<T> tryDequeue() {   // 논블로킹. 비어있으면 즉시 nullopt
        std::lock_guard lock(mutex_);
        if (queue_.empty()) return std::nullopt;
        auto val = std::move(queue_.front());
        queue_.pop();
        return val;
    }

    void close() {
        {
            std::lock_guard lock(mutex_);
            closed_ = true;
        }
        cv_.notify_all();
    }
};
```

### await() 필요성

Java에서는 비데몬 스레드가 살아있으면 JVM이 프로세스를 유지한다. C++에서는 `main()` 함수가 반환하면 프로세스가 종료된다 (`std::thread`를 detach한 경우). `ISession.await()`를 명시적으로 호출하도록 설계하여 언어에 관계없이 안전한 종료를 보장한다.

---

## 변경 축 정리

### 현재 구현 대상

| 변경 축 | 대응 방법 | 원칙 |
|--------|----------|------|
| **입력 계층** | | |
| 입력 방식 추가 | IInputSource 구현체만 추가 | OCP |
| 타입 선택 분리 | ITypeSelector 별도 계층 | SRP |
| 입력 데이터 타입 | IGraphDataInputHandler<T, E> 제네릭 통합 | LSP + ISP |
| 파싱 책임 | parseData()에 위임, GraphSession이 조율 | SRP |
| **조립 계층** | | |
| 그래프 종류 추가 | 새 조립자 + Drawer + InputHandler + GraphFactory 메서드 추가 | OCP |
| Graph 객체 생성 | GraphFactory 타입별 전용 메서드 (구체 클래스 반환) | OCP + 팩토리 패턴 |
| 조립 순서 제어 | GraphDirector (Director 역할 차용) + BuildResult | SRP |
| TypeSelector 주입 | GraphDirector 생성자 주입 | DIP |
| Drawer/VC/ScaleCalc 생성 | GraphFactory 전담 생성 및 주입 | DIP |
| **그래프 계층** | | |
| draw()/onDataChanged() 중복 제거 | AbstractGraph<T, D> — Template Method 패턴 | DRY + OCP |
| 축 있는 그래프 공통화 | AbstractAxisGraph<T, D> — drawBody()/IViewControllable 공통 구현 | DRY + OCP |
| 데이터 추가 기능 분리 | IDataAppendable<E> 별도 인터페이스 | ISP + SRP |
| 배치 처리 분리 | IBatchAppendable<E> — IDataAppendable 확장. 실시간 세션만 사용 | ISP |
| 그래프별 그리기 인터페이스 | IScatterDrawer / IBarDrawer / IPieDrawer 교차 인터페이스 | ISP |
| 뷰/축 기능 분리 | IViewControllable 별도 인터페이스 | ISP |
| 그리기 기능 분리 | ITitle / IAxis<T> / IPlot<T> / IAxisPlot<T> 별도 인터페이스. 축 매핑 유무로 Plot 분리 | ISP |
| 그리기 로직 변경 | Drawer 클래스만 수정 | SRP |
| 뷰/축 제어 변경 | ViewController만 수정 | SRP |
| n차원 뷰 | ViewController(dim) 생성자 주입 → getAvailableViews() 시그니처 통일 | DIP |
| **데이터 계층** | | |
| 데이터 구조 | 그래프마다 별도 Data 클래스. BarGraphData.direction은 생성자 주입 + 불변 | YAGNI |
| 옵저버 관리 | ObserverSupport 헬퍼 클래스 (has-a) | SRP + DRY |
| 데이터 추가 + redraw | appendData() → 옵저버 패턴 (동기) | OCP |
| 메타정보 | GraphMetadata 불변. 생성자에서만 설정 | SRP |
| **출력 계층** | | |
| 출력 방식 추가 | 새 Renderer 클래스 추가 | OCP + 브리지 패턴 |
| Renderer 주입 | 생성자 주입 | DIP |
| **그리기 내부** | | |
| 스케일 계산 | ScaleCalculator — Data 타입 무관, 값 목록만 수신 | SRP + OCP |
| 버킷 방식 렌더링 | Drawer 내부 grid 방식 | SRP |
| **세션/실시간** | | |
| 세션 흐름 제어 | GraphSession<T, E> | SRP |
| 세션 인터페이스 통일 | `ISession` — start()/stop()/await() 공통 계약 | OCP + DIP |
| 세션 완료 대기 | `ISession.await()` — C++ 전환 안전성 보장 | SRP |
| 실행 모드 분기 | `--mode` / `--source` / `--ui` 외부 옵션. 그래프 타입은 런타임 | SRP |
| 실시간 스트림 확장 | `RealtimeGraphSession<T, E>` + `DataQueue<E>` + `IRealtimeSource` 추가 | OCP |
| 실시간 스레드 분리 | receiverThread(IRealtimeSource 내부) / workerThread(dequeue 루프) 분리, DataQueue 버퍼 | SRP |
| 실시간 종료 제어 | `onStop` 콜백 + `DataQueue.close()` poison pill. 스트림 종료 = 세션 종료 | SRP |
| 실시간 그래프 조립 | `GraphDirector.constructRealtimeSession()` — 세션까지 조립. `assembleRealtimeSession()`으로 와일드카드 캡처. `constructGraph()`는 private 내부 메서드 | SRP + DIP |
| DataQueue 외부 주입 | `assembleRealtimeSession()` 내부에서 `DataQueue<E>` 생성. 타입 안전 보장 | DIP |
| 실시간 파싱 책임 | `IDataParser<E>` — handler의 `createParser()`가 생성, `IRealtimeSource<E>` 구현체가 사용 | SRP |
| 실시간 소스 생성 | `RealtimeSourceFactory` — Main이 주입, `assembleRealtimeSession()`에서 E 결정 후 소스 생성 | DIP |
| **C++ 전환** | | |
| C++ 소유권 | unique_ptr(단독)/shared_ptr(공유)/weak_ptr(옵저버) 명시 | - |
| C++ BlockingQueue | DataQueue 직접 구현 (mutex + condition_variable) | - |

### 미래 확장 (GUI 도입 시)

| 변경 축 | 대응 방법 | 원칙 |
|--------|----------|------|
| GUI 세션 전환 | `Main` 진입점 분기. 동기는 세션 불필요, 비동기는 `RealtimeGraphSession.start()` 재사용 | OCP |
| GUI 동기 처리 | 이벤트 핸들러 → `IDataAppendable<E>.appendData()` 직접 호출 | - |
| GUI 비동기 처리 | `DataQueue<E>` + `RealtimeGraphSession.start()` 재사용 | OCP |
| GUIRenderer 스레드 안전 | `print()` 내부에서 이벤트 스레드 위임 보장 | SRP |
| 배경색 / 그래프 색상 변경 | `GraphStyle` Color 필드 활용 | OCP |
| 데이터 포인트 기호 변경 | `ScatterPlotStyle.plotSymbol` / `BarGraphStyle.barSymbol` | OCP |
| 스타일 타입별 확장 | `GraphStyle` ← `ScatterPlotStyle` / `BarGraphStyle` 분기 | OCP |
| 스타일 주입 | `GraphFactory`에서 타입에 맞는 Style 생성 후 Drawer 주입 | DIP |

---

## 버전별 주요 변경 요약

| 항목 | ~v4.5.2 | v4.5.3 | v4.5.4 | v4.5.5 | v4.5.6 | v4.5.7 | v4.5.8 | v4.5.9 | v4.6.0 | v4.6.1 | v4.6.2 | v4.6.3 | v4.6.4 | v4.6.5 | v4.6.6 | v4.6.7 | v4.7.0 | v4.8.2 | v4.8.3 | v4.8.4 | v4.8.5 | v4.8.6 | v4.9.2 | v4.9.3 |
|------|---------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|
| Drawer 인터페이스 | IDrawer + IAxisDrawer | IScatterDrawer / IBarDrawer / IPieDrawer 교차 인터페이스 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | 존재 이유 설명 추가 | ← | ← | ← | 제네릭화: IAxis<T>, IPlot<T> | ← | **IPlot/IAxisPlot 분리 (ISP)** | ← | ← | **drawAxis()에 metadata 파라미터 추가** | **ScatterPlotDrawer·BarGraphDrawer에 gridHeight/gridWidth 필드 추가** |
| 조립자 draw() | drawer.render() | drawTitle() + drawAxis() + drawPlot() 직접 호출 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | **Template Method: AbstractGraph.draw() → drawBody()** | ← | ← | ← | ← | ← |
| ITypeSelector 주입 | 미정 | GraphDirector 생성자 주입 확정 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← |
| BarGraph dim | 미정 | dim=2 하드코딩 확정 | ← | ← | ← | ← | ← | ← | ← | ← | ← | 동적 입력으로 변경 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← |
| 최초 draw() 보장 | 미명시 | readData() 최소 1건 보장 명시 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← |
| 타입 안전성 | 산발적 언급 | ← | ← | B안 4지점 통일 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | **A안 전환 완료** | ← | ← | ← | ← | **캐스팅 제거 범위 명확화. IViewControllable 잔존 명시** | ← |
| calcRange() | 미명시 | ← | nice interval 추가 | ← | calcInterval() 연결 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | 시그니처 변경: List<Double> | ← | ← | ← | ← | ← | ← |
| GraphFactory | instanceof 분기 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | 타입별 전용 메서드 | **ViewController(dim) 주입** | ← | ← | ← | **static 명시** | **Drawer 생성 시 gridHeight/gridWidth 기본값 주입 명시** |
| 옵저버 관리 | Data 내부 중복 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ObserverSupport 분리 | ← | ← | ← | ← | **removeObserver 의도적 생략 근거 명시** | **suspend/resume 공통화 안 한 기술적 근거 추가** |
| 데이터 추가 | IGraph.appendData(Object) | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | IDataAppendable<E> 분리 | ← | ← | ← | ← | ← | ← |
| GUI 전환 경로 | 미명시 | ← | ← | ← | 한계 섹션 초안 | ← | 전면 재작성 | GUIGraphSession → 미정 | ← | ← | ← | ← | ← | ← | ← | GUI 세션 확정 | ← | ← | ← | ← | ← | ← | ← |
| 실시간 확장 | 없음 | ← | ← | ← | ← | ← | ← | ← | 추가 | 생성 주체 명시 | 스레드/종료 추가 | C안 확정 | ← | 코드 수정 | ← | C안 통일 | 제네릭화 | ← | ← | ← | constructRealtimeSession 추가 | **IRealtimeSource<E> 제네릭화. IDataParser<E> + RealtimeSourceFactory 도입. DataQueue 생성 내부 이동. 파싱 책임 확정** | ← |
| 세션 인터페이스 | 없음 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ISession 추가 | ← | await() 추가 | ← | ← | ← | ← | ← | ← |
| C++ 고려사항 | 없음 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | 소유권/BlockingQueue/await 추가 | ← | ← | ← | ← | ← | ← |
| 그래프 상속 계층 | 없음 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | **AbstractGraph + AbstractAxisGraph 도입** | **D 바운드 수정: ITitle만** | ← | ← | ← | ← |
| ViewController dim | 미명시 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | getAvailableViews(dim) 파라미터 | **생성자 주입. 시그니처 통일** | ← | ← | ← | **BarGraph view 무의미 설계 판단 명시** | **BarGraph.getAvailableViews() 오버라이드 — 빈 목록 반환. axisMapping 전환 차단** | **"전환 차단" → "뷰 목록 비노출"로 표현 교정. swapAxes/setView API 잔존 명시** |
| Plot 인터페이스 | 없음 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | IPlot<T> 단일 | ← | **IPlot<T> + IAxisPlot<T> 분리** | ← | ← | ← | ← |
| swapAxes 접근 경로 | 미명시 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | **view 명령어에 흡수. API로만 존재** | ← | ← | ← |
| 에러 처리 위치 | 미명시 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | **Session.run() + Main 두 계층. 실시간 스레드별 try-catch 명시** | ← | ← |
| IInputSource 계약 | readObject() : Object | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | **readObject(prompt : String) 오버로드 추가. 안내 출력 위임 메커니즘 확정** | **parseData/readData 예시에 readObject(prompt) 사용 형태 추가** |
| ConsoleInput 블로킹 해제 | 미명시 | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | **BlockingQueue 우회 방식 명시. Thread.interrupt() 한계 근거** |
| 미완성 항목 | - | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | ← | 클래스목록/종료조건/에러처리/drawPlot/constructGraph 완료 | GraphMetadata n차원 레이블 미확정 | ← | ← | ← | **S5 레이블 미지원 단서 추가** | ← |

> **v4.8.7**: 설계 변경 없음. 문서 구조 재배치(부품→조립→확장→횡단 순서), IGraphDataInputHandler 호출 순서 계약 명시, createParser() 배치 근거 명시, U1 현행화(F안 유력), S1/S3 타입 추적 추가, 검증 결과 S5 조건부 반영.
>
> **v4.8.8**: DataQueue에 tryDequeue()/capacity 추가, ObserverSupport에 suspend/resume 추가, 실시간 콘솔 전체 흐름도 추가, RealtimeGraphSession start()/run() 관계 명확화(workerThread), 최초 draw() 시점 의도, 실시간 인터랙션 불가, 파싱 실패 에러 경로, stream.stop() 멱등.
>
> **v4.8.9**: GraphDirector.constructGraphOnly() 추가, GUI 전환 경로 전면 재작성(블로킹 해결, GUIRenderer 텍스트 기반, onStop 시나리오별 구체화, 이벤트 핸들러 연결 경로).
>
> **v4.9.0**: IBatchAppendable<E> 분리(ISP), BarGraphData.direction 불변, GraphMetadata 불변, getAvailableViews() 순서 쌍 명확화, resume() dirty 플래그, createBar() static 수정, handler 안내 출력 위임 원칙, readObject() 근거 보강.
>
> **v4.9.1**: PieChart suspend/resume 명세 추가, 순서 쌍 후 뷰 예시·S5·S6 갱신, C++ IBatchAppendable 업캐스팅 주의, 흐름도 호출 주체 명시, 동기→비동기 전환 원칙, BarGraph axisMapping 무시 명시, range 중복 계산 인지, handler Console 접두어 메모, source.close() 구현체 책임.
>
> **v4.9.2**: [설계 변경] BarGraph.getAvailableViews() 오버라이드 — 빈 목록 반환, axisMapping 전환 차단. IInputSource에 readObject(prompt : String) 오버로드 추가, 안내 출력 위임 메커니즘 확정. [명세 보완] ScatterPlotDrawer·BarGraphDrawer에 gridHeight/gridWidth 필드 추가. ScatterPlotDrawer.drawAxis() 전치 시 레이블 매핑 흐름(axisMapping[0]→가로축, axisMapping[1]→세로축) 명시. [문서 보완] suspend/resume 공통화 안 한 기술적 근거(타입 바운드 불가) 추가. BarGraphDrawer.drawPlot()·drawAxis() 모두 axisMapping 무시 명시. ConsoleInput 블로킹 해제 — Thread.interrupt() 한계 및 BlockingQueue 우회 방식 명시. S2 시나리오에 BarGraph view 명령어 정합성 추가.
>
> **v4.9.3**: [명세 수정] ScatterPlotDrawer.drawAxis() 레이블 매핑 — 존재하지 않는 getLabel(int) 제거, 현재 getXLabel()/getYLabel() 기반 index 분기로 교체. U1(F안) 확정 시 getLabel(i)로 통일 예정 명시. [흐름도 보완] run() view 명령어 처리를 캐스팅 실패(PieChart)·빈 목록(BarGraph) 2단계로 분리 기술. [표현 교정] "axisMapping 전환 자체를 차단" → "콘솔 view 명령어를 통한 뷰 목록 비노출"로 정정. swapAxes()/setView() 프로그래밍 API는 상속 잔존 명시. [예시 보완] parseData() 코드 예시에 readObject(prompt) 사용 형태 반영. readData() 내부 prompt 전달 패턴 예시 추가. [경미] PieChartDrawer gridHeight/gridWidth 미추가 이유 명시(렌더링 방식 미확정).
---

## 시나리오 검증

설계의 완결성을 확인하기 위해 정상 흐름 14건, 에러 흐름 8건, 총 22개 시나리오를 추적 검증하였다. 모든 시나리오에서 흐름 단절, 타입 불일치, 누락된 경로가 발견되지 않았다.

### 정상 시나리오 (14건)

| # | 시나리오 | 검증 경로 | 결과 |
|---|---------|----------|------|
| S1 | 콘솔 대화형 산포도 — add로 데이터 추가, exit로 종료 | Main → construct() → GraphSession.start() → 명령어 루프(add → parseData → appendData → draw, exit → 종료) → await(). **타입 추적: T=ScatterPlotData, E=Point → GraphSession\<ScatterPlotData, Point\>, IDataAppendable\<Point\>.appendData(Point) — 컴파일 타임 보장** | ✅ |
| S2 | 콘솔 대화형 막대그래프 — direction VERTICAL, add로 추가, view 명령어 시도 | S1과 동일 경로. direction은 BarConsoleInputHandler.readData() 내부 처리. view 명령어 → BarGraph.getAvailableViews() → 빈 목록 반환 → "전환할 뷰가 없습니다" 안내 → 명령어 루프 복귀. axisMapping 전환 시도 없음 | ✅ |
| S3 | 콘솔 실시간 산포도 — 스트림 자체 종료 (경로 A) | Main → constructRealtimeSession(factory) → constructGraph() → assembleRealtimeSession() → handler.createParser() → factory.create(parser) → stream.stream(queue) → RealtimeGraphSession.start() → workerThread dequeue 루프 → IRealtimeSource 내부 queue.close() → poison pill → workerThread 종료 → onStop(멱등) → await() 해제. **타입 추적: assembleRealtimeSession()에서 T=ScatterPlotData, E=Point로 캡처 → IDataParser\<Point\>, DataQueue\<Point\>, IRealtimeSource\<Point\>, RealtimeGraphSession\<ScatterPlotData, Point\> — 모두 동일 E로 컴파일 타임 보장** | ✅ |
| S4 | 콘솔 실시간 산포도 — 외부에서 session.stop() (경로 B) | session.stop() → onStop.run() → stream.stop() (멱등) → queue.close() → poison pill → workerThread 종료 → join() | ✅ |
| S5 | 3차원 산포도 — dim=3, view 명령어로 뷰 네비게이션 | readData(dim=3) → GraphFactory.createScatter() → ViewController(3) → view 명령어 → getAvailableViews() → 순서 쌍 6개 목록 출력({0,1},{1,0},{0,2},{2,0},{1,2},{2,1}) → 번호 선택 → setView() → draw(). **단, 축 레이블 표시는 U1 미해결로 3번째 축 레이블 미지원** | ✅ (레이블 제외) |
| S6 | 2차원 산포도 — view 명령어 시 전치 전환 | ViewController(2) → getAvailableViews() → {0,1}, {1,0} 두 개 → 전치(가로축/세로축 뒤집기) 전환 가능 | ✅ |
| S7 | GUI 동기 — 버튼 클릭으로 데이터 추가 | 이벤트 핸들러 → appendable.appendData(point) → data.append() → observerSupport.notifyObservers() → AbstractGraph.onDataChanged() → AbstractGraph.draw() → drawBody() → GUIRenderer.print() → invokeLater(repaint) | ✅ |
| S8 | GUI 비동기 — 외부 스트림 + DataQueue 재사용 | 이벤트 핸들러/스트림 → queue.enqueue() → workerThread dequeue() → appendable.appendData() → draw() → GUIRenderer.print() → invokeLater(). onStop 콜백으로 종료 | ✅ |
| S9 | PieChart 추가 (미래) | PieChart extends AbstractGraph(축 없음) + IDataAppendable + drawBody()=drawPlot()만. PieChartData + PieChartDrawer(IPieDrawer) + PieConsoleInputHandler + GraphFactory.createPie() + GraphDirector 분기 추가. 기존 코드 수정 없음 | ✅ |
| S10 | PieChart에서 view 명령어 | graph를 IViewControllable로 캐스팅 시도 → PieChart는 미구현 → 캐스팅 실패 → "이 그래프는 뷰 전환을 지원하지 않습니다" 안내 → 명령어 루프 복귀 | ✅ |
| S11 | 막대그래프 모든 값 양수 (10, 20, 30) | values.add(0.0) → calcRange()가 0~30 포함 범위 계산 → toRow(0, ...) 정상 인덱스 → 기준선 grid 안에 위치 | ✅ |
| S12 | 막대그래프 음수값 포함 (-5, 10, 20) | 0이 이미 -5~20 범위 안 → values.add(0.0) 무해 → 정상 동작 | ✅ |
| S13 | 실시간 모드 연결 끊김 | SocketRealtimeSource 내부 연결 끊김 감지 → queue.close() → poison pill → workerThread 종료 → onStop(멱등) → 세션 종료 | ✅ |
| S14 | C++ 전환 시 안전성 | await()로 main 종료 방지. ObserverSupport에서 weak_ptr/raw pointer로 순환 참조 방지. DataQueue는 mutex + condition_variable로 직접 구현. 인터페이스는 virtual 소멸자 포함 | ✅ |

### 에러 시나리오 (8건)

| # | 시나리오 | 검증 경로 | 결과 |
|---|---------|----------|------|
| E1 | readData 첫 입력이 빈 줄 | 최소 1건 보장 정책 → "최소 1건 필요" 안내 → 재입력 요구. 빈 Data 생성 없음 | ✅ |
| E2 | readData 중 "abc" 입력 | 파싱 실패 → 에러 메시지 출력 → 해당 줄 무시 → 루프 계속. 정상 데이터만 수집 | ✅ |
| E3 | dim에 음수(-1) 입력 | "1 이상의 값을 입력하세요" 안내 → 재입력 요구. 음수 dim으로 Point 생성 안됨 | ✅ |
| E4 | 명령어 루프에서 "hello" 입력 | "add, view, exit 중 입력하세요" 안내 → 무시 → 명령어 루프 계속 | ✅ |
| E5 | add 후 parseData 실패 | 에러 메시지 출력 → 무시 → appendData 호출 안됨 → 명령어 루프 계속 | ✅ |
| E6 | view 후 범위 초과 번호 입력 | "유효한 번호를 입력하세요" 안내 → 뷰 전환 취소 → 명령어 루프 복귀 | ✅ |
| E7 | 실시간 수신 데이터 파싱 실패 | SocketRealtimeSource 내부에서 parser.parse(raw) 예외 → 해당 데이터 무시 → stderr 로그 출력 → queue에 넣지 않음 → workerThread 영향 없음 | ✅ |
| E8 | DataQueue.close() 이중 호출 | 멱등성 보장 (closed 플래그 이미 true → 재호출 시 무해) → 에러 없음 | ✅ |

### 검증 결과 요약

설계 오류 **0건**, 주의 사항 **1건** (S5: dim≥3 축 레이블은 U1 미해결로 미지원). 22개 시나리오 중 21건 완전 통과, 1건 조건부 통과.

---

## 미해결 항목

| # | 항목 | 상태 | 내용 |
|---|------|------|------|
| U1 | GraphMetadata의 n차원 레이블 | 미확정 | 현재 xLabel/yLabel 2개 필드로는 dim=3 이상의 축 레이블을 수용할 수 없음. v4.8.6에서 `drawAxis()`가 `metadata`를 직접 받도록 변경되었으므로, `metadata.getLabel(axisMapping[i])` 형태로 접근이 가능하다. 따라서 F안(GraphMetadata의 labels를 `List<String>`으로 변경)이 가장 자연스러운 해결책이 되었다. 기존 "Data=숫자, Metadata=부가정보" 분리 원칙과도 정합한다 |
