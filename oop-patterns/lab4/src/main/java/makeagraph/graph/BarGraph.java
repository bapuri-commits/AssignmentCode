package makeagraph.graph;

import makeagraph.data.BarGraphData;
import makeagraph.data.GraphMetadata;
import makeagraph.drawer.IBarDrawer;
import makeagraph.renderer.IRenderer;
import makeagraph.util.Pair;

import java.util.Collections;
import java.util.List;

public class BarGraph extends AbstractAxisGraph<BarGraphData, IBarDrawer>
        implements IBatchAppendable<Pair<String, Double>> {

    public BarGraph(BarGraphData data, GraphMetadata metadata,
                    IRenderer renderer, IBarDrawer drawer, ViewController vc) {
        super(data, metadata, renderer, drawer, vc);
    }

    @Override public void appendData(Pair<String, Double> bar) { data.append(bar); }
    @Override public void suspendObservers() { data.suspendObservers(); }
    @Override public void resumeObservers() { data.resumeObservers(); }

    @Override
    public List<int[]> getAvailableViews() { return Collections.emptyList(); }
}
