package makeagraph.input;

import java.util.function.Supplier;

public class SimulatedRealtimeSourceFactory implements RealtimeSourceFactory {
    private final Supplier<String> rawGenerator;
    private final long intervalMs;
    private final int count;

    public SimulatedRealtimeSourceFactory(Supplier<String> rawGenerator, long intervalMs, int count) {
        this.rawGenerator = rawGenerator;
        this.intervalMs = intervalMs;
        this.count = count;
    }

    @Override
    public <E> IRealtimeSource<E> create(IDataParser<E> parser) {
        Supplier<E> generator = () -> parser.parse(rawGenerator.get());
        return new SimulatedRealtimeSource<>(generator, intervalMs, count);
    }
}
