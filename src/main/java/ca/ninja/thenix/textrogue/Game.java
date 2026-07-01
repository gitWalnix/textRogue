package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.gen.DungeonGenerator;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;

public class Game {
    static final int TICK_MS = 200;

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
            long tickStart = System.currentTimeMillis();
            processCommand(input.pollCommand());
            gameTick();
            renderer.render(dungeon, player);
            long sleep = TICK_MS - (System.currentTimeMillis() - tickStart);
            if (running && sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
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

    void gameTick() {}

    static int[] chaseStep(int enemyX, int enemyY, int playerX, int playerY) {
        int dx = playerX - enemyX;
        int dy = playerY - enemyY;
        if (Math.abs(dx) >= Math.abs(dy)) {
            return new int[]{Integer.signum(dx), 0};
        } else {
            return new int[]{0, Integer.signum(dy)};
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
