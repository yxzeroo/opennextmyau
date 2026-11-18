package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.SafeWalkEvent;
import myau.event.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.util.ItemUtil;
import myau.util.MoveUtil;
import myau.util.PlayerUtil;
import net.minecraft.client.Minecraft;

public class SafeWalk extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty motion = new FloatProperty("motion", 1.0F, 0.5F, 1.0F);
    public final FloatProperty speedMotion = new FloatProperty("speed-motion", 1.0F, 0.5F, 1.5F);
    public final BooleanProperty air = new BooleanProperty("air", false);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty pitchCheck = new BooleanProperty("pitch-check", true);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", false);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);

    public SafeWalk() {
        super("SafeWalk", false, 0);
    }

    private boolean canSafeWalk() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        }
        if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        }
        if (this.pitchCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        }
        if (this.blocksOnly.getValue() && !ItemUtil.isHoldingPlaceableBlock()) {
            return false;
        }
        if (this.requirePress.getValue() && !mc.gameSettings.keyBindUseItem.isKeyDown()) {
            return false;
        }
        return (mc.thePlayer.onGround && PlayerUtil.canMove(mc.thePlayer.motionX, -1.0, mc.thePlayer.motionZ))
            || (this.air.getValue() && PlayerUtil.canMove(mc.thePlayer.motionX, -2.0, mc.thePlayer.motionZ));
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (this.isEnabled() && this.canSafeWalk()) {
            event.setSafeWalk(true);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && this.canSafeWalk()) {
            if (MoveUtil.getSpeedEffect() > 0) {
                if (this.speedMotion.getValue() != 1.0F) {
                    MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.speedMotion.getValue());
                }
            } else if (this.motion.getValue() != 1.0F) {
                MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.motion.getValue());
            }
        }
    }
}
