package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Facing;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.gen.DungeonGenerator;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Room;
import ca.ninja.thenix.textrogue.world.Tile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    static final int TICK_MS = 200;
    static final int MIN_ENEMIES = 3;
    static final int MAX_ENEMIES = 5;
    static final int ENEMY_HP = 4;
    static final int PROJECTILE_RANGE = 5;
    static final int SHOOT_COOLDOWN_TICKS = 5;

    private final InputHandler input;
    private final Renderer renderer;
    Dungeon dungeon;
    Player player;
    List<Enemy> enemies = new ArrayList<>();
    List<Projectile> projectiles = new ArrayList<>();
    private final Random random = new Random();
    private boolean running;
    private int shootCooldown = 0;

    public Game(InputHandler input, Renderer renderer) {
        this.input = input;
        this.renderer = renderer;
    }

    public void start() {
        dungeon = new DungeonGenerator(System.currentTimeMillis()).generate();
        player = new Player(dungeon.getStartX(), dungeon.getStartY());
        spawnEnemies();
        running = true;
        renderer.render(dungeon, player, enemies, projectiles);
        while (running) {
            long tickStart = System.currentTimeMillis();
            processCommand(input.pollCommand());
            gameTick();
            renderer.render(dungeon, player, enemies, projectiles);
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
            case MOVE_UP    -> { player.setFacing(Facing.UP);    tryMove(player.getX(), player.getY() - 1); }
            case MOVE_DOWN  -> { player.setFacing(Facing.DOWN);  tryMove(player.getX(), player.getY() + 1); }
            case MOVE_LEFT  -> { player.setFacing(Facing.LEFT);  tryMove(player.getX() - 1, player.getY()); }
            case MOVE_RIGHT -> { player.setFacing(Facing.RIGHT); tryMove(player.getX() + 1, player.getY()); }
            case SHOOT      -> shoot();
            case QUIT       -> running = false;
            case UNKNOWN    -> {}
        }
    }

    private void shoot() {
        if (shootCooldown > 0) return;
        shootCooldown = SHOOT_COOLDOWN_TICKS;
        Facing facing = player.getFacing();
        int px = player.getX() + facing.dx();
        int py = player.getY() + facing.dy();
        projectiles.add(new Projectile(px, py, facing, PROJECTILE_RANGE));
    }

    private Enemy enemyAt(int x, int y) {
        for (Enemy e : enemies) {
            if (e.isAlive() && e.getX() == x && e.getY() == y) return e;
        }
        return null;
    }

    void gameTick() {}

    void spawnEnemies() {
        enemies.clear();
        projectiles.clear();
        List<Room> candidates = dungeon.getRooms().stream()
                .filter(r -> r.centerX() != dungeon.getStartX() || r.centerY() != dungeon.getStartY())
                .toList();
        if (candidates.isEmpty()) return;
        int count = MIN_ENEMIES + random.nextInt(MAX_ENEMIES - MIN_ENEMIES + 1);
        for (int i = 0; i < count; i++) {
            Room room = candidates.get(random.nextInt(candidates.size()));
            enemies.add(new Enemy(room.centerX(), room.centerY(), ENEMY_HP));
        }
    }

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
        if (enemyAt(nx, ny) != null) return;
        player.moveTo(nx, ny);
        if (target == Tile.STAIRS_DOWN) descend();
    }

    private void descend() {
        player.descendFloor();
        dungeon = new DungeonGenerator(System.currentTimeMillis() + player.getFloor()).generate();
        player.moveTo(dungeon.getStartX(), dungeon.getStartY());
        spawnEnemies();
    }

    boolean isRunning() { return running; }
}
