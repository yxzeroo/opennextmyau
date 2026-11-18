package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.SendPacketEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.PercentProperty;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.potion.Potion;

public class WTap extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final TimerUtil timer = new TimerUtil();
    private int chanceCounter = 0;
    private boolean active = false;
    private boolean stopForward = false;
    private long delayTicks = 0L;
    private long durationTicks = 0L;

    public final FloatProperty delay = new FloatProperty("delay", 5.5F, 0.0F, 10.0F);
    public final FloatProperty duration = new FloatProperty("duration", 1.5F, 1.0F, 5.0F);
    public final PercentProperty chance = new PercentProperty("chance", 100);

    public WTap() {
        super("WTap", false, 0);
    }

    private boolean canSprint() {
        return !(mc.thePlayer.movementInput.moveForward < 0.8F)
                && !mc.thePlayer.isCollidedHorizontally
                && (!((float) mc.thePlayer.getFoodStats().getFoodLevel() <= 6.0F) || mc.thePlayer.capabilities.allowFlying)
                && (mc.thePlayer.isSprinting()
                    || !mc.thePlayer.isUsingItem()
                        && !mc.thePlayer.isPotionActive(Potion.blindness)
                        && mc.gameSettings.keyBindSprint.isKeyDown());
    }

    @EventTarget(priority = -100)
    public void onRightClickDelay(RightClickDelayEvent event) {
        if (!this.active) {
            return;
        }
        if (!this.stopForward && !this.canSprint()) {
            this.active = false;
            while (this.delayTicks > 0L) {
                this.delayTicks -= 50L;
            }
            while (this.durationTicks > 0L) {
                this.durationTicks -= 50L;
            }
            return;
        }
        if (this.delayTicks > 0L) {
            this.delayTicks -= 50L;
            return;
        }
        if (this.durationTicks > 0L) {
            this.durationTicks -= 50L;
            this.stopForward = true;
            mc.thePlayer.movementInput.moveForward = 0.0F;
        }
        if (this.durationTicks <= 0L) {
            this.active = false;
        }
    }

    @EventTarget
    public void onSendPacket(SendPacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() == C02PacketUseEntity.Action.ATTACK
                    && !this.active
                    && this.timer.hasTimeElapsed(500L)
                    && mc.thePlayer.isSprinting()) {
                this.timer.reset();
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter < 100) {
                    return;
                }
                this.active = true;
                this.stopForward = false;
                this.delayTicks += (long) (50.0F * this.delay.getValue());
                this.durationTicks += (long) (50.0F * this.duration.getValue());
            }
        }
    }
}
