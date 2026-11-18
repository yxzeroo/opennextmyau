package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.KeyEvent;
import myau.event.events.KnockbackEvent;
import myau.event.events.PostTickEvent;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.StrafeEvent;
import myau.event.events.TickEvent;
import myau.event.events.UpdateEvent;
import myau.management.RotationState;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ChatUtil;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.RandomUtil;
import myau.util.RotationUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class LongJump extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil fireballTimer = new TimerUtil();
    private final TimerUtil timeoutTimer = new TimerUtil();
    private boolean jumping = false;
    private int jumpTicks = 0;
    private int jumpStage = 0;
    private boolean rotated = false;
    private boolean thrown = false;
    private int previousSlot = -1;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"FIREBALL", "FIREBALL_MANUAL", "FIREBALL_HIGH", "FIREBALL_FLAT"});
    public final FloatProperty motion = new FloatProperty("motion", 1.0F, 1.0F, 20.0F);
    public final FloatProperty speedMotion = new FloatProperty("speed-motion", 1.0F, 1.0F, 20.0F);
    public final PercentProperty strafe = new PercentProperty("strafe", 0);

    public LongJump() {
        super("LongJump", false, 0);
    }

    private int findFireballSlot() {
        if (mc.thePlayer == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() instanceof ItemFireball) {
                return slot;
            }
        }
        return -1;
    }

    private double getMotionMultiplier() {
        return MoveUtil.getSpeedEffect() > 0 ? (double) this.speedMotion.getValue() : (double) this.motion.getValue();
    }

    public boolean canStartJump() {
        int currentMode = this.mode.getValue();
        return currentMode == 0 || currentMode == 2 || currentMode == 3;
    }

    public boolean isManualMode() {
        return this.mode.getValue() == 1;
    }

    public boolean isFireballMode() {
        return this.canStartJump() || this.isManualMode();
    }

    public boolean isWaitingForFireball() {
        return !this.fireballTimer.hasTimeElapsed(1000L) && !this.jumping;
    }

    public boolean isJumping() {
        return this.jumping;
    }

    @EventTarget(priority = 100)
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if ((this.isManualMode() || this.canStartJump()) && this.isWaitingForFireball()) {
            event.setCancelled(true);
            this.jumping = true;
            this.jumpTicks = 0;
        }
    }

    @EventTarget(priority = 100)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.canStartJump() && !this.thrown && this.rotated) {
            int fireballSlot = this.findFireballSlot();
            if (fireballSlot != -1) {
                this.previousSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = fireballSlot;
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                this.fireballTimer.reset();
                this.thrown = true;
            }
        }
    }

    @EventTarget(priority = 100)
    public void onPostTick(PostTickEvent event) {
        if (this.previousSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.previousSlot;
            this.previousSlot = -1;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.isFireballMode() && this.jumping) {
            ++this.jumpTicks;
            if (this.jumpTicks == 1) {
                switch (this.mode.getValue()) {
                    case 0:
                    case 1:
                        this.jumpStage = 0;
                        break;
                    case 2:
                        this.jumpStage = 1;
                        break;
                    case 3:
                        this.jumpStage = MoveUtil.isForwardPressed() ? 2 : 1;
                        break;
                }
            }
            if (this.jumpTicks == 2 && MoveUtil.isForwardPressed()) {
                MoveUtil.setSpeed(MoveUtil.getSpeed() * this.getMotionMultiplier());
            }
            if (this.jumpTicks >= 1 && this.jumpTicks <= 30) {
                switch (this.jumpStage) {
                    case 1:
                        if (this.jumpTicks == 1) {
                            mc.thePlayer.motionY *= 0.75;
                        } else {
                            double nextMotionY = mc.thePlayer.motionY / (double) 0.98F + 0.055;
                            if (nextMotionY > 0.0) {
                                mc.thePlayer.motionY = nextMotionY;
                            }
                        }
                        break;
                    case 2:
                        if (this.jumpTicks == 1) {
                            mc.thePlayer.motionY *= 0.75;
                        } else {
                            mc.thePlayer.motionY = 0.01 + (double) this.jumpTicks * 0.003;
                        }
                        break;
                }
            }
            if (this.jumpTicks >= 30) {
                this.jumping = false;
                this.jumpTicks = 0;
                this.jumpStage = 0;
                if (this.canStartJump()) {
                    this.setEnabled(false);
                }
                return;
            }
        }
        if (this.canStartJump() && !this.jumping) {
            if (this.timeoutTimer.hasTimeElapsed(1500L)) {
                this.setEnabled(false);
                return;
            }
            this.rotated = true;
            float yaw = RotationUtil.quantizeAngle(
                mc.thePlayer.rotationYaw - 180.0F - RandomUtil.nextFloat(0.0F, 1.0F),
                event.getLastYaw()
            );
            float pitch = RotationUtil.quantizeAngle(
                89.0F + RandomUtil.nextFloat(-0.25F, 0.25F),
                event.getLastPitch()
            );
            event.setRotation(yaw, pitch, 4);
            event.setMoveYaw(yaw, 4);
        }
    }

    @EventTarget
    public void onRightClickDelay(RightClickDelayEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (RotationState.isRotated() && RotationState.getPriority() == 4.0F && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getMoveYaw());
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.isFireballMode()
            && this.jumping
            && this.jumpTicks >= 5
            && this.jumpTicks <= 30
            && this.strafe.getValue() > 0) {
            double speed = MoveUtil.getSpeed();
            MoveUtil.setSpeed(speed * (double) ((float) (100 - this.strafe.getValue()) / 100.0F), MoveUtil.getMoveYaw());
            MoveUtil.addSpeed(speed * (double) ((float) this.strafe.getValue() / 100.0F), MoveUtil.getDirectionYaw());
            MoveUtil.setSpeed(speed);
        }
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindUseItem.getKeyCode()) {
            ItemStack heldItem = mc.thePlayer.inventory.getCurrentItem();
            if (heldItem != null && heldItem.getItem() instanceof ItemFireball) {
                this.fireballTimer.reset();
            }
        }
    }

    @EventTarget(priority = 50)
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.jumping = false;
            this.jumpTicks = 0;
            this.jumpStage = 0;
            if (this.canStartJump()) {
                this.setEnabled(false);
            }
        }
    }

    @Override
    public void onEnabled() {
        this.timeoutTimer.reset();
        if (this.canStartJump() && this.findFireballSlot() == -1) {
            this.setEnabled(false);
            ChatUtil.sendFormatted(String.format("%s%s: &cNo fireball found in your hotbar!&r", Myau.clientName, this.getName()));
        }
    }

    @Override
    public void onDisabled() {
        this.jumping = false;
        this.jumpTicks = 0;
        this.jumpStage = 0;
        this.rotated = false;
        this.thrown = false;
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        if (modeName.contains("FIREBALL")) {
            return new String[]{"Fireball"};
        }
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}
