package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private static final Renderer NULL_RENDERER = new Renderer() {
        public void render(Dungeon d, Player p) {}
        public void close() {}
    };

    private Dungeon dungeonWithFloorAt(int px, int py, int... extraFloors) {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        tiles[py][px] = Tile.FLOOR;
        for (int i = 0; i < extraFloors.length - 1; i += 2) {
            tiles[extraFloors[i + 1]][extraFloors[i]] = Tile.FLOOR;
        }
        return new Dungeon(tiles, List.of(), px, py);
    }

    private Game gameWithInjectedState(Dungeon dungeon, Player player) {
        Game game = new Game(() -> Command.UNKNOWN, NULL_RENDERER);
        game.dungeon = dungeon;
        game.player = player;
        return game;
    }

    @Test
    void quitCommandStopsLoop() {
        Game game = new Game(() -> Command.QUIT, NULL_RENDERER);
        game.start();
        assertFalse(game.isRunning());
    }

    @Test
    void playerCannotMoveIntoWall() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.MOVE_UP);
        assertEquals(40, game.player.getX());
        assertEquals(12, game.player.getY());
    }

    @Test
    void playerCanMoveOntoFloor() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 40, 11);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.MOVE_UP);
        assertEquals(40, game.player.getX());
        assertEquals(11, game.player.getY());
    }

    @Test
    void steppingOnStairsIncrementsFloor() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        tiles[12][40] = Tile.FLOOR;
        tiles[11][40] = Tile.STAIRS_DOWN;
        Dungeon dungeon = new Dungeon(tiles, List.of(), 40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.MOVE_UP);
        assertEquals(2, game.player.getFloor());
    }

    @Test
    void chaseStepPrioritizesLargerAxisDistance() {
        assertArrayEquals(new int[]{1, 0}, Game.chaseStep(0, 0, 5, 1));
        assertArrayEquals(new int[]{0, 1}, Game.chaseStep(0, 0, 1, 5));
    }

    @Test
    void chaseStepMovesTowardNegativeDirection() {
        assertArrayEquals(new int[]{-1, 0}, Game.chaseStep(5, 0, 0, 0));
        assertArrayEquals(new int[]{0, -1}, Game.chaseStep(0, 5, 0, 0));
    }
}
