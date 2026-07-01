# TextRogue Phase C Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fill in `Game.gameTick()` with enemies, player HP/facing, and cooldown-gated animated projectile combat, per `docs/superpowers/specs/2026-07-01-textrogue-phase-c-design.md`.

**Architecture:** Plain concrete `Enemy` and `Projectile` classes join the existing `Player` entity (no new abstraction layers). `Game` gains `enemies`/`projectiles` fields and drives their per-tick behavior from `gameTick()`. `Renderer`'s signature grows to carry the new entity lists plus a `renderDeath` method.

**Tech Stack:** Java 21, JUnit 5, Lanterna 3.1.1 (`DefaultVirtualTerminal` for headless render/input tests).

## Global Constraints

- Java 21 language level (existing `pom.xml` setting) — use switch expressions, records, etc. freely, matching existing code style.
- No new third-party dependencies.
- Follow existing code style: package-private fields/methods on `Game` for test seams (already established by `dungeon`/`player`), no mocking framework, plain JUnit 5 `@Test` methods.
- Balance constants (copied verbatim from the spec): player max HP 20, enemy HP 4, projectile damage 2, enemy contact damage 2, projectile range 5 tiles, projectile speed 1 tile/tick, shot cooldown 5 ticks, enemy chase radius 6 tiles (Chebyshev), enemies per floor random 3–5 in non-start rooms.
- Combat is ranged-only: enemies block player movement like a wall; bumping into one deals no damage.

---

