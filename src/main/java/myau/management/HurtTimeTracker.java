package myau.management;

import myau.event.EventTarget;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;

public class HurtTimeTracker {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean damaged = false;
    private int hurtTicks = 0;

    public int getHurtTicks() {
        return this.hurtTicks;
    }

    @EventTarget(priority = 100)
    public void onTick(TickEvent event) {
        if (this.hurtTicks > 0) {
            --this.hurtTicks;
        }
        if (this.damaged) {
            this.damaged = false;
            this.hurtTicks = mc.thePlayer.hurtTime;
        }
    }

    @EventTarget
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof S06PacketUpdateHealth) {
            S06PacketUpdateHealth packet = (S06PacketUpdateHealth) event.getPacket();
            if (packet.getHealth() - mc.thePlayer.getHealth() < 0.0f) {
                this.damaged = true;
            }
            return;
        }
        if (event.getPacket() instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata) event.getPacket();
            if (packet.getEntityId() == mc.thePlayer.getEntityId() && packet.func_149376_c() != null) {
                for (DataWatcher.WatchableObject watchable : packet.func_149376_c()) {
                    if (watchable.getDataValueId() == 6 && (Float) watchable.getObject() - mc.thePlayer.getHealth() < 0.0f) {
                        this.damaged = true;
                    }
                }
            }
        }
    }
}
