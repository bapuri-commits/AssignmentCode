# MakeAGraph: 설계 → 구현 간극 정리

> v4.9.3 설계 문서를 Java 코드로 옮기면서 마주한 결정, 타협, 발견의 기록.

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

## 4. IPlot<T> — 구현체 없이 인터페이스만 존재하는 상황

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

---

## 7. SocketRealtimeSource의 스레드 이름과 라이프사이클

### 설계

설계 문서는 "receiverThread"라는 이름으로 소켓 읽기 스레드를 언급하지만, `Thread` 객체의 명시적 관리(이름 부여, 데몬 설정 등)에 대해서는 기술하지 않는다.

### 구현에서의 간극

`new Thread(() -> { ... }).start()`로 익명 스레드를 생성하면 디버깅 시 "Thread-0", "Thread-1" 같은 이름만 보인다. 프로덕션에서는 문제지만 과제 수준에서는 과도한 세부사항.

### 결정

현재 구현에서는 이름 없는 익명 스레드를 사용. 필요시 `Thread t = new Thread(runnable, "receiver-thread"); t.setDaemon(true);` 형태로 개선 가능. `SimulatedRealtimeSource`도 동일한 패턴.

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

GUI 전환 시 에러 안내도 IInputSource에 위임하거나, 별도 `IErrorDisplay` 인터페이스를 도입해야 한다. 현재 콘솔 전용이므로 비용 대비 이득이 없어 유보.

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

---

## 15. 설계에 명시되었으나 의도적으로 미구현한 항목

| 항목 | 설계 상태 | 미구현 이유 |
|------|----------|------------|
| PieChart + PieChartData + PieChartDrawer | "미래" 명시 | 현재 요구사항에 없음 |
| IPieDrawer | "미래" 명시 | PieChart와 함께 추가 |
| FileInput | "미래" 명시 | 현재 콘솔 입력만 필요 |
| GUIInput / GUIRenderer / GUITypeSelector | "미래" 명시 | GUI 도입 시점에 추가 |
| GraphStyle 계층 | "GUI 도입 시 추가 예정" | 콘솔에서 스타일 의미 없음 |
| LineGraph | "미래" 명시 | 현재 요구사항에 없음 |
| `--ui gui` 분기 | 설계에 포함 | GUIRenderer 없이 분기 불가 |

---

## 요약: 설계와 구현의 본질적 차이

| 설계가 다루는 것 | 구현이 추가로 결정해야 하는 것 |
|-----------------|---------------------------|
| 클래스 간 관계와 책임 | 필드 초기화 순서, null 방어 |
| 인터페이스 시그니처 | 반환 타입의 불변성(Collections.unmodifiableList 등) |
| 스레드 모델 (2스레드) | 스레드 이름, 데몬 여부, 인터럽트 처리 |
| "멱등이어야 한다" | `volatile` 키워드, `AtomicBoolean`, `compareAndSet` |
| "블로킹 대기" | `ArrayBlockingQueue` vs `LinkedBlockingQueue`, 용량 정책 |
| "파싱 실패 시 무시" | try-catch 범위, 로그 레벨, 어떤 정보를 출력할지 |
| "address : String" | host/port 분리, 파싱 위치, 유효성 검증 시점 |
| "dirty면 1회 호출" | dirty 리셋 타이밍, 재진입 안전성 |

설계 문서가 2200줄이지만 구현에서 추가로 결정해야 할 것들이 최소 이 목록만큼 있었다. 설계는 "무엇을"과 "왜"를 기술하고, 구현은 "어떻게"와 "어떤 순서로"를 결정한다. 그 간극이 바로 엔지니어링의 영역이다.
