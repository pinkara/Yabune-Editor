package com.pinkara.youma.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockUtil {
    public static final int[][] FACING = new int[][]{{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    public static BlockState getBlockState(LevelAccessor world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z));
    }

    public static Block getBlock(LevelAccessor world, int x, int y, int z) {
        return getBlockState(world, x, y, z).getBlock();
    }

    public static BlockEntity getBlockEntity(LevelAccessor world, int x, int y, int z) {
        return world.getBlockEntity(new BlockPos(x, y, z));
    }

    public static boolean isAir(LevelAccessor world, int x, int y, int z) {
        return getBlock(world, x, y, z) == Blocks.AIR;
    }

    public static boolean setBlock(Level level, int x, int y, int z, BlockState state, int flag) {
        return level.setBlock(new BlockPos(x, y, z), state, flag);
    }

    public static void markBlockForUpdate(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 2);
    }

    public static int[] toArray(BlockPos pos) {
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    public static HitResult getMOPFromPlayer(Player player, double distance, boolean liquid) {
        float f = 1.0f;
        float pitch = Mth.lerp(f, player.xRotO, player.getXRot());
        float yaw = Mth.lerp(f, player.yRotO, player.getYRot());
        double x = Mth.lerp(f, player.xo, player.getX());
        double y = Mth.lerp(f, player.yo, player.getY()) + player.getEyeHeight();
        double z = Mth.lerp(f, player.zo, player.getZ());
        Vec3 vec3 = new Vec3(x, y, z);
        float f3 = Mth.cos(-yaw * ((float) Math.PI / 180) - (float) Math.PI);
        float f4 = Mth.sin(-yaw * ((float) Math.PI / 180) - (float) Math.PI);
        float f5 = -Mth.cos(-pitch * ((float) Math.PI / 180));
        float f6 = Mth.sin(-pitch * ((float) Math.PI / 180));
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        Vec3 vec31 = vec3.add((double) f7 * distance, (double) f6 * distance, (double) f8 * distance);
        return player.level().clip(new net.minecraft.world.level.ClipContext(vec3, vec31,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                liquid ? net.minecraft.world.level.ClipContext.Fluid.ANY : net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
    }

    public static BlockPos offsetByDirection(BlockPos pos, Direction side) {
        return pos.relative(side);
    }
}
