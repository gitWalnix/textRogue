package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RendererTest {
    private Dungeon emptyDungeon() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        return new Dungeon(tiles, List.of(), 0, 0);
    }

    private String renderToString(Dungeon dungeon, Player player) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new Renderer(new PrintStream(out)).render(dungeon, player);
        return out.toString();
    }

    @Test
    void outputContainsPlayerSymbol() {
        String output = renderToString(emptyDungeon(), new Player(0, 0));
        assertTrue(output.contains("@"));
    }

    @Test
    void outputContainsFloorNumberOne() {
        String output = renderToString(emptyDungeon(), new Player(0, 0));
        assertTrue(output.contains("Floor: 1"));
    }

    @Test
    void outputContainsUpdatedFloorNumber() {
        Player player = new Player(0, 0);
        player.descendFloor();
        player.descendFloor();
        String output = renderToString(emptyDungeon(), player);
        assertTrue(output.contains("Floor: 3"));
    }

    @Test
    void outputContainsWallCharacter() {
        String output = renderToString(emptyDungeon(), new Player(0, 0));
        assertTrue(output.contains("#"));
    }
}
