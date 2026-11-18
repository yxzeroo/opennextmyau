package myau.management;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;

public class PacketStateTracker {
    public boolean attacked = false;
    public boolean digging = false;
    public boolean placing = false;
    public boolean swappedSlot = false;
    public boolean swung = false;

    public void onPacket(Packet<?> packet) {
        if (packet instanceof C02PacketUseEntity) {
            this.attacked = true;
        }
        if (packet instanceof C07PacketPlayerDigging) {
            this.digging = true;
        }
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            this.placing = true;
        }
        if (packet instanceof C09PacketHeldItemChange) {
            this.swappedSlot = true;
        }
        if (packet instanceof C0APacketAnimation) {
            this.swung = true;
        }
        if (packet instanceof C03PacketPlayer) {
            this.attacked = false;
            this.digging = false;
            this.placing = false;
            this.swappedSlot = false;
            this.swung = false;
        }
    }
}
