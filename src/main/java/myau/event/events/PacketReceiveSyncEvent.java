package myau.event.events;

import myau.event.Event;
import net.minecraft.network.Packet;

public class PacketReceiveSyncEvent extends Event {
    private final Packet<?> packet;

    public PacketReceiveSyncEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }
}
