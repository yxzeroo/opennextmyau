package myau.management;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import myau.Myau;
import myau.enums.DelayModules;
import myau.event.EventTarget;
import myau.event.events.PostTickEvent;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.SendPacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;

public class LagManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private DelayModules delayModule = DelayModules.NONE;
    private long delayTicks = 0L;
    public final Deque<Packet<INetHandlerPlayClient>> delayedPackets = new ConcurrentLinkedDeque<>();

    public boolean handlePacket(Packet<INetHandlerPlayClient> packet) {
        if (this.delayModule == DelayModules.NONE) {
            return false;
        }
        if (packet instanceof S01PacketJoinGame || packet instanceof S07PacketRespawn) {
            this.setDelayState(false, this.delayModule);
            return false;
        }
        if (packet instanceof S00PacketKeepAlive || packet instanceof S06PacketUpdateHealth) {
            return false;
        }
        if (packet instanceof S19PacketEntityStatus) {
            byte opCode = ((S19PacketEntityStatus) packet).getOpCode();
            if (opCode == 2 || opCode == 3) {
                return false;
            }
        }
        if (packet instanceof S1CPacketEntityMetadata) {
            List<DataWatcher.WatchableObject> watchables = ((S1CPacketEntityMetadata) packet).func_149376_c();
            if (watchables != null) {
                for (DataWatcher.WatchableObject watchable : watchables) {
                    if (watchable.getDataValueId() == 6) {
                        return false;
                    }
                }
            }
        }
        this.delayedPackets.offer(packet);
        return true;
    }

    public boolean setDelayState(boolean state, DelayModules module) {
        if (module == DelayModules.NONE) {
            return false;
        }
        if (this.delayModule != DelayModules.NONE && this.delayModule != module) {
            return false;
        }
        if (state) {
            this.delayModule = module;
        } else {
            this.delayModule = DelayModules.NONE;
            this.delayTicks = 0L;
            if (mc.getNetHandler() != null) {
                while (!this.delayedPackets.isEmpty() && this.delayModule == DelayModules.NONE) {
                    Packet<INetHandlerPlayClient> packet = this.delayedPackets.poll();
                    ReceivePacketEvent event = new ReceivePacketEvent(packet);
                    Myau.eventManager.call(event);
                    if (!event.isCancelled()) {
                        try {
                            packet.processPacket(mc.getNetHandler());
                        } catch (ThreadQuickExitException ignored) {
                        }
                    }
                }
            } else {
                this.delayedPackets.clear();
            }
        }
        return true;
    }

    public DelayModules getDelayModule() {
        return this.delayModule;
    }

    public void setDelayModule(DelayModules delayModule) {
        this.delayModule = delayModule;
    }

    public long getDelayTicks() {
        return this.delayTicks;
    }

    @EventTarget
    public void onSendPacket(SendPacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof C00Handshake
            || packet instanceof C00PacketLoginStart
            || packet instanceof C00PacketServerQuery
            || packet instanceof C01PacketPing
            || packet instanceof C01PacketEncryptionResponse) {
            this.setDelayState(false, this.delayModule);
        }
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (mc.thePlayer.isDead) {
            this.setDelayState(false, this.delayModule);
        }
        if (this.delayModule != DelayModules.NONE) {
            ++this.delayTicks;
        }
    }
}
