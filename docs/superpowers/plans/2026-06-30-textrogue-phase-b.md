# TextRogue Phase B — Lanterna + Tick Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the blocking Scanner/ANSI implementation with Lanterna for single-keypress input and double-buffered rendering, and convert the game loop to a fixed-rate tick loop that fires every 200 ms regardless of player input.

**Architecture:** `InputHandler` and `Renderer` become interfaces; `LanternaInputHandler` and `LanternaRenderer` are the only production implementations. `Game.start()` changes from a blocking input loop to a 200 ms tick loop with a `gameTick()` hook for Phase C. `Main` creates a Lanterna `Screen`, passes it to both components, and owns the lifecycle.

**Tech Stack:** Java 21, Maven 3.x, Lanterna 3.1.1, JUnit Jupiter 5.10.2

## Global Constraints

- Group ID `ca.ninja.thenix`, artifact `TextRogue`, Java 21 source/target
- Lanterna version: `3.1.1` exactly
- `InputHandler` is a `@FunctionalInterface` in `ca.ninja.thenix.textrogue.input` with one method: `Command pollCommand()`
- `Renderer` is an interface in `ca.ninja.thenix.textrogue.render` with two methods: `void render(Dungeon, Player)` and `void close() throws IOException`
- `Game.TICK_MS = 200` (package-visible static final int)
- `Game.gameTick()` is package-private, empty body
- `Game.dungeon` and `Game.player` remain package-private (no access modifier) for test injection
- `Game.processCommand()` and `Game.isRunning()` remain package-private
- Tick loop skips `Thread.sleep` when `running` is false (avoids 200 ms delay on quit in tests)
- `Dungeon.WIDTH = 80`, `Dungeon.HEIGHT = 24`; status line is written at row `Dungeon.HEIGHT` (row 24)
- Maven PATH must be prepended in PowerShell before running any `mvn` command:
  `$env:PATH = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot\bin;C:\Program Files\Apache\maven\bin;" + $env:PATH`
- Run command: `mvn compile exec:java`

---

## File Map

| Action | File |
|--------|------|
| Modify | `pom.xml` |
| Replace (interface) | `src/main/java/ca/ninja/thenix/textrogue/input/InputHandler.java` |
| Create | `src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java` |
| Replace (interface) | `src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java` |
| Create | `src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java` |
| Modify | `src/main/java/ca/ninja/thenix/textrogue/Game.java` |
| Modify | `src/main/java/ca/ninja/thenix/textrogue/Main.java` |
| Modify | `src/test/java/ca/ninja/thenix/textrogue/GameTest.java` |
| Delete + Replace | `src/test/java/ca/ninja/thenix/textrogue/input/InputHandlerTest.java` → `LanternaInputHandlerTest.java` |
| Delete + Replace | `src/test/java/ca/ninja/thenix/textrogue/render/RendererTest.java` → `LanternaRendererTest.java` |

---

## Task 1: Add Lanterna 3.1.1 Dependency

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Produces: Lanterna classes available on the classpath for Tasks 2–4

- [ ] **Step 1: Add Lanterna to pom.xml**

In `pom.xml`, add inside `<dependencies>` after the JUnit dependency:

```xml
<dependency>
    <groupId>com.googlecode.lanterna</groupId>
    <artifactId>lanterna</artifactId>
    <version>3.1.1</version>
</dependency>
```

The complete `<dependencies>` block becomes:

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.googlecode.lanterna</groupId>
        <artifactId>lanterna</artifactId>
        <version>3.1.1</version>
    </dependency>
</dependencies>
```

- [ ] **Step 2: Verify dependency resolves and all existing tests still pass**

```powershell
$env:PATH = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot\bin;C:\Program Files\Apache\maven\bin;" + $env:PATH
cd "C:\Programming Projects\Java\TextRogue"
mvn test
```

Expected: `BUILD SUCCESS`, `Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add Lanterna 3.1.1 dependency"
```

---

## Task 2: InputHandler Interface + LanternaInputHandler

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/input/InputHandler.java` (replace with interface)
- Create: `src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java` (one line: `readCommand()` → `pollCommand()`)
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java` (update stubs)
- Delete: `src/test/java/ca/ninja/thenix/textrogue/input/InputHandlerTest.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java`

**Interfaces:**
- Consumes: Lanterna `Screen`, `KeyStroke`, `KeyType` (from Task 1 dependency)
- Produces:
  - `InputHandler` interface: `Command pollCommand()`
  - `LanternaInputHandler(Screen screen)` constructor

- [ ] **Step 1: Replace InputHandler.java with the interface**

Overwrite `src/main/java/ca/ninja/thenix/textrogue/input/InputHandler.java` with:

```java
package ca.ninja.thenix.textrogue.input;

