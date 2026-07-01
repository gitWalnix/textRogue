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

    @Test
    void startsWithFullHp() {
        Player player = new Player(0, 0);
        assertEquals(20, player.getHp());
        assertEquals(20, player.getMaxHp());
    }

    @Test
    void startsFacingDown() {
        Player player = new Player(0, 0);
        assertEquals(Facing.DOWN, player.getFacing());
    }

    @Test
    void setFacingUpdatesFacing() {
        Player player = new Player(0, 0);
        player.setFacing(Facing.LEFT);
        assertEquals(Facing.LEFT, player.getFacing());
    }

    @Test
    void takeDamageReducesHp() {
        Player player = new Player(0, 0);
        player.takeDamage(5);
        assertEquals(15, player.getHp());
    }

    @Test
    void takeDamageClampsAtZero() {
        Player player = new Player(0, 0);
        player.takeDamage(999);
        assertEquals(0, player.getHp());
    }

    @Test
    void isDeadReflectsHp() {
        Player player = new Player(0, 0);
        assertFalse(player.isDead());
        player.takeDamage(20);
        assertTrue(player.isDead());
    }
}
