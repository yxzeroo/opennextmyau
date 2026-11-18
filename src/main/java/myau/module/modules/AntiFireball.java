package myau.module.modules;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.LoadWorldEvent;
import myau.event.events.Render3DEvent;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.TickEvent;
import myau.event.events.UpdateEvent;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ItemUtil;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MathHelper;

public class AntiFireball extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final ArrayList<EntityFireball> trackedFireballs = new ArrayList<>();
    private final ArrayList<EntityFireball> ignoredFireballs = new ArrayList<>();
    private EntityFireball target = null;

    public final FloatProperty range = new FloatProperty("range", 5.0F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 360, 1, 360);
    public final BooleanProperty rotations = new BooleanProperty("rotations", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final ModeProperty showTarget = new ModeProperty("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});

    public AntiFireball() {
        super("AntiFireball", false, 0);
    }

    private boolean isValidFireball(EntityFireball fireball) {
        if (fireball.getEntityBoundingBox().hasNaN()) {
            return false;
        }
        return RotationUtil.distanceToEntity(fireball) <= this.range.getValue().doubleValue() + 3.0
                && RotationUtil.angleToEntity(fireball) <= this.fov.getValue().floatValue();
    }

    private void performSwing() {
        if (this.swing.getValue()) {
            mc.thePlayer.swingItem();
        } else {
            PacketUtil.sendPacket(new C0APacketAnimation());
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        List<EntityFireball> loadedFireballs = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityFireball)
                .map(entity -> (EntityFireball) entity)
                .collect(Collectors.toList());
        this.trackedFireballs.removeIf(fireball -> !loadedFireballs.contains(fireball));
        this.ignoredFireballs.removeIf(fireball -> !loadedFireballs.contains(fireball));
        for (EntityFireball fireball : loadedFireballs) {
            if (this.trackedFireballs.contains(fireball) || this.ignoredFireballs.contains(fireball)) {
                continue;
            }
            if (RotationUtil.distanceToEntity(fireball) > 3.0) {
                this.trackedFireballs.add(fireball);
            } else {
                this.ignoredFireballs.add(fireball);
            }
        }
        if (mc.thePlayer.capabilities.allowFlying) {
            this.target = null;
            return;
        }
        this.target = this.trackedFireballs.stream()
                .filter(this::isValidFireball)
                .min(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                .orElse(null);
    }

    @EventTarget(priority = -100)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || !TeamUtil.isEntityLoaded(this.target)) {
            return;
        }
        if (this.rotations.getValue()
                && !ItemUtil.isHoldingBlock()
                && !ItemUtil.isHoldingBow()
                && !ItemUtil.isHoldingFireball()) {
            float lastYaw = event.getLastYaw();
            float lastPitch = event.getLastPitch();
            float[] targetRotations = RotationUtil.getRotationsToBox(this.target.getEntityBoundingBox());
            float yaw = RotationUtil.quantizeAngle(RotationUtil.wrapAngleDiff(targetRotations[0], lastYaw), lastYaw);
            float pitch = RotationUtil.quantizeAngle(targetRotations[1], lastPitch);
            pitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);
            event.setRotation(yaw, pitch, 0);
            if (this.moveFix.getValue() != 0) {
                event.setMoveYaw(yaw, 0);
            }
        }
        if (!Myau.packetStateTracker.attacked
                && !Myau.packetStateTracker.digging
                && !Myau.packetStateTracker.placing) {
            this.performSwing();
            if (RotationUtil.distanceToEntity(this.target) <= this.range.getValue().doubleValue()) {
                PacketUtil.sendPacket(new C02PacketUseEntity(this.target, C02PacketUseEntity.Action.ATTACK));
                PlayerUtil.attackEntity(this.target);
            }
        }
    }

    @EventTarget
    public void onRightClickDelay(RightClickDelayEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.moveFix.getValue() == 1
                && RotationState.isRotated()
                && RotationState.getPriority() == 0.0F
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getMoveYaw());
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.showTarget.getValue() != 0 && TeamUtil.isEntityLoaded(this.target)) {
            Color color = new Color(-1);
            switch (this.showTarget.getValue()) {
                case 1: {
                    double dotProduct = (this.target.posX - this.target.lastTickPosX) * (mc.thePlayer.posX - this.target.posX)
                            + (this.target.posY - this.target.lastTickPosY) * (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight() - this.target.posY - (double) this.target.height / 2.0)
                            + (this.target.posZ - this.target.lastTickPosZ) * (mc.thePlayer.posZ - this.target.posZ);
                    color = dotProduct < 0.0 ? new Color(0xFF5555) : new Color(0x55FF55);
                    break;
                }
                case 2: {
                    color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
                    break;
                }
            }
            RenderUtil.enableRenderState();
            RenderUtil.drawEntityBox(this.target, color.getRed(), color.getGreen(), color.getBlue(), 63, 0.1F);
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.trackedFireballs.clear();
        this.ignoredFireballs.clear();
    }
}
