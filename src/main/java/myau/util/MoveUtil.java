package myau.util;

import myau.Myau;
import myau.management.RotationState;
import myau.module.modules.TargetStrafe;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class MoveUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isForwardPressed() {
        return mc.gameSettings.keyBindForward.isKeyDown() != mc.gameSettings.keyBindBack.isKeyDown()
            || mc.gameSettings.keyBindLeft.isKeyDown() != mc.gameSettings.keyBindRight.isKeyDown();
    }

    public static int getForwardValue() {
        int forward = 0;
        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            ++forward;
        }
        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            --forward;
        }
        return forward;
    }

    public static int getLeftValue() {
        int strafe = 0;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            ++strafe;
        }
        if (mc.gameSettings.keyBindRight.isKeyDown()) {
            --strafe;
        }
        return strafe;
    }

    public static float getDirectionYaw() {
        return getDirectionYaw(
            RotationState.isRotated() ? RotationState.getMoveYaw() : mc.thePlayer.rotationYaw,
            mc.thePlayer.movementInput.moveForward,
            mc.thePlayer.movementInput.moveStrafe
        );
    }

    public static float getDirectionYaw(float yaw, float forward, float strafe) {
        TargetStrafe targetStrafe = (TargetStrafe) Myau.moduleManager.modules.get(TargetStrafe.class);
        if (targetStrafe.isEnabled() && !Float.isNaN(targetStrafe.getTargetYaw())) {
            return targetStrafe.getTargetYaw();
        }
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        if (strafe != 0.0f) {
            float multiplier = forward == 0.0f ? 1.0f : 0.5f * Math.signum(forward);
            yaw += -90.0f * multiplier * Math.signum(strafe);
        }
        return MathHelper.wrapAngleTo180_float(yaw);
    }

    public static float getMoveYaw() {
        if (getSpeed() == 0.0) {
            return MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        }
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0f);
    }

    public static double getBaseMoveSpeed() {
        double speed = 0.28015;
        if (getSpeedTime() > 0) {
            speed *= 1.0 + 0.15 * (double) getSpeedEffect();
        }
        return speed;
    }

    public static double getJumpMotion(int speedAmplifier) {
        double speed = 0.452;
        if (speedAmplifier == 1) {
            speed *= 1.1;
        } else if (speedAmplifier >= 2) {
            speed *= 1.2;
        }
        return speed;
    }

    public static double getSpeedJumpMotion() {
        int level = getSpeedTime() > 0 ? getSpeedEffect() : 0;
        return getJumpMotion(level);
    }

    public static double getSpeed() {
        return getSpeed(mc.thePlayer.motionX, mc.thePlayer.motionZ);
    }

    public static double getSpeed(double motionX, double motionZ) {
        return Math.hypot(motionX, motionZ);
    }

    public static void setSpeed(double speed) {
        setSpeed(speed, getMoveYaw());
    }

    public static void setSpeed(double speed, float yaw) {
        mc.thePlayer.motionX = -Math.sin(Math.toRadians(yaw)) * speed;
        mc.thePlayer.motionZ = Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static void addSpeed(double speed, float yaw) {
        mc.thePlayer.motionX += -Math.sin(Math.toRadians(yaw)) * speed;
        mc.thePlayer.motionZ += Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static int getSpeedEffect() {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
        }
        return 0;
    }

    public static int getSpeedTime() {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration();
        }
        return 0;
    }

    public static float getFrictionFactor() {
        BlockPos underPos = new BlockPos(
            MathHelper.floor_double(mc.thePlayer.posX),
            MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY) - 1,
            MathHelper.floor_double(mc.thePlayer.posZ)
        );
        float friction = mc.thePlayer.worldObj.getBlockState(underPos).getBlock().slipperiness * 0.91f;
        return mc.thePlayer.getAIMoveSpeed() * (0.16277136f / (friction * friction * friction));
    }

    public static double[] predictMovement() {
        float strafe = (float) getLeftValue() * 0.98f;
        float forward = (float) getForwardValue() * 0.98f;
        float magnitudeSq = strafe * strafe + forward * forward;
        if (magnitudeSq >= 1.0E-4f) {
            float magnitude = Math.max(MathHelper.sqrt_float(magnitudeSq), 1.0f);
            float scale = getFrictionFactor() / magnitude;
            strafe *= scale;
            forward *= scale;
            float sin = MathHelper.sin(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            float cos = MathHelper.cos(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            return new double[]{strafe * cos - forward * sin, forward * cos + strafe * sin};
        }
        return new double[]{0.0, 0.0};
    }

    public static void fixStrafe(float targetYaw) {
        float angle = MathHelper.wrapAngleTo180_float(getDirectionYaw(mc.thePlayer.rotationYaw, getForwardValue(), getLeftValue()) - targetYaw + 22.5f);
        switch ((int) (angle + 180.0f) / 45 % 8) {
            case 0:
                mc.thePlayer.movementInput.moveForward = -1.0f;
                mc.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            case 1:
                mc.thePlayer.movementInput.moveForward = -1.0f;
                mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            case 2:
                mc.thePlayer.movementInput.moveForward = 0.0f;
                mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            case 3:
                mc.thePlayer.movementInput.moveForward = 1.0f;
                mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            case 4:
                mc.thePlayer.movementInput.moveForward = 1.0f;
                mc.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            case 5:
                mc.thePlayer.movementInput.moveForward = 1.0f;
                mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            case 6:
                mc.thePlayer.movementInput.moveForward = 0.0f;
                mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            case 7:
                mc.thePlayer.movementInput.moveForward = -1.0f;
                mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
        }
        if (mc.thePlayer.movementInput.sneak) {
            mc.thePlayer.movementInput.moveForward *= 0.3f;
            mc.thePlayer.movementInput.moveStrafe *= 0.3f;
        }
    }
}
