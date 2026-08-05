# Enemy Pathfinding — Design Spec

## Context

From `TODO.txt`: "Enemies get stuck on walls, no pathfinding." `Game.moveEnemies()`
currently drives each enemy with `chaseStep(enemyX, enemyY, playerX, playerY)`, which
picks a single cardinal direction based purely on which axis has the larger raw
distance to the player. It has no awareness of walls beyond the immediate next tile —
if that tile happens to be a `WALL`, the enemy just skips its turn (`continue`), with no
attempt to route around the obstruction. Enemies near corners or wall segments between
them and the player effectively freeze.

## Goals

- Enemies within `CHASE_RADIUS` navigate around walls to reach the player, using an
  actual shortest path rather than straight-line direction.
- Enemies always appear to move each turn when they have any legal tile to step onto,
  even in a dead end where no neighbor actually gets them closer.
- Keep this cheap: recomputed every turn, independent of enemy count.

## Non-goals

- No diagonal enemy movement (matches existing 4-directional movement).
- No pathfinding for enemies outside `CHASE_RADIUS` — the existing wake-radius gate is
  unchanged.
- No changes to enemy spawning, combat, or player movement.

## Design

### 1. `PathFinder` (new class, `ca.ninja.thenix.textrogue.world`)

A small, pure, independently-testable class:

```java
public class PathFinder {
    public static int[][] distanceField(Dungeon dungeon, int fromX, int fromY) { ... }
}
```

Returns a `Dungeon.HEIGHT x Dungeon.WIDTH` grid of tile-step distances from
`(fromX, fromY)`, computed via BFS over 4-directional neighbors, treating any tile
where `dungeon.getTile(x, y) == Tile.WALL` as impassable. Unreachable tiles (including
out-of-bounds, already handled by `Tile.WALL` via `getTile`) are marked with a sentinel
(`-1`). The search is unbounded — it explores every reachable tile from the source.
No enemy/occupancy awareness here; this is purely about the static map.

### 2. `Game.moveEnemies()` rewrite

Replace the per-enemy `chaseStep`-only decision with:

1. Compute `int[][] distances = PathFinder.distanceField(dungeon, player.getX(), player.getY())` once per call, before the enemy loop.
2. For each living enemy within `CHASE_RADIUS` (Chebyshev distance to player, unchanged
   gate):
   - Build the list of the 4 cardinal neighbor tiles that are not `Tile.WALL` and not
     in the `occupied` set.
   - If that list is empty, the enemy doesn't move this turn (boxed in on all sides).
   - Otherwise, pick the neighbor with the lowest value in `distances`, treating `-1`
     (unreachable) as effectively infinite. Ties — including the case where every
     candidate is `-1`, e.g. the enemy is walled off from the player entirely — are
     broken using the existing `chaseStep` axis preference applied to the candidate
     list itself (never an independent direction outside that list), so the fallback
     can never point at a wall or occupied tile.
   - If the chosen neighbor is the player's tile, deal contact damage instead of
     moving (unchanged existing behavior).
   - Otherwise move there — regardless of whether it's actually closer than the
     enemy's current distance value. This is the "always appear to move" change: the
     old `continue`-on-blocked-step and implicit "no improving neighbor" freeze are
     both replaced by "always take the best available option."

The existing `occupied` set bookkeeping (built before the loop, updated after each
move) is unchanged — it still prevents two enemies from sharing a tile.

`chaseStep` stays as-is (same signature, same tests) — it's now only consulted for tie
breaks and the fully-unreachable fallback, not as the primary decision.

### 3. Chase radius

Unchanged: the existing `Math.max(distX, distY) > CHASE_RADIUS` straight-line check
still gates which enemies act each turn. The distance field itself is computed
unbounded (see Non-goals/Open questions) since the map is only 80x24 = 1,920 tiles —
negligible cost per turn regardless of enemy count.

## Testing

- `PathFinderTest` (new): straight corridor gives increasing distances; an L-bend
  routes around the corner; a dead-end branch gets larger distances than the direct
  path; a pocket separated by walls (no opening) comes back `-1`/unreachable for every
  tile in it; the source tile itself is distance 0.
- `GameTest`: a wall-blocked chase scenario (enemy and player separated by a wall
  segment, connected via a corridor) — enemy now reaches the player over several
  `moveEnemies()` calls instead of freezing.
- `GameTest`: an enemy in a dead end with no strictly-closer neighbor still moves each
  turn (position changes) rather than standing still.
- Existing `chaseStepPrioritizesLargerAxisDistance` / `chaseStepMovesTowardNegativeDirection`
  tests are untouched.

## Open questions

None — all resolved during brainstorming.
