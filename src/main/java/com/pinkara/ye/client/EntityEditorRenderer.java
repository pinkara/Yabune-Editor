package com.pinkara.ye.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pinkara.ye.editor.EntityEditor;

import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.block.NGTObject;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class EntityEditorRenderer extends EntityRenderer<EntityEditor, EntityEditorRenderer.EntityEditorRenderState> {
    private static final RenderType EDITOR_BOX_FILL = RenderType.create(
            "ye_editor_box_fill",
            1536,
            false,
            true,
            net.minecraft.client.renderer.RenderPipelines.DEBUG_QUADS,
            RenderType.CompositeState.builder().createCompositeState(false)
    );

    private final int[] hitBoxBuf = new int[6];
    private final int[] selectBoxBuf = new int[6];

    public EntityEditorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityEditorRenderState createRenderState() {
        return new EntityEditorRenderState();
    }

    @Override
    public void extractRenderState(EntityEditor entity, EntityEditorRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.selectEnd = entity.isSelectEnd();
        state.editMode = entity.getEditMode();
        state.hasCloneBox = entity.hasCloneBox();
        state.cloneBox = entity.getCloneBox();
        state.blocksForRenderer = entity.blocksForRenderer;
        state.x = Mth.lerp(partialTick, entity.xo, entity.getX());
        state.y = Mth.lerp(partialTick, entity.yo, entity.getY());
        state.z = Mth.lerp(partialTick, entity.zo, entity.getZ());

        initSelectBox(entity, state);

        HitResult target = entity.getTarget(true);
        if (target instanceof BlockHitResult blockHit && target.getType() != HitResult.Type.MISS) {
            state.targetPos = blockHit.getBlockPos();
            state.targetValid = true;
        } else {
            state.targetValid = false;
        }
    }

    @Override
    public void submit(EntityEditorRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();

        int startX = state.hitBoxBuf[0];
        int startY = state.hitBoxBuf[1];
        int startZ = state.hitBoxBuf[2];
        int endX = state.hitBoxBuf[3];
        int endY = state.hitBoxBuf[4];
        int endZ = state.hitBoxBuf[5];
        int minX = state.selectBoxBuf[0];
        int minY = state.selectBoxBuf[1];
        int minZ = state.selectBoxBuf[2];
        int maxX = state.selectBoxBuf[3];
        int maxY = state.selectBoxBuf[4];
        int maxZ = state.selectBoxBuf[5];

        poseStack.translate(minX - state.x, minY - state.y, minZ - state.z);

        float difX = maxX - minX;
        float difY = maxY - minY;
        float difZ = maxZ - minZ;
        float minPos = -0.05f;

        // Selection box fill (original dark green)
        renderFilledBox(poseStack, nodeCollector, minPos, minPos, minPos, 1.1f + difX, 1.1f + difY, 1.1f + difZ, 0x204020, 64);
        // Selection box frame (black)
        renderFrame(poseStack, nodeCollector, minPos, minPos, minPos, 1.1f + difX, 1.1f + difY, 1.1f + difZ, 0x000000, 255);

        // Start marker (red)
        float f1 = 0.06f;
        float size1 = 1.12f;
        renderFilledBox(poseStack, nodeCollector, (startX - minX) - f1, (startY - minY) - f1, (startZ - minZ) - f1, size1, size1, size1, 0xFF0000, 128);
        renderFrame(poseStack, nodeCollector, (startX - minX) - f1, (startY - minY) - f1, (startZ - minZ) - f1, size1, size1, size1, 0xFF0000, 255);

        // End marker (blue)
        renderFilledBox(poseStack, nodeCollector, (endX - minX) - f1, (endY - minY) - f1, (endZ - minZ) - f1, size1, size1, size1, 0x0000FF, 128);
        renderFrame(poseStack, nodeCollector, (endX - minX) - f1, (endY - minY) - f1, (endZ - minZ) - f1, size1, size1, size1, 0x0000FF, 255);

        boolean cloneMode = state.selectEnd && state.editMode == 3;

        // Paste preview
        if (state.blocksForRenderer != null && state.editMode == 2 && state.targetValid) {
            poseStack.pushPose();
            poseStack.translate(state.targetPos.getX() - minX, state.targetPos.getY() - minY, state.targetPos.getZ() - minZ);
            renderGhostBlocks(state.blocksForRenderer, poseStack, nodeCollector);
            poseStack.popPose();
        }

        // Clone preview
        if (cloneMode && state.hasCloneBox) {
            poseStack.pushPose();
            int[] box = state.cloneBox;
            NGTObject clipboard = state.blocksForRenderer;
            boolean renderBlocks = clipboard != null;
            float cx = (1.1f + difX) * 0.5f;
            float cz = (1.1f + difZ) * 0.5f;
            for (int i = 0; i <= box[3]; ++i) {
                poseStack.pushPose();
                poseStack.translate(box[0] * i, box[1] * i, box[2] * i);
                if (renderBlocks) {
                    renderGhostBlocks(clipboard, poseStack, nodeCollector);
                }
                renderFilledBox(poseStack, nodeCollector, minPos, minPos, minPos, 1.1f + difX, 1.1f + difY, 1.1f + difZ, 0xFFAA00, 32);
                renderFrame(poseStack, nodeCollector, minPos, minPos, minPos, 1.1f + difX, 1.1f + difY, 1.1f + difZ, 0xFFFF00, 192);

                if (i > 0) {
                    poseStack.pushPose();
                    poseStack.translate(cx, 1.1f + difY + 0.3f, cz);
                    poseStack.mulPose(cameraRenderState.orientation);
                    poseStack.scale(-0.025F, -0.025F, 0.025F);
                    String label = "Clone " + i;
                    Font font = this.getFont();
                    FormattedCharSequence seq = FormattedCharSequence.forward(label, Style.EMPTY);
                    nodeCollector.submitText(poseStack, font.width(seq) / -2.0f, 0, seq, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, 0xFFFFFF00, 0, 0);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        poseStack.popPose();

        // Floating text
        this.renderText(state, poseStack, nodeCollector, cameraRenderState);
    }

    private void initSelectBox(EntityEditor editor, EntityEditorRenderState state) {
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
        System.arraycopy(this.hitBoxBuf, 0, state.hitBoxBuf, 0, 6);
        System.arraycopy(this.selectBoxBuf, 0, state.selectBoxBuf, 0, 6);
    }

    private void renderGhostBlocks(NGTObject ngto, PoseStack poseStack, SubmitNodeCollector nodeCollector) {
        if (ngto == null) return;
        for (int x = 0; x < ngto.xSize; ++x) {
            for (int y = 0; y < ngto.ySize; ++y) {
                for (int z = 0; z < ngto.zSize; ++z) {
                    BlockSet set = ngto.getBlockSet(x, y, z);
                    if (set.state.isAir()) continue;
                    poseStack.pushPose();
                    poseStack.translate(x, y, z);
                    nodeCollector.submitBlock(poseStack, set.state, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                    poseStack.popPose();
                }
            }
        }
    }

    private void renderText(EntityEditorRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        Font font = this.getFont();
        if (font == null) return;

        poseStack.pushPose();
        poseStack.translate(0.0, 2.0, 0.0);
        poseStack.mulPose(cameraRenderState.orientation);
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        String start = String.format("Start : %d, %d, %d", state.hitBoxBuf[0], state.hitBoxBuf[1], state.hitBoxBuf[2]);
        String end = String.format("End : %d, %d, %d", state.hitBoxBuf[3], state.hitBoxBuf[4], state.hitBoxBuf[5]);
        String size = String.format("Size : %d, %d, %d",
                Math.abs(state.hitBoxBuf[3] - state.hitBoxBuf[0]) + 1,
                Math.abs(state.hitBoxBuf[4] - state.hitBoxBuf[1]) + 1,
                Math.abs(state.hitBoxBuf[5] - state.hitBoxBuf[2]) + 1);
        String mode = "Mode : " + getModeName(state.editMode);

        int y = 0;
        drawText(start, -font.width(start) / 2.0f, y, 0xFFFF0000, matrix, nodeCollector, font);
        y += 12;
        drawText(end, -font.width(end) / 2.0f, y, 0xFF0000FF, matrix, nodeCollector, font);
        y += 12;
        drawText(size, -font.width(size) / 2.0f, y, 0xFF00FF00, matrix, nodeCollector, font);
        y += 12;
        drawText(mode, -font.width(mode) / 2.0f, y, 0xFFFFFF00, matrix, nodeCollector, font);

        poseStack.popPose();
    }

    private static void drawText(String text, float x, int y, int color, Matrix4f matrix, SubmitNodeCollector nodeCollector, Font font) {
        FormattedCharSequence seq = FormattedCharSequence.forward(text, Style.EMPTY);
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(matrix);
        nodeCollector.submitText(poseStack, x, y, seq, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, color, 0, 0);
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

    private static void renderFilledBox(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                        float minX, float minY, float minZ,
                                        float width, float height, float depth,
                                        int color, int alpha) {
        nodeCollector.submitCustomGeometry(poseStack, EDITOR_BOX_FILL, (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            float maxX = minX + width;
            float maxY = minY + height;
            float maxZ = minZ + depth;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = alpha / 255.0f;

            // South
            consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
            // North
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
            // East
            consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
            // West
            consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
            // Top
            consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
            // Bottom
            consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
            consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
        });
    }

    private static void renderFrame(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                    float minX, float minY, float minZ,
                                    float width, float height, float depth,
                                    int color, int alpha) {
        nodeCollector.submitCustomGeometry(poseStack, RenderType.lines(), (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            float maxX = minX + width;
            float maxY = minY + height;
            float maxZ = minZ + depth;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = alpha / 255.0f;

            line(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
            line(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
            line(consumer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
            line(consumer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

            line(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
            line(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
            line(consumer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
            line(consumer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

            line(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
            line(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
            line(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
            line(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        });
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

    public static class EntityEditorRenderState extends EntityRenderState {
        public final int[] hitBoxBuf = new int[6];
        public final int[] selectBoxBuf = new int[6];
        public byte editMode;
        public boolean selectEnd;
        public boolean hasCloneBox;
        public int[] cloneBox;
        public NGTObject blocksForRenderer;
        public double x, y, z;
        public BlockPos targetPos = BlockPos.ZERO;
        public boolean targetValid;
    }
}
