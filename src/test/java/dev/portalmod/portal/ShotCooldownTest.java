package dev.portalmod.portal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShotCooldownTest {
    @Test void respawnDoesNotInheritTheOldPlayerAgeOrCooldown() {
        var c=new ShotCooldown(); Object world=new Object(), old=new Object();
        assertTrue(c.acquire(old,world,100_000,4)); assertFalse(c.acquire(old,world,100_001,4));
        assertTrue(c.acquire(new Object(),world,100_001,4));
    }
    @Test void normalShotsStillRespectCooldownAndClockChangesRecover() {
        var c=new ShotCooldown(); Object player=new Object(),world=new Object();
        assertTrue(c.acquire(player,world,100,4)); assertFalse(c.acquire(player,world,103,4));
        assertTrue(c.acquire(player,world,104,4)); assertTrue(c.acquire(player,new Object(),0,4));
        assertTrue(c.acquire(player,world,-10,4)); c.reset(); assertTrue(c.acquire(player,world,-10,4));
    }
}
