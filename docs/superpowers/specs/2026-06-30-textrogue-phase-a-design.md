# TextRogue — Phase A Design Spec

## Overview

A text-based dungeon-crawl roguelike built in Java 21. Phase A covers movement and exploration: procedurally generated dungeons, ASCII rendering in the terminal, player movement, and descending floors. It is the foundation for future phases that add Lanterna (B), combat (C), and full roguelike features like fog of war and permadeath (D).

## Tooling & Setup

- **Language:** Java 21 (Microsoft OpenJDK 21)
- **Build:** Maven 3.x
- **IDE:** IntelliJ IDEA Community Edition
- **Terminal:** Windows Terminal (supports ANSI escape codes)

**Installation:**
```
winget install JetBrains.IntelliJIDEA.Community
winget install Microsoft.OpenJDK.21
winget install Apache.Maven
```

IntelliJ can also manage the JDK installation directly when creating a new project.

**Run command:**
```
mvn compile exec:java -Dexec.mainClass="ca.ninja.thenix.textrogue.Main"
```

## Architecture

### Package Structure

```
ca.ninja.thenix.textrogue/
├── Main.java
├── Game.java
├── entity/
│   └── Player.java
├── world/
│   ├── Dungeon.java
│   ├── Room.java
│   └── Tile.java
├── gen/
│   └── DungeonGenerator.java
├── render/
│   └── Renderer.java
└── input/
    └── InputHandler.java
```

### Class Responsibilities

| Class | Responsibility |
|-------|---------------|
| `Main` | Entry point — creates a `Game` and calls `start()` |
| `Game` | Owns the game loop and all subsystems; holds references to `Dungeon`, `Player`, `Renderer`, `InputHandler` |
| `Player` | Position (x, y) and floor number; grows to hold HP, stats, inventory in future phases |
| `Dungeon` | 2D array of `Tile` values for the current floor plus the list of `Room` objects; provides bounds checking and tile access |
| `Room` | Rectangle (x, y, width, height) with helpers: `center()`, `intersects(Room)` |
| `Tile` | Enum: `WALL`, `FLOOR`, `STAIRS_DOWN` |
| `DungeonGenerator` | Generates a new `Dungeon` using the branching room algorithm |
| `Renderer` | Clears the terminal and redraws the dungeon + player + status line each turn via ANSI escape codes |
| `InputHandler` | Reads a line from stdin (Scanner) and returns the first character; isolated so it can be swapped for a non-blocking reader in Phase B |

## Dungeon Generation

**Algorithm:**

1. Attempt to place 8–12 rooms at random positions with random sizes (4×4 to 10×8). Reject rooms that overlap existing rooms.
2. Build a neighbor graph: connect each room to its nearest neighbor by Euclidean center-point distance. This forms a spanning tree with natural branching.
3. Add 1–2 extra random corridors between unconnected rooms to create occasional loops.
4. Carve L-shaped corridors between connected rooms (horizontal first then vertical, or vice versa — chosen randomly per corridor).
5. Place the player at the center of the first placed room.
6. Place `STAIRS_DOWN` at the center of the room farthest from the player's starting room.

**Dungeon size:** 80×24 tiles (standard terminal dimensions).

## Game Loop

Turn-based: the loop blocks at input, waiting for the player to act. Nothing moves without player input in Phase A.

```
initialize Game, Dungeon, Player
render initial state
loop:
    input = read line from stdin (blocking)
    process command (W/A/S/D = move, Q = quit)
    if player stepped onto STAIRS_DOWN → generate new Dungeon, increment floor number
    render
```

**Future-proofing:** `InputHandler` is the only class that performs blocking I/O. Swapping it for a Lanterna-backed non-blocking reader in Phase B converts the loop to real-time with no changes to `Game`, `Player`, or `Renderer`.

## Rendering

Clear the screen and redraw every turn using ANSI escape codes (supported in Windows Terminal on Windows 10+).

**ASCII symbols:**

| Symbol | Meaning |
|--------|---------|
| `#` | Wall |
| `.` | Floor |
| `>` | Stairs down |
| `@` | Player |

**Colors (ANSI):**

| Element | Color | Code |
|---------|-------|------|
| `#` Wall | Dark gray | `[90m` |
| `.` Floor | White | `[37m` |
| `>` Stairs | Yellow | `[33m` |
| `@` Player | Bright green | `[92m` |
| Reset | — | `[0m` |

Status line rendered below the map: `Floor: 1`

Fog of war is deferred to Phase D. The full dungeon is visible from the start in Phase A.

## Input

| Key | Action |
|-----|--------|
| `w` | Move up |
| `a` | Move left |
| `s` | Move down |
| `d` | Move right |
| `q` | Quit |

Input is read via `Scanner(System.in)`. The player types a character and presses Enter. Movement is blocked by `WALL` tiles. Single-keypress input (no Enter required) is addressed when Lanterna is added in Phase B.

## Phase Roadmap

| Phase | Features |
|-------|---------|
| A (this spec) | Movement, procedural dungeons, ASCII rendering, floor descent |
| B | Lanterna integration — single-keypress input, real-time loop foundation |
| C | Combat — enemies, HP, attack |
| D | Items, inventory, character stats, fog of war, permadeath |

## Out of Scope for Phase A

- Enemies or combat
- Items or inventory
- Fog of war
- Permadeath
- Save/load
- Sound
