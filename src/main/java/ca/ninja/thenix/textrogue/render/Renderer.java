package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.world.Dungeon;
import java.io.IOException;
import java.util.List;

public interface Renderer {
    void render(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles);
    void renderDeath(Player player);
    void close() throws IOException;
}
