package myau.mixin;

import myau.Myau;
import myau.event.events.LivingUpdateEvent;
import myau.event.events.MoveInputEvent;
import myau.event.events.PostUpdateEvent;
import myau.event.events.RightClickDelayEvent;
import myau.event.events.UpdateEvent;
import myau.management.RotationState;
import myau.module.modules.AntiDebuff;
import myau.module.modules.KillAura;
import myau.module.modules.NoSlow;
import myau.util.RotationUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends MixinEntityPlayer {
    @Unique
    private float overrideYaw = Float.NaN;
    @Unique
    private float overridePitch = Float.NaN;
    @Unique
    private float savedYaw;
    @Unique
    private float savedPitch;

    @Shadow
    private float lastReportedYaw;
    @Shadow
    private float lastReportedPitch;
    @Shadow
    public float renderArmYaw;
    @Shadow
    public float prevRenderArmYaw;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void onPreUpdate(CallbackInfo ci) {
        if (!this.worldObj.isBlockLoaded(new BlockPos(this.posX, 0.0, this.posZ))) {
            return;
        }
        if (Myau.eventManager == null) {
            return;
        }
        UpdateEvent event = new UpdateEvent(this.lastReportedYaw, this.lastReportedPitch, this.rotationYaw, this.rotationPitch);
        Myau.eventManager.call(event);
        RotationState.applyState(event.isRotated() && !this.isRiding(), event.getYaw(), event.getPitch(), event.getMoveYaw(), event.getPriority());
        if (event.isRotated()) {
            this.savedYaw = this.rotationYaw;
            this.savedPitch = this.rotationPitch;
            this.overrideYaw = event.getYaw();
            this.overridePitch = event.getPitch();
        } else {
            this.savedYaw = Float.NaN;
            this.savedPitch = Float.NaN;
            this.overrideYaw = Float.NaN;
            this.overridePitch = Float.NaN;
        }
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void onPostUpdate(CallbackInfo ci) {
        if (!this.worldObj.isBlockLoaded(new BlockPos(this.posX, 0.0, this.posZ))) {
            return;
        }
        if (!Float.isNaN(this.savedYaw) && !Float.isNaN(this.savedPitch)) {
            this.lastReportedYaw = this.rotationYaw;
            this.lastReportedPitch = this.rotationPitch;
            this.rotationYaw = RotationUtil.wrapAngleDiff(this.savedYaw, this.rotationYaw);
            this.rotationPitch = this.savedPitch;
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
            this.prevRenderArmYaw = this.rotationYaw - (this.renderArmYaw - this.prevRenderArmYaw) * 2.0f;
            this.renderArmYaw = this.rotationYaw;
        }
        if (Myau.eventManager != null) {
            Myau.eventManager.call(new PostUpdateEvent());
        }
    }

    @Redirect(
        method = "onUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isRiding()Z")
    )
    private boolean redirectIsRidingApplySilentRotation(EntityPlayerSP player) {
        if (!Float.isNaN(this.overrideYaw) && !Float.isNaN(this.overridePitch)) {
            this.rotationYaw = this.overrideYaw;
            this.rotationPitch = this.overridePitch;
        }
        return player.isRiding();
    }

    @Inject(
        method = "onUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V")
    )
    private void onBeforeUpdateWalkingPlayer(CallbackInfo ci) {
        if (Myau.eventManager != null) {
            Myau.eventManager.call(new LivingUpdateEvent());
        }
    }

    @Inject(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V")
    )
    private void onMoveInput(CallbackInfo ci) {
        if (Myau.eventManager != null) {
            Myau.eventManager.call(new MoveInputEvent());
        }
    }

    @Inject(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift = At.Shift.AFTER)
    )
    private void onAfterUpdateMoveState(CallbackInfo ci) {
        if (Myau.eventManager != null) {
            Myau.eventManager.call(new RightClickDelayEvent());
        }
    }

    @Redirect(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z")
    )
    private boolean redirectIsUsingItemSlowdown(EntityPlayerSP player) {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isBlocking() && killAura.autoBlockNoSlow.getValue()) {
            return false;
        }
        NoSlow noSlow = (NoSlow) Myau.moduleManager.modules.get(NoSlow.class);
        if (noSlow.isEnabled() && noSlow.isAnyActive()) {
            return false;
        }
        return player.isUsingItem();
    }

    @Redirect(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z")
    )
    private boolean redirectNauseaCheck(EntityPlayerSP player, Potion potion) {
        if (potion == Potion.confusion && Myau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) player).getActivePotionsMap().containsKey(potion.id);
    }
}
