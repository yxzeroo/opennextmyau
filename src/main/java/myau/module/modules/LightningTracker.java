package myau.module.modules;

import myau.event.EventTarget;
import myau.event.events.ReceivePacketEvent;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;

public class LightningTracker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public LightningTracker() {
        super("LightningTracker", false, 0, true);
    }

    private String getDirection(double fromX, double fromZ, double toX, double toZ) {
        double threshold = Math.sqrt(2.0) - 1.0;
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        if (Math.abs(dx) > Math.abs(dz)) {
            if (Math.abs(dz / dx) <= threshold) {
                return dx > 0.0 ? "E" : "W";
            }
            if (dx > 0.0) {
                return dz > 0.0 ? "SE" : "NE";
            }
            return dz > 0.0 ? "SW" : "NW";
        }
        if (Math.abs(dz) > 0.0) {
            if (Math.abs(dx / dz) <= threshold) {
                return dz > 0.0 ? "S" : "N";
            }
            if (dz > 0.0) {
                return dx > 0.0 ? "SE" : "SW";
            }
            return dx > 0.0 ? "NE" : "NW";
        }
        return "?";
    }

    @EventTarget
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getPacket() instanceof S2CPacketSpawnGlobalEntity) {
            S2CPacketSpawnGlobalEntity packet = (S2CPacketSpawnGlobalEntity) event.getPacket();
            if (packet.func_149053_g() == 1) {
                double x = (double) packet.func_149051_d() / 32.0;
                double y = (double) packet.func_149050_e() / 32.0;
                double z = (double) packet.func_149049_f() / 32.0;
                double distance = mc.thePlayer.getDistance(x, y, z);
                String direction = this.getDirection(mc.thePlayer.posX, mc.thePlayer.posZ, x, z);
                ChatUtil.sendFormatted(String.format(
                        "&8[&e%s&8] &7X: &f&l%d&r &7Y: &f&l%d&r &7Z: &f&l%d&r &7D: &6&l%d&r &6%s&r",
                        this.getName(),
                        (int) x,
                        (int) y,
                        (int) z,
                        (int) distance,
                        direction
                ));
            }
        }
    }
}
