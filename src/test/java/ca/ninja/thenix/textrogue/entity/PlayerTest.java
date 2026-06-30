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
