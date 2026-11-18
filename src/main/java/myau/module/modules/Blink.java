package myau.module.modules;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.events.LoadWorldEvent;
import myau.event.events.PostTickEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;

public class Blink extends Module {
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "PULSE"});
    public final IntProperty ticks = new IntProperty("ticks", 200, 0, 12000);

    public Blink() {
        super("Blink", false, 0);
    }

    @EventTarget(priority = -100)
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (Myau.blinkManager.getBlinkingModule() != BlinkModules.BLINK) {
            this.setEnabled(false);
            return;
        }
        if (this.ticks.getValue() > 0 && Myau.blinkManager.countMovementPackets() > (long) this.ticks.getValue()) {
            switch (this.mode.getValue()) {
                case 0:
                    this.setEnabled(false);
                    break;
                case 1:
                    Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                    Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                    break;
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.setEnabled(false);
    }

    @Override
    public void onEnabled() {
        Myau.blinkManager.setBlinkState(false, Myau.blinkManager.getBlinkingModule());
        Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
    }

    @Override
    public void onDisabled() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }
}