@FunctionalInterface
public interface InputHandler {
    Command pollCommand();
}
```

- [ ] **Step 2: Update Game.java to call pollCommand() instead of readCommand()**

In `src/main/java/ca/ninja/thenix/textrogue/Game.java`, in `start()`, change the one line:

```java
// Before:
Command cmd = input.readCommand();

// After:
Command cmd = input.pollCommand();
```

No other changes to Game.java in this task.

- [ ] **Step 3: Delete InputHandlerTest.java and update GameTest.java**

Delete `src/test/java/ca/ninja/thenix/textrogue/input/InputHandlerTest.java`.

Overwrite `src/test/java/ca/ninja/thenix/textrogue/GameTest.java` with the version below. The only changes from Phase A are: (a) `gameWithInjectedState` uses a lambda stub for `InputHandler`, (b) `quitCommandStopsLoop` uses a lambda stub, (c) the `ByteArrayInputStream` import is removed:

```java
package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private static final PrintStream NULL_OUT = new PrintStream(OutputStream.nullOutputStream());

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
        Game game = new Game(() -> Command.UNKNOWN, new Renderer(NULL_OUT));
        game.dungeon = dungeon;
        game.player = player;
        return game;
    }

    @Test
    void quitCommandStopsLoop() {
        Game game = new Game(() -> Command.QUIT, new Renderer(NULL_OUT));
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
}
```

- [ ] **Step 4: Verify the existing tests still pass (8 fewer now — InputHandlerTest deleted)**

```powershell
$env:PATH = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot\bin;C:\Program Files\Apache\maven\bin;" + $env:PATH
cd "C:\Programming Projects\Java\TextRogue"
mvn test
```

Expected: `BUILD SUCCESS`, `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Write LanternaInputHandlerTest.java (failing — class doesn't exist yet)**

Create `src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java`:

```java
package ca.ninja.thenix.textrogue.input;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class LanternaInputHandlerTest {

    private LanternaInputHandler handlerWithKey(KeyStroke key) throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        terminal.addInput(key);
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        return new LanternaInputHandler(screen);
    }

    private LanternaInputHandler handlerWithNoKey() throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        return new LanternaInputHandler(screen);
    }

    @Test
    void wKeyReturnsMoveUp() throws IOException {
        assertEquals(Command.MOVE_UP, handlerWithKey(new KeyStroke('w', false, false)).pollCommand());
    }

    @Test
    void sKeyReturnsMoveDown() throws IOException {
        assertEquals(Command.MOVE_DOWN, handlerWithKey(new KeyStroke('s', false, false)).pollCommand());
    }

    @Test
    void aKeyReturnsMoveLeft() throws IOException {
        assertEquals(Command.MOVE_LEFT, handlerWithKey(new KeyStroke('a', false, false)).pollCommand());
    }

    @Test
    void dKeyReturnsMoveRight() throws IOException {
        assertEquals(Command.MOVE_RIGHT, handlerWithKey(new KeyStroke('d', false, false)).pollCommand());
    }

    @Test
    void qKeyReturnsQuit() throws IOException {
        assertEquals(Command.QUIT, handlerWithKey(new KeyStroke('q', false, false)).pollCommand());
    }

    @Test
    void arrowUpReturnsMoveUp() throws IOException {
        assertEquals(Command.MOVE_UP, handlerWithKey(new KeyStroke(KeyType.ArrowUp)).pollCommand());
    }

    @Test
    void unknownKeyReturnsUnknown() throws IOException {
        assertEquals(Command.UNKNOWN, handlerWithKey(new KeyStroke('x', false, false)).pollCommand());
    }

    @Test
    void noPendingKeyReturnsUnknown() throws IOException {
        assertEquals(Command.UNKNOWN, handlerWithNoKey().pollCommand());
    }
}
```

- [ ] **Step 6: Run tests — expect compile failure**

```powershell
mvn test
```

Expected: `BUILD FAILURE` — `cannot find symbol: class LanternaInputHandler`

- [ ] **Step 7: Create LanternaInputHandler.java**

Create `src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java`:

```java
package ca.ninja.thenix.textrogue.input;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;

public class LanternaInputHandler implements InputHandler {
    private final Screen screen;

    public LanternaInputHandler(Screen screen) {
        this.screen = screen;
    }

    @Override
    public Command pollCommand() {
        try {
            KeyStroke key = screen.pollInput();
            if (key == null) return Command.UNKNOWN;
            return switch (key.getKeyType()) {
                case Character -> {
                    char c = Character.toLowerCase(key.getCharacter());
                    yield switch (c) {
                        case 'w' -> Command.MOVE_UP;
                        case 's' -> Command.MOVE_DOWN;
                        case 'a' -> Command.MOVE_LEFT;
                        case 'd' -> Command.MOVE_RIGHT;
                        case 'q' -> Command.QUIT;
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
        } catch (IOException e) {
            return Command.QUIT;
        }
    }
}
```

- [ ] **Step 8: Run all tests — expect 39 passing**

```powershell
mvn test
```

Expected: `BUILD SUCCESS`, `Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 9: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/input/InputHandler.java \
        src/main/java/ca/ninja/thenix/textrogue/input/LanternaInputHandler.java \
        src/main/java/ca/ninja/thenix/textrogue/Game.java \
        src/test/java/ca/ninja/thenix/textrogue/GameTest.java \
        src/test/java/ca/ninja/thenix/textrogue/input/LanternaInputHandlerTest.java
git rm src/test/java/ca/ninja/thenix/textrogue/input/InputHandlerTest.java
git commit -m "feat: extract InputHandler interface and add LanternaInputHandler"
```

---

## Task 3: Renderer Interface + LanternaRenderer

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java` (replace with interface)
- Create: `src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java`
- Modify: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java` (swap renderer stubs)
- Delete: `src/test/java/ca/ninja/thenix/textrogue/render/RendererTest.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java`

**Interfaces:**
- Consumes:
  - `InputHandler` interface (Task 2)
  - Lanterna `Screen`, `TextGraphics`, `TextColor` (Task 1)
- Produces:
  - `Renderer` interface: `void render(Dungeon, Player)` and `void close() throws IOException`
  - `LanternaRenderer(Screen screen)` constructor

- [ ] **Step 1: Replace Renderer.java with the interface**

Overwrite `src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java` with:

```java
package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import java.io.IOException;

public interface Renderer {
    void render(Dungeon dungeon, Player player);
    void close() throws IOException;
}
```

- [ ] **Step 2: Update GameTest.java — swap PrintStream stub for Renderer anonymous class**

Overwrite `src/test/java/ca/ninja/thenix/textrogue/GameTest.java` completely:

```java
package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private static final Renderer NULL_RENDERER = new Renderer() {
        public void render(Dungeon d, Player p) {}
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
}
```

- [ ] **Step 3: Delete RendererTest.java and run tests — 4 fewer tests now**

Delete `src/test/java/ca/ninja/thenix/textrogue/render/RendererTest.java`.

```powershell
mvn test
```

Expected: `BUILD SUCCESS`, `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 4: Write LanternaRendererTest.java (failing — class doesn't exist yet)**

Create `src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java`:

```java
package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
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

    private DefaultVirtualTerminal renderTo(Dungeon dungeon, Player player) throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        new LanternaRenderer(screen).render(dungeon, player);
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
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(5, 5));
        assertEquals('#', t.getCharacter(new TerminalPosition(0, 0)).getCharacter());
    }

    @Test
    void floorTileRendersDot() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(2, 2, Tile.FLOOR), new Player(5, 5));
        assertEquals('.', t.getCharacter(new TerminalPosition(2, 2)).getCharacter());
    }

    @Test
    void stairsDownRendersGreaterThan() throws IOException {
        DefaultVirtualTerminal t = renderTo(withTileAt(4, 4, Tile.STAIRS_DOWN), new Player(5, 5));
        assertEquals('>', t.getCharacter(new TerminalPosition(4, 4)).getCharacter());
    }

    @Test
    void playerRendersAsAt() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(3, 3));
        assertEquals('@', t.getCharacter(new TerminalPosition(3, 3)).getCharacter());
    }

    @Test
    void statusLineShowsFloorNumber() throws IOException {
        DefaultVirtualTerminal t = renderTo(allWalls(), new Player(0, 0));
        // "Floor: 1" is written at row Dungeon.HEIGHT (= 24), column 0
        assertEquals('F', t.getCharacter(new TerminalPosition(0, Dungeon.HEIGHT)).getCharacter());
    }
}
```

- [ ] **Step 5: Run tests — expect compile failure**

```powershell
mvn test
```

Expected: `BUILD FAILURE` — `cannot find symbol: class LanternaRenderer`

- [ ] **Step 6: Create LanternaRenderer.java**

Create `src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java`:

```java
package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;

