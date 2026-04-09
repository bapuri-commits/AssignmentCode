# 설계(v4.9.3) vs 구현 — 간극 분석 및 결정 기록

> 이 문서는 `design_v493.md`(설계)와 실제 Java 구현 사이의 모든 간극을 정리한다.
> 설계를 그대로 코드로 옮길 수 없는 지점, AI가 내린 결정, 사람이 구현한다면 고민할 포인트를 상세히 기록한다.

---

## 1. 의도적 대체 — SocketRealtimeSource → SimulatedRealtimeSource

### 설계

```
SocketRealtimeSource<E> implements IRealtimeSource<E>
- address : String
- parser  : IDataParser<E>   ← 생성자 주입
+ stream(queue) : 내부 스레드에서 소켓 읽기 → parser.parse() → queue.enqueue()
+ stop() : 멱등. 소켓 닫기 + queue.close()

SocketRealtimeSourceFactory implements RealtimeSourceFactory
- address : String
+ <E> create(parser) : new SocketRealtimeSource<>(address, parser)
```

### 구현

```
SimulatedRealtimeSource<E> implements IRealtimeSource<E>
- generator : Supplier<E>     ← 이미 파싱 완료된 데이터 생성기
- intervalMs : long
- count : int
+ stream(queue) : 내부 스레드에서 generator.get() → queue.enqueue() (주기적)
+ stop() : 멱등. stopped 플래그 + queue.close()

SimulatedRealtimeSourceFactory implements RealtimeSourceFactory
- rawGenerator : Supplier<String>
- intervalMs : long
- count : int
+ <E> create(parser) : parser와 rawGenerator를 합성 → SimulatedRealtimeSource 생성
```

### 간극과 결정 이유

| 항목 | 설계 (Socket) | 구현 (Simulated) | 결정 근거 |
|------|--------------|------------------|----------|
| 파서 위치 | Source 생성자에 주입, Source 내부에서 parse | Factory의 `create()`에서 합성하여 `Supplier<E>`로 전달 | Socket은 raw 문자열을 읽으므로 파서가 Source 안에 있어야 하지만, Simulated는 데이터를 직접 생성하므로 파서를 Factory 단계에서 소비하는 게 자연스러움 |
| 종료 조건 | 소켓 연결이 끊기면 종료 | `count`만큼 생성 후 자연 종료 | 네트워크 없이 테스트 가능한 유한 시뮬레이션 |
| 데이터 소스 | 네트워크 소켓 | `Supplier<E>` (보통 Random 기반) | 외부 인프라 의존 제거 |

### 사람이 고민할 포인트

- **파서를 어디서 호출할 것인가**: Socket 방식은 "raw 수신 → 파싱 → enqueue" 흐름이 Source 내부에서 완결된다. Simulated 방식은 Factory에서 `() -> parser.parse(rawGenerator.get())`로 합성한 `Supplier<E>`를 Source에 전달한다. 두 방식 모두 `RealtimeSourceFactory.create(IDataParser<E>)`의 계약을 이행하지만, Source 클래스의 책임 범위가 다르다.
- **Socket 구현으로 전환할 때**: `SimulatedRealtimeSource`를 `SocketRealtimeSource`로 교체하려면 Source 생성자가 `(address, parser)`를 받는 형태로 변경해야 한다. Factory의 `create()` 시그니처는 동일하게 유지 가능.

---

## 2. DataQueue — poison pill의 타입 문제

### 설계

```
DataQueue<E>
- queue : BlockingQueue<E>
+ close() : poison pill 삽입
```

설계는 `BlockingQueue<E>` 타입을 명시하지만, poison pill이 `E` 타입이 아닌 경우 어떻게 삽입할지 언급하지 않는다.

### 구현

```java
public class DataQueue<E> {
    private final BlockingQueue<Object> queue;  // ← Object로 선언
    private static final Object POISON = new Object();  // ← E가 아닌 센티널
    // ...
}
```

### 간극의 본질

Java 제네릭에서 `BlockingQueue<E>`에 `E`가 아닌 객체를 넣을 수 없다. poison pill은 "데이터가 아닌 종료 신호"이므로 본질적으로 `E` 타입이 아니다. 해결 방법은 세 가지:

