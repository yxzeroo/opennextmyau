package myau.mixin;

import myau.Myau;
import myau.module.modules.Sprint;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer {
    @Redirect(
        method = "getFovModifier",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getAttributeValue()D")
    )
    private double redirectMovementSpeedAttribute(IAttributeInstance attribute) {
        double value = attribute.getAttributeValue();
        if (!((EntityPlayer) (Object) this instanceof EntityPlayerSP) || Myau.moduleManager == null) {
            return value;
        }
        Sprint sprint = (Sprint) Myau.moduleManager.modules.get(Sprint.class);
        if (sprint.isEnabled() && sprint.shouldApplyFov(attribute)) {
            return value * 1.300000011920929;
        }
        return value;
    }
}
