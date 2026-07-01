package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DungeonTest {
    private Tile[][] allWalls() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        return tiles;
    }

    @Test
    void getTileReturnsSetTile() {
        Tile[][] tiles = allWalls();
        Dungeon dungeon = new Dungeon(tiles, List.of(), 0, 0);
        dungeon.setTile(5, 5, Tile.FLOOR);
        assertEquals(Tile.FLOOR, dungeon.getTile(5, 5));
    }

    @Test
    void outOfBoundsNegativeXReturnsWall() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertEquals(Tile.WALL, dungeon.getTile(-1, 0));
    }

    @Test
    void outOfBoundsNegativeYReturnsWall() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertEquals(Tile.WALL, dungeon.getTile(0, -1));
    }

    @Test
    void outOfBoundsWidthReturnsWall() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertEquals(Tile.WALL, dungeon.getTile(Dungeon.WIDTH, 0));
    }

    @Test
    void outOfBoundsHeightReturnsWall() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertEquals(Tile.WALL, dungeon.getTile(0, Dungeon.HEIGHT));
    }

    @Test
    void startPositionIsReturned() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 10, 7);
        assertEquals(10, dungeon.getStartX());
        assertEquals(7, dungeon.getStartY());
    }

    @Test
    void tilesStartUnexplored() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertFalse(dungeon.isExplored(5, 5));
    }

    @Test
    void markExploredMakesTileExplored() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        dungeon.markExplored(5, 5);
        assertTrue(dungeon.isExplored(5, 5));
        assertFalse(dungeon.isExplored(6, 5));
    }

    @Test
    void markExploredOutOfBoundsDoesNotThrow() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertDoesNotThrow(() -> dungeon.markExplored(-1, -1));
    }

    @Test
    void isExploredOutOfBoundsReturnsFalse() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertFalse(dungeon.isExplored(-1, 0));
        assertFalse(dungeon.isExplored(0, Dungeon.HEIGHT));
    }
}
