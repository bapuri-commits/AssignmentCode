package makeagraph.input;

import makeagraph.data.BarGraphData;
import makeagraph.data.GraphMetadata;
import makeagraph.util.Direction;
import makeagraph.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class BarConsoleInputHandler
        implements IGraphDataInputHandler<BarGraphData, Pair<String, Double>> {

    @Override
    public BarGraphData readData(IInputSource source) {
        Direction direction;
        while (true) {
            String input = (String) source.readObject("방향 (VERTICAL / HORIZONTAL): ");
            if ("VERTICAL".equalsIgnoreCase(input.trim())) {
                direction = Direction.VERTICAL;
                break;
            } else if ("HORIZONTAL".equalsIgnoreCase(input.trim())) {
                direction = Direction.HORIZONTAL;
                break;
            }
            System.out.println("VERTICAL 또는 HORIZONTAL을 입력하세요.");
        }

        IDataParser<Pair<String, Double>> parser = createParser();
        List<Pair<String, Double>> bars = new ArrayList<>();

        String line = (String) source.readObject("데이터를 입력하세요 (카테고리, 값). 빈 줄로 종료:\n");

        while (true) {
            if (line == null || line.isEmpty()) {
                if (bars.isEmpty()) {
                    System.out.println("최소 1건의 데이터가 필요합니다.");
                    line = (String) source.readObject("");
                    continue;
                }
                break;
            }

            try {
                Pair<String, Double> bar = parser.parse(line);
                bars.add(bar);
            } catch (Exception e) {
                System.out.println("입력 오류: " + e.getMessage());
            }

            line = (String) source.readObject("");
        }

        BarGraphData data = new BarGraphData(direction);
        for (Pair<String, Double> bar : bars) {
            data.append(bar);
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
    public IDataParser<Pair<String, Double>> createParser() {
        return raw -> {
            String[] parts = raw.split(",", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("카테고리, 값 형식으로 입력하세요.");
            }
            String category = parts[0].trim();
            double value = Double.parseDouble(parts[1].trim());
            return new Pair<>(category, value);
        };
    }

    @Override
    public Pair<String, Double> parseData(IInputSource source) {
        String raw = (String) source.readObject("데이터를 입력하세요 (예: 사과, 15.0): ");
        return createParser().parse(raw);
    }
}
