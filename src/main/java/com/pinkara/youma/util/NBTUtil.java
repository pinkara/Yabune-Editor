package com.pinkara.youma.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class NBTUtil {
    public static byte[] compress(CompoundTag data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(baos)))) {
            NbtIo.write(data, dos);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        // Read the byte array only after the GZIP stream has been closed,
        // otherwise the trailing GZIP block may be missing.
        return baos.toByteArray();
    }

    public static CompoundTag decompress(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(data))))) {
            return NbtIo.read(dis);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
