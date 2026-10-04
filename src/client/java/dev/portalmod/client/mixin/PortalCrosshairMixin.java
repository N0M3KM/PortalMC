package dev.portalmod.client.mixin;

import dev.portalmod.client.visual.PortalCrosshair;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class PortalCrosshairMixin {
    // Pinned 26.3 Hud.extractCrosshair(GuiGraphicsExtractor, DeltaTracker).
    @Inject(method="extractCrosshair",at=@At("HEAD"),cancellable=true)
    private void portalmod$reticle(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci) {
        if(PortalCrosshair.active() && !Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
            PortalCrosshair.draw(graphics); ci.cancel();
        }
    }
}
