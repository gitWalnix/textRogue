package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PathFinderTest {

    private Dungeon dungeonWithFloors(int... coords) {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        for (int i = 0; i < coords.length - 1; i += 2) {
            tiles[coords[i + 1]][coords[i]] = Tile.FLOOR;
        }
        return new Dungeon(tiles, List.of(), coords[0], coords[1]);
    }

    @Test
    void sourceTileHasDistanceZero() {
        Dungeon dungeon = dungeonWithFloors(40, 12);
        int[][] distances = PathFinder.distanceField(dungeon, 40, 12);
        assertEquals(0, distances[12][40]);
    }

    @Test
    void straightCorridorDistancesIncreaseByOnePerStep() {
        Dungeon dungeon = dungeonWithFloors(40, 12, 41, 12, 42, 12);
        int[][] distances = PathFinder.distanceField(dungeon, 40, 12);
        assertEquals(1, distances[12][41]);
        assertEquals(2, distances[12][42]);
    }

    @Test
    void distancesRouteAroundACorner() {
        // (40,12) -> (40,11) -> (41,11): an L-shaped corridor. There is no
        // direct floor connection along the row from (40,12) to (41,11).
        Dungeon dungeon = dungeonWithFloors(40, 12, 40, 11, 41, 11);
        int[][] distances = PathFinder.distanceField(dungeon, 40, 12);
        assertEquals(1, distances[11][40]);
        assertEquals(2, distances[11][41]);
    }

    @Test
    void tilesWithNoPathToSourceAreUnreachable() {
        // (44,12) is floor but has no connecting floor tiles back to (40,12).
        Dungeon dungeon = dungeonWithFloors(40, 12, 44, 12);
        int[][] distances = PathFinder.distanceField(dungeon, 40, 12);
        assertEquals(PathFinder.UNREACHABLE, distances[12][44]);
    }
}
