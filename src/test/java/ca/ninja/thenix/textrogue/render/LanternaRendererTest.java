package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Facing;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import ca.ninja.thenix.textrogue.world.Tile;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class LanternaRendererTest {

    private DefaultVirtualTerminal renderTo(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles, Set<Position> visibleTiles) throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        new LanternaRenderer(screen).render(dungeon, player, enemies, projectiles, visibleTiles);
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

    private Set<Position> allVisible() {
        Set<Position> visible = new HashSet<>();
        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                visible.add(new Position(x, y));
            }
        }
        return visible;
    }

    @Test
    void wallTileRendersAsHash() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(), List.of(), allVisible());
        assertEquals('#', t.getCharacter(new TerminalPosition(0, 0)).getCharacter());
    }

    @Test
    void floorTileRendersDot() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(2, 2, Tile.FLOOR), new Player(5, 5), List.of(), List.of(), allVisible());
        assertEquals('.', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }

    @Test
    void stairsDownRendersGreaterThan() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(4, 4, Tile.STAIRS_DOWN), new Player(5, 5), List.of(), List.of(), allVisible());
        assertEquals('>', t.getCharacter(new TerminalPosition(4, 4)).getCharacter());
    }

    @Test
    void playerRendersAsFacingGlyphDefaultingDown() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(3, 3), List.of(), List.of(), allVisible());
        assertEquals('v', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void playerRendersFacingGlyphWhenFacingUp() throws IOException {
        Player player = new Player(3, 3);
        player.setFacing(Facing.UP);
        DefaultVirtualTerminal t = renderTo(allWalls(), player, List.of(), List.of(), allVisible());
        assertEquals('^', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void statusLineShowsFloorNumber() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(0, 0), List.of(), List.of(), allVisible());
        assertEquals('F', t.getCharacter(new TerminalPosition(0, Dungeon.HEIGHT)).getCharacter());
    }

    @Test
    void statusLineShowsHp() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(0, 0), List.of(), List.of(), allVisible());
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < 30; x++) sb.append(t.getCharacter(new TerminalPosition(x, Dungeon.HEIGHT)).getCharacter());
        assertTrue(sb.toString().contains("HP: 20/20"));
    }

    @Test
    void enemyRendersAsE() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(new Enemy(3, 3, 4)), List.of(), allVisible());
        assertEquals('e', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void deadEnemyDoesNotRender() throws IOException {
        Enemy enemy = new Enemy(3, 3, 4);
        enemy.takeDamage(4);
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(enemy), List.of(), allVisible());
        assertEquals('#', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void projectileRendersAsStar() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(), List.of(new Projectile(2, 2, Facing.UP, 5)), allVisible());
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

    @Test
    void currentlyVisibleTileRendersNormalColor() throws IOException {
        Dungeon dungeon = withTileAt(2, 2, Tile.FLOOR);
        DefaultVirtualTerminal t = renderTo(dungeon, new Player(5, 5), List.of(), List.of(), Set.of(new Position(2, 2)));
        assertEquals('.', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
        assertEquals(TextColor.ANSI.WHITE, t.getCharacter(new TerminalPosition(2, 2)).getForegroundColor());
    }

    @Test
    void exploredButNotVisibleTileRendersDimmed() throws IOException {
        Dungeon dungeon = withTileAt(2, 2, Tile.FLOOR);
        dungeon.markExplored(2, 2);
        DefaultVirtualTerminal t = renderTo(dungeon, new Player(5, 5), List.of(), List.of(), Set.of());
        assertEquals('.', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
        assertEquals(TextColor.ANSI.BLACK_BRIGHT, t.getCharacter(new TerminalPosition(2, 2)).getForegroundColor());
    }

    @Test
    void unexploredTileRendersBlank() throws IOException {
        Dungeon dungeon = withTileAt(2, 2, Tile.FLOOR);
        DefaultVirtualTerminal t = renderTo(dungeon, new Player(5, 5), List.of(), List.of(), Set.of());
        assertEquals(' ', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }

    @Test
    void enemyOutsideVisibleTilesDoesNotRender() throws IOException {
        Dungeon dungeon = withTileAt(3, 3, Tile.FLOOR);
        dungeon.markExplored(3, 3);
        DefaultVirtualTerminal t = renderTo(dungeon, new Player(5, 5), List.of(new Enemy(3, 3, 4)), List.of(), Set.of());
        assertEquals('.', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void projectileOutsideVisibleTilesDoesNotRender() throws IOException {
        Dungeon dungeon = withTileAt(2, 2, Tile.FLOOR);
        dungeon.markExplored(2, 2);
        DefaultVirtualTerminal t = renderTo(dungeon, new Player(5, 5), List.of(), List.of(new Projectile(2, 2, Facing.UP, 5)), Set.of());
        assertEquals('.', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }

    @Test
    void potionTileRendersAsBang() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(2, 2, Tile.POTION), new Player(5, 5), List.of(), List.of(), allVisible());
        assertEquals('!', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }

    @Test
    void statusLineShowsPotionCount() throws IOException {
        Player player = new Player(0, 0);
        player.addPotion();
        player.addPotion();
        DefaultVirtualTerminal t = renderTo(allWalls(), player, List.of(), List.of(), allVisible());
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < 45; x++) sb.append(t.getCharacter(new TerminalPosition(x, Dungeon.HEIGHT)).getCharacter());
        assertTrue(sb.toString().contains("Potions: 2"));
    }

    private String rowText(DefaultVirtualTerminal terminal, int row) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < Dungeon.WIDTH; x++) sb.append(terminal.getCharacter(new TerminalPosition(x, row)).getCharacter());
        return sb.toString();
    }

    @Test
    void renderDeathShowsStatsAndRestartPrompt() throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        Player player = new Player(0, 0);
        player.descendFloor();
        player.descendFloor();
        player.recordKill();
        player.recordKill();
        player.recordKill();
        player.addPotion();
        player.drinkPotion(5);
        new LanternaRenderer(screen).renderDeath(player);
        int y = Dungeon.HEIGHT / 2;
        assertTrue(rowText(terminal, y).contains("You died"));
        assertTrue(rowText(terminal, y + 2).contains("Floor reached: 3"));
        assertTrue(rowText(terminal, y + 3).contains("Enemies killed: 3"));
        assertTrue(rowText(terminal, y + 4).contains("Potions used: 1"));
        assertTrue(rowText(terminal, y + 6).contains("Press [R] to restart, [Esc] to quit"));
    }
}
