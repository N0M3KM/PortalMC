package dev.portalmod.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.portalmod.PortalItems;
import dev.portalmod.client.assets.LocalPortalAssets;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class PortalHandMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void portalmod$gun(float partialTicks, PoseStack pose, SubmitNodeCollector collector,
                               PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        if (LocalPortalAssets.gunReady() && (state.mainHandItem.is(PortalItems.PORTAL_GUN) || state.offHandItem.is(PortalItems.PORTAL_GUN))) {
            float age = player.avatarRenderState == null ? 0 : player.avatarRenderState.ageInTicks;
            LocalPortalAssets.submitGun(pose, collector, player.avatarRenderState == null ? 15728880 : player.avatarRenderState.lightCoords, age, 0);
            ci.cancel();
        }
    }
}
