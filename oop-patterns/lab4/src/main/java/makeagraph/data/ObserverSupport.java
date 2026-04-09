package makeagraph.data;

import makeagraph.observer.IGraphObserver;
import java.util.ArrayList;
import java.util.List;

public class ObserverSupport {
    private final List<IGraphObserver> observers = new ArrayList<>();
    private boolean suspended = false;
    private boolean dirty = false;

    public void addObserver(IGraphObserver o) { observers.add(o); }

    public void notifyObservers() {
        if (suspended) { dirty = true; return; }
        for (IGraphObserver o : observers) o.onDataChanged();
    }

    public void suspend() { suspended = true; dirty = false; }

    public void resume() {
        suspended = false;
        if (dirty) { dirty = false; notifyObservers(); }
    }
}
