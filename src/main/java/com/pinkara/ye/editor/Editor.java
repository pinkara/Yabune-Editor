package com.pinkara.ye.editor;

import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.block.BlockUtil;
import com.pinkara.youma.block.NGTObject;
import com.pinkara.youma.io.NGTLog;
import com.pinkara.youma.math.AABBInt;
import com.pinkara.youma.util.Stack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;

import java.util.Arrays;

public class Editor {
    public static final byte EditType_Replace = 6;
    public static final byte EditType_Clone = 7;
    public static final byte EditMode_0 = 0;
    public static final byte EditMode_1 = 1;
    public static final byte EditMode_VisibleBox_0 = 2;
    public static final byte EditMode_VisibleBox_1 = 3;
    public static final byte EditMode_Max = 3;

    private static final int UNDO_SIZE = 5;

    private final EntityEditor editorEntity;
    private WorldSnapshot clipboard;
    private final Stack<WorldSnapshot> history = new Stack<>(UNDO_SIZE);

    public Editor(EntityEditor par1) {
        this.editorEntity = par1;
    }

    public static EntityEditor getNewEditor(Level world, Player player, int x, int y, int z) {
        if (world.isClientSide()) {
            return null;
        }
        EntityEditor entity = new EntityEditor(world, player, x, y, z);
        Editor editor = new Editor(entity);
        EditorManager.INSTANCE.add(player.getGameProfile().name(), editor);
        return entity;
    }

    public EntityEditor getEntity() {
        return this.editorEntity;
    }

    public boolean hasClipboard() {
        return this.clipboard != null;
    }

    public WorldSnapshot getClipboard() {
        return this.clipboard;
    }

    public Level getWorld() {
        return this.getEntity().level();
    }

    public AABBInt getSelectBox() {
        int[] start = this.getEntity().getPos(EntityEditor.START_POS);
        int[] end = this.getEntity().getPos(EntityEditor.END_POS);
        if ((start[0] == 0 && start[1] == 0 && start[2] == 0) || (end[0] == 0 && end[1] == 0 && end[2] == 0)) {
            return null;
        }
        int minX = Math.min(start[0], end[0]);
        int maxX = Math.max(start[0], end[0]);
        int minY = Math.min(start[1], end[1]);
        int maxY = Math.max(start[1], end[1]);
        int minZ = Math.min(start[2], end[2]);
        int maxZ = Math.max(start[2], end[2]);
        return new AABBInt(minX, minY, minZ, ++maxX, ++maxY, ++maxZ);
    }

