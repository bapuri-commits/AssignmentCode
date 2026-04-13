# MakeAGraph: 설계 → 구현 간극 정리

> v4.9.3 설계 문서를 Java 코드로 옮기면서 마주한 결정, 타협, 발견의 기록.
> 감사(audit) 결과 발견된 버그와 설계 미비 사항도 포함한다.

---

## 1. 설계 문서의 "주소(address)" vs 실제 소켓 연결

### 설계

```
SocketRealtimeSource<E>
- address : String
+ SocketRealtimeSource(address, parser)
```

설계 문서는 `address`를 단일 `String` 필드로 정의했다. `SocketRealtimeSourceFactory(address)` 역시 하나의 문자열만 받는다.

### 구현에서의 간극

Java의 `new Socket(host, port)`는 host와 port를 분리해서 받는다. 단일 문자열 `"localhost:9090"`을 받으면 팩토리나 소스 내부에서 파싱 로직이 필요하다. 이 파싱 책임을 어디에 둘 것인지가 문제였다.

### 결정

- `SocketRealtimeSource`와 `SocketRealtimeSourceFactory` 모두 `(String host, int port)` 두 파라미터를 받도록 구현.
- `host:port` 파싱은 `Main.main()`에서 커맨드라인 인자를 처리할 때 한 번만 수행.
- 팩토리가 이미 파싱된 값을 보유하므로, 내부 로직에서 재파싱 불필요.

### 근거

파싱 책임을 진입점(Main)에 집중시키면 소스/팩토리는 순수하게 네트워크 연결만 담당한다. "address를 String 하나로 들고 다니다가 connect 시점에 파싱"하면 `MalformedAddressException` 같은 에러가 스트림 시작 시점까지 지연되어 디버깅이 어려워진다. 빠른 실패(fail-fast) 원칙에 부합하는 선택.

### 사람이 고민할 포인트

- Socket 구현으로 전환할 때 `SimulatedRealtimeSource`와 `SocketRealtimeSource`가 **생성자 시그니처가 다르다**는 점을 인식해야 한다. 팩토리 인터페이스의 `create(parser)` 시그니처가 이 차이를 흡수하지만, 팩토리 자체의 생성은 `Main`이 책임진다.
- IPv6 주소(`[::1]:9090`)를 고려하면 `split(":", 2)` 파싱이 깨질 수 있다. 프로덕션에서는 `URI` 파서를 사용하는 것이 안전하다.

---

## 2. SimulatedRealtimeSource — 설계에 없는 클래스

### 설계

설계 문서에는 `SocketRealtimeSource<E>`만 명시되어 있다. 테스트용 시뮬레이션 소스에 대한 언급이 없다.

### 구현 시 필요성

소켓 서버 없이도 실시간 기능을 검증해야 했다. `SocketRealtimeSource`를 먼저 구현하려면 별도 서버 프로세스가 필요한데, 과제 환경에서 이를 보장할 수 없다.

### 결정

- `SimulatedRealtimeSource<E>` + `SimulatedRealtimeSourceFactory`를 추가 구현.
- `Supplier<E>` 기반 생성기로 데이터를 주기적으로 생성하여 `DataQueue`에 넣는 구조.
- `RealtimeDemo.java`가 이 시뮬레이션 소스를 사용해 실시간 기능을 시연.

### 설계 원칙과의 정합성

`IRealtimeSource<E>` 인터페이스와 `RealtimeSourceFactory` 팩토리 인터페이스 덕분에, 소스 구현체를 추가해도 세션이나 디렉터 코드는 변경 없음 (OCP). `SimulatedRealtimeSourceFactory`는 `RealtimeSourceFactory`를 구현하므로 `GraphDirector.constructRealtimeSession()`에 그대로 주입 가능.

단, `SimulatedRealtimeSourceFactory`의 생성자 시그니처가 `SocketRealtimeSourceFactory`와 다르다 — `Supplier<String>`과 interval/count를 받는다. 팩토리 인터페이스가 `create(parser)` 시그니처만 통일하므로, 팩토리 **생성** 시점의 차이는 `Main`이 흡수한다. 이것이 팩토리 패턴의 가치 — "어떻게 만들 것인가"는 팩토리 내부에 캡슐화.

### 파서 위치의 차이 — Socket vs Simulated

| 항목 | 설계 (Socket) | 구현 (Simulated) |
|------|--------------|------------------|
| 파서 위치 | Source 생성자에 주입, Source 내부에서 parse | Factory의 `create()`에서 합성하여 `Supplier<E>`로 전달 |
| 종료 조건 | 소켓 연결이 끊기면 종료 | `count`만큼 생성 후 자연 종료 |
| 데이터 소스 | 네트워크 소켓 | `Supplier<E>` (보통 Random 기반) |

Socket은 "raw 수신 → 파싱 → enqueue" 흐름이 Source 내부에서 완결된다. Simulated는 Factory에서 `() -> parser.parse(rawGenerator.get())`로 합성한 `Supplier<E>`를 Source에 전달한다. 두 방식 모두 `RealtimeSourceFactory.create(IDataParser<E>)`의 계약을 이행하지만, Source 클래스의 책임 범위가 달라진다.

---

## 3. RealtimeSourceFactory의 시그니처 — 설계의 "address"를 받지 않는 인터페이스

### 설계

```java
interface RealtimeSourceFactory {
    <E> IRealtimeSource<E> create(IDataParser<E> parser);
}
```

설계 문서에서 `SocketRealtimeSourceFactory(address)`는 생성자에서 address를 받고, `create()` 시그니처에는 parser만 있다.

### 구현에서의 간극

인터페이스가 제네릭 메서드 `<E> create(IDataParser<E> parser)`를 정의하고 있어서, 구현체가 연결 정보(host/port, supplier 등)를 **필드로 보유**해야 한다. 이는 설계 의도와 일치하지만, 실제 코드로 옮기면 팩토리 인터페이스 자체는 연결 정보에 대해 완전히 무지하다.

