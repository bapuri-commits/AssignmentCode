package makeagraph;

import makeagraph.assembly.GraphDirector;
import makeagraph.input.ConsoleInput;
import makeagraph.input.ConsoleTypeSelector;
import makeagraph.input.IInputSource;
import makeagraph.input.ITypeSelector;
import makeagraph.renderer.IRenderer;
import makeagraph.renderer.TextRenderer;
import makeagraph.session.ISession;

public class Main {
    public static void main(String[] args) {
        IInputSource source = new ConsoleInput();
        IRenderer renderer = new TextRenderer();
        ITypeSelector typeSelector = new ConsoleTypeSelector();

        GraphDirector director = new GraphDirector(typeSelector);
        ISession session = director.construct(source, renderer);
        session.start();
        session.await();
    }
}
