# TextRogue Phase D3 (Permadeath) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the existing bare "You died" screen into a real end-of-run checkpoint: show run stats (floor reached, enemies killed, potions used), then wait indefinitely for the player to press `R` (start a brand-new run) or `Esc` (quit) — no fixed timer, no save/reload.

**Architecture:** `Player` gains two new per-run counters (`killCount`, `potionsUsed`) that live and reset exactly like `floor`/`hp` already do, because `Game` always replaces the whole `Player` object on a new run rather than mutating one in place. `Game.start()`'s inline dungeon/player/enemy setup is extracted into a `newRun()` method so it can be called both at startup and on restart. The death branch of the main loop is replaced with a blocking-poll helper (`awaitDeathChoice()`) that waits for `Command.RESTART` or `Command.QUIT` on the same `TICK_MS` cadence the main loop already uses, instead of `Thread.sleep(DEATH_SCREEN_DISPLAY_MS)`. `LanternaRenderer.renderDeath` grows extra centered lines for the stats and the restart/quit prompt.

**Tech Stack:** Java 21, JUnit 5, Lanterna 3.1.1 (`DefaultVirtualTerminal` for headless render/input tests).

## Global Constraints

- Java 21 language level, no new third-party dependencies.
- Follow existing code style: plain JUnit 5 `@Test` methods, no mocking framework, package-private fields/methods on `Game` for test seams (already established by `dungeon`/`player`/`enemies`/`projectiles`/`random`).
- `Command.RESTART` is a new enum constant, inserted after `DRINK_POTION` and before `QUIT`. Bound to `'r'`/`'R'` in `LanternaInputHandler`.
- `Player.killCount` and `Player.potionsUsed` both start at 0 in the constructor. `Player.recordKill()` increments `killCount`. `Player.drinkPotion(int)` increments `potionsUsed` only on a successful drink (inside the existing `potionCount > 0` branch).
- `Game.DEATH_SCREEN_DISPLAY_MS` is removed entirely — there is no more fixed death-screen timer.
- On death, `Game` polls `input.pollCommand()` once per `TICK_MS`, ignoring anything but `Command.RESTART`/`Command.QUIT`, until one of those arrives.
- `Command.RESTART` is a no-op when it arrives via the normal in-game `processCommand` switch (i.e., during active play, pressing `r` does nothing) — it is only meaningful while `Game` is in the death-screen wait loop.
- Restarting calls the same `newRun()` logic used at startup: a brand-new `Dungeon` (fresh seed), a brand-new `Player` at that dungeon's start position (so `floor`/`hp`/`killCount`/`potionsUsed`/`potionCount` are all back to their defaults), a fresh enemy spawn, and `shootCooldown` reset to 0.
- `Renderer.renderDeath(Player)` keeps its existing signature — no new parameters. `LanternaRenderer.renderDeath` reads `player.getFloor()`, `player.getKillCount()`, `player.getPotionsUsed()` directly.
- Death screen layout (all centered horizontally, `y = Dungeon.HEIGHT / 2` for the first line):
  ```
  You died

  Floor reached: <floor>
  Enemies killed: <killCount>
  Potions used: <potionsUsed>

  Press [R] to restart, [Esc] to quit
  ```

---

