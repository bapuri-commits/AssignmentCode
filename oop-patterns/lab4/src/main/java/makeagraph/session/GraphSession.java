package makeagraph.session;

import makeagraph.graph.IDataAppendable;
import makeagraph.graph.IGraph;
import makeagraph.graph.IViewControllable;
import makeagraph.input.IGraphDataInputHandler;
import makeagraph.input.IInputSource;

import java.util.List;

public class GraphSession<T, E> implements ISession {
    private final IInputSource source;
    private final IGraph<T> graph;
    private final IDataAppendable<E> appendable;
    private final IGraphDataInputHandler<T, E> handler;
    private Thread workerThread;

    public GraphSession(IInputSource source, IGraph<T> graph,
                        IDataAppendable<E> appendable,
                        IGraphDataInputHandler<T, E> handler) {
        this.source = source;
        this.graph = graph;
        this.appendable = appendable;
        this.handler = handler;
    }

    @Override
    public void start() { workerThread = new Thread(this::run); workerThread.start(); }

    @Override
    public void stop() { source.close(); }

    @Override
    public void await() {
        try { if (workerThread != null) workerThread.join(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void run() {
        try {
            graph.draw();

            while (true) {
                Object input = source.readObject("\n명령어 (add / view / exit): ");
                if (input == null) break;
                String cmd = ((String) input).trim().toLowerCase();

                switch (cmd) {
                    case "add" -> {
                        try {
                            E data = handler.parseData(source);
                            appendable.appendData(data);
                        } catch (Exception e) {
                            System.err.println("입력 오류: " + e.getMessage());
                        }
                    }
                    case "view" -> handleView();
                    case "exit" -> { return; }
                    default -> System.out.println("add, view, exit 중 입력하세요.");
                }
            }
        } catch (Exception e) {
            System.err.println("세션 오류: " + e.getMessage());
        } finally {
            source.close();
        }
    }

    private void handleView() {
        if (!(graph instanceof IViewControllable vc)) {
            System.out.println("이 그래프는 뷰 전환을 지원하지 않습니다.");
            return;
        }

        List<int[]> views = vc.getAvailableViews();
        if (views.isEmpty()) {
            System.out.println("전환할 뷰가 없습니다.");
            return;
        }

        for (int i = 0; i < views.size(); i++) {
            System.out.printf("  %d. 축%d - 축%d%n", i + 1, views.get(i)[0], views.get(i)[1]);
        }

        try {
            String numStr = (String) source.readObject("번호 입력: ");
            int idx = Integer.parseInt(numStr.trim()) - 1;
            if (idx >= 0 && idx < views.size()) {
                vc.setView(views.get(idx));
            } else {
                System.out.println("유효한 번호를 입력하세요.");
            }
        } catch (Exception e) {
            System.out.println("유효한 번호를 입력하세요.");
        }
    }
}
