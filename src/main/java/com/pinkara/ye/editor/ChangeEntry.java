package com.pinkara.ye.editor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ChangeEntry {
    public ItemStack fromItem = ItemStack.EMPTY;
    public ItemStack toItem = ItemStack.EMPTY;

    public ChangeEntry() {
    }

    public ChangeEntry(ItemStack from, ItemStack to) {
        this.fromItem = from.copy();
        this.toItem = to.copy();
    }

    public BlockState getFromState() {
        return itemToState(this.fromItem);
    }

    public BlockState getToState() {
        return itemToState(this.toItem);
    }

    public boolean isValid() {
        return !this.fromItem.isEmpty() && !this.toItem.isEmpty();
    }

    private static BlockState itemToState(ItemStack stack) {
        if (stack.isEmpty()) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        net.minecraft.world.level.block.Block block = net.minecraft.world.level.block.Block.byItem(stack.getItem());
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return block.defaultBlockState();
    }
}