### Task 1: Player run stats (killCount, potionsUsed)

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/entity/Player.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java`

**Interfaces:**
- Produces: `Player.getKillCount() -> int`, `Player.recordKill()`, `Player.getPotionsUsed() -> int` (also wired into the existing `drinkPotion(int)`). Used by Task 3 (`Game`'s kill wiring and restart-reset test) and Task 4 (`LanternaRenderer`'s death screen).

- [ ] **Step 1: Write the failing tests**

Append inside `class PlayerTest { ... }`:

```java
    @Test
    void startsWithZeroKillsAndPotionsUsed() {
        Player player = new Player(0, 0);
        assertEquals(0, player.getKillCount());
        assertEquals(0, player.getPotionsUsed());
    }

    @Test
    void recordKillIncrementsKillCount() {
        Player player = new Player(0, 0);
        player.recordKill();
        player.recordKill();
        assertEquals(2, player.getKillCount());
    }

    @Test
    void drinkPotionIncrementsPotionsUsedOnSuccess() {
        Player player = new Player(0, 0);
        player.addPotion();
        player.drinkPotion(10);
        assertEquals(1, player.getPotionsUsed());
    }

    @Test
    void drinkPotionWithNoneDoesNotIncrementPotionsUsed() {
        Player player = new Player(0, 0);
        player.drinkPotion(10);
        assertEquals(0, player.getPotionsUsed());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=PlayerTest`
Expected: FAIL (compilation error — `getKillCount`/`recordKill`/`getPotionsUsed` do not exist)

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
    private int killCount;
    private int potionsUsed;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.floor = 1;
        this.hp = MAX_HP;
        this.facing = Facing.DOWN;
        this.potionCount = 0;
        this.killCount = 0;
        this.potionsUsed = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getFloor() { return floor; }
    public int getHp() { return hp; }
    public int getMaxHp() { return MAX_HP; }
    public Facing getFacing() { return facing; }
    public int getPotionCount() { return potionCount; }
    public int getKillCount() { return killCount; }
    public int getPotionsUsed() { return potionsUsed; }

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

    public void recordKill() {
        killCount++;
    }

    public boolean drinkPotion(int healAmount) {
        if (potionCount <= 0) return false;
        potionCount--;
        potionsUsed++;
        heal(healAmount);
        return true;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -Dtest=PlayerTest`
Expected: PASS (19 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/entity/Player.java src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java
git commit -m "feat: track kill count and potions used on Player"
```

---

### Task 2: Command.RESTART and its key binding

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/input/Command.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java:87-98` (`processCommand`)
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Produces: `Command.RESTART` enum constant; `'r'`/`'R'` key maps to it. Used by Task 3 (`Game`'s death-screen wait loop).

`Game.processCommand`'s switch has no `default` arm, so it must be exhaustive over `Command` — the moment `Command.RESTART` exists, that method stops compiling until it has a matching case. This task adds a no-op `case RESTART -> {}` in the same commit as the enum constant, so the build stays green; Task 3 gives `RESTART` its real meaning in the death-screen wait loop (which reads `input.pollCommand()` directly, not through `processCommand`).

- [ ] **Step 1: Write the failing tests**

In `LanternaInputHandlerTest.java`, append inside `class LanternaInputHandlerTest { ... }`:

```java
    @Test
    void rKeyReturnsRestart() throws IOException {
        assertEquals(Command.RESTART, handlerWithKey(new KeyStroke('r', false, false)).pollCommand());
    }
```

In `GameTest.java`, append inside `class GameTest { ... }`:

```java
    @Test
    void restartCommandDuringActivePlayDoesNothing() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        game.processCommand(Command.RESTART);
        assertEquals(40, game.player.getX());
        assertEquals(12, game.player.getY());
        assertFalse(game.player.isDead());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=LanternaInputHandlerTest,GameTest`
Expected: FAIL — both fail to compile (`Command.RESTART` does not exist yet)

- [ ] **Step 3: Add RESTART to Command**

Replace `Command.java` contents:

```java
package ca.ninja.thenix.textrogue.input;

public enum Command {
    MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, SHOOT, DRINK_POTION, RESTART, QUIT, UNKNOWN
}
```

- [ ] **Step 4: Bind 'r' to RESTART in LanternaInputHandler**

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
                    case 'e' -> Command.DRINK_POTION;
                    case 'r' -> Command.RESTART;
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

- [ ] **Step 5: Add a no-op RESTART case to Game.processCommand**

In `Game.java`, replace the `processCommand` method:

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
            case RESTART      -> {} // only meaningful on the death screen; see Game.awaitDeathChoice (Task 3)
            case QUIT         -> running = false;
            case UNKNOWN      -> {}
        }
    }
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn -q test -Dtest=LanternaInputHandlerTest,GameTest`
Expected: PASS (12 + 27 = 39 tests)

- [ ] **Step 7: Run the full test suite**

Run: `mvn -q test`
Expected: PASS (all tests across every class — confirms the exhaustiveness fix kept `Game.java` compiling)

- [ ] **Step 8: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/input/Command.java src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: add RESTART command bound to 'r' (no-op during active play)"
```

---

### Task 3: Game — newRun() extraction, kill-count wiring, death-screen wait loop

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`

**Interfaces:**
- Consumes: `Player.recordKill()`, `Player.getKillCount()`, `Player.getPotionsUsed()` (Task 1); `Command.RESTART` (Task 2).
- Produces: `Game.newRun()` (package-private, resets `dungeon`/`player`/`enemies`/`projectiles`/`shootCooldown`), `Game.awaitDeathChoice()` (private, blocks until `RESTART` or `QUIT`). Used by Task 5's manual playtest only — `LanternaRenderer` (Task 4) only needs `Player`'s getters from Task 1.

- [ ] **Step 1: Write the failing tests**

In `GameTest.java`, replace the existing `playerDeathStopsLoopAndRendersDeathScreen` test:

```java
    @Test
    void playerDeathStopsLoopAndRendersDeathScreen() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        player.takeDamage(player.getHp());
        boolean[] deathRendered = {false};
        Renderer capturingRenderer = new Renderer() {
            public void render(Dungeon d, Player p, List<Enemy> e, List<Projectile> pr, Set<Position> visibleTiles) {}
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
```

with:

```java
    @Test
    void playerDeathStopsLoopAndRendersDeathScreen() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        player.takeDamage(player.getHp());
        boolean[] deathRendered = {false};
        Renderer capturingRenderer = new Renderer() {
            public void render(Dungeon d, Player p, List<Enemy> e, List<Projectile> pr, Set<Position> visibleTiles) {}
            public void renderDeath(Player p) { deathRendered[0] = true; }
            public void close() {}
        };
        int[] pollCount = {0};
        InputHandler input = () -> {
            pollCount[0]++;
            return pollCount[0] < 2 ? Command.UNKNOWN : Command.QUIT;
        };
        Game game = new Game(input, capturingRenderer);
        game.dungeon = dungeon;
        game.player = player;
        game.start();
        assertTrue(deathRendered[0]);
        assertFalse(game.isRunning());
    }

    @Test
    void deathScreenWaitsForRestartOrQuitIgnoringOtherCommands() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        player.takeDamage(player.getHp());
        int[] pollCount = {0};
        InputHandler input = () -> {
            pollCount[0]++;
            // first poll (pre-death processCommand) and two more inside the
            // death-screen wait loop all return UNKNOWN/gameplay commands
            // before QUIT finally arrives.
            return switch (pollCount[0]) {
                case 1 -> Command.UNKNOWN;
                case 2 -> Command.MOVE_UP;
                case 3 -> Command.SHOOT;
                default -> Command.QUIT;
            };
        };
        Game game = new Game(input, NULL_RENDERER);
        game.dungeon = dungeon;
        game.player = player;
        game.start();
        assertFalse(game.isRunning());
        assertTrue(pollCount[0] >= 4);
    }

    @Test
    void killingEnemyIncrementsPlayerKillCount() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12, 41, 12);
        Player player = new Player(40, 12);
        Game game = gameWithInjectedState(dungeon, player);
        Enemy enemy = new Enemy(41, 12, 2);
        game.enemies.add(enemy);
        game.projectiles.add(new Projectile(41, 12, Facing.RIGHT, 5));
        game.gameTick();
        assertEquals(1, game.player.getKillCount());
    }

    @Test
    void restartAfterDeathStartsFreshRunAndResetsStats() {
        Dungeon dungeon = dungeonWithFloorAt(40, 12);
        Player player = new Player(40, 12);
        player.descendFloor();
        player.descendFloor();
        player.recordKill();
        player.recordKill();
        player.addPotion();
        player.drinkPotion(5);
        player.takeDamage(player.getHp());
        Dungeon dungeonBefore = dungeon;
        int[] pollCount = {0};
        InputHandler input = () -> {
            pollCount[0]++;
            return switch (pollCount[0]) {
                case 1 -> Command.UNKNOWN; // pre-death processCommand poll
                case 2 -> Command.RESTART; // first poll inside the death-screen wait loop
                default -> Command.QUIT;   // stops the fresh run's loop immediately
            };
        };
        Game game = new Game(input, NULL_RENDERER);
        game.dungeon = dungeon;
        game.player = player;
        game.start();
        assertFalse(game.isRunning());
        assertNotSame(dungeonBefore, game.dungeon);
        assertFalse(game.player.isDead());
        assertEquals(1, game.player.getFloor());
        assertEquals(0, game.player.getKillCount());
        assertEquals(0, game.player.getPotionsUsed());
    }
