# TextRogue Phase D2 (Items) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add healing potions — dropped probabilistically by enemies on death, picked up automatically on contact, and consumed on command — as the first item type in TextRogue, built as the smallest slice that proves out drop → ground representation → pickup → inventory → use.

**Architecture:** Potions are represented as a new `Tile.POTION` enum constant rather than a separate moving-entity list, so they ride the existing fog-of-war terrain rendering (visible/explored-dimmed/unexplored-blank) from Phase D1 for free, with zero new rendering branches. `Player` gains a stackable `potionCount` plus `heal`/`addPotion`/`drinkPotion` methods. `Game` rolls a drop chance in `advanceProjectiles()` when a hit kills an enemy, handles pickup in `tryMove()`, and wires a new `DRINK_POTION` command in `processCommand()`. The existing `'q'` → `QUIT` key binding is untouched — there's no actual conflict with the new `'e'` drink key.

**Tech Stack:** Java 21, JUnit 5, Lanterna 3.1.1 (`DefaultVirtualTerminal` for headless render/input tests).

## Global Constraints

- Java 21 language level, no new third-party dependencies.
- Follow existing code style: plain JUnit 5 `@Test` methods, no mocking framework, package-private fields/methods on `Game` for test seams (already established by `dungeon`/`player`/`enemies`/`projectiles`/`random`).
- Potion drop chance: `POTION_DROP_CHANCE = 0.33` (a `double`), rolled once per killing blow via `Game`'s existing `random` field.
- Potion heal amount: `POTION_HEAL_AMOUNT = 10`, capped at `Player.MAX_HP` (never overheals).
- Potions are stackable: `Player.potionCount` is a plain incrementing/decrementing `int`, no upper cap.
- Potion glyph is `'!'`, color `TextColor.ANSI.MAGENTA` — added to `Tile.toChar()` and `LanternaRenderer.colorFor(Tile)`.
- Drop guard: only drop onto a tile that is currently `Tile.FLOOR` — never overwrite `Tile.STAIRS_DOWN` (an enemy that dies standing on stairs simply doesn't drop anything, no special-case restoration logic).
- Pickup is automatic on walking onto a `Tile.POTION` tile (mirrors the existing `STAIRS_DOWN`-triggers-`descend()` pattern in `tryMove()`), and the tile reverts to `Tile.FLOOR` immediately.
- Drink command is bound to `'e'`, added to `Command` as `DRINK_POTION`. No user-facing message on drinking with an empty inventory — the status line's `Potions: 0` is the existing signal.
- `'q'` → `Command.QUIT` is unchanged — both `'q'` and `Escape`/`EOF` continue to quit, exactly as before this phase.
- Potions do **not** get their own entity-visibility gating — as a `Tile`, they already only render according to the three-tier fog-of-war rules every other tile uses (Phase D1, untouched).

---

### Task 1: Tile.POTION and renderer color mapping

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/world/Tile.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/world/TileTest.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java`

**Interfaces:**
- Produces: `Tile.POTION` enum constant, `Tile.POTION.toChar() == '!'`, `LanternaRenderer.colorFor(Tile.POTION) == TextColor.ANSI.MAGENTA`. Used by Task 4 (`Game`'s drop/pickup logic) and Task 5 (status line).

`LanternaRenderer.colorFor(Tile)` is a switch **expression** (`return switch (tile) { ... };`), which Java requires to be exhaustive over the enum. The moment `Tile.POTION` exists, that method stops compiling until it has a matching case — so the color-mapping fix must land in the *same commit* as the new enum constant, or every other file in the module fails to build in between. (The status-line potion-count text in `LanternaRenderer` is a separate, non-exhaustiveness-sensitive change and stays in Task 5.)

- [ ] **Step 1: Write the failing tests**

Append inside `class TileTest { ... }`:

```java
    @Test
    void potionRendersAsExclamation() {
        assertEquals('!', Tile.POTION.toChar());
    }
```

Append inside `class LanternaRendererTest { ... }`:

```java
    @Test
    void potionTileRendersAsBang() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(2, 2, Tile.POTION), new Player(5, 5), List.of(), List.of(), allVisible());
        assertEquals('!', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=TileTest,LanternaRendererTest`
Expected: FAIL (compilation error — `Tile.POTION` does not exist)

- [ ] **Step 3: Write minimal implementation**

Replace `Tile.java` contents:

```java
package ca.ninja.thenix.textrogue.world;

public enum Tile {
    WALL, FLOOR, STAIRS_DOWN, POTION;

    public char toChar() {
        return switch (this) {
            case WALL -> '#';
            case FLOOR -> '.';
            case STAIRS_DOWN -> '>';
            case POTION -> '!';
        };
    }
}
```

In `LanternaRenderer.java`, replace the `colorFor` method:

```java
    private TextColor colorFor(Tile tile) {
        return switch (tile) {
            case WALL        -> TextColor.ANSI.BLACK_BRIGHT;
            case FLOOR       -> TextColor.ANSI.WHITE;
            case STAIRS_DOWN -> TextColor.ANSI.YELLOW;
        };
    }
```

with:

```java
    private TextColor colorFor(Tile tile) {
        return switch (tile) {
            case WALL        -> TextColor.ANSI.BLACK_BRIGHT;
            case FLOOR       -> TextColor.ANSI.WHITE;
            case STAIRS_DOWN -> TextColor.ANSI.YELLOW;
            case POTION      -> TextColor.ANSI.MAGENTA;
        };
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=TileTest,LanternaRendererTest`
Expected: PASS (4 + 17 = 21 tests)

- [ ] **Step 5: Run the full test suite**

Run: `mvn -q test`
Expected: PASS (all tests across every class — this confirms the exhaustiveness fix kept every other file compiling)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/world/Tile.java src/test/java/ca/ninja/thenix/textrogue/world/TileTest.java src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java
git commit -m "feat: add POTION tile type and its renderer color mapping"
```

---

### Task 2: Player potion inventory and healing

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/entity/Player.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java`

**Interfaces:**
- Produces: `Player.getPotionCount() -> int`, `Player.addPotion()`, `Player.heal(int amount)` (caps at `MAX_HP`), `Player.drinkPotion(int healAmount) -> boolean` (returns `false` and no-ops if `potionCount <= 0`, otherwise decrements count, heals, returns `true`). Used by Task 4 (`Game`'s pickup/drink wiring) and Task 5 (`LanternaRenderer` status line).

- [ ] **Step 1: Write the failing tests**

Append inside `class PlayerTest { ... }`:

```java
    @Test
    void startsWithNoPotions() {
        Player player = new Player(0, 0);
        assertEquals(0, player.getPotionCount());
    }

    @Test
    void addPotionIncrementsCount() {
        Player player = new Player(0, 0);
        player.addPotion();
        player.addPotion();
        assertEquals(2, player.getPotionCount());
    }

    @Test
    void healIncreasesHpButCapsAtMax() {
        Player player = new Player(0, 0);
        player.takeDamage(15);
        player.heal(5);
        assertEquals(10, player.getHp());
        player.heal(999);
        assertEquals(20, player.getHp());
    }

    @Test
    void drinkPotionConsumesPotionAndHeals() {
        Player player = new Player(0, 0);
        player.takeDamage(15);
        player.addPotion();
        boolean drank = player.drinkPotion(10);
        assertTrue(drank);
        assertEquals(15, player.getHp());
        assertEquals(0, player.getPotionCount());
    }

    @Test
    void drinkPotionWithNoneDoesNothing() {
        Player player = new Player(0, 0);
        boolean drank = player.drinkPotion(10);
        assertFalse(drank);
        assertEquals(20, player.getHp());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=PlayerTest`
Expected: FAIL (compilation error — `getPotionCount`/`addPotion`/`heal`/`drinkPotion` do not exist)

- [ ] **Step 3: Write minimal implementation**

Replace `Player.java` contents:

```java
package ca.ninja.thenix.textrogue.entity;

public class Player {
    private static final int MAX_HP = 20;

    private int x;
    private int y;
    private int floor;
    private int hp;
    private Facing facing;
    private int potionCount;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.floor = 1;
        this.hp = MAX_HP;
        this.facing = Facing.DOWN;
        this.potionCount = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getFloor() { return floor; }
    public int getHp() { return hp; }
    public int getMaxHp() { return MAX_HP; }
    public Facing getFacing() { return facing; }
    public int getPotionCount() { return potionCount; }

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

    public void heal(int amount) {
        hp = Math.min(MAX_HP, hp + amount);
    }

    public void addPotion() {
        potionCount++;
    }

    public boolean drinkPotion(int healAmount) {
        if (potionCount <= 0) return false;
        potionCount--;
        heal(healAmount);
        return true;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=PlayerTest`
Expected: PASS (15 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/entity/Player.java src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java
git commit -m "feat: add potion inventory and healing to Player"
```

---

### Task 3: DRINK_POTION command bound to 'e'

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/input/Command.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java`

**Interfaces:**
- Produces: `Command.DRINK_POTION` enum constant; `'e'` key maps to it. `'q'` continues to map to `Command.QUIT`, unchanged; `Escape`/`EOF` also continue to map to `Command.QUIT`, unchanged. Used by Task 4 (`Game.processCommand`).

- [ ] **Step 1: Write the failing tests**

The existing `qKeyReturnsQuit` test is untouched — `'q'` still quits. Append these new tests inside `class LanternaInputHandlerTest { ... }`:

```java
    @Test
    void eKeyReturnsDrinkPotion() throws IOException {
        assertEquals(Command.DRINK_POTION, handlerWithKey(new KeyStroke('e', false, false)).pollCommand());
    }

    @Test
    void escapeKeyReturnsQuit() throws IOException {
        assertEquals(Command.QUIT, handlerWithKey(new KeyStroke(KeyType.Escape)).pollCommand());
    }
```

(`escapeKeyReturnsQuit` fills a pre-existing gap — the `EOF, Escape -> Command.QUIT` branch was never directly tested before this, only `'q'` was.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=LanternaInputHandlerTest`
Expected: FAIL — both new tests fail to compile (`Command.DRINK_POTION` does not exist yet); `qKeyReturnsQuit` and every other existing test still pass unaffected

- [ ] **Step 3: Add DRINK_POTION to Command**

Replace `Command.java` contents:

```java
package ca.ninja.thenix.textrogue.input;

public enum Command {
    MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, SHOOT, DRINK_POTION, QUIT, UNKNOWN
}
```

- [ ] **Step 4: Update LanternaInputHandler's key mapping**

In `LanternaInputHandler.java`, replace the `toCommand` method:

```java
    private Command toCommand(KeyStroke key) {
        return switch (key.getKeyType()) {
            case Character -> {
                char c = Character.toLowerCase(key.getCharacter());
                yield switch (c) {
                    case 'w' -> Command.MOVE_UP;
                    case 's' -> Command.MOVE_DOWN;
                    case 'a' -> Command.MOVE_LEFT;
                    case 'd' -> Command.MOVE_RIGHT;
                    case 'q' -> Command.QUIT;
                    case ' ' -> Command.SHOOT;
                    default  -> Command.UNKNOWN;
                };
            }
            case ArrowUp    -> Command.MOVE_UP;
            case ArrowDown  -> Command.MOVE_DOWN;
            case ArrowLeft  -> Command.MOVE_LEFT;
            case ArrowRight -> Command.MOVE_RIGHT;
            case EOF, Escape -> Command.QUIT;
            default -> Command.UNKNOWN;
        };
    }
```

with:

```java
    private Command toCommand(KeyStroke key) {
        return switch (key.getKeyType()) {
            case Character -> {
                char c = Character.toLowerCase(key.getCharacter());
                yield switch (c) {
                    case 'w' -> Command.MOVE_UP;
                    case 's' -> Command.MOVE_DOWN;
                    case 'a' -> Command.MOVE_LEFT;
                    case 'd' -> Command.MOVE_RIGHT;
                    case 'q' -> Command.QUIT;
                    case 'e' -> Command.DRINK_POTION;
                    case ' ' -> Command.SHOOT;
                    default  -> Command.UNKNOWN;
                };
            }
            case ArrowUp    -> Command.MOVE_UP;
            case ArrowDown  -> Command.MOVE_DOWN;
            case ArrowLeft  -> Command.MOVE_LEFT;
            case ArrowRight -> Command.MOVE_RIGHT;
            case EOF, Escape -> Command.QUIT;
            default -> Command.UNKNOWN;
        };
    }
```

(`'q' -> Command.QUIT` is carried over unchanged from the existing code — only the `'e'` case is new.)

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q test -Dtest=LanternaInputHandlerTest`
Expected: PASS (11 tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/input/Command.java src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java
git commit -m "feat: add DRINK_POTION command bound to 'e'"
```

---

### Task 4: Game — drop-on-kill, pickup, and drink wiring

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `Tile.POTION` (Task 1), `Player.addPotion()`/`drinkPotion(int)`/`getPotionCount()` (Task 2), `Command.DRINK_POTION` (Task 3).
- Produces: `Game.POTION_DROP_CHANCE = 0.33` (package-private `static final double`), `Game.POTION_HEAL_AMOUNT = 10` (package-private `static final int`). Used by Task 5's manual playtest only (no other task depends on `Game`'s new behavior directly — `LanternaRenderer` only needs `Player.getPotionCount()` from Task 2).

This task bundles drop-on-kill, pickup, and drink handling into one commit because all three touch `Game.java` and are small enough that splitting them would mean landing partial, hard-to-independently-test behavior (e.g. a pickup path with nothing that ever drops a potion to pick up).

- [ ] **Step 1: Write the failing tests**

Append inside `class GameTest { ... }`:

```java
    @Test
    void walkingOntoPotionAddsToInventoryAndClearsTile() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        tiles[12][40] = Tile.FLOOR;
        tiles[11][40] = Tile.POTION;
        Dungeon dungeon = new Dungeon(tiles, List.of(), 40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.MOVE_UP);
        assertEquals(1, game.player.getPotionCount());
        assertEquals(Tile.FLOOR, dungeon.getTile(40, 11));
    }

    @Test
    void drinkPotionCommandHealsPlayerAndConsumesPotion() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        player.takeDamage(15);
        player.addPotion();
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.DRINK_POTION);
        assertEquals(15, game.player.getHp());
        assertEquals(0, game.player.getPotionCount());
    }

    @Test
    void killingEnemyOnStairsNeverOverwritesStairsTile() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        tiles[12][40] = Tile.FLOOR;
        tiles[12][41] = Tile.STAIRS_DOWN;
        Dungeon dungeon = new Dungeon(tiles, List.of(), 40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        Enemy enemy = new Enemy(41, 12, 2);
        game.enemies.add(enemy);
        game.projectiles.add(new Projectile(41, 12, Facing.RIGHT, 5));
        game.gameTick();
        assertEquals(Tile.STAIRS_DOWN, dungeon.getTile(41, 12));
    }

    @Test
    void killingEnemyProbablyDropsPotionSometimesButNotAlways() {
        boolean droppedAtLeastOnce = false;
        boolean noDropAtLeastOnce = false;
        for (int i = 0; i < 200 && !(droppedAtLeastOnce && noDropAtLeastOnce); i++) {
            Dungeon dungeon = dungeonWithFloorAt(40, 12, 41, 12);
            Player player = new Player(40, 12);
            Game game = gameWithInjectedState(dungeon, player);
            Enemy enemy = new Enemy(41, 12, 2);
            game.enemies.add(enemy);
            game.projectiles.add(new Projectile(41, 12, Facing.RIGHT, 5));
            game.gameTick();
            if (dungeon.getTile(41, 12) == Tile.POTION) droppedAtLeastOnce = true;
            else noDropAtLeastOnce = true;
        }
        assertTrue(droppedAtLeastOnce, "expected at least one potion drop across 200 trials at 33% chance");
        assertTrue(noDropAtLeastOnce, "expected at least one non-drop across 200 trials at 33% chance");
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL for three of the four — `walkingOntoPotionAddsToInventoryAndClearsTile` and `drinkPotionCommandHealsPlayerAndConsumesPotion` fail their assertions (no pickup/drink wiring yet, so potion count and HP never change); `killingEnemyProbablyDropsPotionSometimesButNotAlways` fails its `droppedAtLeastOnce` assertion (nothing ever creates a `Tile.POTION` yet, so no trial ever sees a drop). `killingEnemyOnStairsNeverOverwritesStairsTile` will actually **pass** even before Step 3-6's changes — nothing modifies the tile yet, so the guard is vacuously true. That's expected: it's a regression test that only becomes meaningful once drop-on-kill logic exists in Step 6; it stays green throughout rather than following red-green-refactor, which is fine for a guard-clause test.

- [ ] **Step 3: Add drop/heal constants**

In `Game.java`, add two constants alongside the existing tuning constants (immediately after `VISIBILITY_RADIUS`):

```java
    static final double POTION_DROP_CHANCE = 0.33;
    static final int POTION_HEAL_AMOUNT = 10;
```

- [ ] **Step 4: Add DRINK_POTION handling to processCommand**

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
```

with:

```java
    void processCommand(Command cmd) {
        switch (cmd) {
            case MOVE_UP      -> { player.setFacing(Facing.UP);    tryMove(player.getX(), player.getY() - 1); }
            case MOVE_DOWN    -> { player.setFacing(Facing.DOWN);  tryMove(player.getX(), player.getY() + 1); }
            case MOVE_LEFT    -> { player.setFacing(Facing.LEFT);  tryMove(player.getX() - 1, player.getY()); }
            case MOVE_RIGHT   -> { player.setFacing(Facing.RIGHT); tryMove(player.getX() + 1, player.getY()); }
            case SHOOT        -> shoot();
            case DRINK_POTION -> player.drinkPotion(POTION_HEAL_AMOUNT);
            case QUIT         -> running = false;
            case UNKNOWN      -> {}
        }
    }
```

- [ ] **Step 5: Add pickup handling to tryMove**

Replace the `tryMove` method:

```java
    private void tryMove(int nx, int ny) {
        Tile target = dungeon.getTile(nx, ny);
        if (target == Tile.WALL) return;
        if (enemyAt(nx, ny) != null) return;
        player.moveTo(nx, ny);
        if (target == Tile.STAIRS_DOWN) descend();
    }
```

with:

```java
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
```

- [ ] **Step 6: Add drop-on-kill handling to advanceProjectiles**

Replace the `advanceProjectiles` method:

```java
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

with:

```java
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
                if (!hit.isAlive()) maybeDropPotion(hit.getX(), hit.getY());
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
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn -q test -Dtest=GameTest`
Expected: PASS (26 tests)

- [ ] **Step 8: Run the full test suite**

Run: `mvn -q test`
Expected: PASS (all tests across every class)

- [ ] **Step 9: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: drop potions on enemy kill, pick them up on contact, drink on command"
```

---

### Task 5: Renderer — status line potion count

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java`

**Interfaces:**
- Consumes: `Player.getPotionCount()` (Task 2). (`Tile.POTION`'s glyph/color were already wired up in Task 1, alongside the enum constant itself, to avoid a broken-build window — see Task 1's note.)
- Produces: nothing new consumed by later tasks — this is the last production-code task in the phase.

- [ ] **Step 1: Write the failing test**

Append inside `class LanternaRendererTest { ... }`:

```java
    @Test
    void statusLineShowsPotionCount() throws IOException {
        Player player = new Player(0, 0);
        player.addPotion();
        player.addPotion();
        DefaultVirtualTerminal t = renderTo(allWalls(), player, List.of(), List.of(), allVisible());
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < 45; x++) sb.append(t.getCharacter(new TerminalPosition(x, Dungeon.HEIGHT)).getCharacter());
        assertTrue(sb.toString().contains("Potions: 2"));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=LanternaRendererTest`
Expected: FAIL — `statusLineShowsPotionCount` fails its assertion (status line doesn't include potion count yet)

- [ ] **Step 3: Add status line potion count**

In `LanternaRenderer.java`, replace the status line `putString` call:

```java
        g.putString(0, Dungeon.HEIGHT, "Floor: " + player.getFloor() + "   HP: " + player.getHp() + "/" + player.getMaxHp());
```

with:

```java
        g.putString(0, Dungeon.HEIGHT, "Floor: " + player.getFloor() + "   HP: " + player.getHp() + "/" + player.getMaxHp() + "   Potions: " + player.getPotionCount());
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=LanternaRendererTest`
Expected: PASS (18 tests)

- [ ] **Step 5: Run the full test suite**

Run: `mvn -q test`
Expected: PASS (all tests across every class)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java
git commit -m "feat: show potion count on the status line"
```

---

### Task 6: Manual playtest checklist

**Files:** none (verification only)

**Interfaces:** none

- [ ] **Step 1: Run the full test suite**

Run: `mvn -q test`
Expected: PASS, all tests across every class (96 baseline + 2 Task 1 + 5 Task 2 + 2 Task 3 + 4 Task 4 + 1 Task 5 = 110 total).

- [ ] **Step 2: Compile check**

Run: `mvn -q compile`
Expected: no output (success)

- [ ] **Step 3: Manual playtest (human, not an agent — no GUI in this session)**

From a real PowerShell or cmd window at the project root:

```
mvn compile exec:java
```

Confirm in the opened Swing window:
- Killing enemies with the projectile (space bar) sometimes leaves behind a magenta `!` on the floor, sometimes doesn't (roughly 1 in 3 kills)
- Walking onto a `!` tile picks it up automatically — the status line's `Potions: N` counter increases, and the tile becomes empty floor
- Pressing `e` with at least one potion heals the player (HP increases by 10, capped at 20/20) and decrements the potion counter
- Pressing `e` with zero potions does nothing (no crash, no change)
- Pressing `q` still quits the game, same as `Escape` (unchanged from before this phase)
- A dropped potion on a tile you've walked away from still shows dimmed (remembered terrain), consistent with Phase D1's fog-of-war rules — it doesn't wink out just because it's no longer in the currently-visible radius

- [ ] **Step 4: Commit any fixups discovered during manual playtest**

If the manual playtest surfaces a bug, fix it, re-run `mvn -q test`, and commit:

```bash
git add -A
git commit -m "fix: <describe what the manual playtest caught>"
```

If no issues are found, skip this step (nothing to commit).
