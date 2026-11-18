package myau.module.modules;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class Indicators extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty scale = new FloatProperty("scale", 1.0f, 0.5f, 1.5f);
    public final FloatProperty offset = new FloatProperty("offset", 50.0f, 0.0f, 255.0f);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty fireballs = new BooleanProperty("fireballs", true);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);
    public final BooleanProperty arrows = new BooleanProperty("arrows", true);

    public Indicators() {
        super("Indicators", false, 0, true);
    }

    private boolean shouldRender(Entity entity) {
        double dot = (entity.posX - entity.lastTickPosX) * (mc.thePlayer.posX - entity.posX)
                + (entity.posY - entity.lastTickPosY) * (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight() - entity.posY - (double) entity.height / 2.0)
                + (entity.posZ - entity.lastTickPosZ) * (mc.thePlayer.posZ - entity.posZ);
        if (dot == 0.0) {
            return false;
        }
        if (dot < 0.0 && this.directionCheck.getValue()) {
            return false;
        }
        if (this.fireballs.getValue() && entity instanceof EntityFireball) {
            return true;
        }
        if (this.pearls.getValue() && entity instanceof EntityEnderPearl) {
            return true;
        }
        return this.arrows.getValue() && entity instanceof EntityArrow;
    }

    private Item getIndicatorItem(Entity entity) {
        if (entity instanceof EntityFireball) {
            return Items.fire_charge;
        }
        if (entity instanceof EntityEnderPearl) {
            return Items.ender_pearl;
        }
        if (entity instanceof EntityArrow) {
            return Items.arrow;
        }
        return new Item();
    }

    private Color getIndicatorColor(Entity entity) {
        if (entity instanceof EntityFireball) {
            return new Color(12676363);
        }
        if (entity instanceof EntityEnderPearl) {
            return new Color(2458740);
        }
        if (entity instanceof EntityArrow) {
            return new Color(0x969696);
        }
        return new Color(-1);
    }

    @EventTarget(priority = 50)
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        List<Entity> entities = TeamUtil.getLoadedEntitiesSorted().stream()
                .filter(this::shouldRender)
                .collect(Collectors.toList());
        float scaleValue = this.scale.getValue();
        for (Entity entity : entities) {
            float radius = 10.0f + this.offset.getValue();
            float yawBetween = RotationUtil.getYawBetween(
                    MathUtil.lerp(mc.thePlayer.prevPosX, mc.thePlayer.posX, event.getPartialTicks()),
                    MathUtil.lerp(mc.thePlayer.prevPosZ, mc.thePlayer.posZ, event.getPartialTicks()),
                    MathUtil.lerp(entity.prevPosX, entity.posX, event.getPartialTicks()),
                    MathUtil.lerp(entity.prevPosZ, entity.posZ, event.getPartialTicks())
            );
            if (mc.gameSettings.thirdPersonView == 2) {
                yawBetween += 180.0f;
            }
            float dirX = (float) Math.sin(Math.toRadians(yawBetween));
            float dirY = (float) Math.cos(Math.toRadians(yawBetween)) * -1.0f;
            ScaledResolution scaledResolution = new ScaledResolution(mc);

            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GlStateManager.scale(scaleValue, scaleValue, 0.0f);
            GlStateManager.translate(
                    (float) scaledResolution.getScaledWidth() / 2.0f / scaleValue,
                    (float) scaledResolution.getScaledHeight() / 2.0f / scaleValue,
                    0.0f
            );

            GlStateManager.pushMatrix();
            GlStateManager.translate(radius * dirX - 8.0f, radius * dirY - 8.0f, -300.0f);
            mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(this.getIndicatorItem(entity)), 0, 0);
            GlStateManager.popMatrix();

            String distanceText = String.format("%dm", (int) mc.thePlayer.getDistanceToEntity(entity));
            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    radius * dirX - (float) mc.fontRendererObj.getStringWidth(distanceText) / 2.0f + 1.0f,
                    radius * dirY + 1.0f,
                    -100.0f
            );
            mc.fontRendererObj.drawStringWithShadow(distanceText, 0.0f, 0.0f, ChatColors.GRAY.toRgb() & 0xFFFFFF | 0xBF000000);
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            GlStateManager.translate((radius + 15.0f) * dirX + 1.0f, (radius + 15.0f) * dirY + 1.0f, -100.0f);
            RenderUtil.enableRenderState();
            RenderUtil.drawArrow(0.0f, 0.0f, (float) (Math.atan2(dirY, dirX) + Math.PI), 7.5f, 1.5f, this.getIndicatorColor(entity).getRGB());
            RenderUtil.disableRenderState();
            GlStateManager.popMatrix();

            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }
}
