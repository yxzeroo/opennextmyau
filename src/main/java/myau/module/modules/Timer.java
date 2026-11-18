package myau.module.modules;

import com.google.common.base.CaseFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.PostTickEvent;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.SendPacketEvent;
import myau.event.events.StrafeEvent;
import myau.mixin.IAccessorEntityPlayerSP;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class Timer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private boolean timerModified = false;
    private boolean skipNextPacket = false;
    private boolean inputFrozen = false;
    private boolean motionFrozen = false;
    private double savedMotionX = 0.0;
    private double savedMotionY = 0.0;
    private double savedMotionZ = 0.0;
    private int cancelledPackets = 0;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"VANILLA", "HYPIXEL"});
    public final FloatProperty speed = new FloatProperty("speed", 1.0F, 0.0F, 10.0F, () -> this.mode.getValue() == 0);

    public Timer() {
        super("Timer", false, 0);
    }

    private boolean shouldReleaseBalance() {
        return this.cancelledPackets >= 10 || ((IAccessorEntityPlayerSP) mc.thePlayer).getPositionUpdateTicks() >= 20;
    }

    @EventTarget
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 0) {
            return;
        }
        if (mc.currentScreen != null) {
            this.onDisabled();
            return;
        }
        NoFall noFall = (NoFall) Myau.moduleManager.modules.get(NoFall.class);
        if (noFall.isBlinking()) {
            this.timerModified = false;
            return;
        }
        this.timerModified = true;
        ((IAccessorMinecraft) mc).getTimer().timerSpeed = this.speed.getValue();
    }

    @EventTarget(priority = -1000)
    public void onRightClickDelay(RightClickDelayEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 1 || this.shouldReleaseBalance()) {
            if (this.inputFrozen) {
                this.inputFrozen = false;
                mc.thePlayer.movementInput.moveStrafe = 0.0F;
                mc.thePlayer.movementInput.moveForward = 0.0F;
                mc.thePlayer.movementInput.jump = false;
                mc.thePlayer.movementInput.sneak = false;
            }
            return;
        }
        this.inputFrozen = true;
        mc.thePlayer.movementInput.moveStrafe = 0.0F;
        mc.thePlayer.movementInput.moveForward = 0.0F;
        mc.thePlayer.movementInput.jump = false;
        mc.thePlayer.movementInput.sneak = false;
    }

    @EventTarget(priority = -1000)
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 1 || this.shouldReleaseBalance()) {
            if (this.motionFrozen) {
                this.motionFrozen = false;
                mc.thePlayer.motionX = this.savedMotionX;
                mc.thePlayer.motionY = this.savedMotionY;
                mc.thePlayer.motionZ = this.savedMotionZ;
            }
            return;
        }
        if (!this.motionFrozen) {
            this.motionFrozen = true;
            this.savedMotionX = mc.thePlayer.motionX;
            this.savedMotionY = mc.thePlayer.motionY;
            this.savedMotionZ = mc.thePlayer.motionZ;
        }
        mc.thePlayer.motionX = 0.0;
        if (!mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.0;
        }
        mc.thePlayer.motionZ = 0.0;
    }

    @EventTarget(priority = -1000)
    public void onSendPacket(SendPacketEvent event) {
        if (!this.isEnabled() || event.isCancelled() || !(event.getPacket() instanceof C03PacketPlayer)) {
            return;
        }
        if (this.skipNextPacket) {
            this.skipNextPacket = false;
            return;
        }
        if (this.mode.getValue() != 1 || this.shouldReleaseBalance()) {
            this.cancelledPackets = 0;
            return;
        }
        this.cancelledPackets++;
        if (event.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook) {
            return;
        }
        event.setCancelled(true);
    }

    @EventTarget(priority = -1000)
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 1 || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.skipNextPacket = true;
            this.motionFrozen = false;
        }
    }

    @Override
    public void onDisabled() {
        if (this.timerModified) {
            this.timerModified = false;
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
        }
        this.cancelledPackets = 0;
    }

    @Override
    public void verifyValue(String propertyName) {
        if (this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        switch (this.mode.getValue()) {
            case 0:
                return new String[]{SPEED_FORMAT.format(this.speed.getValue())};
            case 1:
                return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
            default:
                return new String[0];
        }
    }
}
