package dev.portalmod.mixin;

import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.portal.PortalWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClipContext.class)
public abstract class PortalProjectileClipMixin {
    @Shadow @Final private CollisionContext collisionContext;
    @Shadow @Final private Vec3 from;
    @Shadow @Final private Vec3 to;
    @Inject(method = "getBlockShape", at = @At("HEAD"), cancellable = true)
    private void portalmod$projectile(BlockState state, BlockGetter getter, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (!(getter instanceof Level level) || !(collisionContext instanceof EntityCollisionContext context)
                || !(context.getEntity() instanceof Projectile entity) || !PortalWorld.config(level).enabled) return;
        var start = MinecraftCollisionWorld.fromMinecraft(from); var end = MinecraftCollisionWorld.fromMinecraft(to);
        for (var portal : PortalWorld.frames(level)) {
            if (!portal.dimension().equals(level.dimension().identifier().toString()) || PortalWorld.partner(level, portal) == null) continue;
            double t = portal.crossing(start, end);
            if (t < 0 || !portal.contains(start.add(end.subtract(start).scale(t)), PortalWorld.extent(entity.getBoundingBox(), portal.right()), PortalWorld.extent(entity.getBoundingBox(), portal.up()))) continue;
            // Only the segment in front of the entry belongs to this world. The remaining ray is tested at the exit.
            if (portal.distance(MinecraftCollisionWorld.fromMinecraft(Vec3.atCenterOf(pos))) < 0
                    || portal.support().stream().anyMatch(c -> c.x() == pos.getX() && c.y() == pos.getY() && c.z() == pos.getZ())) cir.setReturnValue(Shapes.empty());
        }
    }
}
