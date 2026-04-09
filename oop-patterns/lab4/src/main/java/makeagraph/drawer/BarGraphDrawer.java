package makeagraph.drawer;

import makeagraph.data.BarGraphData;
import makeagraph.data.GraphMetadata;
import makeagraph.util.Direction;
import makeagraph.util.Pair;
import makeagraph.util.Range;
import makeagraph.util.ScaleCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BarGraphDrawer implements IBarDrawer {
    private final ScaleCalculator scaleCalc;
    private final int gridWidth, gridHeight;
    private static final int DEFAULT_WIDTH = 60, DEFAULT_HEIGHT = 20;

    public BarGraphDrawer(ScaleCalculator sc) { this(sc, DEFAULT_WIDTH, DEFAULT_HEIGHT); }

    public BarGraphDrawer(ScaleCalculator sc, int gridWidth, int gridHeight) {
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
    public List<String> drawAxis(BarGraphData data, GraphMetadata metadata, int[] axisMapping) {
        List<String> lines = new ArrayList<>();
        Direction dir = data.getDirection();
        List<Pair<String, Double>> bars = data.getBars();

        List<Double> values = bars.stream().map(Pair::getSecond).collect(Collectors.toList());
        values.add(0.0);
        Range valueRange = scaleCalc.calcRange(values);
        List<String> valueTicks = scaleCalc.getTickLabels(valueRange);

        List<String> categories = bars.stream().map(Pair::getFirst).collect(Collectors.toList());

        if (dir == Direction.VERTICAL) {
            lines.add("Y (" + metadata.getYLabel() + "): " + String.join(", ", valueTicks));
            lines.add("X (" + metadata.getXLabel() + "): " + String.join(", ", categories));
        } else {
            lines.add("Y (" + metadata.getYLabel() + "): " + String.join(", ", categories));
            lines.add("X (" + metadata.getXLabel() + "): " + String.join(", ", valueTicks));
        }

        return lines;
    }

    @Override
    public List<String> drawPlot(BarGraphData data, int[] axisMapping) {
        List<Pair<String, Double>> bars = data.getBars();

        List<Double> values = bars.stream().map(Pair::getSecond).collect(Collectors.toList());
        values.add(0.0);
        Range valueRange = scaleCalc.calcRange(values);

        char[][] grid = new char[gridHeight][gridWidth];
        for (int r = 0; r < gridHeight; r++)
            for (int c = 0; c < gridWidth; c++)
                grid[r][c] = ' ';

        Direction dir = data.getDirection();

        if (dir == Direction.VERTICAL) {
            int barWidth = gridWidth / bars.size();
            for (int i = 0; i < bars.size(); i++) {
                double val = bars.get(i).getSecond();
                int col = i * barWidth + barWidth / 2;
                int topRow = scaleCalc.toRow(val, valueRange.getMin(), valueRange.getMax(), gridHeight);
                int baseRow = scaleCalc.toRow(0, valueRange.getMin(), valueRange.getMax(), gridHeight);
                int minRow = Math.min(topRow, baseRow);
                int maxRow = Math.max(topRow, baseRow);
                for (int r = minRow; r <= maxRow; r++) {
                    if (r >= 0 && r < gridHeight && col >= 0 && col < gridWidth) {
                        grid[r][col] = '*';
                    }
                }
            }
        } else {
            int barHeight = gridHeight / bars.size();
            for (int i = 0; i < bars.size(); i++) {
                double val = bars.get(i).getSecond();
                int row = i * barHeight + barHeight / 2;
                int endCol = scaleCalc.toCol(val, valueRange.getMin(), valueRange.getMax(), gridWidth);
                int baseCol = scaleCalc.toCol(0, valueRange.getMin(), valueRange.getMax(), gridWidth);
                int minCol = Math.min(baseCol, endCol);
                int maxCol = Math.max(baseCol, endCol);
                for (int c = minCol; c <= maxCol; c++) {
                    if (row >= 0 && row < gridHeight && c >= 0 && c < gridWidth) {
                        grid[row][c] = '*';
                    }
                }
            }
        }

        List<String> lines = new ArrayList<>();
        for (int r = 0; r < gridHeight; r++) {
            lines.add(new String(grid[r]));
        }
        return lines;
    }
}
