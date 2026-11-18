package myau.data;

import net.minecraft.network.Packet;

public class TimedPacket {
    public final Packet<?> packet;
    public int ticks;

    public TimedPacket(Packet<?> packet) {
        this.packet = packet;
        this.ticks = 0;
    }
}
