package dev.portalmod.mixin;

import dev.portalmod.portal.PortalWorld;
import java.util.stream.StreamSupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CollisionGetter.class)
public interface PortalCollisionMixin {
    @Inject(method = "getBlockCollisions", at = @At("RETURN"), cancellable = true)
    private void portalmod$opening(Entity entity, AABB box, CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
        if ((Object) this instanceof Level level && entity != null) {
            Iterable<VoxelShape> original = cir.getReturnValue();
            cir.setReturnValue(() -> StreamSupport.stream(original.spliterator(), false)
                    .filter(shape -> !PortalWorld.ignoresShape(level, entity, box, shape)).iterator());
        }
    }
}
