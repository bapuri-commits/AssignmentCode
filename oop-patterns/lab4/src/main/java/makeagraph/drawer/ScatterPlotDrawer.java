package makeagraph.drawer;

import makeagraph.data.GraphMetadata;
import makeagraph.data.Point;
import makeagraph.data.ScatterPlotData;
import makeagraph.util.Range;
import makeagraph.util.ScaleCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScatterPlotDrawer implements IScatterDrawer {
    private final ScaleCalculator scaleCalc;
    private final int gridWidth, gridHeight;
    private static final int DEFAULT_WIDTH = 60, DEFAULT_HEIGHT = 20;

    public ScatterPlotDrawer(ScaleCalculator sc) { this(sc, DEFAULT_WIDTH, DEFAULT_HEIGHT); }

    public ScatterPlotDrawer(ScaleCalculator sc, int gridWidth, int gridHeight) {
        this.scaleCalc = sc;
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
    public List<String> drawAxis(ScatterPlotData data, GraphMetadata metadata, int[] axisMapping) {
        List<String> lines = new ArrayList<>();

        String hLabel = getLabelForAxis(axisMapping[0], metadata);
        String vLabel = getLabelForAxis(axisMapping[1], metadata);

        List<Point> points = data.getPoints();
        List<Double> xValues = points.stream().map(p -> p.get(axisMapping[0])).collect(Collectors.toList());
        List<Double> yValues = points.stream().map(p -> p.get(axisMapping[1])).collect(Collectors.toList());

        Range xRange = scaleCalc.calcRange(xValues);
        Range yRange = scaleCalc.calcRange(yValues);

        List<String> xTicks = scaleCalc.getTickLabels(xRange);
        List<String> yTicks = scaleCalc.getTickLabels(yRange);

        lines.add("Y (" + vLabel + "): " + String.join(", ", yTicks));
        lines.add("X (" + hLabel + "): " + String.join(", ", xTicks));

        return lines;
    }

    private String getLabelForAxis(int axisIndex, GraphMetadata metadata) {
        if (axisIndex == 0) return metadata.getXLabel();
        if (axisIndex == 1) return metadata.getYLabel();
        return "축" + axisIndex;
    }

    @Override
    public List<String> drawPlot(ScatterPlotData data, int[] axisMapping) {
        List<Point> points = data.getPoints();
        List<Double> xValues = points.stream().map(p -> p.get(axisMapping[0])).collect(Collectors.toList());
        List<Double> yValues = points.stream().map(p -> p.get(axisMapping[1])).collect(Collectors.toList());

        Range xRange = scaleCalc.calcRange(xValues);
        Range yRange = scaleCalc.calcRange(yValues);

        char[][] grid = new char[gridHeight][gridWidth];
        for (int r = 0; r < gridHeight; r++)
            for (int c = 0; c < gridWidth; c++)
                grid[r][c] = ' ';

        for (int i = 0; i < points.size(); i++) {
            int col = scaleCalc.toCol(xValues.get(i), xRange.getMin(), xRange.getMax(), gridWidth);
            int row = scaleCalc.toRow(yValues.get(i), yRange.getMin(), yRange.getMax(), gridHeight);
            grid[row][col] = '*';
        }

        List<String> lines = new ArrayList<>();
        for (int r = 0; r < gridHeight; r++) {
            lines.add(new String(grid[r]));
        }
        return lines;
    }
}
