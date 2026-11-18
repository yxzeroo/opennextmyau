package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

public class Chams extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", true);
    public final BooleanProperty enemies = new BooleanProperty("enemies", true);
    public final BooleanProperty bosses = new BooleanProperty("bosses", false);
    public final BooleanProperty mobs = new BooleanProperty("mobs", false);
    public final BooleanProperty creepers = new BooleanProperty("creepers", false);
    public final BooleanProperty endermen = new BooleanProperty("endermen", false);
    public final BooleanProperty blazes = new BooleanProperty("blazes", false);
    public final BooleanProperty animals = new BooleanProperty("animals", false);
    public final BooleanProperty self = new BooleanProperty("self", false);
    public final BooleanProperty bots = new BooleanProperty("bots", false);

    public Chams() {
        super("Chams", false, 0);
    }

    public boolean shouldRenderChams(EntityLivingBase entity) {
        if (entity.deathTime > 0) {
            return false;
        }
        if (mc.getRenderViewEntity().getDistanceToEntity(entity) > 512.0f) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (TeamUtil.isSelf(player)) {
                return this.self.getValue() && mc.gameSettings.thirdPersonView != 0;
            }
            if (TeamUtil.isFriend(player)) {
                return this.friends.getValue();
            }
            if (TeamUtil.isEnemy(player)) {
                return this.enemies.getValue();
            }
            if (TeamUtil.isBot(player)) {
                return this.bots.getValue();
            }
            return this.players.getValue();
        }
        if (entity instanceof EntityDragon || entity instanceof EntityWither) {
            return !entity.isInvisible() && this.bosses.getValue();
        }
        if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            if (entity instanceof EntityCreeper) {
                return this.creepers.getValue();
            }
            if (entity instanceof EntityEnderman) {
                return this.endermen.getValue();
            }
            if (entity instanceof EntityBlaze) {
                return this.blazes.getValue();
            }
            return this.mobs.getValue();
        }
        if (entity instanceof EntityAnimal
                || entity instanceof EntityBat
                || entity instanceof EntitySquid
                || entity instanceof EntityVillager) {
            return this.animals.getValue();
        }
        return false;
    }
}
