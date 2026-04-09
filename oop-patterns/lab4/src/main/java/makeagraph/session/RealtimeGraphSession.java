package makeagraph.session;

import makeagraph.graph.IBatchAppendable;
import makeagraph.graph.IGraph;
import makeagraph.realtime.DataQueue;

public class RealtimeGraphSession<T, E> implements ISession {
    private final IGraph<T> graph;
    private final IBatchAppendable<E> appendable;
    private final DataQueue<E> queue;
    private final Runnable onStop;
    private Thread workerThread;

    public RealtimeGraphSession(DataQueue<E> queue, IGraph<T> graph,
                                IBatchAppendable<E> appendable, Runnable onStop) {
        this.queue = queue;
        this.graph = graph;
        this.appendable = appendable;
        this.onStop = onStop;
    }

    @Override
    public void start() { workerThread = new Thread(this::run); workerThread.start(); }

    @Override
    public void stop() {
        onStop.run();
        try { if (workerThread != null) workerThread.join(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Override
    public void await() {
        try { if (workerThread != null) workerThread.join(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void run() {
        graph.draw();

        while (true) {
            E data = queue.dequeue();
            if (data == null) break;

            try {
                appendable.suspendObservers();
                appendable.appendData(data);

                E next;
                while ((next = queue.tryDequeue()) != null) {
                    appendable.appendData(next);
                }
            } catch (Exception e) {
                System.err.println("실시간 처리 오류: " + e.getMessage());
                onStop.run();
            } finally {
                appendable.resumeObservers();
            }
        }
    }
}
