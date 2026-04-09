package makeagraph.data;

public class GraphMetadata {
    private final String title, xLabel, yLabel;

    public GraphMetadata(String title, String xLabel, String yLabel) {
        this.title = title;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
    }

    public String getTitle() { return title; }
    public String getXLabel() { return xLabel; }
    public String getYLabel() { return yLabel; }
}
