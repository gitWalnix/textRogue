package ca.ninja.thenix.textrogue.vision;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import java.util.HashSet;
import java.util.Set;

public class RadiusVisibilityCalculator implements VisibilityCalculator {
    private final int radius;

    public RadiusVisibilityCalculator(int radius) {
        this.radius = radius;
    }

    @Override
    public Set<Position> computeVisibleTiles(Dungeon dungeon, int playerX, int playerY) {
        Set<Position> visible = new HashSet<>();
        for (int y = playerY - radius; y <= playerY + radius; y++) {
            for (int x = playerX - radius; x <= playerX + radius; x++) {
                if (!dungeon.isInBounds(x, y)) continue;
                if (Math.max(Math.abs(x - playerX), Math.abs(y - playerY)) > radius) continue;
                visible.add(new Position(x, y));
            }
        }
        return visible;
    }
}
