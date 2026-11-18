package myau.mixin;

import myau.Myau;
import myau.module.modules.KeepSprint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {
    @ModifyConstant(method = "attackTargetEntityWithCurrentItem", constant = @Constant(doubleValue = 0.6))
    private double modifyAttackSlowdown(double slowdown) {
        if (Myau.moduleManager == null) {
            return slowdown;
        }
        KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
        if (keepSprint.isEnabled() && keepSprint.shouldKeepSprint()) {
            return slowdown + (1.0 - slowdown) * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0);
        }
        return slowdown;
    }

    @Redirect(
        method = "attackTargetEntityWithCurrentItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V")
    )
    private void redirectAttackStopSprinting(EntityPlayer player, boolean sprinting) {
        if (Myau.moduleManager == null) {
            return;
        }
        KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
        if (keepSprint.isEnabled() && keepSprint.shouldKeepSprint()) {
            return;
        }
        player.setSprinting(sprinting);
    }
}