```

The new tests reference `NULL_RENDERER` — this static field already exists at the top of `GameTest` (predates this phase), so no new field is needed; just confirm it's there before writing the tests above.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -Dtest=GameTest`
Expected: FAIL. Against today's `Game.java`, death still triggers a fixed `Thread.sleep(DEATH_SCREEN_DISPLAY_MS)` (3000ms) followed by an unconditional `running = false` — it never looks at further input, so:
- `playerDeathStopsLoopAndRendersDeathScreen` actually passes (slowly, ~3s) — it only asserts the death screen rendered and the loop stopped, both still true today.
- `deathScreenWaitsForRestartOrQuitIgnoringOtherCommands` FAILS: `pollCount[0]` only reaches 1 (old code never polls again after the first death check), so `assertTrue(pollCount[0] >= 4)` fails.
- `killingEnemyIncrementsPlayerKillCount` FAILS: nothing calls `player.recordKill()` yet, so `getKillCount()` stays 0.
- `restartAfterDeathStartsFreshRunAndResetsStats` FAILS: old code never replaces `dungeon`, so `assertNotSame(dungeonBefore, game.dungeon)` fails.

Each of the three failing tests takes ~3s to fail (they still run the old fixed sleep before their assertions), so this run is slow but should still finish within the default test timeout.

