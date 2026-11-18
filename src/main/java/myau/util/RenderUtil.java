package myau.util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import myau.mixin.IAccessorEntityRenderer;
import myau.mixin.IAccessorMinecraft;
import myau.mixin.IAccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class RenderUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Frustum FRUSTUM = new Frustum();
    private static final IntBuffer VIEWPORT = GLAllocation.createDirectIntBuffer(16);
    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer SCREEN_COORDS = GLAllocation.createDirectFloatBuffer(4);

    public static void drawOutlinedString(String text, float x, float y, int color) {
        String stripped = text.replaceAll("(?i)\u00a7[\\da-f]", "");
        mc.fontRendererObj.drawString(stripped, x + 1.0f, y, 0, false);
        mc.fontRendererObj.drawString(stripped, x - 1.0f, y, 0, false);
        mc.fontRendererObj.drawString(stripped, x, y + 1.0f, 0, false);
        mc.fontRendererObj.drawString(stripped, x, y - 1.0f, 0, false);
        mc.fontRendererObj.drawString(text, x, y, color, false);
    }

    public static void renderItemInGUI(ItemStack stack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glDisable(2896);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        mc.getRenderItem().zLevel = -150.0f;
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, x, y);
        mc.getRenderItem().zLevel = 0.0f;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        GlStateManager.disableDepth();
        ArrayList<String> enchantments = ItemEnchantmentUtil.getEnchantmentStrings(stack);
        for (int i = 0; i < enchantments.size(); ++i) {
            drawOutlinedString(enchantments.get(i), (float) x * 2.0f, ((float) y + (float) i * 4.0f) * 2.0f, -1);
        }
        GlStateManager.enableDepth();
        GlStateManager.scale(2.0f, 2.0f, 2.0f);
        GlStateManager.popMatrix();
    }

    public static void renderPotionEffect(PotionEffect effect, int x, int y) {
        Potion potion = Potion.potionTypes[effect.getPotionID()];
        int iconIndex = potion.getStatusIconIndex();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));
        Gui.drawModalRectWithCustomSizedTexture(x, y, iconIndex % 8 * 18, 198 + iconIndex / 8 * 18, 18, 18, 256.0f, 256.0f);
        GlStateManager.popMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) {
            return;
        }
        setColor(color);
        GL11.glBegin(9);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glEnd();
        GlStateManager.resetColor();
    }

    public static void drawBorderedRect(float x1, float y1, float x2, float y2, float lineWidth, int fillColor, int outlineColor) {
        drawRect(x1, y1, x2, y2, fillColor);
        if (lineWidth <= 0.0f || outlineColor == 0) {
            return;
        }
        setColor(outlineColor);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glBegin(1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawSmoothRect(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) {
            return;
        }
        setColor(color);
        GL11.glEnable(2881);
        GL11.glHint(3155, 4354);
        GL11.glBegin(9);
        for (int i = 0; i < 2; ++i) {
            GL11.glVertex2f(x1, y1);
            GL11.glVertex2f(x1, y2);
            GL11.glVertex2f(x2, y2);
            GL11.glVertex2f(x2, y1);
        }
        GL11.glEnd();
        GL11.glDisable(2881);
        GlStateManager.resetColor();
    }

    public static void drawLine2D(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glBegin(1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawTracerLine(Vec3 start, double endX, double endY, double endZ, float red, float green, float blue, float alpha, float lineWidth) {
        GlStateManager.pushMatrix();
        GlStateManager.color(red, green, blue, alpha);
        boolean viewBobbing = mc.gameSettings.viewBobbing;
        mc.gameSettings.viewBobbing = false;
        ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft) mc).getTimer().renderPartialTicks, 2);
        mc.gameSettings.viewBobbing = viewBobbing;
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glBegin(1);
        GL11.glVertex3d(start.xCoord, start.yCoord, start.zCoord);
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        GL11.glVertex3d(endX - renderManager.getRenderPosX(), endY - renderManager.getRenderPosY(), endZ - renderManager.getRenderPosZ());
        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    public static void drawArrow(float x, float y, float angle, float length, float lineWidth, int color) {
        float leftAngle = angle + (float) Math.toRadians(45.0);
        float rightAngle = angle - (float) Math.toRadians(45.0);
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glBegin(1);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + length * (float) Math.cos(leftAngle), y + length * (float) Math.sin(leftAngle));
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + length * (float) Math.cos(rightAngle), y + length * (float) Math.sin(rightAngle));
        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawTriangle(float x, float y, float angle, float size, int color) {
        float leftAngle = angle + (float) Math.toRadians(26.25);
        float rightAngle = angle - (float) Math.toRadians(26.25);
        setColor(color);
        GL11.glEnable(2881);
        GL11.glHint(3155, 4354);
        GL11.glBegin(9);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + size * (float) Math.cos(leftAngle), y + size * (float) Math.sin(leftAngle));
        GL11.glVertex2f(x + size * (float) Math.cos(rightAngle), y + size * (float) Math.sin(rightAngle));
        GL11.glEnd();
        GL11.glDisable(2881);
        GlStateManager.resetColor();
    }

    public static void drawFramebuffer(Framebuffer framebuffer) {
        ScaledResolution resolution = new ScaledResolution(mc);
        GlStateManager.bindTexture(framebuffer.framebufferTexture);
        GL11.glBegin(7);
        GL11.glTexCoord2d(0.0, 1.0);
        GL11.glVertex2d(0.0, 0.0);
        GL11.glTexCoord2d(0.0, 0.0);
        GL11.glVertex2d(0.0, resolution.getScaledHeight());
        GL11.glTexCoord2d(1.0, 0.0);
        GL11.glVertex2d(resolution.getScaledWidth(), resolution.getScaledHeight());
        GL11.glTexCoord2d(1.0, 1.0);
        GL11.glVertex2d(resolution.getScaledWidth(), 0.0);
        GL11.glEnd();
    }

    public static void drawCircle(double x, double y, double z, double radius, int segments, float lineWidth, int color) {
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glBegin(2);
        for (int i = 0; i <= segments; ++i) {
            double angle = (double) i * (Math.PI * 2.0 / (double) segments);
            GL11.glVertex3d(x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawEntityCircle(Entity entity, double radius, int segments, float lineWidth, int color) {
        float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        double x = MathUtil.lerp(entity.lastTickPosX, entity.posX, partialTicks) - renderManager.getRenderPosX();
        double y = MathUtil.lerp(entity.lastTickPosY, entity.posY, partialTicks) - renderManager.getRenderPosY();
        double z = MathUtil.lerp(entity.lastTickPosZ, entity.posZ, partialTicks) - renderManager.getRenderPosZ();
        drawCircle(x, y, z, radius, segments, lineWidth, color);
    }

    public static void drawFilledBox(AxisAlignedBB box, int red, int green, int blue, int alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
    }

    public static void drawBoundingBox(AxisAlignedBB box, int red, int green, int blue, int alpha, float lineWidth) {
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        worldRenderer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        worldRenderer.begin(1, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0f);
    }

    public static void drawEntityBox(Entity entity, int red, int green, int blue, int alpha, double expand) {
        float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        double x = MathUtil.lerp(entity.lastTickPosX, entity.posX, partialTicks);
        double y = MathUtil.lerp(entity.lastTickPosY, entity.posY, partialTicks);
        double z = MathUtil.lerp(entity.lastTickPosZ, entity.posZ, partialTicks);
        AxisAlignedBB box = entity.getEntityBoundingBox()
            .expand(expand, expand, expand)
            .offset(x - entity.posX, y - entity.posY, z - entity.posZ)
            .offset(-renderManager.getRenderPosX(), -renderManager.getRenderPosY(), -renderManager.getRenderPosZ());
        drawFilledBox(box, red, green, blue, alpha);
    }

    public static void drawEntityBoundingBox(Entity entity, int red, int green, int blue, int alpha, float lineWidth, double expand) {
        float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        double x = MathUtil.lerp(entity.lastTickPosX, entity.posX, partialTicks);
        double y = MathUtil.lerp(entity.lastTickPosY, entity.posY, partialTicks);
        double z = MathUtil.lerp(entity.lastTickPosZ, entity.posZ, partialTicks);
        AxisAlignedBB box = entity.getEntityBoundingBox()
            .expand(expand, expand, expand)
            .offset(x - entity.posX, y - entity.posY, z - entity.posZ)
            .offset(-renderManager.getRenderPosX(), -renderManager.getRenderPosY(), -renderManager.getRenderPosZ());
        drawBoundingBox(box, red, green, blue, alpha, lineWidth);
    }

    public static void drawBlockBox(BlockPos pos, double height, int red, int green, int blue, int alpha) {
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        AxisAlignedBB box = new AxisAlignedBB(
            pos.getX(), pos.getY(), pos.getZ(),
            (double) pos.getX() + 1.0, (double) pos.getY() + height, (double) pos.getZ() + 1.0
        ).offset(-renderManager.getRenderPosX(), -renderManager.getRenderPosY(), -renderManager.getRenderPosZ());
        drawFilledBox(box, red, green, blue, alpha);
    }

    public static void drawBlockBoundingBox(BlockPos pos, double height, int red, int green, int blue, int alpha, float lineWidth) {
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        AxisAlignedBB box = new AxisAlignedBB(
            pos.getX(), pos.getY(), pos.getZ(),
            (double) pos.getX() + 1.0, (double) pos.getY() + height, (double) pos.getZ() + 1.0
        ).offset(-renderManager.getRenderPosX(), -renderManager.getRenderPosY(), -renderManager.getRenderPosZ());
        drawBoundingBox(box, red, green, blue, alpha, lineWidth);
    }

    public static Vector4f projectToScreen(Entity entity, double expand) {
        float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        ScaledResolution resolution = new ScaledResolution(mc);
        float scaleFactor = resolution.getScaleFactor();
        double x = MathUtil.lerp(entity.lastTickPosX, entity.posX, partialTicks);
        double y = MathUtil.lerp(entity.lastTickPosY, entity.posY, partialTicks);
        double z = MathUtil.lerp(entity.lastTickPosZ, entity.posZ, partialTicks);
        AxisAlignedBB box = entity.getEntityBoundingBox()
            .expand(expand, expand, expand)
            .offset(x - entity.posX, y - entity.posY, z - entity.posZ)
            .offset(-renderManager.getRenderPosX(), -renderManager.getRenderPosY(), -renderManager.getRenderPosZ());
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        GL11.glGetFloat(2982, MODELVIEW);
        GL11.glGetFloat(2983, PROJECTION);
        GL11.glGetInteger(2978, VIEWPORT);
        Vector4f bounds = null;
        Vector3f[] corners = new Vector3f[]{
            new Vector3f(minX, minY, minZ),
            new Vector3f(minX, maxY, minZ),
            new Vector3f(maxX, minY, minZ),
            new Vector3f(maxX, maxY, minZ),
            new Vector3f(minX, minY, maxZ),
            new Vector3f(minX, maxY, maxZ),
            new Vector3f(maxX, minY, maxZ),
            new Vector3f(maxX, maxY, maxZ)
        };
        for (Vector3f corner : corners) {
            if (!GLU.gluProject(corner.x, corner.y, corner.z, MODELVIEW, PROJECTION, VIEWPORT, SCREEN_COORDS)) {
                continue;
            }
            Vector3f projected = new Vector3f(
                SCREEN_COORDS.get(0) / scaleFactor,
                (float) resolution.getScaledHeight() - SCREEN_COORDS.get(1) / scaleFactor,
                SCREEN_COORDS.get(2)
            );
            if (projected.z >= 0.0f && projected.z <= 1.0f) {
                if (bounds == null) {
                    bounds = new Vector4f(projected.x, projected.y, projected.z, 0.0f);
                }
                bounds.x = Math.min(projected.x, bounds.x);
                bounds.y = Math.min(projected.y, bounds.y);
                bounds.z = Math.max(projected.x, bounds.z);
                bounds.w = Math.max(projected.y, bounds.w);
            }
        }
        return bounds;
    }

    public static boolean isInViewFrustum(Entity entity, double expand) {
        Entity viewEntity = mc.getRenderViewEntity();
        if (viewEntity == null || viewEntity.getDistanceToEntity(entity) > 512.0f) {
            return false;
        }
        float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        double x = MathUtil.lerp(entity.lastTickPosX, entity.posX, partialTicks);
        double y = MathUtil.lerp(entity.lastTickPosY, entity.posY, partialTicks);
        double z = MathUtil.lerp(entity.lastTickPosZ, entity.posZ, partialTicks);
        AxisAlignedBB box = entity.getEntityBoundingBox()
            .expand(expand, expand, expand)
            .offset(x - entity.posX, y - entity.posY, z - entity.posZ);
        FRUSTUM.setPosition(viewEntity.posX, viewEntity.posY, viewEntity.posZ);
        return FRUSTUM.isBoundingBoxInFrustum(box);
    }

    public static void enableRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
    }

    public static void disableRenderState() {
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void setColor(int argb) {
        float alpha = (float) (argb >> 24 & 0xFF) / 255.0f;
        float red = (float) (argb >> 16 & 0xFF) / 255.0f;
        float green = (float) (argb >> 8 & 0xFF) / 255.0f;
        float blue = (float) (argb & 0xFF) / 255.0f;
        GlStateManager.color(red, green, blue, alpha);
    }
}
