# TextRogue Phase A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a playable text-based dungeon crawler with procedurally generated floors, ASCII terminal rendering, WASD movement, and stair descent.

**Architecture:** A turn-based game loop in `Game` integrates `DungeonGenerator`, `Player`, `Renderer`, and `InputHandler`. Each class has one responsibility and communicates through simple method calls. `InputHandler` is intentionally isolated so it can be swapped for a non-blocking reader in a future phase.

**Tech Stack:** Java 21, Maven 3.x, JUnit Jupiter 5.10.2, ANSI escape codes for terminal rendering.

## Global Constraints

- Java source and target: 21
- Group ID: `ca.ninja.thenix`, artifact: `TextRogue`
- All source files under `src/main/java/ca/ninja/thenix/textrogue/`
- All test files under `src/test/java/ca/ninja/thenix/textrogue/`
- Dungeon dimensions: WIDTH=80, HEIGHT=24 (constants on `Dungeon`)
- Run tests: `mvn test`
- Run game: `mvn compile exec:java`
- No external dependencies beyond JUnit 5 for testing

---

### Task 1: Project Setup

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/ca/ninja/thenix/textrogue/Main.java`

**Interfaces:**
- Produces: compilable Maven project with JUnit 5 and exec plugin; `Main.main()` entry point

- [ ] **Step 1: Update pom.xml**

Replace the entire contents of `pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>ca.ninja.thenix</groupId>
    <artifactId>TextRogue</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>ca.ninja.thenix.textrogue.Main</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create source directory structure**

Run:
```
New-Item -ItemType Directory -Force "src/main/java/ca/ninja/thenix/textrogue/entity"
New-Item -ItemType Directory -Force "src/main/java/ca/ninja/thenix/textrogue/world"
New-Item -ItemType Directory -Force "src/main/java/ca/ninja/thenix/textrogue/gen"
New-Item -ItemType Directory -Force "src/main/java/ca/ninja/thenix/textrogue/render"
New-Item -ItemType Directory -Force "src/main/java/ca/ninja/thenix/textrogue/input"
New-Item -ItemType Directory -Force "src/test/java/ca/ninja/thenix/textrogue/world"
New-Item -ItemType Directory -Force "src/test/java/ca/ninja/thenix/textrogue/entity"
New-Item -ItemType Directory -Force "src/test/java/ca/ninja/thenix/textrogue/gen"
New-Item -ItemType Directory -Force "src/test/java/ca/ninja/thenix/textrogue/render"
New-Item -ItemType Directory -Force "src/test/java/ca/ninja/thenix/textrogue/input"
```

- [ ] **Step 3: Create Main.java**

Create `src/main/java/ca/ninja/thenix/textrogue/Main.java`:

```java
package ca.ninja.thenix.textrogue;

public class Main {
    public static void main(String[] args) {
        System.out.println("TextRogue starting...");
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```
git add pom.xml src/
git commit -m "feat: project setup with Maven, JUnit 5, and Main entry point"
```

---

### Task 2: Tile Enum

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/world/Tile.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/world/TileTest.java`