public class LanternaRenderer implements Renderer {
    private final Screen screen;

    public LanternaRenderer(Screen screen) {
        this.screen = screen;
    }

    @Override
    public void render(Dungeon dungeon, Player player) {
        screen.clear();
        TextGraphics g = screen.newTextGraphics();
        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                if (x == player.getX() && y == player.getY()) {
                    g.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
                    g.putString(x, y, "@");
                } else {
                    Tile tile = dungeon.getTile(x, y);
                    g.setForegroundColor(colorFor(tile));
                    g.putString(x, y, String.valueOf(tile.toChar()));
                }
            }
        }
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(0, Dungeon.HEIGHT, "Floor: " + player.getFloor());
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

- [ ] **Step 7: Run all tests — expect 40 passing**

```powershell
mvn test
```

Expected: `BUILD SUCCESS`, `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 8: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java \
        src/main/java/ca/ninja/thenix/textrogue/render/LanternaRenderer.java \
        src/test/java/ca/ninja/thenix/textrogue/GameTest.java \
        src/test/java/ca/ninja/thenix/textrogue/render/LanternaRendererTest.java
git rm src/test/java/ca/ninja/thenix/textrogue/render/RendererTest.java
git commit -m "feat: extract Renderer interface and add LanternaRenderer"
```

