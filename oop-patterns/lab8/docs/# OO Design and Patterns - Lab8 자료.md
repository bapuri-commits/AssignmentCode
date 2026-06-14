# OO Design and Patterns - Lab 자료 (8)

---

# Laboratory 08 | Exercise Patterns

**Lab 8: 책임 패턴(2)**

성명: ________________

학번: ________________

목차
- InLab #1. Proxy 패턴의 적용
- InLab #2. Chain-of-Responsibility 패턴의 적용
- InLab #3. Flyweight 패턴

Version 2.0

---

## InLab #1: Proxy 패턴

### 1-1. 열차 시간표를 알리기 위한 웹사이트를 개발하고 있다. 새 시간표를 얻기 위해서는 타사 서비스 즉 오래되고 매우 느린 레거시 앱이며 논리를 변경할 수 없는 오래된 시스템을 접근하여야 한다. 업데이트된 시간표가 있는 디스크는 매일 정해진 시간에 삽입된다.

**(1) 새 시간표에 대한 데이터를 얻기 위하여 어떻게 하는 것이 효과적인가?**

> _(작성란)_

---

**(2) 패턴을 적용하지 않고 다음과 같이 설계하였을 때 UML 클래스 다이어그램을 그려라.**

1. TrainTimetable – 열차 시간표를 가져오는 인터페이스
2. TimetableElectricTrain - TrainTimetable 인터페이스를 구현한 클래스.
3. DisplayTimeTable – 파일 시스템과 상호 작용하는 클라이언트 코드로 printTimetable() 메서드는 ElectricTrainTimetable 클래스의 메서드를 사용한다

> _(작성란)_

---

**(3) 매번 ElectricTrainTimetable 클래스가 디스크에 접근하는 경우 매우 느리다. 따라서 캐싱 메커니즘을 추가하여 시스템의 성능을 개선하기 위하여 프록시 패턴을 적용하여 수정하라.**

> _(작성란)_

---

**(4) 다음과 같이 Proxy 클래스를 코딩하였다. 무엇이 문제인지 설명하고 문제를 수정한 코드를 작성하라.**

```java
public class ElectricTrainTimetableProxy implements TrainTimetable {
    // Reference to the original object
    private TrainTimetable trainTimetable = new ElectricTrainTimetable();

    private String[] timetableCache = null

    @Override
    public String[] getTimetable() {
        return trainTimetable.getTimetable();
    }

    @Override
    public String getTrainDepartureTime(String trainId) {
        return trainTimetable.getTrainDepartureTime(trainId);
    }

    public void clearCache() {
        trainTimetable = null;
    }
}
```

> _(작성란)_

---

## InLab #2: Chain-of-Responsibility 패턴

### 2-1. ATM 기에서 현금을 인출하기 위한 기능을 구현하고 있다. 다음과 같은 PaperCurrency 클래스가 인출 총액을 입력받은 후 지폐를 내 주기 위한 디스펜서를 다음과 같은 형태로 만들려고 한다.

- 지폐의 단위에 따라 다른 디스펜서가 처리한다.
- 더 작은 단위의 지폐가 필요하다면 다음 차례의 디스펜서로 제어권을 넘긴다.

**(1) 책임체인 패턴을 적용하여 5 만원권, 만원권, 오천원권, 천원권을 인출하기 위한 디스펜서를 설계하라.**

```java
package org.trishinfotech.responsibility;
public class PaperCurrency {
    protected int amount;
    public PaperCurrency(int amount) {
        super();
        this.amount = amount;
    }
    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
}
```

> _(작성란)_

---

**(2) ATM 에서 5 만원권을 처리하는 `FiftyThousandDispenser` 를 코딩하라.**

> _(작성란)_

---

### 2-2. OS 의 파일 관리 시스템을 설계한다고 하자. 파일과 디렉토리를 합하여 엔트리로 다루는 프로그램을 설계하고 코딩하라. 디렉토리 엔트리를 생성하고, 이름을 받고(getName), 크기를 알아내고(getSize), 내용물의 목록을 인쇄하는(printList) 기능을 포함시켜야 한다.

**\<UML 클래스 다이어그램\>**

> _(작성란)_

---

Main 프로그램은 아래와 같다. 나머지 필요한 프로그램을 작성하라.

**\<main.java\>**

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Making root entries...");
        Directory rootdir = new Directory("root");
        Directory bindir = new Directory("bin");
        Directory tmpdir = new Directory("tmp");
        Directory usrdir = new Directory("usr");
        rootdir.add(bindir);
        rootdir.add(tmpdir);
        rootdir.add(usrdir);
        bindir.add(new File("vi", 10000));
        bindir.add(new File("latex", 20000));
        rootdir.printList();
        System.out.println();

        System.out.println("Making user entries...");
        Directory emchoi = new Directory("emchoi");
        Directory gildong = new Directory("gildong");
        Directory dongguk = new Directory("dongguk");
        usrdir.add(youngjin);
        usrdir.add(gildong);
        usrdir.add(dojun);
        emchoi.add(new File("diary.html", 100));
        emchoi.add(new File("Composite.java", 200));
        gildong.add(new File("memo.tex", 300));
        dongguk.add(new File("game.doc", 400));
        dongguk.add(new File("junk.mail", 500));
        rootdir.printList();
    }
}
```

> _(작성란)_

---

## InLab #3: Bridge 패턴

### 3-1. 다음과 같은 요구 사항으로 화면에 출력하는 프로그램을 브리지 패턴을 이용하여 만들어라.

- 홀수값 만을 저장하는 EvenStack 과 OddStack 두 가지가 필요하다.
- 구현 방법은 Array 와 Linked 두 가지 방법을 사용한다.
- 스택에는 push, pop, top, IsEmpty, IsFull, printStack 메소드가 있어야 한다.

**(1) 브리지 패턴을 이용하여 설계하라(클래스 다이어그램으로 나타낼 것).**

> _(작성란)_

---

**(2) 브리지 패턴의 각 구성요소를 코딩해 보라.**

> _(작성란)_

---

## PostLab #8: 회고 보고서

Lab #8 의 주제는 책임 패턴(2)이었습니다. 프록시, 책임체인, 브리지 패턴의 차이를 알고 코딩으로 체험하기 위한 목적이 강의와 액티비티를 통하여 얼마나 잘 이해되었는지 또한 무엇을 배웠는지 자신의 언어로 정리해 봅시다.

> _(작성란)_