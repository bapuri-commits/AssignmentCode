package makeagraph.graph;

import java.util.List;

public interface IViewControllable {
    void swapAxes(int a, int b);
    void setView(int[] axes);
    List<int[]> getAvailableViews();
}
