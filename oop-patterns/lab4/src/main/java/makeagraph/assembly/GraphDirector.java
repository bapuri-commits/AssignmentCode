package makeagraph.assembly;

import makeagraph.data.BarGraphData;
import makeagraph.data.GraphMetadata;
import makeagraph.data.ScatterPlotData;
import makeagraph.graph.BarGraph;
import makeagraph.graph.ScatterPlot;
import makeagraph.input.BarConsoleInputHandler;
import makeagraph.input.IInputSource;
import makeagraph.input.ITypeSelector;
import makeagraph.input.ScatterConsoleInputHandler;
import makeagraph.input.IDataParser;
import makeagraph.input.IRealtimeSource;
import makeagraph.input.RealtimeSourceFactory;
import makeagraph.realtime.DataQueue;
import makeagraph.renderer.IRenderer;
import makeagraph.session.GraphSession;
import makeagraph.session.ISession;
import makeagraph.session.RealtimeGraphSession;

public class GraphDirector {
    private final ITypeSelector typeSelector;

    public GraphDirector(ITypeSelector typeSelector) { this.typeSelector = typeSelector; }

    public ISession construct(IInputSource source, IRenderer renderer) {
        return assembleSession(constructGraph(source, renderer), source);
    }

    private <T, E> ISession assembleSession(BuildResult<T, E> result, IInputSource source) {
        return new GraphSession<>(source, result.graph(), result.appendable(), result.handler());
    }

    private BuildResult<?, ?> constructGraph(IInputSource source, IRenderer renderer) {
        String type = typeSelector.selectType(source);

        if ("scatter".equals(type)) {
            var handler = new ScatterConsoleInputHandler();
            ScatterPlotData data = handler.readData(source);
            GraphMetadata meta = handler.readMetadata(source);
            ScatterPlot graph = GraphFactory.createScatter(data, meta, renderer);
            data.addObserver(graph);
            return new BuildResult<>(graph, graph, handler);

        } else if ("bar".equals(type)) {
            var handler = new BarConsoleInputHandler();
            BarGraphData data = handler.readData(source);
            GraphMetadata meta = handler.readMetadata(source);
            BarGraph graph = GraphFactory.createBar(data, meta, renderer);
            data.addObserver(graph);
            return new BuildResult<>(graph, graph, handler);
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }

    public ISession constructRealtimeSession(IInputSource source, IRenderer renderer,
                                              RealtimeSourceFactory factory) {
        return assembleRealtimeSession(constructGraph(source, renderer), factory);
    }

    private <T, E> ISession assembleRealtimeSession(BuildResult<T, E> result,
                                                     RealtimeSourceFactory factory) {
        IDataParser<E> parser = result.handler().createParser();
        DataQueue<E> queue = new DataQueue<>(1024);
        IRealtimeSource<E> stream = factory.create(parser);
        stream.stream(queue);
        return new RealtimeGraphSession<>(queue, result.graph(), result.appendable(),
                                          () -> stream.stop());
    }
}
