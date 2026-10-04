package dev.portalmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.portalmod.movement.MovementHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingMovementMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;applyEffectsFromBlocks()V"))
    private void portalmod$deferServerBlockEffects(LivingEntity entity, Operation<Void> original) {
        // Apply once after authoritative movement, rather than again at the old position.
        if (!(entity instanceof ServerPlayer p) || !MovementHooks.active(p)) original.call(entity);
    }

    @ModifyArgs(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
    private void portalmod$preserveSmallVelocity(Args args) {
        if ((Object) this instanceof Player p && MovementHooks.active(p)) {
            // Vanilla's per-tick cutoff must not change the shared prediction equations.
            var velocity = p.getDeltaMovement();
            args.set(0, velocity.x); args.set(1, velocity.y); args.set(2, velocity.z);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void portalmod$skipVanillaJump(CallbackInfo ci) {
        if ((Object) this instanceof Player p && MovementHooks.active(p)) ci.cancel();
    }

    @Inject(method = "maxUpStep", at = @At("HEAD"), cancellable = true)
    private void portalmod$stepHeight(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player p && MovementHooks.active(p)) {
            var config = MovementHooks.config(p);
            cir.setReturnValue((float) (config.stepHeight / config.sourceUnitsPerBlock));
        }
    }
}
