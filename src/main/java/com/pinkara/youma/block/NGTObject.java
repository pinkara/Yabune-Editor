package com.pinkara.youma.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import com.pinkara.youma.util.NBTUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NGTObject {
    private static final List<NGTObject> LOADED_YPO = new ArrayList<>();
    public long objId;
    public List<BlockSet> blockList;
    public int xSize;
    public int ySize;
    public int zSize;
    public int origX;
    public int origY;
    public int origZ;

    public static NGTObject createYPO(List<BlockSet> blocks, int w, int h, int d, int x, int y, int z) {
        return createYPO(System.currentTimeMillis(), blocks, w, h, d, x, y, z);
    }

    public static NGTObject createYPO(long id, List<BlockSet> blocks, int w, int h, int d, int x, int y, int z) {
        NGTObject ngto = new NGTObject(id, blocks, w, h, d, x, y, z);
        int index = LOADED_YPO.indexOf(ngto);
        if (index >= 0) {
            return LOADED_YPO.get(index);
        }
        return ngto;
    }

    private NGTObject(long id, List<BlockSet> blocks, int w, int h, int d, int x, int y, int z) {
        this.objId = id;
        this.blockList = blocks;
        this.xSize = w;
        this.ySize = h;
        this.zSize = d;
        this.origX = x;
        this.origY = y;
        this.origZ = z;
        LOADED_YPO.add(this);
    }

    public boolean isValidPos(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < this.xSize && y < this.ySize && z < this.zSize;
    }

    public BlockSet getBlockSet(int x, int y, int z) {
        int index;
        if (this.isValidPos(x, y, z) && (index = x * this.ySize * this.zSize + y * this.zSize + z) < this.blockList.size()) {
            return this.blockList.get(index);
        }
        return BlockSet.AIR;
    }

    public CompoundTag writeToNBT() {
        return this.writeToNBT(true);
    }

    public CompoundTag writeToNBT(boolean compress) {
        Map<BlockSet, Integer> idMap = new HashMap<>();
        idMap.put(BlockSet.AIR, 0);
        int idCount = 1;
        CompoundTag nbts = new CompoundTag();
        int[] blockIds = new int[this.blockList.size()];
        for (int i = 0; i < this.blockList.size(); ++i) {
            BlockSet set = this.blockList.get(i).asKey();
            Integer val = idMap.get(set);
            if (val == null) {
                val = idCount;
                idMap.put(set, val);
                ++idCount;
            }
            blockIds[i] = val;
            if (set.hasNBT()) {
                nbts.put(String.valueOf(i), set.nbt);
            }
        }
        CompoundTag data = new CompoundTag();
        if (idCount > 255) {
            data.putIntArray("IData", blockIds);
        } else {
            byte[] bytes = new byte[blockIds.length];
            for (int i = 0; i < bytes.length; ++i) {
                bytes[i] = (byte) (blockIds[i] - 128);
            }
            data.putByteArray("BData", bytes);
        }
        data.put("NBTs", nbts);
        ListTag tagList2 = new ListTag();
        for (Map.Entry<BlockSet, Integer> set : idMap.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.put("Set", set.getKey().writeToNBT());
            tag.putInt("Id", set.getValue());
            tagList2.add(tag);
        }
        data.put("IdList", tagList2);
        data.putInt("SizeX", this.xSize);
        data.putInt("SizeY", this.ySize);
        data.putInt("SizeZ", this.zSize);
        data.putInt("OrigX", this.origX);
        data.putInt("OrigY", this.origY);
        data.putInt("OrigZ", this.origZ);
        data.putLong("ObjId", this.objId);
        return compress ? compress(data) : data;
    }

    public static NGTObject readFromNBT(CompoundTag data) {
        if (data.contains("ByteData")) {
            data = decompress(data);
        }
        Map<Integer, BlockSet> idMap = new HashMap<>();
        idMap.put(0, BlockSet.AIR);
        ListTag tagList2 = data.getList("IdList").orElse(new ListTag());
        for (int i = 0; i < tagList2.size(); ++i) {
            CompoundTag tag = tagList2.getCompoundOrEmpty(i);
            BlockSet set = BlockSet.readFromNBT(tag.getCompound("Set").orElse(new CompoundTag()));
            int id = tag.getInt("Id").orElse(0);
            idMap.put(id, set);
        }
        List<BlockSet> list = new ArrayList<>();
        int[] ids;
        if (data.contains("IData")) {
            ids = data.getIntArray("IData").orElse(new int[0]);
        } else {
            byte[] bytes = data.getByteArray("BData").orElse(new byte[0]);
            ids = new int[bytes.length];
            for (int i = 0; i < bytes.length; ++i) {
                ids[i] = bytes[i] + 128;
            }
        }
        CompoundTag nbts = data.getCompound("NBTs").orElse(new CompoundTag());
        for (int i = 0; i < ids.length; ++i) {
            int id = ids[i];
            BlockSet set = idMap.getOrDefault(id, BlockSet.AIR);
            if (nbts.contains(String.valueOf(i))) {
                list.add(set.setNBT(nbts.getCompound(String.valueOf(i)).orElse(new CompoundTag())));
            } else {
                list.add(set);
            }
        }
        int x = data.getInt("SizeX").orElse(0);
        int y = data.getInt("SizeY").orElse(0);
        int z = data.getInt("SizeZ").orElse(0);
        int ox = data.getInt("OrigX").orElse(0);
        int oy = data.getInt("OrigY").orElse(0);
        int oz = data.getInt("OrigZ").orElse(0);
        long objId = data.getLong("ObjId").orElse(0L);
        return createYPO(objId, list, x, y, z, ox, oy, oz);
    }

    private static CompoundTag compress(CompoundTag data) {
        byte[] compressedData = NBTUtil.compress(data);
        if (compressedData != null) {
            CompoundTag nbt = new CompoundTag();
            nbt.putByteArray("ByteData", compressedData);
            return nbt;
        }
        return data;
    }

    private static CompoundTag decompress(CompoundTag data) {
        byte[] compressedData = data.getByteArray("ByteData").orElse(new byte[0]);
        CompoundTag decData = NBTUtil.decompress(compressedData);
        return decData != null ? decData : data;
    }

    @Override
    public int hashCode() {
        return this.xSize << 20 & 0x400 | this.ySize << 10 & 0x400 | this.zSize & 0x400;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NGTObject ngto) {
            if (ngto.xSize == this.xSize && ngto.ySize == this.ySize && ngto.zSize == this.zSize) {
                for (int i = 0; i < this.blockList.size(); ++i) {
                    BlockSet set0 = this.blockList.get(i);
                    BlockSet set1 = ngto.blockList.get(i);
                    if (set0.equals(set1)) continue;
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
