# TextRogue — Phase B Design Spec

## Overview

Phase B adds Lanterna to TextRogue, replacing the blocking Scanner input and raw ANSI rendering from Phase A with single-keypress input and double-buffered screen rendering. It also converts the game loop from turn-based (blocks on input) to fixed-rate ticking (advances every 200 ms regardless of input), laying the foundation for enemy movement in Phase C.

No gameplay changes are visible to the player beyond the removal of the Enter-key requirement. The tick loop runs, but since there are no enemies yet the game behaves identically from the player's perspective.

## What Changes

Four elements change. Everything else — `Dungeon`, `Player`, `Tile`, `Room`, `DungeonGenerator` — is untouched.

| File | Change |
|------|--------|
| `input/InputHandler.java` | Converted from concrete class to interface |
| `input/LanternaInputHandler.java` | New: non-blocking Lanterna-backed implementation |
| `render/Renderer.java` | Converted from concrete class to interface |
| `render/LanternaRenderer.java` | New: double-buffered Lanterna Screen implementation |
| `Game.java` | Loop changes from blocking `readCommand()` to fixed-rate tick |
| `Main.java` | Creates Lanterna `Screen`, wires components, owns lifecycle |

The old Scanner-based `InputHandler` and ANSI `Renderer` concrete classes are deleted. They remain in git history.

## Dependency

Add Lanterna 3.x to `pom.xml`:

```xml
<dependency>
    <groupId>com.googlecode.lanterna</groupId>
    <artifactId>lanterna</artifactId>
    <version>3.1.1</version>
</dependency>
```

Lanterna is a production dependency (not test-scoped) because `LanternaInputHandler` and `LanternaRenderer` are production classes. Tests use Lanterna's built-in `DefaultVirtualTerminal` for headless testing.

## Interfaces

### InputHandler

```java
package ca.ninja.thenix.textrogue.input;

public interface InputHandler {
    Command pollCommand();  // non-blocking: returns UNKNOWN immediately if no key is ready
}
```

`pollCommand()` replaces Phase A's `readCommand()`. It never blocks.

### Renderer

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

`close()` is called by `Main` on shutdown to stop the Lanterna screen cleanly.

## LanternaInputHandler

- Constructor: `LanternaInputHandler(Screen screen)`
- `pollCommand()` calls `screen.pollInput()`, which returns `null` immediately if no key is pending
- Key mapping:

| Key | Command |
|-----|---------|
| Character 'w' or Arrow UP | MOVE_UP |
| Character 's' or Arrow DOWN | MOVE_DOWN |
| Character 'a' or Arrow LEFT | MOVE_LEFT |
| Character 'd' or Arrow RIGHT | MOVE_RIGHT |
| Character 'q' or EOF/Escape | QUIT |
| Anything else / null | UNKNOWN |

## LanternaRenderer

- Constructor: `LanternaRenderer(Screen screen)`
- `render(Dungeon, Player)` each call:
  1. `screen.clear()`
  2. Iterate all 80×24 tile positions; write each tile character with its color using `TextGraphics`
  3. Overwrite player position with `@` in bright green
  4. Write `Floor: N` on row 24 (the row below the 24-row map, zero-indexed row 24)
  5. `screen.refresh()`
- `close()` calls `screen.stopScreen()`

### Color Scheme (matches Phase A)

| Element | Lanterna Color |
|---------|---------------|
| `#` Wall | `TextColor.ANSI.BLACK_BRIGHT` |
| `.` Floor | `TextColor.ANSI.WHITE` |
| `>` Stairs | `TextColor.ANSI.YELLOW` |
| `@` Player | `TextColor.ANSI.GREEN_BRIGHT` |

Background color for all cells: `TextColor.ANSI.DEFAULT`.

## Game Loop

`TICK_MS = 200` (5 ticks per second).

```
initialize dungeon and player
render initial state
loop:
    record tick start time
    cmd = input.pollCommand()           // non-blocking
    processCommand(cmd)                 // UNKNOWN is a no-op
    gameTick()                          // empty in Phase B; Phase C adds enemy movement here
    renderer.render(dungeon, player)
    sleep(TICK_MS - elapsed)            // sleep the remainder of the tick window
```

`gameTick()` is package-private and has an empty body in Phase B. Phase C will fill it in with enemy movement logic.

`QUIT` sets `running = false`. The loop exits at the top of the next iteration (or after the current tick completes). One extra render frame may occur after Q — this is acceptable and matches Phase A behavior.

`Thread.sleep()` in the loop requires handling `InterruptedException`. The loop catches it and sets `running = false` to exit cleanly.

## Main

```java
public static void main(String[] args) throws IOException {
    Screen screen = new DefaultTerminalFactory().createScreen();
    screen.startScreen();
    InputHandler input = new LanternaInputHandler(screen);
    Renderer renderer = new LanternaRenderer(screen);
    try {
        new Game(input, renderer).start();
    } finally {
        renderer.close();
    }
}
```

`Main` owns the `Screen` lifecycle. Both `LanternaInputHandler` and `LanternaRenderer` share the same `Screen` instance.

## Testing

### GameTest

The four existing `GameTest` cases remain. The stub implementations change from concrete classes to interface implementations:

- `InputHandler` stub: an anonymous class or lambda-equivalent returning a scripted sequence of commands
- `Renderer` stub: an anonymous class with a no-op `render()` and `close()`

`quitCommandStopsLoop` previously called `game.start()` with a real `InputHandler(ByteArrayInputStream)`. It now uses a stub `InputHandler` that returns `QUIT` on the first `pollCommand()` call, confirming the loop exits.

### LanternaInputHandlerTest

Uses `DefaultVirtualTerminal` (built into Lanterna) to simulate keypresses without a real terminal:

- 'w' → `MOVE_UP`
- 's' → `MOVE_DOWN`
- 'a' → `MOVE_LEFT`
- 'd' → `MOVE_RIGHT`
- Arrow keys → corresponding MOVE_* commands
- 'q' → `QUIT`
- Unrecognized key → `UNKNOWN`
- No key pending (null from `pollInput`) → `UNKNOWN`

### LanternaRendererTest

Uses `DefaultVirtualTerminal` to verify rendered output:

- Correct character at a WALL tile position
- Correct character at a FLOOR tile position
- Correct character at a STAIRS_DOWN tile position
- `@` appears at the player's position
- Status line `Floor: N` appears on row 24

## Dungeon Dimensions

`Dungeon.WIDTH = 80`, `Dungeon.HEIGHT = 24`. The renderer iterates rows 0–23 for the map and writes the status line on row 24. The Lanterna screen must be at least 80×25 to accommodate this. `DefaultTerminalFactory` sizes the screen to the actual terminal window; in Windows Terminal at default size this is satisfied.

## Out of Scope for Phase B

- Enemy movement (Phase C)
- Combat or HP (Phase C)
- Items or inventory (Phase D)
- Fog of war (Phase D)
- Configurable tick rate
- Pause/resume
