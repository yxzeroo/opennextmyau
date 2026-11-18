package myau.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import myau.Myau;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class TeamUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isEntityLoaded(Entity entity) {
        return entity != null && mc.theWorld.loadedEntityList.contains(entity);
    }

    public static List<Entity> getLoadedEntitiesSorted() {
        RenderManager renderManager = mc.getRenderManager();
        ArrayList<Entity> entities = new ArrayList<>(mc.theWorld.loadedEntityList);
        entities.sort((a, b) -> Double.compare(
            renderManager.getDistanceToCamera(b.posX, b.posY, b.posZ),
            renderManager.getDistanceToCamera(a.posX, a.posY, a.posZ)
        ));
        return entities;
    }

    public static float getEffectiveHealth(EntityLivingBase entity) {
        return entity.getHealth() * (20.0f / (float) entity.getTotalArmorValue());
    }

    public static String stripLastColorCode(Entity entity) {
        return entity.getDisplayName().getFormattedText()
            .replaceAll("\u00a7\\S$", "")
            .replaceAll("(?i)\u00a7r", "\u00a7f")
            .trim();
    }

    public static Color getTeamColor(EntityPlayer player, float alpha) {
        int rgb = 0xFFFFFF;
        ScorePlayerTeam team = (ScorePlayerTeam) player.getTeam();
        if (team != null) {
            String format = FontRenderer.getFormatFromString(team.getColorPrefix());
            if (format.length() >= 2) {
                rgb = mc.fontRendererObj.getColorCode(format.charAt(1));
            }
        }
        return new Color(
            (float) (rgb >> 16 & 0xFF) / 255.0f,
            (float) (rgb >> 8 & 0xFF) / 255.0f,
            (float) (rgb & 0xFF) / 255.0f,
            alpha
        );
    }

    public static boolean isSelf(EntityPlayer player) {
        return player == mc.thePlayer || player == mc.getRenderViewEntity();
    }

    public static boolean isBot(EntityPlayer player) {
        if (player == mc.thePlayer) {
            return false;
        }
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getName());
        if (info == null) {
            return true;
        }
        if (ServerUtil.isHypixel()) {
            if (player.getName().startsWith("\u00a7k")) {
                return player.isInvisible();
            }
            if (info.getResponseTime() < 1) {
                return true;
            }
            ScorePlayerTeam team = info.getPlayerTeam();
            if (team != null) {
                if (team.getTeamName().isEmpty() && team.getColorPrefix().equals("\u00a7c")) {
                    return true;
                }
                if (!team.getColorPrefix().isEmpty()) {
                    return false;
                }
            }
            return info.getGameProfile().getId().version() == 2;
        }
        return false;
    }

    public static boolean isSameTeam(EntityPlayer player) {
        if (player == mc.thePlayer) {
            return true;
        }
        NetworkPlayerInfo selfInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (selfInfo == null || selfInfo.getPlayerTeam() == null) {
            return false;
        }
        NetworkPlayerInfo otherInfo = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (otherInfo == null || otherInfo.getPlayerTeam() == null) {
            return false;
        }
        return selfInfo.getPlayerTeam().getColorPrefix().equals(otherInfo.getPlayerTeam().getColorPrefix());
    }

    public static boolean hasTeamArmorStand(EntityLivingBase entity) {
        if (entity == mc.thePlayer) {
            return true;
        }
        NetworkPlayerInfo selfInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (selfInfo == null || selfInfo.getPlayerTeam() == null || selfInfo.getPlayerTeam().getColorPrefix().length() < 2) {
            return false;
        }
        EntityLivingBase stand = mc.theWorld.findNearestEntityWithinAABB(EntityArmorStand.class, entity.getEntityBoundingBox(), entity);
        return stand != null && stand.getName().contains(selfInfo.getPlayerTeam().getColorPrefix().substring(0, 2));
    }

    public static boolean isShopNPC(EntityLivingBase entity) {
        EntityLivingBase stand = mc.theWorld.findNearestEntityWithinAABB(EntityArmorStand.class, entity.getEntityBoundingBox(), entity);
        if (stand == null) {
            return false;
        }
        String name = stand.getName();
        return name.contains("RIGHT CLICK")
            || name.contains("ITEM SHOP")
            || name.contains("UPGRADES")
            || name.contains("BANKER")
            || name.contains("STREAK POWERS");
    }

    public static boolean isFriend(EntityPlayer player) {
        return Myau.friendManager.contains(player.getName());
    }

    public static boolean isEnemy(EntityPlayer player) {
        return Myau.enemyManager.contains(player.getName());
    }
}
