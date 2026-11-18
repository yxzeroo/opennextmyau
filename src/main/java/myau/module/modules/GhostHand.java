package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.ItemUtil;
import myau.util.TeamUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class GhostHand extends Module {
    public final BooleanProperty teamOnly = new BooleanProperty("team-only", true);
    public final BooleanProperty ignoreWeapons = new BooleanProperty("ignore-weapons", false);

    public GhostHand() {
        super("GhostHand", false, 0);
    }

    public boolean shouldSkipEntity(Entity entity) {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) entity;
        if (TeamUtil.isBot(player)) {
            return false;
        }
        if (this.teamOnly.getValue() && !TeamUtil.isSameTeam(player)) {
            return false;
        }
        return !this.ignoreWeapons.getValue() || !ItemUtil.isHoldingWeapon();
    }
}
