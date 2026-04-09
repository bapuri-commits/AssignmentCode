package makeagraph.data;

import makeagraph.observer.IGraphObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScatterPlotData {
    private final List<Point> points = new ArrayList<>();
    private final ObserverSupport observerSupport = new ObserverSupport();

    public void append(Point point) { points.add(point); observerSupport.notifyObservers(); }
    public List<Point> getPoints() { return Collections.unmodifiableList(points); }
    public void addObserver(IGraphObserver o) { observerSupport.addObserver(o); }
    public void suspendObservers() { observerSupport.suspend(); }
    public void resumeObservers() { observerSupport.resume(); }
}
