package makeagraph.session;

public interface ISession {
    void start();
    void stop();
    void await();
}
