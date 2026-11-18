package myau.event.events;

import myau.event.Event;
import net.minecraft.util.MathHelper;

public class UpdateEvent extends Event {
    private final float lastYaw;
    private final float lastPitch;
    private float yaw;
    private float pitch;
    private float moveYaw;
    private int rotationPriority = -1;
    private int movePriority = -1;
    private boolean rotated = false;

    public UpdateEvent(float lastYaw, float lastPitch, float yaw, float pitch) {
        this.lastYaw = lastYaw;
        this.lastPitch = lastPitch;
        this.yaw = yaw;
        this.pitch = pitch;
        this.moveYaw = yaw;
    }

    public float getLastYaw() {
        return this.lastYaw;
    }

    public float getLastPitch() {
        return this.lastPitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getMoveYaw() {
        return this.moveYaw;
    }

    public int getPriority() {
        return this.movePriority;
    }

    public boolean isRotated() {
        return this.rotated;
    }

    public void setRotation(float yaw, float pitch, int priority) {
        if (this.rotationPriority <= priority) {
            this.yaw = yaw;
            this.pitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);
            this.rotationPriority = priority;
            this.rotated = true;
        }
    }

    public void setMoveYaw(float moveYaw, int priority) {
        if (this.movePriority < priority) {
            this.moveYaw = moveYaw;
            this.movePriority = priority;
            this.rotated = true;
        }
    }
}
