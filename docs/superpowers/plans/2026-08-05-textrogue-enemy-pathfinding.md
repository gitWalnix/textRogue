# Enemy Pathfinding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enemies within `CHASE_RADIUS` navigate around walls to reach the player using a real shortest path, and always take some step each turn when one is legally available (never freeze on a blocked straight-line approach).

**Architecture:** A new pure `PathFinder` class computes a BFS distance-from-player field over the whole dungeon once per `moveEnemies()` call. Each enemy then steps to whichever legal cardinal neighbor has the lowest distance value, replacing the old "single greedy axis step, skip turn if blocked" logic in `Game.moveEnemies()`.

**Tech Stack:** Java 21, Maven, JUnit 5 (`org.junit.jupiter`).

## Global Constraints

- No diagonal enemy movement — cardinal (up/down/left/right) only, matching existing movement.
- `CHASE_RADIUS` gating (straight-line Chebyshev distance, unchanged) still decides which enemies act each turn.
- No changes to enemy spawning, combat/damage rules, or player movement.
- The distance-field computation is unbounded (covers the whole 80x24 map) — do not add an early-exit radius cutoff.
- `Game.chaseStep(int, int, int, int)` keeps its exact existing signature and behavior — it's reused only as a tie-break, and all existing `chaseStep` tests must keep passing unmodified.

---

### Task 1: `PathFinder` — BFS distance field

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/world/PathFinder.java`
- Test: `src/test/java/ca/ninja/thenix/textrogue/world/PathFinderTest.java`

**Interfaces:**
- Produces: `PathFinder.UNREACHABLE` (`public static final int`, value `-1`) and
  `public static int[][] distanceField(Dungeon dungeon, int fromX, int fromY)` —
  returns a `Dungeon.HEIGHT x Dungeon.WIDTH` grid (indexed `[y][x]`, matching
  `Dungeon`'s own `tiles[y][x]` convention) of BFS tile-step distances from
  `(fromX, fromY)` over 4-directional neighbors, treating any tile where
  `dungeon.getTile(x, y) == Tile.WALL` as impassable. Tiles with no path from the
  source are `UNREACHABLE`. The source tile itself is `0`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/ca/ninja/thenix/textrogue/world/PathFinderTest.java`:

```java
package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PathFinderTest {

    private Dungeon dungeonWithFloors(int... coords) {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        for (int i = 0; i < coords.length - 1; i += 2) {
            tiles[coords[i + 1]][coords[i]] = Tile.FLOOR;
        }
        return new Dungeon(tiles, List.of(), coords[0], coords[1]);
    }

    @Test
    void sourceTileHasDistanceZero() {
        Dungeon dungeon = dungeonWithFloors(40, 12);
        int[][] distances = PathFinder.distanceField(dungeon, 40, 12);
        assertEquals(0, distances[12][40]);
    }

    @Test
    void straightCorridorDistancesIncreaseByOnePerStep() {
        Dungeon dungeon = dungeonWithFloors(40, 12, 41, 12, 42, 12);
        int[][] distances = PathFinder.distanceField(dungeon, 40, 12);
        assertEquals(1, distances[12][41]);
        assertEquals(2, distances[12][42]);
    }

    @Test
    void distancesRouteAroundACorner() {
        // (40,12) -> (40,11) -> (41,11): an L-shaped corridor. There is no
        // direct floor connection along the row from (40,12) to (41,11).
        Dungeon dungeon = dungeonWithFloors(40, 12, 40, 11, 41, 11);
        int[][] distances = PathFinder.distanceField(dungeon, 40, 12);
        assertEquals(1, distances[11][40]);
        assertEquals(2, distances[11][41]);
    }

    @Test
    void tilesWithNoPathToSourceAreUnreachable() {
        // (44,12) is floor but has no connecting floor tiles back to (40,12).
        Dungeon dungeon = dungeonWithFloors(40, 12, 44, 12);
        int[][] distances = PathFinder.distanceField(dungeon, 40, 12);
        assertEquals(PathFinder.UNREACHABLE, distances[12][44]);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail to compile (PathFinder doesn't exist yet)**

Run: `mvn -q -Dtest=PathFinderTest test`
Expected: compilation failure — `cannot find symbol: class PathFinder`

- [ ] **Step 3: Create `PathFinder`**

Create `src/main/java/ca/ninja/thenix/textrogue/world/PathFinder.java`:

```java
package ca.ninja.thenix.textrogue.world;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class PathFinder {
    public static final int UNREACHABLE = -1;

    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};

    public static int[][] distanceField(Dungeon dungeon, int fromX, int fromY) {
        int[][] distances = new int[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (int[] row : distances) Arrays.fill(row, UNREACHABLE);
        distances[fromY][fromX] = 0;

        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{fromX, fromY});

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int cx = current[0];
            int cy = current[1];
            int nextDistance = distances[cy][cx] + 1;
            for (int i = 0; i < DX.length; i++) {
                int nx = cx + DX[i];
                int ny = cy + DY[i];
                if (dungeon.getTile(nx, ny) == Tile.WALL) continue;
                if (distances[ny][nx] != UNREACHABLE) continue;
                distances[ny][nx] = nextDistance;
                queue.add(new int[]{nx, ny});
            }
        }
        return distances;
    }
}
```

Note: no explicit bounds check is needed before the `distances[ny][nx]` accesses —
`Dungeon.getTile` already returns `Tile.WALL` for any out-of-bounds coordinate, so
out-of-bounds neighbors are always filtered out by the `== Tile.WALL` check before
the array is ever indexed with them.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=PathFinderTest test`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/world/PathFinder.java src/test/java/ca/ninja/thenix/textrogue/world/PathFinderTest.java
git commit -m "feat: add PathFinder BFS distance field"
```

---

### Task 2: Route enemy movement through `PathFinder`

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java:176-208` (`moveEnemies`, `tileKey`)
- Test: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `PathFinder.distanceField(Dungeon, int, int)` and `PathFinder.UNREACHABLE` from Task 1; `Game.chaseStep(int, int, int, int)` (existing, unchanged).
- Produces: no new public/package interface — this task only changes `moveEnemies()`'s internal behavior. Existing package-private fields/methods (`enemies`, `gameTick()`) that tests already use are unchanged.

