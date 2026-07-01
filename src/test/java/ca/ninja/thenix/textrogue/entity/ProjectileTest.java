package ca.ninja.thenix.textrogue.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectileTest {
    @Test
    void startsAtGivenPositionDirectionAndRange() {
        Projectile p = new Projectile(5, 5, Facing.RIGHT, 5);
        assertEquals(5, p.getX());
        assertEquals(5, p.getY());
        assertEquals(Facing.RIGHT, p.getDirection());
        assertEquals(5, p.getRemainingRange());
    }

    @Test
    void advanceMovesInDirectionAndDecrementsRange() {
        Projectile p = new Projectile(5, 5, Facing.RIGHT, 5);
        p.advance();
        assertEquals(6, p.getX());
        assertEquals(5, p.getY());
        assertEquals(4, p.getRemainingRange());
    }

    @Test
    void advanceUpMovesNegativeY() {
        Projectile p = new Projectile(5, 5, Facing.UP, 5);
        p.advance();
        assertEquals(5, p.getX());
        assertEquals(4, p.getY());
    }
}
