package makeagraph;

import makeagraph.assembly.GraphFactory;
import makeagraph.data.*;
import makeagraph.graph.*;
import makeagraph.input.*;
import makeagraph.realtime.DataQueue;
import makeagraph.renderer.IRenderer;
import makeagraph.renderer.TextRenderer;
import makeagraph.session.RealtimeGraphSession;
import makeagraph.util.*;

import java.util.Random;

public class RealtimeDemo {
    public static void main(String[] args) throws InterruptedException {
        IRenderer renderer = new TextRenderer();

        System.out.println("===== [1] Scatter Plot - Realtime Demo =====");
        System.out.println("1초 간격으로 랜덤 좌표 5개가 자동 추가됩니다.\n");
        runScatterDemo(renderer);

        Thread.sleep(1000);

        System.out.println("\n\n===== [2] Bar Graph - Realtime Demo =====");
        System.out.println("1초 간격으로 랜덤 막대 5개가 자동 추가됩니다.\n");
        runBarDemo(renderer);
    }

    private static void runScatterDemo(IRenderer renderer) throws InterruptedException {
        ScatterPlotData data = new ScatterPlotData();
        data.append(Point.of(10, 20));
        data.append(Point.of(20, 35));
        data.append(Point.of(30, 50));

        GraphMetadata meta = new GraphMetadata(
            "Realtime Scatter", "X-axis", "Y-axis"
        );

        ScatterPlot graph = GraphFactory.createScatter(data, meta, renderer);
        data.addObserver(graph);

        Random rng = new Random(42);
        IDataParser<Point> parser = raw -> {
            String[] parts = raw.split(",");
            return Point.of(
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim())
            );
        };

        SimulatedRealtimeSourceFactory factory = new SimulatedRealtimeSourceFactory(
            () -> {
                double x = rng.nextInt(50);
                double y = rng.nextInt(80);
                return x + ", " + y;
            },
            1000, 5
        );

        DataQueue<Point> queue = new DataQueue<>(64);
        IRealtimeSource<Point> source = factory.create(parser);
        source.stream(queue);

        RealtimeGraphSession<ScatterPlotData, Point> session =
            new RealtimeGraphSession<>(queue, graph, graph, () -> source.stop());

        session.start();
        session.await();
    }

    private static void runBarDemo(IRenderer renderer) throws InterruptedException {
        BarGraphData data = new BarGraphData(Direction.VERTICAL);
        data.append(new Pair<>("Mon", 5.0));
        data.append(new Pair<>("Tue", 8.0));

        GraphMetadata meta = new GraphMetadata(
            "Realtime Bar", "Day", "Count"
        );

        BarGraph graph = GraphFactory.createBar(data, meta, renderer);
        data.addObserver(graph);

        String[] days = {"Wed", "Thu", "Fri", "Sat", "Sun"};
        Random rng = new Random(42);
        int[] idx = {0};

        IDataParser<Pair<String, Double>> parser = raw -> {
            String[] parts = raw.split(",", 2);
            return new Pair<>(parts[0].trim(), Double.parseDouble(parts[1].trim()));
        };

        SimulatedRealtimeSourceFactory factory = new SimulatedRealtimeSourceFactory(
            () -> {
                String day = days[idx[0] % days.length];
                idx[0]++;
                double val = 1 + rng.nextInt(15);
                return day + ", " + val;
            },
            1000, 5
        );

        DataQueue<Pair<String, Double>> queue = new DataQueue<>(64);
        IRealtimeSource<Pair<String, Double>> source = factory.create(parser);
        source.stream(queue);

        RealtimeGraphSession<BarGraphData, Pair<String, Double>> session =
            new RealtimeGraphSession<>(queue, graph, graph, () -> source.stop());

        session.start();
        session.await();
    }
}
