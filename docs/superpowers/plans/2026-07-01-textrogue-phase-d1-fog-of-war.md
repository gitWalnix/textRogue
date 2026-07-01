# TextRogue Phase D1 (Fog of War) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add radius-based fog of war — tiles within a radius of the player are fully visible, previously-seen tiles stay drawn dimmed, and everything else is blank — behind a `VisibilityCalculator` seam that can later be swapped for real shadowcasting.

**Architecture:** A new `vision` package holds `VisibilityCalculator` (interface) and `RadiusVisibilityCalculator` (the only implementation for now). `Dungeon` gains a parallel `explored` boolean grid. `Game` computes the current visible-tile set once per tick via a package-private `updateVisibility()` method, marks those tiles explored, and passes the set to `Renderer`, whose signature grows a `Set<Position> visibleTiles` parameter.

**Tech Stack:** Java 21, JUnit 5, Lanterna 3.1.1 (`DefaultVirtualTerminal` for headless render tests).

## Global Constraints

- Java 21 language level, no new third-party dependencies.
- Follow existing code style: plain JUnit 5 `@Test` methods, no mocking framework, package-private fields/methods on `Game` for test seams (already established by `dungeon`/`player`/`enemies`/`projectiles`).
- Visibility radius: `VISIBILITY_RADIUS = 8` tiles, Chebyshev distance (`max(|dx|, |dy|)`), matching the style of the existing `CHASE_RADIUS` constant on `Game`.
- Walls do **not** block vision in this phase — radius-only, by design, so a future shadowcasting implementation has something to meaningfully improve on.
- Remembered (explored-but-not-currently-visible) tiles render in a single flat dim color, `TextColor.ANSI.BLACK_BRIGHT`, for every tile type — no per-type darkened variants.
- Enemies and projectiles render only when their tile is in the *current* tick's visible set (in addition to the existing `enemy.isAlive()` check for enemies) — never merely "explored."
- The player's own glyph always renders regardless of visibility.

---

