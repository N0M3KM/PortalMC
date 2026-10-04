package dev.portalmod.mixin;
import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.portal.PortalWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Projectile.class)
abstract class PortalProjectileHitsMixin {
    @Inject(method = "canHitEntity", at = @At("RETURN"), cancellable = true)
    private void portalmod$hidden(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Projectile projectile = (Projectile)(Object)this;
        if (!cir.getReturnValueZ() || !PortalWorld.config(projectile.level()).enabled) return;
        var from = MinecraftCollisionWorld.fromMinecraft(projectile.position());
        var to = from.add(MinecraftCollisionWorld.fromMinecraft(projectile.getDeltaMovement()));
        for (var entry : PortalWorld.frames(projectile.level())) {
            if (!entry.dimension().equals(projectile.level().dimension().identifier().toString()) || PortalWorld.partner(projectile.level(), entry) == null) continue;
            double t = entry.crossing(from, to);
            if (t >= 0 && entry.contains(from.add(to.subtract(from).scale(t)), PortalWorld.extent(projectile.getBoundingBox(), entry.right()), PortalWorld.extent(projectile.getBoundingBox(), entry.up()))
                    && entry.distance(MinecraftCollisionWorld.fromMinecraft(target.getBoundingBox().getCenter())) < 0) cir.setReturnValue(false);
        }
    }
}
