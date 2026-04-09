package makeagraph.input;

@FunctionalInterface
public interface IDataParser<E> {
    E parse(String raw);
}
