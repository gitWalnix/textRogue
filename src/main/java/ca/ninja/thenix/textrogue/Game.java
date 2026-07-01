package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.gen.DungeonGenerator;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;

public class Game {
    private final InputHandler input;
    private final Renderer renderer;
    Dungeon dungeon;
    Player player;
    private boolean running;

    public Game(InputHandler input, Renderer renderer) {
        this.input = input;
        this.renderer = renderer;
    }

    public void start() {
        dungeon = new DungeonGenerator(System.currentTimeMillis()).generate();
        player = new Player(dungeon.getStartX(), dungeon.getStartY());
        running = true;

        renderer.render(dungeon, player);
        while (running) {
            Command cmd = input.pollCommand();
            processCommand(cmd);
            renderer.render(dungeon, player);
        }
    }

    void processCommand(Command cmd) {
        switch (cmd) {
            case MOVE_UP    -> tryMove(player.getX(), player.getY() - 1);
            case MOVE_DOWN  -> tryMove(player.getX(), player.getY() + 1);
            case MOVE_LEFT  -> tryMove(player.getX() - 1, player.getY());
            case MOVE_RIGHT -> tryMove(player.getX() + 1, player.getY());
            case QUIT       -> running = false;
            case UNKNOWN    -> {}
        }
    }

    private void tryMove(int nx, int ny) {
        Tile target = dungeon.getTile(nx, ny);
        if (target == Tile.WALL) return;
        player.moveTo(nx, ny);
        if (target == Tile.STAIRS_DOWN) descend();
    }

    private void descend() {
        player.descendFloor();
        dungeon = new DungeonGenerator(System.currentTimeMillis() + player.getFloor()).generate();
        player.moveTo(dungeon.getStartX(), dungeon.getStartY());
    }

    boolean isRunning() { return running; }
}
