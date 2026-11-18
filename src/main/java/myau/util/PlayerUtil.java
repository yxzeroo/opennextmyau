package myau.util;

import com.google.common.collect.Multimap;
import java.util.Collection;
import myau.Myau;
import myau.module.modules.KeepSprint;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ForgeHooks;

public class PlayerUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isJumping() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }

    public static boolean isSneaking() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
    }

    public static boolean isMovingLeft() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
    }

    public static boolean isMovingRight() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
    }

    public static boolean isAttacking() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    public static boolean isUsingItem() {
        return mc.currentScreen == null && KeyBindUtil.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode());
    }

    public static boolean canFall(float fallThreshold) {
        if (mc.thePlayer.capabilities.allowFlying || mc.thePlayer.capabilities.disableDamage) {
            return false;
        }
        PotionEffect jumpEffect = mc.thePlayer.getActivePotionEffect(Potion.jump);
        float jumpBonus = jumpEffect != null ? (float) (jumpEffect.getAmplifier() + 1) : 0.0f;
        float fallDistance = mc.thePlayer.fallDistance;
        if (mc.thePlayer.motionY < -0.67 || !isBlockUnder()) {
            fallDistance -= (float) mc.thePlayer.motionY;
        }
        return MathHelper.ceiling_float_int(fallDistance - fallThreshold - jumpBonus) > 0;
    }

    public static boolean isAirBelow(int maxDepth) {
        if (mc.thePlayer.capabilities.allowFlying || mc.thePlayer.capabilities.disableDamage) {
            return false;
        }
        int baseY = MathHelper.floor_double(mc.thePlayer.posY);
        for (int offset = 0; offset <= maxDepth; ++offset) {
            int checkY = baseY - offset;
            if (checkY < 0) {
                break;
            }
            Block block = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, checkY, mc.thePlayer.posZ)).getBlock();
            if (!(block instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOverVoid() {
        return isOverVoid(mc.thePlayer.getEntityBoundingBox().expand(-1.0E-6, 0.0, -1.0E-6));
    }

    public static boolean isOverVoid(AxisAlignedBB boundingBox) {
        if (mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) {
            return false;
        }
        int startY = MathHelper.floor_double(boundingBox.minY);
        if (startY < 0) {
            return true;
        }
        int minX = MathHelper.floor_double(boundingBox.minX);
        int maxX = MathHelper.floor_double(boundingBox.maxX + 1.0);
        int minZ = MathHelper.floor_double(boundingBox.minZ);
        int maxZ = MathHelper.floor_double(boundingBox.maxZ + 1.0);
        for (int x = minX; x < maxX; ++x) {
            for (int z = minZ; z < maxZ; ++z) {
                for (int y = startY; y >= 0; --y) {
                    if (!BlockUtil.isReplaceable(new BlockPos(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean canMove(double offsetX, double offsetY, double offsetZ) {
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().offset(offsetX, offsetY, offsetZ);
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, box).isEmpty();
    }

    public static boolean canMove(double offsetX, double offsetY, double offsetZ, float widthScale) {
        double expand = mc.thePlayer.width / -2.0f * (1.0f - widthScale);
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().offset(offsetX, offsetY, offsetZ).expand(expand, 0.0, expand);
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, box).isEmpty();
    }

    public static boolean isBlockUnder() {
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().offset(0.0, -1.0, 0.0);
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, box).isEmpty();
    }

    public static boolean isBlockAbove() {
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().offset(0.0, 1.0, 0.0);
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, box).isEmpty();
    }

    public static boolean canReach(BlockPos pos, double range) {
        return canReach(pos, mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ, range);
    }

    public static boolean canReach(BlockPos pos, double x, double y, double z, double range) {
        return pos.distanceSqToCenter(x, y, z) < Math.pow(range, 2.0);
    }

    public static void attackEntity(Entity target) {
        if (!ForgeHooks.onPlayerAttackTarget(mc.thePlayer, target)) {
            return;
        }
        if (!target.canAttackWithItem() || target.hitByEntity(mc.thePlayer)) {
            return;
        }
        float baseDamage = (float) mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
        float enchantDamage = EnchantmentHelper.getModifierForCreature(
            mc.thePlayer.getHeldItem(),
            target instanceof EntityLivingBase ? ((EntityLivingBase) target).getCreatureAttribute() : EnumCreatureAttribute.UNDEFINED
        );
        int knockbackLevel = EnchantmentHelper.getKnockbackModifier(mc.thePlayer);
        if (mc.thePlayer.isSprinting()) {
            ++knockbackLevel;
        }
        if (baseDamage > 0.0f || enchantDamage > 0.0f) {
            boolean critical = mc.thePlayer.fallDistance > 0.0f
                && !mc.thePlayer.onGround
                && !mc.thePlayer.isOnLadder()
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isPotionActive(Potion.blindness)
                && mc.thePlayer.ridingEntity == null;
            if (critical && baseDamage > 0.0f) {
                baseDamage *= 1.5f;
            }
            baseDamage += enchantDamage;
            boolean appliedTempFire = false;
            int fireAspect = EnchantmentHelper.getFireAspectModifier(mc.thePlayer);
            if (target instanceof EntityLivingBase && fireAspect > 0 && !target.isBurning()) {
                appliedTempFire = true;
                target.setFire(1);
            }
            double prevMotionX = target.motionX;
            double prevMotionY = target.motionY;
            double prevMotionZ = target.motionZ;
            if (target.attackEntityFrom(DamageSource.causePlayerDamage(mc.thePlayer), baseDamage)) {
                if (knockbackLevel > 0) {
                    target.addVelocity(
                        -MathHelper.sin(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f) * (float) knockbackLevel * 0.5f,
                        0.1,
                        MathHelper.cos(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f) * (float) knockbackLevel * 0.5f
                    );
                    KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
                    if (keepSprint.isEnabled()
                        && (!keepSprint.groundOnly.getValue() || mc.thePlayer.onGround)
                        && (!keepSprint.reachOnly.getValue() || !(RotationUtil.distanceToEntity(target) <= 3.0))) {
                        double multiplier = 0.6 + 0.4 * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0);
                        mc.thePlayer.motionX *= multiplier;
                        mc.thePlayer.motionZ *= multiplier;
                    } else {
                        mc.thePlayer.motionX *= 0.6;
                        mc.thePlayer.motionZ *= 0.6;
                        mc.thePlayer.setSprinting(false);
                    }
                }
                if (target instanceof EntityPlayerMP && target.velocityChanged) {
                    ((EntityPlayerMP) target).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(target));
                    target.velocityChanged = false;
                    target.motionX = prevMotionX;
                    target.motionY = prevMotionY;
                    target.motionZ = prevMotionZ;
                }
                if (critical) {
                    mc.thePlayer.onCriticalHit(target);
                }
                if (enchantDamage > 0.0f) {
                    mc.thePlayer.onEnchantmentCritical(target);
                }
                if (baseDamage >= 18.0f) {
                    mc.thePlayer.triggerAchievement(AchievementList.overkill);
                }
                mc.thePlayer.setLastAttacker(target);
                if (target instanceof EntityLivingBase) {
                    EnchantmentHelper.applyThornEnchantments((EntityLivingBase) target, mc.thePlayer);
                }
                EnchantmentHelper.applyArthropodEnchantments(mc.thePlayer, target);
                if (target instanceof EntityLivingBase) {
                    mc.thePlayer.addStat(StatList.damageDealtStat, Math.round(baseDamage * 10.0f));
                    if (fireAspect > 0) {
                        target.setFire(fireAspect * 4);
                    }
                }
                mc.thePlayer.addExhaustion(0.3f);
            } else if (appliedTempFire) {
                target.extinguish();
            }
        }
    }

    public static float getAttackDamage(Entity target) {
        ItemStack stack = mc.thePlayer.getHeldItem();
        float damage = 1.0f;
        if (stack != null) {
            Multimap<String, AttributeModifier> modifiers = stack.getAttributeModifiers();
            Collection<AttributeModifier> attackModifiers = modifiers.get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName());
            for (AttributeModifier modifier : attackModifiers) {
                damage += (float) modifier.getAmount();
            }
        }
        float enchantDamage = EnchantmentHelper.getModifierForCreature(
            stack,
            target instanceof EntityLivingBase ? ((EntityLivingBase) target).getCreatureAttribute() : EnumCreatureAttribute.UNDEFINED
        );
        if (damage <= 0.0f && enchantDamage <= 0.0f) {
            return 0.0f;
        }
        boolean critical = mc.thePlayer.fallDistance > 0.0f
            && !mc.thePlayer.onGround
            && !mc.thePlayer.isOnLadder()
            && !mc.thePlayer.isInWater()
            && !mc.thePlayer.isPotionActive(Potion.blindness)
            && mc.thePlayer.ridingEntity == null;
        if (critical && damage > 0.0f) {
            damage *= 1.5f;
        }
        return damage + enchantDamage;
    }

    public static int getArmSwingAnimationEnd() {
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            return 6 - (1 + mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier());
        }
        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            return 6 + (1 + mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2;
        }
        return 6;
    }

    public static void swingItem() {
        if (!mc.thePlayer.isSwingInProgress || mc.thePlayer.swingProgressInt >= getArmSwingAnimationEnd() / 2 || mc.thePlayer.swingProgressInt < 0) {
            mc.thePlayer.swingProgressInt = -1;
            mc.thePlayer.isSwingInProgress = true;
        }
    }
}
