package dev.portalmod.mixin;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(AbstractArrow.class)
public interface PortalArrowAccessor {
    @Invoker("stepMoveAndHit") void portalmod$step(BlockHitResult hit);
}
