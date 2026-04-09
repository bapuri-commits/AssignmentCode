package makeagraph.graph;

public interface IBatchAppendable<E> extends IDataAppendable<E> {
    void suspendObservers();
    void resumeObservers();
}
