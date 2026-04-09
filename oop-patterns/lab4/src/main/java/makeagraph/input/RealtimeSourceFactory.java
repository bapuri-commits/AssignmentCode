package makeagraph.input;

public interface RealtimeSourceFactory {
    <E> IRealtimeSource<E> create(IDataParser<E> parser);
}
