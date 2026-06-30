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