- [ ] **Step 1: Write the failing tests**

Add these tests to `src/test/java/ca/ninja/thenix/textrogue/GameTest.java` (anywhere
among the other enemy-movement tests, e.g. right after `enemyOutsideRadiusDoesNotMove`):

```java
@Test
void enemyRoutesAroundWallToReachPlayer() {
    // Direct path along row 12 is blocked at (41,12). The only route is the
    // detour through row 11.
    Dungeon dungeon = dungeonWithFloorAt(40, 12,
            40, 11, 41, 11, 42, 11, 42, 12);
    Player player = new Player(40, 12);
    Game game = gameWithInjectedState(dungeon, player);
    Enemy enemy = new Enemy(42, 12, 4);
    game.enemies.add(enemy);
    game.gameTick();
    assertEquals(42, enemy.getX());
    assertEquals(11, enemy.getY());
}

@Test
void enemyEventuallyReachesPlayerAfterRoutingAroundWall() {
    Dungeon dungeon = dungeonWithFloorAt(40, 12,
            40, 11, 41, 11, 42, 11, 42, 12);
    Player player = new Player(40, 12);
    Game game = gameWithInjectedState(dungeon, player);
    Enemy enemy = new Enemy(42, 12, 4);
    game.enemies.add(enemy);
    int startHp = player.getHp();
    for (int i = 0; i < 10; i++) game.gameTick();
    assertTrue(player.getHp() < startHp,
            "enemy should have routed around the wall and reached the player");
}

@Test
void enemyIsolatedFromPlayerStillMovesInsteadOfFreezing() {
    // (44,12) and (45,12) are a two-tile pocket with no floor connection
    // back to the player at (40,12) - fully unreachable, but still within
    // CHASE_RADIUS by straight-line distance.
    Dungeon dungeon = dungeonWithFloorAt(40, 12, 44, 12, 45, 12);
    Player player = new Player(40, 12);
    Game game = gameWithInjectedState(dungeon, player);
    Enemy enemy = new Enemy(44, 12, 4);
    game.enemies.add(enemy);
    game.gameTick();
    assertEquals(45, enemy.getX());
    assertEquals(12, enemy.getY());
}

@Test
void enemyBoxedInOnAllSidesDoesNotMove() {
    // (44,12) is a single isolated floor tile - every neighbor is a wall.
    Dungeon dungeon = dungeonWithFloorAt(40, 12, 44, 12);
    Player player = new Player(40, 12);
    Game game = gameWithInjectedState(dungeon, player);
    Enemy enemy = new Enemy(44, 12, 4);
    game.enemies.add(enemy);
    game.gameTick();
    assertEquals(44, enemy.getX());
    assertEquals(12, enemy.getY());
}
```

- [ ] **Step 2: Run tests to verify `enemyRoutesAroundWallToReachPlayer` and `enemyEventuallyReachesPlayerAfterRoutingAroundWall` fail**

Run: `mvn -q -Dtest=GameTest test`
Expected: FAIL — `enemyRoutesAroundWallToReachPlayer` fails because the enemy is
still at `(42, 12)` (frozen by the wall at `(41,12)` under the old greedy logic).
`enemyEventuallyReachesPlayerAfterRoutingAroundWall` fails for the same reason (hp
never drops). `enemyIsolatedFromPlayerStillMovesInsteadOfFreezing` fails because the
enemy stays at `(44,12)`. `enemyBoxedInOnAllSidesDoesNotMove` already passes (nothing
to fix there — it exercises the "genuinely stuck" fallback which already worked).

- [ ] **Step 3: Replace `moveEnemies()` and add `bestStep()`**

In `src/main/java/ca/ninja/thenix/textrogue/Game.java`, add the import (alongside
the existing `ca.ninja.thenix.textrogue.world.*` imports):

```java
import ca.ninja.thenix.textrogue.world.PathFinder;
```

Replace the existing `moveEnemies()` method (lines 176-204) with:

```java
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
```

`tileKey` (lines 206-208) is unchanged — leave it exactly as-is.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test`
Expected: PASS — full suite, including all pre-existing tests (chaseStep behavior,
spawn, projectile, death/restart tests) and all four new tests from Step 1.

- [ ] **Step 5: Update `TODO.txt`**

Remove the now-fixed line from `TODO.txt`:

```
Enemies get stuck on walls, no pathfinding
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java TODO.txt
git commit -m "feat: route enemy chase movement through PathFinder"
```
