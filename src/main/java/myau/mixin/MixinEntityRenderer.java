package myau.mixin;

import java.util.List;
import myau.Myau;
import myau.data.Box;
import myau.event.events.PickEvent;
import myau.event.events.RaytraceEvent;
import myau.module.modules.AntiDebuff;
import myau.module.modules.GhostHand;
import myau.module.modules.KillAura;
import myau.module.modules.NoHurtCam;
import myau.module.modules.Scaffold;
import myau.module.modules.ViewClip;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@SideOnly(Side.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Unique
    private Box<Integer> spoofedSlot = null;
    @Unique
    private Box<ItemStack> savedItemInUse = null;
    @Unique
    private Box<Integer> savedItemInUseCount = null;

    @Shadow
    private Minecraft mc;
    @Shadow
    private float thirdPersonDistance;

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void onPreUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (this.mc.thePlayer == null) {
            return;
        }
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        int slot;
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue() && (slot = scaffold.getSpoofSlot()) >= 0) {
            this.spoofedSlot = new Box<>(this.mc.thePlayer.inventory.currentItem);
            this.mc.thePlayer.inventory.currentItem = slot;
        }
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.shouldFakeBlock()) {
            IAccessorEntityPlayer playerAccessor = (IAccessorEntityPlayer) this.mc.thePlayer;
            this.savedItemInUse = new Box<>(playerAccessor.getItemInUse());
            playerAccessor.setItemInUse(this.mc.thePlayer.inventory.getCurrentItem());
            this.savedItemInUseCount = new Box<>(playerAccessor.getItemInUseCount());
            playerAccessor.setItemInUseCount(69000);
        }
    }

    @Inject(method = "updateCameraAndRender", at = @At("RETURN"))
    private void onPostUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (this.spoofedSlot != null) {
            this.mc.thePlayer.inventory.currentItem = this.spoofedSlot.value;
            this.spoofedSlot = null;
        }
        if (this.savedItemInUse != null) {
            ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUse(this.savedItemInUse.value);
            this.savedItemInUse = null;
        }
        if (this.savedItemInUseCount != null) {
            ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUseCount(this.savedItemInUseCount.value);
            this.savedItemInUseCount = null;
        }
    }

    @Inject(method = "updateRenderer", at = @At("HEAD"))
    private void onPreUpdateRenderer(CallbackInfo ci) {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        int slot;
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue() && (slot = scaffold.getSpoofSlot()) >= 0) {
            this.spoofedSlot = new Box<>(this.mc.thePlayer.inventory.currentItem);
            this.mc.thePlayer.inventory.currentItem = slot;
        }
    }

    @Inject(method = "updateRenderer", at = @At("RETURN"))
    private void onPostUpdateRenderer(CallbackInfo ci) {
        if (this.spoofedSlot != null) {
            this.mc.thePlayer.inventory.currentItem = this.spoofedSlot.value;
            this.spoofedSlot = null;
        }
    }

    @ModifyConstant(method = "hurtCameraEffect", constant = @Constant(floatValue = 14.0f, ordinal = 0))
    private float modifyHurtCameraIntensity(float intensity) {
        if (Myau.moduleManager == null) {
            return intensity;
        }
        NoHurtCam noHurtCam = (NoHurtCam) Myau.moduleManager.modules.get(NoHurtCam.class);
        if (noHurtCam.isEnabled()) {
            return intensity * (float) noHurtCam.multiplier.getValue() / 100.0f;
        }
        return intensity;
    }

    @ModifyConstant(method = "getMouseOver", constant = @Constant(doubleValue = 3.0, ordinal = 1))
    private double modifyPickRange(double range) {
        if (Myau.eventManager != null) {
            PickEvent event = new PickEvent(range);
            Myau.eventManager.call(event);
            return event.getRange();
        }
        return range;
    }

    @ModifyVariable(method = "getMouseOver", at = @At("STORE"), name = "d0")
    private double modifyRaytraceRange(double range) {
        if (Myau.eventManager != null) {
            RaytraceEvent event = new RaytraceEvent(range);
            Myau.eventManager.call(event);
            return event.getRange();
        }
        return range;
    }

    @Inject(
        method = "getMouseOver",
        at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0),
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void filterMouseOverEntities(
        float partialTicks,
        CallbackInfo ci,
        Entity renderViewEntity,
        double blockReach,
        double hitDistance,
        Vec3 eyePos,
        boolean extendedCheck,
        int unusedInt,
        Vec3 lookVec,
        Vec3 reachVec,
        Vec3 hitVec,
        float expandBorder,
        List<Entity> candidateEntities,
        double closestDistance,
        int entityIndex
    ) {
        if (Myau.moduleManager == null) {
            return;
        }
        GhostHand ghostHand = (GhostHand) Myau.moduleManager.modules.get(GhostHand.class);
        if (ghostHand.isEnabled()) {
            candidateEntities.removeIf(ghostHand::shouldSkipEntity);
        }
    }

    @Redirect(
        method = "orientCamera",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D")
    )
    private double redirectCameraClipDistance(Vec3 from, Vec3 to) {
        if (Myau.moduleManager == null) {
            return from.distanceTo(to);
        }
        ViewClip viewClip = (ViewClip) Myau.moduleManager.modules.get(ViewClip.class);
        return viewClip.isEnabled() ? (double) this.thirdPersonDistance : from.distanceTo(to);
    }

    @Redirect(
        method = "setupFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;")
    )
    private Material redirectFogMaterial(Block block) {
        if (Myau.moduleManager == null) {
            return block.getMaterial();
        }
        ViewClip viewClip = (ViewClip) Myau.moduleManager.modules.get(ViewClip.class);
        return viewClip.isEnabled() ? Material.air : block.getMaterial();
    }

    @Redirect(
        method = "updateFogColor",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z")
    )
    private boolean redirectFogColorBlindness(EntityLivingBase entity, Potion potion) {
        if (potion == Potion.blindness && Myau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entity).getActivePotionsMap().containsKey(potion.id);
    }

    @Redirect(
        method = "setupFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z")
    )
    private boolean redirectSetupFogBlindness(EntityLivingBase entity, Potion potion) {
        if (potion == Potion.blindness && Myau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entity).getActivePotionsMap().containsKey(potion.id);
    }

    @Redirect(
        method = "setupCameraTransform",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z")
    )
    private boolean redirectCameraTransformNausea(EntityPlayerSP player, Potion potion) {
        if (potion == Potion.confusion && Myau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) player).getActivePotionsMap().containsKey(potion.id);
    }
}
