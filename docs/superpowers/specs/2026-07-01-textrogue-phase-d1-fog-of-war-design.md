# TextRogue — Phase D1 (Fog of War) Design Spec

## Overview

Phase D bundles three fairly independent subsystems from the original roadmap — items, fog of war, permadeath. Rather than one large spec, Phase D is split into three sub-phases, each with its own spec → plan → implementation cycle: **D1 (fog of war, this spec) → D2 (items) → D3 (permadeath)**. Fog of war goes first because it's purely a visibility/rendering concern layered on the existing `Dungeon`/`Player` data — no new entities, lowest risk, and it changes how every later feature is *seen* rather than what exists.

Visibility for D1 is **radius-based** (Chebyshev distance from the player, ignoring walls) rather than true line-of-sight/shadowcasting. This is a deliberate simplification, explicitly scoped so a real shadowcasting algorithm can be swapped in later without touching `Game` or the renderer: visibility computation lives behind a `VisibilityCalculator` interface from day one.

Once a tile has been seen, it stays visible on screen in a dimmed color even after the player moves away ("remembered" terrain) — only truly unexplored tiles are blank. Enemies and projectiles, unlike terrain, only ever render on tiles within the *current* tick's visibility set, since their positions go stale the moment they move.

## What Changes

| File | Change |
|------|--------|
| `world/Position.java` | New: `record Position(int x, int y)` — a shared coordinate type |
| `vision/VisibilityCalculator.java` | New interface: one method, computes the current visible-tile set |
| `vision/RadiusVisibilityCalculator.java` | New: radius-based implementation (Chebyshev distance, ignores walls) |
| `world/Dungeon.java` | Adds an `explored` boolean grid (parallel to `tiles`) + `markExplored(x,y)`/`isExplored(x,y)` |
| `render/Renderer.java` | `render(...)` gains a `Set<Position> visibleTiles` parameter |
| `render/LanternaRenderer.java` | Three-tier tile rendering (unexplored/remembered/visible); enemies/projectiles gated on `visibleTiles` |
| `Game.java` | Computes the visible-tile set once per tick via `VisibilityCalculator`, marks those tiles explored on the dungeon, passes the set to the renderer |

`Player`, `Enemy`, `Projectile`, `DungeonGenerator`, `Room`, `Tile`, input handling, and combat logic (Phase C) are untouched.

## Components

### Position

```java
package ca.ninja.thenix.textrogue.world;

public record Position(int x, int y) {}
```

