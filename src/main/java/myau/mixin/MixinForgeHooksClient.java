package myau.mixin;

import myau.Myau;
import myau.event.events.Render3DEvent;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class MixinForgeHooksClient {
    @Inject(method = "dispatchRenderLast", at = @At("RETURN"), remap = false)
    private static void onDispatchRenderLast(RenderGlobal context, float partialTicks, CallbackInfo ci) {
        if (Myau.eventManager != null) {
            Myau.eventManager.call(new Render3DEvent(partialTicks));
        }
    }
}
