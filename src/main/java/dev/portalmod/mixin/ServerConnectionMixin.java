package dev.portalmod.mixin;

import dev.portalmod.server.ServerMovement;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerConnectionMixin {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Shadow private boolean clientIsFloating;
    @Shadow public abstract void resetPosition();

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void portalmod$ignoreClientPosition(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, player.level());
        if (ServerMovement.active(player) && awaitingPositionFromClient == null) {
            // Rotation travels with the validated input command. Ignore all client positions.
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void portalmod$serverTick(CallbackInfo ci) {
        if (player.level().getServer().isPaused()) return;
        ServerMovement.tick(player, awaitingPositionFromClient != null);
        if (ServerMovement.active(player)) {
            resetPosition();
            // Vanilla floating checks judge client position packets; the custom solver enforces gravity.
            clientIsFloating = false;
        }
    }

    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
    private void portalmod$resetForTeleport(CallbackInfo ci) { ServerMovement.invalidate(player); }
}