**Interfaces:**
- Produces: `Tile` enum with values `WALL`, `FLOOR`, `STAIRS_DOWN` and method `char toChar()`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/ca/ninja/thenix/textrogue/world/TileTest.java`:

```java
package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileTest {
    @Test
    void wallRendersAsHash() {
        assertEquals('#', Tile.WALL.toChar());
    }

    @Test
    void floorRendersAsDot() {
        assertEquals('.', Tile.FLOOR.toChar());
    }

    @Test
    void stairsRendersAsGreaterThan() {
        assertEquals('>', Tile.STAIRS_DOWN.toChar());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl . -Dtest=TileTest`
Expected: FAIL — `cannot find symbol: Tile`

- [ ] **Step 3: Create Tile.java**

Create `src/main/java/ca/ninja/thenix/textrogue/world/Tile.java`:

```java
package ca.ninja.thenix.textrogue.world;

public enum Tile {
    WALL, FLOOR, STAIRS_DOWN;

    public char toChar() {
        return switch (this) {
            case WALL -> '#';
            case FLOOR -> '.';
            case STAIRS_DOWN -> '>';
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=TileTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```
git add src/main/java/ca/ninja/thenix/textrogue/world/Tile.java src/test/java/ca/ninja/thenix/textrogue/world/TileTest.java
git commit -m "feat: add Tile enum with ASCII character mapping"
```

---

### Task 3: Room Record

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/world/Room.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/world/RoomTest.java`

**Interfaces:**
- Produces: `Room` record with fields `int x, y, width, height`; methods `int centerX()`, `int centerY()`, `boolean intersects(Room other)`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/ca/ninja/thenix/textrogue/world/RoomTest.java`:

```java
package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomTest {
    @Test
    void centerXIsMiddleOfWidth() {
        Room room = new Room(0, 0, 10, 8);
        assertEquals(5, room.centerX());
    }

    @Test
    void centerYIsMiddleOfHeight() {
        Room room = new Room(0, 0, 10, 8);
        assertEquals(4, room.centerY());
    }

    @Test
    void overlappingRoomsIntersect() {
        Room a = new Room(0, 0, 5, 5);
        Room b = new Room(3, 3, 5, 5);
        assertTrue(a.intersects(b));
    }

    @Test
    void separateRoomsDoNotIntersect() {
        Room a = new Room(0, 0, 3, 3);
        Room b = new Room(5, 5, 3, 3);
        assertFalse(a.intersects(b));
    }

    @Test
    void adjacentRoomsDoNotIntersect() {
        Room a = new Room(0, 0, 3, 3);
        Room b = new Room(3, 0, 3, 3);
        assertFalse(a.intersects(b));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RoomTest`
Expected: FAIL — `cannot find symbol: Room`

- [ ] **Step 3: Create Room.java**

Create `src/main/java/ca/ninja/thenix/textrogue/world/Room.java`:

```java
package ca.ninja.thenix.textrogue.world;

public record Room(int x, int y, int width, int height) {
    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public boolean intersects(Room other) {
        return x < other.x + other.width &&
               x + width > other.x &&
               y < other.y + other.height &&
               y + height > other.y;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RoomTest`
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```
git add src/main/java/ca/ninja/thenix/textrogue/world/Room.java src/test/java/ca/ninja/thenix/textrogue/world/RoomTest.java
git commit -m "feat: add Room record with center and intersection logic"
```

---

### Task 4: Dungeon Class

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/world/Dungeon.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/world/DungeonTest.java`

**Interfaces:**
- Consumes: `Tile` (WALL, FLOOR, STAIRS_DOWN), `Room`
- Produces: `Dungeon(Tile[][] tiles, List<Room> rooms, int startX, int startY)`; methods `Tile getTile(int x, int y)`, `void setTile(int x, int y, Tile tile)`, `boolean isInBounds(int x, int y)`, `List<Room> getRooms()`, `int getStartX()`, `int getStartY()`; constants `int WIDTH = 80`, `int HEIGHT = 24`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/ca/ninja/thenix/textrogue/world/DungeonTest.java`:

```java
package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DungeonTest {
    private Tile[][] allWalls() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        return tiles;
    }

    @Test
    void getTileReturnsSetTile() {
        Tile[][] tiles = allWalls();
        Dungeon dungeon = new Dungeon(tiles, List.of(), 0, 0);
        dungeon.setTile(5, 5, Tile.FLOOR);
        assertEquals(Tile.FLOOR, dungeon.getTile(5, 5));
    }

    @Test
    void outOfBoundsNegativeXReturnsWall() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertEquals(Tile.WALL, dungeon.getTile(-1, 0));
    }

    @Test
    void outOfBoundsNegativeYReturnsWall() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertEquals(Tile.WALL, dungeon.getTile(0, -1));
    }

    @Test
    void outOfBoundsWidthReturnsWall() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertEquals(Tile.WALL, dungeon.getTile(Dungeon.WIDTH, 0));
    }

    @Test
    void outOfBoundsHeightReturnsWall() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 0, 0);
        assertEquals(Tile.WALL, dungeon.getTile(0, Dungeon.HEIGHT));
    }

    @Test
    void startPositionIsReturned() {
        Dungeon dungeon = new Dungeon(allWalls(), List.of(), 10, 7);
        assertEquals(10, dungeon.getStartX());
        assertEquals(7, dungeon.getStartY());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=DungeonTest`
Expected: FAIL — `cannot find symbol: Dungeon`

- [ ] **Step 3: Create Dungeon.java**

Create `src/main/java/ca/ninja/thenix/textrogue/world/Dungeon.java`:

```java
package ca.ninja.thenix.textrogue.world;

import java.util.Collections;
import java.util.List;

public class Dungeon {
    public static final int WIDTH = 80;
    public static final int HEIGHT = 24;

    private final Tile[][] tiles;
    private final List<Room> rooms;
    private final int startX;
    private final int startY;

    public Dungeon(Tile[][] tiles, List<Room> rooms, int startX, int startY) {
        this.tiles = tiles;
        this.rooms = rooms;
        this.startX = startX;
        this.startY = startY;
    }

    public Tile getTile(int x, int y) {
        if (!isInBounds(x, y)) return Tile.WALL;
        return tiles[y][x];
    }

    public void setTile(int x, int y, Tile tile) {
        if (isInBounds(x, y)) tiles[y][x] = tile;
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=DungeonTest`
Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```
git add src/main/java/ca/ninja/thenix/textrogue/world/Dungeon.java src/test/java/ca/ninja/thenix/textrogue/world/DungeonTest.java
git commit -m "feat: add Dungeon class with tile grid and bounds checking"
```

---

### Task 5: Player Class

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/entity/Player.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java`

**Interfaces:**
- Produces: `Player(int x, int y)`; methods `int getX()`, `int getY()`, `int getFloor()`, `void moveTo(int x, int y)`, `void descendFloor()`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java`:

```java
package ca.ninja.thenix.textrogue.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {
    @Test
    void startsAtGivenPosition() {
        Player player = new Player(5, 3);
        assertEquals(5, player.getX());
        assertEquals(3, player.getY());
    }

    @Test
    void startsOnFloorOne() {
        Player player = new Player(0, 0);
        assertEquals(1, player.getFloor());
    }

    @Test
    void moveToUpdatesPosition() {
        Player player = new Player(0, 0);
        player.moveTo(3, 4);
        assertEquals(3, player.getX());
        assertEquals(4, player.getY());
    }

    @Test
    void descendFloorIncrementsFloor() {
        Player player = new Player(0, 0);
        player.descendFloor();
        assertEquals(2, player.getFloor());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=PlayerTest`
Expected: FAIL — `cannot find symbol: Player`

- [ ] **Step 3: Create Player.java**

Create `src/main/java/ca/ninja/thenix/textrogue/entity/Player.java`:

```java
package ca.ninja.thenix.textrogue.entity;

public class Player {
    private int x;
    private int y;
    private int floor;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.floor = 1;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getFloor() { return floor; }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void descendFloor() {
        floor++;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=PlayerTest`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```
git add src/main/java/ca/ninja/thenix/textrogue/entity/Player.java src/test/java/ca/ninja/thenix/textrogue/entity/PlayerTest.java
git commit -m "feat: add Player class with position and floor tracking"
```

---

### Task 6: Command Enum + InputHandler

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/input/Command.java`
- Create: `src/main/java/ca/ninja/thenix/textrogue/input/InputHandler.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/input/InputHandlerTest.java`

**Interfaces:**
- Produces: `Command` enum with values `MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, QUIT, UNKNOWN`
- Produces: `InputHandler(InputStream in)`; method `Command readCommand()`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/ca/ninja/thenix/textrogue/input/InputHandlerTest.java`:

```java
package ca.ninja.thenix.textrogue.input;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class InputHandlerTest {
    private InputHandler handlerFor(String input) {
        return new InputHandler(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    void wReturnsMoveUp() {
        assertEquals(Command.MOVE_UP, handlerFor("w\n").readCommand());
    }

    @Test
    void uppercaseWReturnsMoveUp() {
        assertEquals(Command.MOVE_UP, handlerFor("W\n").readCommand());
    }

    @Test
    void sReturnsMoveDown() {
        assertEquals(Command.MOVE_DOWN, handlerFor("s\n").readCommand());
    }

    @Test
    void aReturnsMoveLeft() {
        assertEquals(Command.MOVE_LEFT, handlerFor("a\n").readCommand());
    }

    @Test
    void dReturnsMoveRight() {
        assertEquals(Command.MOVE_RIGHT, handlerFor("d\n").readCommand());
    }

    @Test
    void qReturnsQuit() {
        assertEquals(Command.QUIT, handlerFor("q\n").readCommand());
    }

    @Test
    void unknownCharReturnsUnknown() {
        assertEquals(Command.UNKNOWN, handlerFor("x\n").readCommand());
    }

    @Test
    void emptyLineReturnsUnknown() {
        assertEquals(Command.UNKNOWN, handlerFor("\n").readCommand());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=InputHandlerTest`
Expected: FAIL — `cannot find symbol: Command`

- [ ] **Step 3: Create Command.java**

Create `src/main/java/ca/ninja/thenix/textrogue/input/Command.java`:

```java
package ca.ninja.thenix.textrogue.input;

public enum Command {
    MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, QUIT, UNKNOWN
}
```

- [ ] **Step 4: Create InputHandler.java**

Create `src/main/java/ca/ninja/thenix/textrogue/input/InputHandler.java`:

```java
package ca.ninja.thenix.textrogue.input;

import java.io.InputStream;
import java.util.Scanner;

public class InputHandler {
    private final Scanner scanner;

    public InputHandler(InputStream in) {
        this.scanner = new Scanner(in);
    }

    public Command readCommand() {
        if (!scanner.hasNextLine()) return Command.QUIT;
        String line = scanner.nextLine().trim().toLowerCase();
        if (line.isEmpty()) return Command.UNKNOWN;
        return switch (line.charAt(0)) {
            case 'w' -> Command.MOVE_UP;
            case 's' -> Command.MOVE_DOWN;
            case 'a' -> Command.MOVE_LEFT;
            case 'd' -> Command.MOVE_RIGHT;
            case 'q' -> Command.QUIT;
            default -> Command.UNKNOWN;
        };
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=InputHandlerTest`
Expected: `Tests run: 8, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```
git add src/main/java/ca/ninja/thenix/textrogue/input/ src/test/java/ca/ninja/thenix/textrogue/input/
git commit -m "feat: add Command enum and InputHandler with Scanner-based input"
```

---

### Task 7: DungeonGenerator

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/gen/DungeonGenerator.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/gen/DungeonGeneratorTest.java`

**Interfaces:**
- Consumes: `Tile`, `Room`, `Dungeon`
- Produces: `DungeonGenerator(long seed)`; method `Dungeon generate()`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/ca/ninja/thenix/textrogue/gen/DungeonGeneratorTest.java`:

```java
package ca.ninja.thenix.textrogue.gen;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DungeonGeneratorTest {
    private boolean dungeonContains(Dungeon dungeon, Tile target) {
        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                if (dungeon.getTile(x, y) == target) return true;
            }
        }
        return false;
    }

    @Test
    void generatedDungeonHasFloorTiles() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertTrue(dungeonContains(dungeon, Tile.FLOOR));
    }

    @Test
    void generatedDungeonHasStairsDown() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertTrue(dungeonContains(dungeon, Tile.STAIRS_DOWN));
    }

    @Test
    void startPositionIsOnFloor() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertEquals(Tile.FLOOR, dungeon.getTile(dungeon.getStartX(), dungeon.getStartY()));
    }

    @Test
    void generatedDungeonHasAtLeastEightRooms() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertTrue(dungeon.getRooms().size() >= 8);
    }

    @Test
    void startPositionIsWithinBounds() {
        Dungeon dungeon = new DungeonGenerator(42L).generate();
        assertTrue(dungeon.isInBounds(dungeon.getStartX(), dungeon.getStartY()));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=DungeonGeneratorTest`
Expected: FAIL — `cannot find symbol: DungeonGenerator`

- [ ] **Step 3: Create DungeonGenerator.java**

Create `src/main/java/ca/ninja/thenix/textrogue/gen/DungeonGenerator.java`:

```java
package ca.ninja.thenix.textrogue.gen;

import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Room;
import ca.ninja.thenix.textrogue.world.Tile;

import java.util.*;

public class DungeonGenerator {
    private static final int MIN_ROOMS = 8;
    private static final int MAX_ROOMS = 12;
    private static final int MIN_ROOM_WIDTH = 4;
    private static final int MAX_ROOM_WIDTH = 10;
    private static final int MIN_ROOM_HEIGHT = 4;
    private static final int MAX_ROOM_HEIGHT = 8;
    private static final int MAX_ATTEMPTS = 150;
    private static final int EXTRA_CORRIDORS = 2;

    private final Random random;

    public DungeonGenerator(long seed) {
        this.random = new Random(seed);
    }

    public Dungeon generate() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);

        List<Room> rooms = placeRooms(tiles);
        connectRooms(tiles, rooms);

        int startX = rooms.get(0).centerX();
        int startY = rooms.get(0).centerY();

        Room stairsRoom = farthestRoom(rooms, startX, startY);
        tiles[stairsRoom.centerY()][stairsRoom.centerX()] = Tile.STAIRS_DOWN;

        return new Dungeon(tiles, rooms, startX, startY);
    }

    private List<Room> placeRooms(Tile[][] tiles) {
        List<Room> rooms = new ArrayList<>();
        int target = MIN_ROOMS + random.nextInt(MAX_ROOMS - MIN_ROOMS + 1);
        int attempts = 0;

        while (rooms.size() < target && attempts < MAX_ATTEMPTS) {
            attempts++;
            int w = MIN_ROOM_WIDTH + random.nextInt(MAX_ROOM_WIDTH - MIN_ROOM_WIDTH + 1);
            int h = MIN_ROOM_HEIGHT + random.nextInt(MAX_ROOM_HEIGHT - MIN_ROOM_HEIGHT + 1);
            int x = 1 + random.nextInt(Dungeon.WIDTH - w - 2);
            int y = 1 + random.nextInt(Dungeon.HEIGHT - h - 2);

            Room candidate = new Room(x, y, w, h);
            if (rooms.stream().noneMatch(r -> r.intersects(candidate))) {
                rooms.add(candidate);
                carveRoom(tiles, candidate);
            }
        }
        return rooms;
    }

    private void carveRoom(Tile[][] tiles, Room room) {
        for (int y = room.y(); y < room.y() + room.height(); y++) {
            for (int x = room.x(); x < room.x() + room.width(); x++) {
                tiles[y][x] = Tile.FLOOR;
            }
        }
    }

    private void connectRooms(Tile[][] tiles, List<Room> rooms) {
        Set<Integer> connected = new HashSet<>();
        connected.add(0);

        while (connected.size() < rooms.size()) {
            double minDist = Double.MAX_VALUE;
            int from = -1, to = -1;

            for (int i : connected) {
                for (int j = 0; j < rooms.size(); j++) {
                    if (connected.contains(j)) continue;
                    double d = euclidean(rooms.get(i), rooms.get(j));
                    if (d < minDist) { minDist = d; from = i; to = j; }
                }
            }

            if (from == -1) break;
            carveCorridor(tiles, rooms.get(from), rooms.get(to));
            connected.add(to);
        }

        for (int i = 0; i < EXTRA_CORRIDORS; i++) {
            int a = random.nextInt(rooms.size());
            int b = random.nextInt(rooms.size());
            if (a != b) carveCorridor(tiles, rooms.get(a), rooms.get(b));
        }
    }

    private void carveCorridor(Tile[][] tiles, Room a, Room b) {
        int ax = a.centerX(), ay = a.centerY();
        int bx = b.centerX(), by = b.centerY();

        if (random.nextBoolean()) {
            carveHorizontal(tiles, ay, ax, bx);
            carveVertical(tiles, bx, ay, by);
        } else {
            carveVertical(tiles, ax, ay, by);
            carveHorizontal(tiles, by, ax, bx);
        }
    }

    private void carveHorizontal(Tile[][] tiles, int y, int x1, int x2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            if (y >= 0 && y < Dungeon.HEIGHT && x >= 0 && x < Dungeon.WIDTH)
                tiles[y][x] = Tile.FLOOR;
        }
    }

    private void carveVertical(Tile[][] tiles, int x, int y1, int y2) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            if (y >= 0 && y < Dungeon.HEIGHT && x >= 0 && x < Dungeon.WIDTH)
                tiles[y][x] = Tile.FLOOR;
        }
    }

    private double euclidean(Room a, Room b) {
        int dx = a.centerX() - b.centerX();
        int dy = a.centerY() - b.centerY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private Room farthestRoom(List<Room> rooms, int fromX, int fromY) {
        Room farthest = rooms.get(0);
        double maxDist = 0;
        for (Room r : rooms) {
            int dx = r.centerX() - fromX;
            int dy = r.centerY() - fromY;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d > maxDist) { maxDist = d; farthest = r; }
        }
        return farthest;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=DungeonGeneratorTest`
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```
git add src/main/java/ca/ninja/thenix/textrogue/gen/ src/test/java/ca/ninja/thenix/textrogue/gen/
git commit -m "feat: add DungeonGenerator with nearest-neighbor room connection and loop corridors"
```

---

### Task 8: Renderer

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/render/RendererTest.java`

**Interfaces:**
- Consumes: `Dungeon`, `Player`
- Produces: `Renderer(PrintStream out)`; method `void render(Dungeon dungeon, Player player)`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/ca/ninja/thenix/textrogue/render/RendererTest.java`:

```java
package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RendererTest {
    private Dungeon emptyDungeon() {
        Tile[][] tiles = new Tile[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (Tile[] row : tiles) Arrays.fill(row, Tile.WALL);
        return new Dungeon(tiles, List.of(), 0, 0);
    }

    private String renderToString(Dungeon dungeon, Player player) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new Renderer(new PrintStream(out)).render(dungeon, player);
        return out.toString();
    }

    @Test
    void outputContainsPlayerSymbol() {
        String output = renderToString(emptyDungeon(), new Player(0, 0));
        assertTrue(output.contains("@"));
    }

    @Test
    void outputContainsFloorNumberOne() {
        String output = renderToString(emptyDungeon(), new Player(0, 0));
        assertTrue(output.contains("Floor: 1"));
    }

    @Test
    void outputContainsUpdatedFloorNumber() {
        Player player = new Player(0, 0);
        player.descendFloor();
        player.descendFloor();
        String output = renderToString(emptyDungeon(), player);
        assertTrue(output.contains("Floor: 3"));
    }

    @Test
    void outputContainsWallCharacter() {
        String output = renderToString(emptyDungeon(), new Player(0, 0));
        assertTrue(output.contains("#"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RendererTest`
Expected: FAIL — `cannot find symbol: Renderer`

- [ ] **Step 3: Create Renderer.java**

Create `src/main/java/ca/ninja/thenix/textrogue/render/Renderer.java`:

```java
package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;

import java.io.PrintStream;

public class Renderer {
    private static final String CLEAR = "\u001B[2J\u001B[H";
    private static final String RESET = "\u001B[0m";
    private static final String DARK_GRAY = "\u001B[90m";
    private static final String WHITE = "\u001B[37m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BRIGHT_GREEN = "\u001B[92m";

    private final PrintStream out;

    public Renderer(PrintStream out) {
        this.out = out;
    }

    public void render(Dungeon dungeon, Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(CLEAR);

        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                if (x == player.getX() && y == player.getY()) {
                    sb.append(BRIGHT_GREEN).append('@').append(RESET);
                } else {
                    Tile tile = dungeon.getTile(x, y);
                    sb.append(colorFor(tile)).append(tile.toChar()).append(RESET);
                }
            }
            sb.append('\n');
        }

        sb.append("Floor: ").append(player.getFloor()).append('\n');
        out.print(sb);
    }

    private String colorFor(Tile tile) {
        return switch (tile) {
            case WALL -> DARK_GRAY;
            case FLOOR -> WHITE;
            case STAIRS_DOWN -> YELLOW;
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RendererTest`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```
git add src/main/java/ca/ninja/thenix/textrogue/render/ src/test/java/ca/ninja/thenix/textrogue/render/
git commit -m "feat: add Renderer with ANSI color output and floor status line"
```

---

### Task 9: Game Loop + Main Wiring

**Files:**
- Create: `src/main/java/ca/ninja/thenix/textrogue/Game.java`
- Create: `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`
- Modify: `src/main/java/ca/ninja/thenix/textrogue/Main.java`

**Interfaces:**
- Consumes: `DungeonGenerator`, `Dungeon`, `Player`, `Renderer`, `InputHandler`, `Command`
- Produces: `Game(InputHandler input, Renderer renderer)`; method `void start()`; package-private fields `Dungeon dungeon`, `Player player` and method `void processCommand(Command cmd)` for testing

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/ca/ninja/thenix/textrogue/GameTest.java`:

```java
package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
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
        InputHandler input = new InputHandler(new ByteArrayInputStream(new byte[0]));
        Game game = new Game(input, new Renderer(NULL_OUT));
        game.dungeon = dungeon;
        game.player = player;
        return game;
    }

    @Test
    void quitCommandStopsLoop() {
        InputHandler input = new InputHandler(new ByteArrayInputStream("q\n".getBytes()));
        Game game = new Game(input, new Renderer(NULL_OUT));
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

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=GameTest`
Expected: FAIL — `cannot find symbol: Game`

- [ ] **Step 3: Create Game.java**

Create `src/main/java/ca/ninja/thenix/textrogue/Game.java`:

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
            Command cmd = input.readCommand();
            processCommand(cmd);
            renderer.render(dungeon, player);
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

    private void tryMove(int nx, int ny) {
        Tile target = dungeon.getTile(nx, ny);
        if (target == Tile.WALL) return;
        player.moveTo(nx, ny);
        if (target == Tile.STAIRS_DOWN) descend();
    }

    private void descend() {
        player.descendFloor();
        dungeon = new DungeonGenerator(System.currentTimeMillis()).generate();
        player.moveTo(dungeon.getStartX(), dungeon.getStartY());
    }

    boolean isRunning() { return running; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=GameTest`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: Update Main.java to wire everything together**

Replace `src/main/java/ca/ninja/thenix/textrogue/Main.java`:

```java
package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;

public class Main {
    public static void main(String[] args) {
        InputHandler input = new InputHandler(System.in);
        Renderer renderer = new Renderer(System.out);
        new Game(input, renderer).start();
    }
}
```

- [ ] **Step 6: Run all tests to confirm nothing is broken**

Run: `mvn test`
Expected: All tests pass, zero failures

- [ ] **Step 7: Run the game and verify it plays**

Run: `mvn compile exec:java`

Verify:
- Dungeon renders with `#` walls, `.` floors, `>` stairs in color
- Typing `w` + Enter moves `@` up if there is floor above
- Typing `>` (stepping onto stairs) generates a new dungeon and shows `Floor: 2`
- Typing `q` + Enter exits cleanly

- [ ] **Step 8: Commit**

```
git add src/main/java/ca/ninja/thenix/textrogue/Game.java src/main/java/ca/ninja/thenix/textrogue/Main.java src/test/java/ca/ninja/thenix/textrogue/GameTest.java
git commit -m "feat: add Game loop and wire Main — Phase A complete"
```
