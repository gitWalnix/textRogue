package ca.ninja.thenix.textrogue.world;

import java.util.Collections;
import java.util.List;

public class Dungeon {
    public static final int WIDTH = 80;
    public static final int HEIGHT = 24;

    private final Tile[][] tiles;
    private final List<Room> rooms;
    private final int startX;
    private final int startY;
    private final boolean[][] explored;

    public Dungeon(Tile[][] tiles, List<Room> rooms, int startX, int startY) {
        this.tiles = tiles;
        this.rooms = rooms;
        this.startX = startX;
        this.startY = startY;
        this.explored = new boolean[HEIGHT][WIDTH];
    }

    public Tile getTile(int x, int y) {
        if (!isInBounds(x, y)) return Tile.WALL;
        return tiles[y][x];
    }

    public void setTile(int x, int y, Tile tile) {
        if (isInBounds(x, y)) tiles[y][x] = tile;
    }

    public void markExplored(int x, int y) {
        if (isInBounds(x, y)) explored[y][x] = true;
    }

    public boolean isExplored(int x, int y) {
        if (!isInBounds(x, y)) return false;
        return explored[y][x];
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
}
