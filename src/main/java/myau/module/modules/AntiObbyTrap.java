package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class AntiObbyTrap extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty setAir = new BooleanProperty("set-air", true);

    public AntiObbyTrap() {
        super("AntiObbyTrap", false, 0);
    }

    public boolean isTrappedBlock(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!block.getMaterial().isSolid() || !block.isFullBlock()) {
            return false;
        }
        Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        return block.getCollisionBoundingBox(mc.theWorld, pos, state).isVecInside(eyePos);
    }
}
