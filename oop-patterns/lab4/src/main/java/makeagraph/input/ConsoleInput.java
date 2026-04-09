package makeagraph.input;

import java.util.Scanner;

public class ConsoleInput implements IInputSource {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public Object readObject() { return readObject(""); }

    @Override
    public Object readObject(String prompt) {
        if (prompt != null && !prompt.isEmpty()) System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }

    @Override
    public void close() { scanner.close(); }
}
