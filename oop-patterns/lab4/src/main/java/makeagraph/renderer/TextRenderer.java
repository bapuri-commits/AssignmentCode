package makeagraph.renderer;

import java.util.List;

public class TextRenderer implements IRenderer {
    @Override
    public void print(List<String> lines) {
        System.out.println();
        for (String line : lines) System.out.println(line);
    }
}
