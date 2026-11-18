package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.PostTickEvent;
import myau.event.events.SendPacketEvent;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C07PacketPlayerDigging;

public class SpeedMine extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean startedBreaking = false;
    private boolean stoppedBreaking = false;
    private int chanceCounter = 100;
    public final PercentProperty speed = new PercentProperty("speed", 15);
    public final IntProperty delay = new IntProperty("delay", 0, 0, 5);
    public final PercentProperty chance = new PercentProperty("chance", 50);

    public SpeedMine() {
        super("SpeedMine", false, 0);
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        IAccessorPlayerControllerMP controller = (IAccessorPlayerControllerMP) mc.playerController;
        if (this.startedBreaking) {
            this.startedBreaking = false;
            if (controller.getIsHittingBlock() && this.chanceCounter >= 100) {
                float curDamage = controller.getCurBlockDamageMP();
                float bonusDamage = 0.3F * (this.speed.getValue().floatValue() / 100.0F);
                controller.setCurBlockDamageMP(curDamage + bonusDamage);
            }
        }
        if (this.stoppedBreaking) {
            this.stoppedBreaking = false;
            if (controller.getBlockHitDelay() == 5) {
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    controller.setBlockHitDelay(this.delay.getValue());
                }
            }
        }
    }

    @EventTarget(priority = -100)
    public void onSendPacket(SendPacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.getPacket();
            switch (packet.getStatus()) {
                case START_DESTROY_BLOCK:
                    this.startedBreaking = true;
                    break;
                case STOP_DESTROY_BLOCK:
                    this.stoppedBreaking = true;
                    break;
            }
        }
    }

    @Override
    public void onDisabled() {
        this.startedBreaking = false;
        this.stoppedBreaking = false;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{
                String.format("%d%%", this.speed.getValue()),
                String.format("%s", this.delay.getValue())
        };
    }
}
