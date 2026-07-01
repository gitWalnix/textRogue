package ca.ninja.thenix.textrogue.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyTest {
    @Test
    void startsAliveAtGivenPositionAndHp() {
        Enemy enemy = new Enemy(3, 4, 4);
        assertEquals(3, enemy.getX());
        assertEquals(4, enemy.getY());
        assertEquals(4, enemy.getHp());
        assertTrue(enemy.isAlive());
    }

    @Test
    void moveToUpdatesPosition() {
        Enemy enemy = new Enemy(0, 0, 4);
        enemy.moveTo(1, 2);
        assertEquals(1, enemy.getX());
        assertEquals(2, enemy.getY());
    }

    @Test
    void takeDamageReducesHp() {
        Enemy enemy = new Enemy(0, 0, 4);
        enemy.takeDamage(2);
        assertEquals(2, enemy.getHp());
        assertTrue(enemy.isAlive());
    }

    @Test
    void takeDamageKillsAtZeroHp() {
        Enemy enemy = new Enemy(0, 0, 4);
        enemy.takeDamage(4);
        assertEquals(0, enemy.getHp());
        assertFalse(enemy.isAlive());
    }

    @Test
    void takeDamageClampsHpAtZero() {
        Enemy enemy = new Enemy(0, 0, 4);
        enemy.takeDamage(999);
        assertEquals(0, enemy.getHp());
        assertFalse(enemy.isAlive());
    }
}
