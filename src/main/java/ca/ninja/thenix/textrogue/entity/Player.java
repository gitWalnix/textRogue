package ca.ninja.thenix.textrogue.entity;

public class Player {
    private int x;
    private int y;
    private int floor;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.floor = 1;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getFloor() { return floor; }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void descendFloor() {
        floor++;
    }
}
