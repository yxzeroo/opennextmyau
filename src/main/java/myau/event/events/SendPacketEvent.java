package myau.event.events;

import myau.event.EventCancellable;
import net.minecraft.network.Packet;

public class SendPacketEvent extends EventCancellable {
    private final Packet<?> packet;

    public SendPacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }
}