### Task 1: Facing enum

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/entity/Facing.java`
- Test: `src/test/java/ca/ninja/thenix/textrogue/entity/FacingTest.java`

**Interfaces:**
- Produces: `Facing` enum with values `UP, DOWN, LEFT, RIGHT`; methods `int dx()`, `int dy()`, `char toChar()`. Used by `Player`, `Projectile`, `LanternaInputHandler` is not affected, `LanternaRenderer`.

- [ ] **Step 1: Write the failing test**

```java
package ca.ninja.thenix.textrogue.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FacingTest {
    @Test
    void deltasPointInCorrectDirection() {
        assertEquals(0, Facing.UP.dx());
        assertEquals(-1, Facing.UP.dy());
        assertEquals(0, Facing.DOWN.dx());
        assertEquals(1, Facing.DOWN.dy());
        assertEquals(-1, Facing.LEFT.dx());
        assertEquals(0, Facing.LEFT.dy());
        assertEquals(1, Facing.RIGHT.dx());
        assertEquals(0, Facing.RIGHT.dy());
    }

    @Test
    void toCharMatchesDirection() {
        assertEquals('^', Facing.UP.toChar());
        assertEquals('v', Facing.DOWN.toChar());
        assertEquals('<', Facing.LEFT.toChar());
        assertEquals('>', Facing.RIGHT.toChar());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=FacingTest`
Expected: FAIL (compilation error — `Facing` does not exist)

- [ ] **Step 3: Write minimal implementation**

```java
package ca.ninja.thenix.textrogue.entity;

public enum Facing {
    UP, DOWN, LEFT, RIGHT;

    public int dx() { return this == LEFT ? -1 : this == RIGHT ? 1 : 0; }
    public int dy() { return this == UP ? -1 : this == DOWN ? 1 : 0; }

    public char toChar() {
        return switch (this) {
            case UP -> '^';
            case DOWN -> 'v';
            case LEFT -> '<';
            case RIGHT -> '>';
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=FacingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/entity/Facing.java src/test/java/ca/ninja/thenix/textrogue/entity/FacingTest.java
git commit -m "feat: add Facing enum for player/projectile direction"
```

---

### Task 2: Player gains HP and facing

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/entity/Player.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java`

**Interfaces:**
- Consumes: `Facing` enum (Task 1).
- Produces: `Player.getHp()`, `getMaxHp()`, `getFacing()`, `setFacing(Facing)`, `takeDamage(int)`, `isDead()`. Default `hp = 20`, `maxHp = 20`, `facing = Facing.DOWN`.

- [ ] **Step 1: Write the failing tests**

Append to `PlayerTest.java` (add import `ca.ninja.thenix.textrogue.entity.Facing` is unnecessary since same package; just add these methods inside the existing `class PlayerTest { ... }`):

```java
    @Test
    void startsWithFullHp() {
        Player player = new Player(0, 0);
        assertEquals(20, player.getHp());
        assertEquals(20, player.getMaxHp());
    }

    @Test
    void startsFacingDown() {
        Player player = new Player(0, 0);
        assertEquals(Facing.DOWN, player.getFacing());
    }

    @Test
    void setFacingUpdatesFacing() {
        Player player = new Player(0, 0);
        player.setFacing(Facing.LEFT);
        assertEquals(Facing.LEFT, player.getFacing());
    }

    @Test
    void takeDamageReducesHp() {
        Player player = new Player(0, 0);
        player.takeDamage(5);
        assertEquals(15, player.getHp());
    }

    @Test
    void takeDamageClampsAtZero() {
        Player player = new Player(0, 0);
        player.takeDamage(999);
        assertEquals(0, player.getHp());
    }

    @Test
    void isDeadReflectsHp() {
        Player player = new Player(0, 0);
        assertFalse(player.isDead());
        player.takeDamage(20);
        assertTrue(player.isDead());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=PlayerTest`
Expected: FAIL (compilation error — `getHp`, `getFacing`, etc. do not exist)

- [ ] **Step 3: Write minimal implementation**

Replace the full contents of `Player.java`:

```java
package ca.ninja.thenix.textrogue.entity;

public class Player {
    private static final int MAX_HP = 20;

    private int x;
    private int y;
    private int floor;
    private int hp;
    private Facing facing;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.floor = 1;
        this.hp = MAX_HP;
        this.facing = Facing.DOWN;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getFloor() { return floor; }
    public int getHp() { return hp; }
    public int getMaxHp() { return MAX_HP; }
    public Facing getFacing() { return facing; }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void descendFloor() {
        floor++;
    }

    public void setFacing(Facing facing) {
        this.facing = facing;
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp < 0) hp = 0;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=PlayerTest`
Expected: PASS (10 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/entity/Player.java src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java
git commit -m "feat: add HP and facing to Player"
```

---

### Task 3: Enemy class

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/entity/Enemy.java`
- Test: `src/test/java/ca/ninja/thenix/textrogue/entity/EnemyTest.java`

**Interfaces:**
- Produces: `Enemy(int x, int y, int hp)`, `getX()`, `getY()`, `getHp()`, `isAlive()`, `moveTo(int, int)`, `takeDamage(int)`.

- [ ] **Step 1: Write the failing test**

```java
package ca.ninja.thenix.textrogue.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyTest {
    @Test
    void startsAliveAtGivenPositionAndHp() {
        Enemy enemy = new Enemy(3, 4, 4);
        assertEquals(3, enemy.getX());
        assertEquals(4, enemy.getY());
        assertEquals(4, enemy.getHp());
        assertTrue(enemy.isAlive());
    }

    @Test
    void moveToUpdatesPosition() {
        Enemy enemy = new Enemy(0, 0, 4);
        enemy.moveTo(1, 2);
        assertEquals(1, enemy.getX());
        assertEquals(2, enemy.getY());
    }

    @Test
    void takeDamageReducesHp() {
        Enemy enemy = new Enemy(0, 0, 4);
        enemy.takeDamage(2);
        assertEquals(2, enemy.getHp());
        assertTrue(enemy.isAlive());
    }

    @Test
    void takeDamageKillsAtZeroHp() {
        Enemy enemy = new Enemy(0, 0, 4);
        enemy.takeDamage(4);
        assertEquals(0, enemy.getHp());
        assertFalse(enemy.isAlive());
    }

    @Test
    void takeDamageClampsHpAtZero() {
        Enemy enemy = new Enemy(0, 0, 4);
        enemy.takeDamage(999);
        assertEquals(0, enemy.getHp());
        assertFalse(enemy.isAlive());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=EnemyTest`
Expected: FAIL (compilation error — `Enemy` does not exist)

- [ ] **Step 3: Write minimal implementation**

```java
package ca.ninja.thenix.textrogue.entity;

public class Enemy {
    private int x;
    private int y;
    private int hp;
    private boolean alive;

    public Enemy(int x, int y, int hp) {
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.alive = true;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getHp() { return hp; }
    public boolean isAlive() { return alive; }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=EnemyTest`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/entity/Enemy.java src/test/java/ca/ninja/thenix/textrogue/entity/EnemyTest.java
git commit -m "feat: add Enemy entity"
```

---

### Task 4: Projectile class

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/entity/Projectile.java`
- Test: `src/test/java/ca/ninja/thenix/textrogue/entity/ProjectileTest.java`

**Interfaces:**
- Consumes: `Facing` (Task 1).
- Produces: `Projectile(int x, int y, Facing direction, int remainingRange)`, `getX()`, `getY()`, `getDirection()`, `getRemainingRange()`, `advance()`.

- [ ] **Step 1: Write the failing test**

```java
package ca.ninja.thenix.textrogue.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectileTest {
    @Test
    void startsAtGivenPositionDirectionAndRange() {
        Projectile p = new Projectile(5, 5, Facing.RIGHT, 5);
        assertEquals(5, p.getX());
        assertEquals(5, p.getY());
        assertEquals(Facing.RIGHT, p.getDirection());
        assertEquals(5, p.getRemainingRange());
    }

    @Test
    void advanceMovesInDirectionAndDecrementsRange() {
        Projectile p = new Projectile(5, 5, Facing.RIGHT, 5);
        p.advance();
        assertEquals(6, p.getX());
        assertEquals(5, p.getY());
        assertEquals(4, p.getRemainingRange());
    }

    @Test
    void advanceUpMovesNegativeY() {
        Projectile p = new Projectile(5, 5, Facing.UP, 5);
        p.advance();
        assertEquals(5, p.getX());
        assertEquals(4, p.getY());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=ProjectileTest`
Expected: FAIL (compilation error — `Projectile` does not exist)

- [ ] **Step 3: Write minimal implementation**

```java
package ca.ninja.thenix.textrogue.entity;

public class Projectile {
    private int x;
    private int y;
    private final Facing direction;
    private int remainingRange;

    public Projectile(int x, int y, Facing direction, int remainingRange) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.remainingRange = remainingRange;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public Facing getDirection() { return direction; }
    public int getRemainingRange() { return remainingRange; }

    public void advance() {
        x += direction.dx();
        y += direction.dy();
        remainingRange--;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=ProjectileTest`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/entity/Projectile.java src/test/java/ca/ninja/thenix/textrogue/entity/ProjectileTest.java
git commit -m "feat: add Projectile entity"
```

---

### Task 5: SHOOT command wired to space bar

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/input/Command.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java`

**Interfaces:**
- Produces: `Command.SHOOT`. `LanternaInputHandler` maps the space-bar character key to it.

- [ ] **Step 1: Write the failing test**

Append inside `class LanternaInputHandlerTest { ... }`:

```java
    @Test
    void spaceKeyReturnsShoot() throws IOException {
        assertEquals(Command.SHOOT, handlerWithKey(new KeyStroke(' ', false, false)).pollCommand());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=LanternaInputHandlerTest`
Expected: FAIL (compilation error — `Command.SHOOT` does not exist)

- [ ] **Step 3: Write minimal implementation**

Replace `Command.java` contents:

```java
package ca.ninja.thenix.textrogue.input;

public enum Command {
    MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, SHOOT, QUIT, UNKNOWN
}
```

In `LanternaInputHandler.java`, inside the `toCommand` method's inner switch on `c`, add the space-bar case (insert before `default`):

```java
                    case ' ' -> Command.SHOOT;
```

So the inner `yield switch (c) { ... }` block reads:

```java
                yield switch (c) {
                    case 'w' -> Command.MOVE_UP;
                    case 's' -> Command.MOVE_DOWN;
                    case 'a' -> Command.MOVE_LEFT;
                    case 'd' -> Command.MOVE_RIGHT;
                    case 'q' -> Command.QUIT;
                    case ' ' -> Command.SHOOT;
                    default  -> Command.UNKNOWN;
                };
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=LanternaInputHandlerTest`
Expected: PASS (9 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/input/Command.java src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java
git commit -m "feat: add SHOOT command mapped to space bar"
```

---

### Task 6: Chase-step pure helper on Game

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Produces: `static int[] chaseStep(int enemyX, int enemyY, int playerX, int playerY)` — returns a `{dx, dy}` step (each in `{-1,0,1}`) toward the player, prioritizing the axis with the larger absolute distance. Used by Task 9's enemy AI.

- [ ] **Step 1: Write the failing test**

Append inside `class GameTest { ... }`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL (compilation error — `Game.chaseStep` does not exist)

- [ ] **Step 3: Write minimal implementation**

Add this method to `Game.java` (anywhere inside the class body, e.g. just above `private void tryMove`):

```java
    static int[] chaseStep(int enemyX, int enemyY, int playerX, int playerY) {
        int dx = playerX - enemyX;
        int dy = playerY - enemyY;
        if (Math.abs(dx) >= Math.abs(dy)) {
            return new int[]{Integer.signum(dx), 0};
        } else {
            return new int[]{0, Integer.signum(dy)};
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=GameTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: add chase-step pure helper for enemy AI"
```

---

### Task 7: Renderer draws facing/HP/enemies/projectiles/death; Game tracks enemies and projectiles

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `Enemy` (Task 3), `Projectile` (Task 4), `Facing` (Task 1), `Room` (existing, `world` package).
- Produces: `Renderer.render(Dungeon, Player, List<Enemy>, List<Projectile>)`, `Renderer.renderDeath(Player)`. `Game.enemies` (`List<Enemy>`, package-private, initialized empty), `Game.projectiles` (`List<Projectile>`, package-private, initialized empty), `Game.spawnEnemies()` (package-private, no-arg) — clears both lists then spawns 3–5 `Enemy` at random non-start-room centers using `dungeon.getRooms()`.

This task is one deliverable because the `Renderer` signature and `Game`'s new fields are inseparable — `Game.start()`'s two `render(...)` calls must pass real lists the moment the interface changes, so both sides are done together. Sub-steps below still follow test-first order.

- [ ] **Step 1: Write the failing Renderer/LanternaRenderer tests**

Replace `LanternaRendererTest.java` in full:

```java
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=LanternaRendererTest`
Expected: FAIL (compilation error — `Renderer.render` 4-arg overload and `renderDeath` don't exist yet)

- [ ] **Step 3: Update Renderer interface**

Replace `Renderer.java` contents:

```java
package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.world.Dungeon;
import java.io.IOException;
import java.util.List;

public interface Renderer {
    void render(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles);
    void renderDeath(Player player);
    void close() throws IOException;
}
```

- [ ] **Step 4: Update LanternaRenderer implementation**

Replace `LanternaRenderer.java` contents:

```java
package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;
import java.util.List;

public class LanternaRenderer implements Renderer {
    private final Screen screen;

    public LanternaRenderer(Screen screen) {
        this.screen = screen;
    }

    @Override
    public void render(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles) {
        screen.clear();
        TextGraphics g = screen.newTextGraphics();
        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                Tile tile = dungeon.getTile(x, y);
                g.setForegroundColor(colorFor(tile));
                g.putString(x, y, String.valueOf(tile.toChar()));
            }
        }
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            g.setForegroundColor(TextColor.ANSI.RED);
            g.putString(enemy.getX(), enemy.getY(), "e");
        }
        for (Projectile projectile : projectiles) {
            g.setForegroundColor(TextColor.ANSI.YELLOW);
            g.putString(projectile.getX(), projectile.getY(), "*");
        }
        g.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        g.putString(player.getX(), player.getY(), String.valueOf(player.getFacing().toChar()));
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(0, Dungeon.HEIGHT, "Floor: " + player.getFloor() + "   HP: " + player.getHp() + "/" + player.getMaxHp());
        try {
            screen.refresh();
        } catch (IOException ignored) {}
    }

    @Override
    public void renderDeath(Player player) {
        screen.clear();
        TextGraphics g = screen.newTextGraphics();
        g.setForegroundColor(TextColor.ANSI.RED);
        String message = "You died";
        int x = Math.max(0, (Dungeon.WIDTH - message.length()) / 2);
        int y = Dungeon.HEIGHT / 2;
        g.putString(x, y, message);
        try {
            screen.refresh();
        } catch (IOException ignored) {}
    }

    private TextColor colorFor(Tile tile) {
        return switch (tile) {
            case WALL        -> TextColor.ANSI.BLACK_BRIGHT;
            case FLOOR       -> TextColor.ANSI.WHITE;
            case STAIRS_DOWN -> TextColor.ANSI.YELLOW;
        };
    }

    @Override
    public void close() throws IOException {
        screen.stopScreen();
    }
}
```

- [ ] **Step 5: Run renderer tests to verify they pass**

Run: `mvn -q test -Dtest=LanternaRendererTest`
Expected: PASS (11 tests)

- [ ] **Step 6: Write the failing Game tests for enemy spawning**

Append inside `class GameTest { ... }` (add imports `ca.ninja.thenix.textrogue.entity.Enemy`, `ca.ninja.thenix.textrogue.entity.Facing`, `ca.ninja.thenix.textrogue.entity.Projectile`, and `ca.ninja.thenix.textrogue.world.Room` at the top of the file alongside the existing imports):

```java
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
```

Note: `descendingClearsOldEnemiesAndSpawnsNewOnes` triggers a real `descend()`, which generates a brand-new dungeon via `DungeonGenerator` (real rooms, not the empty `List.of()` used for this test's own dungeon) — that's why the assertion only checks the count range and that the stale enemy/projectile are gone, not specific positions.

- [ ] **Step 7: Run test to verify it fails**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL (compilation error — `Game.enemies` and `Game.spawnEnemies()` don't exist; `NULL_RENDERER` also fails to compile against the new `Renderer` signature)

- [ ] **Step 8: Fix the NULL_RENDERER stub to match the new Renderer interface**

In `GameTest.java`, replace:

```java
    private static final Renderer NULL_RENDERER = new Renderer() {
        public void render(Dungeon d, Player p) {}
        public void close() {}
    };
```

with:

```java
    private static final Renderer NULL_RENDERER = new Renderer() {
        public void render(Dungeon d, Player p, List<Enemy> enemies, List<Projectile> projectiles) {}
        public void renderDeath(Player p) {}
        public void close() {}
    };
```

Add `import ca.ninja.thenix.textrogue.entity.Enemy;` and `import ca.ninja.thenix.textrogue.entity.Projectile;` and `import ca.ninja.thenix.textrogue.world.Room;` to `GameTest.java`'s existing import block.

- [ ] **Step 9: Add enemies/projectiles fields and spawnEnemies() to Game, update render call sites**

In `Game.java`, add these imports:

```java
import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.world.Room;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
```

Add fields (alongside the existing `dungeon`/`player`/`running` fields) and constants (alongside `TICK_MS`):

```java
    static final int MIN_ENEMIES = 3;
    static final int MAX_ENEMIES = 5;
    static final int ENEMY_HP = 4;

    List<Enemy> enemies = new ArrayList<>();
    List<Projectile> projectiles = new ArrayList<>();
    private final Random random = new Random();
```

Add the `spawnEnemies()` method (package-private, no modifier):

```java
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
```

In `start()`, call `spawnEnemies()` right after the player is created, and update both `render` calls to pass the new lists:

```java
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
```

Also update `descend()` to respawn enemies for the new floor:

```java
    private void descend() {
        player.descendFloor();
        dungeon = new DungeonGenerator(System.currentTimeMillis() + player.getFloor()).generate();
        player.moveTo(dungeon.getStartX(), dungeon.getStartY());
        spawnEnemies();
    }
```

- [ ] **Step 10: Run all tests to verify they pass**

Run: `mvn -q test`
Expected: PASS (all tests, including the new `spawnEnemiesPlacesBetween3And5EnemiesExcludingStartRoom`)

- [ ] **Step 11: Compile and manually sanity-check**

Run: `mvn -q compile`
Expected: no output (success)

- [ ] **Step 12: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: render enemies/projectiles/facing/HP/death, spawn enemies per floor"
```

---

### Task 8: processCommand handles facing, enemy-blocked movement, and SHOOT

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `Facing` (Task 1), `Enemy` (Task 3), `Projectile` (Task 4), `Command.SHOOT` (Task 5), `Game.enemies`/`Game.projectiles` (Task 7).
- Produces: `Game` constants `PROJECTILE_RANGE = 5`, `SHOOT_COOLDOWN_TICKS = 5`; package-private field `shootCooldown` (starts at 0); `processCommand` now sets `player.facing` on every move attempt and blocks moves into living-enemy tiles; `SHOOT` spawns a `Projectile` when off cooldown.

- [ ] **Step 1: Write the failing tests**

Append inside `class GameTest { ... }` (add `import ca.ninja.thenix.textrogue.entity.Facing;` and `import ca.ninja.thenix.textrogue.entity.Projectile;` if not already present from Task 7):

```java
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL (facing not updated on move; enemy doesn't block movement; `SHOOT` case not handled — `processCommand` falls through to `UNKNOWN`, no projectile spawned)

- [ ] **Step 3: Write minimal implementation**

Add constants and field alongside the ones from Task 7:

```java
    static final int PROJECTILE_RANGE = 5;
    static final int SHOOT_COOLDOWN_TICKS = 5;

    private int shootCooldown = 0;
```

Replace the `processCommand` method:

```java
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
```

Replace the `tryMove` method to also block on living enemies:

```java
    private void tryMove(int nx, int ny) {
        Tile target = dungeon.getTile(nx, ny);
        if (target == Tile.WALL) return;
        if (enemyAt(nx, ny) != null) return;
        player.moveTo(nx, ny);
        if (target == Tile.STAIRS_DOWN) descend();
    }
```

Add the import `ca.ninja.thenix.textrogue.entity.Facing;` to `Game.java` if not already present.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=GameTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: facing-aware movement, enemy-blocked movement, cooldown-gated SHOOT"
```

---

### Task 9: Enemy chase AI and contact damage in gameTick()

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `Game.chaseStep` (Task 6), `Game.enemies` (Task 7), `Player.takeDamage` (Task 2).
- Produces: `Game` constants `CHASE_RADIUS = 6`, `ENEMY_CONTACT_DAMAGE = 2`. `gameTick()` moves alive enemies within radius one step toward the player (subject to wall/occupancy blocking), and deals contact damage instead of moving onto the player's tile.

- [ ] **Step 1: Write the failing tests**

Append inside `class GameTest { ... }`:

```java
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL (`gameTick()` is currently empty, so enemies never move and the player never takes contact damage)

- [ ] **Step 3: Write minimal implementation**

Add constants alongside the others:

```java
    static final int CHASE_RADIUS = 6;
    static final int ENEMY_CONTACT_DAMAGE = 2;
```

Replace `gameTick()`:

```java
    void gameTick() {
        if (shootCooldown > 0) shootCooldown--;
        moveEnemies();
    }

    private void moveEnemies() {
        java.util.Set<Long> occupied = new java.util.HashSet<>();
        for (Enemy e : enemies) {
            if (e.isAlive()) occupied.add(tileKey(e.getX(), e.getY()));
        }

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;

            int distX = Math.abs(enemy.getX() - player.getX());
            int distY = Math.abs(enemy.getY() - player.getY());
            if (Math.max(distX, distY) > CHASE_RADIUS) continue;

            int[] step = chaseStep(enemy.getX(), enemy.getY(), player.getX(), player.getY());
            int nx = enemy.getX() + step[0];
            int ny = enemy.getY() + step[1];

            if (nx == player.getX() && ny == player.getY()) {
                player.takeDamage(ENEMY_CONTACT_DAMAGE);
                continue;
            }
            if (dungeon.getTile(nx, ny) == Tile.WALL) continue;
            if (occupied.contains(tileKey(nx, ny))) continue;

            occupied.remove(tileKey(enemy.getX(), enemy.getY()));
            occupied.add(tileKey(nx, ny));
            enemy.moveTo(nx, ny);
        }
    }

    private static long tileKey(int x, int y) {
        return ((long) y << 32) | (x & 0xffffffffL);
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=GameTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: enemy chase AI and contact damage"
```

---

### Task 10: Projectile advance and collision in gameTick()

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `Game.projectiles` (Task 7), `Enemy.takeDamage` (Task 3), `Projectile.advance`/`getRemainingRange` (Task 4).
- Produces: `Game` constant `PROJECTILE_DAMAGE = 2`. `gameTick()` now also advances/resolves projectiles each tick: checks each projectile's *current* tile before advancing (wall → remove; living enemy → damage + remove; otherwise advance if range remains, else remove).

- [ ] **Step 1: Write the failing tests**

Append inside `class GameTest { ... }`:

```java
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL (`gameTick()` doesn't touch `projectiles` yet, so nothing advances, hits, or gets removed)

- [ ] **Step 3: Write minimal implementation**

Add the constant alongside the others:

```java
    static final int PROJECTILE_DAMAGE = 2;
```

Update `gameTick()` to also call the new method:

```java
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=GameTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: projectile advance and collision resolution"
```

---

### Task 11: Player death stops the loop and shows the death screen

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `Player.isDead()` (Task 2), `Renderer.renderDeath` (Task 7).
- Produces: `start()` becomes lazy-init (only builds a fresh dungeon/player/enemies if `dungeon` is still `null`, preserving the existing test seam where `gameWithInjectedState` pre-sets `dungeon`/`player`), and the loop now calls `renderer.renderDeath(player)` and stops (no sleep) the first tick `player.isDead()` becomes true.

- [ ] **Step 1: Write the failing test**

Append inside `class GameTest { ... }`:

```java
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
        game.start();
        assertTrue(deathRendered[0]);
        assertFalse(game.isRunning());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL — either a `NullPointerException`/compile issue because `start()` unconditionally regenerates `dungeon`/`player` (discarding the injected dead player before the loop ever sees it), so `deathRendered[0]` stays `false`

- [ ] **Step 3: Write minimal implementation**

Replace `start()`:

```java
    public void start() {
        if (dungeon == null) {
            dungeon = new DungeonGenerator(System.currentTimeMillis()).generate();
            player = new Player(dungeon.getStartX(), dungeon.getStartY());
            spawnEnemies();
        }
        running = true;
        renderer.render(dungeon, player, enemies, projectiles);
        while (running) {
            long tickStart = System.currentTimeMillis();
            processCommand(input.pollCommand());
            gameTick();
            renderer.render(dungeon, player, enemies, projectiles);
            if (player.isDead()) {
                renderer.renderDeath(player);
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=GameTest`
Expected: PASS

- [ ] **Step 5: Run the full test suite**

Run: `mvn -q test`
Expected: PASS (all tests across all classes)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: player death stops loop and renders death screen"
```

---

### Task 12: Final regression pass

**Files:** none (verification only)

**Interfaces:** none

- [ ] **Step 1: Run the full test suite**

Run: `mvn -q test`
Expected: PASS, all tests across `PlayerTest`, `EnemyTest`, `ProjectileTest`, `FacingTest`, `Command`-related tests, `LanternaInputHandlerTest`, `LanternaRendererTest`, `GameTest`, plus the pre-existing `TileTest`, `RoomTest`, `DungeonTest`, `DungeonGeneratorTest`.

- [ ] **Step 2: Compile the runnable jar path**

Run: `mvn -q compile`
Expected: no output (success)

- [ ] **Step 3: Manual playtest (human, not an agent — no TTY/GUI available in this session)**

From a real PowerShell or cmd window at the project root:

```
mvn compile exec:java
```

Confirm in the opened Swing window:
- Player glyph changes between `^`/`v`/`<`/`>` as you press w/a/s/d
- Space bar fires a `*` that travels up to 5 tiles and disappears on hitting a wall or an `e`
- Walking into an `e` blocks movement (no bump damage)
- An `e` that gets close chases you and reduces the `HP: n/20` status line when adjacent
- Killing all enemies with projectiles doesn't crash anything
- Letting HP reach 0 shows "You died" and the window/program exits
- Descending stairs (`>`) clears old enemies and spawns a fresh batch

- [ ] **Step 4: Commit any final fixups discovered during manual playtest**

If the manual playtest surfaces a bug, fix it, re-run `mvn -q test`, and commit:

```bash
git add -A
git commit -m "fix: <describe what the manual playtest caught>"
```

If no issues are found, skip this step (nothing to commit).
