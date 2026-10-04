package dev.portalmod.client.portal;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import dev.portalmod.PortalMod;
import dev.portalmod.movement.MinecraftCollisionWorld;
import dev.portalmod.movement.Vector;
import dev.portalmod.portal.PortalFrame;
import dev.portalmod.portal.PortalWorld;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Destination scene passes avoid re-entering Minecraft's mutable primary LevelRenderer.
 * Each pass uses a rigid portal camera, an oblique reverse-Z near plane and screen-space compositing. */
public final class PortalRenderer {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("portalmod", "runtime/white");
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET)
        .withLocation(Identifier.parse("portalmod:pipeline/portal_view")).withVertexShader(Identifier.parse("portalmod:core/portal_view"))
        .withFragmentShader(Identifier.parse("portalmod:core/portal_view")).withColorTargetState(ColorTargetState.DEFAULT).withCull(false).build());
    private static final List<Slot> slots = new ArrayList<>();
    private static final Map<Identifier, RenderType> types = new HashMap<>();
    private static final Map<Long, Scene> scenes = new HashMap<>();
    private static List<Draw> mainDraws = List.of();
    private static Matrix4f mainProjection = new Matrix4f();
    private static PortalViewConfig config;
    private static int budget;
    private static boolean whiteReady, failed;
    private static ProjectionMatrixBuffer projection;
    public static int lastPassCount;
    private record Draw(PortalFrame frame, Identifier texture) { }
    private record Block(BlockPos position, List<BlockStateModelPart> parts, int[] tint, int light) { }
    private record Scene(long tick, List<Block> blocks, List<BlockPos> blockEntities, List<BlockPos> fluids) { }
    private PortalRenderer() { }
    public static void initialize() { config = PortalViewConfig.load(); }
    public static Matrix4f captureProjection(Matrix4f matrix) { mainProjection.set(matrix); return matrix; }
    private static RenderType type(Identifier id) {
        return types.computeIfAbsent(id, key -> RenderType.create("portalmod_view_" + key, RenderSetup.builder(PIPELINE).withTexture("Sampler0", key).createRenderSetup()));
    }
    public static void prepare() {
        mainDraws = List.of(); lastPassCount = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !config.enabled || !ClientPortals.config().enabled || failed || ClientPortals.frames().isEmpty()) return;
        try {
            if (!whiteReady) {
                NativeImage image = new NativeImage(1, 1, false); image.setPixel(0, 0, 0xffffffff);
                mc.getTextureManager().register(WHITE, new DynamicTexture(() -> "Portal original solid pixel", image)); whiteReady = true;
            }
            if (projection == null) projection = new ProjectionMatrixBuffer("Portal views");
            budget = 0;
            scenes.keySet().removeIf(id -> ClientPortals.frames().stream().noneMatch(frame -> frame.id() == id));
            CameraRenderState camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            mainDraws = views(camera, mainProjection, config.recursionDepth, -1);
            lastPassCount = budget;
        } catch (Exception e) {
            failed = true; PortalMod.LOGGER.error("Portal destination rendering disabled after failure; traversal remains available", e);
        }
    }
    private static List<Draw> views(CameraRenderState camera, Matrix4f perspective, int depth, long excluded) {
        var mc = Minecraft.getInstance(); List<Draw> draws = new ArrayList<>();
        Vector eye = MinecraftCollisionWorld.fromMinecraft(camera.pos);
        for (PortalFrame entry : ClientPortals.frames()) {
            if (entry.id() == excluded || !entry.dimension().equals(mc.level.dimension().identifier().toString()) || entry.distance(eye) <= 0) continue;
            Vector relative = entry.center().subtract(eye);
            if (relative.lengthSquared() > config.maxViewDistance * config.maxViewDistance) continue;
            Vector3f inView = camera.viewRotationMatrix.transformDirection(new Vector3f((float) relative.x(), (float) relative.y(), (float) relative.z()));
            if (inView.z > 2) continue;
            PortalFrame exit = PortalWorld.partner(mc.level, entry);
            Identifier texture = WHITE;
            if (exit != null && depth > 0 && budget < config.maxRenderPasses) {
                int index = budget++;
                Slot slot = slot(index);
                CameraRenderState mapped = mapCamera(camera, entry, exit);
                Matrix4f clipped = clip(perspective, mapped, exit);
                List<Draw> children = views(mapped, perspective, depth - 1, exit.id());
                SubmitNodeStorage nodes = new SubmitNodeStorage();
                scene(nodes, mapped, exit);
                surfaces(nodes, mapped, children);
                draw(nodes, slot.target, mapped, clipped, true);
                texture = slot.id;
            }
            draws.add(new Draw(entry, texture));
        }
        return draws;
    }
    private static CameraRenderState mapCamera(CameraRenderState source, PortalFrame entry, PortalFrame exit) {
        CameraRenderState result = new CameraRenderState();
        result.pos = MinecraftCollisionWorld.toMinecraft(entry.pointTo(exit, MinecraftCollisionWorld.fromMinecraft(source.pos)));
        Vector x = entry.rotateTo(exit, new Vector(1, 0, 0)), y = entry.rotateTo(exit, new Vector(0, 1, 0)), z = entry.rotateTo(exit, new Vector(0, 0, 1));
        Matrix3f rotation = new Matrix3f().setColumn(0, new Vector3f((float)x.x(), (float)x.y(), (float)x.z()))
            .setColumn(1, new Vector3f((float)y.x(), (float)y.y(), (float)y.z())).setColumn(2, new Vector3f((float)z.x(), (float)z.y(), (float)z.z()));
        result.orientation = new Quaternionf().setFromNormalized(rotation).mul(source.orientation);
        result.viewRotationMatrix.rotation(new Quaternionf(result.orientation).conjugate());
        result.initialized = true; result.isFirstPerson = false;
        result.projectionMatrix.set(source.projectionMatrix); result.cameraEntityPartialTicks = source.cameraEntityPartialTicks;
        result.fogData = source.fogData;
        return result;
    }
    /** Reverse-Z clipping: replace the row for w-z=0; keep the far corner fixed. */
    private static Matrix4f clip(Matrix4f base, CameraRenderState camera, PortalFrame exit) {
        Vector n = exit.normal();
        Vector3f normal = camera.viewRotationMatrix.transformDirection(new Vector3f((float)n.x(), (float)n.y(), (float)n.z()));
        float distance = (float) exit.distance(MinecraftCollisionWorld.fromMinecraft(camera.pos));
        Vector4f plane = new Vector4f(normal, distance - (float)ClientPortals.config().exitEpsilon);
        boolean zeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
        Vector4f q = new Matrix4f(base).invert().transform(new Vector4f(Math.copySign(1f, plane.x), Math.copySign(1f, plane.y), zeroToOne ? 0 : -1, 1));
        float dot = plane.dot(q);
        if (Math.abs(dot) < 1.0e-6) return new Matrix4f(base);
        plane.mul((zeroToOne ? 1f : 2f) / dot);
        Matrix4f result = new Matrix4f(base);
        result.setRow(2, base.getRow(3, new Vector4f()).sub(plane));
        return result;
    }
    private static Scene buildScene(PortalFrame exit) {
        var mc = Minecraft.getInstance(); var level = mc.level;
        List<Block> blocks = new ArrayList<>(); List<BlockPos> blockEntities = new ArrayList<>(), fluids = new ArrayList<>(); BlockPos origin = BlockPos.containing(MinecraftCollisionWorld.toMinecraft(exit.center()));
        RandomSource random = RandomSource.create(0);
        // Scan from the aperture outward, so the cap drops distant geometry first.
        outer: for (int shell = 0; shell <= config.sceneRadius; shell++) for (int x = -shell; x <= shell; x++) for (int y = -shell; y <= shell; y++) for (int z = -shell; z <= shell; z++) {
            if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) != shell) continue;
            BlockPos pos = origin.offset(x, y, z); var state = level.getBlockState(pos);
            if (state.hasBlockEntity() && blockEntities.size() < config.maxSceneBlockEntities) blockEntities.add(pos.immutable());
            if (!state.getFluidState().isEmpty() && fluids.size() < config.maxSceneFluidBlocks) fluids.add(pos.immutable());
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            boolean exposed = false;
            for (Direction direction : Direction.values()) if (!level.getBlockState(pos.relative(direction)).isSolidRender()) { exposed = true; break; }
            if (!exposed) continue;
            List<BlockStateModelPart> parts = new ArrayList<>();
            random.setSeed(state.getSeed(pos)); mc.getModelManager().getBlockStateModelSet().get(state).collectParts(random, parts);
            int[] tint = new int[4]; for (int i = 0; i < tint.length; i++) {
                var source = mc.getBlockColors().getTintSource(state, i);
                tint[i] = source == null ? -1 : 0xff000000 | source.colorInWorld(state, level, pos);
            }
            int blockLight = level.getBrightness(LightLayer.BLOCK, pos), skyLight = level.getBrightness(LightLayer.SKY, pos);
            for (Direction face : Direction.values()) {
                blockLight = Math.max(blockLight, level.getBrightness(LightLayer.BLOCK, pos.relative(face)));
                skyLight = Math.max(skyLight, level.getBrightness(LightLayer.SKY, pos.relative(face)));
            }
            int light = net.minecraft.util.LightCoordsUtil.pack(blockLight, skyLight);
            blocks.add(new Block(pos.immutable(), List.copyOf(parts), tint, light));
            if (blocks.size() >= config.maxSceneBlocks) break outer;
        }
        return new Scene(level.getGameTime(), List.copyOf(blocks), List.copyOf(blockEntities), List.copyOf(fluids));
    }
    private static void scene(SubmitNodeStorage nodes, CameraRenderState camera, PortalFrame exit) {
        var mc = Minecraft.getInstance();
        Scene cached = scenes.get(exit.id());
        if (cached == null || mc.level.getGameTime() - cached.tick >= config.sceneRefreshTicks) { cached = buildScene(exit); scenes.put(exit.id(), cached); }
        PoseStack pose = new PoseStack();
        for (Block block : cached.blocks) {
            pose.pushPose(); pose.translate(block.position.getX() - camera.pos.x, block.position.getY() - camera.pos.y, block.position.getZ() - camera.pos.z);
            nodes.submitBlockModel(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), block.parts, block.tint, block.light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        var blockEntityDispatcher = mc.getBlockEntityRenderDispatcher();
        blockEntityDispatcher.prepare(camera.pos);
        try {
            for (BlockPos pos : cached.blockEntities) {
                var entity = mc.level.getBlockEntity(pos); if (entity == null) continue;
                var state = blockEntityDispatcher.tryExtractRenderState(entity, camera.cameraEntityPartialTicks, null, false);
                if (state == null) continue;
                pose.pushPose(); pose.translate(pos.getX()-camera.pos.x, pos.getY()-camera.pos.y, pos.getZ()-camera.pos.z);
                blockEntityDispatcher.submit(state, pose, nodes, camera); pose.popPose();
            }
        } finally { blockEntityDispatcher.prepare(mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos); }
        for (BlockPos pos : cached.fluids) {
            pose.pushPose(); pose.translate((pos.getX() & ~15)-camera.pos.x, (pos.getY() & ~15)-camera.pos.y, (pos.getZ() & ~15)-camera.pos.z);
            nodes.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (saved, consumer) -> {
                var state = mc.level.getBlockState(pos);
                new FluidRenderer(mc.getModelManager().getFluidStateModelSet()).tesselate(mc.level, pos, layer -> new PosedConsumer(saved, consumer), state, state.getFluidState());
            });
            pose.popPose();
        }
        int count = 0;
        Vec3 center = MinecraftCollisionWorld.toMinecraft(exit.center());
        for (var entity : mc.level.entitiesForRendering()) {
            if (count >= config.maxSceneEntities) break;
            if (entity.position().distanceToSqr(center) > config.sceneRadius * config.sceneRadius) continue;
            var state = mc.getEntityRenderDispatcher().extractEntity(entity, camera.cameraEntityPartialTicks);
            mc.getEntityRenderDispatcher().submit(state, camera, state.x - camera.pos.x, state.y - camera.pos.y, state.z - camera.pos.z, pose, nodes); count++;
        }
    }
    private static void surfaces(SubmitNodeStorage nodes, CameraRenderState camera, List<Draw> draws) {
        for (Draw draw : draws) {
            int color = draw.frame.orange() ? 0xffff8c17 : 0xff2aa9ff;
            quad(nodes, camera, draw.frame, draw.texture, -0.5, -1, 0.5, 1, draw.texture.equals(WHITE) ? color : 0xffffffff);
            double w = config.rimWidth;
            quad(nodes, camera, draw.frame, WHITE, -0.5-w, -1-w, -0.5, 1+w, color);
            quad(nodes, camera, draw.frame, WHITE, 0.5, -1-w, 0.5+w, 1+w, color);
            quad(nodes, camera, draw.frame, WHITE, -0.5, -1-w, 0.5, -1, color);
            quad(nodes, camera, draw.frame, WHITE, -0.5, 1, 0.5, 1+w, color);
        }
    }
    private static void quad(SubmitNodeStorage nodes, CameraRenderState camera, PortalFrame frame, Identifier texture, double left, double bottom, double right, double top, int color) {
        Vector center = frame.center().subtract(MinecraftCollisionWorld.fromMinecraft(camera.pos)).add(frame.normal().scale(config.surfaceOffset));
        Vector[] vertices = {center.add(frame.right().scale(left)).add(frame.up().scale(bottom)), center.add(frame.right().scale(right)).add(frame.up().scale(bottom)),
            center.add(frame.right().scale(right)).add(frame.up().scale(top)), center.add(frame.right().scale(left)).add(frame.up().scale(top))};
        nodes.submitCustomGeometry(new PoseStack(), type(texture), (pose, consumer) -> {
            for (Vector v : vertices) consumer.addVertex(pose, (float)v.x(), (float)v.y(), (float)v.z()).setColor(color).setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose, (float)frame.normal().x(), (float)frame.normal().y(), (float)frame.normal().z());
        });
    }
    public static void composite() {
        if (mainDraws.isEmpty() || failed) return;
        var mc = Minecraft.getInstance(); CameraRenderState camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        try {
            SubmitNodeStorage nodes = new SubmitNodeStorage(); surfaces(nodes, camera, mainDraws);
            draw(nodes, mc.gameRenderer.mainRenderTarget(), camera, mainProjection, false);
        } catch (Exception e) { failed = true; PortalMod.LOGGER.error("Portal compositing disabled after failure", e); }
    }
    private static void draw(SubmitNodeStorage nodes, RenderTarget target, CameraRenderState camera, Matrix4f matrix, boolean clear) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(com.mojang.blaze3d.platform.Lighting.Entry.LEVEL);
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        if (clear) encoder.clearColorAndDepthTextures(target.getColorTexture(), camera.fogData.color, target.getDepthTexture(), 0);
        RenderSystem.backupProjectionMatrix();
        var stack = RenderSystem.getModelViewStack(); stack.pushMatrix(); stack.identity().mul(camera.viewRotationMatrix);
        try {
            RenderSystem.setProjectionMatrix(projection.getBuffer(matrix), ProjectionType.PERSPECTIVE);
            try (var frame = Minecraft.getInstance().gameRenderer.featureRenderDispatcher().prepareFrame(nodes);
                 var pass = encoder.createRenderPass(() -> "Portal destination", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(pass); FeatureRenderDispatcher.renderAllFeatures(pass, frame);
            }
        } finally { stack.popMatrix(); RenderSystem.restoreProjectionMatrix(); }
    }
    private static Slot slot(int index) {
        var window = Minecraft.getInstance().getWindow();
        int w = Math.max(64, (int)(window.getWidth() * config.resolutionScale)), h = Math.max(64, (int)(window.getHeight() * config.resolutionScale));
        double cap = Math.min(1, (double)config.maxResolution / Math.max(w, h)); w = (int)(w*cap); h = (int)(h*cap);
        while (slots.size() <= index) slots.add(new Slot(slots.size(), w, h));
        Slot slot = slots.get(index);
        if (slot.target.width != w || slot.target.height != h) { slot.target.resize(w, h); slot.update(); }
        return slot;
    }
    private static final class Slot {
        final Identifier id; final TextureTarget target;
        Slot(int index, int w, int h) {
            id = Identifier.fromNamespaceAndPath("portalmod", "runtime/view_" + index);
            target = new TextureTarget("Portal view " + index, w, h, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT); update();
        }
        void update() { Minecraft.getInstance().getTextureManager().register(id, new BorrowedTexture(target)); }
    }
    private static final class BorrowedTexture extends AbstractTexture {
        BorrowedTexture(RenderTarget target) {
            texture = target.getColorTexture(); textureView = target.getColorTextureView();
            sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);
        }
        @Override public void close() { /* RenderTarget owns GPU attachments. */ }
    }
    private record PosedConsumer(PoseStack.Pose pose, VertexConsumer delegate) implements VertexConsumer {
        public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(pose,x,y,z); return this; }
        public VertexConsumer setColor(int r,int g,int b,int a) { delegate.setColor(r,g,b,a); return this; }
        public VertexConsumer setColor(int color) { delegate.setColor(color); return this; }
        public VertexConsumer setUv(float u,float v) { delegate.setUv(u,v); return this; }
        public VertexConsumer setUv1(int u,int v) { delegate.setUv1(u,v); return this; }
        public VertexConsumer setUv2(int u,int v) { delegate.setUv2(u,v); return this; }
        public VertexConsumer setUv3(float u,float v) { delegate.setUv3(u,v); return this; }
        public VertexConsumer setNormal(float x,float y,float z) { delegate.setNormal(pose,x,y,z); return this; }
        public VertexConsumer setLineWidth(float w) { delegate.setLineWidth(w); return this; }
    }
    public static void close() {
        for (Slot slot : slots) { Minecraft.getInstance().getTextureManager().release(slot.id); slot.target.destroyBuffers(); }
        slots.clear(); scenes.clear(); types.clear(); mainDraws = List.of(); failed = false;
        if (projection != null) { projection.close(); projection = null; }
    }
}
