package myau.mixin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.Future;
import myau.Myau;
import myau.event.events.PacketReceiveSyncEvent;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.SendPacketEvent;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {
    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private void onChannelRead0(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (packet.getClass().getName().startsWith("net.minecraft.network.play.client")) {
            return;
        }
        if (Myau.eventManager == null || Myau.lagManager == null) {
            return;
        }
        Myau.eventManager.call(new PacketReceiveSyncEvent(packet));
        if (Myau.lagManager.handlePacket((Packet<INetHandlerPlayClient>) packet)) {
            ci.cancel();
            return;
        }
        ReceivePacketEvent event = new ReceivePacketEvent(packet);
        Myau.eventManager.call(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
            return;
        }
        if (Myau.eventManager == null || Myau.packetStateTracker == null || Myau.blinkManager == null || Myau.lagRangeManager == null) {
            return;
        }
        SendPacketEvent event = new SendPacketEvent(packet);
        Myau.eventManager.call(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        if (Myau.lagRangeManager.isFlushing()) {
            return;
        }
        if (!Myau.blinkManager.isFlushing()) {
            Myau.packetStateTracker.onPacket(packet);
            if (Myau.blinkManager.offerPacket(packet)) {
                ci.cancel();
                return;
            }
        }
        if (Myau.lagRangeManager.shouldLagPacket(packet)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;[Lio/netty/util/concurrent/GenericFutureListener;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendPacketNoEvent(
        Packet<?> packet,
        GenericFutureListener<? extends Future<? super Void>> listener,
        GenericFutureListener<? extends Future<? super Void>>[] listeners,
        CallbackInfo ci
    ) {
        if (packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
            return;
        }
        if (Myau.packetStateTracker == null || Myau.blinkManager == null || Myau.lagRangeManager == null) {
            return;
        }
        if (Myau.lagRangeManager.isFlushing()) {
            return;
        }
        if (!Myau.blinkManager.isFlushing()) {
            Myau.packetStateTracker.onPacket(packet);
            if (Myau.blinkManager.offerPacket(packet)) {
                ci.cancel();
                return;
            }
        }
        if (Myau.lagRangeManager.shouldLagPacket(packet)) {
            ci.cancel();
        }
    }
}
