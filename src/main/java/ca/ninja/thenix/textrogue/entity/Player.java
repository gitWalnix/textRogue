package ca.ninja.thenix.textrogue.entity;

public class Player {
    private static final int MAX_HP = 20;

    private int x;
    private int y;
    private int floor;
    private int hp;
    private Facing facing;
    private int potionCount;
    private int killCount;
    private int potionsUsed;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.floor = 1;
        this.hp = MAX_HP;
        this.facing = Facing.DOWN;
        this.potionCount = 0;
        this.killCount = 0;
        this.potionsUsed = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getFloor() { return floor; }
    public int getHp() { return hp; }
    public int getMaxHp() { return MAX_HP; }
    public Facing getFacing() { return facing; }
    public int getPotionCount() { return potionCount; }
    public int getKillCount() { return killCount; }
    public int getPotionsUsed() { return potionsUsed; }

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

    public void heal(int amount) {
        hp = Math.min(MAX_HP, hp + amount);
    }

    public void addPotion() {
        potionCount++;
    }

    public void recordKill() {
        killCount++;
    }

    public boolean drinkPotion(int healAmount) {
        if (potionCount <= 0) return false;
        potionCount--;
        potionsUsed++;
        heal(healAmount);
        return true;
    }
}
