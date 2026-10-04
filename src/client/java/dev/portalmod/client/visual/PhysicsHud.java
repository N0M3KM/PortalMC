package dev.portalmod.client.visual;

import com.mojang.blaze3d.platform.InputConstants;
import dev.portalmod.client.portal.PortalRenderer;
import dev.portalmod.movement.MovementHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** Source-style measurements. Velocity converts Minecraft blocks/tick to blocks/s, then Source units/s. */
public final class PhysicsHud {
    private static final FontDescription FONT=new FontDescription.Resource(Identifier.fromNamespaceAndPath("portalmod","telemetry"));
    private static KeyMapping key;
    private static double peak,airTime,impact,fallSpeed;
    private static boolean grounded=true;
    private static long traversals,traversalBase;
    private static int playerId=-1;
    private PhysicsHud() { }
    public static void initialize() {
        // F8 is unused by vanilla 26.3; configurable through Minecraft's normal Controls screen.
        key=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.portalmod.showpos",InputConstants.Type.KEYBOARD,InputConstants.KEY_F8,KeyMapping.Category.MISC));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,context) -> dispatcher.register(ClientCommands.literal("portalpos")
            .executes(c -> toggle()).then(ClientCommands.literal("on").executes(c -> enabled(true)))
            .then(ClientCommands.literal("off").executes(c -> enabled(false))).then(ClientCommands.literal("reset").executes(c -> { reset(); return 1; }))
            .then(ClientCommands.literal("debug").executes(c -> { PortalVisualConfig.current.hudDebug=!PortalVisualConfig.current.hudDebug; return 1; }))
            .then(ClientCommands.literal("performance").executes(c -> { PortalRenderer.performancePreset(); return 1; }))));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("portalmod","showpos"),(graphics,delta) -> draw(graphics));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(key.consumeClick()) toggle();
            var player=client.player; if(player==null || client.isPaused()) return;
            if(player.getId()!=playerId) { reset(); playerId=player.getId(); grounded=player.onGround(); }
            var v=player.getDeltaMovement().scale(20);
            peak=Math.max(peak,v.horizontalDistance());
            if(!player.onGround()) { airTime+=.05; fallSpeed=Math.max(fallSpeed,-v.y); }
            if(player.onGround() && !grounded) { impact=fallSpeed; fallSpeed=0; }
            if(player.onGround()) airTime=0;
            grounded=player.onGround();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client) -> { traversals=0; traversalBase=0; playerId=-1; reset(); });
    }
    public static void traversal(long count) { traversals=Math.max(traversals,count); }
    public static void landed(double speed) { impact=speed; }
    private static int toggle() { return enabled(!PortalVisualConfig.current.hudEnabled); }
    private static int enabled(boolean value) { PortalVisualConfig.current.hudEnabled=value; return 1; }
    private static void reset() { peak=0; airTime=0; impact=0; fallSpeed=0; traversalBase=traversals; }
    private static String f(String format,Object... values) { return String.format(Locale.ROOT,format,values); }
    private static void draw(GuiGraphicsExtractor graphics) {
        var mc=Minecraft.getInstance(); var c=PortalVisualConfig.current; var p=mc.player;
        if(!c.hudEnabled || p==null) return;
        double units=MovementHooks.config(p).sourceUnitsPerBlock;
        var v=p.getDeltaMovement().scale(20); double horizontal=v.horizontalDistance(),total=v.length();
        List<String> lines=new ArrayList<>();
        lines.add("PORTALMC SHOWPOS"); lines.add(f("POS B: %.2f %.2f %.2f",p.getX(),p.getY(),p.getZ()));
        lines.add(f("POS U: %.1f %.1f %.1f",p.getX()*units,p.getY()*units,p.getZ()*units));
        var camera=mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        lines.add(f("ANG: %.1f %.1f %.1f",camera.xRot,camera.yRot,camera.isFirstPerson?CameraEffects.sample().roll():0));
        lines.add(f("SPD U/S H/V/T: %.1f %.1f %.1f",horizontal*units,v.y*units,total*units));
        lines.add(f("SPD B/S H/V/T: %.2f %.2f %.2f",horizontal,v.y,total));
        lines.add(f("GROUND: %s CROUCH: %s",p.onGround()?"YES":"NO",p.isCrouching()?"YES":"NO"));
        lines.add(f("PEAK H: %.1f U/S  %.2f B/S",peak*units,peak));
        lines.add(f("AIR: %.2f S LAND: %.1f U/S",airTime,impact*units));
        lines.add(f("PORTAL COUNT: %d",traversals-traversalBase));
        if(c.hudDebug) {
            lines.add(f("FPS: %d PORTAL CPU: %.2f MS",mc.getFps(),PortalRenderer.lastPrepareMs+PortalRenderer.lastCompositeMs));
            lines.add(f("PASSES: %d DEPTH: %d",PortalRenderer.lastPassCount,PortalRenderer.effectiveDepth));
            StringBuilder pass=new StringBuilder("CPU PASS MS:");
            for(int i=0;i<PortalRenderer.lastPassCount;i++) {
                pass.append(f(" %d=%.2f",i+1,PortalRenderer.passMillis[i]));
                if(i%3==2 || i==PortalRenderer.lastPassCount-1) { lines.add(pass.toString()); pass=new StringBuilder("            "); }
            }
        }
        List<Component> text=lines.stream().<Component>map(line -> c.hudMonospace?Component.literal(line).withStyle(style -> style.withFont(FONT)):Component.literal(line)).toList();
        int width=text.stream().mapToInt(mc.font::width).max().orElse(0);
        int x=Math.max(0,graphics.guiWidth()-width-c.hudMargin),y=c.hudMargin;
        if(c.hudBackground) graphics.fill(x-2,y-2,x+width+2,y+text.size()*c.hudLineHeight+2,0x88000000);
        for(Component line:text) { graphics.text(mc.font,line,x,y,c.hudColor,c.hudShadow); y+=c.hudLineHeight; }
    }
}
