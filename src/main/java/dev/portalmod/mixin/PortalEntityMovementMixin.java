package dev.portalmod.mixin;

import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.movement.MotionState;
import dev.portalmod.movement.Vector;
import dev.portalmod.portal.PortalServer;
import dev.portalmod.portal.PortalTraversal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Split native mob/item travel at the aperture before querying the hidden wall behind it. */
@Mixin(Entity.class)
abstract class PortalEntityMovementMixin {
    @Unique private boolean portalmod$moving;
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void portalmod$split(MoverType mover, Vec3 displacement, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if (portalmod$moving || entity.level().isClientSide() || entity instanceof Player) return;
        if(!dev.portalmod.portal.PortalWorld.config(entity.level()).enabled || dev.portalmod.portal.PortalServer.frames().isEmpty()) return;
        Vector position = MinecraftCollisionWorld.fromMinecraft(entity.position()), delta = MinecraftCollisionWorld.fromMinecraft(displacement);
        Vector velocity = MinecraftCollisionWorld.fromMinecraft(entity.getDeltaMovement()).scale(20);
        var crossing = PortalTraversal.cross(entity, position, new MotionState(position.add(delta), velocity, entity.onGround(), false, false));
        if (crossing == null) return;
        Vector offset = new Vector(0, entity.getBoundingBox().getYsize()/2, 0);
        double t = crossing.entry().crossing(position.add(offset), position.add(delta).add(offset));
        Vec3 prefix = displacement.scale(t);
        if (((EntityCollisionAccessor)entity).portalmod$collide(prefix).subtract(prefix).lengthSqr() > 1e-12) return;
        ci.cancel(); portalmod$moving = true;
        try {
            entity.move(mover, prefix);
            if (entity.position().subtract(MinecraftCollisionWorld.toMinecraft(position).add(prefix)).lengthSqr() > 1e-10) return;
            Vec3 destination = MinecraftCollisionWorld.toMinecraft(crossing.exitStart());
            PortalTraversal.rotateView(entity, crossing);
            entity.absSnapTo(destination.x, destination.y, destination.z, entity.getYRot(), entity.getXRot());
            entity.setDeltaMovement(MinecraftCollisionWorld.toMinecraft(crossing.entry().rotateTo(crossing.exit(), velocity).scale(.05)));
            entity.setOnGround(false); entity.resetFallDistance();
            entity.move(mover, MinecraftCollisionWorld.toMinecraft(crossing.state().position().subtract(crossing.exitStart())));
            entity.needsSync = true;
            PortalServer.transported(entity);
        } finally { portalmod$moving = false; }
    }
}