    public AABBInt getPasteBox() {
        byte mode = this.getEntity().getEditMode();
        if (mode != 2 && mode != 3) {
            NGTLog.debug("[Yabune Editor](Edit) Not paste mode");
            return null;
        }
        HitResult target = this.getEntity().getTarget(true);
        if (target == null || target.getType() != HitResult.Type.BLOCK) {
            NGTLog.debug("[Yabune Editor](Edit) BlockHitResult not found");
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult) target;
        if (this.clipboard == null) {
            NGTLog.debug("[Yabune Editor](Edit) Clipboard is empty");
            return null;
        }
        int[] box = this.getEntity().getPos(EntityEditor.PASTE_BOX);
        if (this.clipboard.getSize() != box[0] * box[1] * box[2]) {
            this.getEntity().updateBlockList(null);
            NGTLog.debug("[Yabune Editor](Edit) Illegal block list size");
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        int minX = pos.getX();
        int minY = pos.getY();
        int minZ = pos.getZ();
        int maxX = minX + box[0];
        int maxY = minY + box[1];
        int maxZ = minZ + box[2];
        return new AABBInt(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean editBlocks(byte editType, float par2) {
        Level world = this.getWorld();
        if (!this.getEntity().isSelectEnd()) {
            NGTLog.debug("[Yabune Editor](Edit) Not select end");
            return false;
        }
        if (world.isClientSide()) {
            NGTLog.debug("[Yabune Editor](Edit) Can't edit in Client");
            return false;
        }
        AABBInt box = this.getSelectBox();
        if (box == null) {
            return false;
        }
        if (editType == EditType_Replace || editType == EditType_Clone) {
            this.record(box);
        }
        if (editType == EditType_Replace) {
            this.repeat(box, (rbox, index, rep, rx, ry, rz) -> {
                BlockState state0 = this.getEntity().getSlotBlockState(0);
                BlockState state1 = this.getEntity().getSlotBlockState(1);
                if (state0 != null && state1 != null) {
                    BlockState current = BlockUtil.getBlockState(world, rx, ry, rz);
                    if (current == state0) {
                        this.setBlock(rx, ry, rz, new BlockSet(state1), true);
                    }
                }
            }, 1);
        } else if (editType == EditType_Clone) {
            this.repeat(box, (rbox, index, rep, rx, ry, rz) -> {
                if (this.getEntity().hasCloneBox()) {
                    BlockSet blockSet = this.getBlockSet(rx, ry, rz);
                    int[] box1 = this.getEntity().getCloneBox();
                    for (int l = 1; l < box1[3] + 1; ++l) {
                        int x = rx + box1[0] * l;
                        int y = ry + box1[1] * l;
                        int z = rz + box1[2] * l;
                        this.setBlock(x, y, z, blockSet, true);
                    }
                }
            }, 1);
        }
        return true;
    }

    public void transformBlocks(EditorTransform type) {
        BlockSet[] blocks = new BlockSet[this.clipboard.getSize()];
        int[] box = this.getEntity().getPos(EntityEditor.PASTE_BOX);
        int xSize = box[0];
        int ySize = box[1];
        int zSize = box[2];
        int xSize2 = xSize;
        int ySize2 = ySize;
        int zSize2 = zSize;
        if (type == EditorTransform.Transform_RotateX || type == EditorTransform.Transform_RotateX_Minus) {
            ySize = zSize2;
            zSize = ySize2;
        } else if (type == EditorTransform.Transform_RotateY || type == EditorTransform.Transform_RotateY_Minus) {
            xSize = zSize2;
            zSize = xSize2;
        } else if (type == EditorTransform.Transform_RotateZ || type == EditorTransform.Transform_RotateZ_Minus) {
            xSize = ySize2;
            ySize = xSize2;
        }
        int xSizeF = xSize;
        int ySizeF = ySize;
        int zSizeF = zSize;
        AABBInt box2 = new AABBInt(xSize2, ySize2, zSize2);
        box2.repeat((i, j, k, count) -> {
            BlockSet set = this.clipboard.getBlocks().get(count);
            BlockState state = set.state;
            int x1 = i;
            int y1 = j;
            int z1 = k;
            if (type == EditorTransform.Transform_RotateX) {
                y1 = zSize2 - k - 1;
                z1 = j;
            } else if (type == EditorTransform.Transform_RotateX_Minus) {
                y1 = k;
                z1 = ySize2 - j - 1;
            } else if (type == EditorTransform.Transform_RotateY) {
                z1 = xSize2 - i - 1;
                x1 = k;
                state = state.rotate(Rotation.COUNTERCLOCKWISE_90);
            } else if (type == EditorTransform.Transform_RotateY_Minus) {
                z1 = i;
                x1 = zSize2 - k - 1;
                state = state.rotate(Rotation.CLOCKWISE_90);
            } else if (type == EditorTransform.Transform_RotateZ) {
                x1 = ySize2 - j - 1;
                y1 = i;
            } else if (type == EditorTransform.Transform_RotateZ_Minus) {
                x1 = j;
                y1 = xSize2 - i - 1;
            } else if (type == EditorTransform.Transform_MirrorX) {
                x1 = xSize2 - i - 1;
                state = state.mirror(Mirror.FRONT_BACK);
            } else if (type == EditorTransform.Transform_MirrorY) {
                y1 = ySize2 - j - 1;
            } else if (type == EditorTransform.Transform_MirrorZ) {
                z1 = zSize2 - k - 1;
                state = state.mirror(Mirror.LEFT_RIGHT);
            }
            int index2 = x1 * ySizeF * zSizeF + y1 * zSizeF + z1;
            blocks[index2] = new BlockSet(state, set.nbt);
        });
        this.clipboard = new WorldSnapshot(Arrays.asList(blocks), new AABBInt(xSize, ySize, zSize));
        this.getEntity().setPos(EntityEditor.PASTE_BOX, xSize, ySize, zSize);
        this.getEntity().updateBlockList(this.clipboard.convertYPO());
    }

    public BlockSet getBlockSet(int x, int y, int z) {
        return BlockSet.getBlockSet(this.getWorld(), x, y, z, true);
    }

    public void setBlock(int px, int py, int pz, BlockSet blockSet, boolean syncClient) {
        Level world = this.getWorld();
        int flag = syncClient ? 3 : 2;
        BlockUtil.setBlock((ServerLevel) world, px, py, pz, blockSet.state, flag);
        if (blockSet.state.getBlock() != Blocks.AIR) {
            world.updateNeighborsAt(new BlockPos(px, py, pz), blockSet.state.getBlock());
        }
        if (blockSet.state.hasBlockEntity()) {
            BlockEntity tile = BlockUtil.getBlockEntity(world, px, py, pz);
            if (tile != null && blockSet.nbt != null) {
                this.setBlockEntityData(tile, blockSet.nbt, px, py, pz);
            }
        }
    }

    private void setBlockEntityData(BlockEntity tile, net.minecraft.nbt.CompoundTag nbt, int x, int y, int z) {
        if (nbt != null) {
            net.minecraft.nbt.CompoundTag nbt0 = nbt.copy();
            nbt0.putInt("x", x);
            nbt0.putInt("y", y);
            nbt0.putInt("z", z);
            tile.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, this.getWorld().registryAccess(), nbt0));
        }
    }

    public WorldSnapshot copy(AABBInt box, String options) {
        this.clipboard = new WorldSnapshot(this, box, options);
        this.getEntity().setPos(EntityEditor.PASTE_BOX, box.sizeX(), box.sizeY(), box.sizeZ());
        if (!options.contains("notSync")) {
            this.getEntity().updateBlockList(this.clipboard.convertYPO());
        }
        return this.clipboard;
    }

    public void loadData(NGTObject ngto) {
        this.clipboard = new WorldSnapshot(ngto);
        this.getEntity().setPos(EntityEditor.PASTE_BOX, ngto.xSize, ngto.ySize, ngto.zSize);
    }

    public void paste(AABBInt box, String options) {
        this.clipboard.setBlocks(this, box.minX, box.minY, box.minZ, options);
    }

    public void delete(AABBInt box, String options) {
        this.fill(box, BlockSet.AIR, options);
    }

    public void fill(AABBInt box, BlockSet blockSet, String options) {
        this.repeat(box, (box2, index, rep, x, y, z) -> {
            if (options.contains(WorldSnapshot.IGNORE_WATER) && this.getWorld().getFluidState(new BlockPos(x, y, z)).isSource()) {
                return;
            }
            this.setBlock(x, y, z, blockSet, true);
        }, 1);
        this.updateBlocks(box);
    }

    public void updateBlocks(AABBInt box) {
        int minCX = box.minX >> 4;
        int minCZ = box.minZ >> 4;
        int maxCX = (box.maxX >> 4) + 1;
        int maxCZ = (box.maxZ >> 4) + 1;
        Level level = this.getWorld();
        if (level instanceof ServerLevel server) {
            for (int cx = minCX; cx < maxCX; ++cx) {
                for (int cz = minCZ; cz < maxCZ; ++cz) {
                    LevelChunk chunk = server.getChunk(cx, cz);
                    ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                            chunk, server.getChunkSource().getLightEngine(), null, null);
                    server.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false)
                            .forEach(player -> player.connection.send(packet));
                }
            }
        }
    }

    public void repeat(AABBInt box, Repeatable repeater, int rep) {
        for (int i = 0; i < rep; ++i) {
            int i2 = i;
            box.repeat((x, y, z, count) -> repeater.processing(box, count, i2, x, y, z));
        }
    }

    public void record(AABBInt box) {
        this.history.push(new WorldSnapshot(this, box, ""));
    }

    public void undo() {
        WorldSnapshot snapshot = this.history.pop();
        if (snapshot != null) {
            snapshot.restore(this);
        }
    }

    public interface Repeatable {
        void processing(AABBInt box, int index, int rep, int x, int y, int z);
    }
}
