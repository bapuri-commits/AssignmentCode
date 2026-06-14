package door;

public class OpenState implements DoorState {
    @Override
    public void click(Door2 door){
        door.setState(new StayOpenState());
    }
    @Override
    public void complete(Door2 door){
        //no-op: OpenState 상황에서 complete호출은 아무 행동도 하지 않음
    }
    @Override
    public void timeout(Door2 door){
        door.setState(new ClosingState());
    }
}
