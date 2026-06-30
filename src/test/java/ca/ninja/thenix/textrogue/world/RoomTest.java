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
