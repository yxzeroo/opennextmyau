package myau.mixin;

import myau.Myau;
import myau.module.modules.Chams;
import myau.module.modules.ViewClip;
import myau.module.modules.Xray;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(VisGraph.class)
public abstract class MixinVisGraph {
    @Inject(method = "func_178606_a", at = @At("HEAD"), cancellable = true)
    private void onMarkOpaque(CallbackInfo ci) {
        if (Myau.moduleManager == null) {
            return;
        }
        if (Myau.moduleManager.modules.get(Chams.class).isEnabled()
            || Myau.moduleManager.modules.get(ViewClip.class).isEnabled()
            || Myau.moduleManager.modules.get(Xray.class).isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "computeVisibility", at = @At("HEAD"), cancellable = true)
    private void onComputeVisibility(CallbackInfoReturnable<SetVisibility> cir) {
        if (Myau.moduleManager == null) {
            return;
        }
        if (Myau.moduleManager.modules.get(Chams.class).isEnabled()
            || Myau.moduleManager.modules.get(ViewClip.class).isEnabled()
            || Myau.moduleManager.modules.get(Xray.class).isEnabled()) {
            SetVisibility visibility = new SetVisibility();
            visibility.setAllVisible(true);
            cir.setReturnValue(visibility);
        }
    }
}