- [ ] **Step 3: Extract newRun() and use it at startup**

In `Game.java`, replace the `start` method's setup and the `DEATH_SCREEN_DISPLAY_MS` constant. First, remove this line from the constants block near the top of the class:

```java
    static final int DEATH_SCREEN_DISPLAY_MS = 3000;
```

Then replace the `start` method:

```java
    public void start() {
        if (dungeon == null) {
            dungeon = new DungeonGenerator(System.currentTimeMillis()).generate();
            player = new Player(dungeon.getStartX(), dungeon.getStartY());
            spawnEnemies();
        }
        running = true;
        renderer.render(dungeon, player, enemies, projectiles, updateVisibility());
        while (running) {
            long tickStart = System.currentTimeMillis();
            processCommand(input.pollCommand());
            gameTick();
            renderer.render(dungeon, player, enemies, projectiles, updateVisibility());
            if (player.isDead()) {
                renderer.renderDeath(player);
                running = false;
                try {
                    Thread.sleep(DEATH_SCREEN_DISPLAY_MS);
                } catch (InterruptedException e) {
                    // proceed to exit regardless
                }
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

with:

```java
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
```

- [ ] **Step 4: Record a kill when a projectile hit finishes off an enemy**

In `Game.java`, replace the `advanceProjectiles` method:

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
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q test -Dtest=GameTest`
Expected: PASS (30 tests)

- [ ] **Step 6: Run the full test suite**

Run: `mvn -q test`
Expected: PASS (all tests across every class)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: wait for restart/quit on death, add newRun(), track kills"
```

---

### Task 4: Renderer — richer death screen

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java`

**Interfaces:**
- Consumes: `Player.getFloor()` (existing), `Player.getKillCount()`, `Player.getPotionsUsed()` (Task 1).
- Produces: nothing new consumed by later tasks — this is the last production-code task in the phase.

- [ ] **Step 1: Write the failing test**

Append inside `class LanternaRendererTest { ... }`:

