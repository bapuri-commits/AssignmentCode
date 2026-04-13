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

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.Random;

/**
 * SocketRealtimeSource E2E 통합 테스트.
 * 내장 TCP 서버를 띄우고 → SocketRealtimeSource로 실제 네트워크 경로를 검증한다.
 *
 * 테스트 시나리오:
 *   [1] Scatter Plot — 랜덤 좌표를 소켓으로 수신
 *   [2] Bar Graph   — 랜덤 카테고리/값을 소켓으로 수신
 */
public class SocketRealtimeDemo {

    private static final int DATA_COUNT = 6;
    private static final long INTERVAL_MS = 800;

    public static void main(String[] args) throws Exception {
        IRenderer renderer = new TextRenderer();

        System.out.println("===== SocketRealtimeSource E2E 통합 테스트 =====\n");

        System.out.println("──── [1/2] Scatter Plot (TCP 소켓 경로) ────");
        System.out.println("내장 서버가 랜덤 좌표를 " + INTERVAL_MS + "ms 간격으로 " + DATA_COUNT + "건 전송합니다.\n");
        runScatterSocketTest(renderer);

        Thread.sleep(1000);

        System.out.println("\n\n──── [2/2] Bar Graph (TCP 소켓 경로) ────");
        System.out.println("내장 서버가 랜덤 막대 데이터를 " + INTERVAL_MS + "ms 간격으로 " + DATA_COUNT + "건 전송합니다.\n");
        runBarSocketTest(renderer);

        System.out.println("\n===== 모든 소켓 테스트 완료 =====");
    }

    // ──────────────────────────────────────────────
    //  Scatter Plot — 소켓 경로 테스트
    // ──────────────────────────────────────────────

    private static void runScatterSocketTest(IRenderer renderer) throws Exception {
        ScatterPlotData data = new ScatterPlotData();
        data.append(Point.of(10, 20));
        data.append(Point.of(25, 45));
        data.append(Point.of(40, 60));

        GraphMetadata meta = new GraphMetadata(
            "Socket Scatter Test", "X-axis", "Y-axis"
        );

        ScatterPlot graph = GraphFactory.createScatter(data, meta, renderer);
        data.addObserver(graph);

        int port = startEmbeddedServer("scatter", DATA_COUNT, INTERVAL_MS);
        System.out.println("[테스트] 내장 서버 포트: " + port);

        IDataParser<Point> parser = raw -> {
            String[] parts = raw.split(",");
            return Point.of(
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim())
            );
        };

        SocketRealtimeSourceFactory factory = new SocketRealtimeSourceFactory("localhost", port);
        DataQueue<Point> queue = new DataQueue<>(64);
        IRealtimeSource<Point> source = factory.create(parser);
        source.stream(queue);

        RealtimeGraphSession<ScatterPlotData, Point> session =
            new RealtimeGraphSession<>(queue, graph, graph, () -> source.stop());

        session.start();
        session.await();
        System.out.println("[테스트] Scatter 소켓 테스트 완료 ✓");
    }

    // ──────────────────────────────────────────────
    //  Bar Graph — 소켓 경로 테스트
    // ──────────────────────────────────────────────

    private static void runBarSocketTest(IRenderer renderer) throws Exception {
        BarGraphData data = new BarGraphData(Direction.VERTICAL);
        data.append(new Pair<>("Mon", 5.0));
        data.append(new Pair<>("Tue", 8.0));

        GraphMetadata meta = new GraphMetadata(
            "Socket Bar Test", "Category", "Value"
        );

        BarGraph graph = GraphFactory.createBar(data, meta, renderer);
        data.addObserver(graph);

        int port = startEmbeddedServer("bar", DATA_COUNT, INTERVAL_MS);
        System.out.println("[테스트] 내장 서버 포트: " + port);

        IDataParser<Pair<String, Double>> parser = raw -> {
            String[] parts = raw.split(",", 2);
            return new Pair<>(parts[0].trim(), Double.parseDouble(parts[1].trim()));
        };

        SocketRealtimeSourceFactory factory = new SocketRealtimeSourceFactory("localhost", port);
        DataQueue<Pair<String, Double>> queue = new DataQueue<>(64);
        IRealtimeSource<Pair<String, Double>> source = factory.create(parser);
        source.stream(queue);

        RealtimeGraphSession<BarGraphData, Pair<String, Double>> session =
            new RealtimeGraphSession<>(queue, graph, graph, () -> source.stop());

        session.start();
        session.await();
        System.out.println("[테스트] Bar 소켓 테스트 완료 ✓");
    }

    // ──────────────────────────────────────────────
    //  내장 TCP 서버 (OS가 빈 포트를 자동 할당)
    // ──────────────────────────────────────────────

    private static int startEmbeddedServer(String type, int count, long intervalMs) throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        String[] barCategories = {"Wed", "Thu", "Fri", "Sat", "Sun", "Holiday"};
        Random rng = new Random(System.nanoTime());

        Thread serverThread = new Thread(() -> {
            try {
                Socket client = serverSocket.accept();
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                for (int i = 0; i < count; i++) {
                    String line;
                    if ("bar".equalsIgnoreCase(type)) {
                        String cat = barCategories[i % barCategories.length];
                        double val = 1 + rng.nextInt(20);
                        line = cat + ", " + val;
                    } else {
                        double x = rng.nextDouble() * 80;
                        double y = rng.nextDouble() * 100;
                        line = String.format(Locale.US, "%.1f, %.1f", x, y);
                    }

                    out.println(line);
                    System.out.printf("  [서버→클라이언트] #%d: %s%n", i + 1, line);
                    Thread.sleep(intervalMs);
                }

                client.close();
                serverSocket.close();
            } catch (Exception e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[서버 에러] " + e.getMessage());
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(100);
        return port;
    }
}
