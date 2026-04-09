package makeagraph.realtime;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataQueue<E> {
    private final BlockingQueue<Object> queue;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private static final Object POISON = new Object();

    public DataQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity + 1);
    }

    public void enqueue(E data) {
        if (closed.get()) return;
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    public E dequeue() {
        try {
            Object item = queue.take();
            if (item == POISON) {
                queue.put(POISON);
                return null;
            }
            return (E) item;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public E tryDequeue() {
        Object item = queue.poll();
        if (item == null || item == POISON) {
            if (item == POISON) {
                try { queue.put(POISON); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            return null;
        }
        return (E) item;
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            queue.offer(POISON);
        }
    }
}
