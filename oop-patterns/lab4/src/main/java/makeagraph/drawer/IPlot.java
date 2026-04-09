package makeagraph.drawer;

import java.util.List;

/**
 * 축 매핑 없이 데이터를 그리는 계약.
 * PieChart 등 축 없는 그래프의 Drawer가 구현한다.
 * IAxisPlot<T>과 독립 — 축 매핑 필요 여부가 분리 기준 (ISP).
 */
public interface IPlot<T> {
    List<String> drawPlot(T data);
}
