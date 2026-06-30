package ca.ninja.thenix.textrogue.gen;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DungeonGeneratorTest {
    private boolean dungeonContains(Dungeon dungeon, Tile target) {
        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                if (dungeon.getTile(x, y) == target) return true;
            }
        }
        return false;
    }

    @Test
    void generatedDungeonHasFloorTiles() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertTrue(dungeonContains(dungeon, Tile.FLOOR));
    }

    @Test
    void generatedDungeonHasStairsDown() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertTrue(dungeonContains(dungeon, Tile.STAIRS_DOWN));
    }

    @Test
    void startPositionIsOnFloor() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertEquals(Tile.FLOOR, dungeon.getTile(dungeon.getStartX(), dungeon.getStartY()));
    }

    @Test
    void generatedDungeonHasAtLeastEightRooms() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertTrue(dungeon.getRooms().size() >= 8);
    }

    @Test
    void startPositionIsWithinBounds() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertTrue(dungeon.isInBounds(dungeon.getStartX(), dungeon.getStartY()));
    }
}
