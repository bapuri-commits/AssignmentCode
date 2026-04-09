package makeagraph.input;

import makeagraph.data.GraphMetadata;
import makeagraph.data.Point;
import makeagraph.data.ScatterPlotData;

import java.util.ArrayList;
import java.util.List;

public class ScatterConsoleInputHandler
        implements IGraphDataInputHandler<ScatterPlotData, Point> {

    private int dim;

    @Override
    public ScatterPlotData readData(IInputSource source) {
        while (true) {
            String input = (String) source.readObject("몇 차원 데이터입니까? ");
            try {
                dim = Integer.parseInt(input.trim());
                if (dim > 0) break;
                System.out.println("1 이상의 정수를 입력하세요.");
            } catch (NumberFormatException e) {
                System.out.println("정수를 입력하세요.");
            }
        }

        IDataParser<Point> parser = createParser();
        List<Point> points = new ArrayList<>();

        String line = (String) source.readObject("데이터를 입력하세요 (v1, v2, ...). 빈 줄로 종료:\n");

        while (true) {
            if (line == null || line.isEmpty()) {
                if (points.isEmpty()) {
                    System.out.println("최소 1건의 데이터가 필요합니다.");
                    line = (String) source.readObject("");
                    continue;
                }
                break;
            }

            try {
                Point p = parser.parse(line);
                points.add(p);
            } catch (Exception e) {
                System.out.println("입력 오류: " + e.getMessage());
            }

            line = (String) source.readObject("");
        }

        ScatterPlotData data = new ScatterPlotData();
        for (Point p : points) {
            data.append(p);
        }
        return data;
    }

    @Override
    public GraphMetadata readMetadata(IInputSource source) {
        String title = (String) source.readObject("그래프 제목: ");
        String xLabel = (String) source.readObject("X축 레이블: ");
        String yLabel = (String) source.readObject("Y축 레이블: ");
        return new GraphMetadata(title, xLabel, yLabel);
    }

    @Override
    public IDataParser<Point> createParser() {
        return raw -> {
            String[] parts = raw.split(",");
            if (parts.length != dim) {
                throw new IllegalArgumentException(dim + "개의 값이 필요합니다.");
            }
            double[] values = new double[dim];
            for (int i = 0; i < dim; i++) {
                values[i] = Double.parseDouble(parts[i].trim());
            }
            return Point.of(values);
        };
    }

    @Override
    public Point parseData(IInputSource source) {
        String raw = (String) source.readObject("데이터를 입력하세요 (예: 1.0, 2.0): ");
        return createParser().parse(raw);
    }
}
