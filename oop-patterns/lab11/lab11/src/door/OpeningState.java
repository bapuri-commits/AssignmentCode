package door;

public class OpeningState implements DoorState {
    @Override
    public void click(Door2 door){
        door.setState(new ClosingState());
    }
    @Override
    public void complete(Door2 door){
        door.setState(new OpenState());
    }
    @Override
    public void timeout(Door2 door){
        door.setState(new ClosingState());
    }
}
