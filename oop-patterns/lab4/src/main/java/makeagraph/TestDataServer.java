package makeagraph;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.Random;

/**
 * TCP 테스트 서버 — SocketRealtimeSource 검증용.
 *
 * 사용법:
 *   java makeagraph.TestDataServer scatter 9090        (산점도 데이터)
 *   java makeagraph.TestDataServer bar 9090            (막대 그래프 데이터)
 *   java makeagraph.TestDataServer scatter 9090 500 20 (interval=500ms, count=20)
 *
 * 클라이언트가 연결하면 지정된 형식의 랜덤 데이터를 줄 단위로 전송한다.
 */
public class TestDataServer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("사용법: TestDataServer <scatter|bar> <port> [intervalMs] [count]");
            System.exit(1);
        }

        String type = args[0];
        int port = Integer.parseInt(args[1]);
        long interval = args.length >= 3 ? Long.parseLong(args[2]) : 1000;
        int count = args.length >= 4 ? Integer.parseInt(args[3]) : 10;

        Random rng = new Random();
        String[] barCategories = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun",
                                   "A", "B", "C", "D", "E", "F"};

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("[서버] 포트 %d에서 대기 중... (type=%s, interval=%dms, count=%d)%n",
                              port, type, interval, count);

            try (Socket client = serverSocket.accept()) {
                System.out.println("[서버] 클라이언트 연결됨: " + client.getRemoteSocketAddress());

                PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                for (int i = 0; i < count; i++) {
                    String line;
                    if ("bar".equalsIgnoreCase(type)) {
                        String cat = barCategories[rng.nextInt(barCategories.length)];
                        double val = 1 + rng.nextInt(20);
                        line = cat + ", " + val;
                    } else {
                        double x = rng.nextDouble() * 100;
                        double y = rng.nextDouble() * 100;
                        line = String.format(Locale.US, "%.1f, %.1f", x, y);
                    }

                    out.println(line);
                    System.out.printf("[서버] #%d 전송: %s%n", i + 1, line);
                    Thread.sleep(interval);
                }

                System.out.println("[서버] 전송 완료. 연결 종료.");
            }
        }
    }
}