| 방법 | 장단점 |
|------|--------|
| **A. `BlockingQueue<Object>` 사용 (현재 구현)** | poison과 데이터를 혼재. `@SuppressWarnings("unchecked")` 캐스팅 필요. 단순하고 동작 확실 |
| **B. `Optional<E>` 래핑** | `BlockingQueue<Optional<E>>` — `Optional.empty()`가 poison. 타입 안전하지만 모든 enqueue에 Optional 래핑 오버헤드 |
| **C. `closed` 플래그만 사용** | poison 없이 `closed + notify`로 종료. `dequeue()`에서 `while (!closed && queue.isEmpty()) wait()`. 표준적이지만 코드가 복잡 |

### 사람이 고민할 포인트

- 설계는 C++ 전환도 고려하는데, C++에서는 `std::optional<T>`로 poison pill을 자연스럽게 표현할 수 있다 (설계 문서의 C++ DataQueue 골격 참조). Java에서는 방법 A가 가장 실용적이나, `@SuppressWarnings`가 붙는 지점을 명확히 인지해야 한다.
- `dequeue()`에서 poison을 꺼낸 뒤 **다시 poison을 삽입**하는 이유: 다중 소비자 시나리오에서 하나의 소비자가 poison을 꺼내면 다른 소비자는 종료 신호를 받지 못한다. 현재는 소비자가 1개뿐이지만, 확장성을 위해 재삽입하는 것이 방어적 프로그래밍이다.

---

## 3. RealtimeGraphSession — 종료 경로 A에서 onStop 미호출

### 설계 (종료 경로 A)

```
receiverThread 내부 → queue.close() → poison pill → workerThread 루프 종료
→ onStop.run() (멱등) → session.await() 해제
```

### 구현

```java
private void run() {
    graph.draw();
    while (true) {
        E data = queue.dequeue();
        if (data == null) break;  // ← 루프 탈출
        // ... 배치 소비 ...
    }
    // ← 여기서 onStop.run()을 호출하지 않음. 그냥 스레드 종료
}
```

### 간극

설계에서는 경로 A(스트림 자체 종료)에서도 `onStop.run()`이 호출된다고 명시한다. 현재 구현에서는 루프 탈출 후 아무것도 하지 않는다.

**현재 동작에 문제가 없는 이유**: `SimulatedRealtimeSource`는 `stream()` 메서드의 `finally` 블록에서 이미 `queue.close()`를 호출한다. `onStop`은 `() -> source.stop()`인데, `source.stop()`은 `stopped = true; queue.close()`를 수행할 뿐이다. 이미 종료된 소스에 대해 멱등 호출이므로 결과는 동일하다.

**문제가 될 수 있는 경우**: 향후 `onStop` 콜백에 추가 정리 로직(로그 기록, 리소스 해제 등)이 포함되면, 경로 A에서 이 로직이 실행되지 않는다.

### 사람이 고민할 포인트

- 설계에 맞추려면 `run()` 메서드의 루프 탈출 후 `onStop.run()`을 추가해야 한다. 멱등성이 보장되므로 경로 B에서 중복 호출되어도 안전하다.
- 또는 `run()` 전체를 `try-finally`로 감싸서 `finally { onStop.run(); }`으로 처리하면 정상/예외 모두 커버 가능.

---

## 4. Main.java — 단순화된 진입점

### 설계

```java
String mode   = args.getOrDefault("--mode",   "interactive");
String ui     = args.getOrDefault("--ui",     "console");
String source = args.getOrDefault("--source", null);
// mode에 따라 construct() 또는 constructRealtimeSession() 분기
// ui에 따라 TextRenderer 또는 GUIRenderer 선택
```

### 구현

```java
public static void main(String[] args) {
    IInputSource source = new ConsoleInput();
    IRenderer renderer = new TextRenderer();
    ITypeSelector typeSelector = new ConsoleTypeSelector();
    GraphDirector director = new GraphDirector(typeSelector);
    ISession session = director.construct(source, renderer);
    session.start();
    session.await();
}
```

### 간극

