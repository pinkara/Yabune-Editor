package com.pinkara.ye.editor;

import com.pinkara.youma.block.BlockSet;
import com.pinkara.youma.math.AABBInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MeshExporter {
    public enum Format {
        OBJ,
        STL
    }

    public static boolean export(AABBInt box, Level level, Path path, Format format) {
        if (box == null || level == null) return false;
        try {
            Files.createDirectories(path.getParent());
            String content = format == Format.OBJ ? exportObj(box, level) : exportStl(box, level);
            Files.writeString(path, content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String exportObj(AABBInt box, Level level) {
        StringBuilder sb = new StringBuilder();
        sb.append("# YE OBJ export\n");
        sb.append("# ").append(box.sizeX()).append(" x ").append(box.sizeY()).append(" x ").append(box.sizeZ()).append("\n");

        int vertexOffset = 1;
        for (int y = box.minY; y < box.maxY; ++y) {
            for (int z = box.minZ; z < box.maxZ; ++z) {
                for (int x = box.minX; x < box.maxX; ++x) {
                    if (isAir(level, x, y, z)) continue;
                    int faces = visibleFaces(box, level, x, y, z);
                    if (faces == 0) continue;
                    float fx = x - box.minX;
                    float fy = y - box.minY;
                    float fz = z - box.minZ;
                    vertexOffset = appendCubeObj(sb, fx, fy, fz, faces, vertexOffset);
                }
            }
        }
        return sb.toString();
    }

    private static String exportStl(AABBInt box, Level level) {
        StringBuilder sb = new StringBuilder();
        sb.append("solid YE\n");
        for (int y = box.minY; y < box.maxY; ++y) {
            for (int z = box.minZ; z < box.maxZ; ++z) {
                for (int x = box.minX; x < box.maxX; ++x) {
                    if (isAir(level, x, y, z)) continue;
                    int faces = visibleFaces(box, level, x, y, z);
                    if (faces == 0) continue;
                    float fx = x - box.minX;
                    float fy = y - box.minY;
                    float fz = z - box.minZ;
                    appendCubeStl(sb, fx, fy, fz, faces);
                }
            }
        }
        sb.append("endsolid YE\n");
        return sb.toString();
    }

    private static boolean isAir(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
        return state.isAir();
    }

    private static int visibleFaces(AABBInt box, Level level, int x, int y, int z) {
        int mask = 0;
        if (x <= box.minX || isAir(level, x - 1, y, z)) mask |= 1;      // west
        if (x >= box.maxX - 1 || isAir(level, x + 1, y, z)) mask |= 2;  // east
        if (y <= box.minY || isAir(level, x, y - 1, z)) mask |= 4;      // bottom
        if (y >= box.maxY - 1 || isAir(level, x, y + 1, z)) mask |= 8;  // top
        if (z <= box.minZ || isAir(level, x, y, z - 1)) mask |= 16;     // north
        if (z >= box.maxZ - 1 || isAir(level, x, y, z + 1)) mask |= 32; // south
        return mask;
    }

    private static int appendCubeObj(StringBuilder sb, float x, float y, float z, int faces, int offset) {
        int start = offset;
        // vertices
        sb.append("v ").append(x).append(' ').append(y).append(' ').append(z).append('\n');
        sb.append("v ").append(x + 1).append(' ').append(y).append(' ').append(z).append('\n');
        sb.append("v ").append(x + 1).append(' ').append(y + 1).append(' ').append(z).append('\n');
        sb.append("v ").append(x).append(' ').append(y + 1).append(' ').append(z).append('\n');
        sb.append("v ").append(x).append(' ').append(y).append(' ').append(z + 1).append('\n');
        sb.append("v ").append(x + 1).append(' ').append(y).append(' ').append(z + 1).append('\n');
        sb.append("v ").append(x + 1).append(' ').append(y + 1).append(' ').append(z + 1).append('\n');
        sb.append("v ").append(x).append(' ').append(y + 1).append(' ').append(z + 1).append('\n');
        // faces (counter-clockwise from outside)
        if ((faces & 1) != 0) { sb.append("f ").append(start).append(' ').append(start + 3).append(' ').append(start + 7).append(' ').append(start + 4).append('\n'); } // west
        if ((faces & 2) != 0) { sb.append("f ").append(start + 1).append(' ').append(start + 5).append(' ').append(start + 6).append(' ').append(start + 2).append('\n'); } // east
        if ((faces & 4) != 0) { sb.append("f ").append(start).append(' ').append(start + 4).append(' ').append(start + 5).append(' ').append(start + 1).append('\n'); } // bottom
        if ((faces & 8) != 0) { sb.append("f ").append(start + 3).append(' ').append(start + 2).append(' ').append(start + 6).append(' ').append(start + 7).append('\n'); } // top
        if ((faces & 16) != 0) { sb.append("f ").append(start).append(' ').append(start + 1).append(' ').append(start + 2).append(' ').append(start + 3).append('\n'); } // north
        if ((faces & 32) != 0) { sb.append("f ").append(start + 4).append(' ').append(start + 7).append(' ').append(start + 6).append(' ').append(start + 5).append('\n'); } // south
        return start + 8;
    }

    private static void appendCubeStl(StringBuilder sb, float x, float y, float z, int faces) {
        float x1 = x, x2 = x + 1;
        float y1 = y, y2 = y + 1;
        float z1 = z, z2 = z + 1;
        if ((faces & 1) != 0) appendQuadStl(sb, -1, 0, 0, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2);
        if ((faces & 2) != 0) appendQuadStl(sb, 1, 0, 0, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1);
        if ((faces & 4) != 0) appendQuadStl(sb, 0, -1, 0, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        if ((faces & 8) != 0) appendQuadStl(sb, 0, 1, 0, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        if ((faces & 16) != 0) appendQuadStl(sb, 0, 0, -1, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        if ((faces & 32) != 0) appendQuadStl(sb, 0, 0, 1, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
    }

    private static void appendQuadStl(StringBuilder sb, float nx, float ny, float nz,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float x3, float y3, float z3,
                                      float x4, float y4, float z4) {
        appendTriangleStl(sb, nx, ny, nz, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        appendTriangleStl(sb, nx, ny, nz, x1, y1, z1, x3, y3, z3, x4, y4, z4);
    }

    private static void appendTriangleStl(StringBuilder sb, float nx, float ny, float nz,
                                          float x1, float y1, float z1,
                                          float x2, float y2, float z2,
                                          float x3, float y3, float z3) {
        sb.append("facet normal ").append(nx).append(' ').append(ny).append(' ').append(nz).append('\n');
        sb.append("  outer loop\n");
        sb.append("    vertex ").append(x1).append(' ').append(y1).append(' ').append(z1).append('\n');
        sb.append("    vertex ").append(x2).append(' ').append(y2).append(' ').append(z2).append('\n');
        sb.append("    vertex ").append(x3).append(' ').append(y3).append(' ').append(z3).append('\n');
        sb.append("  endloop\n");
        sb.append("endfacet\n");
    }
}
