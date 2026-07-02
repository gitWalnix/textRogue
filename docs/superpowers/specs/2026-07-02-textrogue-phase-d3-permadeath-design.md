# Phase D3: Permadeath — Design Spec

## Context

Phases A–D2 are complete. Dying already stops the run: `Player.isDead()` triggers a
"You died" message that displays for a fixed `DEATH_SCREEN_DISPLAY_MS` (3000ms), after
which `Game.start()` returns and the process exits. There is no save/reload, so death
was already permanent in effect — but the experience is bare (no run summary) and the
only way to play again is relaunching the process.

Phase D3 makes death a proper, informative checkpoint: show a summary of the run, then
let the player either start a brand-new run in the same session or quit.

## Goals

- Death screen shows: floor reached, enemies killed, potions used.
- Player can press `R` to start a fresh run (new dungeon, new `Player`, stats reset) or
  `Esc` to quit, from the death screen.
- The death screen waits indefinitely for one of those two inputs — no fixed timer.
- Progress is never saved/resumed — restarting always begins a brand-new run at floor 1.

## Non-goals

- No persistent high-score list or run history across restarts.
- No confirmation prompt before restart/quit — a single keypress acts immediately.
- No changes to how the player dies (contact/projectile damage rules are unchanged).

## Design

### 1. Stat tracking (`Player`)

Two new fields alongside the existing `floor`, both starting at 0 in the constructor:

- `killCount` — incremented by a new `recordKill()` method.
- `potionsUsed` — incremented inside `drinkPotion(int healAmount)`, only on a
  successful drink (i.e., inside the existing `potionCount > 0` branch, after
  `potionCount--`).

New getters: `getKillCount()`, `getPotionsUsed()`. `getFloor()` already exists.

Because `Player` is fully replaced by `newRun()` (see below) rather than mutated in
place, these stats naturally reset on restart with no extra bookkeeping.

`Game.advanceProjectiles()` calls `player.recordKill()` at the point where it already
detects a kill (`if (!hit.isAlive())`, alongside the existing `maybeDropPotion` call) —
this is the only place enemies currently die, since contact damage only hurts the
player, not enemies.

### 2. Restart flow (`Game`)

Extract the dungeon/player/enemy setup that currently lives inline in `start()` (under
`if (dungeon == null)`) into a private `newRun()` method:

```java
private void newRun() {
    dungeon = new DungeonGenerator(System.currentTimeMillis()).generate();
    player = new Player(dungeon.getStartX(), dungeon.getStartY());
    spawnEnemies();
    shootCooldown = 0;
}
```

`start()` still only calls `newRun()` when `dungeon == null`, so tests that inject
state via `gameWithInjectedState` (which set `game.dungeon`/`game.player` directly
before calling other methods) are unaffected.

On death, replace the fixed-sleep block with:

```java
if (player.isDead()) {
    renderer.renderDeath(player);
    Command choice = awaitDeathChoice();
    if (choice == Command.RESTART) {
        newRun();
        renderer.render(dungeon, player, enemies, projectiles, updateVisibility());
        continue; // back into the main while(running) loop
    }
    running = false;
    break;
}
```

`awaitDeathChoice()` polls `input.pollCommand()` on the same `TICK_MS` cadence as the
main loop (sleeping between polls so it doesn't busy-spin), ignoring `UNKNOWN` and any
gameplay commands, until it sees `RESTART` or `QUIT`.

`DEATH_SCREEN_DISPLAY_MS` is removed — no fixed display time remains.

### 3. Input

Add `Command.RESTART` to the `Command` enum. Bind it to `r`/`R` in
`LanternaInputHandler.toCommand()`. `Escape`/`EOF` already map to `Command.QUIT`, so no
new quit binding is needed. `R` and `Esc` are otherwise unused during active gameplay,
so no conflicts.

### 4. Death screen rendering

`Renderer.renderDeath(Player)` keeps its existing signature — floor, kills, and
potions are all readable from `Player`, so no new parameters are needed.
`LanternaRenderer.renderDeath` adds the stat lines and restart/quit prompt below the
existing "You died" message:

```
You died

Floor reached: 4
Enemies killed: 12
Potions used: 2

Press [R] to restart, [Esc] to quit
```

## Testing

- `Player`: `recordKill()` increments `killCount`; `drinkPotion()` increments
  `potionsUsed` only on success (not when `potionCount == 0`); a fresh `Player` starts
  both at 0.
- `Game`: a kill via projectile damage increments `player.getKillCount()`.
- `Game`: existing `playerDeathStopsLoopAndRendersDeathScreen` is rewritten — the
  always-`UNKNOWN` input stub would now hang forever since there's no fixed timer.
  Replace with a stub returning `UNKNOWN` a few times then `QUIT`, asserting
  `deathRendered` is true and `isRunning()` is false, with no minimum-elapsed-time
  assertion.
- `Game`: new test — a stub returning `UNKNOWN` then `RESTART` after death causes the
  loop to continue running (`isRunning()` true, or observable via a subsequent `QUIT`
  ending it), with a new `Dungeon` instance and a `Player` with `killCount`/
  `potionsUsed`/`floor` reset to their initial values.
- `LanternaRenderer`: `renderDeathShowsMessage` (existing) extended or a new test
  asserts the stat lines and restart/quit prompt are drawn.
- `LanternaInputHandler`: `r` maps to `Command.RESTART`.

## Open questions

None — all resolved during brainstorming.
