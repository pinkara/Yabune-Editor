package com.pinkara.ye.editor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pinkara.youma.math.AABBInt;
import com.mojang.logging.LogUtils;
import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.block.NGTObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BedRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Base64;

public class MeshExporter {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Set<BakedQuad> FALLBACK_QUADS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ResourceLocation, TextureAtlas> ATLAS_BY_TEXTURE = new HashMap<>();

    public enum Format {
        OBJ,
        STL,
        FBX
    }

    @FunctionalInterface
    private interface FaceVisibilityChecker {
        boolean isVisible(int x, int y, int z, Direction dir);
    }

    private static class MeshData {
        List<Vertex> vertices = new ArrayList<>();
        List<UV> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        Map<TextureAtlasSprite, Material> materials = new LinkedHashMap<>();
    }

    public static boolean export(AABBInt box, Level level, BlockRenderDispatcher renderer, Path path, Format format) {
        if (box == null || level == null || renderer == null) return false;
        try {
            Files.createDirectories(path.getParent());
            MeshData data = collectMeshData(box, level, renderer);
            switch (format) {
                case OBJ -> writeObj(data, path);
                case FBX -> writeFbx(data, path);
                case STL -> writeStl(data, path);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean export(NGTObject ngto, BlockRenderDispatcher renderer, Path path, Format format) {
        if (ngto == null || renderer == null) {
            LOGGER.error("export(NGTObject) called with null: ngto={}, renderer={}", ngto, renderer);
            return false;
        }
        try {
            Files.createDirectories(path.getParent());
            LOGGER.info("Exporting NGTObject size={}x{}x{} to {}", ngto.xSize, ngto.ySize, ngto.zSize, path);
            MeshData data = collectMeshData(ngto, renderer);
            LOGGER.info("Collected mesh: {} vertices, {} uvs, {} normals, {} faces, {} materials",
                    data.vertices.size(), data.uvs.size(), data.normals.size(), data.faces.size(), data.materials.size());
            switch (format) {
                case OBJ -> writeObj(data, path);
                case FBX -> writeFbx(data, path);
                case STL -> writeStl(data, path);
            }
            LOGGER.info("Export completed successfully: {}", path);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to export NGTObject to {}", path, e);
            return false;
        }
    }

    // Legacy server-side export (cubes without textures)
    public static boolean export(AABBInt box, Level level, Path path, Format format) {
        if (box == null || level == null) return false;
        try {
            Files.createDirectories(path.getParent());
            String content = format == Format.OBJ ? exportObjLegacy(box, level) : exportStlLegacy(box, level);
            Files.writeString(path, content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static MeshData collectMeshData(AABBInt box, Level level, BlockRenderDispatcher renderer) {
        MeshData data = new MeshData();
        RandomSource rand = RandomSource.create();
        for (int y = box.minY; y < box.maxY; ++y) {
            for (int z = box.minZ; z < box.maxZ; ++z) {
                for (int x = box.minX; x < box.maxX; ++x) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.isAir()) continue;
                    collectBlockQuads(x, y, z, state, renderer, rand, data, (dx, dy, dz, dir) -> isFaceVisible(box, level, dx, dy, dz, dir));
                }
            }
        }
        return data;
    }

    private static MeshData collectMeshData(NGTObject ngto, BlockRenderDispatcher renderer) {
        MeshData data = new MeshData();
        RandomSource rand = RandomSource.create();
        int blocks = 0;
        for (int y = 0; y < ngto.ySize; ++y) {
            for (int z = 0; z < ngto.zSize; ++z) {
                for (int x = 0; x < ngto.xSize; ++x) {
                    BlockSet bs = ngto.getBlockSet(x, y, z);
                    if (bs == null || bs.state == null || bs.state.isAir()) continue;
                    blocks++;
                    try {
                        collectBlockQuads(x, y, z, bs.state, renderer, rand, data, (dx, dy, dz, dir) -> isFaceVisible(ngto, dx, dy, dz, dir));
                    } catch (Exception e) {
                        LOGGER.error("Failed to collect quads at {},{},{} for state {}", x, y, z, bs.state, e);
                    }
                }
            }
        }
        LOGGER.info("collectMeshData processed {} non-air blocks", blocks);
        return data;
    }

    private static void collectBlockQuads(int x, int y, int z, BlockState state, BlockRenderDispatcher renderer, RandomSource rand, MeshData data, FaceVisibilityChecker checker) {
        var model = renderer.getBlockModel(state);
        List<BlockModelPart> parts = model.collectParts(rand);
        List<BakedQuad> quads = new ArrayList<>();
        int[] counts = new int[7]; // 6 directions + null
        for (BlockModelPart part : parts) {
            for (Direction dir : Direction.values()) {
                int before = quads.size();
                quads.addAll(part.getQuads(dir));
                counts[dir.ordinal()] += quads.size() - before;
            }
            int before = quads.size();
            quads.addAll(part.getQuads(null));
            counts[6] += quads.size() - before;
        }

        if (quads.isEmpty()) {
            LOGGER.warn("No quads collected for state {} at {},{},{} (model={}, parts={})", state, x, y, z, model.getClass().getSimpleName(), parts.size());
            List<BakedQuad> fallback = renderFallbackQuads(state, renderer, rand);
            if (!fallback.isEmpty()) {
                LOGGER.info("Fallback quads for state {} at {},{},{}: {}", state, x, y, z, fallback.size());
                quads.addAll(fallback);
            }
        } else if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
            LOGGER.info("Bed quads at {},{},{}: state={}, parts={}, counts={}", x, y, z, state, parts.size(), counts);
        }

        for (BakedQuad quad : quads) {
            int[] vData = quad.vertices();
            int stride = vData.length / 4;

            TextureAtlasSprite sprite = quad.sprite();
            boolean fallbackQuad = FALLBACK_QUADS.contains(quad);
            if (fallbackQuad) {
                float firstAtlasU = Float.intBitsToFloat(vData[4]);
                float firstAtlasV = Float.intBitsToFloat(vData[5]);
                TextureAtlasSprite realSprite = findSpriteForFallbackUV(sprite, firstAtlasU, firstAtlasV);
                if (realSprite != null) {
                    sprite = realSprite;
                }
            }

            Material mat = data.materials.computeIfAbsent(sprite, Material::new);
            int tintIndex = quad.tintIndex();
            if (tintIndex != -1 && mat.tintColor == -1) {
                mat.tintColor = getBiomeTint(state, tintIndex);
            }

            Vertex[] qv = new Vertex[4];
            UV[] quv = new UV[4];
            float su0 = sprite.getU0();
            float su1 = sprite.getU1();
            float sv0 = sprite.getV0();
            float sv1 = sprite.getV1();
            float suRange = su1 - su0;
            float svRange = sv1 - sv0;
            for (int i = 0; i < 4; i++) {
                int off = i * stride;
                float vx = Float.intBitsToFloat(vData[off]) + x;
                float vy = Float.intBitsToFloat(vData[off + 1]) + y;
                float vz = Float.intBitsToFloat(vData[off + 2]) + z;
                qv[i] = new Vertex(vx, vy, vz);
                float atlasU = Float.intBitsToFloat(vData[off + 4]);
                float atlasV = Float.intBitsToFloat(vData[off + 5]);
                float u = suRange > 0.0001f ? (atlasU - su0) / suRange : atlasU;
                float v = svRange > 0.0001f ? (atlasV - sv0) / svRange : atlasV;
                quv[i] = new UV(u, 1.0f - v); // flip V so 0 is bottom
            }
            Vector3f n = computeNormal(qv[0], qv[1], qv[2]);
            data.normals.add(n);
            int normalIdx = data.normals.size();

            int[] idx = new int[4];
            int[] uvidx = new int[4];
            for (int i = 0; i < 4; i++) {
                data.vertices.add(qv[i]);
                data.uvs.add(quv[i]);
                idx[i] = data.vertices.size();
                uvidx[i] = data.uvs.size();
            }
            data.faces.add(new Face(mat, idx, uvidx, normalIdx));
        }
    }

    private static List<BakedQuad> renderFallbackQuads(BlockState state, BlockRenderDispatcher renderer, RandomSource rand) {
        List<BakedQuad> result = new ArrayList<>();
        BlockPos pos = new BlockPos(0, 0, 0);
        SimpleBlockAndTintGetter level = new SimpleBlockAndTintGetter(state, pos);

        var model = renderer.getBlockModel(state);
        List<BlockModelPart> parts = new ArrayList<>();
        model.collectParts(level, pos, state, rand, parts);
        int partQuadCount = 0;
        for (BlockModelPart part : parts) {
            for (Direction dir : Direction.values()) partQuadCount += part.getQuads(dir).size();
            partQuadCount += part.getQuads(null).size();
        }
        LOGGER.info("Fallback collectParts for {}: parts={}, quads={}", state, parts.size(), partQuadCount);

        PoseStack poseStack = new PoseStack();
        QuadBuffer buffer = new QuadBuffer();
        VertexConsumer capture = buffer.getBuffer(RenderType.cutout());
        CaptureSubmitNodeCollector collector = new CaptureSubmitNodeCollector(capture);
        if (!parts.isEmpty()) {
            try {
                renderer.getModelRenderer().tesselateBlock(level, parts, state, pos, poseStack, (ChunkSectionLayer type) -> capture, false, OverlayTexture.NO_OVERLAY);
            } catch (Exception e) {
                LOGGER.error("tesselateBlock failed for {}", state, e);
            }
        }
        if (buffer.getVertices().isEmpty()) {
            LOGGER.warn("Fallback tesselateBlock produced no vertices for {}, trying special renderers", state);
            boolean rendered = renderSpecialBlock(state, poseStack, collector);
            if (!rendered) {
                LOGGER.warn("Special renderer produced no vertices for {}, trying renderBreakingTexture", state);
                try {
                    renderer.renderBreakingTexture(state, pos, level, poseStack, capture);
                } catch (Exception e) {
                    LOGGER.error("renderBreakingTexture failed for {}, trying renderSingleBlock", state, e);
                    buffer.getCapture().clear();
                    try {
                        renderer.renderSingleBlock(state, poseStack, buffer, 0xF000F0, 0, level, pos);
                    } catch (Exception e2) {
                        LOGGER.error("renderSingleBlock with fake level also failed for {}", state, e2);
                        return result;
                    }
                }
            }
        }
        List<Capture> verts = buffer.getVertices();
        if (verts.size() % 4 != 0) {
            LOGGER.warn("Fallback vertices not a multiple of 4: {}", verts.size());
        }
        TextureAtlasSprite sprite = collector.getSprite();
        if (sprite == null) sprite = renderer.getBlockModel(state).particleIcon();
        if (!verts.isEmpty()) {
            Capture v0 = verts.get(0);
            LOGGER.info("Fallback sprite for {}: {} atlas={} u0={} u1={} v0={} v1={} firstUv={},{} vertexCount={}",
                    state, sprite.contents().name(), sprite.atlasLocation(), sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), v0.u, v0.v, verts.size());
        }
        for (int i = 0; i + 3 < verts.size(); i += 4) {
            Capture v0 = verts.get(i);
            Direction dir = directionFromNormal(v0.nx, v0.ny, v0.nz);
            BakedQuad quad = new BakedQuad(buildVertexData(verts.subList(i, i + 4), sprite), -1, dir, sprite, false, 0);
            FALLBACK_QUADS.add(quad);
            result.add(quad);
        }
        return result;
    }

    private static boolean renderSpecialBlock(BlockState state, PoseStack poseStack, CaptureSubmitNodeCollector collector) {
        Block block = state.getBlock();
        if (!(block instanceof EntityBlock entityBlock)) return false;
        BlockEntity blockEntity = entityBlock.newBlockEntity(posForSpecial, state);
        if (blockEntity == null) return false;
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer;
        try {
            renderer = dispatcher.getRenderer(blockEntity);
        } catch (Exception e) {
            LOGGER.error("Failed to get BlockEntityRenderer for {}", state, e);
            return false;
        }
        if (renderer == null) return false;
        BlockEntityRenderState renderState = renderer.createRenderState();
        if (renderState instanceof BedRenderState bed) {
            bed.color = ((BedBlock) block).getColor();
            bed.facing = state.getValue(BedBlock.FACING);
            bed.isHead = state.getValue(BedBlock.PART) == BedPart.HEAD;
        } else if (renderState instanceof ChestRenderState chest) {
            chest.type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
            chest.angle = state.hasProperty(ChestBlock.FACING) ? state.getValue(ChestBlock.FACING).toYRot() : Direction.SOUTH.toYRot();
            chest.open = 0.0f;
            if (block == Blocks.ENDER_CHEST) {
                chest.material = ChestRenderState.ChestMaterialType.ENDER_CHEST;
            } else if (block instanceof TrappedChestBlock || block == Blocks.TRAPPED_CHEST) {
                chest.material = ChestRenderState.ChestMaterialType.TRAPPED;
            } else if (block instanceof ChestBlock) {
                chest.material = ChestRenderState.ChestMaterialType.REGULAR;
            }
        } else {
            LOGGER.info("BlockEntityRenderer renderState type {} not handled for {}, falling back to SpecialBlockModelRenderer", renderState.getClass().getSimpleName(), state);
            Minecraft.getInstance().getModelManager().specialBlockModelRenderer().get().renderByBlock(block, ItemDisplayContext.NONE, poseStack, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            return !collector.isEmpty();
        }
        try {
            renderer.submit(renderState, poseStack, collector, new CameraRenderState());
        } catch (Exception e) {
            LOGGER.error("BlockEntityRenderer submit failed for {}", state, e);
            return false;
        }
        return !collector.isEmpty();
    }

    private static final BlockPos posForSpecial = new BlockPos(0, 0, 0);

    private static Direction directionFromNormal(float x, float y, float z) {
        float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ax > ay && ax > az) return x > 0 ? Direction.EAST : Direction.WEST;
        if (ay > az) return y > 0 ? Direction.UP : Direction.DOWN;
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static int[] buildVertexData(List<Capture> verts, TextureAtlasSprite sprite) {
        int[] data = new int[32];
        for (int i = 0; i < 4; i++) {
            Capture v = verts.get(i);
            int off = i * 8;
            data[off] = Float.floatToRawIntBits(v.x);
            data[off + 1] = Float.floatToRawIntBits(v.y);
            data[off + 2] = Float.floatToRawIntBits(v.z);
            data[off + 3] = (v.a << 24) | (v.b << 16) | (v.g << 8) | v.r;
            data[off + 4] = Float.floatToRawIntBits(v.u);
            data[off + 5] = Float.floatToRawIntBits(v.v);
            data[off + 6] = 0;
            data[off + 7] = packNormal(v.nx, v.ny, v.nz);
        }
        return data;
    }

    private static TextureAtlasSprite findSpriteForFallbackUV(TextureAtlasSprite hint, float u, float v) {
        if (hint == null) return null;
        TextureAtlas atlas = getAtlasByTextureLocation(hint.atlasLocation());
        if (atlas == null) return null;
        for (TextureAtlasSprite sprite : atlas.getTextures().values()) {
            if (u >= sprite.getU0() - 1e-4f && u <= sprite.getU1() + 1e-4f &&
                v >= sprite.getV0() - 1e-4f && v <= sprite.getV1() + 1e-4f) {
                return sprite;
            }
        }
        return null;
    }

    private static TextureAtlas getAtlasByTextureLocation(ResourceLocation textureLocation) {
        if (textureLocation == null) return null;
        return ATLAS_BY_TEXTURE.computeIfAbsent(textureLocation, loc -> {
            AtlasManager manager = Minecraft.getInstance().getAtlasManager();
            final TextureAtlas[] found = {null};
            manager.forEach((id, atlas) -> {
                if (atlas.location().equals(loc)) found[0] = atlas;
            });
            return found[0];
        });
    }

    private static int packNormal(float x, float y, float z) {
        int nx = (int) Math.clamp(x * 127.0f, -128, 127);
        int ny = (int) Math.clamp(y * 127.0f, -128, 127);
        int nz = (int) Math.clamp(z * 127.0f, -128, 127);
        return ((nx & 0xFF) << 16) | ((ny & 0xFF) << 8) | (nz & 0xFF);
    }

    private static class CaptureSubmitNodeCollector implements SubmitNodeCollector {
        private final VertexConsumer capture;
        private TextureAtlasSprite sprite;
        private boolean used = false;

        CaptureSubmitNodeCollector(VertexConsumer capture) { this.capture = capture; }

        @Override
        public OrderedSubmitNodeCollector order(int i) { return this; }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int packedLight, int packedOverlay, int color, TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
            this.used = true;
            this.sprite = sprite;
            model.renderToBuffer(poseStack, capture, packedLight, packedOverlay, color);
        }

        @Override
        public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType, int packedLight, int packedOverlay, TextureAtlasSprite sprite, boolean bool1, boolean bool2, int color, ModelFeatureRenderer.CrumblingOverlay overlay, int outlineColor) {
            this.used = true;
            this.sprite = sprite;
            part.render(poseStack, capture, packedLight, packedOverlay, color);
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel model, float r, float g, float b, int packedLight, int packedOverlay, int color) {
            this.used = true;
            ModelBlockRenderer.renderModel(poseStack.last(), capture, model, r, g, b, packedLight, packedOverlay);
        }

        @Override public void submitBlock(PoseStack poseStack, BlockState state, int i, int j, int k) { this.used = true; }
        @Override public void submitHitbox(PoseStack poseStack, EntityRenderState state, HitboxesRenderState hitbox) { this.used = true; }
        @Override public void submitShadow(PoseStack poseStack, float f, List<EntityRenderState.ShadowPiece> list) { this.used = true; }
        @Override public void submitNameTag(PoseStack poseStack, Vec3 vec3, int i, Component component, boolean b, int j, double d, CameraRenderState camera) { this.used = true; }
        @Override public void submitText(PoseStack poseStack, float f, float g, FormattedCharSequence text, boolean b, Font.DisplayMode mode, int i, int j, int k, int l) { this.used = true; }
        @Override public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf quaternion) { this.used = true; }
        @Override public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leash) { this.used = true; }
        @Override public void submitItem(PoseStack poseStack, ItemDisplayContext ctx, int i, int j, int k, int[] ints, List<BakedQuad> quads, RenderType rt, ItemStackRenderState.FoilType foil) { this.used = true; }
        @Override public void submitCustomGeometry(PoseStack poseStack, RenderType rt, SubmitNodeCollector.CustomGeometryRenderer renderer) { this.used = true; }
        @Override public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state) { this.used = true; }
        @Override public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) { this.used = true; }

        public TextureAtlasSprite getSprite() { return sprite; }
        public boolean isEmpty() { return !used; }
    }

    private static class SimpleBlockAndTintGetter implements BlockAndTintGetter {
        private final BlockState state;
        private final BlockPos pos;
        SimpleBlockAndTintGetter(BlockState state, BlockPos pos) { this.state = state; this.pos = pos; }
        @Override public float getShade(Direction direction, boolean shade) { return 1.0F; }
        @Override public LevelLightEngine getLightEngine() { return LevelLightEngine.EMPTY; }
        @Override public int getBlockTint(BlockPos pos, net.minecraft.world.level.ColorResolver resolver) { return -1; }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
        @Override public BlockState getBlockState(BlockPos pos) { return pos.equals(this.pos) ? state : Blocks.AIR.defaultBlockState(); }
        @Override public FluidState getFluidState(BlockPos pos) { return Fluids.EMPTY.defaultFluidState(); }
        @Override public int getHeight() { return 384; }
        @Override public int getMinY() { return -64; }
    }

    private static class QuadBuffer implements MultiBufferSource {
        private final CaptureVertex vertex = new CaptureVertex();
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return vertex;
        }
        public List<Capture> getVertices() { return vertex.getVertices(); }
        public CaptureVertex getCapture() { return vertex; }
    }

    private static class CaptureVertex implements VertexConsumer {
        private final List<Capture> vertices = new ArrayList<>();
        private Capture current = new Capture();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (current.hasPos) vertices.add(current);
            current = new Capture();
            current.x = x; current.y = y; current.z = z;
            current.hasPos = true;
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { current.r = r; current.g = g; current.b = b; current.a = a; return this; }
        @Override public VertexConsumer setUv(float u, float v) { current.u = u; current.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { current.nx = x; current.ny = y; current.nz = z; return this; }

        public List<Capture> getVertices() {
            if (current.hasPos) vertices.add(current);
            return vertices;
        }
        public void clear() { vertices.clear(); current = new Capture(); }
    }

    private static class Capture {
        float x, y, z;
        int r = 255, g = 255, b = 255, a = 255;
        float u, v;
        float nx, ny, nz;
        boolean hasPos;
    }

    private static void writeObj(MeshData data, Path path) throws IOException {
        // Write OBJ
        String baseName = path.getFileName().toString();
        if (baseName.toLowerCase().endsWith(".obj")) baseName = baseName.substring(0, baseName.length() - 4);
        Path mtlPath = path.resolveSibling(baseName + ".mtl");

        StringBuilder obj = new StringBuilder();
        obj.append("# YE OBJ export\n");
        obj.append("# vertices: ").append(data.vertices.size()).append("\n");
        obj.append("mtllib ").append(mtlPath.getFileName().toString()).append("\n\n");

        for (Vertex v : data.vertices) obj.append("v ").append(v.x).append(' ').append(v.y).append(' ').append(v.z).append('\n');
        obj.append("\n");
        for (UV uv : data.uvs) obj.append("vt ").append(uv.u).append(' ').append(uv.v).append('\n');
        obj.append("\n");
        for (Vector3f n : data.normals) obj.append("vn ").append(n.x).append(' ').append(n.y).append(' ').append(n.z).append('\n');
        obj.append("\n");

        Material currentMat = null;
        for (Face f : data.faces) {
            if (currentMat != f.mat) {
                currentMat = f.mat;
                obj.append("usemtl ").append(currentMat.name).append("\n");
            }
            obj.append("f");
            for (int i = 0; i < 4; i++) {
                obj.append(' ').append(f.vIdx[i]).append('/').append(f.vtIdx[i]).append('/').append(f.vnIdx);
            }
            obj.append('\n');
        }

        Files.writeString(path, obj.toString());

        // Write textures and MTL
        StringBuilder mtl = new StringBuilder();
        for (Material mat : data.materials.values()) {
            Path texFile = path.resolveSibling(mat.fileName);
            saveTexture(mat.sprite, texFile, mat.tintColor);
            mtl.append("newmtl ").append(mat.name).append("\n");
            mtl.append("Ka 1.0 1.0 1.0\n");
            mtl.append("Kd 1.0 1.0 1.0\n");
            mtl.append("Ks 0.0 0.0 0.0\n");
            mtl.append("map_Kd ").append(mat.fileName).append("\n\n");
        }
        Files.writeString(mtlPath, mtl.toString());
    }

    private static void writeFbx(MeshData data, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("; FBX 7.7.0 project file\n");
        sb.append("; Created by Yabune Editor\n");
        sb.append("FBXHeaderExtension:  {\n");
        sb.append("    FBXHeaderVersion: 1003\n");
        sb.append("    FBXVersion: 7700\n");
        sb.append("    Creator: \"Yabune Editor\"\n");
        sb.append("}\n");
        sb.append("GlobalSettings:  {\n");
        sb.append("    Version: 1000\n");
        sb.append("    Properties70:  {\n");
        sb.append("        P: \"UpAxis\", \"int\", \"Integer\", \"\",1\n");
        sb.append("        P: \"UpAxisSign\", \"int\", \"Integer\", \"\",1\n");
        sb.append("        P: \"FrontAxis\", \"int\", \"Integer\", \"\",2\n");
        sb.append("        P: \"FrontAxisSign\", \"int\", \"Integer\", \"\",1\n");
        sb.append("        P: \"CoordAxis\", \"int\", \"Integer\", \"\",0\n");
        sb.append("        P: \"CoordAxisSign\", \"int\", \"Integer\", \"\",1\n");
        sb.append("        P: \"OriginalUpAxis\", \"int\", \"Integer\", \"\",-1\n");
        sb.append("        P: \"UnitScaleFactor\", \"double\", \"Number\", \"\",1\n");
        sb.append("    }\n");
        sb.append("}\n");

        sb.append("Definitions:  {\n");
        sb.append("    Version: 100\n");
        int defCount = 5 + data.materials.size() * 2;
        sb.append("    Count: ").append(defCount).append("\n");
        sb.append("    ObjectType: \"GlobalSettings\" {\n");
        sb.append("        Count: 1\n");
        sb.append("    }\n");
        sb.append("    ObjectType: \"Model\" {\n");
        sb.append("        Count: 1\n");
        sb.append("    }\n");
        sb.append("    ObjectType: \"Geometry\" {\n");
        sb.append("        Count: 1\n");
        sb.append("    }\n");
        sb.append("    ObjectType: \"Material\" {\n");
        sb.append("        Count: ").append(data.materials.size()).append("\n");
        sb.append("    }\n");
        sb.append("    ObjectType: \"Texture\" {\n");
        sb.append("        Count: ").append(data.materials.size()).append("\n");
        sb.append("    }\n");
        sb.append("    ObjectType: \"Video\" {\n");
        sb.append("        Count: ").append(data.materials.size()).append("\n");
        sb.append("    }\n");
        sb.append("}\n");

        sb.append("Objects:  {\n");

        long geoId = 1000000000L;
        long modelId = 1000000001L;
        long matBaseId = 2000000000L;
        long texBaseId = 3000000000L;
        long vidBaseId = 4000000000L;

        sb.append("    Geometry: ").append(geoId).append(", \"Geometry::Mesh\", \"Mesh\" {\n");

        sb.append("        Vertices: *").append(data.vertices.size() * 3).append(" {\n");
        sb.append("            a: ");
        for (int i = 0; i < data.vertices.size(); i++) {
            Vertex v = data.vertices.get(i);
            if (i > 0) sb.append(",");
            sb.append(v.x).append(",").append(v.y).append(",").append(v.z);
        }
        sb.append("\n        }\n");

        int totalTriIndices = data.faces.size() * 2 * 3;
        sb.append("        PolygonVertexIndex: *").append(totalTriIndices).append(" {\n");
        sb.append("            a: ");
        int pviCount = 0;
        for (int i = 0; i < data.faces.size(); i++) {
            Face f = data.faces.get(i);
            int[][] tris = {{0, 1, 2}, {0, 2, 3}};
            for (int[] tri : tris) {
                if (pviCount++ > 0) sb.append(",");
                sb.append(f.vIdx[tri[0]] - 1).append(",");
                sb.append(f.vIdx[tri[1]] - 1).append(",");
                sb.append(-((f.vIdx[tri[2]] - 1) + 1));
            }
        }
        sb.append("\n        }\n");

        sb.append("        GeometryVersion: 124\n");

        sb.append("        LayerElementNormal: 0 {\n");
        sb.append("            Version: 101\n");
        sb.append("            Name: \"\"\n");
        sb.append("            MappingInformationType: \"ByPolygonVertex\"\n");
        sb.append("            ReferenceInformationType: \"Direct\"\n");
        sb.append("            Normals: *").append(totalTriIndices * 3).append(" {\n");
        sb.append("                a: ");
        int nCount = 0;
        for (int i = 0; i < data.faces.size(); i++) {
            Face f = data.faces.get(i);
            Vector3f n = data.normals.get(f.vnIdx - 1);
            int[][] tris = {{0, 1, 2}, {0, 2, 3}};
            for (int[] tri : tris) {
                for (int ignored : tri) {
                    if (nCount++ > 0) sb.append(",");
                    sb.append(n.x).append(",").append(n.y).append(",").append(n.z);
                }
            }
        }
        sb.append("\n            }\n");
        sb.append("        }\n");

        sb.append("        LayerElementUV: 0 {\n");
        sb.append("            Version: 101\n");
        sb.append("            Name: \"UVMap\"\n");
        sb.append("            MappingInformationType: \"ByPolygonVertex\"\n");
        sb.append("            ReferenceInformationType: \"Direct\"\n");
        sb.append("            UV: *").append(totalTriIndices * 2).append(" {\n");
        sb.append("                a: ");
        int uvCount = 0;
        for (int i = 0; i < data.faces.size(); i++) {
            Face f = data.faces.get(i);
            int[][] tris = {{0, 1, 2}, {0, 2, 3}};
            for (int[] tri : tris) {
                for (int vi : tri) {
                    if (uvCount++ > 0) sb.append(",");
                    UV uv = data.uvs.get(f.vtIdx[vi] - 1);
                    sb.append(uv.u).append(",").append(uv.v);
                }
            }
        }
        sb.append("\n            }\n");
        sb.append("            UVIndex: *").append(totalTriIndices).append(" {\n");
        sb.append("                a: ");
        for (int i = 0; i < totalTriIndices; i++) {
            if (i > 0) sb.append(",");
            sb.append(i);
        }
        sb.append("\n            }\n");
        sb.append("        }\n");

        sb.append("        LayerElementMaterial: 0 {\n");
        sb.append("            Version: 101\n");
        sb.append("            Name: \"\"\n");
        sb.append("            MappingInformationType: \"ByPolygon\"\n");
        sb.append("            ReferenceInformationType: \"IndexToDirect\"\n");
        sb.append("            Materials: *").append(data.faces.size() * 2).append(" {\n");
        sb.append("                a: ");
        Map<Material, Integer> matIndex = new LinkedHashMap<>();
        int mi = 0;
        for (Material mat : data.materials.values()) matIndex.put(mat, mi++);
        int matIdxCount = 0;
        for (int i = 0; i < data.faces.size(); i++) {
            Face f = data.faces.get(i);
            int matId = matIndex.get(f.mat);
            if (matIdxCount++ > 0) sb.append(",");
            sb.append(matId).append(",").append(matId);
        }
        sb.append("\n            }\n");
        sb.append("        }\n");

        sb.append("    }\n");

        sb.append("    Model: ").append(modelId).append(", \"Model::Mesh\", \"Mesh\" {\n");
        sb.append("        Version: 232\n");
        sb.append("        Properties70:  {\n");
        sb.append("            P: \"Lcl Translation\", \"Lcl Translation\", \"\", \"Lcl\",0,0,0\n");
        sb.append("            P: \"Lcl Rotation\", \"Lcl Rotation\", \"\", \"Lcl\",0,0,0\n");
        sb.append("            P: \"Lcl Scaling\", \"Lcl Scaling\", \"\", \"Lcl\",1,1,1\n");
        sb.append("        }\n");
        sb.append("        Shading: Y\n");
        sb.append("        Culling: \"CullingOff\"\n");
        sb.append("    }\n");

        mi = 0;
        for (Material mat : data.materials.values()) {
            long matId = matBaseId + mi;
            long texId = texBaseId + mi;
            long vidId = vidBaseId + mi;

            Path texFile = path.resolveSibling(mat.fileName);
            byte[] texBytes;
            try {
                texBytes = saveTexture(mat.sprite, texFile, mat.tintColor);
            } catch (Exception e) {
                LOGGER.error("Failed to read texture bytes for {}", mat.fileName, e);
                texBytes = new byte[0];
            }
            String base64 = texBytes.length > 0 ? Base64.getEncoder().encodeToString(texBytes) : "";

            sb.append("    Material: ").append(matId).append(", \"Material::").append(mat.name).append("\", \"\" {\n");
            sb.append("        Version: 102\n");
            sb.append("        ShadingModel: \"Phong\"\n");
            sb.append("        MultiLayer: 0\n");
            sb.append("        Properties70:  {\n");
            sb.append("            P: \"DiffuseColor\", \"ColorRGB\", \"Color\", \"\",1,1,1\n");
            sb.append("            P: \"SpecularColor\", \"ColorRGB\", \"Color\", \"\",0,0,0\n");
            sb.append("        }\n");
            sb.append("    }\n");

            sb.append("    Texture: ").append(texId).append(", \"Texture::").append(mat.name).append("\", \"\" {\n");
            sb.append("        Type: \"TextureVideoClip\"\n");
            sb.append("        Version: 202\n");
            sb.append("        TextureName: \"Texture::").append(mat.name).append("\"\n");
            sb.append("        Media: \"Video::").append(mat.name).append("\"\n");
            sb.append("        Filename: \"").append(mat.fileName).append("\"\n");
            sb.append("        RelativeFilename: \"").append(mat.fileName).append("\"\n");
            sb.append("    }\n");

            sb.append("    Video: ").append(vidId).append(", \"Video::").append(mat.name).append("\", \"Clip\" {\n");
            sb.append("        Type: \"Clip\"\n");
            sb.append("        Properties70:  {\n");
            sb.append("            P: \"Path\", \"KString\", \"XRefUrl\", \"\",\"").append(mat.fileName).append("\"\n");
            sb.append("        }\n");
            sb.append("        UseMipMap: 0\n");
            sb.append("        Filename: \"").append(mat.fileName).append("\"\n");
            sb.append("        RelativeFilename: \"").append(mat.fileName).append("\"\n");
            if (!base64.isEmpty()) {
                sb.append("        Content: , \"").append(base64).append("\"\n");
            }
            sb.append("    }\n");

            mi++;
        }

        sb.append("}\n");

        sb.append("Connections:  {\n");
        sb.append("    C: \"OO\",").append(geoId).append(",").append(modelId).append("\n");
        for (int i = 0; i < data.materials.size(); i++) {
            long matId = matBaseId + i;
            long texId = texBaseId + i;
            long vidId = vidBaseId + i;
            sb.append("    C: \"OO\",").append(matId).append(",").append(modelId).append("\n");
            sb.append("    C: \"OP\",").append(texId).append(",").append(matId).append(",\"DiffuseColor\"\n");
            sb.append("    C: \"OO\",").append(vidId).append(",").append(texId).append("\n");
        }
        sb.append("}\n");

        Files.writeString(path, sb.toString());
    }

    private static void writeStl(MeshData data, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("solid YE\n");
        for (Face f : data.faces) {
            Vector3f n = data.normals.get(f.vnIdx - 1);
            Vertex v0 = data.vertices.get(f.vIdx[0] - 1);
            Vertex v1 = data.vertices.get(f.vIdx[1] - 1);
            Vertex v2 = data.vertices.get(f.vIdx[2] - 1);
            Vertex v3 = data.vertices.get(f.vIdx[3] - 1);
            appendTriangleStl(sb, n, v0, v1, v2);
            appendTriangleStl(sb, n, v0, v2, v3);
        }
        sb.append("endsolid YE\n");
        Files.writeString(path, sb.toString());
    }
    private static boolean isFaceVisible(AABBInt box, Level level, int x, int y, int z, Direction dir) {
        int nx = x + dir.getStepX();
        int ny = y + dir.getStepY();
        int nz = z + dir.getStepZ();
        if (nx < box.minX || nx >= box.maxX || ny < box.minY || ny >= box.maxY || nz < box.minZ || nz >= box.maxZ) {
            return true;
        }
        return level.getBlockState(new BlockPos(nx, ny, nz)).isAir();
    }

    private static boolean isFaceVisible(NGTObject ngto, int x, int y, int z, Direction dir) {
        int nx = x + dir.getStepX();
        int ny = y + dir.getStepY();
        int nz = z + dir.getStepZ();
        if (nx < 0 || nx >= ngto.xSize || ny < 0 || ny >= ngto.ySize || nz < 0 || nz >= ngto.zSize) return true;
        BlockSet bs = ngto.getBlockSet(nx, ny, nz);
        return bs == null || bs.state == null || bs.state.isAir();
    }

    private static Vector3f computeNormal(Vertex a, Vertex b, Vertex c) {
        Vector3f ab = new Vector3f(b.x - a.x, b.y - a.y, b.z - a.z);
        Vector3f ac = new Vector3f(c.x - a.x, c.y - a.y, c.z - a.z);
        Vector3f n = ab.cross(ac);
        if (n.lengthSquared() > 0.000001f) n.normalize();
        return n;
    }

    private static final int TEXTURE_SCALE = 8; // nearest-neighbor upscale to keep pixels sharp

    private static byte[] saveTexture(TextureAtlasSprite sprite, Path path, int tintColor) {
        try {
            Files.createDirectories(path.getParent());
            NativeImage img = sprite.contents().getOriginalImage();
            NativeImage processed = new NativeImage(img.getWidth() * TEXTURE_SCALE, img.getHeight() * TEXTURE_SCALE, false);
            processTexture(img, processed, tintColor);
            img = processed;
            img.writeToFile(path);
            return Files.readAllBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private static void processTexture(NativeImage src, NativeImage dst, int tintColor) {
        int tr = 255, tg = 255, tb = 255;
        if (tintColor != -1) {
            tr = (tintColor >> 16) & 0xFF;
            tg = (tintColor >> 8) & 0xFF;
            tb = tintColor & 0xFF;
        }
        for (int y = 0; y < dst.getHeight(); y++) {
            for (int x = 0; x < dst.getWidth(); x++) {
                int abgr = src.getPixel(x / TEXTURE_SCALE, y / TEXTURE_SCALE);
                int a = (abgr >> 24) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int r = abgr & 0xFF;
                if (tintColor != -1) {
                    r = r * tr / 255;
                    g = g * tg / 255;
                    b = b * tb / 255;
                }
                dst.setPixel(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }

    private static int getBiomeTint(BlockState state, int tintIndex) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc.getBlockColors().getColor(state, null, null, tintIndex);
        } catch (Exception e) {
            return -1;
        }
    }

    private static String sanitizeMaterial(String s) {
        return s.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static class Vertex {
        final float x, y, z;
        Vertex(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    }

    private static class UV {
        final float u, v;
        UV(float u, float v) { this.u = u; this.v = v; }
    }

    private static class Face {
        final Material mat;
        final int[] vIdx;
        final int[] vtIdx;
        final int vnIdx;
        Face(Material mat, int[] vIdx, int[] vtIdx, int vnIdx) {
            this.mat = mat;
            this.vIdx = vIdx;
            this.vtIdx = vtIdx;
            this.vnIdx = vnIdx;
        }
    }


    private static class Material {
        final TextureAtlasSprite sprite;
        final String name;
        final String fileName;
        int tintColor = -1;
        Material(TextureAtlasSprite sprite) {
            this.sprite = sprite;
            ResourceLocation texId = sprite.contents().name();
            this.name = sanitizeMaterial(texId.getNamespace() + "_" + texId.getPath());
            this.fileName = sanitizeMaterial(texId.getPath()) + ".png";
        }
    }

    private static void appendTriangleStl(StringBuilder sb, Vector3f n, Vertex a, Vertex b, Vertex c) {
        sb.append("facet normal ").append(n.x).append(' ').append(n.y).append(' ').append(n.z).append('\n');
        sb.append("  outer loop\n");
        sb.append("    vertex ").append(a.x).append(' ').append(a.y).append(' ').append(a.z).append('\n');
        sb.append("    vertex ").append(b.x).append(' ').append(b.y).append(' ').append(b.z).append('\n');
        sb.append("    vertex ").append(c.x).append(' ').append(c.y).append(' ').append(c.z).append('\n');
        sb.append("  endloop\n");
        sb.append("endfacet\n");
    }

    // Legacy cube-based exports
    private static String exportObjLegacy(AABBInt box, Level level) {
        StringBuilder sb = new StringBuilder();
        sb.append("# YE OBJ export\n");
        sb.append("# ").append(box.sizeX()).append(" x ").append(box.sizeY()).append(" x ").append(box.sizeZ()).append("\n");
        int vertexOffset = 1;
        for (int y = box.minY; y < box.maxY; ++y) {
            for (int z = box.minZ; z < box.maxZ; ++z) {
                for (int x = box.minX; x < box.maxX; ++x) {
                    if (isAirLegacy(level, x, y, z)) continue;
                    int faces = visibleFacesLegacy(box, level, x, y, z);
                    if (faces == 0) continue;
                    float fx = x - box.minX;
                    float fy = y - box.minY;
                    float fz = z - box.minZ;
                    vertexOffset = appendCubeObjLegacy(sb, fx, fy, fz, faces, vertexOffset);
                }
            }
        }
        return sb.toString();
    }

    private static String exportStlLegacy(AABBInt box, Level level) {
        StringBuilder sb = new StringBuilder();
        sb.append("solid YE\n");
        for (int y = box.minY; y < box.maxY; ++y) {
            for (int z = box.minZ; z < box.maxZ; ++z) {
                for (int x = box.minX; x < box.maxX; ++x) {
                    if (isAirLegacy(level, x, y, z)) continue;
                    int faces = visibleFacesLegacy(box, level, x, y, z);
                    if (faces == 0) continue;
                    float fx = x - box.minX;
                    float fy = y - box.minY;
                    float fz = z - box.minZ;
                    appendCubeStlLegacy(sb, fx, fy, fz, faces);
                }
            }
        }
        sb.append("endsolid YE\n");
        return sb.toString();
    }

    private static boolean isAirLegacy(Level level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    private static int visibleFacesLegacy(AABBInt box, Level level, int x, int y, int z) {
        int mask = 0;
        if (x <= box.minX || isAirLegacy(level, x - 1, y, z)) mask |= 1;
        if (x >= box.maxX - 1 || isAirLegacy(level, x + 1, y, z)) mask |= 2;
        if (y <= box.minY || isAirLegacy(level, x, y - 1, z)) mask |= 4;
        if (y >= box.maxY - 1 || isAirLegacy(level, x, y + 1, z)) mask |= 8;
        if (z <= box.minZ || isAirLegacy(level, x, y, z - 1)) mask |= 16;
        if (z >= box.maxZ - 1 || isAirLegacy(level, x, y, z + 1)) mask |= 32;
        return mask;
    }

    private static int appendCubeObjLegacy(StringBuilder sb, float x, float y, float z, int faces, int offset) {
        int start = offset;
        sb.append("v ").append(x).append(' ').append(y).append(' ').append(z).append('\n');
        sb.append("v ").append(x + 1).append(' ').append(y).append(' ').append(z).append('\n');
        sb.append("v ").append(x + 1).append(' ').append(y + 1).append(' ').append(z).append('\n');
        sb.append("v ").append(x).append(' ').append(y + 1).append(' ').append(z).append('\n');
        sb.append("v ").append(x).append(' ').append(y).append(' ').append(z + 1).append('\n');
        sb.append("v ").append(x + 1).append(' ').append(y).append(' ').append(z + 1).append('\n');
        sb.append("v ").append(x + 1).append(' ').append(y + 1).append(' ').append(z + 1).append('\n');
        sb.append("v ").append(x).append(' ').append(y + 1).append(' ').append(z + 1).append('\n');
        if ((faces & 1) != 0) { sb.append("f ").append(start).append(' ').append(start + 3).append(' ').append(start + 7).append(' ').append(start + 4).append('\n'); }
        if ((faces & 2) != 0) { sb.append("f ").append(start + 1).append(' ').append(start + 5).append(' ').append(start + 6).append(' ').append(start + 2).append('\n'); }
        if ((faces & 4) != 0) { sb.append("f ").append(start).append(' ').append(start + 4).append(' ').append(start + 5).append(' ').append(start + 1).append('\n'); }
        if ((faces & 8) != 0) { sb.append("f ").append(start + 3).append(' ').append(start + 2).append(' ').append(start + 6).append(' ').append(start + 7).append('\n'); }
        if ((faces & 16) != 0) { sb.append("f ").append(start).append(' ').append(start + 1).append(' ').append(start + 2).append(' ').append(start + 3).append('\n'); }
        if ((faces & 32) != 0) { sb.append("f ").append(start + 4).append(' ').append(start + 7).append(' ').append(start + 6).append(' ').append(start + 5).append('\n'); }
        return start + 8;
    }

    private static void appendCubeStlLegacy(StringBuilder sb, float x, float y, float z, int faces) {
        float x1 = x, x2 = x + 1;
        float y1 = y, y2 = y + 1;
        float z1 = z, z2 = z + 1;
        if ((faces & 1) != 0) appendQuadStlLegacy(sb, -1, 0, 0, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2);
        if ((faces & 2) != 0) appendQuadStlLegacy(sb, 1, 0, 0, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1);
        if ((faces & 4) != 0) appendQuadStlLegacy(sb, 0, -1, 0, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        if ((faces & 8) != 0) appendQuadStlLegacy(sb, 0, 1, 0, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        if ((faces & 16) != 0) appendQuadStlLegacy(sb, 0, 0, -1, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        if ((faces & 32) != 0) appendQuadStlLegacy(sb, 0, 0, 1, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
    }

    private static void appendQuadStlLegacy(StringBuilder sb, float nx, float ny, float nz,
                                            float x1, float y1, float z1,
                                            float x2, float y2, float z2,
                                            float x3, float y3, float z3,
                                            float x4, float y4, float z4) {
        appendTriangleStlLegacy(sb, nx, ny, nz, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        appendTriangleStlLegacy(sb, nx, ny, nz, x1, y1, z1, x3, y3, z3, x4, y4, z4);
    }

    private static void appendTriangleStlLegacy(StringBuilder sb, float nx, float ny, float nz,
                                                float x1, float y1, float z1,
                                                float x2, float y2, float z2,
                                                float x3, float y3, float z3) {
        sb.append("facet normal ").append(nx).append(' ').append(ny).append(' ').append(nz).append('\n');
        sb.append("  outer loop\n");
        sb.append("    vertex ").append(x1).append(' ').append(y1).append(' ').append(z1).append('\n');
        sb.append("    vertex ").append(x2).append(' ').append(y2).append(' ').append(z2).append('\n');
        sb.append("    vertex ").append(x3).append(' ').append(y3).append(' ').append(z3).append('\n');
        sb.append("  endloop\n");
        sb.append("endfacet\n");
    }
}
