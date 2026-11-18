package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.MoveInputEvent;
import myau.event.events.StrafeEvent;
import myau.mixin.IAccessorEntity;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.PercentProperty;
import myau.util.MoveUtil;
import net.minecraft.client.Minecraft;

public class Speed extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty multiplier = new FloatProperty("multiplier", 1.0F, 0.0F, 10.0F);
    public final FloatProperty friction = new FloatProperty("friction", 1.0F, 0.0F, 10.0F);
    public final PercentProperty strafe = new PercentProperty("strafe", 0);

    public Speed() {
        super("Speed", false, 0);
    }

    private boolean canSpeed() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        return !scaffold.isEnabled()
            && MoveUtil.isForwardPressed()
            && mc.thePlayer.getFoodStats().getFoodLevel() > 6
            && !mc.thePlayer.isSneaking()
            && !mc.thePlayer.isInWater()
            && !mc.thePlayer.isInLava()
            && !((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    @EventTarget(priority = -50)
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled() || !this.canSpeed()) {
            return;
        }
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.42F;
            MoveUtil.setSpeed(MoveUtil.getSpeedJumpMotion() * (double) this.multiplier.getValue(), MoveUtil.getDirectionYaw());
            return;
        }
        if (this.friction.getValue() != 1.0F) {
            event.setFriction(event.getFriction() * this.friction.getValue());
        }
        if (this.strafe.getValue() > 0) {
            double speed = MoveUtil.getSpeed();
            MoveUtil.setSpeed(speed * (double) ((float) (100 - this.strafe.getValue()) / 100.0F), MoveUtil.getMoveYaw());
            MoveUtil.addSpeed(speed * (double) ((float) this.strafe.getValue() / 100.0F), MoveUtil.getDirectionYaw());
            MoveUtil.setSpeed(speed);
        }
    }

    @EventTarget(priority = -50)
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || !this.canSpeed()) {
            return;
        }
        mc.thePlayer.movementInput.jump = false;
    }
}
