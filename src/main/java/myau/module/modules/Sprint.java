package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.PostTickEvent;
import myau.event.events.TickEvent;
import myau.mixin.IAccessorEntityLivingBase;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

public class Sprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasSprinting = false;
    public final BooleanProperty fovFix = new BooleanProperty("fov-fix", true);

    public Sprint() {
        super("Sprint", true, 0, true);
    }

    public boolean shouldApplyFov(IAttributeInstance attribute) {
        if (!this.fovFix.getValue()) {
            return false;
        }
        AttributeModifier sprintingModifier = ((IAccessorEntityLivingBase) mc.thePlayer).getSprintingSpeedBoostModifier();
        if (attribute.getModifier(sprintingModifier.getID()) != null) {
            return false;
        }
        return this.wasSprinting;
    }

    public boolean shouldForceFov(boolean sprinting) {
        return this.fovFix.getValue() && !sprinting && this.wasSprinting;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.wasSprinting = mc.thePlayer.isSprinting();
    }

    @Override
    public void onDisabled() {
        this.wasSprinting = false;
        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
    }
}
