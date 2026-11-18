package myau.management;

import java.util.LinkedHashMap;
import myau.enums.FloatModules;
import myau.event.EventTarget;
import myau.event.events.LivingUpdateEvent;
import net.minecraft.client.Minecraft;

public class FloatManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final LinkedHashMap<FloatModules, Boolean> activeModules = new LinkedHashMap<>();
    private boolean floating = false;

    public boolean isFloating() {
        return this.floating;
    }

    public boolean canFloat() {
        return mc.thePlayer.onGround
            && mc.thePlayer.posY - mc.thePlayer.lastTickPosY < 0.0
            && mc.thePlayer.motionY < 0.0;
    }

    public boolean isActive() {
        return this.activeModules.containsValue(true);
    }

    public void setFloatState(boolean state, FloatModules module) {
        this.activeModules.put(module, state);
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if ((this.isActive() || this.isFloating()) && this.canFloat()) {
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.001, mc.thePlayer.posZ);
            this.floating = true;
            return;
        }
        this.floating = false;
    }
}
