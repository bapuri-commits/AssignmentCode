package door;

public class Door2 extends Observable {//java.util.Observable 사용하지 않고 직접 적당히 옵저버 패턴용 인터페이스 만듦
    private DoorState state;
    public void setState(DoorState state){
        this.state = state;
        notifyObservers();
    }
    public void click(){
        state.click(this);
    }
    public void complete(){
        state.complete(this);
    }
    public void timeout(){
        state.timeout(this);
    }
    public Door2(){
        this.state = new ClosedState();
    }
}
