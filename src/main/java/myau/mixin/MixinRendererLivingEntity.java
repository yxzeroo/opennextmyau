package myau.mixin;

import myau.Myau;
import myau.module.modules.Chams;
import myau.module.modules.ESP;
import myau.module.modules.NameTags;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = RendererLivingEntity.class, priority = 991)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {
    @Unique
    private boolean chamsPolygonOffsetActive = false;

    protected MixinRendererLivingEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"))
    private void onPreDoRender(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (Myau.moduleManager == null) {
            return;
        }
        Chams chams = (Chams) Myau.moduleManager.modules.get(Chams.class);
        if (chams.isEnabled() && chams.shouldRenderChams(entity)) {
            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0f, -2500000.0f);
            this.chamsPolygonOffsetActive = true;
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("RETURN"))
    private void onPostDoRender(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (this.chamsPolygonOffsetActive) {
            GL11.glPolygonOffset(1.0f, 2500000.0f);
            GL11.glDisable(32823);
            this.chamsPolygonOffsetActive = false;
        }
    }

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void onCanRenderName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (Myau.moduleManager == null) {
            return;
        }
        NameTags nameTags = (NameTags) Myau.moduleManager.modules.get(NameTags.class);
        if (nameTags.isEnabled() && nameTags.shouldRenderTags(entity)) {
            cir.setReturnValue(false);
            return;
        }
        ESP esp = (ESP) Myau.moduleManager.modules.get(ESP.class);
        if (esp.isEnabled() && !esp.shouldRenderTags()) {
            cir.setReturnValue(false);
        }
    }
}
