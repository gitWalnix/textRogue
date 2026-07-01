package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LanternaRendererTest {

    private DefaultVirtualTerminal renderTo(Dungeon dungeon, Player player) throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        new LanternaRenderer(screen).render(dungeon, player);
        return terminal;
    }

    private Dungeon allWalls() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        return new Dungeon(tiles, List.of(), 0, 0);
    }

    private Dungeon withTileAt(int x, int y, Tile tile) {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        tiles[y][x] = tile;
        return new Dungeon(tiles, List.of(), x, y);
    }

    @Test
    void wallTileRendersAsHash() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5));
        assertEquals('#', t.getCharacter(new TerminalPosition(0, 0)).getCharacter());
    }

    @Test
    void floorTileRendersDot() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(2, 2, Tile.FLOOR), new Player(5, 5));
        assertEquals('.', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }

    @Test
    void stairsDownRendersGreaterThan() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(4, 4, Tile.STAIRS_DOWN), new Player(5, 5));
        assertEquals('>', t.getCharacter(new TerminalPosition(4, 4)).getCharacter());
    }

    @Test
    void playerRendersAsAt() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(3, 3));
        assertEquals('@', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void statusLineShowsFloorNumber() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(0, 0));
        // "Floor: 1" is written at row Dungeon.HEIGHT (= 24), column 0
        assertEquals('F', t.getCharacter(new TerminalPosition(0, Dungeon.HEIGHT)).getCharacter());
    }
}
