package dev.portalmod.mixin;

import dev.portalmod.portal.PortalServer;
import java.util.Set;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native pairing keeps equipment, metadata, movement and projectiles synchronized at the destination. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
abstract class PortalEntityTrackingMixin {
    @Shadow @Final private Entity entity;
    @Shadow @Final private ServerEntity serverEntity;
    @Shadow @Final private Set<ServerPlayerConnection> seenBy;
    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void portalmod$visible(ServerPlayer player, CallbackInfo ci) {
        if (player != entity && PortalServer.visibleThroughPortal(player, entity)) {
            if (seenBy.add(player.connection)) serverEntity.addPairing(player);
            ci.cancel();
        }
    }
}