- `--mode` / `--source` / `--ui` 인자 파싱 없음
- 실시간 모드 진입 경로 없음 (`constructRealtimeSession` 미사용)
- GUI 관련 분기 없음

### 결정 근거

현재 구현은 **콘솔 대화형(interactive + console)** 단일 경로만 지원한다. 실시간 모드는 `RealtimeDemo.java`로 별도 진입점을 제공하여 테스트한다. 설계의 `--mode` 분기는 Socket 구현이 완성된 후 Main에 추가하는 것이 자연스럽다.

### 사람이 고민할 포인트

- `RealtimeDemo`가 `GraphDirector.constructRealtimeSession()`을 우회하고 직접 컴포넌트를 조립한다. 이는 테스트 편의를 위한 것이지만, Director의 조립 로직이 검증되지 않는 사각지대가 된다.
- 설계 완성도를 높이려면, Main에서 `--mode realtime` 경로를 추가하고 `RealtimeDemo`의 로직을 Director 경유로 변경해야 한다.

---

## 5. ScatterConsoleInputHandler.readMetadata() — n차원 레이블 미지원

### 설계

```
ScatterConsoleInputHandler.readMetadata() 입력 순서:
1. 그래프 제목 입력
2. dim개의 축 레이블 입력   ← dim 필드 참조
→ GraphMetadata 생성
```

### 구현

```java
public GraphMetadata readMetadata(IInputSource source) {
    String title = (String) source.readObject("그래프 제목: ");
    String xLabel = (String) source.readObject("X축 레이블: ");
    String yLabel = (String) source.readObject("Y축 레이블: ");
    return new GraphMetadata(title, xLabel, yLabel);
}
```

### 간극

설계는 `dim`개의 축 레이블을 입력받아야 한다고 명시하지만, `GraphMetadata`는 `title`, `xLabel`, `yLabel` 3개 필드만 보유한다 (설계 문서도 동일). 따라서 dim=2일 때만 정상 동작하며, dim≥3이면 3번째 이후 축 레이블을 입력받을 수 없다.

이것은 설계 문서의 미해결 항목 U1과 직결된다:

> **U1**: GraphMetadata의 n차원 레이블 — 현재 xLabel/yLabel 2개 필드로는 dim=3 이상의 축 레이블을 수용할 수 없음.

### 구현의 대응

`ScatterPlotDrawer.getLabelForAxis()`에서 dim≥3인 축에 대해 `"축" + axisIndex` 폴백을 제공한다:

```java
private String getLabelForAxis(int axisIndex, GraphMetadata metadata) {
    if (axisIndex == 0) return metadata.getXLabel();
    if (axisIndex == 1) return metadata.getYLabel();
    return "축" + axisIndex;  // ← dim≥3 폴백
}
```

### 사람이 고민할 포인트

- `readMetadata()`에서 dim개의 레이블을 입력받으려면 `GraphMetadata`를 `List<String> labels`로 변경해야 한다 (U1 해결). 하지만 이렇게 하면 `BarConsoleInputHandler.readMetadata()`에서도 `List<String>`을 사용해야 하고, `BarGraphDrawer`의 레이블 접근 로직도 변경된다.
- **현재 요구사항이 2D**라면 U1을 미해결로 두는 것이 합리적이다. 다만 readMetadata()에서 "dim개 입력" 대신 "xLabel, yLabel 고정 입력"을 하고 있음을 인지해야 한다.

---

## 6. IInputSource.readObject() 반환 타입 — Object vs String

### 설계

```
IInputSource
+ readObject() : Object
+ readObject(prompt : String) : Object
+ close() : void
```

> readObject()의 반환 타입이 Object인 이유: 향후 제네릭화(IInputSource<T>)로 String과 바이너리를 모두 수용할 수 있다. 현재는 Object로 두되, 캐스팅 지점이 명령어 루프 1곳으로 한정.

### 구현

설계를 그대로 따랐다. 모든 호출 지점에서 `(String) source.readObject(prompt)` 캐스팅이 발생한다.

### 사람이 고민할 포인트

