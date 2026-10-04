package dev.portalmod.client.mixin;

import dev.portalmod.client.visual.CameraEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraEffectsMixin {
    // Confirmed in the pinned 26.3 mapped Camera source. Apply once to extracted state;
    // both portal camera mapping and first-person HUD use this same orientation.
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void portalmod$viewPunch(CameraRenderState state,DeltaTracker delta,CallbackInfo ci) {
        var mc=Minecraft.getInstance();
        if(!state.isFirstPerson || mc.player==null || mc.getCameraEntity()!=mc.player) return;
        var effect=CameraEffects.sample();
        state.orientation.mul(new Quaternionf().rotationXYZ((float)Math.toRadians(-effect.pitch()),0,(float)Math.toRadians(effect.roll())));
        state.viewRotationMatrix.rotation(new Quaternionf(state.orientation).conjugate());
        state.xRot+=effect.pitch();
    }
}
