package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Facing;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Room;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private static final Renderer NULL_RENDERER = new Renderer() {
        public void render(Dungeon d, Player p, List<Enemy> enemies, List<Projectile> projectiles) {}
        public void renderDeath(Player p) {}
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

    @Test
    void spawnEnemiesPlacesBetween3And5EnemiesExcludingStartRoom() {
        Room startRoom = new Room(0, 0, 4, 4);
        Room otherRoom = new Room(10, 10, 4, 4);
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        Dungeon dungeon = new Dungeon(tiles, List.of(startRoom, otherRoom), startRoom.centerX(), startRoom.centerY());
        Game game = gameWithInjectedState(dungeon, new Player(startRoom.centerX(), startRoom.centerY()));
        game.spawnEnemies();
        assertTrue(game.enemies.size() >= 3 && game.enemies.size() <= 5);
        for (Enemy e : game.enemies) {
            assertEquals(otherRoom.centerX(), e.getX());
            assertEquals(otherRoom.centerY(), e.getY());
        }
    }

    @Test
    void descendingClearsOldEnemiesAndSpawnsNewOnes() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        tiles[12][40] = Tile.FLOOR;
        tiles[11][40] = Tile.STAIRS_DOWN;
        Dungeon dungeon = new Dungeon(tiles, List.of(), 40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        Enemy staleEnemy = new Enemy(99, 99, 4);
        game.enemies.add(staleEnemy);
        game.projectiles.add(new Projectile(1, 1, Facing.UP, 5));

        game.processCommand(Command.MOVE_UP);

        assertFalse(game.enemies.contains(staleEnemy));
        assertTrue(game.projectiles.isEmpty());
        assertTrue(game.enemies.size() >= 3 && game.enemies.size() <= 5);
    }

    @Test
    void movingUpdatesFacing() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 40, 11);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.MOVE_UP);
        assertEquals(Facing.UP, game.player.getFacing());
    }

    @Test
    void facingUpdatesEvenWhenMoveBlockedByWall() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.MOVE_UP);
        assertEquals(Facing.UP, game.player.getFacing());
        assertEquals(12, game.player.getY());
    }

    @Test
    void playerCannotMoveIntoLivingEnemy() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 40, 11);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.enemies.add(new Enemy(40, 11, 4));
        game.processCommand(Command.MOVE_UP);
        assertEquals(40, game.player.getX());
        assertEquals(12, game.player.getY());
    }

    @Test
    void shootSpawnsProjectileInFacingDirection() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        player.setFacing(Facing.UP);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.SHOOT);
        assertEquals(1, game.projectiles.size());
        Projectile p = game.projectiles.get(0);
        assertEquals(40, p.getX());
        assertEquals(11, p.getY());
        assertEquals(Facing.UP, p.getDirection());
    }

    @Test
    void shootOnCooldownDoesNothing() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.SHOOT);
        game.processCommand(Command.SHOOT);
        assertEquals(1, game.projectiles.size());
    }

    @Test
    void enemyWithinRadiusStepsTowardPlayer() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 41, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        Enemy enemy = new Enemy(42, 12, 4);
        game.enemies.add(enemy);
        game.gameTick();
        assertEquals(41, enemy.getX());
        assertEquals(12, enemy.getY());
    }

    @Test
    void enemyOutsideRadiusDoesNotMove() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 60, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        Enemy enemy = new Enemy(60, 12, 4);
        game.enemies.add(enemy);
        game.gameTick();
        assertEquals(60, enemy.getX());
    }

    @Test
    void adjacentEnemyDamagesPlayerInsteadOfMoving() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 41, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        Enemy enemy = new Enemy(41, 12, 4);
        game.enemies.add(enemy);
        int startHp = player.getHp();
        game.gameTick();
        assertEquals(startHp - 2, player.getHp());
        assertEquals(41, enemy.getX());
    }

    @Test
    void projectileDamagesEnemyInPath() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 41, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        Enemy enemy = new Enemy(41, 12, 4);
        game.enemies.add(enemy);
        game.projectiles.add(new Projectile(41, 12, Facing.RIGHT, 5));
        game.gameTick();
        assertEquals(2, enemy.getHp());
        assertTrue(game.projectiles.isEmpty());
    }

    @Test
    void projectileRemovedWhenHittingWall() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.projectiles.add(new Projectile(41, 12, Facing.RIGHT, 5));
        game.gameTick();
        assertTrue(game.projectiles.isEmpty());
    }

    @Test
    void projectileRemovedAfterExhaustingRange() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 41, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.projectiles.add(new Projectile(41, 12, Facing.RIGHT, 0));
        game.gameTick();
        assertTrue(game.projectiles.isEmpty());
    }

    @Test
    void projectileAdvancesWhenPathIsClear() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 41, 12, 42, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.projectiles.add(new Projectile(41, 12, Facing.RIGHT, 5));
        game.gameTick();
        assertEquals(1, game.projectiles.size());
        assertEquals(42, game.projectiles.get(0).getX());
    }

    @Test
    void playerDeathStopsLoopAndRendersDeathScreen() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        player.takeDamage(player.getHp());
        boolean[] deathRendered = {false};
        Renderer capturingRenderer = new Renderer() {
            public void render(Dungeon d, Player p, List<Enemy> e, List<Projectile> pr) {}
            public void renderDeath(Player p) { deathRendered[0] = true; }
            public void close() {}
        };
        Game game = new Game(() -> Command.UNKNOWN, capturingRenderer);
        game.dungeon = dungeon;
        game.player = player;
        long start = System.currentTimeMillis();
        game.start();
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(deathRendered[0]);
        assertFalse(game.isRunning());
        assertTrue(elapsed >= Game.DEATH_SCREEN_DISPLAY_MS,
                "death screen should stay visible for at least " + Game.DEATH_SCREEN_DISPLAY_MS + "ms, was " + elapsed);
    }
}
