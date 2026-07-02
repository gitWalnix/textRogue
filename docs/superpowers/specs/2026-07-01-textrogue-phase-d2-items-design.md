# TextRogue — Phase D2 (Items) Design Spec

## Overview

Phase D2 is the second of three D-sub-phases (D1 fog of war → **D2 items, this spec** → D3 permadeath). Scope is deliberately the smallest useful slice: a single item type — healing potions — dropped by enemies on death, picked up automatically, and consumed on command. This proves out the item pipeline (drop → ground representation → pickup → inventory → use) without building a general inventory/equipment system; later item types (if ever added) would extend this pipeline rather than replace it.

Two design choices carry over directly from Phase D1's fog-of-war work:

1. **Potions are represented as a `Tile`, not a separate entity list.** Unlike enemies/projectiles (which move and must never render on stale/remembered position — the exact property Phase D1's tests were hardened to prove), a potion on the ground doesn't move once dropped. Modeling it as `Tile.POTION` means it rides the existing three-tier terrain rendering (visible / explored-dimmed / unexplored-blank) for free, with zero new rendering logic and zero risk of the entity-gating bug D1 just guarded against.
2. **Drops are probabilistic, on enemy death, not pre-placed at dungeon generation.** This is a small roguelike-feel improvement over the original "items scattered at generation" idea, and it composes naturally with existing combat code (`Game.advanceProjectiles()` already has the killing blow's `(x, y)` in hand).

`q` stays bound to `QUIT` exactly as it is today — there's no actual conflict with `e` (the new drink-potion key), so there's no reason to remove a working quit binding. (An earlier draft of this design proposed dropping `q`; that was reversed after checking with the user.)

## What Changes

| File | Change |
|------|--------|
| `world/Tile.java` | New enum constant `POTION` (glyph `!`) |
| `entity/Player.java` | New `potionCount` field; `addPotion()`, `drinkPotion()`, `heal(int amount)`, `getPotionCount()` |
| `input/Command.java` | New `DRINK_POTION` constant |
| `input/LanternaInputHandler.java` | Bind `e` → `DRINK_POTION`; existing `q` → `QUIT` binding unchanged |
| `Game.java` | New constants `POTION_DROP_CHANCE`, `POTION_HEAL_AMOUNT`; drop-on-kill logic in `advanceProjectiles()`; pickup logic in `tryMove()`; `DRINK_POTION` case in `processCommand()` |
| `render/LanternaRenderer.java` | `colorFor(Tile)` gains a `POTION` case; status line gains `Potions: N` |

`Dungeon`, `DungeonGenerator`, `Room`, `Enemy`, `Projectile`, `Position`, vision code, and the fog-of-war rendering tiers (Phase D1) are untouched — potions ride the existing terrain path unmodified.

## Components

### Tile

```java
public enum Tile {
    WALL, FLOOR, STAIRS_DOWN, POTION;

    public char toChar() {
        return switch (this) {
            case WALL        -> '#';
            case FLOOR       -> '.';
            case STAIRS_DOWN -> '>';
            case POTION      -> '!';
        };
    }
}
```

`LanternaRenderer.colorFor(Tile)` gets a matching `case POTION -> TextColor.ANSI.MAGENTA;`. No other rendering change is needed — `POTION` tiles flow through the same visible/explored/unexplored branching every other tile already uses.

### Player (additions)

```java
private int potionCount = 0;

public int getPotionCount() { return potionCount; }

public void addPotion() {
    potionCount++;
}

public void heal(int amount) {
    hp = Math.min(MAX_HP, hp + amount);
}

public boolean drinkPotion(int healAmount) {
    if (potionCount <= 0) return false;
    potionCount--;
    heal(healAmount);
    return true;
}
```

`drinkPotion` takes the heal amount as a parameter (rather than hardcoding `Game`'s constant inside `Player`) to keep balance numbers centralized in `Game`, consistent with how `PROJECTILE_DAMAGE`, `ENEMY_CONTACT_DAMAGE`, etc. already live there rather than in `Player`/`Enemy`.

### Command / LanternaInputHandler

```java
public enum Command {
    MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, SHOOT, DRINK_POTION, QUIT, UNKNOWN
}
```

In `toCommand(KeyStroke)`'s `Character` branch: add `case 'e' -> Command.DRINK_POTION;`. The existing `case 'q' -> Command.QUIT;` and the `EOF, Escape -> Command.QUIT` branch are both unchanged — `q` and `Escape` continue to both quit, exactly as today.

### Game — drop on kill

In `advanceProjectiles()`, the existing hit-handling branch:

```java
Enemy hit = enemyAt(p.getX(), p.getY());
if (hit != null) {
    hit.takeDamage(PROJECTILE_DAMAGE);
    if (!hit.isAlive()) maybeDropPotion(hit.getX(), hit.getY());
    it.remove();
    continue;
}
```

```java
void maybeDropPotion(int x, int y) {
    if (dungeon.getTile(x, y) != Tile.FLOOR) return;
    if (random.nextDouble() < POTION_DROP_CHANCE) {
        dungeon.setTile(x, y, Tile.POTION);
    }
}
```

Reuses the `Random random` field `Game` already has for `spawnEnemies()`. The `Tile.FLOOR` guard exists specifically to avoid overwriting `STAIRS_DOWN` in the edge case where an enemy dies while standing on it — the drop is simply skipped rather than attempting to preserve/restore the stairs tile underneath, keeping the logic single-branch and side-effect-free elsewhere.

`maybeDropPotion` is package-private (no modifier), not `private` — it needs to be, matching the existing `spawnEnemies()`/`updateVisibility()` test-seam pattern, so `GameTest` can call it directly many times for the statistical drop-rate test below without driving a full projectile-kill cycle 1000 times.

### Game — pickup

In `tryMove(int nx, int ny)`:

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

Mirrors the existing `STAIRS_DOWN`-triggers-`descend()` pattern: pickup is automatic on walking onto the tile, no separate confirmation step. The tile reverts to `FLOOR` immediately so it renders correctly (and can't be picked up twice).

### Game — drink command

New constants alongside the existing balance constants:

```java
static final double POTION_DROP_CHANCE = 0.33;
static final int POTION_HEAL_AMOUNT = 10;
```

In `processCommand(Command cmd)`, add a case:

```java
case DRINK_POTION -> player.drinkPotion(POTION_HEAL_AMOUNT);
```

No feedback message is shown on a no-op drink (empty inventory) — the status line's `Potions: 0` is the player's existing signal that nothing will happen. This keeps the change additive and avoids introducing a message/toast system for a one-phase feature.

### Renderer — status line

```java
g.putString(0, Dungeon.HEIGHT, "Floor: " + player.getFloor() + "   HP: " + player.getHp() + "/" + player.getMaxHp() + "   Potions: " + player.getPotionCount());
```

## Testing

Following existing JUnit 5 conventions (plain `@Test`, no mocking framework):

- **PlayerTest additions**: `addPotion()` increments `getPotionCount()`; `heal()` caps at `MAX_HP` (does not overheal); `drinkPotion()` returns `true` and decrements count + heals when count > 0; `drinkPotion()` returns `false` and is a no-op (no HP change) when count is 0.
- **TileTest additions**: `Tile.POTION.toChar()` returns `'!'`.
- **LanternaInputHandlerTest additions**: `'e'` maps to `Command.DRINK_POTION`. The existing `qKeyReturnsQuit` and `Escape`-maps-to-`QUIT` tests are untouched.
- **LanternaRendererTest additions**: a `POTION` tile in `visibleTiles` renders `'!'`; status line contains `"Potions: N"` for a player with a nonzero potion count.
- **GameTest additions**:
  - Pickup: place a `Tile.POTION` adjacent to the player, issue the matching move command, assert `player.getPotionCount()` increased by 1 and the tile is now `Tile.FLOOR`.
  - Drink: give the player a potion via `addPotion()`, damage them, issue `DRINK_POTION`, assert HP increased by `POTION_HEAL_AMOUNT` (capped at max) and potion count decreased.
  - Non-`FLOOR` guard is deterministic, not statistical: `maybeDropPotion`'s `Tile.FLOOR` check runs *before* the random roll, so calling it once on a `STAIRS_DOWN` (or `WALL`) tile is guaranteed to never place a potion regardless of RNG — a single-call assertion suffices, no trial loop or seeding needed.
  - Drop-on-`FLOOR` chance is statistical: call `maybeDropPotion` many times (e.g. 1000) on fresh `FLOOR` tiles with the project's existing unseeded `Random`, and assert the resulting `POTION`-tile count falls within a generous tolerance band around the ~330 expected at `POTION_DROP_CHANCE = 0.33` (e.g. 250–410). This matches the non-seeded, range-based testing style already used for `spawnEnemies()`'s "3 to 5 enemies" assertion, rather than introducing a new seeded-`Random` test seam for this one case.

## Out of Scope for Phase D2

- Multiple item types, equipment, weapons, scrolls (potions only, this phase)
- An inventory UI/list beyond the single `Potions: N` status-line counter
- Any message/toast feedback for pickup or drink actions
- Removing dead enemies from `Game.enemies` (list-hygiene item noted in Phase C's final review; unrelated to implementing drops since the killing blow already has the death position in hand — left as-is)
- Permadeath / restart flow (Phase D3)
- Any change to fog-of-war tiers, visibility computation, or entity (enemy/projectile) rendering gating (Phase D1, untouched)
