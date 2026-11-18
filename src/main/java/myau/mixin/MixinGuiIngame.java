package myau.mixin;

import myau.Myau;
import myau.module.modules.Scaffold;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {
    @Redirect(
        method = "updateTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack redirectHighlightedCurrentItem(InventoryPlayer inventory) {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        int slot;
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue() && (slot = scaffold.getSpoofSlot()) >= 0) {
            return inventory.getStackInSlot(slot);
        }
        return inventory.getCurrentItem();
    }
}