### 교훈

팩토리 패턴에서 "생성자에 주입하는 정보"와 "create() 메서드에 전달하는 정보"의 분리가 핵심이다. 이 분리가 Main이 E를 모르면서도 팩토리를 주입할 수 있게 해준다 — `Main`은 `new SocketRealtimeSourceFactory(host, port)`만 하고, E는 `assembleRealtimeSession()` 내부에서 결정된다.

설계 문서가 이 분리를 정확히 기술하고 있었지만, 실제로 코드를 작성해보니 "왜 create()에 address가 없는지"가 비로소 체감되었다.

---

## 4. IPlot\<T\> — 구현체 없이 인터페이스만 존재하는 상황

### 설계

```
IPlot<T>
+ drawPlot(data : T) : List<String>

PieChartDrawer implements IPieDrawer  (미래)
IPieDrawer extends ITitle, IPlot<PieChartData>  (미래)
```

### 구현에서의 간극

`IPlot<T>`는 축 없는 그래프(PieChart 등)의 Drawer가 구현할 인터페이스인데, 현재 PieChart 자체가 미래 항목이다. 그러면 `IPlot<T>`를 지금 만들어야 하는가?

### 결정

만들었다. 이유:

1. **인터페이스 계층의 완결성**: `IAxisPlot<T>`이 존재하면서 `IPlot<T>`가 없으면, "축 매핑 없는 그래프의 그리기 계약"이 설계에만 존재하고 코드에는 없는 상태. 새 개발자가 PieChart를 추가할 때 인터페이스부터 만들어야 하는 번거로움.
2. **ISP 분리의 완성**: `IAxisPlot<T>`와 `IPlot<T>`가 독립 인터페이스라는 설계 의도를 코드에 명시.
3. **비용 0**: 인터페이스 하나 추가에 부작용 없음.

### 사람이 고민할 포인트

`IPlot<T>`와 `IAxisPlot<T>`의 분리는 설계의 핵심 ISP 포인트이다. 현재는 `IAxisPlot<T>`만 사용되므로 `IPlot<T>`가 없어도 컴파일되지만, PieChart 추가 시 반드시 도입해야 한다. 이 인터페이스의 존재 자체가 "축 없는 그래프도 지원한다"는 설계 의도의 증거이다.

---

## 5. constructGraphOnly() — 호출자 없는 public 메서드

### 설계

```
GraphDirector
+ constructGraphOnly(source, renderer) : BuildResult<?, ?>   ← GUI 동기용
```

### 구현에서의 간극

현재 콘솔 모드만 구현되어 있어서 `constructGraphOnly()`를 호출하는 코드가 없다. GUI 동기 모드에서 이벤트 핸들러가 `BuildResult`에서 graph/appendable을 꺼내 직접 조작하는 시나리오를 위한 메서드.

### 결정

구현했다. 이유:

1. **설계 문서의 명시적 API**: GUI 진입점이 `constructGraphOnly()`를 통해 세션 없이 그래프 객체를 받는 경로가 설계에 확정됨.
2. **단순 위임**: 내부 `constructGraph()`의 public 노출에 불과. 1줄 메서드.
3. **와일드카드 노출 인지**: 반환 타입이 `BuildResult<?, ?>`이므로 GUI 진입점에서 unchecked cast가 발생한다는 설계 문서의 경고를 코드 레벨에서도 확인할 수 있음.

### 사람이 고민할 포인트

`constructGraphOnly()`가 없으면 GUI 동기 모드를 구현할 때 `BuildResult`를 외부에 노출하는 방법을 다시 설계해야 한다. 현재 이 메서드는 코드에 존재하지만 호출자가 없으므로, IDE의 "unused" 경고가 발생할 수 있다. `@SuppressWarnings("unused")` 또는 Javadoc으로 용도를 명시하는 것이 좋다.

---

## 6. Main의 커맨드라인 인자 파싱 — 라이브러리 없이 직접 구현

### 설계

```
--mode    interactive (기본) / realtime
--source  host:port
--ui      console (기본) / gui
```

### 구현에서의 간극

설계 문서는 `args.getOrDefault("--mode", "interactive")` 같은 의사 코드를 사용했는데, Java의 `String[] args`는 Map이 아니다. Apache Commons CLI나 picocli 같은 라이브러리를 쓸 수 있지만, 과제 의존성을 최소화해야 한다.

### 결정

`Map<String, String>`으로 변환하는 `parseArgs()` 헬퍼를 직접 구현. `--key value` 쌍을 순회하며 매핑.

### 타협

- 플래그 형태(`--verbose` 같은 값 없는 옵션) 미지원 — 현재 필요 없음.
- 잘못된 옵션에 대한 도움말 출력 미구현 — 과제 범위 밖.
- `--ui gui`는 파싱하지만 GUIRenderer가 없어서 실제 분기 미구현 — 설계 문서에서도 "미래"로 분류.

### 사람이 고민할 포인트

- 설계의 Main은 `--mode` 분기가 완성되어 있지만, 초기 구현에서는 콘솔 대화형만 지원했다. `SocketRealtimeSource` 구현 이후 `--mode realtime` 경로가 추가되어 설계와의 정합성이 높아졌다.
- `Integer.parseInt(parts[1])`에서 포트 번호가 유효하지 않으면 `NumberFormatException`이 발생한다. 현재 이 예외는 최외곽 `try-catch`에서 잡히지만, 사용자에게 "포트 번호가 잘못되었다"는 명확한 메시지를 주려면 별도 검증이 필요하다.

---

## 7. SocketRealtimeSource/SimulatedRealtimeSource의 스레드 라이프사이클

### 설계

설계 문서는 "receiverThread"라는 이름으로 소켓 읽기 스레드를 언급하지만, `Thread` 객체의 명시적 관리(이름 부여, 데몬 설정 등)에 대해서는 기술하지 않는다.

### 구현에서의 간극

