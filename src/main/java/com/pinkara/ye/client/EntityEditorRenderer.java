package com.pinkara.ye.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pinkara.ye.editor.EntityEditor;
import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.block.NGTObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class EntityEditorRenderer extends EntityRenderer<EntityEditor> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("ye", "textures/atc.png");

    private static final RenderType EDITOR_BOX_FILL = RenderType.create(
            "ye_editor_box_fill",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private final int[] hitBoxBuf = new int[6];
    private final int[] selectBoxBuf = new int[6];

    public EntityEditorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityEditor entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        this.initSelectBox(entity);

        int startX = this.hitBoxBuf[0];
        int startY = this.hitBoxBuf[1];
        int startZ = this.hitBoxBuf[2];
        int endX = this.hitBoxBuf[3];
        int endY = this.hitBoxBuf[4];
        int endZ = this.hitBoxBuf[5];
        int minX = this.selectBoxBuf[0];
        int minY = this.selectBoxBuf[1];
        int minZ = this.selectBoxBuf[2];
        int maxX = this.selectBoxBuf[3];
        int maxY = this.selectBoxBuf[4];
        int maxZ = this.selectBoxBuf[5];

        double pX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double pY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double pZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

        poseStack.translate(minX - pX, minY - pY, minZ - pZ);

        float difX = maxX - minX;
        float difY = maxY - minY;
        float difZ = maxZ - minZ;
        float minPos = -0.05f;

        // Selection box fill (original dark green)
        renderFilledBox(poseStack, buffer, minPos, minPos, minPos, 1.1f + difX, 1.1f + difY, 1.1f + difZ, 0x204020, 64);
        // Selection box frame (black)
        renderFrame(poseStack, buffer, minPos, minPos, minPos, 1.1f + difX, 1.1f + difY, 1.1f + difZ, 0x000000, 255);

        // Start marker (red)
        float f1 = 0.06f;
        float size1 = 1.12f;
        renderFilledBox(poseStack, buffer, (startX - minX) - f1, (startY - minY) - f1, (startZ - minZ) - f1, size1, size1, size1, 0xFF0000, 128);
        renderFrame(poseStack, buffer, (startX - minX) - f1, (startY - minY) - f1, (startZ - minZ) - f1, size1, size1, size1, 0xFF0000, 255);

        // End marker (blue)
        renderFilledBox(poseStack, buffer, (endX - minX) - f1, (endY - minY) - f1, (endZ - minZ) - f1, size1, size1, size1, 0x0000FF, 128);
        renderFrame(poseStack, buffer, (endX - minX) - f1, (endY - minY) - f1, (endZ - minZ) - f1, size1, size1, size1, 0x0000FF, 255);

        byte editMode = entity.getEditMode();
        boolean cloneMode = entity.isSelectEnd() && editMode == 3;

        // Paste preview: show the copied structure at the targeted block in paste mode.
        if (entity.blocksForRenderer != null && editMode == 2) {
            HitResult target = entity.getTarget(true);
            if (target instanceof BlockHitResult blockHit && target.getType() != HitResult.Type.MISS) {
                BlockPos tgPos = blockHit.getBlockPos();
                poseStack.pushPose();
                poseStack.translate(tgPos.getX() - minX, tgPos.getY() - minY, tgPos.getZ() - minZ);
                this.renderBlocks(entity, poseStack, buffer);
                poseStack.popPose();
            }
        }

        // Clone preview: show the copied structure inside each translucent yellow zone.
        if (cloneMode && entity.hasCloneBox()) {
            poseStack.pushPose();
            int[] box = entity.getCloneBox();
            Font font = this.getFont();
            NGTObject clipboard = entity.blocksForRenderer;
            boolean renderBlocks = clipboard != null;
            float cx = (1.1f + difX) * 0.5f;
            float cz = (1.1f + difZ) * 0.5f;
            for (int i = 0; i <= box[3]; ++i) {
                poseStack.pushPose();
                poseStack.translate(box[0] * i, box[1] * i, box[2] * i);
                if (renderBlocks) {
                    renderGhostBlocks(clipboard, poseStack, buffer);
                }
                renderFilledBox(poseStack, buffer, minPos, minPos, minPos, 1.1f + difX, 1.1f + difY, 1.1f + difZ, 0xFFAA00, 32);
                renderFrame(poseStack, buffer, minPos, minPos, minPos, 1.1f + difX, 1.1f + difY, 1.1f + difZ, 0xFFFF00, 192);

                if (font != null && i > 0) {
                    poseStack.pushPose();
                    poseStack.translate(cx, 1.1f + difY + 0.3f, cz);
                    poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                    poseStack.scale(-0.025F, -0.025F, 0.025F);
                    String label = "Clone " + i;
                    font.drawInBatch(label, -font.width(label) / 2.0f, 0, 0xFFFF00, false,
                            poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        poseStack.popPose();

        // Floating text
        this.renderText(entity, poseStack, buffer);
    }

    private void initSelectBox(EntityEditor editor) {
        for (int i = 0; i < 6; ++i) {
            this.hitBoxBuf[i] = 0;
            this.selectBoxBuf[i] = 0;
        }
        int[] start = editor.getPos(EntityEditor.START_POS);
        int[] end = editor.getPos(EntityEditor.END_POS);
        if (!editor.isSelectEnd()) {
            HitResult target = editor.getTarget(false);
            if (target instanceof BlockHitResult blockHit && target.getType() != HitResult.Type.MISS) {
                BlockPos pos = blockHit.getBlockPos();
                end[0] = pos.getX();
                end[1] = pos.getY();
                end[2] = pos.getZ();
            } else {
                end[0] = start[0];
                end[1] = start[1];
                end[2] = start[2];
            }
        }
        for (int i = 0; i < 3; ++i) {
            this.hitBoxBuf[i] = start[i];
            this.hitBoxBuf[i + 3] = end[i];
            if (start[i] < end[i]) {
                this.selectBoxBuf[i] = start[i];
                this.selectBoxBuf[i + 3] = end[i];
            } else {
                this.selectBoxBuf[i] = end[i];
                this.selectBoxBuf[i + 3] = start[i];
            }
        }
    }

    private void renderBlocks(EntityEditor editor, PoseStack poseStack, MultiBufferSource buffer) {
        renderGhostBlocks(editor.blocksForRenderer, poseStack, buffer);
        editor.setUpdate(false);
    }

    private void renderGhostBlocks(NGTObject ngto, PoseStack poseStack, MultiBufferSource buffer) {
        if (ngto == null) return;

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (int x = 0; x < ngto.xSize; ++x) {
            for (int y = 0; y < ngto.ySize; ++y) {
                for (int z = 0; z < ngto.zSize; ++z) {
                    BlockSet set = ngto.getBlockSet(x, y, z);
                    if (set.state.isAir()) continue;
                    poseStack.pushPose();
                    poseStack.translate(x, y, z);
                    dispatcher.renderSingleBlock(set.state, poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }
        }
    }

    private void renderText(EntityEditor entity, PoseStack poseStack, MultiBufferSource buffer) {
        Font font = this.getFont();
        if (font == null) return;

        poseStack.pushPose();
        poseStack.translate(0.0, 2.0, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        String start = String.format("Start : %d, %d, %d", this.hitBoxBuf[0], this.hitBoxBuf[1], this.hitBoxBuf[2]);
        String end = String.format("End : %d, %d, %d", this.hitBoxBuf[3], this.hitBoxBuf[4], this.hitBoxBuf[5]);
        String size = String.format("Size : %d, %d, %d",
                Math.abs(this.hitBoxBuf[3] - this.hitBoxBuf[0]) + 1,
                Math.abs(this.hitBoxBuf[4] - this.hitBoxBuf[1]) + 1,
                Math.abs(this.hitBoxBuf[5] - this.hitBoxBuf[2]) + 1);
        String mode = "Mode : " + getModeName(entity.getEditMode());

        int y = 0;
        font.drawInBatch(start, -font.width(start) / 2.0f, y, 0xFF0000, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        y += 12;
        font.drawInBatch(end, -font.width(end) / 2.0f, y, 0x0000FF, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        y += 12;
        font.drawInBatch(size, -font.width(size) / 2.0f, y, 0x00FF00, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        y += 12;
        font.drawInBatch(mode, -font.width(mode) / 2.0f, y, 0xFFFF00, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    private static String getModeName(byte mode) {
        return switch (mode) {
            case 0 -> "Select Start";
            case 1 -> "Select End";
            case 2 -> "Paste";
            case 3 -> "Clone";
            default -> "Unknown";
        };
    }

    private static void renderFilledBox(PoseStack poseStack, MultiBufferSource buffer,
                                        float minX, float minY, float minZ,
                                        float width, float height, float depth,
                                        int color, int alpha) {
        VertexConsumer builder = buffer.getBuffer(EDITOR_BOX_FILL);
        Matrix4f matrix = poseStack.last().pose();
        float maxX = minX + width;
        float maxY = minY + height;
        float maxZ = minZ + depth;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = alpha / 255.0f;

        // South
        builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
        // North
        builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        // East
        builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        // West
        builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
        // Top
        builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
        // Bottom
        builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
        builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
        builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
    }

    private static void renderFrame(PoseStack poseStack, MultiBufferSource buffer,
                                    float minX, float minY, float minZ,
                                    float width, float height, float depth,
                                    int color, int alpha) {
        VertexConsumer builder = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        float maxX = minX + width;
        float maxY = minY + height;
        float maxZ = minZ + depth;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = alpha / 255.0f;

        line(builder, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(builder, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(builder, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(builder, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        line(builder, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(builder, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(builder, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(builder, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        line(builder, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(builder, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(builder, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(builder, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void line(VertexConsumer builder, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0.0f) len = 1.0f;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;
        builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEditor entity) {
        return TEXTURE;
    }
}
