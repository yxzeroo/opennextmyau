package myau.module.modules;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.Render3DEvent;
import myau.mixin.IAccessorMinecraft;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.PercentProperty;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class ChestESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ColorProperty color = new ColorProperty("color", 0xFFAA00);
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final BooleanProperty tracers = new BooleanProperty("tracers", false);

    public ChestESP() {
        super("ChestESP", false, 0);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        RenderUtil.enableRenderState();
        List<TileEntity> chests = mc.theWorld.loadedTileEntityList.stream()
                .filter(tileEntity -> tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityEnderChest)
                .collect(Collectors.toList());
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        for (TileEntity tileEntity : chests) {
            AxisAlignedBB boundingBox = new AxisAlignedBB(
                    (double) tileEntity.getPos().getX() + 0.0625,
                    (double) tileEntity.getPos().getY() + 0.0,
                    (double) tileEntity.getPos().getZ() + 0.0625,
                    (double) tileEntity.getPos().getX() + 0.9375,
                    (double) tileEntity.getPos().getY() + 0.875,
                    (double) tileEntity.getPos().getZ() + 0.9375
            ).offset(
                    -renderManager.getRenderPosX(),
                    -renderManager.getRenderPosY(),
                    -renderManager.getRenderPosZ()
            );
            Color boxColor = new Color(this.color.getValue());
            int alpha = (int) ((float) this.opacity.getValue() / 100.0f * 255.0f);
            RenderUtil.drawBoundingBox(boundingBox, boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), alpha, 1.5f);
            if (this.tracers.getValue()) {
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
                Tracers tracersModule = (Tracers) Myau.moduleManager.modules.get(Tracers.class);
                float tracerAlpha = (float) tracersModule.opacity.getValue() / 100.0f;
                RenderUtil.drawTracerLine(
                        eyeVector,
                        (double) tileEntity.getPos().getX() + 0.5,
                        (double) tileEntity.getPos().getY() + 0.5,
                        (double) tileEntity.getPos().getZ() + 0.5,
                        (float) boxColor.getRed() / 255.0f,
                        (float) boxColor.getGreen() / 255.0f,
                        (float) boxColor.getBlue() / 255.0f,
                        tracerAlpha,
                        1.5f
                );
            }
        }
        RenderUtil.disableRenderState();
    }
}
