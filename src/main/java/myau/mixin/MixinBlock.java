package myau.mixin;

import myau.Myau;
import myau.module.modules.Xray;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(Block.class)
public abstract class MixinBlock {
    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    private void onShouldSideBeRendered(
        IBlockAccess worldIn,
        BlockPos pos,
        EnumFacing side,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (Myau.moduleManager == null) {
            return;
        }
        Xray xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
        if (xray.isEnabled() && xray.mode.getValue() == 1 && xray.isXrayBlock(Block.getIdFromBlock((Block) (Object) this))) {
            BlockPos blockPos = new BlockPos(
                pos.getX() - side.getDirectionVec().getX(),
                pos.getY() - side.getDirectionVec().getY(),
                pos.getZ() - side.getDirectionVec().getZ()
            );
            if (xray.canCaveRender(blockPos)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    private void onGetBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> cir) {
        if (Myau.moduleManager == null) {
            return;
        }
        Xray xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
        if (xray.isEnabled()) {
            int blockId = Block.getIdFromBlock((Block) (Object) this);
            if (!xray.isXrayBlock(blockId) || (xray.mode.getValue() == 0 && !xray.isTargetBlockEnabled(blockId))) {
                cir.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
            }
        }
    }
}
