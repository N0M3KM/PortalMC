package dev.portalmod.gametest;

import dev.portalmod.config.ConfigManager;
import dev.portalmod.movement.MovementHooks;
import dev.portalmod.server.ServerMovement;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Real client -> payload -> integrated server -> acknowledged prediction, with actual block collision. */
public final class MovementGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            var server = world.getServer();
            server.runCommand("difficulty peaceful");
            server.runCommand("fill -8 79 -8 8 79 64 stone");
            server.runCommand("gamemode survival @a");
            server.runCommand("tp @a 0.5 80 0.5 0 0");
            context.waitFor(client -> client.player != null && MovementHooks.active(client.player)
                    && client.player.onGround() && Math.abs(client.player.getY() - 80) < 0.01, 400);
            world.getConnection().waitForChunksRender();
            context.getInput().lookAt(0, 0);
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(40);
            context.getInput().releaseKey(options -> options.keyUp);
            context.waitTicks(10);
            double clientZ = context.computeOnClient(client -> client.player.getZ());
            double serverZ = server.computeOnServer(s -> world.getConnection().getServerPlayer().getZ());
            require(clientZ > 5 && clientZ < 12, "Walking distance should follow the converted 175-unit speed, got client=" + clientZ + ", server=" + serverZ);
            require(Math.abs(clientZ - serverZ) < 0.15, "Client and server should converge after releasing input");

            // A malicious vanilla position packet must not replace the server's simulated position.
            context.runOnClient(client -> client.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    client.player.position().add(200, 0, 0), true, false)));
            world.getConnection().waitForServerboundPackets();
            double authoritativeX = server.computeOnServer(s -> world.getConnection().getServerPlayer().getX());
            require(Math.abs(authoritativeX) < 5,
                    "Server accepted a client-authored position during Portal movement");

            context.getInput().holdKey(options -> options.keyJump);
            context.waitFor(client -> client.player.getY() > 80.5, 100);
            context.getInput().releaseKey(options -> options.keyJump);
            context.waitFor(client -> client.player.onGround(), 200);

            double beforeCrouch = context.computeOnClient(client -> client.player.getZ());
            context.getInput().holdKey(options -> options.keyShift);
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(40);
            context.getInput().releaseKey(options -> options.keyUp);
            context.getInput().releaseKey(options -> options.keyShift);
            context.waitTicks(10);
            double crouchDistance = context.computeOnClient(client -> client.player.getZ()) - beforeCrouch;
            require(crouchDistance > 2 && crouchDistance < 4, "Crouch speed conversion failed: " + crouchDistance);

            server.runCommand("fill -2 80 18 2 82 18 stone");
            server.runCommand("tp @a 0.5 80 15.0 0 0");
            context.waitFor(client -> Math.abs(client.player.getZ() - 15) < 0.01 && MovementHooks.active(client.player), 100);
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(40);
            context.getInput().releaseKey(options -> options.keyUp);
            context.waitTicks(10);
            require(context.computeOnClient(client -> client.player.getZ()) < 17.71, "Player passed through a full-block wall");
            server.runCommand("fill -2 80 18 2 82 18 air");

            context.getInput().holdKey(options -> options.keyJump);
            int upwardStarts = 0;
            boolean rising = false;
            for (int tick = 0; tick < 80; tick++) {
                context.waitTicks(1);
                boolean nowRising = context.computeOnClient(client -> client.player.getDeltaMovement().y > 0);
                if (nowRising && !rising) upwardStarts++;
                rising = nowRising;
            }
            context.getInput().releaseKey(options -> options.keyJump);
            require(upwardStarts >= 3, "Holding jump should produce repeated bunny hops, got " + upwardStarts);
            context.waitFor(client -> client.player.onGround(), 200);

            server.runCommand("item replace entity @a armor.feet with minecraft:air");
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            require(drop(context, world) < 19, "Unprotected survival player should take fall damage");
            server.runCommand("item replace entity @a armor.feet with portalmod:long_fall_boots");
            require(drop(context, world) >= 19, "Long-fall boots should prevent fall damage");
            server.runCommand("item replace entity @a armor.feet with minecraft:air");
            server.runCommand("item replace entity @a weapon.mainhand with portalmod:portal_gun");
            require(drop(context, world) >= 19, "Held gun should prevent fall damage");

            server.runOnServer(s -> { ConfigManager.server().preventFallDamageWithGun = false; ServerMovement.reloadSessions(); });
            require(drop(context, world) < 19, "Disabling gun fall protection should restore fall damage");
            server.runOnServer(s -> { ConfigManager.server().preventFallDamageWithGun = true; ServerMovement.reloadSessions(); });

            server.runCommand("portalmod movement vanilla");
            context.waitFor(client -> !MovementHooks.active(client.player), 100);
            server.runCommand("portalmod movement portal");
            context.waitFor(client -> MovementHooks.active(client.player), 100);
            context.takeScreenshot("phase-1-movement");
        }
    }

    private float drop(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(s -> world.getConnection().getServerPlayer().setHealth(20));
        world.getServer().runCommand("tp @a 0.5 92 0.5 0 0");
        context.waitFor(client -> client.player.getY() > 90, 100);
        context.waitFor(client -> client.player.getY() < 80.1 && client.player.onGround(), 300);
        world.getConnection().waitForServerboundPackets();
        return world.getServer().computeOnServer(s -> world.getConnection().getServerPlayer().getHealth());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
