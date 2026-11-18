package myau.mixin;

import myau.management.RotationState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(RenderManager.class)
public abstract class MixinRenderManager {
    @Unique
    private float savedPrevRenderYawOffset;
    @Unique
    private float savedRenderYawOffset;
    @Unique
    private float savedPrevRotationYawHead;
    @Unique
    private float savedRotationYawHead;
    @Unique
    private float savedPrevRotationPitch;
    @Unique
    private float savedRotationPitch;

    @Inject(method = "renderEntityStatic", at = @At("HEAD"))
    private void onPreRenderEntityStatic(Entity entity, float partialTicks, boolean hideDebugBox, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EntityPlayerSP && RotationState.isRotated(1)) {
            EntityPlayerSP player = (EntityPlayerSP) entity;
            this.savedPrevRenderYawOffset = player.prevRenderYawOffset;
            this.savedRenderYawOffset = player.renderYawOffset;
            this.savedPrevRotationYawHead = player.prevRotationYawHead;
            this.savedRotationYawHead = player.rotationYawHead;
            this.savedPrevRotationPitch = player.prevRotationPitch;
            this.savedRotationPitch = player.rotationPitch;
            player.prevRenderYawOffset = RotationState.getPrevRenderYawOffset();
            player.renderYawOffset = RotationState.getRenderYawOffset();
            player.prevRotationYawHead = RotationState.getPrevRotationYawHead();
            player.rotationYawHead = RotationState.getRotationYawHead();
            player.prevRotationPitch = RotationState.getPrevRotationPitch();
            player.rotationPitch = RotationState.getRotationPitch();
        }
    }

    @Inject(method = "renderEntityStatic", at = @At("RETURN"))
    private void onPostRenderEntityStatic(Entity entity, float partialTicks, boolean hideDebugBox, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EntityPlayerSP && RotationState.isRotated(1)) {
            EntityPlayerSP player = (EntityPlayerSP) entity;
            player.prevRenderYawOffset = this.savedPrevRenderYawOffset;
            player.renderYawOffset = this.savedRenderYawOffset;
            player.prevRotationYawHead = this.savedPrevRotationYawHead;
            player.rotationYawHead = this.savedRotationYawHead;
            player.prevRotationPitch = this.savedPrevRotationPitch;
            player.rotationPitch = this.savedRotationPitch;
        }
    }
}
