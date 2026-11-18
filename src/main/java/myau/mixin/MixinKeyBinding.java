package myau.mixin;

import myau.Myau;
import myau.event.events.SwapItemEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding {
    @Final
    @Shadow
    private String keyDescription;

    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void onIsPressed(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || Myau.eventManager == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        for (int slot = 0; slot < 9; ++slot) {
            if (mc.gameSettings.keyBindsHotbar[slot].getKeyDescription().equals(this.keyDescription)) {
                SwapItemEvent event = new SwapItemEvent(slot, 0);
                Myau.eventManager.call(event);
                if (event.isCancelled()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
