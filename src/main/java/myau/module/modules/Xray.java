package myau.module.modules;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.CopyOnWriteArraySet;
import myau.event.EventTarget;
import myau.event.events.LoadWorldEvent;
import myau.event.events.ReceivePacketEvent;
import myau.event.events.Render3DEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.MathUtil;
import myau.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraftforge.common.ForgeModContainer;

public class Xray extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final LinkedHashSet<Integer> XRAY_BLOCK_IDS = new LinkedHashSet<>(
            Arrays.asList(56, 14, 15, 16, 73, 74, 21, 129, 52, 83, 115)
    );
    private static final LinkedHashSet<Vec3i> SMALL_CAVE_OFFSETS = new LinkedHashSet<>(Arrays.asList(
            new Vec3i(0, -1, 0),
            new Vec3i(1, 0, 0),
            new Vec3i(0, 0, -1),
            new Vec3i(0, 0, 1),
            new Vec3i(-1, 0, 0),
            new Vec3i(0, 1, 0)
    ));
    private static final LinkedHashSet<Vec3i> LARGE_CAVE_OFFSETS = new LinkedHashSet<>(Arrays.asList(
            new Vec3i(0, -2, 0),
            new Vec3i(1, -1, 0),
            new Vec3i(0, -1, -1),
            new Vec3i(0, -1, 0),
            new Vec3i(0, -1, 1),
            new Vec3i(-1, -1, 0),
            new Vec3i(2, 0, 0),
            new Vec3i(0, 0, 2),
            new Vec3i(0, 0, -2),
            new Vec3i(-2, 0, 0),
            new Vec3i(1, 0, -1),
            new Vec3i(1, 0, 0),
            new Vec3i(1, 0, 1),
            new Vec3i(0, 0, -1),
            new Vec3i(0, 0, 1),
            new Vec3i(-1, 0, -1),
            new Vec3i(-1, 0, 0),
            new Vec3i(-1, 0, 1),
            new Vec3i(1, 1, 0),
            new Vec3i(0, 1, -1),
            new Vec3i(0, 1, 0),
            new Vec3i(0, 1, 1),
            new Vec3i(-1, 1, 0),
            new Vec3i(0, 2, 0)
    ));

    public final CopyOnWriteArraySet<BlockPos> trackedBlocks = new CopyOnWriteArraySet<>();
    public final CopyOnWriteArraySet<BlockPos> pendingBlocks = new CopyOnWriteArraySet<>();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"SOFT", "FULL"});
    public final PercentProperty opacity = new PercentProperty("opacity", 50);
    public final IntProperty range = new IntProperty("range", 64, 16, 512);
    public final BooleanProperty cavesOnly = new BooleanProperty("caves-only", true);
    public final IntProperty cavesRadius = new IntProperty("caves-radius", 2, 1, 2);
    public final BooleanProperty diamonds = new BooleanProperty("diamonds", true);
    public final BooleanProperty diamondsTracers = new BooleanProperty("diamonds-tracers", true, this.diamonds::getValue);
    public final BooleanProperty gold = new BooleanProperty("gold", true);
    public final BooleanProperty goldTracers = new BooleanProperty("gold-tracers", true, this.gold::getValue);
    public final BooleanProperty iron = new BooleanProperty("iron", false);
    public final BooleanProperty ironTracers = new BooleanProperty("iron-tracers", false, this.iron::getValue);
    public final BooleanProperty coal = new BooleanProperty("coal", false);
    public final BooleanProperty coalTracers = new BooleanProperty("coal-tracers", false, this.coal::getValue);
    public final BooleanProperty redstone = new BooleanProperty("redstone", false);
    public final BooleanProperty redstoneTracers = new BooleanProperty("redstone-tracers", false, this.redstone::getValue);
    public final BooleanProperty lapis = new BooleanProperty("lapis", false);
    public final BooleanProperty lapisTracers = new BooleanProperty("lapis-tracers", false, this.lapis::getValue);
    public final BooleanProperty emeralds = new BooleanProperty("emeralds", false);
    public final BooleanProperty emeraldsTracers = new BooleanProperty("emeralds-tracers", false, this.emeralds::getValue);
    public final BooleanProperty spawners = new BooleanProperty("spawners", false);
    public final BooleanProperty spawnersTracers = new BooleanProperty("spawners-tracers", false, this.spawners::getValue);
    public final BooleanProperty canes = new BooleanProperty("canes", false);
    public final BooleanProperty canesTracers = new BooleanProperty("canes-tracers", false, this.canes::getValue);
    public final BooleanProperty warts = new BooleanProperty("warts", false);
    public final BooleanProperty wartsTracers = new BooleanProperty("warts-tracers", false, this.warts::getValue);

    public Xray() {
        super("Xray", false, 0);
    }

    private void renderTrackedBlock(BlockPos pos, int blockId, Vec3 eyeVector) {
        if (mc.thePlayer.getDistance(pos.getX(), pos.getY(), pos.getZ()) <= (double) this.range.getValue()) {
            Color blockColor = this.getBlockColor(blockId);
            RenderUtil.drawBlockBoundingBox(pos, 1.0, blockColor.getRed(), blockColor.getGreen(), blockColor.getBlue(), blockColor.getAlpha(), 1.5f);
            if (this.shouldDrawTracer(blockId)) {
                RenderUtil.drawTracerLine(
                        eyeVector,
                        (double) pos.getX() + 0.5,
                        (double) pos.getY() + 0.5,
                        (double) pos.getZ() + 0.5,
                        (float) blockColor.getRed() / 255.0f,
                        (float) blockColor.getGreen() / 255.0f,
                        (float) blockColor.getBlue() / 255.0f,
                        1.0f,
                        1.5f
                );
            }
        }
    }

    private Color getBlockColor(int blockId) {
        switch (blockId) {
            case 56:
                return new Color(0x55FFFF);
            case 14:
                return new Color(0xFFFF55);
            case 15:
                return new Color(0xFFFFFF);
            case 16:
                return new Color(0);
            case 73:
            case 74:
                return new Color(0xFF5555);
            case 21:
                return new Color(0x5555FF);
            case 129:
                return new Color(0x55FF55);
            case 52:
                return new Color(0xFF55FF);
            case 83:
                return new Color(0xAAFFAA);
            case 115:
                return new Color(0xAA0000);
            default:
                return new Color(-1);
        }
    }

    private boolean shouldDrawTracer(int blockId) {
        if (!this.isTargetBlockEnabled(blockId)) {
            return false;
        }
        switch (blockId) {
            case 56:
                return this.diamondsTracers.getValue();
            case 14:
                return this.goldTracers.getValue();
            case 15:
                return this.ironTracers.getValue();
            case 16:
                return this.coalTracers.getValue();
            case 73:
            case 74:
                return this.redstoneTracers.getValue();
            case 21:
                return this.lapisTracers.getValue();
            case 129:
                return this.emeraldsTracers.getValue();
            case 52:
                return this.spawnersTracers.getValue();
            case 83:
                return this.canesTracers.getValue();
            case 115:
                return this.wartsTracers.getValue();
            default:
                return false;
        }
    }

    private boolean isExposedBlock(BlockPos pos) {
        if (!mc.theWorld.isBlockLoaded(pos, false)) {
            return false;
        }
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block instanceof BlockMobSpawner) {
            return true;
        }
        return !block.isFullBlock() || !block.getMaterial().isOpaque() || block.canProvidePower();
    }

    public boolean isXrayBlock(int blockId) {
        return XRAY_BLOCK_IDS.contains(blockId);
    }

    public boolean isTargetBlockEnabled(int blockId) {
        switch (blockId) {
            case 56:
                return this.diamonds.getValue();
            case 14:
                return this.gold.getValue();
            case 15:
                return this.iron.getValue();
            case 16:
                return this.coal.getValue();
            case 73:
            case 74:
                return this.redstone.getValue();
            case 21:
                return this.lapis.getValue();
            case 129:
                return this.emeralds.getValue();
            case 52:
                return this.spawners.getValue();
            case 83:
                return this.canes.getValue();
            case 115:
                return this.warts.getValue();
            default:
                return false;
        }
    }

    public boolean canCaveRender(BlockPos pos) {
        if (!this.cavesOnly.getValue()) {
            return true;
        }
        LinkedHashSet<Vec3i> offsets = this.cavesRadius.getValue() >= 2 ? LARGE_CAVE_OFFSETS : SMALL_CAVE_OFFSETS;
        for (Vec3i offset : offsets) {
            if (this.isExposedBlock(pos.add(offset))) {
                return true;
            }
        }
        return false;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
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
        RenderUtil.enableRenderState();
        for (BlockPos pos : this.trackedBlocks) {
            if (this.pendingBlocks.contains(pos)) {
                this.trackedBlocks.remove(pos);
                continue;
            }
            int blockId = Block.getIdFromBlock(mc.theWorld.getBlockState(pos).getBlock());
            if (this.isTargetBlockEnabled(blockId)) {
                this.renderTrackedBlock(pos, blockId, eyeVector);
            } else {
                this.trackedBlocks.remove(pos);
            }
        }
        for (BlockPos pos : this.pendingBlocks) {
            int blockId = Block.getIdFromBlock(mc.theWorld.getBlockState(pos).getBlock());
            if (this.isTargetBlockEnabled(blockId)) {
                this.renderTrackedBlock(pos, blockId, eyeVector);
            } else {
                this.pendingBlocks.remove(pos);
            }
        }
        RenderUtil.disableRenderState();
    }

    @EventTarget
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange packet = (S22PacketMultiBlockChange) event.getPacket();
            for (S22PacketMultiBlockChange.BlockUpdateData updateData : packet.getChangedBlocks()) {
                if (this.isTargetBlockEnabled(Block.getIdFromBlock(updateData.getBlockState().getBlock()))) {
                    this.pendingBlocks.add(new BlockPos(updateData.getPos()));
                }
            }
        } else if (event.getPacket() instanceof S23PacketBlockChange) {
            S23PacketBlockChange packet = (S23PacketBlockChange) event.getPacket();
            if (this.isTargetBlockEnabled(Block.getIdFromBlock(packet.getBlockState().getBlock()))) {
                this.pendingBlocks.add(new BlockPos(packet.getBlockPosition()));
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.trackedBlocks.clear();
        this.pendingBlocks.clear();
    }

    @Override
    public void onEnabled() {
        ForgeModContainer.forgeLightPipelineEnabled = false;
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    @Override
    public void onDisabled() {
        ForgeModContainer.forgeLightPipelineEnabled = true;
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    @Override
    public void verifyValue(String propertyName) {
        this.trackedBlocks.clear();
        this.pendingBlocks.clear();
        if (this.isEnabled() && mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}
