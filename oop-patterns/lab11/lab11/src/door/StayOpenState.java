package door;

public class StayOpenState implements DoorState {
    @Override
    public void click(Door2 door){
        door.setState(new ClosingState());
    }
    @Override
    public void complete(Door2 door){
        //no-op: StayOpenState에서는 complete를 호출해도 아무일도 벌어지지 않음.
    }
    @Override
    public void timeout(Door2 door){
        door.setState(new ClosingState());
    }
}
