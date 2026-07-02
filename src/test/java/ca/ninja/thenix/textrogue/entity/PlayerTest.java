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

    @Test
    void startsWithNoPotions() {
        Player player = new Player(0, 0);
        assertEquals(0, player.getPotionCount());
    }

    @Test
    void addPotionIncrementsCount() {
        Player player = new Player(0, 0);
        player.addPotion();
        player.addPotion();
        assertEquals(2, player.getPotionCount());
    }

    @Test
    void healIncreasesHpButCapsAtMax() {
        Player player = new Player(0, 0);
        player.takeDamage(15);
        player.heal(5);
        assertEquals(10, player.getHp());
        player.heal(999);
        assertEquals(20, player.getHp());
    }

    @Test
    void drinkPotionConsumesPotionAndHeals() {
        Player player = new Player(0, 0);
        player.takeDamage(15);
        player.addPotion();
        boolean drank = player.drinkPotion(10);
        assertTrue(drank);
        assertEquals(15, player.getHp());
        assertEquals(0, player.getPotionCount());
    }

    @Test
    void drinkPotionWithNoneDoesNothing() {
        Player player = new Player(0, 0);
        boolean drank = player.drinkPotion(10);
        assertFalse(drank);
        assertEquals(20, player.getHp());
    }
}
