package com.pinkara.youma.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public final class ItemUtil {
    public static ItemStack[] getEmptyArray(int cap) {
        ItemStack[] array = new ItemStack[cap];
        Arrays.fill(array, ItemStack.EMPTY);
        return array;
    }

    public static BlockPos getPlacePos(BlockPos pos, Direction side) {
        return pos.relative(side);
    }
}
