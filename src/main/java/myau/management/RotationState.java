package myau.management;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationState {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static int ticksSinceRotation = -1;
    private static float prevRenderYawOffset;
    private static float renderYawOffset;
    private static float prevRotationYawHead;
    private static float rotationYawHead;
    private static float prevRotationPitch;
    private static float rotationPitch;
    private static float moveYaw;
    private static int priority;

    private static float calculateRenderYawOffset(float targetYaw, float currentOffset) {
        float offset = currentOffset;
        double deltaX = mc.thePlayer.posX - mc.thePlayer.prevPosX;
        double deltaZ = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
        if ((float) (deltaX * deltaX + deltaZ * deltaZ) > 0.0025000002f) {
            offset = (float) MathHelper.atan2(deltaZ, deltaX) * 180.0f / (float) Math.PI - 90.0f;
        }
        if (mc.thePlayer.swingProgress > 0.0f) {
            offset = targetYaw;
        }
        float bodyStep = MathHelper.wrapAngleTo180_float(offset - currentOffset);
        currentOffset += bodyStep * 0.3f;
        float headDiff = MathHelper.wrapAngleTo180_float(targetYaw - currentOffset);
        if (headDiff < -75.0f) {
            headDiff = -75.0f;
        }
        if (headDiff >= 75.0f) {
            headDiff = 75.0f;
        }
        offset = targetYaw - headDiff;
        if (headDiff * headDiff > 2500.0f) {
            offset += headDiff * 0.2f;
        }
        return offset;
    }

    public static void applyState(boolean rotated, float yaw, float pitch, float targetMoveYaw, int movePriority) {
        ticksSinceRotation = rotated ? 0 : ticksSinceRotation + 1;
        prevRenderYawOffset = renderYawOffset;
        renderYawOffset = rotated ? calculateRenderYawOffset(yaw, renderYawOffset) : mc.thePlayer.renderYawOffset;
        prevRotationYawHead = rotationYawHead;
        rotationYawHead = rotated ? yaw : mc.thePlayer.rotationYawHead;
        prevRotationPitch = rotationPitch;
        rotationPitch = rotated ? pitch : mc.thePlayer.rotationPitch;
        moveYaw = targetMoveYaw;
        priority = movePriority;
    }

    public static boolean isRotated() {
        return isRotated(0);
    }

    public static boolean isRotated(int maxTicks) {
        return ticksSinceRotation >= 0 && ticksSinceRotation <= maxTicks;
    }

    public static float getPrevRenderYawOffset() {
        return prevRenderYawOffset;
    }

    public static float getRenderYawOffset() {
        return renderYawOffset;
    }

    public static float getPrevRotationYawHead() {
        return prevRotationYawHead;
    }

    public static float getRotationYawHead() {
        return rotationYawHead;
    }

    public static float getPrevRotationPitch() {
        return prevRotationPitch;
    }

    public static float getRotationPitch() {
        return rotationPitch;
    }

    public static float getMoveYaw() {
        return moveYaw;
    }

    public static float getPriority() {
        return priority;
    }
}