Replaces the ad-hoc packed-`long` coordinate trick `Game.tileKey(x, y)` currently uses internally for enemy occupancy — that internal helper is untouched (still private to `Game`'s own occupancy tracking), but any *new* cross-file coordinate sharing (visibility) uses `Position` instead of inventing another packed-long convention. A record gives free `equals`/`hashCode`, so `Set<Position>` behaves correctly with no extra code.

### VisibilityCalculator

```java
package ca.ninja.thenix.textrogue.vision;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import java.util.Set;

public interface VisibilityCalculator {
    Set<Position> computeVisibleTiles(Dungeon dungeon, int playerX, int playerY);
}
```

This is the seam for future shadowcasting: a later `ShadowcastVisibilityCalculator` implements the same interface (still returning a `Set<Position>`, just computed by tracing sightlines instead of a radius scan) and `Game` swaps which implementation it constructs — no changes to `Game`'s calling code, `Dungeon`, or the renderer.

### RadiusVisibilityCalculator

```java
package ca.ninja.thenix.textrogue.vision;

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

`Game` constructs this with `VISIBILITY_RADIUS = 8`. Walls do **not** block vision in this implementation — a player can "see" 8 tiles in every direction including through walls into rooms they haven't physically entered. This is the known, accepted trade-off of doing radius-only visibility now instead of shadowcasting; revisiting it is exactly what swapping in a shadowcasting `VisibilityCalculator` later is for.

### Dungeon (additions)

- `private final boolean[][] explored` — same `[HEIGHT][WIDTH]` shape as `tiles`, initialized to all-`false` in the constructor (so every new floor starts fully unexplored, with no extra reset code needed — a new `Dungeon` instance is created per floor already)
- `void markExplored(int x, int y)` — no-ops if out of bounds (matches `setTile`'s existing bounds-check pattern)
- `boolean isExplored(int x, int y)` — returns `false` if out of bounds (matches `getTile`'s existing bounds-check pattern, which returns `WALL` for out-of-bounds rather than throwing)

### Renderer / LanternaRenderer

`Renderer.render(...)` gains one parameter:

```java
void render(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles, Set<Position> visibleTiles);
```

`renderDeath(Player)` and `close()` are unchanged.

Per-tile rendering logic in `LanternaRenderer` becomes three-tiered:

1. **Currently visible** (`visibleTiles.contains(new Position(x, y))`): render exactly as today — the tile's normal full-brightness color (`colorFor(tile)`).
2. **Explored but not currently visible** (`dungeon.isExplored(x, y)` and not in `visibleTiles`): render the tile's character in a single flat dim color (`TextColor.ANSI.BLACK_BRIGHT`) regardless of tile type — walls, floors, and stairs all render identically dimmed. This is a deliberate simplification (no per-type darkened variants) to keep the color palette small; revisit only if it turns out to matter during play.
3. **Unexplored** (neither of the above): render a blank space character with the default background — nothing drawn.

Enemies and projectiles: only rendered if `visibleTiles.contains(new Position(enemy.getX(), enemy.getY()))` (respectively for projectiles). This applies *in addition to* the existing `enemy.isAlive()` check — a dead-but-currently-visible enemy still doesn't render (Phase C behavior), and a living-but-not-currently-visible enemy now also doesn't render (Phase D1 addition).

The player's own glyph always renders regardless of visibility (you can always see yourself).

## Game Loop

`Game` gains one field:

```java
private final VisibilityCalculator visibilityCalculator = new RadiusVisibilityCalculator(VISIBILITY_RADIUS);
static final int VISIBILITY_RADIUS = 8;
```

The compute-and-mark step is its own package-private method, `Set<Position> updateVisibility()`, called immediately before each render (both the initial pre-loop render and the per-tick render inside the loop):

```java
Set<Position> updateVisibility() {
    Set<Position> visible = visibilityCalculator.computeVisibleTiles(dungeon, player.getX(), player.getY());
    for (Position p : visible) dungeon.markExplored(p.x(), p.y());
    return visible;
}
```

Both render call sites become:

```java
renderer.render(dungeon, player, enemies, projectiles, updateVisibility());
```

Exposing this as its own method (rather than inlining it into `start()`) gives tests a direct seam: `game.updateVisibility()` can be called and its return value / `game.dungeon.isExplored(...)` asserted against, without needing a real `Renderer` or running the full loop.

This recomputation happens unconditionally every tick (not just after a successful move) — it's a cheap ~17×17 tile scan (radius 8) and keeping it simple avoids tracking "did the player's position actually change this tick" as a separate concern.

Visibility state (the `explored` grid) lives on `Dungeon`, so it resets automatically on `descend()` (a brand new `Dungeon` is generated) with no additional code in `Game`.

## Testing

Following the existing JUnit 5 conventions (plain `@Test`, no mocking framework):

- **PositionTest**: not strictly necessary (a record's `equals`/`hashCode` are generated and trivial), but one test confirming two `Position`s with the same coordinates are equal (and hash equal) guards against ever changing it to a non-record class that breaks that invariant.
- **RadiusVisibilityCalculatorTest**:
  - A tile exactly at the radius distance (Chebyshev) is included; one tile further is excluded.
  - Tiles outside `Dungeon` bounds are never included even if within radius.
  - The player's own tile is always included.
- **DungeonTest additions**: `isExplored` defaults to `false` for every tile on a fresh `Dungeon`; `markExplored` then `isExplored` returns `true` for that tile only (neighboring tiles remain `false`); `markExplored`/`isExplored` on out-of-bounds coordinates don't throw (mirrors `setTile`/`getTile`'s existing bounds handling).
- **LanternaRendererTest additions**: a tile inside `visibleTiles` renders its normal color; a tile that's `explored` but not in `visibleTiles` renders dimmed; a tile that's neither renders blank; an enemy inside `visibleTiles` renders as `'e'`; an enemy outside `visibleTiles` (but alive) does not render even though it would have under Phase C's rules.
- **GameTest additions**: calling `game.updateVisibility()` directly marks tiles within `VISIBILITY_RADIUS` of the player as explored on `game.dungeon` (and returns them in the resulting `Set<Position>`); a tile one past the radius is neither marked nor returned.

## Out of Scope for Phase D1

- Real shadowcasting/line-of-sight (the `VisibilityCalculator` seam exists specifically so this can be added later without a rewrite)
- Per-tile-type dimmed color variants (single flat dim color for all remembered tile types)
- Items, pickups, inventory (Phase D2)
- Permadeath / restart flow (Phase D3)
- Any change to combat, enemy AI, or projectile logic (Phase C, untouched)
