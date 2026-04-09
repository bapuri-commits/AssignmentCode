package makeagraph.assembly;

import makeagraph.data.BarGraphData;
import makeagraph.data.GraphMetadata;
import makeagraph.data.ScatterPlotData;
import makeagraph.drawer.BarGraphDrawer;
import makeagraph.drawer.IBarDrawer;
import makeagraph.drawer.IScatterDrawer;
import makeagraph.drawer.ScatterPlotDrawer;
import makeagraph.graph.BarGraph;
import makeagraph.graph.ScatterPlot;
import makeagraph.graph.ViewController;
import makeagraph.renderer.IRenderer;
import makeagraph.util.ScaleCalculator;

public class GraphFactory {
    private GraphFactory() {}

    public static ScatterPlot createScatter(ScatterPlotData data, GraphMetadata meta,
                                            IRenderer renderer) {
        ScaleCalculator sc = new ScaleCalculator();
        IScatterDrawer drawer = new ScatterPlotDrawer(sc);
        int dim = data.getPoints().get(0).size();
        ViewController vc = new ViewController(dim);
        return new ScatterPlot(data, meta, renderer, drawer, vc);
    }

    public static BarGraph createBar(BarGraphData data, GraphMetadata meta,
                                     IRenderer renderer) {
        ScaleCalculator sc = new ScaleCalculator();
        IBarDrawer drawer = new BarGraphDrawer(sc);
        ViewController vc = new ViewController(2);
        return new BarGraph(data, meta, renderer, drawer, vc);
    }
}
