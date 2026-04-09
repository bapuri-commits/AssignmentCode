package makeagraph.assembly;

import makeagraph.graph.IBatchAppendable;
import makeagraph.graph.IGraph;
import makeagraph.input.IGraphDataInputHandler;

public class BuildResult<T, E> {
    private final IGraph<T> graph;
    private final IBatchAppendable<E> appendable;
    private final IGraphDataInputHandler<T, E> handler;

    public BuildResult(IGraph<T> graph, IBatchAppendable<E> appendable,
                       IGraphDataInputHandler<T, E> handler) {
        this.graph = graph;
        this.appendable = appendable;
        this.handler = handler;
    }

    public IGraph<T> graph() { return graph; }
    public IBatchAppendable<E> appendable() { return appendable; }
    public IGraphDataInputHandler<T, E> handler() { return handler; }
}
