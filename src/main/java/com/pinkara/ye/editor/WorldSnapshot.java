package com.pinkara.ye.editor;

import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.block.NGTObject;
import com.pinkara.youma.io.NGTLog;
import com.pinkara.youma.math.AABBInt;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldSnapshot {
    public static final String IGNORE_AIR = "IgnoreAir";
    public static final String IGNORE_WATER = "IgnoreWater";
    private final List<BlockSet> blockList = new ArrayList<>();
    private final AABBInt origBox;
    private final boolean hasOrigPos;

    public WorldSnapshot(NGTObject ngto) {
        this.blockList.addAll(ngto.blockList);
        this.origBox = new AABBInt(ngto.origX, ngto.origY, ngto.origZ, ngto.origX + ngto.xSize, ngto.origY + ngto.ySize, ngto.origZ + ngto.zSize);
        this.hasOrigPos = false;
    }

    public WorldSnapshot(List<BlockSet> list, AABBInt box) {
        this.blockList.addAll(list);
        this.origBox = box;
        this.hasOrigPos = false;
    }

    public WorldSnapshot(Editor editor, AABBInt box, String options) {
        this.save(editor, box, options);
        this.origBox = box;
        this.hasOrigPos = true;
    }

    private void save(Editor editor, AABBInt box, String options) {
        NGTLog.startTimer();
        editor.repeat(box, (box2, index, rep, x, y, z) -> {
            BlockSet blockSet = editor.getBlockSet(x, y, z);
            if (options.contains(IGNORE_WATER) && blockSet.state.getFluidState().isSource()) {
                blockSet = BlockSet.AIR;
            }
            this.blockList.add(blockSet);
        }, 1);
        NGTLog.stopTimer("save snapshot");
    }

    public void restore(Editor editor) {
        if (this.hasOrigPos) {
            for (BlockSet blockSet : this.blockList) {
                editor.setBlock(blockSet.x, blockSet.y, blockSet.z, blockSet, false);
            }
            editor.updateBlocks(this.origBox);
        }
    }

    public void setBlocks(Editor editor, int x, int y, int z, String options) {
        AABBInt box = new AABBInt(x, y, z, x + this.origBox.sizeX(), y + this.origBox.sizeY(), z + this.origBox.sizeZ());
        editor.repeat(box, (rbox, index, rep, rx, ry, rz) -> {
            BlockSet blockSet = this.blockList.get(index);
            if (options.contains(IGNORE_AIR) && blockSet.state.getBlock() == Blocks.AIR) {
                return;
            }
            editor.setBlock(rx, ry, rz, blockSet, true);
        }, 1);
        editor.updateBlocks(box);
    }

    public NGTObject convertYPO() {
        return NGTObject.createYPO(this.blockList, this.origBox.sizeX(), this.origBox.sizeY(), this.origBox.sizeZ(), this.origBox.minX, this.origBox.minY, this.origBox.minZ);
    }

    public int getSize() {
        return this.blockList.size();
    }

    public List<BlockSet> getBlocks() {
        return this.blockList;
    }
}
