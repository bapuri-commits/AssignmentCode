package makeagraph.input;

public class SocketRealtimeSourceFactory implements RealtimeSourceFactory {
    private final String host;
    private final int port;

    public SocketRealtimeSourceFactory(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public <E> IRealtimeSource<E> create(IDataParser<E> parser) {
        return new SocketRealtimeSource<>(host, port, parser);
    }
}
