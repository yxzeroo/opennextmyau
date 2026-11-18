package myau.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class BlockUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isReplaceable(BlockPos pos) {
        return isReplaceable(mc.theWorld.getBlockState(pos).getBlock());
    }

    public static boolean isReplaceable(Block block) {
        if (!block.getMaterial().isReplaceable()) {
            return false;
        }
        return !(block instanceof BlockSnow) || !(block.getBlockBoundsMaxY() > 0.125);
    }

    public static boolean isInteractable(BlockPos pos) {
        return isInteractable(mc.theWorld.getBlockState(pos).getBlock());
    }

    public static boolean isInteractable(Block block) {
        if (block instanceof BlockContainer) return true;
        if (block instanceof BlockWorkbench) return true;
        if (block instanceof BlockAnvil) return true;
        if (block instanceof BlockBed) return true;
        if (block instanceof BlockDoor && block.getMaterial() != Material.iron) return true;
        if (block instanceof BlockTrapDoor) return true;
        if (block instanceof BlockFenceGate) return true;
        if (block instanceof BlockFence) return true;
        if (block instanceof BlockButton) return true;
        if (block instanceof BlockLever) return true;
        if (block instanceof BlockJukebox) return true;
        return block instanceof BlockNote;
    }

    public static boolean isSolid(Block block) {
        if (block instanceof BlockStairs) return false;
        if (block instanceof BlockSlab) return false;
        if (block instanceof BlockEndPortalFrame) return false;
        if (block instanceof BlockEndPortal) return false;
        if (block instanceof BlockVine) return false;
        if (block instanceof BlockPumpkin) return false;
        if (block instanceof BlockCactus) return false;
        if (block instanceof BlockBush) return false;
        if (block instanceof BlockFalling) return false;
        if (block instanceof BlockWeb) return false;
        if (block instanceof BlockPane) return false;
        if (block instanceof BlockCarpet) return false;
        if (block instanceof BlockSnow) return false;
        if (block instanceof BlockFence) return false;
        if (block instanceof BlockFenceGate) return false;
        if (block instanceof BlockWall) return false;
        if (block instanceof BlockLadder) return false;
        if (block instanceof BlockTorch) return false;
        if (block instanceof BlockRedstoneWire) return false;
        if (block instanceof BlockRedstoneDiode) return false;
        if (block instanceof BlockBasePressurePlate) return false;
        if (block instanceof BlockTripWire) return false;
        if (block instanceof BlockTripWireHook) return false;
        if (block instanceof BlockRailBase) return false;
        if (block instanceof BlockSlime) return false;
        return !(block instanceof BlockTNT);
    }

    public static Vec3 getHitVec(BlockPos pos, EnumFacing facing, float yaw, float pitch) {
        MovingObjectPosition mop = RotationUtil.rayTraceBlocks(yaw, pitch, mc.playerController.getBlockReachDistance(), 1.0f);
        if (mop != null
            && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && mop.getBlockPos().equals(pos)
            && mop.sideHit == facing) {
            return mop.hitVec;
        }
        return getClickVec(pos, facing);
    }

    public static Vec3 getClickVec(BlockPos pos, EnumFacing facing) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        Vec3 sample = new Vec3(
            (double) pos.getX() + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), block.getBlockBoundsMinX()), block.getBlockBoundsMaxX()),
            (double) pos.getY() + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), block.getBlockBoundsMinY()), block.getBlockBoundsMaxY()),
            (double) pos.getZ() + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), block.getBlockBoundsMinZ()), block.getBlockBoundsMaxZ())
        );
        switch (facing) {
            default:
                return new Vec3(sample.xCoord, (double) pos.getY() + block.getBlockBoundsMinY(), sample.zCoord);
            case UP:
                return new Vec3(sample.xCoord, (double) pos.getY() + block.getBlockBoundsMaxY(), sample.zCoord);
            case NORTH:
                return new Vec3(sample.xCoord, sample.yCoord, (double) pos.getZ() + block.getBlockBoundsMinZ());
            case EAST:
                return new Vec3((double) pos.getX() + block.getBlockBoundsMaxX(), sample.yCoord, sample.zCoord);
            case SOUTH:
                return new Vec3(sample.xCoord, sample.yCoord, (double) pos.getZ() + block.getBlockBoundsMaxZ());
            case WEST:
                return new Vec3((double) pos.getX() + block.getBlockBoundsMinX(), sample.yCoord, sample.zCoord);
        }
    }
}
