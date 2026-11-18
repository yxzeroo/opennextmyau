package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.SendPacketEvent;
import myau.event.events.TickEvent;
import myau.mixin.IAccessorC03PacketPlayer;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.ServerUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.AxisAlignedBB;

public class NoFall extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil delayTimer = new TimerUtil();
    private final TimerUtil lobbyTimer = new TimerUtil();
    private boolean slowFalling = false;
    private boolean wasOnGround = false;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"PACKET", "BLINK", "NO_GROUND", "SPOOF"});
    public final FloatProperty distance = new FloatProperty("distance", 3.0F, 0.0F, 20.0F);
    public final IntProperty delay = new IntProperty("delay", 0, 0, 10000);

    public NoFall() {
        super("NoFall", false, 0);
    }

    private boolean isDelayElapsed() {
        return this.lobbyTimer.hasTimeElapsed(3000L) && this.delayTimer.hasTimeElapsed(this.delay.getValue().longValue());
    }

    public boolean isBlinking() {
        return this.slowFalling;
    }

    @EventTarget(priority = 50)
    public void onSendPacket(SendPacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof C03PacketPlayer) {
            C03PacketPlayer packet = (C03PacketPlayer) event.getPacket();
            switch (this.mode.getValue()) {
                case 0: {
                    if (this.slowFalling) {
                        this.slowFalling = false;
                        ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
                    } else if (!packet.isOnGround()) {
                        AxisAlignedBB checkBox = mc.thePlayer.getEntityBoundingBox().expand(2.0, 0.0, 2.0);
                        if (PlayerUtil.canFall(this.distance.getValue()) && !PlayerUtil.isOverVoid(checkBox) && this.isDelayElapsed()) {
                            this.delayTimer.reset();
                            this.slowFalling = true;
                            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 0.5F;
                        }
                    }
                    break;
                }
                case 1: {
                    boolean validPlayerState = !mc.thePlayer.isOnLadder()
                        && !mc.thePlayer.capabilities.allowFlying
                        && mc.thePlayer.hurtTime == 0;
                    if (Myau.blinkManager.getBlinkingModule() != BlinkModules.NO_FALL) {
                        if (this.wasOnGround
                            && !packet.isOnGround()
                            && validPlayerState
                            && PlayerUtil.isAirBelow(this.distance.getValue().intValue())
                            && mc.thePlayer.motionY < 0.0) {
                            Myau.blinkManager.setBlinkState(false, Myau.blinkManager.getBlinkingModule());
                            Myau.blinkManager.setBlinkState(true, BlinkModules.NO_FALL);
                        }
                    } else if (!validPlayerState) {
                        Myau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                        ChatUtil.sendFormatted(String.format("%s%s: &cFailed player check!&r", Myau.clientName, this.getName()));
                    } else if (PlayerUtil.isOverVoid(mc.thePlayer.getEntityBoundingBox().expand(2.0, 0.0, 2.0))) {
                        Myau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                        ChatUtil.sendFormatted(String.format("%s%s: &cFailed void check!&r", Myau.clientName, this.getName()));
                    } else if (packet.isOnGround()) {
                        for (Packet<?> blinkedPacket : Myau.blinkManager.blinkedPackets) {
                            if (blinkedPacket instanceof C03PacketPlayer) {
                                ((IAccessorC03PacketPlayer) blinkedPacket).setOnGround(true);
                            }
                        }
                        Myau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                        this.delayTimer.reset();
                    }
                    this.wasOnGround = packet.isOnGround() && validPlayerState && this.isDelayElapsed();
                    break;
                }
                case 2: {
                    ((IAccessorC03PacketPlayer) packet).setOnGround(false);
                    break;
                }
                case 3: {
                    if (!packet.isOnGround()) {
                        AxisAlignedBB checkBox = mc.thePlayer.getEntityBoundingBox().expand(2.0, 0.0, 2.0);
                        if (PlayerUtil.canFall(this.distance.getValue()) && !PlayerUtil.isOverVoid(checkBox) && this.isDelayElapsed()) {
                            this.delayTimer.reset();
                            ((IAccessorC03PacketPlayer) packet).setOnGround(true);
                            mc.thePlayer.fallDistance = 0.0F;
                        }
                    }
                    break;
                }
            }
        }
    }

    @EventTarget(priority = 50)
    public void onReceivePacket(ReceivePacketEvent event) {
        if (this.isEnabled() && !event.isCancelled() && event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.onDisabled();
        }
    }

    @EventTarget(priority = 100)
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            if (ServerUtil.hasPlayersLine()) {
                this.lobbyTimer.reset();
            }
            if (this.mode.getValue() == 0 && this.slowFalling) {
                PacketUtil.sendPacketNoEvent(new C03PacketPlayer(true));
                mc.thePlayer.fallDistance = 0.0F;
            }
        }
    }

    @Override
    public void onDisabled() {
        this.wasOnGround = false;
        Myau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
        if (this.slowFalling) {
            this.slowFalling = false;
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
        }
    }

    @Override
    public void verifyValue(String propertyName) {
        if (this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
