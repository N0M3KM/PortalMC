package dev.portalmod.assets;

/** Enumerate each voxel once, near to far. Six shell faces avoid rescanning cube interiors. */
public final class SceneOffsets {
    private SceneOffsets() { }
    public static int[] create(int radius) {
        if(radius<0 || radius>32) throw new IllegalArgumentException("Scene radius");
        int side=radius*2+1; int[] offsets=new int[side*side*side*3]; int cursor=3;
        for(int shell=1;shell<=radius;shell++) {
            for(int x=-shell;x<=shell;x++) for(int z=-shell;z<=shell;z++) for(int sign:new int[]{-1,1}) cursor=put(offsets,cursor,x,sign*shell,z);
            for(int x=-shell;x<=shell;x++) for(int y=1-shell;y<shell;y++) for(int sign:new int[]{-1,1}) cursor=put(offsets,cursor,x,y,sign*shell);
            for(int y=1-shell;y<shell;y++) for(int z=1-shell;z<shell;z++) for(int sign:new int[]{-1,1}) cursor=put(offsets,cursor,sign*shell,y,z);
        }
        if(cursor!=offsets.length) throw new IllegalStateException("Scene enumeration count");
        return offsets;
    }
    private static int put(int[] a,int cursor,int x,int y,int z) { a[cursor++]=x; a[cursor++]=y; a[cursor++]=z; return cursor; }
}
