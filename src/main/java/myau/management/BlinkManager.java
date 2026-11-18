package myau.management;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import myau.enums.BlinkModules;
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
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;

public class BlinkManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private BlinkModules blinkModule = BlinkModules.NONE;
    private boolean flushing = false;
    public final Deque<Packet<?>> blinkedPackets = new ConcurrentLinkedDeque<>();

    public boolean offerPacket(Packet<?> packet) {
        if (this.blinkModule == BlinkModules.NONE
            || packet instanceof C00PacketKeepAlive
            || packet instanceof C01PacketChatMessage) {
            return false;
        }
        if (this.blinkedPackets.isEmpty() && packet instanceof C0FPacketConfirmTransaction) {
            return false;
        }
        this.blinkedPackets.offer(packet);
        return true;
    }

    public boolean setBlinkState(boolean state, BlinkModules module) {
        if (module == BlinkModules.NONE) {
            return false;
        }
        if (this.blinkModule != BlinkModules.NONE && this.blinkModule != module) {
            return false;
        }
        if (state) {
            this.blinkModule = module;
        } else {
            this.blinkModule = BlinkModules.NONE;
            if (mc.getNetHandler() != null) {
                this.flushing = true;
                while (!this.blinkedPackets.isEmpty()) {
                    PacketUtil.sendPacketNoEvent(this.blinkedPackets.poll());
                }
                this.flushing = false;
            } else {
                this.blinkedPackets.clear();
            }
        }
        return true;
    }

    public BlinkModules getBlinkingModule() {
        return this.blinkModule;
    }

    public long countMovementPackets() {
        return this.blinkedPackets.stream().filter(packet -> packet instanceof C03PacketPlayer).count();
    }

    public boolean isFlushing() {
        return this.flushing;
    }

    @EventTarget
    public void onSendPacket(SendPacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof C00Handshake
            || packet instanceof C00PacketLoginStart
            || packet instanceof C00PacketServerQuery
            || packet instanceof C01PacketPing
            || packet instanceof C01PacketEncryptionResponse) {
            this.setBlinkState(false, this.blinkModule);
        }
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (mc.thePlayer.isDead) {
            this.setBlinkState(false, this.blinkModule);
        }
    }
}
