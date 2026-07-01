package ca.ninja.thenix.textrogue.entity;

public class Player {
    private static final int MAX_HP = 20;

    private int x;
    private int y;
    private int floor;
    private int hp;
    private Facing facing;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.floor = 1;
        this.hp = MAX_HP;
        this.facing = Facing.DOWN;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getFloor() { return floor; }
    public int getHp() { return hp; }
    public int getMaxHp() { return MAX_HP; }
    public Facing getFacing() { return facing; }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void descendFloor() {
        floor++;
    }

    public void setFacing(Facing facing) {
        this.facing = facing;
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp < 0) hp = 0;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}
