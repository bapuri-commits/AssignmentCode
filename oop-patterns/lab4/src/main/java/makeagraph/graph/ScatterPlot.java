package makeagraph.graph;

import makeagraph.data.GraphMetadata;
import makeagraph.data.Point;
import makeagraph.data.ScatterPlotData;
import makeagraph.drawer.IScatterDrawer;
import makeagraph.renderer.IRenderer;

public class ScatterPlot extends AbstractAxisGraph<ScatterPlotData, IScatterDrawer>
        implements IBatchAppendable<Point> {

    public ScatterPlot(ScatterPlotData data, GraphMetadata metadata,
                       IRenderer renderer, IScatterDrawer drawer, ViewController vc) {
        super(data, metadata, renderer, drawer, vc);
    }

    @Override public void appendData(Point point) { data.append(point); }
    @Override public void suspendObservers() { data.suspendObservers(); }
    @Override public void resumeObservers() { data.resumeObservers(); }
}
