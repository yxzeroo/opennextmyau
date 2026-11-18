package myau.mixin;

import myau.Myau;
import myau.module.modules.AntiObfuscate;
import myau.module.modules.NickHider;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {
    @ModifyVariable(method = "renderString", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String modifyRenderedString(String text) {
        if (Myau.moduleManager == null) {
            return text;
        }
        AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
        if (antiObfuscate.isEnabled()) {
            text = antiObfuscate.stripObfuscated(text);
        }
        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        return nickHider.isEnabled() ? nickHider.replaceNick(text) : text;
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String modifyMeasuredString(String text) {
        if (Myau.moduleManager == null) {
            return text;
        }
        AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
        if (antiObfuscate.isEnabled()) {
            text = antiObfuscate.stripObfuscated(text);
        }
        NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
        return nickHider.isEnabled() ? nickHider.replaceNick(text) : text;
    }

    @Redirect(
        method = "getStringWidth",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;charAt(I)C", ordinal = 1)
    )
    private char redirectColorCodeResetInWidth(String text, int index) {
        char code = text.charAt(index);
        if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || (code >= 'A' && code <= 'F')) {
            return 'r';
        }
        return code;
    }
}
