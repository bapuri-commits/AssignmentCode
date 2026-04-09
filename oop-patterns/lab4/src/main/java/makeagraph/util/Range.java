package makeagraph.util;

public class Range {
    private final double min, max;

    public Range(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
}
