package makeagraph.input;

import makeagraph.data.GraphMetadata;

public interface IGraphDataInputHandler<T, E> {
    T readData(IInputSource source);
    GraphMetadata readMetadata(IInputSource source);
    E parseData(IInputSource source);
    IDataParser<E> createParser();
}
