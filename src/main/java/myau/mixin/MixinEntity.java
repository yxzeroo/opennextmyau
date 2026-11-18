package myau.mixin;

import myau.Myau;
import myau.event.events.KnockbackEvent;
import myau.event.events.SafeWalkEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public World worldObj;
    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public double motionX;
    @Shadow
    public double motionY;
    @Shadow
    public double motionZ;
    @Shadow
    public float rotationYaw;
    @Shadow
    public float rotationPitch;
    @Shadow
    public float prevRotationYaw;
    @Shadow
    public float prevRotationPitch;
    @Shadow
    public boolean onGround;

    @Shadow
    public boolean isRiding() {
        return false;
    }

    @Inject(method = "setVelocity", at = @At("HEAD"), cancellable = true)
    private void onSetVelocity(double x, double y, double z, CallbackInfo ci) {
        if ((Entity) (Object) this instanceof EntityPlayerSP && Myau.eventManager != null) {
            KnockbackEvent event = new KnockbackEvent(x, y, z);
            Myau.eventManager.call(event);
            if (event.isCancelled()) {
                ci.cancel();
                this.motionX = event.getX();
                this.motionY = event.getY();
                this.motionZ = event.getZ();
            }
        }
    }

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void onSetAngles(CallbackInfo ci) {
        if ((Entity) (Object) this instanceof EntityPlayerSP && Myau.rotationManager != null && Myau.rotationManager.isMouseLocked()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "moveEntity", ordinal = 0, at = @At("STORE"), name = "flag")
    private boolean modifySafeWalkFlag(boolean safeWalk) {
        if ((Entity) (Object) this instanceof EntityPlayerSP && Myau.eventManager != null) {
            SafeWalkEvent event = new SafeWalkEvent(safeWalk);
            Myau.eventManager.call(event);
            return event.isSafeWalk();
        }
        return safeWalk;
    }
}
