package ca.ninja.thenix.textrogue.vision;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import java.util.Set;

public interface VisibilityCalculator {
    Set<Position> computeVisibleTiles(Dungeon dungeon, int playerX, int playerY);
}
