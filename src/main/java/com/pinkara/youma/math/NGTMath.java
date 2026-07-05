package com.pinkara.youma.math;

import net.minecraft.util.Mth;

import java.util.Random;

public final class NGTMath {
    public static final Random RANDOM = new Random();
    public static final float PI = (float) Math.PI;
    private static final double TO_RAD = Math.PI / 180.0;
    private static final double TO_DEG = 57.29577951308232;

    public static float toDegrees(float par1) {
        return par1 * (float) TO_DEG;
    }

    public static float toRadians(float par1) {
        return par1 * (float) TO_RAD;
    }

    public static double normalizeAngle(double par1) {
        double d0;
        for (d0 = par1; d0 >= 360.0; d0 -= 360.0) {
        }
        while (d0 < 0.0) {
            d0 += 360.0;
        }
        return d0;
    }

    public static float getSin(float par1) {
        return Mth.sin(par1);
    }

    public static float getCos(float par1) {
        return Mth.cos(par1);
    }

    public static float sin(float par1) {
        return Mth.sin(toRadians(par1));
    }

    public static float cos(float par1) {
        return Mth.cos(toRadians(par1));
    }

    public static int floor(float par1) {
        return Mth.floor(par1);
    }

    public static int floor(double par1) {
        return Mth.floor(par1);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }
}
