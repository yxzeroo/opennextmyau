package myau.management;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import myau.data.TimedPacket;
import myau.event.EventTarget;
import myau.event.events.PostTickEvent;
import myau.event.events.SendPacketEvent;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.util.Vec3;

public class LagRangeManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final Deque<TimedPacket> delayedPackets = new ConcurrentLinkedDeque<>();
    private int tickDelay = 0;
    private boolean flushing = false;
    private Vec3 lastPosition = new Vec3(0.0, 0.0, 0.0);

    private void flushPackets() {
        if (mc.getNetHandler() == null) {
            this.delayedPackets.clear();
            return;
        }
        this.flushing = true;
        while (!this.delayedPackets.isEmpty()) {
            TimedPacket timedPacket = this.delayedPackets.peek();
            if (this.tickDelay > 0 && timedPacket.ticks <= this.tickDelay) {
                break;
            }
            PacketUtil.sendPacketNoEvent(timedPacket.packet);
            if (timedPacket.packet instanceof C03PacketPlayer) {
                C03PacketPlayer playerPacket = (C03PacketPlayer) timedPacket.packet;
                if (playerPacket.isMoving()) {
                    this.lastPosition = new Vec3(playerPacket.getPositionX(), playerPacket.getPositionY(), playerPacket.getPositionZ());
                }
            }
            this.delayedPackets.poll();
        }
        this.flushing = false;
    }

    private void incrementTicks() {
        this.delayedPackets.forEach(timedPacket -> ++timedPacket.ticks);
    }

    public boolean shouldLagPacket(Packet<?> packet) {
        this.flushPackets();
        if (packet instanceof C00PacketKeepAlive || packet instanceof C01PacketChatMessage) {
            return false;
        }
        if (this.tickDelay > 0) {
            this.delayedPackets.offer(new TimedPacket(packet));
            return true;
        }
        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer playerPacket = (C03PacketPlayer) packet;
            if (playerPacket.isMoving()) {
                this.lastPosition = new Vec3(playerPacket.getPositionX(), playerPacket.getPositionY(), playerPacket.getPositionZ());
            }
        }
        return false;
    }

    public void setDelay(int tickDelay) {
        this.tickDelay = tickDelay;
    }

    public Vec3 getLastPosition() {
        return this.lastPosition;
    }

    public boolean isFlushing() {
        return this.flushing;
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (mc.thePlayer.isDead) {
            this.setDelay(0);
        }
        this.incrementTicks();
        this.flushPackets();
    }

    @EventTarget
    public void onSendPacket(SendPacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof C00Handshake
            || packet instanceof C00PacketLoginStart
            || packet instanceof C00PacketServerQuery
            || packet instanceof C01PacketPing
            || packet instanceof C01PacketEncryptionResponse) {
            this.setDelay(0);
        }
    }
}
