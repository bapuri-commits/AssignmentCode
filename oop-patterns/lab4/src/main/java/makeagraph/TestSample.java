package makeagraph;

import makeagraph.assembly.GraphFactory;
import makeagraph.data.*;
import makeagraph.graph.*;
import makeagraph.renderer.TextRenderer;
import makeagraph.renderer.IRenderer;
import makeagraph.util.*;

public class TestSample {
    public static void main(String[] args) {
        IRenderer renderer = new TextRenderer();

        System.out.println("========== [1] Scatter Plot ==========");
        testScatter(renderer);

        System.out.println("\n\n========== [2] Bar Graph (VERTICAL) ==========");
        testBar(renderer);
    }

    private static void testScatter(IRenderer renderer) {
        ScatterPlotData data = new ScatterPlotData();
        double[][] points = {
            {30, 28}, {0, 10}, {5, 20}, {10, 28}, {10, 20},
            {35, 55}, {35, 55}, {36, 62}, {20, 28}, {20, 36},
            {25, 36}, {30, 47}
        };
        for (double[] p : points) {
            data.append(Point.of(p[0], p[1]));
        }

        GraphMetadata meta = new GraphMetadata(
            "Lemonade Sales vs. Temperature",
            "Temperature (F)",
            "Daily Sales (1000 KRW)"
        );

        ScatterPlot graph = GraphFactory.createScatter(data, meta, renderer);
        data.addObserver(graph);

        System.out.println("[Initial draw]");
        graph.draw();

        System.out.println("\n[After adding (15, 40)]");
        graph.appendData(Point.of(15, 40));
    }

    private static void testBar(IRenderer renderer) {
        BarGraphData data = new BarGraphData(Direction.VERTICAL);
        data.append(new Pair<>("A", 11.0));
        data.append(new Pair<>("B", 4.0));
        data.append(new Pair<>("C", 2.0));
        data.append(new Pair<>("D", 1.0));
        data.append(new Pair<>("F", 1.0));

        GraphMetadata meta = new GraphMetadata(
            "OOP Design Course Grades",
            "Grade",
            "Number of Students"
        );

        BarGraph graph = GraphFactory.createBar(data, meta, renderer);
        data.addObserver(graph);

        System.out.println("[Initial draw]");
        graph.draw();

        System.out.println("\n[After adding (A+, 3)]");
        graph.appendData(new Pair<>("A+", 3.0));
    }
}
