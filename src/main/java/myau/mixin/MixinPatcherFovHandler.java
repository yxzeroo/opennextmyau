package myau.mixin;

import myau.Myau;
import myau.module.modules.Sprint;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Pseudo
@Mixin(targets = "club.sk1er.patcher.util.fov.FovHandler")
public abstract class MixinPatcherFovHandler {
    @Redirect(
        method = "fovChange",
        remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;func_70051_ag()Z", remap = false)
    )
    @Dynamic("Patcher")
    private boolean redirectIsSprinting(EntityPlayer player) {
        boolean sprinting = player.isSprinting();
        if (!(player instanceof EntityPlayerSP) || Myau.moduleManager == null) {
            return sprinting;
        }
        Sprint sprint = (Sprint) Myau.moduleManager.modules.get(Sprint.class);
        if (sprint.isEnabled() && sprint.shouldForceFov(sprinting)) {
            return true;
        }
        return sprinting;
    }
}
