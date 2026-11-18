package myau.util;

import java.util.Random;

public class RandomUtil {
    private static final Random RANDOM = new Random();

    public static boolean nextBoolean() {
        return RANDOM.nextBoolean();
    }

    public static long nextLong(long min, long max) {
        return (long) nextDouble(min, max + 1L);
    }

    public static float nextFloat(float min, float max) {
        return RANDOM.nextFloat() * (max - min) + min;
    }

    public static double nextDouble(double min, double max) {
        return RANDOM.nextDouble() * (max - min) + min;
    }
}
