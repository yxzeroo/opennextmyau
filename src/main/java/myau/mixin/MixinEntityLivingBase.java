package myau.mixin;

import myau.Myau;
import myau.event.events.StrafeEvent;
import myau.management.RotationState;
import myau.module.modules.Jesus;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity {
    @ModifyVariable(method = "jump", at = @At("STORE"), ordinal = 0)
    private float modifyJumpYawRadians(float yawRadians) {
        if ((Entity) (Object) this instanceof EntityPlayerSP && RotationState.isRotated()) {
            return RotationState.getMoveYaw() * ((float) Math.PI / 180.0f);
        }
        return yawRadians;
    }

    @Redirect(
        method = "moveEntityWithHeading",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveFlying(FFF)V")
    )
    private void redirectMoveFlying(EntityLivingBase entity, float strafe, float forward, float friction) {
        if ((Entity) (Object) this instanceof EntityPlayerSP && Myau.eventManager != null) {
            StrafeEvent event = new StrafeEvent(strafe, forward, friction);
            Myau.eventManager.call(event);
            strafe = event.getStrafe();
            forward = event.getForward();
            friction = event.getFriction();
            boolean rotated = RotationState.isRotated();
            float originalYaw = this.rotationYaw;
            if (rotated) {
                this.rotationYaw = RotationState.getMoveYaw();
            }
            entity.moveFlying(strafe, forward, friction);
            if (rotated) {
                this.rotationYaw = originalYaw;
            }
            return;
        }
        entity.moveFlying(strafe, forward, friction);
    }

    @ModifyVariable(method = "moveEntityWithHeading", name = "f3", at = @At("STORE"))
    private float modifyDepthStrider(float depthStrider) {
        if ((EntityLivingBase) (Object) this instanceof EntityPlayerSP
            && depthStrider == (float) EnchantmentHelper.getDepthStriderModifier((EntityLivingBase) (Object) this)) {
            if (Myau.moduleManager == null) {
                return depthStrider;
            }
            Jesus jesus = (Jesus) Myau.moduleManager.modules.get(Jesus.class);
            if (jesus.isEnabled() && (!jesus.groundOnly.getValue() || this.onGround)) {
                return Math.max(depthStrider, jesus.speed.getValue());
            }
        }
        return depthStrider;
    }
}
