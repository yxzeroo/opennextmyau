package myau.util;

public class MathUtil {
    public static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    public static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    public static float toRadians(float degrees) {
        return degrees / 180.0f * (float) Math.PI;
    }
}
