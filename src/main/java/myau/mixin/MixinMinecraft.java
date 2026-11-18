package myau.mixin;

import myau.Initializer;
import myau.Myau;
import myau.event.events.HitBlockEvent;
import myau.event.events.KeyEvent;
import myau.event.events.LeftClickMouseEvent;
import myau.event.events.LoadWorldEvent;
import myau.event.events.PostTickEvent;
import myau.event.events.ResizeEvent;
import myau.event.events.RightClickMouseEvent;
import myau.event.events.SwapItemEvent;
import myau.event.events.TickEvent;
import myau.module.modules.NoHitDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow
    private int leftClickCounter;
    @Shadow
    public PlayerControllerMP playerController;
    @Shadow
    public WorldClient theWorld;
    @Shadow
    public EntityPlayerSP thePlayer;
    @Shadow
    public GuiScreen currentScreen;

    @Inject(method = "startGame", at = @At("HEAD"))
    private void onPreStartGame(CallbackInfo ci) {
        Initializer.preInit();
    }

    @Inject(method = "startGame", at = @At("RETURN"))
    private void onPostStartGame(CallbackInfo ci) {
        Myau.init();
    }

    @Inject(method = "runGameLoop", at = @At("RETURN"))
    private void onPostGameLoop(CallbackInfo ci) {
        if (Myau.moduleManager != null) {
            Myau.moduleManager.onRenderTick();
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onPreTick(CallbackInfo ci) {
        if (this.theWorld != null && this.thePlayer != null && Myau.eventManager != null) {
            Myau.eventManager.call(new TickEvent());
        }
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void onPostTick(CallbackInfo ci) {
        if (this.theWorld != null && this.thePlayer != null && Myau.eventManager != null) {
            Myau.eventManager.call(new PostTickEvent());
        }
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void onLoadWorld(WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
        if (Myau.eventManager != null) {
            Myau.eventManager.call(new LoadWorldEvent());
        }
    }

    @Inject(method = "updateFramebufferSize", at = @At("RETURN"))
    private void onResizeFramebuffer(CallbackInfo ci) {
        if (Myau.eventManager != null) {
            Myau.eventManager.call(new ResizeEvent());
        }
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void onClickMouse(CallbackInfo ci) {
        if (Myau.moduleManager != null && Myau.moduleManager.modules.get(NoHitDelay.class).isEnabled()) {
            this.leftClickCounter = 0;
        }
        if (Myau.eventManager != null) {
            LeftClickMouseEvent event = new LeftClickMouseEvent();
            Myau.eventManager.call(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void onRightClickMouse(CallbackInfo ci) {
        if (Myau.eventManager != null) {
            RightClickMouseEvent event = new RightClickMouseEvent();
            Myau.eventManager.call(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "sendClickBlockToController", at = @At("HEAD"), cancellable = true)
    private void onSendClickBlockToController(CallbackInfo ci) {
        if (Myau.eventManager != null) {
            HitBlockEvent event = new HitBlockEvent();
            Myau.eventManager.call(event);
            if (event.isCancelled()) {
                ci.cancel();
                this.playerController.resetBlockRemoving();
            }
        }
    }

    @Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V")
    )
    private void redirectSetKeyBindState(int keyCode, boolean pressed) {
        KeyBinding.setKeyBindState(keyCode, pressed);
        if (pressed && this.currentScreen == null && Myau.eventManager != null) {
            Myau.eventManager.call(new KeyEvent(keyCode));
        }
    }

    @Redirect(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V")
    )
    private void redirectChangeCurrentItem(InventoryPlayer inventory, int direction) {
        if (Myau.eventManager != null) {
            SwapItemEvent event = new SwapItemEvent(-1, direction);
            Myau.eventManager.call(event);
            if (!event.isCancelled()) {
                inventory.changeCurrentItem(direction);
            }
        }
    }
}
