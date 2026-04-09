package makeagraph.drawer;

import java.util.List;

public interface IAxisPlot<T> {
    List<String> drawPlot(T data, int[] axisMapping);
}
