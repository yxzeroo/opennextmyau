package myau.module.modules;

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.RenderUtil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class BedESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final CopyOnWriteArraySet<BlockPos> beds = new CopyOnWriteArraySet<>();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "FULL"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"CUSTOM", "HUD"});
    public final ColorProperty customColor = new ColorProperty("custom-color", 0xFF5555, () -> this.color.getValue() == 0);
    public final PercentProperty opacity = new PercentProperty("opacity", 25);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty obsidian = new BooleanProperty("obsidian", true);

    public BedESP() {
        super("BedESP", false, 0);
    }

    private Color getColor() {
        switch (this.color.getValue()) {
            case 0:
                return new Color(this.customColor.getValue());
            case 1:
                return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default:
                return new Color(-1);
        }
    }

    private void renderObsidianBox(AxisAlignedBB box) {
        if (this.outline.getValue()) {
            RenderUtil.drawBoundingBox(box, 170, 0, 170, 255, 1.5f);
        }
        RenderUtil.drawFilledBox(box, 170, 0, 170, (int) ((float) this.opacity.getValue() / 100.0f * 255.0f));
    }

    private void renderObsidianBlock(BlockPos pos) {
        if (this.outline.getValue()) {
            RenderUtil.drawBlockBoundingBox(pos, 1.0, 170, 0, 170, 255, 1.5f);
        }
        RenderUtil.drawBlockBox(pos, 1.0, 170, 0, 170, (int) ((float) this.opacity.getValue() / 100.0f * 255.0f));
    }

    public double getBoxHeight() {
        return this.mode.getValue() == 1 ? 1.0 : 0.5625;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        RenderUtil.enableRenderState();
        for (BlockPos headPos : this.beds) {
            IBlockState headState = mc.theWorld.getBlockState(headPos);
            if (!(headState.getBlock() instanceof BlockBed) || headState.getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD) {
                this.beds.remove(headPos);
                continue;
            }
            BlockPos footPos = headPos.offset(headState.getValue(BlockBed.FACING).getOpposite());
            IBlockState footState = mc.theWorld.getBlockState(footPos);
            if (!(footState.getBlock() instanceof BlockBed) || footState.getValue(BlockBed.PART) != BlockBed.EnumPartType.FOOT) {
                continue;
            }
            if (this.obsidian.getValue()) {
                for (EnumFacing facing : Arrays.asList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST)) {
                    BlockPos headNeighbor = headPos.offset(facing);
                    BlockPos footNeighbor = footPos.offset(facing);
                    boolean headObsidian = mc.theWorld.getBlockState(headNeighbor).getBlock() instanceof BlockObsidian;
                    boolean footObsidian = mc.theWorld.getBlockState(footNeighbor).getBlock() instanceof BlockObsidian;
                    if (headObsidian && footObsidian) {
                        AxisAlignedBB mergedBox = new AxisAlignedBB(
                                Math.min(headNeighbor.getX(), footNeighbor.getX()),
                                headNeighbor.getY(),
                                Math.min(headNeighbor.getZ(), footNeighbor.getZ()),
                                Math.max((double) headNeighbor.getX() + 1.0, (double) footNeighbor.getX() + 1.0),
                                (double) headNeighbor.getY() + 1.0,
                                Math.max((double) headNeighbor.getZ() + 1.0, (double) footNeighbor.getZ() + 1.0)
                        ).offset(-renderManager.getRenderPosX(), -renderManager.getRenderPosY(), -renderManager.getRenderPosZ());
                        this.renderObsidianBox(mergedBox);
                    } else if (headObsidian) {
                        this.renderObsidianBlock(headNeighbor);
                    } else if (footObsidian) {
                        this.renderObsidianBlock(footNeighbor);
                    }
                }
            }
            AxisAlignedBB bedBox = new AxisAlignedBB(
                    Math.min(headPos.getX(), footPos.getX()),
                    headPos.getY(),
                    Math.min(headPos.getZ(), footPos.getZ()),
                    Math.max((double) headPos.getX() + 1.0, (double) footPos.getX() + 1.0),
                    (double) headPos.getY() + this.getBoxHeight(),
                    Math.max((double) headPos.getZ() + 1.0, (double) footPos.getZ() + 1.0)
            ).offset(-renderManager.getRenderPosX(), -renderManager.getRenderPosY(), -renderManager.getRenderPosZ());
            Color bedColor = this.getColor();
            if (this.outline.getValue()) {
                RenderUtil.drawBoundingBox(bedBox, bedColor.getRed(), bedColor.getGreen(), bedColor.getBlue(), 255, 1.5f);
            }
            RenderUtil.drawFilledBox(bedBox, bedColor.getRed(), bedColor.getGreen(), bedColor.getBlue(), (int) ((float) this.opacity.getValue() / 100.0f * 255.0f));
        }
        RenderUtil.disableRenderState();
    }

    @Override
    public void onEnabled() {
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}
