package door;

public class ClosedState implements DoorState {
    @Override
    public void click(Door2 door){
        door.setState(new OpeningState());
    }
    @Override
    public void complete(Door2 door){
        // no-op: Closed 상태에서는 complete 무시
    }
    @Override
    public void timeout(Door2 door){
        door.setState(new ClosingState());
    }
}
