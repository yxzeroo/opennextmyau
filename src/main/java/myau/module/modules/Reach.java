package myau.module.modules;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;
import myau.event.EventTarget;
import myau.event.events.PickEvent;
import myau.event.events.RaytraceEvent;
import myau.event.events.TickEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.PercentProperty;

public class Reach extends Module {
    private static final DecimalFormat RANGE_FORMAT = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));

    private final Random random = new Random();
    private boolean active = true;

    public final FloatProperty range = new FloatProperty("range", 3.1F, 3.0F, 6.0F);
    public final PercentProperty chance = new PercentProperty("chance", 100);

    public Reach() {
        super("Reach", false, 0);
    }

    @EventTarget
    public void onPick(PickEvent event) {
        if (!this.isEnabled() || !this.active) {
            return;
        }
        event.setRange(this.range.getValue().doubleValue());
    }

    @EventTarget
    public void onRaytrace(RaytraceEvent event) {
        if (!this.isEnabled() || !this.active) {
            return;
        }
        event.setRange(Math.max(event.getRange(), this.range.getValue().doubleValue() + 0.5));
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.active = this.random.nextDouble() <= this.chance.getValue().doubleValue() / 100.0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{RANGE_FORMAT.format(this.range.getValue())};
    }
}
