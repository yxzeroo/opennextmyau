package myau.module.modules;

import com.google.common.base.Predicate;
import java.awt.Color;
import java.util.ArrayList;
import myau.event.EventTarget;
import myau.event.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import myau.util.RenderUtil;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemSnowball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class Trajectories extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final BooleanProperty bow = new BooleanProperty("bow", true);
    public final BooleanProperty projectiles = new BooleanProperty("projectiles", false);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);

    public Trajectories() {
        super("Trajectories", false, 0, true);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.thePlayer.getHeldItem() == null || mc.gameSettings.thirdPersonView != 0) {
            return;
        }
        Item heldItem = mc.thePlayer.getHeldItem().getItem();
        RenderManager renderManager = mc.getRenderManager();
        IAccessorRenderManager accessorRenderManager = (IAccessorRenderManager) renderManager;
        boolean isBow = false;
        float velocityMultiplier = 1.5f;
        float drag = 0.99f;
        float gravity;
        float hitboxExpand;
        if (heldItem instanceof ItemBow && this.bow.getValue()) {
            if (!mc.thePlayer.isUsingItem()) {
                return;
            }
            isBow = true;
            gravity = 0.05f;
            hitboxExpand = 0.3f;
            float charge = (float) mc.thePlayer.getItemInUseDuration() / 20.0f;
            charge = (charge * charge + charge * 2.0f) / 3.0f;
            if (charge < 0.1f) {
                return;
            }
            if (charge > 1.0f) {
                charge = 1.0f;
            }
            velocityMultiplier = charge * 3.0f;
        } else if (heldItem instanceof ItemFishingRod && this.projectiles.getValue()) {
            gravity = 0.04f;
            hitboxExpand = 0.25f;
            drag = 0.92f;
        } else if ((heldItem instanceof ItemSnowball || heldItem instanceof ItemEgg) && this.projectiles.getValue()) {
            gravity = 0.03f;
            hitboxExpand = 0.25f;
        } else if (heldItem instanceof ItemEnderPearl && this.pearls.getValue()) {
            gravity = 0.03f;
            hitboxExpand = 0.25f;
        } else {
            return;
        }

        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        double posX = accessorRenderManager.getRenderPosX() - (double) MathHelper.cos(yaw / 180.0f * (float) Math.PI) * 0.16;
        double posY = accessorRenderManager.getRenderPosY() + (double) mc.thePlayer.getEyeHeight() - (double) 0.1f;
        double posZ = accessorRenderManager.getRenderPosZ() - (double) MathHelper.sin(yaw / 180.0f * (float) Math.PI) * 0.16;
        double motionFactor = isBow ? 1.0 : 0.4;
        double motionX = (double) (MathHelper.sin(yaw / 180.0f * (float) Math.PI) * MathHelper.cos(pitch / 180.0f * (float) Math.PI)) * motionFactor * -1.0;
        double motionY = (double) MathHelper.sin(pitch / 180.0f * (float) Math.PI) * motionFactor * -1.0;
        double motionZ = (double) (MathHelper.cos(yaw / 180.0f * (float) Math.PI) * MathHelper.cos(pitch / 180.0f * (float) Math.PI)) * motionFactor;
        float distance = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= (double) distance;
        motionY /= (double) distance;
        motionZ /= (double) distance;
        motionX *= (double) velocityMultiplier;
        motionY *= (double) velocityMultiplier;
        motionZ *= (double) velocityMultiplier;

        MovingObjectPosition landingPosition = null;
        boolean hasLanded = false;
        boolean hitEntity = false;
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        ArrayList<Vec3> pathPoints = new ArrayList<>();

        while (!hasLanded && posY > 0.0) {
            Vec3 currentPos = new Vec3(posX, posY, posZ);
            Vec3 nextPos = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            landingPosition = mc.theWorld.rayTraceBlocks(currentPos, nextPos, false, true, false);
            currentPos = new Vec3(posX, posY, posZ);
            nextPos = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            if (landingPosition != null) {
                hasLanded = true;
                nextPos = new Vec3(landingPosition.hitVec.xCoord, landingPosition.hitVec.yCoord, landingPosition.hitVec.zCoord);
            }
            AxisAlignedBB searchBox = new AxisAlignedBB(
                    posX - (double) hitboxExpand,
                    posY - (double) hitboxExpand,
                    posZ - (double) hitboxExpand,
                    posX + (double) hitboxExpand,
                    posY + (double) hitboxExpand,
                    posZ + (double) hitboxExpand
            ).addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0);
            int minChunkX = MathHelper.floor_double((searchBox.minX - 2.0) / 16.0);
            int maxChunkX = MathHelper.floor_double((searchBox.maxX + 2.0) / 16.0);
            int minChunkZ = MathHelper.floor_double((searchBox.minZ - 2.0) / 16.0);
            int maxChunkZ = MathHelper.floor_double((searchBox.maxZ + 2.0) / 16.0);
            ArrayList<Entity> nearbyEntities = new ArrayList<>();
            for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
                    mc.theWorld.getChunkFromChunkCoords(chunkX, chunkZ)
                            .getEntitiesWithinAABBForEntity(mc.thePlayer, searchBox, nearbyEntities, (Predicate<? super Entity>) null);
                }
            }
            for (Entity entity : nearbyEntities) {
                if (entity.canBeCollidedWith() && entity != mc.thePlayer) {
                    AxisAlignedBB entityBox = entity.getEntityBoundingBox().expand(hitboxExpand, hitboxExpand, hitboxExpand);
                    MovingObjectPosition intercept = entityBox.calculateIntercept(currentPos, nextPos);
                    if (intercept != null) {
                        hitEntity = true;
                        hasLanded = true;
                        landingPosition = intercept;
                    }
                }
            }
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            if (mc.theWorld.getBlockState(new BlockPos(posX, posY, posZ)).getBlock().getMaterial() == Material.water) {
                motionX *= 0.6;
                motionY *= 0.6;
                motionZ *= 0.6;
            } else {
                motionX *= (double) drag;
                motionY *= (double) drag;
                motionZ *= (double) drag;
            }
            motionY -= (double) gravity;
            pathPoints.add(new Vec3(
                    posX - accessorRenderManager.getRenderPosX(),
                    posY - accessorRenderManager.getRenderPosY(),
                    posZ - accessorRenderManager.getRenderPosZ()
            ));
        }

        if (pathPoints.size() > 1) {
            RenderUtil.enableRenderState();
            int alpha = (int) ((float) this.opacity.getValue() / 100.0f * 255.0f);
            Color lineColor = hitEntity ? new Color(85, 255, 85, alpha) : new Color(255, 255, 255, alpha);
            RenderUtil.setColor(lineColor.getRGB());
            GL11.glLineWidth(1.5f);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
            pathPoints.forEach(point -> worldRenderer.pos(point.xCoord, point.yCoord, point.zCoord).endVertex());
            Tessellator.getInstance().draw();
            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    posX - accessorRenderManager.getRenderPosX(),
                    posY - accessorRenderManager.getRenderPosY(),
                    posZ - accessorRenderManager.getRenderPosZ()
            );
            if (landingPosition != null) {
                switch (landingPosition.sideHit.getAxis()) {
                    case X:
                        GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f);
                        break;
                    case Y:
                        GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f);
                        break;
                    default:
                        break;
                }
                RenderUtil.drawLine2D(-0.25f, -0.25f, 0.25f, 0.25f, 1.5f, lineColor.getRGB());
                RenderUtil.drawLine2D(-0.25f, 0.25f, 0.25f, -0.25f, 1.5f, lineColor.getRGB());
            }
            GlStateManager.popMatrix();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(2.0f);
            GlStateManager.resetColor();
            RenderUtil.disableRenderState();
        }
    }
}
