package makeagraph.input;

public class ConsoleTypeSelector implements ITypeSelector {
    @Override
    public String selectType(IInputSource source) {
        while (true) {
            String input = (String) source.readObject("그래프 타입을 선택하세요 (scatter / bar): ");
            if ("scatter".equalsIgnoreCase(input) || "bar".equalsIgnoreCase(input))
                return input.toLowerCase();
            System.out.println("scatter 또는 bar를 입력하세요.");
        }
    }
}
