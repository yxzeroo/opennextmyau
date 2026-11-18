package myau.module.modules;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.events.Render2DEvent;
import myau.event.events.Render3DEvent;
import myau.event.events.ResizeEvent;
import myau.mixin.IAccessorEntityRenderer;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;
import myau.util.ColorUtil;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.util.shader.GlowShader;
import myau.util.shader.OutlineShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.util.vector.Vector4f;

public class ESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final GlowShader glowShader = new GlowShader();
    private final OutlineShader outlineShader = new OutlineShader();
    private Framebuffer framebuffer = null;
    private boolean renderTags = true;
    private boolean renderGlint = true;
    public final ModeProperty mode = new ModeProperty("mode", 2, new String[]{"NONE", "2D", "3D", "OUTLINE"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final ModeProperty healthBar = new ModeProperty("health-bar", 0, new String[]{"NONE", "2D", "RAVEN"});
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", true);
    public final BooleanProperty enemies = new BooleanProperty("enemies", true);
    public final BooleanProperty self = new BooleanProperty("self", false);
    public final BooleanProperty bots = new BooleanProperty("bots", false);

    public ESP() {
        super("ESP", false, 0);
    }

    private boolean shouldRenderPlayer(EntityPlayer player) {
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

    private Color getEntityColor(EntityPlayer player) {
        if (TeamUtil.isFriend(player)) {
            return Myau.friendManager.getColor();
        }
        if (TeamUtil.isEnemy(player)) {
            return Myau.enemyManager.getColor();
        }
        switch (this.color.getValue()) {
            case 0:
                return TeamUtil.getTeamColor(player, 1.0f);
            case 1:
                int teamRgb = TeamUtil.isSameTeam(player) ? ChatColors.BLUE.toRgb() : ChatColors.RED.toRgb();
                return new Color(teamRgb);
            case 2:
                HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                return new Color(hud.getColor(System.currentTimeMillis()).getRGB());
            default:
                return new Color(-1);
        }
    }

    public boolean shouldRenderTags() {
        return this.renderTags;
    }

    public boolean shouldRenderGlint() {
        return this.renderGlint;
    }

    @EventTarget
    public void onResize(ResizeEvent event) {
        if (this.framebuffer != null) {
            this.framebuffer.deleteFramebuffer();
        }
        this.framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
    }

    @EventTarget(priority = 50)
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        int espMode = this.mode.getValue();
        int healthBarMode = this.healthBar.getValue();
        if (espMode != 1 && espMode != 3 && healthBarMode != 1) {
            return;
        }
        if (espMode == 3 && !OpenGlHelper.isFramebufferEnabled()) {
            this.setEnabled(false);
            ChatUtil.sendFormatted(String.format("%s%s: &cFramebuffer is not enabled!&r", Myau.clientName, this.getName()));
            return;
        }
        List<EntityPlayer> playersToRender = TeamUtil.getLoadedEntitiesSorted().stream()
                .filter(EntityPlayer.class::isInstance)
                .map(EntityPlayer.class::cast)
                .filter(player -> this.shouldRenderPlayer(player) && RenderUtil.isInViewFrustum(player, 0.1f))
                .collect(Collectors.toList());
        if (playersToRender.isEmpty()) {
            return;
        }
        if (espMode == 3) {
            GlStateManager.pushMatrix();
            GlStateManager.pushAttrib();
            if (this.framebuffer == null) {
                this.framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            } else {
                this.framebuffer.framebufferClear();
            }
            this.framebuffer.bindFramebuffer(false);
            ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(event.getPartialTicks(), 0);
            boolean entityShadows = mc.gameSettings.entityShadows;
            mc.gameSettings.entityShadows = false;
            this.renderTags = false;
            this.renderGlint = false;
            this.outlineShader.use();
            for (EntityPlayer player : playersToRender) {
                Color outlineColor = this.getEntityColor(player);
                this.outlineShader.setColor(outlineColor);
                boolean invisible = player.isInvisible();
                player.setInvisible(false);
                mc.getRenderManager().renderEntityStatic(player, event.getPartialTicks(), true);
                player.setInvisible(invisible);
            }
            this.outlineShader.stop();
            this.renderGlint = true;
            this.renderTags = true;
            mc.gameSettings.entityShadows = entityShadows;
            mc.entityRenderer.setupOverlayRendering();
            mc.getFramebuffer().bindFramebuffer(false);
            this.glowShader.use();
            RenderUtil.drawFramebuffer(this.framebuffer);
            this.glowShader.stop();
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }
        if (espMode == 1 || healthBarMode == 1) {
            Map<EntityPlayer, Vector4f> screenBoundsMap = new HashMap<>();
            ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(event.getPartialTicks(), 0);
            for (EntityPlayer player : playersToRender) {
                screenBoundsMap.put(player, RenderUtil.projectToScreen(player, 0.1f));
            }
            mc.entityRenderer.setupOverlayRendering();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            for (EntityPlayer player : playersToRender) {
                Vector4f bounds = screenBoundsMap.get(player);
                if (bounds == null) {
                    continue;
                }
                float minX = bounds.x;
                float minY = bounds.y;
                float maxX = bounds.z;
                float maxY = bounds.w;
                if (espMode == 1) {
                    int boxRgb = this.getEntityColor(player).getRGB();
                    int shadowRgb = (boxRgb & 0xFCFCFC) >> 2 | boxRgb & 0xFF000000;
                    RenderUtil.drawBorderedRect(minX, minY, maxX, maxY, 3.0f, 0, shadowRgb);
                    RenderUtil.drawBorderedRect(minX, minY, maxX, maxY, 1.5f, 0, boxRgb);
                }
                if (healthBarMode == 1) {
                    float totalHealth = player.getHealth() + player.getAbsorptionAmount();
                    float healthRatio = Math.min(Math.max(totalHealth / player.getMaxHealth(), 0.0f), 1.0f);
                    float barOffset = (maxX - minX) * 0.08f;
                    int healthRgb = ColorUtil.getHealthBlend(healthRatio).getRGB();
                    int healthShadowRgb = (healthRgb & 0xFCFCFC) >> 2 | healthRgb & 0xFF000000;
                    RenderUtil.drawLine2D(minX - barOffset, minY, minX - barOffset, maxY, 3.0f, healthShadowRgb);
                    RenderUtil.drawLine2D(minX - barOffset, maxY, minX - barOffset, maxY + (minY - maxY) * healthRatio, 1.5f, healthRgb);
                }
            }
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        int espMode = this.mode.getValue();
        int healthBarMode = this.healthBar.getValue();
        if (espMode != 2 && healthBarMode != 2) {
            return;
        }
        if (espMode == 3 && !OpenGlHelper.isFramebufferEnabled()) {
            this.setEnabled(false);
            ChatUtil.sendFormatted(String.format("%s%s: &cFramebuffer is not enabled!&r", Myau.clientName, this.getName()));
            return;
        }
        List<EntityPlayer> playersToRender = TeamUtil.getLoadedEntitiesSorted().stream()
                .filter(EntityPlayer.class::isInstance)
                .map(EntityPlayer.class::cast)
                .filter(player -> this.shouldRenderPlayer(player) && RenderUtil.isInViewFrustum(player, 0.1f))
                .collect(Collectors.toList());
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        RenderUtil.enableRenderState();
        for (EntityPlayer player : playersToRender) {
            if (espMode == 2) {
                Color boxColor = this.getEntityColor(player);
                RenderUtil.drawEntityBoundingBox(player, boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), boxColor.getAlpha(), 1.5f, 0.1f);
            }
            if (healthBarMode == 2) {
                double renderX = MathUtil.lerp(player.lastTickPosX, player.posX, event.getPartialTicks()) - renderManager.getRenderPosX();
                double renderY = MathUtil.lerp(player.lastTickPosY, player.posY, event.getPartialTicks()) - renderManager.getRenderPosY() - (double) 0.1f;
                double renderZ = MathUtil.lerp(player.lastTickPosZ, player.posZ, event.getPartialTicks()) - renderManager.getRenderPosZ();
                GlStateManager.pushMatrix();
                GlStateManager.translate(renderX, renderY, renderZ);
                GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0f, 0.0f, 1.0f, 0.0f);
                float totalHealth = player.getHealth() + player.getAbsorptionAmount();
                float healthRatio = Math.min(Math.max(totalHealth / player.getMaxHealth(), 0.0f), 1.0f);
                Color healthColor = ColorUtil.getHealthBlend(healthRatio);
                float barX = 0.6f;
                float barHeight = player.height + 0.2f;
                float barWidth = 0.1f;
                float borderPadding = barWidth * 0.275f;
                RenderUtil.drawSmoothRect(barX - borderPadding, borderPadding * -1.0f, barX + barWidth + borderPadding, barHeight + borderPadding, Color.black.getRGB());
                RenderUtil.drawSmoothRect(barX, 0.0f, barX + barWidth, barHeight, Color.darkGray.getRGB());
                RenderUtil.drawSmoothRect(barX, 0.0f, barX + barWidth, barHeight * healthRatio, healthColor.getRGB());
                GlStateManager.popMatrix();
            }
        }
        RenderUtil.disableRenderState();
    }

    @Override
    public void onEnabled() {
        if (this.mode.getValue() == 3 && !OpenGlHelper.isFramebufferEnabled()) {
            this.setEnabled(false);
            ChatUtil.sendFormatted(String.format("%s%s: &cFramebuffer is not enabled!&r", Myau.clientName, this.getName()));
        }
    }
}
