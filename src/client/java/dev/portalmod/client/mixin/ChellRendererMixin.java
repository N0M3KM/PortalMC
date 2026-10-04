package dev.portalmod.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.portalmod.client.assets.LocalPortalAssets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class ChellRendererMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void portalmod$chell(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar && LocalPortalAssets.chellReady() && !state.isInvisible && !avatar.isSpectator) {
            LocalPortalAssets.submitChell(avatar, pose, collector);
            ci.cancel();
        }
    }
}
