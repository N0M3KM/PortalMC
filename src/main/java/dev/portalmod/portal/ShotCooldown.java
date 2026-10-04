package dev.portalmod.portal;

/** A player incarnation/world clock owns this client-side cooldown, not the player's lifetime age. */
public final class ShotCooldown {
    private Object player, world;
    private long lastTick;
    private boolean fired;
    public boolean acquire(Object currentPlayer,Object currentWorld,long tick,int delay) {
        if(player!=currentPlayer || world!=currentWorld || tick<lastTick) {
            player=currentPlayer; world=currentWorld; fired=false;
        }
        if(fired && tick-lastTick<delay) return false;
        lastTick=tick; fired=true; return true;
    }
    public void reset() { player=null; world=null; fired=false; }
}
