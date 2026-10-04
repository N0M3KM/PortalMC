package dev.portalmod.gametest;

import dev.portalmod.client.portal.ClientPortals;
import dev.portalmod.client.portal.PortalRenderer;
import dev.portalmod.portal.PortalServer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Actual gun payloads, wall collision, player prediction and destination render passes. */
final class PortalGameTest {
    static void run(ClientGameTestContext context, TestSingleplayerContext world) {
        context.runOnClient(client -> { PortalRenderer.config().recursionDepth=2; PortalRenderer.config().maxRenderPasses=6; PortalRenderer.config().adaptiveRecursion=false; });
        var server = world.getServer();
        server.runCommand("fill -4 79 -4 4 79 55 stone");
        server.runCommand("time set day");
        server.runCommand("fill -2 80 18 2 83 18 white_concrete");
        server.runCommand("fill -2 80 40 2 83 40 white_concrete");
        server.runCommand("fill -2 80 28 2 82 28 gold_block");
        server.runCommand("setblock -1 80 35 chest");
        server.runCommand("setblock -1 80 36 water");
        server.runCommand("tp @a 0.5 80 15.0 0 0");
        context.waitFor(client -> Math.abs(client.player.getZ() - 15) < 0.05 && client.player.onGround(), 120);
        context.getInput().lookAt(0, 0);
        context.runOnClient(client -> ClientPortals.fire(false));
        context.waitFor(client -> ClientPortals.frames().size() == 1, 120);
        server.runCommand("tp @a 0.5 80 37.0 0 0");
        context.waitFor(client -> Math.abs(client.player.getZ() - 37) < 0.05, 120);
        context.getInput().lookAt(0, 0); context.waitTicks(6);
        context.runOnClient(client -> ClientPortals.fire(true));
        context.waitFor(client -> ClientPortals.frames().size() == 2, 120);
        require(server.computeOnServer(s -> PortalServer.frames().size()) == 2, "Portal pair was not authoritative");
        server.runCommand("tp @a 0.5 80 15.0 0 0");
        context.waitFor(client -> Math.abs(client.player.getZ() - 15) < 0.05, 120);
        context.getInput().lookAt(0, 0); context.waitTicks(15);
        context.takeScreenshot("phase-3-linked-wall-view");
        require(context.computeOnClient(client -> PortalRenderer.lastPassCount) > 0, "Destination portal render never ran");
        context.getInput().holdKey(options -> options.keyUp);
        try { context.waitFor(client -> client.player.getZ() > 25, 160); }
        finally { context.getInput().releaseKey(options -> options.keyUp); }
        context.waitTicks(10);
        double cz = context.computeOnClient(client -> client.player.getZ());
        double sz = server.computeOnServer(s -> world.getConnection().getServerPlayer().getZ());
        require(sz > 25 && Math.abs(cz - sz) < 0.2, "Wall traversal failed to converge: client=" + cz + ", server=" + sz);
        float heading = context.computeOnClient(client -> client.player.getYRot());
        require(Math.abs(heading) > 170, "Exit heading did not rotate: " + heading + ", server=" + server.computeOnServer(s -> world.getConnection().getServerPlayer().getYRot()));
        context.takeScreenshot("phase-3-after-traversal");
        // Blocks behind the entrance must not steal the remaining travel or momentum.
        server.runCommand("fill -2 80 19 2 83 22 stone");
        server.runCommand("tp @a 0.5 80 17.6 0 0"); context.waitFor(client -> Math.abs(client.player.getZ()-17.6)<.05,120);
        context.waitTicks(6);
        server.runOnServer(s -> world.getConnection().getServerPlayer().setDeltaMovement(new net.minecraft.world.phys.Vec3(0,0,2)));
        context.runOnClient(client -> client.player.setDeltaMovement(new net.minecraft.world.phys.Vec3(0,0,2)));
        context.waitFor(client -> client.player.getZ()>25,120);
        require(context.computeOnClient(client -> client.player.getDeltaMovement().z)<-.5, "Thick wall swallowed portal momentum");
        server.runCommand("tp @a 3.5 80 36.0 180 0"); context.waitFor(client -> Math.abs(client.player.getX()-3.5)<.05,120);
        server.runCommand("summon item 0.5 80.4 17.0 {Item:{id:\"minecraft:stone\",count:1},PickupDelay:32767s,Motion:[0.0d,0.0d,0.6d],NoGravity:1b,Tags:[\"portal_item\"]}");
        server.runCommand("summon chicken 0.5 80.0 17.0 {NoGravity:1b,Motion:[0.0d,0.0d,1.0d],Tags:[\"portal_mob\"]}");
        server.runCommand("summon arrow 0.5 81.5 17.0 {Motion:[0.0d,0.0d,0.8d],NoGravity:1b,Tags:[\"portal_arrow\"]}");
        context.waitTicks(12);
        server.runOnServer(s -> {
            var level = world.getConnection().getServerPlayer().level();
            for (String tag : java.util.List.of("portal_item", "portal_mob", "portal_arrow")) {
                var found = level.getEntities((net.minecraft.world.entity.Entity)null, new net.minecraft.world.phys.AABB(-2,79,0,2,84,55), e -> e.entityTags().contains(tag));
                require(found.size() == 1 && found.getFirst().getZ() > 25, "Native " + tag + " failed traversal: " + found.stream().map(e -> e.position().toString()).toList());
                if (tag.equals("portal_arrow")) require(found.getFirst().getDeltaMovement().z < -.3, "Arrow lost portal momentum in a thick wall");
            }
        });
        server.runCommand("summon snowball 0.5 81.5 17.0 {Motion:[0.0d,0.0d,2.0d],NoGravity:1b,Tags:[\"portal_snowball\"]}");
        context.waitTicks(2);
        server.runOnServer(s -> {
            var found = world.getConnection().getServerPlayer().level().getEntities((net.minecraft.world.entity.Entity)null,
                new net.minecraft.world.phys.AABB(-2,79,0,2,84,55), e -> e.entityTags().contains("portal_snowball"));
            require(found.size()==1 && found.getFirst().getZ()>25 && found.getFirst().getDeltaMovement().z<-.5, "Throwable projectile failed traversal/momentum");
        });
        server.runCommand("setblock 0 81 37 iron_block");
        server.runCommand("summon snowball 0.5 81.5 17.0 {Motion:[0.0d,0.0d,10.0d],NoGravity:1b,Tags:[\"portal_impact\"]}");
        context.waitTicks(2);
        server.runOnServer(s -> require(world.getConnection().getServerPlayer().level().getEntities((net.minecraft.world.entity.Entity)null,
            new net.minecraft.world.phys.AABB(-2,79,0,2,84,55), e -> e.entityTags().contains("portal_impact")).isEmpty(), "Projectile skipped an obstacle at its destination"));
        server.runCommand("setblock 0 81 37 air");
        server.runCommand("fill -2 80 19 2 83 22 air");
        // A non-tagged full block must fizzle without replacing a working portal.
        long blueId = server.computeOnServer(s -> PortalServer.frames().stream().filter(p -> !p.orange()).findFirst().orElseThrow().id());
        server.runCommand("tp @a 0.5 80 25.0 0 0"); context.waitFor(client -> Math.abs(client.player.getZ()-25) < .05, 120);
        context.getInput().lookAt(0,0); context.waitTicks(6); context.runOnClient(client -> ClientPortals.fire(false)); context.waitTicks(8);
        require(server.computeOnServer(s -> PortalServer.frames().stream().filter(p -> !p.orange()).findFirst().orElseThrow().id()) == blueId, "Invalid surface replaced the blue portal");
        // Floor-to-wall fling: acquire a floor portal and drop onto its aperture center.
        server.runCommand("tp @a 0.5 80 9.5 0 90"); context.waitFor(client -> Math.abs(client.player.getZ()-9.5) < .05,120);
        context.getInput().lookAt(0,90); context.waitTicks(6); context.runOnClient(client -> ClientPortals.fire(false));
        context.waitFor(client -> ClientPortals.frames().stream().anyMatch(p -> !p.orange() && p.normal().y() == 1),120);
        server.runCommand("tp @a 0.5 88 10.0 0 90"); context.waitFor(client -> client.player.getY()>87,120);
        context.waitFor(client -> client.player.getZ()>25,200);
        require(context.computeOnClient(client -> Math.abs(client.player.getDeltaMovement().z)) > .4, "Floor fling lost falling momentum");
        context.takeScreenshot("phase-3-floor-fling");
        // Ceiling-to-wall: jump through a low ceiling aperture.
        server.runCommand("fill -1 82 10 1 82 13 white_concrete");
        server.runCommand("tp @a 0.5 80 12.5 0 -90"); context.waitFor(client -> Math.abs(client.player.getZ()-12.5)<.05,120);
        context.getInput().lookAt(0,-90); context.waitTicks(6); context.runOnClient(client -> ClientPortals.fire(false));
        context.waitFor(client -> ClientPortals.frames().stream().anyMatch(p -> !p.orange() && p.normal().y() == -1),120);
        server.runCommand("tp @a 0.5 80 13.0 0 -90"); context.waitFor(client -> Math.abs(client.player.getZ()-13)<.05,120);
        context.getInput().holdKey(options -> options.keyJump);
        try { context.waitFor(client -> client.player.getZ()>25,160); }
        finally { context.getInput().releaseKey(options -> options.keyJump); }
        context.takeScreenshot("phase-3-ceiling-exit");
        // Recreate the original wall portal before checking support removal.
        server.runCommand("tp @a 0.5 80 15.0 0 0"); context.waitFor(client -> Math.abs(client.player.getZ()-15)<.05,120);
        context.getInput().lookAt(0,0); context.waitTicks(6); context.runOnClient(client -> ClientPortals.fire(false));
        context.waitFor(client -> ClientPortals.frames().stream().anyMatch(p -> !p.orange() && p.normal().z() == -1),120);
        server.runCommand("fill -2 80 0 2 83 0 white_concrete");
        server.runCommand("tp @a 0.5 80 3.0 180 0"); context.waitFor(client -> Math.abs(client.player.getZ()-3)<.05,120);
        context.getInput().lookAt(180,0); context.waitTicks(6); context.runOnClient(client -> ClientPortals.fire(true));
        context.waitFor(client -> ClientPortals.frames().stream().anyMatch(p -> p.orange() && p.normal().z()==1),120);
        server.runCommand("tp @a 0.5 80 15.0 0 0"); context.waitFor(client -> Math.abs(client.player.getZ()-15)<.05,120);
        context.getInput().lookAt(0,0); context.waitTicks(12);
        require(context.computeOnClient(client -> PortalRenderer.lastPassCount)>1, "Facing portals did not render recursive destination views");
        context.takeScreenshot("phase-3-recursive-views");
        // A destination well outside the normal client cache must still have chunks and entities.
        server.runOnServer(s -> { var level = world.getConnection().getServerPlayer().level(); for (int x=-1;x<=0;x++) for(int z=30;z<=31;z++) level.getChunk(x,z); });
        server.runCommand("fill -4 80 490 4 84 510 air");
        server.runCommand("fill -4 79 490 4 79 510 stone");
        server.runCommand("fill -2 80 500 2 83 500 white_concrete");
        server.runCommand("fill -2 80 490 2 82 490 diamond_block");
        server.runCommand("tp @a 0.5 80 497.0 0 0"); context.waitFor(client -> Math.abs(client.player.getZ()-497)<.05,200);
        context.getInput().lookAt(0,0); context.waitTicks(6); context.runOnClient(client -> ClientPortals.fire(true));
        context.waitFor(client -> ClientPortals.frames().stream().anyMatch(p -> p.orange() && p.center().z()>400),160);
        server.runCommand("summon chicken 0.5 80 496.0 {NoAI:1b,Tags:[\"portal_remote\"]}");
        int remoteId = server.computeOnServer(s -> world.getConnection().getServerPlayer().level().getEntities((net.minecraft.world.entity.Entity)null,
            new net.minecraft.world.phys.AABB(-2,79,492,2,84,500), e -> e.entityTags().contains("portal_remote")).getFirst().getId());
        server.runCommand("tp @a 0.5 80 15.0 0 0"); context.waitFor(client -> Math.abs(client.player.getZ()-15)<.05,120);
        context.getInput().lookAt(0,0);
        context.waitFor(client -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(new net.minecraft.core.BlockPos(0,80,500)).getBlock()).toString().equals("minecraft:white_concrete")
                && client.level.getEntity(remoteId) != null,200);
        require(context.computeOnClient(client -> !client.level.getChunkSource().storage.inRange(0,31)), "Remote destination test was within native view range");
        context.waitTicks(12); context.takeScreenshot("phase-3-remote-destination");
        context.getInput().holdKey(options -> options.keyUp);
        try { context.waitFor(client -> client.player.getZ()>400,160); }
        finally { context.getInput().releaseKey(options -> options.keyUp); }
        context.waitTicks(6);
        require(server.computeOnServer(s -> world.getConnection().getServerPlayer().getZ())>400, "Remote traversal did not reach authoritative destination");
        server.runCommand("setblock 0 80 18 air");
        context.waitFor(client -> ClientPortals.frames().size() == 1, 120);
        require(server.computeOnServer(s -> PortalServer.frames().size()) == 1, "Breaking support did not remove portal");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
