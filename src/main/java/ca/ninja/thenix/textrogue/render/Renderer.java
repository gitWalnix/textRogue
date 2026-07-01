package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface Renderer {
    void render(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles, Set<Position> visibleTiles);
    void renderDeath(Player player);
    void close() throws IOException;
}
