package dev.portalmod.portal;

import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.movement.Vector;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared aperture/collision contract. No client classes in this source set. */
public final class PortalWorld {
    private static Supplier<List<PortalFrame>> client = List::of;
    private static Supplier<PortalConfig> clientConfig = PortalConfig::get;
    private PortalWorld() { }
    public static void installClient(Supplier<List<PortalFrame>> frames, Supplier<PortalConfig> config) { client = frames; clientConfig = config; }
    public static PortalConfig config(Level level) { return level.isClientSide() ? clientConfig.get() : PortalConfig.get(); }
    public static List<PortalFrame> frames(Level level) { return level.isClientSide() ? client.get() : PortalServer.frames(); }
    public static PortalFrame partner(Level level, PortalFrame entry) {
        return frames(level).stream().filter(p -> p.owner().equals(entry.owner()) && p.orange() != entry.orange()
                && p.dimension().equals(entry.dimension())).findFirst().orElse(null);
    }
    public static double extent(AABB box, Vector axis) {
        return (box.getXsize() * Math.abs(axis.x()) + box.getYsize() * Math.abs(axis.y()) + box.getZsize() * Math.abs(axis.z())) / 2;
    }
    public static boolean ignoresShape(Level level, Entity entity, AABB box, VoxelShape shape) {
        if (entity == null || !config(level).enabled || shape.isEmpty() || entity.isPassenger() || entity.isVehicle()) return false;
        AABB hull = entity.getBoundingBox();
        Vector center = MinecraftCollisionWorld.fromMinecraft(hull.getCenter());
        Vector sweptCenter = MinecraftCollisionWorld.fromMinecraft(box.getCenter());
        for (PortalFrame p : frames(level)) {
            if (!p.dimension().equals(level.dimension().identifier().toString()) || partner(level, p) == null
                    || p.distance(center) < -1e-7
                    || Math.abs(p.distance(sweptCenter)) > extent(box, p.normal()) + config(level).collisionMargin
                    || !p.contains(center, extent(hull, p.right()), extent(hull, p.up()))) continue;
            AABB s = shape.bounds();
            for (PortalFrame.Cell c : p.support()) {
                if (s.minX >= c.x() - 1e-7 && s.maxX <= c.x() + 1.0000001
                        && s.minY >= c.y() - 1e-7 && s.maxY <= c.y() + 1.0000001
                        && s.minZ >= c.z() - 1e-7 && s.maxZ <= c.z() + 1.0000001) return true;
            }
        }
        return false;
    }
}
