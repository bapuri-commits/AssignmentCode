# Prompt 03 — 그리기(Drawer) · ViewController 계층

> 이 프롬프트는 MakeAGraph 프로젝트의 **3단계: 그리기 로직 + 뷰 제어** 구현을 요청합니다.
> **1~2단계(데이터/유틸 + 인터페이스) 코드가 이미 완성된 상태**에서 진행합니다.

---

## 사전 컨텍스트

1~2단계에서 생성된 파일들을 컨텍스트로 첨부하세요. 특히:
- `data/ScatterPlotData.java`, `data/BarGraphData.java`, `data/GraphMetadata.java`, `data/Point.java`
- `util/ScaleCalculator.java`, `util/Range.java`, `util/Direction.java`, `util/Pair.java`
- `drawer/ITitle.java`, `drawer/IAxis.java`, `drawer/IAxisPlot.java`, `drawer/IScatterDrawer.java`, `drawer/IBarDrawer.java`
- `graph/IViewControllable.java`

---

## 지시

아래 명세에 따라 Drawer 구현체와 ViewController를 구현해 주세요.

패키지 구조:

```
src/main/java/makeagraph/
├── drawer/
│   ├── ScatterPlotDrawer.java
│   └── BarGraphDrawer.java
└── graph/
    └── ViewController.java
```

---

## 구현 대상 (3개 파일)

### 1. `ViewController` — 뷰/축 제어

```java
package makeagraph.graph;

import java.util.List;
import java.util.ArrayList;

public class ViewController {
    private final int dim;          // 생성자에서 주입. 불변
    private int[] axisMapping;      // 항상 길이 2

    public ViewController(int dim) {
        this.dim = dim;
        this.axisMapping = new int[]{0, 1};
    }

    public void swapAxes(int a, int b) {
        // axisMapping에서 a와 b를 교환
    }

    public void setView(int[] axes) {
        // axes 길이가 2가 아니면 예외
        // axisMapping을 axes로 교체
    }

    public int[] getAxes() {
        return axisMapping.clone();  // 방어적 복사
    }

    public List<int[]> getAvailableViews() {
        // dim에서 2개를 골라 순서 쌍(ordered pair)을 생성하여 반환
        // 예: dim=3 → {0,1},{1,0},{0,2},{2,0},{1,2},{2,1} (6개)
        // 예: dim=2 → {0,1},{1,0} (2개)
    }
}
```

- `axisMapping`은 항상 길이 2 — "n차원 데이터에서 어떤 두 축을 2D로 투영할지".
- `getAvailableViews()`는 **순서 쌍** 반환 — `{0,1}`과 `{1,0}`은 다른 뷰.

### 2. `ScatterPlotDrawer` — 산포도 그리기

