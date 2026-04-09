package makeagraph.data;

import makeagraph.observer.IGraphObserver;
import makeagraph.util.Direction;
import makeagraph.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BarGraphData {
    private final List<Pair<String, Double>> bars = new ArrayList<>();
    private final Direction direction;
    private final ObserverSupport observerSupport = new ObserverSupport();

    public BarGraphData(Direction direction) { this.direction = direction; }

    public void append(Pair<String, Double> bar) { bars.add(bar); observerSupport.notifyObservers(); }
    public List<Pair<String, Double>> getBars() { return Collections.unmodifiableList(bars); }
    public Direction getDirection() { return direction; }
    public void addObserver(IGraphObserver o) { observerSupport.addObserver(o); }
    public void suspendObservers() { observerSupport.suspend(); }
    public void resumeObservers() { observerSupport.resume(); }
}
