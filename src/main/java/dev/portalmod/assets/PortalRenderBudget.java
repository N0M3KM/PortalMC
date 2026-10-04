package dev.portalmod.assets;

/** Hysteresis prevents recursion flicker: reduce after sustained overload, restore after a quiet interval. */
public final class PortalRenderBudget {
    private int depth=-1,hot,cool;
    public int update(int requested,boolean adaptive,double frameMs,double portalMs,double frameBudget,double portalBudget,int overloadFrames,int recoveryFrames) {
        if(depth<0 || depth>requested) depth=requested;
        if(!adaptive) { depth=requested; hot=0; cool=0; return depth; }
        if(!Double.isFinite(frameMs) || frameMs<=0 || frameMs>250) return depth;
        if(frameMs>frameBudget || portalMs>portalBudget) {
            cool=0;
            if(++hot>=overloadFrames) { depth=Math.max(Math.min(1,requested),depth-1); hot=0; }
        } else {
            hot=0;
            if(++cool>=recoveryFrames) { depth=Math.min(requested,depth+1); cool=0; }
        }
        return depth;
    }
    public void reset() { depth=-1; hot=0; cool=0; }
}
