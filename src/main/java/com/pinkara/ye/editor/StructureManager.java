package com.pinkara.ye.editor;

import com.pinkara.youma.block.NGTObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StructureManager {
    public static final StructureManager INSTANCE = new StructureManager();

    private final Path structuresDir;

    private StructureManager() {
        this.structuresDir = FMLPaths.GAMEDIR.get().resolve("yabune_editor").resolve("structures");
        try {
            Files.createDirectories(this.structuresDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Path getStructuresDir() {
        return this.structuresDir;
    }

    public List<String> listStructures() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(this.structuresDir)) {
            return names;
        }
        try (var stream = Files.newDirectoryStream(this.structuresDir, "*.nbt")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                names.add(fileName.substring(0, fileName.length() - 4));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public boolean save(String name, NGTObject ngto) {
        if (ngto == null) {
            return false;
        }
        String safeName = sanitizeName(name);
        if (safeName.isEmpty()) {
            return false;
        }
        try {
            Files.createDirectories(this.structuresDir);
            CompoundTag tag = ngto.writeToNBT(false); // uncompressed for reliability
            NbtIo.write(tag, this.structuresDir.resolve(safeName + ".nbt"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public NGTObject load(String name) {
        String safeName = sanitizeName(name);
        if (safeName.isEmpty()) {
            return null;
        }
        Path file = this.structuresDir.resolve(safeName + ".nbt");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            CompoundTag tag = NbtIo.read(file);
            if (tag == null) {
                return null;
            }
            return NGTObject.readFromNBT(tag);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean delete(String name) {
        String safeName = sanitizeName(name);
        if (safeName.isEmpty()) {
            return false;
        }
        try {
            return Files.deleteIfExists(this.structuresDir.resolve(safeName + ".nbt"));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
