package myau.event.events;

import myau.event.EventCancellable;
import net.minecraft.network.Packet;

public class ReceivePacketEvent extends EventCancellable {
    private final Packet<?> packet;

    public ReceivePacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }
}
