package makeagraph.graph;

import makeagraph.data.GraphMetadata;
import makeagraph.drawer.IAxis;
import makeagraph.drawer.IAxisPlot;
import makeagraph.drawer.ITitle;
import makeagraph.renderer.IRenderer;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAxisGraph<T, D extends ITitle & IAxis<T> & IAxisPlot<T>>
        extends AbstractGraph<T, D>
        implements IViewControllable {

    protected final ViewController viewController;

    protected AbstractAxisGraph(T data, GraphMetadata metadata,
                                IRenderer renderer, D drawer, ViewController vc) {
        super(data, metadata, renderer, drawer);
        this.viewController = vc;
    }

    @Override
    protected List<String> drawBody() {
        int[] axes = viewController.getAxes();
        List<String> body = new ArrayList<>();
        body.addAll(drawer.drawAxis(data, metadata, axes));
        body.addAll(drawer.drawPlot(data, axes));
        return body;
    }

    @Override
    public void swapAxes(int a, int b) { viewController.swapAxes(a, b); draw(); }

    @Override
    public void setView(int[] axes) { viewController.setView(axes); draw(); }

    @Override
    public List<int[]> getAvailableViews() { return viewController.getAvailableViews(); }
}
