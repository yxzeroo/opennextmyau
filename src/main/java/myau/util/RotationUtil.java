package myau.util;

import myau.mixin.IAccessorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float wrapAngleDiff(float targetYaw, float currentYaw) {
        return currentYaw + MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);
    }

    public static float clampAngle(float delta, float maxStep) {
        maxStep = MathHelper.clamp_float(maxStep, 0.0f, 180.0f);
        if (delta > maxStep) {
            return maxStep;
        }
        if (delta < -maxStep) {
            return -maxStep;
        }
        return delta;
    }

    public static float smoothAngle(float delta, float smoothing) {
        return delta * (0.5f + 0.5f * (1.0f - MathHelper.clamp_float(smoothing, 0.0f, 1.0f)));
    }

    public static float quantizeAngle(float targetAngle, float currentAngle) {
        float delta = MathHelper.wrapAngleTo180_float(targetAngle - currentAngle);
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float gcdFactor = sensitivity * sensitivity * sensitivity * 8.0f;
        double quantizedDelta = (double) ((float) Math.round((double) delta / ((double) gcdFactor * 0.15)) * gcdFactor) * 0.15;
        return (float) ((double) currentAngle + quantizedDelta);
    }

    public static float[] getRotationsToBox(AxisAlignedBB box) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 center = getCenter(box);
        Vec3 clamped = clampVecToBox(eyePos, box);
        double deltaX = MathUtil.lerp(center.xCoord, clamped.xCoord, 0.875) - eyePos.xCoord;
        double deltaY = clamped.yCoord - eyePos.yCoord;
        double deltaZ = MathUtil.lerp(center.zCoord, clamped.zCoord, 0.875) - eyePos.zCoord;
        float[] rotations = getRotations(deltaX, deltaY, deltaZ);
        rotations[1] += 10.0f * MathHelper.clamp_float((float) ((eyePos.yCoord - center.yCoord) / (center.yCoord - box.minY)), -0.5f, 0.5f);
        return rotations;
    }

    public static float[] getRotations(double deltaX, double deltaY, double deltaZ) {
        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-Math.atan2(deltaY, horizontalDist) * 180.0 / Math.PI);
        return new float[]{yaw, pitch};
    }

    public static Vec3 getCenter(AxisAlignedBB box) {
        return new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);
    }

    public static Vec3 clampVecToBox(Vec3 vector, AxisAlignedBB box) {
        double[] coords = new double[]{vector.xCoord, vector.yCoord, vector.zCoord};
        double[] minCoords = new double[]{box.minX, box.minY, box.minZ};
        double[] maxCoords = new double[]{box.maxX, box.maxY, box.maxZ};
        for (int i = 0; i < 3; ++i) {
            if (coords[i] > maxCoords[i]) {
                coords[i] = maxCoords[i];
            } else if (coords[i] < minCoords[i]) {
                coords[i] = minCoords[i];
            }
        }
        return new Vec3(coords[0], coords[1], coords[2]);
    }

    public static double distanceToEntity(Entity entity) {
        float border = entity.getCollisionBorderSize();
        return distanceToBox(entity.getEntityBoundingBox().expand(border, border, border));
    }

    public static double distanceToEntity(Entity entity, Vec3 eyePos) {
        float border = entity.getCollisionBorderSize();
        return distanceToBox(entity.getEntityBoundingBox().expand(border, border, border), eyePos);
    }

    public static double distanceToBox(AxisAlignedBB box) {
        return distanceToBox(box, mc.thePlayer.getPositionEyes(1.0f));
    }

    public static double distanceToBox(AxisAlignedBB box, Vec3 eyePos) {
        if (box.isVecInside(eyePos)) {
            return 0.0;
        }
        Vec3 closest = clampVecToBox(eyePos, box);
        double dx = closest.xCoord - eyePos.xCoord;
        double dy = closest.yCoord - eyePos.yCoord;
        double dz = closest.zCoord - eyePos.zCoord;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static float angleToEntity(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float border = entity.getCollisionBorderSize();
        return angleToBox(entity.getEntityBoundingBox().expand(border, border, border), eyePos);
    }

    public static float angleToBox(AxisAlignedBB box, Vec3 eyePos) {
        if (box.isVecInside(eyePos)) {
            return 0.0f;
        }
        double reach = 30.0 + (box.maxX - box.minX) + (box.maxZ - box.minZ) + (box.maxY - box.minY);
        if (interceptBox(box, eyePos, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, reach) != null) {
            return 0.0f;
        }
        float[] targetRotations = getRotationsToBox(box);
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(targetRotations[0] - mc.thePlayer.rotationYaw));
        float pitchDiff = Math.abs(targetRotations[1] - mc.thePlayer.rotationPitch);
        return Math.max(yawDiff * 2.0f, pitchDiff * 2.0f);
    }

    public static float getYawBetween(double fromX, double fromZ, double toX, double toZ) {
        float yaw = (float) (Math.atan2(toZ - fromZ, toX - fromX) * 180.0 / Math.PI) - 90.0f;
        return MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
    }

    public static MovingObjectPosition rayTraceBlocks(float yaw, float pitch, double range, float partialTicks) {
        return rayTraceBlocks(mc.thePlayer.getPositionEyes(partialTicks), yaw, pitch, range);
    }

    public static MovingObjectPosition rayTraceBlocks(Vec3 eyePos, float yaw, float pitch, double range) {
        Vec3 lookVec = ((IAccessorEntity) mc.thePlayer).callGetVectorForRotation(pitch, yaw);
        Vec3 endVec = eyePos.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);
        return mc.theWorld.rayTraceBlocks(eyePos, endVec);
    }

    public static MovingObjectPosition rayTraceEntityBlocks(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float border = entity.getCollisionBorderSize();
        Vec3 closest = clampVecToBox(eyePos, entity.getEntityBoundingBox().expand(border, border, border));
        return mc.theWorld.rayTraceBlocks(eyePos, closest);
    }

    public static MovingObjectPosition interceptBox(AxisAlignedBB box, float yaw, float pitch, double range) {
        return interceptBox(box, mc.thePlayer.getPositionEyes(1.0f), yaw, pitch, range);
    }

    public static MovingObjectPosition interceptBox(AxisAlignedBB box, Vec3 eyePos, float yaw, float pitch, double range) {
        Vec3 lookVec = ((IAccessorEntity) mc.thePlayer).callGetVectorForRotation(pitch, yaw);
        Vec3 endVec = eyePos.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);
        return box.calculateIntercept(eyePos, endVec);
    }

    public static boolean isRayBlocked(AxisAlignedBB box, Vec3 eyePos, float yaw, float pitch, double range) {
        if (box.isVecInside(eyePos)) {
            return false;
        }
        MovingObjectPosition boxHit = interceptBox(box, eyePos, yaw, pitch, range);
        if (boxHit == null) {
            return true;
        }
        MovingObjectPosition blockHit = rayTraceBlocks(eyePos, yaw, pitch, range);
        if (blockHit == null) {
            return false;
        }
        return eyePos.distanceTo(boxHit.hitVec) > eyePos.distanceTo(blockHit.hitVec);
    }
}
