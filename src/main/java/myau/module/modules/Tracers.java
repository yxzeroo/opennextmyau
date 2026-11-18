package myau.module.modules;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.events.Render2DEvent;
import myau.event.events.Render3DEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class Tracers extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final BooleanProperty lines = new BooleanProperty("lines", true);
    public final BooleanProperty arrows = new BooleanProperty("arrows", false);
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", true);
    public final BooleanProperty enemies = new BooleanProperty("enemies", true);
    public final BooleanProperty bots = new BooleanProperty("bots", false);

    public Tracers() {
        super("Tracers", false, 0);
    }

    private boolean shouldRenderTracer(EntityPlayer player) {
        if (player.deathTime > 0) {
            return false;
        }
        if (mc.getRenderViewEntity().getDistanceToEntity(player) > 512.0f) {
            return false;
        }
        if (TeamUtil.isSelf(player)) {
            return false;
        }
        if (TeamUtil.isFriend(player)) {
            return this.friends.getValue();
        }
        if (TeamUtil.isEnemy(player)) {
            return this.enemies.getValue();
        }
        if (TeamUtil.isBot(player)) {
            return this.bots.getValue();
        }
        return this.players.getValue();
    }

    private Color getTracerColor(EntityPlayer player, float alpha) {
        if (TeamUtil.isFriend(player)) {
            Color friendColor = Myau.friendManager.getColor();
            return new Color(
                    (float) friendColor.getRed() / 255.0f,
                    (float) friendColor.getGreen() / 255.0f,
                    (float) friendColor.getBlue() / 255.0f,
                    alpha
            );
        }
        if (TeamUtil.isEnemy(player)) {
            Color enemyColor = Myau.enemyManager.getColor();
            return new Color(
                    (float) enemyColor.getRed() / 255.0f,
                    (float) enemyColor.getGreen() / 255.0f,
                    (float) enemyColor.getBlue() / 255.0f,
                    alpha
            );
        }
        switch (this.color.getValue()) {
            case 0:
                return TeamUtil.getTeamColor(player, alpha);
            case 1:
                int teamRgb = TeamUtil.isSameTeam(player) ? ChatColors.BLUE.toRgb() : ChatColors.RED.toRgb();
                return new Color(teamRgb & 0xFFFFFF | (int) (alpha * 255.0f) << 24, true);
            case 2:
                HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                int hudRgb = hud.getColor(System.currentTimeMillis()).getRGB();
                return new Color(hudRgb & 0xFFFFFF | (int) (alpha * 255.0f) << 24, true);
            default:
                return new Color(1.0f, 1.0f, 1.0f, alpha);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || !this.lines.getValue()) {
            return;
        }
        RenderUtil.enableRenderState();
        float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        Vec3 eyeVector;
        if (mc.gameSettings.thirdPersonView == 0) {
            Entity viewEntity = mc.getRenderViewEntity();
            eyeVector = new Vec3(0.0, 0.0, 1.0)
                    .rotatePitch((float) (-Math.toRadians(MathUtil.lerp(viewEntity.prevRotationPitch, viewEntity.rotationPitch, partialTicks))))
                    .rotateYaw((float) (-Math.toRadians(MathUtil.lerp(viewEntity.prevRotationYaw, viewEntity.rotationYaw, partialTicks))));
        } else {
            eyeVector = new Vec3(0.0, 0.0, 0.0)
                    .rotatePitch((float) (-Math.toRadians(MathUtil.lerp(mc.thePlayer.prevCameraPitch, mc.thePlayer.cameraPitch, partialTicks))))
                    .rotateYaw((float) (-Math.toRadians(MathUtil.lerp(mc.thePlayer.prevCameraYaw, mc.thePlayer.cameraYaw, partialTicks))));
        }
        eyeVector = new Vec3(eyeVector.xCoord, eyeVector.yCoord + (double) mc.getRenderViewEntity().getEyeHeight(), eyeVector.zCoord);
        List<EntityPlayer> targets = TeamUtil.getLoadedEntitiesSorted().stream()
                .filter(entity -> entity instanceof EntityPlayer && this.shouldRenderTracer((EntityPlayer) entity))
                .map(EntityPlayer.class::cast)
                .collect(Collectors.toList());
        for (EntityPlayer player : targets) {
            Color tracerColor = this.getTracerColor(player, (float) this.opacity.getValue() / 100.0f);
            double targetX = MathUtil.lerp(player.lastTickPosX, player.posX, event.getPartialTicks());
            double targetY = MathUtil.lerp(player.lastTickPosY, player.posY, event.getPartialTicks()) - (player.isSneaking() ? 0.125 : 0.0);
            double targetZ = MathUtil.lerp(player.lastTickPosZ, player.posZ, event.getPartialTicks());
            RenderUtil.drawTracerLine(
                    eyeVector,
                    targetX,
                    targetY + (double) player.getEyeHeight(),
                    targetZ,
                    (float) tracerColor.getRed() / 255.0f,
                    (float) tracerColor.getGreen() / 255.0f,
                    (float) tracerColor.getBlue() / 255.0f,
                    (float) tracerColor.getAlpha() / 255.0f,
                    1.5f
            );
        }
        RenderUtil.disableRenderState();
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || !this.arrows.getValue()) {
            return;
        }
        List<EntityPlayer> targets = TeamUtil.getLoadedEntitiesSorted().stream()
                .filter(entity -> entity instanceof EntityPlayer && this.shouldRenderTracer((EntityPlayer) entity))
                .map(EntityPlayer.class::cast)
                .collect(Collectors.toList());
        for (EntityPlayer player : targets) {
            float radius = 40.0f;
            float yawDelta = RotationUtil.getYawBetween(
                    MathUtil.lerp(mc.thePlayer.prevPosX, mc.thePlayer.posX, event.getPartialTicks()),
                    MathUtil.lerp(mc.thePlayer.prevPosZ, mc.thePlayer.posZ, event.getPartialTicks()),
                    MathUtil.lerp(player.prevPosX, player.posX, event.getPartialTicks()),
                    MathUtil.lerp(player.prevPosZ, player.posZ, event.getPartialTicks())
            );
            if (mc.gameSettings.thirdPersonView == 2) {
                yawDelta += 180.0f;
            }
            float dirX = (float) Math.sin(Math.toRadians(yawDelta));
            float dirY = (float) Math.cos(Math.toRadians(yawDelta)) * -1.0f;
            float alpha = (float) this.opacity.getValue() / 100.0f;
            float absAngle = Math.abs(MathHelper.wrapAngleTo180_float(yawDelta));
            if (absAngle < 30.0f) {
                alpha = 0.0f;
            } else if (absAngle < 60.0f) {
                alpha *= (absAngle - 30.0f) / 30.0f;
            }
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            float hudScale = hud.scale.getValue();
            ScaledResolution resolution = new ScaledResolution(mc);
            GlStateManager.pushMatrix();
            GlStateManager.scale(hudScale, hudScale, 0.0f);
            GlStateManager.translate(
                    (float) resolution.getScaledWidth() / 2.0f / hudScale,
                    (float) resolution.getScaledHeight() / 2.0f / hudScale,
                    0.0f
            );
            GlStateManager.pushMatrix();
            GlStateManager.translate((radius + 15.0f) * dirX + 1.0f, (radius + 15.0f) * dirY + 1.0f, -100.0f);
            RenderUtil.enableRenderState();
            RenderUtil.drawTriangle(
                    0.0f,
                    0.0f,
                    (float) (Math.atan2(dirY, dirX) + Math.PI),
                    10.0f,
                    this.getTracerColor(player, alpha).getRGB()
            );
            RenderUtil.disableRenderState();
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        }
    }
}