```java
    private String rowText(DefaultVirtualTerminal terminal, int row) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < Dungeon.WIDTH; x++) sb.append(terminal.getCharacter(new TerminalPosition(x, row)).getCharacter());
        return sb.toString();
    }

    @Test
    void renderDeathShowsStatsAndRestartPrompt() throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        Player player = new Player(0, 0);
        player.descendFloor();
        player.descendFloor();
        player.recordKill();
        player.recordKill();
        player.recordKill();
        player.addPotion();
        player.drinkPotion(5);
        new LanternaRenderer(screen).renderDeath(player);
        int y = Dungeon.HEIGHT / 2;
        assertTrue(rowText(terminal, y).contains("You died"));
        assertTrue(rowText(terminal, y + 2).contains("Floor reached: 3"));
        assertTrue(rowText(terminal, y + 3).contains("Enemies killed: 3"));
        assertTrue(rowText(terminal, y + 4).contains("Potions used: 1"));
        assertTrue(rowText(terminal, y + 6).contains("Press [R] to restart, [Esc] to quit"));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=LanternaRendererTest`
Expected: FAIL — `renderDeathShowsStatsAndRestartPrompt` fails its assertions (the stat lines and prompt don't exist yet)

- [ ] **Step 3: Add the stat lines and prompt to renderDeath**

In `LanternaRenderer.java`, replace the `renderDeath` method:

```java
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
```

with:

```java
    @Override
    public void renderDeath(Player player) {
        screen.clear();
        TextGraphics g = screen.newTextGraphics();
        int y = Dungeon.HEIGHT / 2;
        g.setForegroundColor(TextColor.ANSI.RED);
        drawCentered(g, y, "You died");
        g.setForegroundColor(TextColor.ANSI.WHITE);
        drawCentered(g, y + 2, "Floor reached: " + player.getFloor());
        drawCentered(g, y + 3, "Enemies killed: " + player.getKillCount());
        drawCentered(g, y + 4, "Potions used: " + player.getPotionsUsed());
        drawCentered(g, y + 6, "Press [R] to restart, [Esc] to quit");
        try {
            screen.refresh();
        } catch (IOException ignored) {}
    }

    private void drawCentered(TextGraphics g, int y, String text) {
        int x = Math.max(0, (Dungeon.WIDTH - text.length()) / 2);
        g.putString(x, y, text);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=LanternaRendererTest`
Expected: PASS (19 tests)

- [ ] **Step 5: Run the full test suite**

Run: `mvn -q test`
Expected: PASS (all tests across every class)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java
git commit -m "feat: show run stats and restart prompt on the death screen"
```

---

### Task 5: Manual playtest checklist

**Files:** none (verification only)

**Interfaces:** none

- [ ] **Step 1: Run the full test suite**

Run: `mvn -q test`
Expected: PASS, all tests across every class (110 baseline + 4 Task 1 + 2 Task 2 + 3 Task 3 + 1 Task 4 = 120 total; note Task 3 replaces one existing test and adds three, and Task 2 adds one to `GameTest` and one to `LanternaInputHandlerTest`).

- [ ] **Step 2: Compile check**

Run: `mvn -q compile`
Expected: no output (success)

- [ ] **Step 3: Manual playtest (human, not an agent — no GUI in this session)**

From a real PowerShell or cmd window at the project root:

```
mvn compile exec:java
```

Confirm in the opened Swing window:
- Walk into an enemy's projectile fire (or let enemies chase you down) until HP reaches 0 — the death screen appears showing "You died", "Floor reached: N", "Enemies killed: N", "Potions used: N" with values matching what happened during the run, and the "Press [R] to restart, [Esc] to quit" prompt
- The death screen does not disappear on its own — it stays up indefinitely until a key is pressed
- Pressing keys other than `R`/`Esc` (e.g. `W`, `Space`) on the death screen does nothing
- Pressing `R` starts a brand-new run: a new dungeon, player back at floor 1 with full HP, 0 potions, 0 kills, and the status line reflects the reset
- Dying again after a restart shows updated stats for the new run (not cumulative with the previous run)
- Pressing `Esc` on the death screen quits the game (window closes / process exits)
- Pressing `r` during normal (non-death) gameplay does nothing

- [ ] **Step 4: Commit any fixups discovered during manual playtest**

If the manual playtest surfaces a bug, fix it, re-run `mvn -q test`, and commit:

```bash
git add -A
git commit -m "fix: <describe what the manual playtest caught>"
```

If no issues are found, skip this step (nothing to commit).
