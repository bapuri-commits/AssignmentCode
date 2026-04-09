package makeagraph.graph;

import makeagraph.data.GraphMetadata;
import makeagraph.drawer.ITitle;
import makeagraph.observer.IGraphObserver;
import makeagraph.renderer.IRenderer;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGraph<T, D extends ITitle>
        implements IGraph<T>, IGraphObserver {

    protected final T data;
    protected final GraphMetadata metadata;
    protected final IRenderer renderer;
    protected final D drawer;

    protected AbstractGraph(T data, GraphMetadata metadata, IRenderer renderer, D drawer) {
        this.data = data;
        this.metadata = metadata;
        this.renderer = renderer;
        this.drawer = drawer;
    }

    @Override
    public final void draw() {
        List<String> lines = new ArrayList<>();
        lines.addAll(drawer.drawTitle(metadata));
        lines.addAll(drawBody());
        renderer.print(lines);
    }

    @Override
    public void onDataChanged() { draw(); }

    protected abstract List<String> drawBody();
}
