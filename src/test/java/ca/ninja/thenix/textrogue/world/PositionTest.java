package ca.ninja.thenix.textrogue.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionTest {
    @Test
    void sameCoordinatesAreEqual() {
        assertEquals(new Position(3, 4), new Position(3, 4));
        assertEquals(new Position(3, 4).hashCode(), new Position(3, 4).hashCode());
    }

    @Test
    void differentCoordinatesAreNotEqual() {
        assertNotEquals(new Position(3, 4), new Position(4, 3));
    }
}
