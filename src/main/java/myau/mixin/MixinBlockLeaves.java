package myau.mixin;

import myau.Myau;
import myau.module.modules.Xray;
import net.minecraft.block.BlockLeaves;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(BlockLeaves.class)
public abstract class MixinBlockLeaves {
    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    private void onGetBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> cir) {
        if (Myau.moduleManager == null) {
            return;
        }
        Xray xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
        if (xray.isEnabled()) {
            cir.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
        }
    }
}
