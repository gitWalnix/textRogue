package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Facing;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
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

    private DefaultVirtualTerminal renderTo(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles) throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        new LanternaRenderer(screen).render(dungeon, player, enemies, projectiles);
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
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(), List.of());
        assertEquals('#', t.getCharacter(new TerminalPosition(0, 0)).getCharacter());
    }

    @Test
    void floorTileRendersDot() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(2, 2, Tile.FLOOR), new Player(5, 5), List.of(), List.of());
        assertEquals('.', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }

    @Test
    void stairsDownRendersGreaterThan() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(4, 4, Tile.STAIRS_DOWN), new Player(5, 5), List.of(), List.of());
        assertEquals('>', t.getCharacter(new TerminalPosition(4, 4)).getCharacter());
    }

    @Test
    void playerRendersAsFacingGlyphDefaultingDown() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(3, 3), List.of(), List.of());
        assertEquals('v', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void playerRendersFacingGlyphWhenFacingUp() throws IOException {
        Player player = new Player(3, 3);
        player.setFacing(Facing.UP);
        DefaultVirtualTerminal t = renderTo(allWalls(), player, List.of(), List.of());
        assertEquals('^', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void statusLineShowsFloorNumber() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(0, 0), List.of(), List.of());
        // "Floor: 1 ..." is written at row Dungeon.HEIGHT (= 24), column 0
        assertEquals('F', t.getCharacter(new TerminalPosition(0, Dungeon.HEIGHT)).getCharacter());
    }

    @Test
    void statusLineShowsHp() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(0, 0), List.of(), List.of());
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < 30; x++) sb.append(t.getCharacter(new TerminalPosition(x, Dungeon.HEIGHT)).getCharacter());
        assertTrue(sb.toString().contains("HP: 20/20"));
    }

    @Test
    void enemyRendersAsE() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(new Enemy(3, 3, 4)), List.of());
        assertEquals('e', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void deadEnemyDoesNotRender() throws IOException {
        Enemy enemy = new Enemy(3, 3, 4);
        enemy.takeDamage(4);
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(enemy), List.of());
        assertEquals('#', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void projectileRendersAsStar() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(), List.of(new Projectile(2, 2, Facing.UP, 5)));
        assertEquals('*', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }

    @Test
    void renderDeathShowsMessage() throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        new LanternaRenderer(screen).renderDeath(new Player(0, 0));
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < Dungeon.WIDTH; x++) sb.append(terminal.getCharacter(new TerminalPosition(x, Dungeon.HEIGHT / 2)).getCharacter());
        assertTrue(sb.toString().contains("You died"));
    }
}
