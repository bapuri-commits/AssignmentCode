package makeagraph.input;

public interface IInputSource {
    default Object readObject() { return readObject(""); }
    Object readObject(String prompt);
    void close();
}
