package makeagraph.input;

import makeagraph.realtime.DataQueue;

public interface IRealtimeSource<E> {
    void stream(DataQueue<E> queue);
    void stop();
}
