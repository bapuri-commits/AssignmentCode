package door;

public interface DoorState {
    public void click(Door2 door);
    public void complete(Door2 door);
    public void timeout(Door2 door);
}
