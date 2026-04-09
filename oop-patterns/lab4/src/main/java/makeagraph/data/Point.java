package makeagraph.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Point {
    private final List<Double> coords;

    private Point(List<Double> coords) {
        this.coords = Collections.unmodifiableList(new ArrayList<>(coords));
    }

    public double get(int index) { return coords.get(index); }
    public int size() { return coords.size(); }

    public static Point of(double... values) {
        List<Double> list = new ArrayList<>();
        for (double v : values) list.add(v);
        return new Point(list);
    }
}
