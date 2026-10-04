package dev.portalmod.mixin;

import dev.portalmod.portal.PortalConfig;
import dev.portalmod.portal.PortalServer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
abstract class PortalTrackingRefreshMixin {
    @Shadow @Final private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
    @Shadow @Final private ServerLevel level;
    @Unique private boolean portalmod$hadPortals;
    @Inject(method = "tick", at = @At("HEAD"))
    private void portalmod$refresh(CallbackInfo ci) {
        boolean hasPortals = !PortalServer.frames().isEmpty();
        if ((hasPortals || portalmod$hadPortals) && (hasPortals != portalmod$hadPortals || level.getGameTime() % PortalConfig.get().entityRefreshTicks == 0))
            for (var tracked : entityMap.values()) tracked.updatePlayers(level.players());
        portalmod$hadPortals = hasPortals;
    }
}