`new Thread(() -> { ... }).start()`로 익명 스레드를 생성하면 디버깅 시 "Thread-0", "Thread-1" 같은 이름만 보인다. 프로덕션에서는 문제지만 과제 수준에서는 과도한 세부사항.

### 결정

현재 구현에서는 이름 없는 익명 스레드를 사용. 필요시 `Thread t = new Thread(runnable, "receiver-thread"); t.setDaemon(true);` 형태로 개선 가능. `SimulatedRealtimeSource`도 동일한 패턴.

### 스레드 참조 미보유 문제 (SimulatedRealtimeSource)

생성한 스레드의 참조를 필드에 저장하지 않으므로, `stop()` 호출 시 스레드를 `interrupt()`할 수 없다. `stopped` 플래그를 `volatile`로 선언하여 다음 루프 반복에서 감지하도록 했으나, `Thread.sleep(intervalMs)` 중에는 **최대 intervalMs만큼 대기 후에야 종료**된다.

스레드 참조가 있다면 `stop()` 시 `thread.interrupt()` → `Thread.sleep()`에서 즉시 `InterruptedException` 발생 → 즉각 종료가 가능하다.

### 사람이 고민할 포인트

- 현재 intervalMs=1000ms(1초)이므로 최악의 경우 1초 지연 후 종료. 사용 시나리오에 따라 수용 가능.
- `SocketRealtimeSource`는 `closeSocket()`이 `readLine()` 블로킹을 `SocketException`으로 깨뜨리므로 즉각 종료가 가능하다. Socket과 Simulated의 종료 메커니즘 비대칭이 존재한다.
- 엄밀하게 구현하려면: `private Thread producerThread;` 필드 추가 → `stream()`에서 저장 → `stop()`에서 `producerThread.interrupt()` 호출.

---

## 8. DataQueue의 용량 초과 정책

### 설계

> bounded queue — 생성 시 capacity를 지정. 용량 초과 시 정책(drop-oldest, drop-newest, 블로킹 등)은 실시간 데이터의 성격에 따라 구현체가 결정한다.

### 구현에서의 간극

`ArrayBlockingQueue`를 사용하면 `put()`이 용량 초과 시 자동으로 블로킹된다. 설계 문서는 "drop-oldest가 일반적"이라고 언급하지만, 현재 구현은 **블로킹** 정책을 채택했다.

### 근거

1. `ArrayBlockingQueue.put()`의 블로킹이 가장 단순하고 데이터 유실이 없다.
2. 소비 스레드(`RealtimeGraphSession`)가 배치 소비(`tryDequeue()` 반복)로 빠르게 큐를 비우므로 실질적으로 용량 초과가 발생하기 어렵다.
3. drop-oldest를 구현하려면 `ReentrantLock` + 수동 큐 관리가 필요 — 복잡성 대비 이득이 현재 규모에서 작다.

### 잠재적 데드락 위험 (감사에서 추가 발견)

**블로킹 정책의 위험**: producer(`SimulatedRealtimeSource`)가 `queue.put()`에서 블로킹되면, `stop()`을 호출해도 producer 스레드가 깨어나지 않을 수 있다. `stop()`은 `stopped = true`를 설정하고 `queue.close()`로 poison을 넣으려 하지만, 큐가 꽉 차있으면 `offer(POISON)`이 실패한다.

**완화 요인**: `capacity + 1`로 poison 슬롯을 확보했고, consumer가 지속적으로 소비하므로 큐가 꽉 찬 채로 오래 유지되지 않는다. SimulatedRealtimeSource의 intervalMs(1초) 덕분에 실질적 데드락 가능성은 매우 낮다.

### 사람이 고민할 포인트

- **엄밀한 해결**: `offer(data, timeout, unit)` + drop-oldest, 또는 producer 스레드 참조를 저장하여 `stop()` 시 `thread.interrupt()` 호출.
- 실시간 데이터 스트림에서 "오래된 데이터를 버리는 것"과 "모든 데이터를 보존하는 것" 중 어느 쪽이 올바른 선택인지는 비즈니스 요구사항에 의존한다. 그래프 시각화에서는 최신 데이터만 중요하므로 drop-oldest가 더 적합할 수 있다.

---

## 9. ObserverSupport의 resume() — dirty 플래그의 미묘함

### 설계

```
resume(): suspended=false. dirty면 notifyObservers() 1회. dirty 아니면 무호출
```

### 구현에서 발견한 점

`resume()` 내부에서 `dirty`를 먼저 false로 리셋한 뒤 `notifyObservers()`를 호출해야 한다. 순서가 반대면:

```java
// 잘못된 순서
public void resume() {
    suspended = false;
    if (dirty) {
        notifyObservers();  // 내부에서 다시 dirty를 체크하지만 suspended=false이므로 직접 통보
        dirty = false;      // 이미 통보됨 — 문제없어 보이지만...
    }
}
```

```java
// 올바른 순서 (현재 구현)
public void resume() {
    suspended = false;
    if (dirty) {
        dirty = false;
        notifyObservers();  // dirty=false 상태에서 호출 → 재진입 시에도 안전
    }
}
```

차이: `notifyObservers()` → `onDataChanged()` → `draw()` 경로에서 만약 draw 중에 또 다른 append가 발생하면(현재 구조에서는 불가하지만 방어적으로), dirty 플래그 순서에 따라 무한 루프가 발생할 수 있다. 현재 구현은 `dirty = false`를 먼저 수행하여 안전.

### 교훈

설계 문서의 "dirty면 notifyObservers() 1회"라는 한 줄이 실제 코드에서는 필드 리셋 순서라는 세부사항을 내포한다. 설계는 **의도**를 기술하고, 구현은 **순서**까지 결정해야 한다.

---

## 10. GraphSession의 switch vs if-else — Java 버전 의존성

### 설계

설계 문서는 명령어 처리를 `"add" / "view" / "exit" / 기타` 분기로 기술하되, 구체적 구문을 지정하지 않는다.

