package makeagraph.util;

import java.util.ArrayList;
import java.util.List;

public class ScaleCalculator {

    /**
     * nice interval 기반 축 범위 계산.
     *  1) dataMin/Max 추출
     *  2) calcInterval()로 눈금 간격 결정
     *  3) axisMin = floor(dataMin / interval) * interval
     *     axisMax = ceil(dataMax / interval) * interval
     */
    public Range calcRange(List<Double> values) {
        double dataMin = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double dataMax = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);

        double interval = calcInterval(new Range(dataMin, dataMax));

        double axisMin = Math.floor(dataMin / interval) * interval;
        double axisMax = Math.ceil(dataMax / interval) * interval;

        return new Range(axisMin, axisMax);
    }

    /**
     * nice interval 계산.
     *  roughInterval = rawRange / 5
     *  magnitude = 10^floor(log10(roughInterval))
     *  1, 2, 5, 10 중 roughInterval에 가장 가까운 step 선택
     */
    public double calcInterval(Range range) {
        double rawRange = range.getMax() - range.getMin();
        if (rawRange == 0) return 1;

        double roughInterval = rawRange / 5.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughInterval)));

        double[] niceSteps = {1, 2, 5, 10};
        double bestStep = niceSteps[0];
        double minDiff = Double.MAX_VALUE;

        for (double step : niceSteps) {
            double candidate = step * magnitude;
            double diff = Math.abs(candidate - roughInterval);
            if (diff < minDiff) {
                minDiff = diff;
                bestStep = candidate;
            }
        }

        return bestStep;
    }

    /** range 내 눈금 위치를 문자열 목록으로 반환 */
    public List<String> getTickLabels(Range range) {
        double interval = calcInterval(range);
        List<String> labels = new ArrayList<>();

        for (double tick = range.getMin(); tick <= range.getMax() + interval * 0.001; tick += interval) {
            if (tick == (long) tick) {
                labels.add(String.valueOf((long) tick));
            } else {
                labels.add(String.valueOf(tick));
            }
        }

        return labels;
    }

    /** col = (int)((x - xMin) / (xMax - xMin) * (gridWidth - 1)) */
    public int toCol(double x, double xMin, double xMax, int gridWidth) {
        if (xMax == xMin) return 0;
        return (int) ((x - xMin) / (xMax - xMin) * (gridWidth - 1));
    }

    /** row = gridHeight - 1 - (int)((y - yMin) / (yMax - yMin) * (gridHeight - 1)) — y축 반전 */
    public int toRow(double y, double yMin, double yMax, int gridHeight) {
        if (yMax == yMin) return 0;
        return gridHeight - 1 - (int) ((y - yMin) / (yMax - yMin) * (gridHeight - 1));
    }
}
