package myau.module.modules;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.events.Render2DEvent;
import myau.event.events.SendPacketEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ColorUtil;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.ResourceLocation;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastAttackedTarget = null;
    private EntityLivingBase target = null;
    private ResourceLocation headTexture = null;
    private float oldHealth = 0.0f;
    private float newHealth = 0.0f;
    private float maxHealth = 0.0f;

    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "HUD"});
    public final ModeProperty positionX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty positionY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final FloatProperty scale = new FloatProperty("scale", 1.0f, 0.5f, 1.5f);
    public final IntProperty offsetX = new IntProperty("offset-x", 0, -255, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 40, -255, 255);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty head = new BooleanProperty("head", true);
    public final BooleanProperty indicator = new BooleanProperty("indicator", true);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty animations = new BooleanProperty("animations", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);

    public TargetHUD() {
        super("TargetHUD", false, 0, true);
    }

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        }
        if (!this.kaOnly.getValue() && !this.lastAttackTimer.hasTimeElapsed(1500L) && TeamUtil.isEntityLoaded(this.lastAttackedTarget)) {
            return this.lastAttackedTarget;
        }
        if (this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat) {
            return mc.thePlayer;
        }
        return null;
    }

    private ResourceLocation getSkinTexture(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(entity.getName());
            if (playerInfo != null) {
                return playerInfo.getLocationSkin();
            }
        }
        return null;
    }

    private Color getTargetColor(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entity)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isEnemy((EntityPlayer) entity)) {
                return Myau.enemyManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entity instanceof EntityPlayer)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((EntityPlayer) entity, 1.0f);
            case 1: {
                int rgb = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(rgb);
            }
            default:
                return new Color(-1);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }
        EntityLivingBase previousTarget = this.target;
        this.target = this.resolveTarget();
        if (this.target == null) {
            return;
        }

        float playerHealth = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0f;
        float absorption = this.target.getAbsorptionAmount() / 2.0f;
        float targetHealth = this.target.getHealth() / 2.0f + absorption;
        if (this.target != previousTarget) {
            this.headTexture = null;
            this.animTimer.setTime(0L);
            this.oldHealth = targetHealth;
            this.newHealth = targetHealth;
        }

        if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
            this.oldHealth = this.newHealth;
            this.newHealth = targetHealth;
            this.maxHealth = this.target.getMaxHealth() / 2.0f;
            if (this.oldHealth != this.newHealth) {
                this.animTimer.reset();
            }
        }

        ResourceLocation skinTexture = this.getSkinTexture(this.target);
        if (skinTexture != null) {
            this.headTexture = skinTexture;
        }

        float elapsed = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
        float animatedHealth = MathUtil.lerp(this.oldHealth, this.newHealth, elapsed / 150.0f);
        float healthRatio = Math.min(Math.max(animatedHealth / this.maxHealth, 0.0f), 1.0f);
        Color targetColor = this.getTargetColor(this.target);
        Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(healthRatio) : targetColor;
        float diffRatio = Math.min(Math.max((playerHealth - targetHealth + 1.0f) / 2.0f, 0.0f), 1.0f);
        Color indicatorColor = ColorUtil.getHealthBlend(diffRatio);

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripLastColorCode(this.target)));
        int targetNameWidth = mc.fontRendererObj.getStringWidth(targetNameText);
        String healthText = ChatColors.formatColor(String.format(
                "&r&f%s%s\u2764&r",
                healthFormat.format(targetHealth),
                absorption > 0.0f ? "&6" : "&c"
        ));
        int healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
        String statusText = ChatColors.formatColor(String.format(
                "&r&l%s&r",
                targetHealth == playerHealth ? "D" : (targetHealth < playerHealth ? "W" : "L")
        ));
        int statusTextWidth = mc.fontRendererObj.getStringWidth(statusText);
        String healthDiffText = ChatColors.formatColor(String.format(
                "&r%s&r",
                targetHealth == playerHealth ? "0.0" : diffFormat.format(playerHealth - targetHealth)
        ));
        int healthDiffWidth = mc.fontRendererObj.getStringWidth(healthDiffText);

        float maxContentWidth = Math.max(
                (float) targetNameWidth + (this.indicator.getValue() ? 2.0f + (float) statusTextWidth + 2.0f : 0.0f),
                (float) healthTextWidth + (this.indicator.getValue() ? 2.0f + (float) healthDiffWidth + 2.0f : 0.0f)
        );
        float headIconOffset = this.head.getValue() && this.headTexture != null ? 25.0f : 0.0f;
        float boxWidth = Math.max(headIconOffset + 70.0f, headIconOffset + 2.0f + maxContentWidth + 2.0f);
        float boxHeight = 27.0f;
        float scaleValue = this.scale.getValue();

        float posX = (float) this.offsetX.getValue() / scaleValue;
        switch (this.positionX.getValue()) {
            case 1:
                posX += (float) scaledResolution.getScaledWidth() / scaleValue / 2.0f - boxWidth / 2.0f;
                break;
            case 2:
                posX *= -1.0f;
                posX += (float) scaledResolution.getScaledWidth() / scaleValue - boxWidth;
                break;
        }

        float posY = (float) this.offsetY.getValue() / scaleValue;
        switch (this.positionY.getValue()) {
            case 1:
                posY += (float) scaledResolution.getScaledHeight() / scaleValue / 2.0f - boxHeight / 2.0f;
                break;
            case 2:
                posY *= -1.0f;
                posY += (float) scaledResolution.getScaledHeight() / scaleValue - boxHeight;
                break;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleValue, scaleValue, 0.0f);
        GlStateManager.translate(posX, posY, -450.0f);

        RenderUtil.enableRenderState();
        RenderUtil.drawBorderedRect(
                0.0f,
                0.0f,
                boxWidth,
                boxHeight,
                1.5f,
                new Color(0.0f, 0.0f, 0.0f, (float) this.background.getValue() / 100.0f).getRGB(),
                this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB()
        );
        RenderUtil.drawRect(headIconOffset + 2.0f, 22.0f, boxWidth - 2.0f, 25.0f, ColorUtil.darker(healthBarColor, 0.2f).getRGB());
        RenderUtil.drawRect(
                headIconOffset + 2.0f,
                22.0f,
                headIconOffset + 2.0f + healthRatio * (boxWidth - 2.0f - headIconOffset - 2.0f),
                25.0f,
                healthBarColor.getRGB()
        );
        RenderUtil.disableRenderState();

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        boolean useShadow = this.shadow.getValue();
        mc.fontRendererObj.drawString(targetNameText, headIconOffset + 2.0f, 2.0f, -1, useShadow);
        mc.fontRendererObj.drawString(healthText, headIconOffset + 2.0f, 12.0f, -1, useShadow);
        if (this.indicator.getValue()) {
            mc.fontRendererObj.drawString(statusText, boxWidth - 2.0f - (float) statusTextWidth, 2.0f, indicatorColor.getRGB(), useShadow);
            mc.fontRendererObj.drawString(healthDiffText, boxWidth - 2.0f - (float) healthDiffWidth, 12.0f, ColorUtil.darker(indicatorColor, 0.8f).getRGB(), useShadow);
        }
        if (this.head.getValue() && this.headTexture != null) {
            GlStateManager.color(1.0f, 1.0f, 1.0f);
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(2, 2, 8.0f, 8.0f, 8, 8, 23, 23, 64.0f, 64.0f);
            Gui.drawScaledCustomSizeModalRect(2, 2, 40.0f, 8.0f, 8, 8, 23, 23, 64.0f, 64.0f);
            GlStateManager.color(1.0f, 1.0f, 1.0f);
        }
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    @EventTarget
    public void onSendPacket(SendPacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand)) {
                this.lastAttackTimer.reset();
                this.lastAttackedTarget = (EntityLivingBase) entity;
            }
        }
    }
}
