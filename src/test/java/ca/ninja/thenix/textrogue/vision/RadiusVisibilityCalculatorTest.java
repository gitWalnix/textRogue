package ca.ninja.thenix.textrogue.vision;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class RadiusVisibilityCalculatorTest {
    private Dungeon allWalls() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        return new Dungeon(tiles, List.of(), 0, 0);
    }

    @Test
    void tileExactlyAtRadiusIsIncluded() {
        RadiusVisibilityCalculator calculator = new RadiusVisibilityCalculator(3);
        Set<Position> visible = calculator.computeVisibleTiles(allWalls(), 10, 10);
        assertTrue(visible.contains(new Position(13, 10)));
    }

    @Test
    void tileOneBeyondRadiusIsExcluded() {
        RadiusVisibilityCalculator calculator = new RadiusVisibilityCalculator(3);
        Set<Position> visible = calculator.computeVisibleTiles(allWalls(), 10, 10);
        assertFalse(visible.contains(new Position(14, 10)));
    }

    @Test
    void playerOwnTileIsIncluded() {
        RadiusVisibilityCalculator calculator = new RadiusVisibilityCalculator(3);
        Set<Position> visible = calculator.computeVisibleTiles(allWalls(), 10, 10);
        assertTrue(visible.contains(new Position(10, 10)));
    }

    @Test
    void outOfBoundsTilesAreExcludedEvenWithinRadius() {
        RadiusVisibilityCalculator calculator = new RadiusVisibilityCalculator(5);
        Set<Position> visible = calculator.computeVisibleTiles(allWalls(), 0, 0);
        assertFalse(visible.contains(new Position(-2, 0)));
        assertFalse(visible.contains(new Position(0, -2)));
    }
}
