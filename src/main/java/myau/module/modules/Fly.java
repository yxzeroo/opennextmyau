package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.StrafeEvent;
import myau.event.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.util.KeyBindUtil;
import myau.util.MoveUtil;
import net.minecraft.client.Minecraft;

public class Fly extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private double verticalMotion = 0.0;
    public final FloatProperty horizontalSpeed = new FloatProperty("horizontal-speed", 1.0F, 0.0F, 100.0F);
    public final FloatProperty verticalSpeed = new FloatProperty("vertical-speed", 1.0F, 0.0F, 100.0F);

    public Fly() {
        super("Fly", false, 0);
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (!mc.thePlayer.onGround) {
            mc.thePlayer.motionY = this.verticalMotion;
        }
        MoveUtil.setSpeed(0.0);
        event.setFriction((float) MoveUtil.getBaseMoveSpeed() * this.horizontalSpeed.getValue());
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.verticalMotion = 0.0;
        if (mc.currentScreen == null) {
            if (KeyBindUtil.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                this.verticalMotion += this.verticalSpeed.getValue().doubleValue() * (double) 0.42F;
            }
            if (KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                this.verticalMotion -= this.verticalSpeed.getValue().doubleValue() * (double) 0.42F;
            }
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }

    @Override
    public void onDisabled() {
        mc.thePlayer.motionY = 0.0;
        MoveUtil.setSpeed(0.0);
        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSneak.getKeyCode());
    }
}
