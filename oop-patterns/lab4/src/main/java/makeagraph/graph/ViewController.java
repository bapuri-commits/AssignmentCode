package makeagraph.graph;

import java.util.ArrayList;
import java.util.List;

public class ViewController {
    private final int dim;
    private int[] axisMapping;

    public ViewController(int dim) {
        this.dim = dim;
        this.axisMapping = new int[]{0, 1};
    }

    public void swapAxes(int a, int b) {
        int temp = axisMapping[a];
        axisMapping[a] = axisMapping[b];
        axisMapping[b] = temp;
    }

    public void setView(int[] axes) {
        if (axes.length != 2) throw new IllegalArgumentException("axes must be length 2");
        this.axisMapping = axes.clone();
    }

    public int[] getAxes() { return axisMapping.clone(); }

    public List<int[]> getAvailableViews() {
        List<int[]> views = new ArrayList<>();
        for (int i = 0; i < dim; i++) {
            for (int j = i + 1; j < dim; j++) {
                views.add(new int[]{i, j});
                views.add(new int[]{j, i});
            }
        }
        return views;
    }
}
