package dev.portalmod.mixin;

import dev.portalmod.PortalItems;
import dev.portalmod.config.MovementConfig;
import dev.portalmod.movement.MovementHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMovementMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void portalmod$travel(Vec3 input, CallbackInfo ci) {
        if (MovementHooks.travel((Player) (Object) this)) ci.cancel();
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void portalmod$fallProtection(double distance, float multiplier, DamageSource source,
                                          CallbackInfoReturnable<Boolean> cir) {
        Player p = (Player) (Object) this;
        MovementConfig config = MovementHooks.config(p);
        boolean gun = config.preventFallDamageWithGun
                && (p.getMainHandItem().is(PortalItems.PORTAL_GUN) || p.getOffhandItem().is(PortalItems.PORTAL_GUN));
        boolean boots = config.preventFallDamageWithBoots && p.getItemBySlot(EquipmentSlot.FEET).is(PortalItems.LONG_FALL_BOOTS);
        if (gun || boots) cir.setReturnValue(false);
    }
}
