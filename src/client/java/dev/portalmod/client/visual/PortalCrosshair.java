package dev.portalmod.client.visual;

import dev.portalmod.PortalItems;
import dev.portalmod.client.portal.ClientPortals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Original dual-crescent reticle. Colour indicators use this owner's server-confirmed portal state. */
public final class PortalCrosshair {
    private PortalCrosshair() { }
    public static boolean active() {
        var mc=Minecraft.getInstance(); var p=mc.player;
        return PortalVisualConfig.current.portalCrosshair && p!=null && !p.isSpectator()
            && mc.options.getCameraType().isFirstPerson()
            && (p.getMainHandItem().is(PortalItems.PORTAL_GUN) || p.getOffhandItem().is(PortalItems.PORTAL_GUN));
    }
    public static void draw(GuiGraphicsExtractor g) {
        var c=PortalVisualConfig.current; var p=Minecraft.getInstance().player;
        if(p==null) return;
        boolean blue=false,orange=false;
        for(var frame:ClientPortals.frames()) if(frame.owner().equals(p.getUUID())) { if(frame.orange()) orange=true; else blue=true; }
        int x=g.guiWidth()/2,y=g.guiHeight()/2,r=c.crosshairSize/2;
        g.nextStratum();
        crescent(g,x,y,r,false,c.crosshairBlue,blue);
        crescent(g,x,y,r,true,c.crosshairOrange,orange);
        g.fill(x,y,x+1,y+1,0xffffffff);
    }
    private static void crescent(GuiGraphicsExtractor g,int x,int y,int r,boolean right,int color,boolean open) {
        // An oval broken at top/bottom; original raster geometry, no Valve texture is shipped.
        int rgb=color&0xffffff,alpha=open?0xff000000:0x88000000;
        for(int dy=-r+2;dy<=r-2;dy++) {
            int dx=Math.max(3,(int)Math.round(Math.sqrt(1-(double)dy*dy/(r*r))*r*.65));
            int edge=x+(right?dx:-dx);
            g.fill(edge,y+dy,edge+2,y+dy+1,alpha|rgb);
        }
        int indicator=x+(right?r:-r);
        if(open) g.fill(indicator-1,y-1,indicator+2,y+2,0xff000000|rgb);
        else {
            g.fill(indicator-1,y-1,indicator+2,y,0x88000000|rgb);
            g.fill(indicator-1,y+1,indicator+2,y+2,0x88000000|rgb);
        }
    }
}
