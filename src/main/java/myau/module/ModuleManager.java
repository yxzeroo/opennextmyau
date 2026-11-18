package myau.module;

import java.util.LinkedHashMap;
import myau.Myau;
import myau.data.Box;
import myau.module.modules.HUD;
import myau.util.ChatUtil;
import myau.util.KeyBindUtil;
import myau.util.SoundUtil;
import net.minecraft.client.Minecraft;

public class ModuleManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean playToggleSound = false;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public Module getModule(String name) {
        return this.modules.values().stream()
            .filter(module -> module.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public boolean toggleModule(Module module) {
        boolean targetState = !module.isEnabled();
        module.setEnabled(targetState);
        return module.isEnabled() == targetState;
    }

    public void onRenderTick() {
        if (mc.theWorld == null || mc.thePlayer == null || mc.currentScreen != null) {
            for (Module module : this.modules.values()) {
                module.setKeyBindState(null);
            }
            this.playToggleSound = false;
            return;
        }
        HUD hud = (HUD) this.modules.get(HUD.class);
        for (Module module : this.modules.values()) {
            int keyCode = module.getKey();
            if (keyCode == 0) {
                module.setKeyBindState(null);
                continue;
            }
            boolean isPressed = KeyBindUtil.isKeyDown(keyCode);
            Box<Boolean> previousState = module.getKeyBindState();
            if (previousState != null && !previousState.value && isPressed && this.toggleModule(module)) {
                if (hud.toggleAlerts.getValue()) {
                    String stateText = module.isEnabled() ? "&a&lON" : "&c&lOFF";
                    ChatUtil.sendFormatted(String.format("%s%s: %s&r", Myau.clientName, module.getName(), stateText));
                }
                if (hud.toggleSounds.getValue()) {
                    this.playToggleSound = true;
                }
            }
            module.setKeyBindState(new Box<>(isPressed));
        }
        if (this.playToggleSound) {
            this.playToggleSound = false;
            SoundUtil.playSound("random.click");
        }
    }
}
