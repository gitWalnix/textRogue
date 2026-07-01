package ca.ninja.thenix.textrogue.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FacingTest {
    @Test
    void deltasPointInCorrectDirection() {
        assertEquals(0, Facing.UP.dx());
        assertEquals(-1, Facing.UP.dy());
        assertEquals(0, Facing.DOWN.dx());
        assertEquals(1, Facing.DOWN.dy());
        assertEquals(-1, Facing.LEFT.dx());
        assertEquals(0, Facing.LEFT.dy());
        assertEquals(1, Facing.RIGHT.dx());
        assertEquals(0, Facing.RIGHT.dy());
    }

    @Test
    void toCharMatchesDirection() {
        assertEquals('^', Facing.UP.toChar());
        assertEquals('v', Facing.DOWN.toChar());
        assertEquals('<', Facing.LEFT.toChar());
        assertEquals('>', Facing.RIGHT.toChar());
    }
}
