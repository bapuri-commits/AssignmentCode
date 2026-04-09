package makeagraph.drawer;

import makeagraph.data.GraphMetadata;
import java.util.List;

public interface IAxis<T> {
    List<String> drawAxis(T data, GraphMetadata metadata, int[] axisMapping);
}
