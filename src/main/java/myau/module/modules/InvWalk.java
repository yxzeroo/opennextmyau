package myau.module.modules;

import com.google.common.base.CaseFormat;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.SendPacketEvent;
import myau.event.events.TickEvent;
import myau.event.events.UpdateEvent;
import myau.mixin.IAccessorC0DPacketCloseWindow;
import myau.module.Module;
import myau.property.properties.ModeProperty;
import myau.util.KeyBindUtil;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;

public class InvWalk extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Queue<C0EPacketClickWindow> clickQueue = new ConcurrentLinkedQueue<>();
    private boolean keysPressed = false;
    private C16PacketClientStatus pendingOpenPacket = null;
    private int clickDelayTicks = 0;
    public final ModeProperty mode = new ModeProperty("mode", 1, new String[]{"VANILLA", "LEGIT", "HYPIXEL"});

    public InvWalk() {
        super("InvWalk", false, 0);
    }

    public void pressMovementKeys() {
        KeyBinding[] movementKeys = new KeyBinding[]{
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
        };
        for (KeyBinding keyBinding : movementKeys) {
            KeyBindUtil.updateKeyState(keyBinding.getKeyCode());
        }
        if (Myau.moduleManager.modules.get(Sprint.class).isEnabled()) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
        this.keysPressed = true;
    }

    public boolean canInvWalk() {
        if (!(mc.currentScreen instanceof GuiContainer)) {
            return false;
        }
        if (mc.currentScreen instanceof GuiContainerCreative) {
            return false;
        }
        switch (this.mode.getValue()) {
            case 1:
                return mc.currentScreen instanceof GuiInventory && this.pendingOpenPacket != null && this.clickQueue.isEmpty();
            case 2:
                return mc.currentScreen instanceof GuiInventory && this.clickQueue.isEmpty();
            default:
                return true;
        }
    }

    @EventTarget(priority = -100)
    public void onTick(TickEvent event) {
        while (!this.clickQueue.isEmpty()) {
            PacketUtil.sendPacketNoEvent(this.clickQueue.poll());
        }
    }

    @EventTarget(priority = -100)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.canInvWalk() && this.clickDelayTicks == 0) {
            this.pressMovementKeys();
        } else {
            if (this.keysPressed) {
                if (mc.currentScreen != null) {
                    KeyBinding.unPressAllKeys();
                }
                this.keysPressed = false;
            }
            if (this.pendingOpenPacket != null) {
                PacketUtil.sendPacketNoEvent(this.pendingOpenPacket);
                this.pendingOpenPacket = null;
            }
            if (this.clickDelayTicks > 0) {
                this.clickDelayTicks--;
            }
        }
    }

    @EventTarget
    public void onSendPacket(SendPacketEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getPacket() instanceof C16PacketClientStatus) {
            if (this.mode.getValue() == 1) {
                C16PacketClientStatus statusPacket = (C16PacketClientStatus) event.getPacket();
                if (statusPacket.getStatus() == EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                    event.setCancelled(true);
                    this.pendingOpenPacket = statusPacket;
                }
            }
        } else if (event.getPacket() instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow clickPacket = (C0EPacketClickWindow) event.getPacket();
            switch (this.mode.getValue()) {
                case 1:
                    if (clickPacket.getWindowId() == 0) {
                        if ((clickPacket.getMode() == 3 || clickPacket.getMode() == 4) && clickPacket.getSlotId() == -999) {
                            event.setCancelled(true);
                            return;
                        }
                        if (this.pendingOpenPacket != null) {
                            KeyBinding.unPressAllKeys();
                            event.setCancelled(true);
                            this.clickQueue.offer(clickPacket);
                        }
                    }
                    break;
                case 2:
                    if (clickPacket.getWindowId() == 0) {
                        if ((clickPacket.getMode() == 3 || clickPacket.getMode() == 4) && clickPacket.getSlotId() == -999) {
                            event.setCancelled(true);
                        } else {
                            KeyBinding.unPressAllKeys();
                            event.setCancelled(true);
                            this.clickQueue.offer(clickPacket);
                            this.clickDelayTicks = 8;
                        }
                    }
                    break;
            }
            if (this.pendingOpenPacket != null) {
                PacketUtil.sendPacketNoEvent(this.pendingOpenPacket);
                this.pendingOpenPacket = null;
            }
        } else if (event.getPacket() instanceof C0DPacketCloseWindow) {
            C0DPacketCloseWindow closePacket = (C0DPacketCloseWindow) event.getPacket();
            if (this.pendingOpenPacket != null && ((IAccessorC0DPacketCloseWindow) closePacket).getWindowId() == 0) {
                this.pendingOpenPacket = null;
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onDisabled() {
        if (this.keysPressed) {
            if (mc.currentScreen != null) {
                KeyBinding.unPressAllKeys();
            }
            this.keysPressed = false;
        }
        if (this.pendingOpenPacket != null) {
            PacketUtil.sendPacketNoEvent(this.pendingOpenPacket);
            this.pendingOpenPacket = null;
        }
        this.clickDelayTicks = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
