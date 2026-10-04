package dev.portalmod.client.mixin;

import dev.portalmod.client.portal.ClientPortals;
import dev.portalmod.client.portal.PortalChunkCacheBridge;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkCache.class)
public abstract class RemotePortalChunksMixin implements PortalChunkCacheBridge {
    @Shadow @Final private ClientLevel level;
    @Unique private final Map<Long, LevelChunk> portalmod$remote = new LinkedHashMap<>();
    @Inject(method = "getChunk", at = @At("RETURN"), cancellable = true)
    private void portalmod$get(int x, int z, ChunkStatus status, boolean load, CallbackInfoReturnable<LevelChunk> cir) {
        if ((cir.getReturnValue() == null || cir.getReturnValue() instanceof EmptyLevelChunk) && ClientPortals.remoteAllowed(x, z)) {
            LevelChunk chunk = portalmod$remote.get(new ChunkPos(x, z).pack());
            if (chunk != null) cir.setReturnValue(chunk);
        }
    }
    @Inject(method = "replaceWithPacketData", at = @At("HEAD"), cancellable = true)
    private void portalmod$store(int x, int z, ClientboundLevelChunkPacketData data, CallbackInfoReturnable<LevelChunk> cir) {
        if (!ClientPortals.remoteAllowed(x, z)) return;
        if (((ClientChunkCache)(Object)this).storage.inRange(x, z)) return;
        portalmod$remote.entrySet().removeIf(e -> !ClientPortals.remoteAllowed(e.getValue().getPos().x(), e.getValue().getPos().z()));
        long key = new ChunkPos(x, z).pack();
        LevelChunk chunk = portalmod$remote.computeIfAbsent(key, ignored -> new LevelChunk(level, new ChunkPos(x, z)));
        chunk.replaceWithPacketData(x, z, data);
        level.onChunkLoaded(new ChunkPos(x, z));
        while (portalmod$remote.size() > ClientPortals.config().maxRemoteChunks) portalmod$remote.remove(portalmod$remote.keySet().iterator().next());
        cir.setReturnValue(chunk);
    }
    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void portalmod$retainNative(int x, int z, ClientboundLevelChunkPacketData data, CallbackInfoReturnable<LevelChunk> cir) {
        if (cir.getReturnValue() != null && ClientPortals.remoteAllowed(x, z)) portalmod$remote.put(new ChunkPos(x, z).pack(), cir.getReturnValue());
        portalmod$prune();
    }
    @Override public void portalmod$retain() {
        var cache = (ClientChunkCache)(Object)this;
        for (var frame : ClientPortals.frames()) {
            int cx = Math.floorDiv((int)Math.floor(frame.center().x()),16), cz = Math.floorDiv((int)Math.floor(frame.center().z()),16), r = ClientPortals.config().chunkRadius;
            for (int x = cx-r; x <= cx+r; x++) for (int z = cz-r; z <= cz+r; z++) if (ClientPortals.remoteAllowed(x,z)) {
                LevelChunk chunk = cache.getChunk(x,z,ChunkStatus.FULL,false);
                if (chunk != null && !(chunk instanceof EmptyLevelChunk)) portalmod$remote.put(new ChunkPos(x,z).pack(), chunk);
            }
        }
        portalmod$prune();
    }
    @Unique private void portalmod$prune() {
        portalmod$remote.entrySet().removeIf(e -> !ClientPortals.remoteAllowed(e.getValue().getPos().x(), e.getValue().getPos().z()));
        while (portalmod$remote.size() > ClientPortals.config().maxRemoteChunks) portalmod$remote.remove(portalmod$remote.keySet().iterator().next());
    }
}
