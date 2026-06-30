package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileTest {
    @Test
    void wallRendersAsHash() {
        assertEquals('#', Tile.WALL.toChar());
    }

    @Test
    void floorRendersAsDot() {
        assertEquals('.', Tile.FLOOR.toChar());
    }

    @Test
    void stairsRendersAsGreaterThan() {
        assertEquals('>', Tile.STAIRS_DOWN.toChar());
    }
}
