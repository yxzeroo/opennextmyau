package myau.module.modules;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ColorUtil;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

public class NameTags extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    public final FloatProperty scale = new FloatProperty("scale", 1.0f, 0.5f, 2.0f);
    public final BooleanProperty autoScale = new BooleanProperty("auto-scale", true);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final ModeProperty distance = new ModeProperty("distance", 0, new String[]{"NONE", "DEFAULT", "VAPE"});
    public final ModeProperty health = new ModeProperty("health", 2, new String[]{"NONE", "HP", "HEARTS", "TAB"});
    public final BooleanProperty armor = new BooleanProperty("armor", true);
    public final BooleanProperty effects = new BooleanProperty("effects", true);
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", true);
    public final BooleanProperty enemies = new BooleanProperty("enemies", true);
    public final BooleanProperty bosses = new BooleanProperty("bosses", false);
    public final BooleanProperty mobs = new BooleanProperty("mobs", false);
    public final BooleanProperty creepers = new BooleanProperty("creepers", false);
    public final BooleanProperty endermen = new BooleanProperty("endermen", false);
    public final BooleanProperty blazes = new BooleanProperty("blazes", false);
    public final BooleanProperty animals = new BooleanProperty("animals", false);
    public final BooleanProperty self = new BooleanProperty("self", false);
    public final BooleanProperty bots = new BooleanProperty("bots", false);

    public NameTags() {
        super("NameTags", false, 0);
    }

    public boolean shouldRenderTags(EntityLivingBase entity) {
        if (entity.deathTime > 0) {
            return false;
        }
        if (mc.getRenderViewEntity().getDistanceToEntity(entity) > 512.0f) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (TeamUtil.isSelf(player)) {
                return this.self.getValue() && mc.gameSettings.thirdPersonView != 0;
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
        if (entity instanceof EntityDragon || entity instanceof EntityWither) {
            return !entity.isInvisible() && this.bosses.getValue();
        }
        if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            if (entity instanceof EntityCreeper) {
                return this.creepers.getValue();
            }
            if (entity instanceof EntityEnderman) {
                return this.endermen.getValue();
            }
            if (entity instanceof EntityBlaze) {
                return this.blazes.getValue();
            }
            return this.mobs.getValue();
        }
        if (entity instanceof EntityAnimal
                || entity instanceof EntityBat
                || entity instanceof EntitySquid
                || entity instanceof EntityVillager) {
            return this.animals.getValue();
        }
        return false;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!this.shouldRenderTags(living)) {
                continue;
            }
            if (!entity.ignoreFrustumCheck && !RenderUtil.isInViewFrustum(entity, 10.0)) {
                continue;
            }
            String entityName = TeamUtil.stripLastColorCode(entity);
            if (StringUtils.isBlank(EnumChatFormatting.getTextWithoutFormattingCodes(entityName))) {
                continue;
            }
            double renderX = MathUtil.lerp(entity.lastTickPosX, entity.posX, event.getPartialTicks()) - renderManager.getRenderPosX();
            double renderY = MathUtil.lerp(entity.lastTickPosY, entity.posY, event.getPartialTicks()) - renderManager.getRenderPosY() + (double) entity.getEyeHeight();
            double renderZ = MathUtil.lerp(entity.lastTickPosZ, entity.posZ, event.getPartialTicks()) - renderManager.getRenderPosZ();
            double entityDistance = mc.getRenderViewEntity().getDistanceToEntity(entity);
            GlStateManager.pushMatrix();
            GlStateManager.translate(renderX, renderY + (entity.isSneaking() ? 0.225 : 0.4), renderZ);
            GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0f, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, mc.gameSettings.thirdPersonView == 2 ? -1.0f : 1.0f, 0.0f, 0.0f);
            double baseScale = Math.pow(Math.min(Math.max(this.autoScale.getValue() ? entityDistance : 0.0, 6.0), 128.0), 0.75) * 0.0075;
            double tagScale = baseScale * (double) this.scale.getValue();
            GlStateManager.scale(-tagScale, -tagScale, 1.0);
            String distancePrefix = "";
            switch (this.distance.getValue()) {
                case 1:
                    distancePrefix = String.format("&7%dm&r ", (int) entityDistance);
                    break;
                case 2:
                    distancePrefix = String.format("&a[&f%d&a]&r ", (int) entityDistance);
                    break;
                default:
                    break;
            }
            float currentHealth = living.getHealth();
            float absorption = living.getAbsorptionAmount();
            float maxHealth = living.getMaxHealth();
            float healthRatio = Math.min(Math.max((currentHealth + absorption) / maxHealth, 0.0f), 1.0f);
            String healthSuffix = "";
            switch (this.health.getValue()) {
                case 1:
                    String absorptionHp = absorption > 0.0f ? String.format(" &6%d&r", (int) absorption) : "&r";
                    healthSuffix = String.format(" %d%s", (int) currentHealth, absorptionHp);
                    break;
                case 2:
                    String absorptionHearts = absorption > 0.0f
                            ? String.format(" &6%s&r", HEALTH_FORMAT.format((double) absorption / 2.0))
                            : "&r";
                    healthSuffix = String.format(" %s%s", HEALTH_FORMAT.format((double) currentHealth / 2.0), absorptionHearts);
                    break;
                case 3:
                    if (entity instanceof EntityPlayer) {
                        Scoreboard scoreboard = mc.theWorld.getScoreboard();
                        if (scoreboard != null) {
                            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);
                            if (objective != null) {
                                Score score = scoreboard.getValueFromObjective(entity.getName(), objective);
                                if (score != null) {
                                    healthSuffix = String.format(" &e%d&r", score.getScorePoints());
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
            String tagText = ChatColors.formatColor(String.format("%s&f%s&r%s", distancePrefix, entityName, healthSuffix));
            int textWidth = mc.fontRendererObj.getStringWidth(tagText);
            boolean drawShadow = this.shadow.getValue();
            if (this.background.getValue() > 0) {
                float bgAlpha = (float) this.background.getValue() / 100.0f;
                Color bgColor = entity.isSneaking() || entity.isInvisible()
                        ? new Color(0.33f, 0.0f, 0.33f, bgAlpha)
                        : new Color(0.0f, 0.0f, 0.0f, bgAlpha);
                RenderUtil.enableRenderState();
                RenderUtil.drawRect(
                        (float) (-textWidth) / 2.0f - 1.0f,
                        (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0f,
                        (float) textWidth / 2.0f + (drawShadow ? 1.0f : 0.0f),
                        drawShadow ? 0.0f : -1.0f,
                        bgColor.getRGB()
                );
                RenderUtil.disableRenderState();
            }
            GlStateManager.disableDepth();
            mc.fontRendererObj.drawString(
                    tagText,
                    (float) (-textWidth) / 2.0f,
                    -mc.fontRendererObj.FONT_HEIGHT,
                    ColorUtil.getHealthBlend(healthRatio).getRGB(),
                    drawShadow
            );
            GlStateManager.enableDepth();
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                int verticalOffset = mc.fontRendererObj.FONT_HEIGHT + 2;
                if (this.armor.getValue()) {
                    List<ItemStack> equipment = new ArrayList<>();
                    for (int slot = 4; slot >= 0; --slot) {
                        ItemStack stack = slot == 0 ? player.getHeldItem() : player.inventory.armorInventory[slot - 1];
                        if (stack != null) {
                            equipment.add(stack);
                        }
                    }
                    if (!equipment.isEmpty()) {
                        int startX = equipment.size() * -8;
                        for (int index = 0; index < equipment.size(); ++index) {
                            RenderUtil.renderItemInGUI(equipment.get(index), startX + index * 16, -verticalOffset - 16);
                        }
                        verticalOffset += 16;
                    }
                }
                if (this.effects.getValue()) {
                    List<PotionEffect> activeEffects = player.getActivePotionEffects().stream()
                            .filter(effect -> Potion.potionTypes[effect.getPotionID()].hasStatusIcon())
                            .collect(Collectors.toList());
                    if (!activeEffects.isEmpty()) {
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(0.5f, 0.5f, 1.0f);
                        int startX = activeEffects.size() * -9;
                        for (int index = 0; index < activeEffects.size(); ++index) {
                            RenderUtil.renderPotionEffect(activeEffects.get(index), startX + index * 18, -(verticalOffset * 2) - 18);
                        }
                        GlStateManager.popMatrix();
                    }
                }
                if (TeamUtil.isFriend(player)) {
                    RenderUtil.enableRenderState();
                    RenderUtil.drawBorderedRect(
                            (float) (-textWidth) / 2.0f - 1.0f,
                            (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0f,
                            (float) textWidth / 2.0f + 1.0f,
                            drawShadow ? 0.0f : -1.0f,
                            1.5f,
                            0,
                            Myau.friendManager.getColor().getRGB()
                    );
                    RenderUtil.disableRenderState();
                } else if (TeamUtil.isEnemy(player)) {
                    RenderUtil.enableRenderState();
                    RenderUtil.drawBorderedRect(
                            (float) (-textWidth) / 2.0f - 1.0f,
                            (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0f,
                            (float) textWidth / 2.0f + 1.0f,
                            drawShadow ? 0.0f : -1.0f,
                            1.5f,
                            0,
                            Myau.enemyManager.getColor().getRGB()
                    );
                    RenderUtil.disableRenderState();
                }
            }
            GlStateManager.popMatrix();
        }
    }
}
