package makeagraph.input;

import makeagraph.realtime.DataQueue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class SocketRealtimeSource<E> implements IRealtimeSource<E> {
    private final String host;
    private final int port;
    private final IDataParser<E> parser;
    private DataQueue<E> queue;
    private volatile boolean stopped = false;
    private Socket socket;

    public SocketRealtimeSource(String host, int port, IDataParser<E> parser) {
        this.host = host;
        this.port = port;
        this.parser = parser;
    }

    @Override
    public void stream(DataQueue<E> queue) {
        this.queue = queue;
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

                String raw;
                while (!stopped && (raw = reader.readLine()) != null) {
                    try {
                        E data = parser.parse(raw);
                        queue.enqueue(data);
                    } catch (Exception e) {
                        System.err.println("파싱 실패, 무시: " + raw);
                    }
                }
            } catch (Exception e) {
                if (!stopped) {
                    System.err.println("스트림 에러: " + e.getMessage());
                }
            } finally {
                queue.close();
                closeSocket();
            }
        }).start();
    }

    @Override
    public void stop() {
        if (stopped) return;
        stopped = true;
        closeSocket();
        if (queue != null) queue.close();
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }
}