### Task 1: Position record

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/world/Position.java`
- Test: `src/test/java/ca/ninja/thenix/textrogue/world/PositionTest.java`

**Interfaces:**
- Produces: `record Position(int x, int y)` — used by Task 3's `VisibilityCalculator` and Task 4's `Renderer`/`Game` changes.

- [ ] **Step 1: Write the failing test**

```java
package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionTest {
    @Test
    void sameCoordinatesAreEqual() {
        assertEquals(new Position(3, 4), new Position(3, 4));
        assertEquals(new Position(3, 4).hashCode(), new Position(3, 4).hashCode());
    }

    @Test
    void differentCoordinatesAreNotEqual() {
        assertNotEquals(new Position(3, 4), new Position(4, 3));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=PositionTest`
Expected: FAIL (compilation error — `Position` does not exist)

- [ ] **Step 3: Write minimal implementation**

```java
package ca.ninja.thenix.textrogue.world;

public record Position(int x, int y) {}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=PositionTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/world/Position.java src/test/java/ca/ninja/thenix/textrogue/world/PositionTest.java
git commit -m "feat: add Position record for shared tile coordinates"
```

---

### Task 2: Dungeon explored-tile tracking

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/world/Dungeon.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/world/DungeonTest.java`

**Interfaces:**
- Produces: `Dungeon.markExplored(int x, int y)`, `Dungeon.isExplored(int x, int y)`. `isExplored` returns `false` for out-of-bounds coordinates (mirrors `getTile`'s existing out-of-bounds handling, which returns `WALL` rather than throwing); `markExplored` is a no-op for out-of-bounds coordinates (mirrors `setTile`'s existing bounds check).

- [ ] **Step 1: Write the failing tests**

Append inside `class DungeonTest { ... }`:

```java
    @Test
    void tilesStartUnexplored() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertFalse(dungeon.isExplored(5, 5));
    }

    @Test
    void markExploredMakesTileExplored() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        dungeon.markExplored(5, 5);
        assertTrue(dungeon.isExplored(5, 5));
        assertFalse(dungeon.isExplored(6, 5));
    }

    @Test
    void markExploredOutOfBoundsDoesNotThrow() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertDoesNotThrow(() -> dungeon.markExplored(-1, -1));
    }

    @Test
    void isExploredOutOfBoundsReturnsFalse() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertFalse(dungeon.isExplored(-1, 0));
        assertFalse(dungeon.isExplored(0, Dungeon.HEIGHT));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=DungeonTest`
Expected: FAIL (compilation error — `markExplored`/`isExplored` do not exist)

- [ ] **Step 3: Write minimal implementation**

In `Dungeon.java`, add a field alongside `tiles`:

```java
    private final boolean[][] explored;
```

Initialize it in the constructor (append to the existing assignment list):

```java
    public Dungeon(Tile[][] tiles, List<Room> rooms, int startX, int startY) {
        this.tiles = tiles;
        this.rooms = rooms;
        this.startX = startX;
        this.startY = startY;
        this.explored = new boolean[HEIGHT][WIDTH];
    }
```

Add the two new methods (placed near `getTile`/`setTile`):

```java
    public void markExplored(int x, int y) {
        if (isInBounds(x, y)) explored[y][x] = true;
    }

    public boolean isExplored(int x, int y) {
        if (!isInBounds(x, y)) return false;
        return explored[y][x];
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=DungeonTest`
Expected: PASS (9 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/world/Dungeon.java src/test/java/ca/ninja/thenix/textrogue/world/DungeonTest.java
git commit -m "feat: track explored tiles on Dungeon"
```

---

### Task 3: VisibilityCalculator + RadiusVisibilityCalculator

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/vision/VisibilityCalculator.java`
- Create: `src/main/java/ca/ninja/thenix/textrogue/vision/RadiusVisibilityCalculator.java`
- Test: `src/test/java/ca/ninja/thenix/textrogue/vision/RadiusVisibilityCalculatorTest.java`

**Interfaces:**
- Consumes: `Position` (Task 1), `Dungeon.isInBounds(int, int)` (existing).
- Produces: `VisibilityCalculator.computeVisibleTiles(Dungeon, int playerX, int playerY) -> Set<Position>`; `RadiusVisibilityCalculator(int radius)` constructor. Used by Task 4's `Game`.

- [ ] **Step 1: Write the failing tests**

```java
package ca.ninja.thenix.textrogue.vision;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class RadiusVisibilityCalculatorTest {
    private Dungeon allWalls() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        return new Dungeon(tiles, List.of(), 0, 0);
    }

    @Test
    void tileExactlyAtRadiusIsIncluded() {
        RadiusVisibilityCalculator calculator = new RadiusVisibilityCalculator(3);
        Set<Position> visible = calculator.computeVisibleTiles(allWalls(), 10, 10);
        assertTrue(visible.contains(new Position(13, 10)));
    }

    @Test
    void tileOneBeyondRadiusIsExcluded() {
        RadiusVisibilityCalculator calculator = new RadiusVisibilityCalculator(3);
        Set<Position> visible = calculator.computeVisibleTiles(allWalls(), 10, 10);
        assertFalse(visible.contains(new Position(14, 10)));
    }

    @Test
    void playerOwnTileIsIncluded() {
        RadiusVisibilityCalculator calculator = new RadiusVisibilityCalculator(3);
        Set<Position> visible = calculator.computeVisibleTiles(allWalls(), 10, 10);
        assertTrue(visible.contains(new Position(10, 10)));
    }

    @Test
    void outOfBoundsTilesAreExcludedEvenWithinRadius() {
        RadiusVisibilityCalculator calculator = new RadiusVisibilityCalculator(5);
        Set<Position> visible = calculator.computeVisibleTiles(allWalls(), 0, 0);
        assertFalse(visible.contains(new Position(-2, 0)));
        assertFalse(visible.contains(new Position(0, -2)));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=RadiusVisibilityCalculatorTest`
Expected: FAIL (compilation error — `VisibilityCalculator`/`RadiusVisibilityCalculator` do not exist)

- [ ] **Step 3: Write minimal implementation**

```java
package ca.ninja.thenix.textrogue.vision;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import java.util.Set;

public interface VisibilityCalculator {
    Set<Position> computeVisibleTiles(Dungeon dungeon, int playerX, int playerY);
}
```

```java
package ca.ninja.thenix.textrogue.vision;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import java.util.HashSet;
import java.util.Set;

public class RadiusVisibilityCalculator implements VisibilityCalculator {
    private final int radius;

    public RadiusVisibilityCalculator(int radius) {
        this.radius = radius;
    }

    @Override
    public Set<Position> computeVisibleTiles(Dungeon dungeon, int playerX, int playerY) {
        Set<Position> visible = new HashSet<>();
        for (int y = playerY - radius; y <= playerY + radius; y++) {
            for (int x = playerX - radius; x <= playerX + radius; x++) {
                if (!dungeon.isInBounds(x, y)) continue;
                if (Math.max(Math.abs(x - playerX), Math.abs(y - playerY)) > radius) continue;
                visible.add(new Position(x, y));
            }
        }
        return visible;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=RadiusVisibilityCalculatorTest`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/vision/VisibilityCalculator.java src/main/java/ca/ninja/thenix/textrogue/vision/RadiusVisibilityCalculator.java src/test/java/ca/ninja/thenix/textrogue/vision/RadiusVisibilityCalculatorTest.java
git commit -m "feat: add radius-based VisibilityCalculator"
```

---

### Task 4: Renderer draws fog-of-war tiers; Game computes visibility each tick

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `Position` (Task 1), `Dungeon.isExplored`/`markExplored` (Task 2), `VisibilityCalculator`/`RadiusVisibilityCalculator` (Task 3).
- Produces: `Renderer.render(Dungeon, Player, List<Enemy>, List<Projectile>, Set<Position> visibleTiles)`. `Game.updateVisibility()` (package-private, no-arg) — computes the current visible set via the calculator, marks each tile explored on `dungeon`, and returns the set. `Game.VISIBILITY_RADIUS = 8`.

This task is one deliverable for the same reason Phase C's equivalent renderer-signature task was: the moment `Renderer`'s signature changes, `Game.java`'s two `render(...)` call sites must be updated in the same commit to keep the build green — there's no way to land the interface change and the wiring separately without an intermediate broken state.

- [ ] **Step 1: Write the failing Renderer/LanternaRenderer tests**

Replace `LanternaRendererTest.java` in full:

```java
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
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(new Enemy(3, 3, 4)), List.of(), Set.of());
        assertEquals(' ', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void projectileOutsideVisibleTilesDoesNotRender() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5), List.of(), List.of(new Projectile(2, 2, Facing.UP, 5)), Set.of());
        assertEquals(' ', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=LanternaRendererTest`
Expected: FAIL (compilation error — `Renderer.render` 5-arg overload doesn't exist yet)

- [ ] **Step 3: Update Renderer interface**

Replace `Renderer.java` contents:

```java
package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface Renderer {
    void render(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles, Set<Position> visibleTiles);
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
import ca.ninja.thenix.textrogue.world.Position;
import ca.ninja.thenix.textrogue.world.Tile;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class LanternaRenderer implements Renderer {
    private final Screen screen;

    public LanternaRenderer(Screen screen) {
        this.screen = screen;
    }

    @Override
    public void render(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles, Set<Position> visibleTiles) {
        screen.clear();
        TextGraphics g = screen.newTextGraphics();
        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                Position pos = new Position(x, y);
                if (visibleTiles.contains(pos)) {
                    Tile tile = dungeon.getTile(x, y);
                    g.setForegroundColor(colorFor(tile));
                    g.putString(x, y, String.valueOf(tile.toChar()));
                } else if (dungeon.isExplored(x, y)) {
                    Tile tile = dungeon.getTile(x, y);
                    g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                    g.putString(x, y, String.valueOf(tile.toChar()));
                } else {
                    g.putString(x, y, " ");
                }
            }
        }
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            if (!visibleTiles.contains(new Position(enemy.getX(), enemy.getY()))) continue;
            g.setForegroundColor(TextColor.ANSI.RED);
            g.putString(enemy.getX(), enemy.getY(), "e");
        }
        for (Projectile projectile : projectiles) {
            if (!visibleTiles.contains(new Position(projectile.getX(), projectile.getY()))) continue;
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
Expected: PASS (16 tests)

- [ ] **Step 6: Fix GameTest's Renderer stubs and add the visibility test**

In `GameTest.java`, add imports `ca.ninja.thenix.textrogue.world.Position;` and `java.util.Set;` to the existing import block.

Replace the `NULL_RENDERER` field:

```java
    private static final Renderer NULL_RENDERER = new Renderer() {
        public void render(Dungeon d, Player p, List<Enemy> enemies, List<Projectile> projectiles, Set<Position> visibleTiles) {}
        public void renderDeath(Player p) {}
        public void close() {}
    };
```

Replace the anonymous `Renderer` inside `playerDeathStopsLoopAndRendersDeathScreen`:

```java
        Renderer capturingRenderer = new Renderer() {
            public void render(Dungeon d, Player p, List<Enemy> e, List<Projectile> pr, Set<Position> visibleTiles) {}
            public void renderDeath(Player p) { deathRendered[0] = true; }
            public void close() {}
        };
```

Append a new test inside `class GameTest { ... }`:

```java
    @Test
    void updateVisibilityMarksTilesWithinRadiusExplored() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        Set<Position> visible = game.updateVisibility();
        assertTrue(dungeon.isExplored(40, 12));
        assertTrue(visible.contains(new Position(40, 12)));
        assertTrue(dungeon.isExplored(40 + Game.VISIBILITY_RADIUS, 12));
        assertFalse(dungeon.isExplored(40 + Game.VISIBILITY_RADIUS + 1, 12));
    }
```

- [ ] **Step 7: Run test to verify it fails**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL (compilation error — `Game.updateVisibility()` and `Game.VISIBILITY_RADIUS` don't exist; `render` stub signature mismatch also fails compilation until Step 8 below)

- [ ] **Step 8: Add visibility computation to Game and wire the render call sites**

In `Game.java`, add imports:

```java
import ca.ninja.thenix.textrogue.vision.RadiusVisibilityCalculator;
import ca.ninja.thenix.textrogue.vision.VisibilityCalculator;
import ca.ninja.thenix.textrogue.world.Position;
import java.util.Set;
```

Add the constant alongside the other tuning constants:

```java
    static final int VISIBILITY_RADIUS = 8;
```

Add the field alongside `random`:

```java
    private final VisibilityCalculator visibilityCalculator = new RadiusVisibilityCalculator(VISIBILITY_RADIUS);
```

Add the method (package-private, no modifier, placed near `spawnEnemies`):

```java
    Set<Position> updateVisibility() {
        Set<Position> visible = visibilityCalculator.computeVisibleTiles(dungeon, player.getX(), player.getY());
        for (Position p : visible) dungeon.markExplored(p.x(), p.y());
        return visible;
    }
```

Update both `renderer.render(...)` call sites in `start()` (the initial pre-loop render and the per-tick render inside the loop) to:

```java
            renderer.render(dungeon, player, enemies, projectiles, updateVisibility());
```

(Both call sites use the identical line — one right after `running = true;`, the other right after `gameTick();` inside the `while (running)` loop, replacing their current 4-arg calls.)

- [ ] **Step 9: Run tests to verify they pass**

Run: `mvn -q test -Dtest=GameTest`
Expected: PASS

- [ ] **Step 10: Run the full test suite**

Run: `mvn -q test`
Expected: PASS (all tests across every class)

- [ ] **Step 11: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: render fog-of-war tiers, compute visibility each tick"
```

---

### Task 5: Manual playtest checklist

**Files:** none (verification only)

**Interfaces:** none

- [ ] **Step 1: Run the full test suite**

Run: `mvn -q test`
Expected: PASS, all tests across every class.

- [ ] **Step 2: Compile check**

Run: `mvn -q compile`
Expected: no output (success)

- [ ] **Step 3: Manual playtest (human, not an agent — no GUI in this session)**

From a real PowerShell or cmd window at the project root:

```
mvn compile exec:java
```

Confirm in the opened Swing window:
- Tiles beyond ~8 tiles from the player are blank (not drawn)
- Walking into a new area reveals it at full brightness
- Walking away from an explored area leaves it visible but visibly dimmer than the currently-visible area
- An enemy that was visible a moment ago, once you move far enough away, disappears from view even though its tile remains dimly visible as explored terrain
- Descending stairs to a new floor resets fog — the new floor starts fully blank again
- The player's own glyph is always visible regardless of surroundings

- [ ] **Step 4: Commit any fixups discovered during manual playtest**

If the manual playtest surfaces a bug, fix it, re-run `mvn -q test`, and commit:

```bash
git add -A
git commit -m "fix: <describe what the manual playtest caught>"
```

If no issues are found, skip this step (nothing to commit).