```java
package makeagraph.drawer;

import makeagraph.data.ScatterPlotData;
import makeagraph.data.GraphMetadata;
import makeagraph.data.Point;
import makeagraph.util.ScaleCalculator;
import makeagraph.util.Range;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ScatterPlotDrawer implements IScatterDrawer {

    private final ScaleCalculator scaleCalc;
    private final int gridWidth;
    private final int gridHeight;

    private static final int DEFAULT_GRID_WIDTH = 60;
    private static final int DEFAULT_GRID_HEIGHT = 20;

    public ScatterPlotDrawer(ScaleCalculator scaleCalc) {
        this(scaleCalc, DEFAULT_GRID_WIDTH, DEFAULT_GRID_HEIGHT);
    }

    public ScatterPlotDrawer(ScaleCalculator scaleCalc, int gridWidth, int gridHeight) {
        this.scaleCalc = scaleCalc;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    @Override
    public List<String> drawTitle(GraphMetadata metadata) {
        // 제목을 중앙 정렬하여 반환
        // 예: "    My Scatter Plot    "
        List<String> lines = new ArrayList<>();
        lines.add(metadata.getTitle());
        lines.add("");  // 빈 줄
        return lines;
    }

    @Override
    public List<String> drawAxis(ScatterPlotData data, GraphMetadata metadata, int[] axisMapping) {
        // 축 레이블 매핑 — axisMapping에 따라 레이블을 분기:
        //   axisMapping[i] == 0 → metadata.getXLabel()
        //   axisMapping[i] == 1 → metadata.getYLabel()
        //   axisMapping[i] >= 2 → 레이블 없음 (U1 미해결: dim≥3 축 레이블 미지원)
        //
        // 예: axisMapping={0,1} → 가로축=xLabel, 세로축=yLabel (기본)
        //     axisMapping={1,0} → 가로축=yLabel, 세로축=xLabel (전치)
        //
        // 이 분기는 U1(F안) 확정 시 metadata.getLabel(axisMapping[i])로 통일 예정.

        String hAxisLabel = getLabel(metadata, axisMapping[0]);  // 가로축
        String vAxisLabel = getLabel(metadata, axisMapping[1]);  // 세로축

        // 눈금 라벨은 ScaleCalculator.getTickLabels() 활용
        // 반환: 축 눈금과 레이블이 포함된 문자열 라인들
    }

    private String getLabel(GraphMetadata metadata, int axisIndex) {
        if (axisIndex == 0) return metadata.getXLabel();
        if (axisIndex == 1) return metadata.getYLabel();
        return "축" + axisIndex;  // dim≥3 임시 처리
    }

    @Override
    public List<String> drawPlot(ScatterPlotData data, int[] axisMapping) {
        // 버킷 방식 — char[][] grid에 데이터 포인트를 '*'로 배치

        List<Double> xValues = data.getPoints().stream()
            .map(p -> p.get(axisMapping[0])).collect(Collectors.toList());
        List<Double> yValues = data.getPoints().stream()
            .map(p -> p.get(axisMapping[1])).collect(Collectors.toList());

        Range xRange = scaleCalc.calcRange(xValues);
        Range yRange = scaleCalc.calcRange(yValues);

        char[][] grid = new char[gridHeight][gridWidth];
        for (char[] row : grid) Arrays.fill(row, ' ');

        for (Point p : data.getPoints()) {
            int col = scaleCalc.toCol(p.get(axisMapping[0]),
                xRange.getMin(), xRange.getMax(), gridWidth);
            int row = scaleCalc.toRow(p.get(axisMapping[1]),
                yRange.getMin(), yRange.getMax(), gridHeight);
            if (row >= 0 && row < gridHeight && col >= 0 && col < gridWidth) {
                grid[row][col] = '*';
            }
        }

        List<String> lines = new ArrayList<>();
        for (char[] row : grid) {
            lines.add(new String(row));
        }
        return lines;
    }
}
```

- **중복 좌표 처리**: 같은 grid 셀에 여러 포인트 → 덮어쓰기 (`*` 하나).
- `drawAxis()`의 구체적 출력 형태는 구현자 재량. 최소한 눈금 수치와 축 레이블 포함.

### 3. `BarGraphDrawer` — 막대그래프 그리기

