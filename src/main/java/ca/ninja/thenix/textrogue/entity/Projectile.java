package ca.ninja.thenix.textrogue.entity;

public class Projectile {
    private int x;
    private int y;
    private final Facing direction;
    private int remainingRange;

    public Projectile(int x, int y, Facing direction, int remainingRange) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.remainingRange = remainingRange;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public Facing getDirection() { return direction; }
    public int getRemainingRange() { return remainingRange; }

    public void advance() {
        x += direction.dx();
        y += direction.dy();
        remainingRange--;
    }
}
