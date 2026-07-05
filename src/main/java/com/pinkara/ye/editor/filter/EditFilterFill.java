package com.pinkara.ye.editor.filter;

import com.pinkara.ye.YE;
import com.pinkara.ye.editor.Editor;
import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.math.AABBInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EditFilterFill extends EditFilterBase {
    @Override
    public String getFilterName() {
        return "Fill";
    }

    @Override
    public boolean edit(Editor editor) {
        AABBInt box = editor.getSelectBox();
        if (box == null) {
            return false;
        }
        editor.record(box);

        Player player = editor.getEntity().getPlayer();
        BlockState state = Blocks.AIR.defaultBlockState();
        if (player != null) {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() != YE.EDITOR.get()) {
                Block block = Block.byItem(held.getItem());
                if (block != null && block != Blocks.AIR) {
                    state = block.defaultBlockState();
                }
            }
        }

        BlockSet blockSet = new BlockSet(state, null);
        editor.fill(box, blockSet, "");
        return true;
    }
}