```java
package makeagraph.drawer;

import makeagraph.data.BarGraphData;
import makeagraph.data.GraphMetadata;
import makeagraph.util.*;
import java.util.*;
import java.util.stream.Collectors;

public class BarGraphDrawer implements IBarDrawer {

    private final ScaleCalculator scaleCalc;
    private final int gridWidth;
    private final int gridHeight;

    private static final int DEFAULT_GRID_WIDTH = 60;
    private static final int DEFAULT_GRID_HEIGHT = 20;

    public BarGraphDrawer(ScaleCalculator scaleCalc) {
        this(scaleCalc, DEFAULT_GRID_WIDTH, DEFAULT_GRID_HEIGHT);
    }

    public BarGraphDrawer(ScaleCalculator scaleCalc, int gridWidth, int gridHeight) {
        this.scaleCalc = scaleCalc;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    @Override
    public List<String> drawTitle(GraphMetadata metadata) {
        List<String> lines = new ArrayList<>();
        lines.add(metadata.getTitle());
        lines.add("");
        return lines;
    }

    @Override
    public List<String> drawAxis(BarGraphData data, GraphMetadata metadata, int[] axisMapping) {
        // axisMapping은 무시 — direction으로 축 레이블 결정:
        //   VERTICAL   → 가로축(x) = 카테고리, 세로축(y) = 값
        //   HORIZONTAL → 가로축(x) = 값,       세로축(y) = 카테고리
        //
        // metadata.getXLabel() / getYLabel()을 direction에 따라 배치
        // 눈금 라벨은 ScaleCalculator.getTickLabels() 활용
        // 반환: 축 눈금과 레이블이 포함된 문자열 라인들
    }

    @Override
    public List<String> drawPlot(BarGraphData data, int[] axisMapping) {
        List<Pair<String, Double>> bars = data.getBars();
        List<Double> values = new ArrayList<>(
            bars.stream().map(Pair::getSecond).collect(Collectors.toList())
        );

        // [기준선 보장] 0을 포함하도록 values에 0.0 추가
        values.add(0.0);

        Range valueRange = scaleCalc.calcRange(values);

        char[][] grid = new char[gridHeight][gridWidth];
        for (char[] row : grid) Arrays.fill(row, ' ');

        if (data.getDirection() == Direction.VERTICAL) {
            // 수직 막대: 각 카테고리가 x축 위치, 값이 y축 높이
            int barWidth = gridWidth / bars.size();
            for (int i = 0; i < bars.size(); i++) {
                double val = bars.get(i).getSecond();
                int col = i * barWidth + barWidth / 2;
                int topRow = scaleCalc.toRow(val,
                    valueRange.getMin(), valueRange.getMax(), gridHeight);
                int baseRow = scaleCalc.toRow(0,
                    valueRange.getMin(), valueRange.getMax(), gridHeight);
                for (int r = Math.min(topRow, baseRow); r <= Math.max(topRow, baseRow); r++) {
                    if (r >= 0 && r < gridHeight && col >= 0 && col < gridWidth) {
                        grid[r][col] = '*';
                    }
                }
            }
        } else {
            // 수평 막대: 카테고리가 y축, 값이 x축 길이
            int barHeight = gridHeight / bars.size();
            for (int i = 0; i < bars.size(); i++) {
                double val = bars.get(i).getSecond();
                int row = i * barHeight + barHeight / 2;
                int endCol = scaleCalc.toCol(val,
                    valueRange.getMin(), valueRange.getMax(), gridWidth);
                int baseCol = scaleCalc.toCol(0,
                    valueRange.getMin(), valueRange.getMax(), gridWidth);
                for (int c = Math.min(baseCol, endCol); c <= Math.max(baseCol, endCol); c++) {
                    if (row >= 0 && row < gridHeight && c >= 0 && c < gridWidth) {
                        grid[row][c] = '*';
                    }
                }
            }
        }

        List<String> lines = new ArrayList<>();
        for (char[] row : grid) {
            lines.add(new String(row));
        }
        return lines;
    }
}
```

- **axisMapping 무시**: BarGraphDrawer는 `data.getDirection()`으로 렌더링 방향을 결정. axisMapping 파라미터는 IAxisPlot 계약상 받지만 사용하지 않음.
- **기준선 보장**: values에 `0.0`을 추가하여 calcRange()가 0을 포함하는 범위를 계산. 이래야 `toRow(0, ...)` 시 grid 안에 기준선이 위치.

---

## 주의사항

1. `ScaleCalculator`는 Drawer의 필드로 보유 — 생성자에서 주입받음.
2. `gridWidth`/`gridHeight`는 기본값 상수 사용하되 생성자 오버로드로 주입 가능. `GraphFactory`에서 기본값 생성자를 호출.
3. `drawAxis()`는 구현 재량이 크지만, 최소한 눈금 수치와 축 레이블이 출력되어야 함.
4. **축 레이블 매핑**: `axisMapping` 인덱스에 따라 `getXLabel()`/`getYLabel()`을 분기하여 가로축/세로축 레이블 결정. 전치({1,0}) 시 레이블이 교차하는 것은 의도된 동작.
5. `Pair`의 import 경로가 1단계에서 정의한 위치와 일치하는지 확인.
6. `ViewController.getAvailableViews()`의 순서 쌍 생성 — 순열(permutation) 로직으로 구현 (조합이 아님).
