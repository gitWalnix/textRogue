package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Facing;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.gen.DungeonGenerator;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.vision.RadiusVisibilityCalculator;
import ca.ninja.thenix.textrogue.vision.VisibilityCalculator;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.PathFinder;
import ca.ninja.thenix.textrogue.world.Position;
import ca.ninja.thenix.textrogue.world.Room;
import ca.ninja.thenix.textrogue.world.Tile;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Game {
    static final int TICK_MS = 200;
    static final int MIN_ENEMIES = 3;
    static final int MAX_ENEMIES = 5;
    static final int ENEMY_HP = 2;
    static final int PROJECTILE_RANGE = 5;
    static final int SHOOT_COOLDOWN_TICKS = 5;
    static final int CHASE_RADIUS = 6;
    static final int ENEMY_CONTACT_DAMAGE = 2;
    static final int PROJECTILE_DAMAGE = 2;
    static final int VISIBILITY_RADIUS = 8;
    static final double POTION_DROP_CHANCE = 0.33;
    static final int POTION_HEAL_AMOUNT = 10;

    private final InputHandler input;
    private final Renderer renderer;
    Dungeon dungeon;
    Player player;
    List<Enemy> enemies = new ArrayList<>();
    List<Projectile> projectiles = new ArrayList<>();
    private final Random random = new Random();
    private final VisibilityCalculator visibilityCalculator = new RadiusVisibilityCalculator(VISIBILITY_RADIUS);
    private boolean running;
    private int shootCooldown = 0;

    public Game(InputHandler input, Renderer renderer) {
        this.input = input;
        this.renderer = renderer;
    }

    public void start() {
        if (dungeon == null) newRun();
        running = true;
        renderer.render(dungeon, player, enemies, projectiles, updateVisibility());
        while (running) {
            long tickStart = System.currentTimeMillis();
            processCommand(input.pollCommand());
            gameTick();
            renderer.render(dungeon, player, enemies, projectiles, updateVisibility());
            if (player.isDead()) {
                renderer.renderDeath(player);
                Command choice = awaitDeathChoice();
                if (choice == Command.RESTART) {
                    newRun();
                    renderer.render(dungeon, player, enemies, projectiles, updateVisibility());
                    continue;
                }
                running = false;
                break;
            }
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

    private Command awaitDeathChoice() {
        while (true) {
            Command cmd = input.pollCommand();
            if (cmd == Command.RESTART || cmd == Command.QUIT) return cmd;
            try {
                Thread.sleep(TICK_MS);
            } catch (InterruptedException e) {
                return Command.QUIT;
            }
        }
    }

    private void newRun() {
        dungeon = new DungeonGenerator(System.currentTimeMillis()).generate();
        player = new Player(dungeon.getStartX(), dungeon.getStartY());
        spawnEnemies();
        shootCooldown = 0;
    }

    void processCommand(Command cmd) {
        switch (cmd) {
            case MOVE_UP      -> { player.setFacing(Facing.UP);    tryMove(player.getX(), player.getY() - 1); }
            case MOVE_DOWN    -> { player.setFacing(Facing.DOWN);  tryMove(player.getX(), player.getY() + 1); }
            case MOVE_LEFT    -> { player.setFacing(Facing.LEFT);  tryMove(player.getX() - 1, player.getY()); }
            case MOVE_RIGHT   -> { player.setFacing(Facing.RIGHT); tryMove(player.getX() + 1, player.getY()); }
            case SHOOT        -> shoot();
            case DRINK_POTION -> player.drinkPotion(POTION_HEAL_AMOUNT);
            case RESTART      -> {} // only meaningful on the death screen; see Game.awaitDeathChoice (Task 3)
            case QUIT         -> running = false;
            case UNKNOWN      -> {}
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

    void gameTick() {
        if (shootCooldown > 0) shootCooldown--;
        moveEnemies();
        advanceProjectiles();
    }

    private void advanceProjectiles() {
        java.util.Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            Tile tile = dungeon.getTile(p.getX(), p.getY());
            if (tile == Tile.WALL) {
                it.remove();
                continue;
            }
            Enemy hit = enemyAt(p.getX(), p.getY());
            if (hit != null) {
                hit.takeDamage(PROJECTILE_DAMAGE);
                if (!hit.isAlive()) {
                    player.recordKill();
                    maybeDropPotion(hit.getX(), hit.getY());
                }
                it.remove();
                continue;
            }
            if (p.getRemainingRange() <= 0) {
                it.remove();
                continue;
            }
            p.advance();
        }
    }

    private void maybeDropPotion(int x, int y) {
        if (dungeon.getTile(x, y) != Tile.FLOOR) return;
        if (random.nextDouble() < POTION_DROP_CHANCE) {
            dungeon.setTile(x, y, Tile.POTION);
        }
    }

    private void moveEnemies() {
        java.util.Set<Long> occupied = new java.util.HashSet<>();
        for (Enemy e : enemies) {
            if (e.isAlive()) occupied.add(tileKey(e.getX(), e.getY()));
        }

        int[][] distances = PathFinder.distanceField(dungeon, player.getX(), player.getY());

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            int distX = Math.abs(enemy.getX() - player.getX());
            int distY = Math.abs(enemy.getY() - player.getY());
            if (Math.max(distX, distY) > CHASE_RADIUS) continue;

            int[] step = bestStep(enemy.getX(), enemy.getY(), distances, occupied);
            if (step == null) continue;

            int nx = enemy.getX() + step[0];
            int ny = enemy.getY() + step[1];

            if (nx == player.getX() && ny == player.getY()) {
                player.takeDamage(ENEMY_CONTACT_DAMAGE);
                continue;
            }

            occupied.remove(tileKey(enemy.getX(), enemy.getY()));
            occupied.add(tileKey(nx, ny));
            enemy.moveTo(nx, ny);
        }
    }

    private static final int[][] CARDINAL_DIRECTIONS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    private int[] bestStep(int x, int y, int[][] distances, java.util.Set<Long> occupied) {
        List<int[]> candidates = new ArrayList<>();
        for (int[] dir : CARDINAL_DIRECTIONS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (dungeon.getTile(nx, ny) == Tile.WALL) continue;
            if (occupied.contains(tileKey(nx, ny))) continue;
            candidates.add(dir);
        }
        if (candidates.isEmpty()) return null;

        int bestRank = Integer.MAX_VALUE;
        for (int[] dir : candidates) {
            bestRank = Math.min(bestRank, rank(distances[y + dir[1]][x + dir[0]]));
        }
        if (bestRank > rank(distances[y][x])) return null;

        int[] tieBreak = chaseStep(x, y, player.getX(), player.getY());
        int[] fallback = null;
        for (int[] dir : candidates) {
            if (rank(distances[y + dir[1]][x + dir[0]]) != bestRank) continue;
            if (fallback == null) fallback = dir;
            if (dir[0] == tieBreak[0] && dir[1] == tieBreak[1]) return dir;
        }
        return fallback;
    }

    private static int rank(int distance) {
        return distance == PathFinder.UNREACHABLE ? Integer.MAX_VALUE : distance;
    }

    private static long tileKey(int x, int y) {
        return ((long) y << 32) | (x & 0xffffffffL);
    }

    Set<Position> updateVisibility() {
        Set<Position> visible = visibilityCalculator.computeVisibleTiles(dungeon, player.getX(), player.getY());
        for (Position p : visible) dungeon.markExplored(p.x(), p.y());
        return visible;
    }

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
            int enemyX = room.x() + random.nextInt(room.width());
            int enemyY = room.y() + random.nextInt(room.height());
            enemies.add(new Enemy(enemyX, enemyY, ENEMY_HP));
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
        if (target == Tile.POTION) {
            player.addPotion();
            dungeon.setTile(nx, ny, Tile.FLOOR);
        }
    }

    private void descend() {
        player.descendFloor();
        dungeon = new DungeonGenerator(System.currentTimeMillis() + player.getFloor()).generate();
        player.moveTo(dungeon.getStartX(), dungeon.getStartY());
        spawnEnemies();
    }

    boolean isRunning() { return running; }
}
