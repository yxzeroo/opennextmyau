package myau.mixin;

import myau.Myau;
import myau.event.events.CancelUseEvent;
import myau.event.events.WindowClickEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {
    @Inject(method = "windowClick", at = @At("HEAD"), cancellable = true)
    private void onWindowClick(
        int windowId,
        int slotId,
        int mouseButtonClicked,
        int mode,
        EntityPlayer playerIn,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        if (Myau.eventManager == null) {
            return;
        }
        WindowClickEvent event = new WindowClickEvent(windowId, slotId, mouseButtonClicked, mode);
        Myau.eventManager.call(event);
        if (event.isCancelled()) {
            cir.cancel();
        }
    }

    @Inject(method = "onStoppedUsingItem", at = @At("HEAD"), cancellable = true)
    private void onStoppedUsingItem(CallbackInfo ci) {
        if (Myau.eventManager == null) {
            return;
        }
        CancelUseEvent event = new CancelUseEvent();
        Myau.eventManager.call(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