### 구현에서의 간극

Java 14+의 향상된 `switch` 표현식(`case "add" ->`)을 사용하면 깔끔하지만, Java 8/11 환경에서는 컴파일 에러가 발생한다. 현재 구현은 Java 14+ `switch` 표현식을 사용했다.

### 결정

과제 제출 환경이 Java 14+를 지원한다고 가정. 만약 Java 11 이하를 사용해야 하면 `if-else if` 체인으로 교체가 필요하다.

---

## 11. BarGraphDrawer의 axisMapping 무시 — 파라미터는 받지만 사용하지 않는 문제

### 설계

> BarGraphDrawer는 axisMapping을 무시하고 direction으로만 렌더링한다.

### 구현에서의 간극

`drawPlot(BarGraphData data, int[] axisMapping)`에서 `axisMapping` 파라미터를 완전히 무시한다. 이는 `IAxisPlot<T>` 인터페이스의 시그니처를 준수하기 위함이지만, 호출자가 "axisMapping을 전달했으니 반영될 것"이라고 오해할 여지가 있다.

### 설계의 의도

이것은 의도된 트레이드오프다:
- `AbstractAxisGraph`에서 `drawBody()`를 공통화하려면 `drawer.drawPlot(data, axes)` 호출이 모든 축 있는 그래프에서 동일해야 한다.
- BarGraph만을 위해 `drawBody()`를 오버라이드하면 공통화의 이점이 사라진다.
- 대신 `getAvailableViews()`가 빈 목록을 반환하여 사용자가 view 명령어로 axisMapping을 바꾸는 경로 자체를 차단한다.

### 교훈

인터페이스 시그니처의 통일성과 구현체의 현실 사이에 갭이 존재할 때, 설계 문서는 "무시한다"고 한 줄로 정리하지만 구현자는 "정말로 무시해도 안전한 모든 경로를 검증"해야 한다. `getAvailableViews()` 오버라이드가 그 안전 장치.

---

## 12. handler의 System.out.println() — 설계 원칙과 현실

### 설계

> 안내 메시지 출력은 handler 내부에서 System.out.println()으로 직접 하지 않고, IInputSource 구현체에 위임하는 것이 원칙이다.

### 구현의 현실

`BarConsoleInputHandler`와 `ScatterConsoleInputHandler` 내부에 `System.out.println("최소 1건의 데이터가 필요합니다.")` 같은 직접 출력이 존재한다. `source.readObject(prompt)` 오버로드로 안내를 위임하는 것이 원칙이지만, 에러 메시지(입력 검증 실패)는 프롬프트가 아니라 **반응**이므로 위임 대상이 아니다.

### 타협

- **프롬프트**(입력 요청): `source.readObject("몇 차원 데이터입니까? ")` — IInputSource에 위임 완료.
- **에러 안내**(입력 검증 실패): `System.out.println("정수를 입력하세요.")` — 직접 출력 유지.

### 사람이 고민할 포인트

- 설계 원칙을 완벽히 따르려면 에러 메시지도 위임해야 하지만, 현재 `IInputSource`에는 "출력 전용" 메서드가 없다. `readObject(prompt)`는 "출력 후 입력"이지 "출력만"은 아니다.
- 해결 방법: (A) `IInputSource`에 `void display(String message)` 메서드 추가, (B) 별도 `IOutputSink` 인터페이스 도입, (C) 현행 유지 후 GUI 전환 시 handler 이름에서 "Console" 접두어를 유지하며 콘솔 전용으로 한정.
- 설계 문서도 이 문제를 인식하고 있다: "handler 이름의 'Console' 접두어는 최초 구현 환경을 반영한 것이며, handler가 진정으로 UI 무관하다면 GUI 도입 시 이름 변경을 고려한다."

---

## 13. GraphFactory의 static 메서드 — 테스트 가능성 vs 단순성

### 설계

> GraphFactory — static 유틸리티 클래스. 인스턴스 생성 불필요.

### 구현에서의 고려

static 메서드는 mock/spy가 어렵다. 단위 테스트에서 `GraphFactory.createScatter()`를 대체하려면 Mockito의 `mockStatic()`이 필요하다.

### 결정

설계 문서의 결정을 따라 static 유지. 이유:
1. 팩토리 내부 로직이 순수 조립(new 호출 + 주입)이므로 테스트에서 대체할 필요가 거의 없다.
2. `GraphDirector` 테스트는 `ITypeSelector`를 mock하면 충분하다.
3. 인스턴스 팩토리로 전환하면 GraphDirector 생성자에 파라미터가 하나 더 늘어나서 Main이 복잡해진다.

### ScaleCalculator 공유 — 인스턴스 주입의 의미

`GraphFactory.createScatter()`에서 매번 `new ScaleCalculator()`를 생성한다. `ScaleCalculator`가 상태를 갖지 않는 순수 유틸리티라면 싱글턴이나 static 메서드로 충분하지만, 설계는 인스턴스를 Drawer에 주입하는 방식을 택했다. 이는 향후 "다른 스케일 알고리즘을 주입"할 가능성을 열어둔 것이다.

설계는 "range 계산이 drawAxis()와 drawPlot()에서 각각 수행되어 2회 중복"됨을 인지하고 있다. 캐싱이 필요해지면 Drawer 내부에서 처리하거나 `drawBody()`에서 range를 선계산하는 최적화를 고려한다.

---

## 14. RealtimeDemo의 타입 불일치 — 설계 경로와 다른 수동 조립

### 설계

실시간 세션은 `GraphDirector.constructRealtimeSession()`으로 조립하며, 타입 안전성은 `assembleRealtimeSession()`의 제네릭 캡처로 보장된다.

### 구현의 RealtimeDemo

`RealtimeDemo`는 테스트 편의를 위해 `GraphDirector`를 거치지 않고 **직접 조립**한다. `GraphFactory.createScatter()`로 그래프를 만들고, `DataQueue<Point>`를 직접 생성하고, `SimulatedRealtimeSourceFactory`에서 소스를 만들어 `RealtimeGraphSession`을 직접 구성한다.

