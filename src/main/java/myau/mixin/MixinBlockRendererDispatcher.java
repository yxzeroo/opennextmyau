package myau.mixin;

import myau.Myau;
import myau.module.modules.BedESP;
import myau.module.modules.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher {
    @Inject(method = "renderBlock", at = @At("HEAD"))
    private void onRenderBlock(
        IBlockState state,
        BlockPos pos,
        IBlockAccess blockAccess,
        WorldRenderer worldRendererIn,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (Myau.moduleManager == null) {
            return;
        }
        BedESP bedESP = (BedESP) Myau.moduleManager.modules.get(BedESP.class);
        if (bedESP.isEnabled() && state.getBlock() instanceof BlockBed && state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
            bedESP.beds.add(new BlockPos(pos));
        }
        Xray xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
        if (xray.isEnabled() && xray.isTargetBlockEnabled(Block.getIdFromBlock(state.getBlock()))) {
            if (xray.canCaveRender(pos)) {
                xray.trackedBlocks.add(new BlockPos(pos));
            } else {
                xray.trackedBlocks.remove(pos);
            }
        }
    }
}
