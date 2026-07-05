package com.pinkara.youma.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockSet implements Comparable<BlockSet> {
    public static final BlockSet AIR = new BlockSet(BlockPos.ZERO, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), null);

    public final int x;
    public final int y;
    public final int z;
    public final BlockState state;
    public final CompoundTag nbt;

    public BlockSet(BlockState state) {
        this(BlockPos.ZERO, state, null);
    }

    public BlockSet(BlockState state, CompoundTag nbt) {
        this(BlockPos.ZERO, state, nbt);
    }

    public BlockSet(int x, int y, int z, BlockState state) {
        this(new BlockPos(x, y, z), state, null);
    }

    public BlockSet(BlockPos pos, BlockState state, CompoundTag nbt) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.state = state;
        this.nbt = nbt;
    }

    public boolean hasNBT() {
        return this.nbt != null;
    }

    public BlockSet setNBT(CompoundTag nbt) {
        return new BlockSet(new BlockPos(this.x, this.y, this.z), this.state, nbt);
    }

    public static BlockSet readFromNBT(CompoundTag nbt) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(nbt.getString("Block")));
        if (block == null) {
            return AIR;
        }
        BlockState state = block.defaultBlockState();
        if (nbt.contains("Properties", 10)) {
            CompoundTag props = nbt.getCompound("Properties");
            for (String key : props.getAllKeys()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
                if (property != null) {
                    state = setValue(state, property, props.getString(key));
                }
            }
        } else if (nbt.contains("Meta")) {
            // Legacy fallback for old metadata based saves
            int meta = nbt.getInt("Meta");
            state = getStateFromLegacyMeta(block, meta);
        }
        CompoundTag tagData = nbt.contains("TagData", 10) ? nbt.getCompound("TagData") : null;
        return new BlockSet(state, tagData);
    }

    private static BlockState getStateFromLegacyMeta(Block block, int meta) {
        try {
            return block.defaultBlockState();
        } catch (Exception e) {
            return block.defaultBlockState();
        }
    }

    private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    public CompoundTag writeToNBT() {
        CompoundTag nbt = new CompoundTag();
        ResourceLocation name = BuiltInRegistries.BLOCK.getKey(this.state.getBlock());
        if (name == null) {
            return nbt;
        }
        nbt.putString("Block", name.toString());
        CompoundTag props = new CompoundTag();
        for (var entry : this.state.getValues().entrySet()) {
            props.putString(entry.getKey().getName(), entry.getValue().toString());
        }
        if (!props.isEmpty()) {
            nbt.put("Properties", props);
        }
        if (this.nbt != null) {
            nbt.put("TagData", this.nbt.copy());
        }
        return nbt;
    }

    public BlockPos toBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public static BlockSet getBlockSet(LevelAccessor world, int x, int y, int z, boolean savePos) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        CompoundTag nbt = null;
        if (state.hasBlockEntity()) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile != null) {
                nbt = tile.saveWithFullMetadata(world instanceof net.minecraft.world.level.Level lvl ? lvl.registryAccess() : RegistryAccess.EMPTY);
            }
        }
        if (savePos) {
            return new BlockSet(pos, state, nbt);
        }
        return new BlockSet(BlockPos.ZERO, state, nbt);
    }

    public BlockSet asKey() {
        return new BlockSet(BlockPos.ZERO, this.state, this.nbt);
    }

    @Override
    public boolean equals(Object par1) {
        if (par1 instanceof BlockSet bs) {
            boolean flagState = this.state == bs.state;
            boolean flagNBT = this.nbt != null && bs.nbt != null ? this.nbt.equals(bs.nbt) : true;
            if (this.y < 0 && bs.y < 0) {
                return flagState && flagNBT;
            }
            return bs.x == this.x && bs.y == this.y && bs.z == this.z && flagState && flagNBT;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return BuiltInRegistries.BLOCK.getId(this.state.getBlock());
    }

    @Override
    public int compareTo(BlockSet obj) {
        return BuiltInRegistries.BLOCK.getId(this.state.getBlock()) - BuiltInRegistries.BLOCK.getId(obj.state.getBlock());
    }
}