---

## Task 4: Fixed-Rate Tick Loop + gameTick() + Main Wiring

**Files:**
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Main.java`

**Interfaces:**
- Consumes:
  - `InputHandler.pollCommand()` (Task 2)
  - `Renderer.render()` and `Renderer.close()` (Task 3)
  - `LanternaInputHandler(Screen)` (Task 2)
  - `LanternaRenderer(Screen)` (Task 3)
  - Lanterna: `DefaultTerminalFactory`, `Screen`
- Produces:
  - `Game.TICK_MS = 200`
  - `Game.gameTick()` — package-private, empty body (Phase C hook)
  - Playable game launched by `mvn compile exec:java`

- [ ] **Step 1: Rewrite Game.java with tick loop and gameTick()**

Overwrite `src/main/java/ca/ninja/thenix/textrogue/Game.java` with:

```java
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
```

Key changes from Phase A:
- `TICK_MS = 200` constant
- `start()` loop: `pollCommand()` (non-blocking), `gameTick()` call, fixed-rate sleep
- Sleep only runs when `running` is still true (avoids 200 ms delay on quit)
- `gameTick()` — empty, package-private

- [ ] **Step 2: Run tests — all 40 should pass**

```powershell
$env:PATH = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot\bin;C:\Program Files\Apache\maven\bin;" + $env:PATH
cd "C:\Programming Projects\Java\TextRogue"
mvn test
```

Expected: `BUILD SUCCESS`, `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 3: Rewrite Main.java to wire Lanterna**

Overwrite `src/main/java/ca/ninja/thenix/textrogue/Main.java` with:

```java
package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.input.LanternaInputHandler;
import ca.ninja.thenix.textrogue.render.LanternaRenderer;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Screen screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();
        LanternaInputHandler input = new LanternaInputHandler(screen);
        LanternaRenderer renderer = new LanternaRenderer(screen);
        try {
            new Game(input, renderer).start();
        } finally {
            renderer.close();
        }
    }
}
```

- [ ] **Step 4: Compile and run the game manually**

```powershell
mvn compile exec:java
```

Expected: the game launches in the terminal window. Press `w`/`a`/`s`/`d` to move without pressing Enter. Press `q` to quit. The dungeon should render correctly with `@` player character, `#` walls, `.` floors, `>` stairs.

If running in Windows Terminal and the screen appears garbled or outside the terminal (a GUI window opens), that means `DefaultTerminalFactory` fell back to Swing. This is a known Lanterna behaviour when the Maven process doesn't have a proper TTY. The fix is to run the compiled jar directly from the terminal instead of through Maven exec plugin. If this happens, note it as a concern in the report — it is a runtime environment issue, not a code bug.

- [ ] **Step 5: Run tests one final time to confirm nothing broke**

```powershell
mvn test
```

Expected: `BUILD SUCCESS`, `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ca/ninja/thenix/textrogue/Game.java \
        src/main/java/ca/ninja/thenix/textrogue/Main.java
git commit -m "feat: add fixed-rate tick loop, gameTick hook, and Lanterna Main wiring"
```
