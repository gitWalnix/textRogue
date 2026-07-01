package ca.ninja.thenix.textrogue.entity;

public enum Facing {
    UP, DOWN, LEFT, RIGHT;

    public int dx() { return this == LEFT ? -1 : this == RIGHT ? 1 : 0; }
    public int dy() { return this == UP ? -1 : this == DOWN ? 1 : 0; }

    public char toChar() {
        return switch (this) {
            case UP -> '^';
            case DOWN -> 'v';
            case LEFT -> '<';
            case RIGHT -> '>';
        };
    }
}
