package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.events.KeyEvent;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

public class MCF extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public MCF() {
        super("MCF", false, 0, true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (!this.isEnabled() || event.getKey() != -98) {
            return;
        }
        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
            String targetName = mc.objectMouseOver.entityHit.getName();
            if (!Myau.friendManager.contains(targetName)) {
                Myau.friendManager.add(targetName);
                ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your friend list&r", Myau.clientName, targetName));
            } else {
                Myau.friendManager.remove(targetName);
                ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your friend list&r", Myau.clientName, targetName));
            }
        }
    }
}
