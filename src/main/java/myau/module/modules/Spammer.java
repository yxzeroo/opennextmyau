package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.TextProperty;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;

public class Spammer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private int charCode = 19968;
    public final TextProperty text = new TextProperty("text", "meow");
    public final FloatProperty delay = new FloatProperty("delay", 3.5f, 0.0f, 3600.0f);
    public final IntProperty random = new IntProperty("random", 0, 0, 10);

    public Spammer() {
        super("Spammer", false, 0);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.timer.hasTimeElapsed((long) (this.delay.getValue() * 1000.0f))) {
            this.timer.reset();
            String message = this.text.getValue();
            if (this.random.getValue() > 0) {
                message = String.format("%s ", message);
                for (int i = 0; i < this.random.getValue(); ++i) {
                    message = String.format("%s%s", message, (char) this.charCode);
                    ++this.charCode;
                    if (this.charCode > 40959) {
                        this.charCode = 19968;
                    }
                }
            }
            mc.thePlayer.sendChatMessage(message);
        }
    }
}