- 현재 모든 사용처가 `(String)` 캐스팅을 수행한다. `IInputSource`를 `IInputSource<String>`으로 제네릭화하면 캐스팅이 완전히 제거되지만, `GraphDirector`와 `IGraphDataInputHandler`의 시그니처까지 전파된다.
- 설계는 "향후 바이너리 입력"을 위해 `Object`를 유지하지만, 실제로 바이너리 입력이 필요해질 때 `IInputSource<T>`로 전환하면 충분하다. 현시점에서 `String`으로 고정하는 것도 합리적 선택이다 (YAGNI).
- `readObject()`가 `null`을 반환할 수 있다 (`Scanner.hasNextLine() == false`일 때). `GraphSession.run()`에서 `null` 체크 → break로 처리하고 있지만, 다른 호출 지점(handler 내부)에서는 NPE 위험이 있다.

---

## 7. ConsoleTypeSelector — 안내 출력의 위치

### 설계 원칙

> 안내 출력은 구현체 책임. handler가 source.readObject("dim을 입력하세요")처럼 프롬프트를 인자로 전달하면, ConsoleInput은 프롬프트를 System.out에 출력하고 입력을 받고, GUIInput은 대화 상자를 띄우고 입력을 받는다.

### 구현

```java
// ConsoleTypeSelector — readObject(prompt)로 안내 위임 ✅
source.readObject("그래프 타입을 선택하세요 (scatter / bar): ");

// 에러 안내는 System.out.println() 직접 호출 ❌
System.out.println("scatter 또는 bar를 입력하세요.");
```

```java
// ScatterConsoleInputHandler — 동일 패턴
System.out.println("1 이상의 정수를 입력하세요.");  // ← 직접 출력
System.out.println("최소 1건의 데이터가 필요합니다."); // ← 직접 출력
```

### 간극

프롬프트(입력 요청)는 `readObject(prompt)`로 올바르게 위임하지만, **에러/안내 메시지**는 `System.out.println()`으로 직접 출력한다. GUI 전환 시 이 메시지들이 콘솔에만 출력되고 GUI에는 표시되지 않는다.

### 사람이 고민할 포인트

- 설계 원칙을 완벽히 따르려면 에러 메시지도 `IInputSource`에 위임해야 한다. 하지만 현재 `IInputSource`에는 "출력 전용" 메서드가 없다. `readObject(prompt)`는 "출력 후 입력"이지 "출력만"은 아니다.
- 해결 방법: (A) `IInputSource`에 `void display(String message)` 메서드 추가, (B) 별도 `IOutputSink` 인터페이스 도입, (C) 현행 유지 후 GUI 전환 시 handler 이름에서 "Console" 접두어를 유지하며 콘솔 전용으로 한정.
- 설계 문서도 이 문제를 인식하고 있다: "handler 이름의 'Console' 접두어는 최초 구현 환경을 반영한 것이며, handler가 진정으로 UI 무관하다면 GUI 도입 시 이름 변경을 고려한다."

---

## 8. GraphSession — stop()과 close()의 관계

### 설계

```
GraphSession.stop() : source.close() 후 루프 종료
GraphSession.run() finally → source.close()
```

> stop()은 source.close()를 호출하여 입력 스트림을 닫고 루프를 종료한다.

### 구현

```java
@Override
public void stop() { source.close(); }

private void run() {
    try {
        // ... 명령어 루프 ...
    } finally {
        source.close();  // ← exit으로 정상 종료해도 close()
    }
}
```

### 간극은 없으나 주의할 점

- `stop()`과 `run()` 모두 `source.close()`를 호출할 수 있으므로, `ConsoleInput.close()`는 **멱등**이어야 한다. 현재 `Scanner.close()`는 이미 닫힌 Scanner에 대해 예외를 던지지 않으므로 안전하다.
- 설계는 "stop() 시 블로킹 해제를 위해 ConsoleInput은 내부적으로 입력 스레드를 분리할 수 있다"고 언급한다. 현재 `ConsoleInput`은 단순 `Scanner` 래핑이므로, 외부 스레드에서 `stop()`을 호출해도 `readObject()`의 `scanner.nextLine()` 블로킹이 즉시 해제되지 않는다. **`exit` 명령어로만 정상 종료 가능**하다.

