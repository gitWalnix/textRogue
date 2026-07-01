package ca.ninja.thenix.textrogue.entity;

public class Enemy {
    private int x;
    private int y;
    private int hp;
    private boolean alive;

    public Enemy(int x, int y, int hp) {
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.alive = true;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getHp() { return hp; }
    public boolean isAlive() { return alive; }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
    }
}
