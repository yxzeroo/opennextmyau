package myau.management;

import myau.event.EventTarget;
import myau.event.events.Render3DEvent;
import myau.event.events.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private float lastStep = Float.NaN;
    private float yawDelta = Float.NaN;
    private float pitchDelta = Float.NaN;
    private int priority = Integer.MIN_VALUE;
    private boolean mouseLocked = false;

    private void applyStep(float step) {
        if (mc.thePlayer == null || Float.isNaN(this.yawDelta) || Float.isNaN(this.pitchDelta) || Float.isNaN(this.lastStep)) {
            return;
        }
        float yawChange = this.yawDelta * (step - this.lastStep);
        if (yawChange != 0.0f) {
            mc.thePlayer.prevRotationYaw = mc.thePlayer.rotationYaw;
            mc.thePlayer.rotationYaw += yawChange;
        }
        float pitchChange = this.pitchDelta * (step - this.lastStep);
        if (pitchChange != 0.0f) {
            mc.thePlayer.prevRotationPitch = mc.thePlayer.rotationPitch;
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchChange, -90.0f, 90.0f);
        }
        this.lastStep = step;
    }

    private void resetState() {
        this.lastStep = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.priority = Integer.MIN_VALUE;
        this.mouseLocked = false;
    }

    public void setRotation(float targetYaw, float targetPitch, int priority, boolean lockMouse) {
        if (this.priority > priority) {
            return;
        }
        this.priority = priority;
        this.yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
        this.pitchDelta = targetPitch - mc.thePlayer.rotationPitch;
        this.lastStep = 0.0f;
        this.mouseLocked = lockMouse;
    }

    public boolean isMouseLocked() {
        return this.mouseLocked;
    }

    @EventTarget(priority = 100)
    public void onTick(TickEvent event) {
        this.applyStep(1.0f);
        this.resetState();
    }

    @EventTarget(priority = 100)
    public void onRender3D(Render3DEvent event) {
        this.applyStep(event.getPartialTicks());
    }
}