### 사람이 고민할 포인트

- `System.in` 기반 `Scanner`는 `Thread.interrupt()`로 중단되지 않는다 (Java의 알려진 한계). 설계가 제안하는 "내부 BlockingQueue 우회" 패턴을 구현하려면 `ConsoleInput`을 크게 리팩토링해야 한다.
- 현재 콘솔 대화형에서는 `exit` 명령이 유일한 종료 경로이므로 실질적 문제는 없다. GUI 전환 시 `GUIInput`에서는 이 문제가 자연스럽게 해결된다 (대화 상자 닫기 → BlockingQueue 닫기).

---

## 9. 미구현 인터페이스 및 클래스

### 설계에 있지만 구현하지 않은 것들

| 항목 | 설계상 위치 | 미구현 이유 |
|------|-----------|-----------|
| `IPlot<T>` | 그리기 인터페이스 | PieChart 전용. PieChart가 미래 확장이므로 YAGNI |
| `IPieDrawer` | 그리기 인터페이스 | 동일 |
| `PieChart`, `PieChartData`, `PieChartDrawer` | 그래프 구체 클래스 | 미래 확장 |
| `constructGraphOnly()` | GraphDirector | GUI 동기 모드 전용. 현재 콘솔 모드만 구현 |
| `SocketRealtimeSource` | 입력 계층 | SimulatedRealtimeSource로 대체 |
| `SocketRealtimeSourceFactory` | 입력 계층 | SimulatedRealtimeSourceFactory로 대체 |
| `FileInput`, `GUIInput` | IInputSource 구현체 | 미래 확장 |
| `GUIRenderer` | IRenderer 구현체 | 미래 확장 |
| `GUITypeSelector` | ITypeSelector 구현체 | 미래 확장 |
| `GraphStyle` 계층 | 스타일 | GUI 도입 시 추가 예정 |

### 사람이 고민할 포인트

- `IPlot<T>`와 `IAxisPlot<T>`의 분리는 설계의 핵심 ISP 포인트이다. 현재는 `IAxisPlot<T>`만 존재하므로 `IPlot<T>`가 없어도 컴파일되지만, PieChart 추가 시 반드시 도입해야 한다.
- `constructGraphOnly()`가 없으면 GUI 동기 모드를 구현할 때 `BuildResult`를 외부에 노출하는 방법을 다시 설계해야 한다.

---

## 10. DataQueue — capacity 초과 정책

### 설계

> bounded queue — 생성 시 capacity를 지정. 용량 초과 시 정책(drop-oldest, drop-newest, 블로킹 등)은 실시간 데이터의 성격에 따라 구현체가 결정한다.

### 구현

