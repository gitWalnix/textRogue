package ca.ninja.thenix.textrogue.world;

public enum Tile {
    WALL, FLOOR, STAIRS_DOWN, POTION;

    public char toChar() {
        return switch (this) {
            case WALL -> '#';
            case FLOOR -> '.';
            case STAIRS_DOWN -> '>';
            case POTION -> '!';
        };
    }
}
