package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import java.io.IOException;

public interface Renderer {
    void render(Dungeon dungeon, Player player);
    void close() throws IOException;
}
