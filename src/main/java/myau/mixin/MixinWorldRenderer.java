package myau.mixin;

import java.nio.IntBuffer;
import myau.Myau;
import myau.module.modules.Xray;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Redirect(
        method = "putColorMultiplier",
        at = @At(value = "INVOKE", target = "java/nio/IntBuffer.put(II)Ljava/nio/IntBuffer;", remap = false)
    )
    private IntBuffer redirectPutColorMultiplier(IntBuffer buffer, int index, int color) {
        if (Myau.moduleManager == null) {
            return buffer.put(index, color);
        }
        Xray xray = (Xray) Myau.moduleManager.modules.get(Xray.class);
        if (xray.isEnabled()) {
            int alpha = (int) ((float) xray.opacity.getValue() * 255.0f / 100.0f) << 24;
            return buffer.put(index, (color & 0xFFFFFF) | alpha);
        }
        return buffer.put(index, color);
    }
}