### 간극

이 직접 조립 경로에서는 `assembleRealtimeSession()`의 제네릭 캡처가 없으므로, 프로그래머가 타입 일치를 수동으로 보장해야 한다. 예: `DataQueue<Point>`에 `IRealtimeSource<Pair<String, Double>>`를 연결하면 런타임에야 문제가 드러난다.

### 근거

`RealtimeDemo`는 테스트/시연 코드이므로 "설계의 안전한 조립 경로"를 거치지 않아도 된다. 프로덕션 코드(Main)는 반드시 `GraphDirector`를 통해야 한다는 것이 설계의 의도.

### 사람이 고민할 포인트

- Director를 경유하는 통합 테스트를 별도로 작성해야 한다. 하지만 Director 경유 시 콘솔 입력이 필요하므로 자동화된 테스트가 어렵다 (stdin 모킹 필요).
- `IInputSource`를 mock으로 구현하여 미리 정의된 응답을 반환하는 `MockInputSource`를 만들면 Director 통합 테스트가 가능하다.
- `GraphDirector.constructRealtimeSession()`의 조립 로직이 `RealtimeDemo`에서는 검증되지 않는 사각지대가 된다. Main에서 `--mode realtime` 경로가 추가되어 이 사각지대는 크게 줄었다.

---