```java
public void enqueue(E data) {
    if (closed.get()) return;
    try {
        queue.put(data);  // ← 블로킹. 용량 꽉 차면 대기
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

`ArrayBlockingQueue.put()`은 용량이 꽉 차면 **블로킹**한다. 즉 producer 스레드가 consumer가 소비할 때까지 대기한다.

### 사람이 고민할 포인트

- **블로킹 정책의 위험**: producer(`SimulatedRealtimeSource`)가 `queue.put()`에서 블로킹되면, `stop()`을 호출해도 producer 스레드가 깨어나지 않을 수 있다. `stop()`은 `stopped = true`를 설정하고 `queue.close()`로 poison을 넣으려 하지만, 큐가 꽉 차있으면 `offer(POISON)`이 실패한다.
- **완화 요인**: `capacity + 1`로 poison 슬롯을 확보했고, consumer가 지속적으로 소비하므로 큐가 꽉 찬 채로 오래 유지되지 않는다. SimulatedRealtimeSource의 intervalMs(1초) 덕분에 실질적 데드락 가능성은 매우 낮다.
- **엄밀한 해결**: `offer(data, timeout, unit)` + drop-oldest, 또는 producer 스레드 참조를 저장하여 `stop()` 시 `thread.interrupt()` 호출.

---

## 11. SimulatedRealtimeSource — 스레드 관리

### 설계 (SocketRealtimeSource 기준)

설계는 `stop()` 시 "소켓 닫기"로 receiverThread를 종료시키는 방식을 사용한다. 소켓 닫기는 `readFromSocket()`에서 IOException을 발생시켜 스레드를 자연스럽게 종료한다.

### 구현

```java
public void stream(DataQueue<E> queue) {
    this.queue = queue;
    new Thread(() -> {
        try {
            for (int i = 0; i < count && !stopped; i++) {
                E data = generator.get();
                queue.enqueue(data);
                Thread.sleep(intervalMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            queue.close();
        }
    }).start();  // ← 스레드 참조를 저장하지 않음
}
```

### 간극

- **스레드 참조 미보유**: 생성한 스레드의 참조를 필드에 저장하지 않으므로, `stop()` 호출 시 스레드를 `interrupt()`할 수 없다. `stopped` 플래그를 `volatile`로 선언하여 다음 루프 반복에서 감지하도록 했으나, `Thread.sleep(intervalMs)` 중에는 최대 intervalMs만큼 대기 후에야 종료된다.
- **InterruptedException 경로**: 스레드 참조가 있다면 `stop()` 시 `thread.interrupt()` → `Thread.sleep()`에서 즉시 `InterruptedException` 발생 → 즉각 종료가 가능하다.

### 사람이 고민할 포인트

- 현재 intervalMs=1000ms(1초)이므로 최악의 경우 1초 지연 후 종료된다. 사용 시나리오에 따라 수용 가능할 수 있다.
- 엄밀하게 구현하려면: `private Thread producerThread;` 필드 추가 → `stream()`에서 저장 → `stop()`에서 `producerThread.interrupt()` 호출.

---

## 12. ObserverSupport — 스레드 안전성

### 설계

> 옵저버 패턴은 **동기 구조**로 확정.
> receiverThread는 queue에 넣기만 하고 appendData()를 직접 호출하지 않는다.
> 따라서 두 스레드가 draw()를 동시 호출하는 상황은 발생하지 않는다.

### 구현

```java
public class ObserverSupport {
    private final List<IGraphObserver> observers = new ArrayList<>();
    private boolean suspended = false;
    private boolean dirty = false;
    // ← synchronized 없음
}
```

### 간극은 없으나 주의할 점

설계가 정확히 명시한 대로, `ObserverSupport`의 모든 메서드(`notifyObservers`, `suspend`, `resume`)는 **workerThread에서만 호출**된다. `addObserver()`는 `GraphDirector.constructGraph()`에서 세션 시작 전에 호출되므로 스레드 경합이 없다. 따라서 `synchronized`가 불필요하다.

### 사람이 고민할 포인트

- 이 "단일 스레드 접근 보장"은 코드에 명시되지 않은 **암묵적 계약**이다. 미래에 누군가가 다른 스레드에서 `addObserver()`나 `notifyObservers()`를 호출하면 `ConcurrentModificationException`이 발생할 수 있다.
- 방어적으로 `synchronized`를 추가하거나, 클래스 Javadoc에 "이 클래스는 스레드 안전하지 않음. 단일 스레드에서만 사용해야 함"을 명시해야 한다.

---

## 13. GraphFactory — Drawer 생성 시 ScaleCalculator 공유 여부

### 설계

```java
ScaleCalculator scaleCalc = new ScaleCalculator();
IScatterDrawer drawer = new ScatterPlotDrawer(scaleCalc);
```

### 구현 — 설계와 동일

```java
public static ScatterPlot createScatter(...) {
    ScaleCalculator scaleCalc = new ScaleCalculator();
    IScatterDrawer drawer = new ScatterPlotDrawer(scaleCalc);
    // ...
}
```

### 사람이 고민할 포인트

- `ScaleCalculator`가 **상태를 갖지 않는** 순수 유틸리티 클래스라면, 매번 새 인스턴스를 만들 필요 없이 `static` 메서드 모음 또는 싱글턴으로 충분하다. 현재 설계는 인스턴스를 주입하는 방식을 택했는데, 이는 향후 "다른 스케일 알고리즘을 주입"할 가능성을 열어둔 것이다.
- 설계는 "range 계산이 drawAxis()와 drawPlot()에서 각각 수행되어 2회 중복"됨을 인지하고 있다. 캐싱이 필요해지면 Drawer 내부에서 처리하거나 `drawBody()`에서 range를 선계산하는 최적화를 고려한다.

---

## 14. Pair 클래스 — 커스텀 구현

### 설계

> Pair<String, Double> — javafx.util.Pair 명시적 배제

### 구현

`makeagraph.util.Pair<A, B>` 커스텀 클래스를 생성했다.

### 사람이 고민할 포인트

- Java 표준 라이브러리에 범용 Pair가 없다. `javafx.util.Pair`는 JavaFX 의존성을 도입하고, `Map.Entry`는 의미론이 다르다.
- 커스텀 Pair가 `equals()`, `hashCode()`, `toString()`을 구현하는지 확인해야 한다. 특히 HashMap/HashSet에 키로 사용하거나 중복 검사가 필요할 때.

---

## 15. BarGraphDrawer.drawAxis() — direction에 따른 레이블 매핑

### 설계

> BarGraphDrawer는 axisMapping을 무시하고 data.getDirection()으로 렌더링 방향을 결정한다.
> VERTICAL이면 x축=카테고리, y축=값, HORIZONTAL이면 x축=값, y축=카테고리.

### 구현

```java
if (dir == Direction.VERTICAL) {
    lines.add("Y (" + metadata.getYLabel() + "): " + String.join(", ", valueTicks));
    lines.add("X (" + metadata.getXLabel() + "): " + String.join(", ", categories));
} else {
    lines.add("Y (" + metadata.getYLabel() + "): " + String.join(", ", categories));
    lines.add("X (" + metadata.getXLabel() + "): " + String.join(", ", valueTicks));
}
```

### 사람이 고민할 포인트

- **레이블과 데이터의 불일치 가능성**: 사용자가 `readMetadata()`에서 "X축 레이블: 성적", "Y축 레이블: 학생 수"를 입력하고 direction=HORIZONTAL을 선택하면, Y축에 카테고리(성적)가 표시되고 X축에 값(학생 수)이 표시된다. 하지만 레이블은 여전히 "Y (학생 수)", "X (성적)"으로 **원래 입력 그대로** 출력된다. HORIZONTAL에서는 Y가 카테고리, X가 값이므로 레이블이 뒤바뀌어야 할 수 있다.
- 이것은 "direction에 따라 metadata의 xLabel/yLabel을 교환해야 하는가"라는 의미론적 질문이다. 설계는 이 부분을 명시하지 않는다. 현재 구현은 metadata를 있는 그대로 출력한다.

---

## 16. RealtimeDemo — GraphDirector 우회

### 설계

```
Main (실시간)
 ├── director.constructRealtimeSession(source, renderer, factory) → ISession
 ├── session.start()
 └── session.await()
```

### 구현 (RealtimeDemo)

```java
// GraphDirector를 사용하지 않고 직접 조립
ScatterPlotData data = new ScatterPlotData();
data.append(Point.of(10, 20));
ScatterPlot graph = GraphFactory.createScatter(data, meta, renderer);
data.addObserver(graph);

DataQueue<Point> queue = new DataQueue<>(64);
IRealtimeSource<Point> source = factory.create(parser);
source.stream(queue);

RealtimeGraphSession<ScatterPlotData, Point> session =
    new RealtimeGraphSession<>(queue, graph, graph, () -> source.stop());
```

### 간극

`RealtimeDemo`는 `GraphDirector`를 거치지 않고 모든 컴포넌트를 직접 조립한다. 이는 다음을 의미한다:

1. `GraphDirector.constructRealtimeSession()`의 조립 로직이 검증되지 않는다.
2. `constructGraph()` 내부의 타입 선택 → readData → readMetadata → 팩토리 → 옵저버 등록 흐름이 테스트되지 않는다.
3. 대신 각 컴포넌트의 개별 동작은 검증된다.

### 사람이 고민할 포인트

- Director를 경유하는 통합 테스트를 별도로 작성해야 한다. 하지만 Director 경유 시 콘솔 입력이 필요하므로 자동화된 테스트가 어렵다 (stdin 모킹 필요).
- `IInputSource`를 mock으로 구현하여 미리 정의된 응답을 반환하는 `MockInputSource`를 만들면 Director 통합 테스트가 가능하다.

---

## 17. GraphSession.handleView() — 현재 뷰 표시 없음

### 설계

```
> view
사용 가능한 뷰:
  1. x - y
  2. y - x
  3. x - z  [현재]     ← 현재 뷰 표시
```

### 구현

```java
for (int i = 0; i < views.size(); i++) {
    System.out.printf("  %d. 축%d - 축%d%n", i + 1, views.get(i)[0], views.get(i)[1]);
}
// ← [현재] 표시 없음
```

### 간극

- 현재 어떤 뷰가 활성화되어 있는지 표시하지 않는다.
- `ViewController`에서 현재 `axisMapping`을 가져와 비교하면 구현 가능하나, `IViewControllable` 인터페이스에 `getAxes()` 같은 현재 상태 조회 메서드가 없다 (`ViewController`에는 있지만 인터페이스에는 노출되지 않음).

### 사람이 고민할 포인트

- `IViewControllable`에 `getAxes() : int[]`를 추가할지 여부. 현재 인터페이스는 "변경" 메서드만 제공하고 "조회"는 제공하지 않는다. ISP 관점에서 별도 인터페이스로 분리할 수도 있다.

---

## 18. 설계에 명시되었으나 구현에서 달라진 세부 사항

| 항목 | 설계 | 구현 | 비고 |
|------|------|------|------|
| `AbstractGraph.draw()` 접근 제어자 | "final 권장" | `public final void draw()` ✅ | 설계 의도 준수 |
| `ViewController.setView()` 유효성 검증 | "axes.length != 2이면 예외" | 별도 검증 없음 | `setView()`에 잘못된 길이 배열이 전달될 경우 ArrayIndexOutOfBoundsException 발생 |
| `GraphSession.run()` 전체 구조 | try-catch-finally 3단 | try-catch-finally 구현 ✅ | 설계 일치 |
| `ISession.start()` 논블로킹 계약 | "항상 논블로킹" | 새 Thread 생성 후 즉시 반환 ✅ | 설계 일치 |
| `BarGraphDrawer.drawPlot()` 경계 검사 | 설계 코드에 없음 | `if (r >= 0 && r < gridHeight && col >= 0 && col < gridWidth)` 추가 ✅ | 방어적 코딩. 설계보다 안전 |
| view 목록 형식 | "x - y" | "축0 - 축1" | 레이블 대신 축 인덱스 사용. getLabelForAxis()를 활용하면 개선 가능 |

---

## 19. 전체 요약 — 간극의 심각도 분류

### 즉시 수정 고려 (설계 위반)

| # | 항목 | 심각도 | 대응 |
|---|------|--------|------|
| 3 | RealtimeGraphSession 경로 A에서 onStop 미호출 | 낮음 (현재 동작에 문제 없음) | run()에 finally { onStop.run(); } 추가 권장 |
| 17 | view 목록에 [현재] 표시 없음 | 낮음 (UX) | ViewController.getAxes()와 비교 로직 추가 |

### 의도적 결정 (설계와 다르지만 합당한 이유 있음)

| # | 항목 |
|---|------|
| 1 | SocketRealtimeSource → SimulatedRealtimeSource |
| 4 | Main.java 단순화 |
| 9 | 미래 확장 인터페이스 미구현 (YAGNI) |
| 16 | RealtimeDemo가 GraphDirector 우회 |

### 설계 미비 / 열린 문제

| # | 항목 |
|---|------|
| 2 | DataQueue poison pill의 타입 문제 (설계가 Java 구현 레벨까지 명시하지 않음) |
| 5 | GraphMetadata n차원 레이블 (U1 미해결) |
| 7 | handler 에러 메시지의 직접 출력 (설계 원칙과 구현 사이의 마찰) |
| 10 | DataQueue capacity 초과 시 블로킹 정책의 잠재적 데드락 |
| 15 | BarGraphDrawer HORIZONTAL 시 레이블 의미론 |
