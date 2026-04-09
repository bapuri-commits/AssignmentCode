package makeagraph.input;

import makeagraph.realtime.DataQueue;

import java.util.function.Supplier;

public class SimulatedRealtimeSource<E> implements IRealtimeSource<E> {
    private final Supplier<E> generator;
    private final long intervalMs;
    private final int count;
    private DataQueue<E> queue;
    private volatile boolean stopped = false;

    public SimulatedRealtimeSource(Supplier<E> generator, long intervalMs, int count) {
        this.generator = generator;
        this.intervalMs = intervalMs;
        this.count = count;
    }

    @Override
    public void stream(DataQueue<E> queue) {
        this.queue = queue;
        new Thread(() -> {
            try {
                for (int i = 0; i < count && !stopped; i++) {
                    E data = generator.get();
                    queue.enqueue(data);
                    Thread.sleep(intervalMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("시뮬레이션 에러: " + e.getMessage());
            } finally {
                queue.close();
            }
        }).start();
    }

    @Override
    public void stop() {
        if (stopped) return;
        stopped = true;
        if (queue != null) queue.close();
    }
}
