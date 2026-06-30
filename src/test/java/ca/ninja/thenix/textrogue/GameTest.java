package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private static final PrintStream NULL_OUT = new PrintStream(OutputStream.nullOutputStream());

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
        InputHandler input = new InputHandler(new ByteArrayInputStream(new byte[0]));
        Game game = new Game(input, new Renderer(NULL_OUT));
        game.dungeon = dungeon;
        game.player = player;
        return game;
    }

    @Test
    void quitCommandStopsLoop() {
        InputHandler input = new InputHandler(new ByteArrayInputStream("q\n".getBytes()));
        Game game = new Game(input, new Renderer(NULL_OUT));
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
}