## 15. DataQueue — poison pill의 타입 문제

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
}
```

### 간극의 본질

Java 제네릭에서 `BlockingQueue<E>`에 `E`가 아닌 객체를 넣을 수 없다. poison pill은 "데이터가 아닌 종료 신호"이므로 본질적으로 `E` 타입이 아니다. 해결 방법은 세 가지:

| 방법 | 장단점 |
|------|--------|
| **A. `BlockingQueue<Object>` 사용 (현재 구현)** | poison과 데이터를 혼재. `@SuppressWarnings("unchecked")` 필요. 단순하고 동작 확실 |
| **B. `Optional<E>` 래핑** | `BlockingQueue<Optional<E>>` — `Optional.empty()`가 poison. 타입 안전하지만 모든 enqueue에 래핑 오버헤드 |
| **C. `closed` 플래그만 사용** | poison 없이 `closed + notify`로 종료. 코드가 복잡 |

### 사람이 고민할 포인트

- 설계는 C++ 전환도 고려하는데, C++에서는 `std::optional<T>`로 poison pill을 자연스럽게 표현할 수 있다 (설계 문서의 C++ DataQueue 골격 참조). Java에서는 방법 A가 가장 실용적.
- `dequeue()`에서 poison을 꺼낸 뒤 **다시 poison을 삽입**하는 이유: 다중 소비자 시나리오에서 하나의 소비자가 poison을 꺼내면 다른 소비자는 종료 신호를 받지 못한다. 현재는 소비자가 1개뿐이지만, 확장성을 위해 재삽입하는 것이 방어적 프로그래밍이다.
- `@SuppressWarnings("unchecked")`가 `dequeue()`와 `tryDequeue()` 두 곳에 붙는다. 이 캐스팅이 안전한 이유는 POISON이 `null`을 반환하는 분기에서 걸러지기 때문인데, 이 불변식이 깨지면 `ClassCastException`이 발생한다.

---

## 16. RealtimeGraphSession — 종료 경로 A에서 onStop 미호출

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

**현재 동작에 문제가 없는 이유**: `SimulatedRealtimeSource`/`SocketRealtimeSource`는 `stream()` 메서드의 `finally` 블록에서 이미 `queue.close()`를 호출한다. `onStop`은 `() -> source.stop()`인데, `source.stop()`은 `stopped = true; queue.close()`를 수행할 뿐이다. 이미 종료된 소스에 대해 멱등 호출이므로 결과는 동일하다.

**문제가 될 수 있는 경우**: 향후 `onStop` 콜백에 추가 정리 로직(로그 기록, 리소스 해제 등)이 포함되면, 경로 A에서 이 로직이 실행되지 않는다.

### 사람이 고민할 포인트

- 설계에 맞추려면 `run()` 메서드의 루프 탈출 후 `onStop.run()`을 추가해야 한다. 멱등성이 보장되므로 경로 B에서 중복 호출되어도 안전하다.
- 또는 `run()` 전체를 `try-finally`로 감싸서 `finally { onStop.run(); }`으로 처리하면 정상/예외 모두 커버 가능.
- 심각도: **낮음** — 현재 동작에 실질적 문제 없음. 향후 리팩토링 시 수정 권장.

---

## 17. ScatterConsoleInputHandler.readMetadata() — n차원 레이블 미지원

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

`ScatterPlotDrawer.getLabelForAxis()`에서 dim≥3인 축에 대해 `"축" + axisIndex` 폴백을 제공:

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

## 18. IInputSource.readObject() — Object 반환 타입과 캐스팅 전파

### 설계

```
IInputSource
+ readObject() : Object
+ readObject(prompt : String) : Object
+ close() : void
```

> readObject()의 반환 타입이 Object인 이유: 향후 제네릭화(IInputSource\<T\>)로 String과 바이너리를 모두 수용할 수 있다. 현재는 Object로 두되, 캐스팅 지점이 명령어 루프 1곳으로 한정.

### 구현

설계를 그대로 따랐다. 모든 호출 지점에서 `(String) source.readObject(prompt)` 캐스팅이 발생한다.

### 사람이 고민할 포인트

- 현재 모든 사용처가 `(String)` 캐스팅을 수행한다. `IInputSource`를 `IInputSource<String>`으로 제네릭화하면 캐스팅이 완전히 제거되지만, `GraphDirector`와 `IGraphDataInputHandler`의 시그니처까지 전파된다.
- 설계는 "향후 바이너리 입력"을 위해 `Object`를 유지하지만, 실제로 바이너리 입력이 필요해질 때 `IInputSource<T>`로 전환하면 충분하다. 현시점에서 `String`으로 고정하는 것도 합리적 선택이다 (YAGNI).
- `readObject()`가 `null`을 반환할 수 있다 (`Scanner.hasNextLine() == false`일 때). `GraphSession.run()`에서 `null` 체크 → break로 처리하고 있지만, handler 내부(`readData`, `readMetadata`)에서는 NPE 위험이 잔존한다. 이는 "입력 스트림이 중간에 끊기는 시나리오"를 handler가 고려하지 않기 때문이다.

---

## 19. GraphSession — stop()과 close()의 멱등성, System.in 블로킹

### 설계

```
GraphSession.stop() : source.close() 후 루프 종료
GraphSession.run() finally → source.close()
```

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
- `System.in` 기반 `Scanner`는 `Thread.interrupt()`로 중단되지 않는다 (Java의 알려진 한계). 외부 스레드에서 `stop()`을 호출해도 `readObject()`의 `scanner.nextLine()` 블로킹이 즉시 해제되지 않는다. **`exit` 명령어로만 정상 종료 가능**하다.

### 사람이 고민할 포인트

- 설계는 "stop() 시 블로킹 해제를 위해 ConsoleInput은 내부적으로 입력 스레드를 분리할 수 있다"고 언급한다. 이를 구현하려면 `ConsoleInput`을 크게 리팩토링해야 한다.
- 현재 콘솔 대화형에서는 `exit` 명령이 유일한 종료 경로이므로 실질적 문제는 없다. GUI 전환 시 `GUIInput`에서는 이 문제가 자연스럽게 해결된다 (대화 상자 닫기 → BlockingQueue 닫기).

---

## 20. ObserverSupport — 스레드 안전성의 암묵적 계약

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
- 성능 관점에서 `synchronized`는 현재 단일 스레드 접근에서 무의미한 오버헤드이므로, 문서화(Javadoc)가 더 적절한 방어 수단이다.

---

## 21. Pair 클래스 — equals/hashCode 미구현

### 설계

> Pair\<String, Double\> — javafx.util.Pair 명시적 배제

### 구현

`makeagraph.util.Pair<A, B>` 커스텀 클래스를 생성. `getFirst()`, `getSecond()` 두 메서드만 제공.

### 간극

`equals()`, `hashCode()`, `toString()`이 구현되어 있지 않다. Java 표준 라이브러리에 범용 Pair가 없어서 커스텀 구현이 필요했지만, 최소한의 기능만 구현했다.

### 사람이 고민할 포인트

- 현재 `Pair`는 `BarGraphData`의 데이터 엘리먼트로 사용된다. `HashMap`이나 `HashSet`의 키로 사용하거나 중복 검사가 필요한 경우 `equals()`/`hashCode()` 미구현이 버그로 이어진다.
- 현재 코드에서는 `Pair`를 리스트에 담아 순회하며 `getFirst()`/`getSecond()`로 접근만 하므로 실질적 문제는 없다. 하지만 방어적으로 구현해두는 것이 좋다.
- Java 16+에서는 `record Pair<A, B>(A first, B second) {}`로 한 줄에 `equals`/`hashCode`/`toString`이 자동 생성된다.

---

## 22. BarGraphDrawer — HORIZONTAL 모드의 레이블 의미론

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

### 간극

사용자가 `readMetadata()`에서 "X축 레이블: 성적", "Y축 레이블: 학생 수"를 입력하고 direction=HORIZONTAL을 선택하면, Y축에 카테고리(성적)가 표시되고 X축에 값(학생 수)이 표시된다. 하지만 레이블은 여전히 metadata에 입력된 그대로 출력된다 — "Y (학생 수)", "X (성적)".

HORIZONTAL에서는 Y가 카테고리, X가 값이므로 **레이블이 의미론적으로 뒤바뀌어야 할 수 있다**.

### 사람이 고민할 포인트

- 이것은 "direction에 따라 metadata의 xLabel/yLabel을 교환해야 하는가"라는 의미론적 질문이다. 설계는 이 부분을 명시하지 않는다.
- 해결 방법: (A) `drawAxis()`에서 direction이 HORIZONTAL이면 xLabel/yLabel을 교환, (B) `readMetadata()`에서 direction을 먼저 물어보고 레이블 프롬프트를 조정, (C) 현행 유지 — 사용자가 방향에 맞게 레이블을 입력한다고 가정.
- 현재 구현은 (C)를 택했다. 사용자 경험 관점에서는 (B)가 가장 친절하다.

---

## 23. GraphSession.handleView() — 현재 활성 뷰 표시 없음

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

- `IViewControllable`에 `getAxes() : int[]`를 추가할지 여부. 현재 인터페이스는 "변경" 메서드만 제공하고 "조회"는 제공하지 않는다. ISP 관점에서 별도 `IViewQueryable` 인터페이스로 분리할 수도 있다.
- 또한 view 목록 형식이 설계의 "x - y" 대신 "축0 - 축1"로 표시된다. `getLabelForAxis()`를 활용하면 "xLabel - yLabel" 형태로 개선 가능하지만, `GraphSession`이 `ScatterPlotDrawer`의 private 메서드에 접근할 수 없으므로 레이블 접근 경로를 별도로 마련해야 한다.

---

## 24. 감사에서 발견된 버그 2건 — 설계가 예측하지 못한 구현 결함

### BUG-1 [CRITICAL]: RealtimeGraphSession.run() — resumeObservers() 누락 경로

**수정 전**:

```java
try {
    appendable.suspendObservers();
    appendable.appendData(data);
    // ... 배치 소비 ...
    appendable.resumeObservers();  // ← catch로 빠지면 실행되지 않음
} catch (Exception e) {
    System.err.println("실시간 처리 오류: " + e.getMessage());
    onStop.run();
}
```

**문제**: 배치 처리 중 예외가 발생하면 `resumeObservers()`가 호출되지 않아 옵저버가 **영구 일시중지** 상태에 빠진다. 이후 모든 데이터 추가가 무시된다.

**수정 후**:

```java
try {
    appendable.suspendObservers();
    appendable.appendData(data);
    // ... 배치 소비 ...
} catch (Exception e) {
    System.err.println("실시간 처리 오류: " + e.getMessage());
    onStop.run();
} finally {
    appendable.resumeObservers();  // ← 정상/예외 모두 실행
}
```

**설계와의 관계**: 설계 문서는 "suspend → 배치 처리 → resume" 흐름을 기술하지만, 예외 경로에서의 resume 보장을 명시하지 않았다. `try-finally`는 구현 수준의 방어적 결정이다.

### BUG-2 [MEDIUM]: ScatterPlotDrawer.drawPlot() — grid 경계 검사 누락

**수정 전**:

```java
int col = scaleCalc.toCol(xValues.get(i), xRange.getMin(), xRange.getMax(), gridWidth);
int row = scaleCalc.toRow(yValues.get(i), yRange.getMin(), yRange.getMax(), gridHeight);
grid[row][col] = '*';  // ← 경계 밖이면 ArrayIndexOutOfBoundsException
```

**수정 후**:

```java
if (row >= 0 && row < gridHeight && col >= 0 && col < gridWidth) {
    grid[row][col] = '*';
}
```

**문제 원인**: `toCol()`/`toRow()`의 부동소수점 연산에서 경계값이 정확히 gridWidth 또는 gridHeight로 반환될 수 있다. `BarGraphDrawer`는 이미 경계 검사를 포함하고 있었지만 `ScatterPlotDrawer`에는 누락되어 있었다.

**설계와의 관계**: 설계 문서는 `toCol()`/`toRow()`의 반환 범위를 "0 ≤ x < grid 크기"로 가정하지만, 부동소수점 정밀도를 고려한 경계 검사를 명시하지 않았다. 이 역시 구현 수준의 방어적 결정이다.

---

## 25. GraphDirector.construct() — 와일드카드 캡처 헬퍼 추가

### 설계

```java
ISession construct(IInputSource source, IRenderer renderer) {
    BuildResult<?, ?> result = constructGraph(source, renderer);
    return new GraphSession<>(source, result.graph(), result.appendable(), result.handler());
}
```

설계 문서는 `assembleRealtimeSession()`에 대해서만 "private 제네릭 헬퍼로 와일드카드 캡처"를 명시하고, 대화형 경로(`construct()`)에서는 직접 `new GraphSession<>()`을 호출하는 것으로 기술한다.

### 구현

```java
public ISession construct(IInputSource source, IRenderer renderer) {
    return assembleSession(constructGraph(source, renderer), source);
}
private <T, E> ISession assembleSession(BuildResult<T, E> result, IInputSource source) {
    return new GraphSession<>(source, result.graph(), result.appendable(), result.handler());
}
```

### 간극

구현은 `assembleSession()`이라는 **private 제네릭 헬퍼**를 추가하여 `BuildResult<?, ?>`의 와일드카드를 `<T, E>`로 캡처한다. 설계의 의사 코드를 그대로 Java로 옮기면, `GraphSession` 생성자에 전달되는 `result.graph()`(IGraph\<?\>)와 `result.handler()`(IGraphDataInputHandler\<?, ?\>)의 와일드카드가 독립 캡처되어 T, E 일치가 컴파일 타임에 보장되지 않는다.

구현은 `assembleRealtimeSession()`과 동일한 기법을 대화형 경로에도 일관되게 적용한 것으로, 설계의 의도를 더 정확하게 Java로 실현한 결과이다. 행동적 차이는 없다.

### 사람이 고민할 포인트

- 설계 문서에서 대화형 경로의 `construct()`를 직접 `new GraphSession<>()`으로 기술한 것은 **의사 코드 수준의 단순화**이다. 실제 Java에서는 와일드카드 캡처가 필요하며, 이는 설계 문서가 이미 실시간 경로에서 설명한 패턴과 동일하다.
- `assembleSession()`이라는 헬퍼 이름은 `assembleRealtimeSession()`과 대칭을 이루어 코드 가독성을 높인다.

---

## 26. 설계에 명시되었으나 의도적으로 미구현한 항목 (업데이트)

| 항목 | 설계 상태 | 구현 상태 | 비고 |
|------|----------|-----------|------|
| PieChart + PieChartData + PieChartDrawer | "미래" 명시 | 미구현 | 현재 요구사항에 없음 |
| IPieDrawer | "미래" 명시 | 미구현 | PieChart와 함께 추가 |
| FileInput | "미래" 명시 | 미구현 | 현재 콘솔 입력만 필요 |
| GUIInput / GUIRenderer / GUITypeSelector | "미래" 명시 | 미구현 | GUI 도입 시점에 추가 |
| GraphStyle 계층 | "GUI 도입 시 추가 예정" | 미구현 | 콘솔에서 스타일 의미 없음 |
| LineGraph | "미래" 명시 | 미구현 | 현재 요구사항에 없음 |
| `--ui gui` 분기 | 설계에 포함 | 파싱만 구현, 분기 미구현 | GUIRenderer 없이 분기 불가 |
| ~~SocketRealtimeSource~~ | 설계 포함 | **구현 완료** | host/port 분리 방식 (§1 참조) |
| ~~SocketRealtimeSourceFactory~~ | 설계 포함 | **구현 완료** | |
| ~~IPlot\<T\>~~ | 설계 포함 | **구현 완료** | 구현체 없이 인터페이스만 (§4 참조) |
| ~~constructGraphOnly()~~ | 설계 포함 | **구현 완료** | 호출자 없음 (§5 참조) |
| ~~Main --mode 분기~~ | 설계 포함 | **구현 완료** | interactive + realtime 지원 (§6 참조) |

> ~~취소선~~ 항목은 이전에 미구현이었으나 현재 구현 완료된 것들이다.

---

## 26. 설계에 명시되었으나 구현에서 달라진 세부 사항

| 항목 | 설계 | 구현 | 비고 |
|------|------|------|------|
| `AbstractGraph.draw()` 접근 제어자 | "final 권장" | `public final void draw()` | 설계 의도 준수 |
| `ViewController.setView()` 유효성 검증 | "axes.length != 2이면 예외" | 별도 검증 없음 | 잘못된 길이 배열 시 AIOOBE 발생 가능 |
| `GraphSession.run()` 전체 구조 | try-catch-finally 3단 | try-catch-finally 구현 | 설계 일치 |
| `ISession.start()` 논블로킹 계약 | "항상 논블로킹" | 새 Thread 생성 후 즉시 반환 | 설계 일치 |
| `ScatterPlotDrawer.drawPlot()` 경계 검사 | 설계 코드에 없음 | 경계 검사 추가 | 방어적 코딩. 설계보다 안전 (§24 BUG-2) |
| view 목록 형식 | "x - y" 레이블 | "축0 - 축1" 인덱스 | getLabelForAxis() 활용 시 개선 가능 (§23) |
| view 목록의 [현재] 표시 | 설계에 명시 | 미구현 | §23 참조 |
| `DataQueue` 내부 타입 | `BlockingQueue<E>` | `BlockingQueue<Object>` | poison pill 타입 문제 (§15) |
| `SocketRealtimeSource` 생성자 | `(address)` 단일 문자열 | `(host, port)` 분리 | fail-fast 파싱 (§1) |

---

## 전체 요약: 설계와 구현의 본질적 차이

| 설계가 다루는 것 | 구현이 추가로 결정해야 하는 것 |
|-----------------|---------------------------|
| 클래스 간 관계와 책임 | 필드 초기화 순서, null 방어 |
| 인터페이스 시그니처 | 반환 타입의 불변성(Collections.unmodifiableList 등) |
| 스레드 모델 (2스레드) | 스레드 이름, 데몬 여부, 인터럽트 처리, 참조 보유 여부 |
| "멱등이어야 한다" | `volatile` 키워드, `AtomicBoolean`, `compareAndSet` |
| "블로킹 대기" | `ArrayBlockingQueue` vs `LinkedBlockingQueue`, 용량 정책, 데드락 가능성 |
| "파싱 실패 시 무시" | try-catch 범위, 로그 레벨, 어떤 정보를 출력할지 |
| "address : String" | host/port 분리, 파싱 위치, 유효성 검증 시점 |
| "dirty면 1회 호출" | dirty 리셋 타이밍, 재진입 안전성 |
| "poison pill로 종료" | `BlockingQueue<Object>` vs `Optional<E>`, `@SuppressWarnings` 위치 |
| "suspend → 배치 → resume" | 예외 경로에서의 resume 보장 (try-finally) |
| "타입 선택 → 팩토리" | static 메서드의 테스트 가능성, Pair의 equals/hashCode |

---

## 심각도 분류

### 즉시 수정 완료 (감사에서 발견 → 수정됨)

| # | 항목 | 심각도 | 대응 |
|---|------|--------|------|
| 24-1 | RealtimeGraphSession resumeObservers() 누락 경로 | CRITICAL | `finally` 블록으로 이동하여 수정 완료 |
| 24-2 | ScatterPlotDrawer grid 경계 검사 누락 | MEDIUM | 경계 검사 추가하여 수정 완료 |

### 수정 권장 (설계 위반이나 현재 동작에 실질적 문제 없음)

| # | 항목 | 심각도 | 대응 |
|---|------|--------|------|
| 16 | RealtimeGraphSession 경로 A에서 onStop 미호출 | 낮음 | run()에 finally { onStop.run(); } 추가 권장 |
| 23 | view 목록에 [현재] 표시 없음 | 낮음 (UX) | ViewController.getAxes()와 비교 로직 추가 |
| 21 | Pair의 equals/hashCode 미구현 | 낮음 | 현재 사용 패턴에선 문제 없음. 방어적 추가 권장 |

### 의도적 결정 (설계와 다르지만 합당한 이유 있음)

| # | 항목 |
|---|------|
| 1 | SocketRealtimeSource 생성자 `(host, port)` — 설계의 `(address)` 대신 |
| 2 | SimulatedRealtimeSource 추가 — 설계에 없는 테스트용 클래스 |
| 14 | RealtimeDemo가 GraphDirector 우회 |
| 15 | DataQueue `BlockingQueue<Object>` — 설계의 `BlockingQueue<E>` 대신 |
| 17 | readMetadata()에서 dim=2 고정 — 설계의 n차원 대신 |
| 25 | GraphDirector.construct()에 와일드카드 캡처 헬퍼 추가 — 설계의 직접 호출 대신 |

### 설계 미비 / 열린 문제

| # | 항목 |
|---|------|
| 8 | DataQueue capacity 초과 시 블로킹 정책의 잠재적 데드락 |
| 12 | handler 에러 메시지의 직접 출력 (설계 원칙과 구현 사이의 마찰) |
| 18 | IInputSource.readObject()의 null 반환 — handler 내부 NPE 위험 |
| 19 | ConsoleInput의 System.in 블로킹 해제 불가 |
| 22 | BarGraphDrawer HORIZONTAL 시 레이블 의미론 불일치 |

---

설계 문서가 2200줄이지만 구현에서 추가로 결정해야 할 것들이 최소 이 목록만큼 있었다. 설계는 "무엇을"과 "왜"를 기술하고, 구현은 "어떻게"와 "어떤 순서로"를 결정한다. 그 간극이 바로 엔지니어링의 영역이다.

감사(audit)는 이 간극 중 두 가지를 **실제 버그**로 격상시켰다 — resumeObservers() 누락과 grid 경계 검사. 설계가 아무리 정밀해도, 예외 경로와 부동소수점 정밀도 같은 "구현의 세부사항"은 코드를 실제로 작성하고 검증해야만 발견된다.
