package makeagraph;

import makeagraph.assembly.GraphDirector;
import makeagraph.input.ConsoleInput;
import makeagraph.input.ConsoleTypeSelector;
import makeagraph.input.IInputSource;
import makeagraph.input.ITypeSelector;
import makeagraph.input.RealtimeSourceFactory;
import makeagraph.input.SocketRealtimeSourceFactory;
import makeagraph.renderer.IRenderer;
import makeagraph.renderer.TextRenderer;
import makeagraph.session.ISession;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<String, String> opts = parseArgs(args);

        String mode = opts.getOrDefault("--mode", "interactive");
        String ui = opts.getOrDefault("--ui", "console");
        String source = opts.getOrDefault("--source", null);

        IRenderer renderer = new TextRenderer();
        ISession session;

        try {
            if ("realtime".equals(mode)) {
                if (source == null) {
                    System.err.println("실시간 모드에는 --source host:port 가 필요합니다.");
                    System.exit(1);
                }

                String[] parts = source.split(":", 2);
                if (parts.length != 2) {
                    System.err.println("--source 형식: host:port (예: localhost:9090)");
                    System.exit(1);
                }
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                IInputSource initSource = new ConsoleInput();
                ITypeSelector typeSelector = new ConsoleTypeSelector();
                GraphDirector director = new GraphDirector(typeSelector);

                RealtimeSourceFactory factory = new SocketRealtimeSourceFactory(host, port);
                session = director.constructRealtimeSession(initSource, renderer, factory);

            } else {
                IInputSource inputSource = new ConsoleInput();
                ITypeSelector typeSelector = new ConsoleTypeSelector();
                GraphDirector director = new GraphDirector(typeSelector);
                session = director.construct(inputSource, renderer);
            }

            session.start();
            session.await();

        } catch (Exception e) {
            System.err.println("오류: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                opts.put(args[i], args[i + 1]);
                i++;
            }
        }
        return opts;
    }
}
