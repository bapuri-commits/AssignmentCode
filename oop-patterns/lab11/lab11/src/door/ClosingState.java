package door;

public class ClosingState implements DoorState {
    @Override
    public void click(Door2 door){
        door.setState(new OpeningState());
    }
    @Override
    public void complete(Door2 door){
        door.setState(new ClosedState());
    }
    @Override
    public void timeout(Door2 door){
        door.setState(new ClosingState());
    }
}
