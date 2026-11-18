package myau.mixin;

import myau.Myau;
import myau.event.events.Render2DEvent;
import myau.module.modules.NickHider;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiIngameForge.class)
public abstract class MixinGuiIngameForge {
    @Inject(
        method = "renderGameOverlay",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;renderTitle(IIF)V", shift = At.Shift.AFTER, remap = false)
    )
    private void onRenderGameOverlay(float partialTicks, CallbackInfo ci) {
        if (Myau.eventManager != null) {
            Myau.eventManager.call(new Render2DEvent(partialTicks));
        }
    }

    @Redirect(
        method = "renderExperience",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;experience:F")
    )
    private float redirectExperienceProgress(EntityPlayerSP player) {
        if (Myau.moduleManager == null) {
            return player.experience;
        }
        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        if (nickHider.isEnabled() && nickHider.level.getValue()) {
            return 0.0f;
        }
        return player.experience;
    }

    @Redirect(
        method = "renderExperience",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;experienceLevel:I")
    )
    private int redirectExperienceLevel(EntityPlayerSP player) {
        if (Myau.moduleManager == null) {
            return player.experienceLevel;
        }
        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        if (nickHider.isEnabled() && nickHider.level.getValue()) {
            return 0;
        }
        return player.experienceLevel;
    }
}
