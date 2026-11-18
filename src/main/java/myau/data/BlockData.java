package myau.data;

import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class BlockData {
    private final BlockPos blockPos;
    private final EnumFacing facing;

    public BlockData(BlockPos blockPos, EnumFacing facing) {
        this.blockPos = blockPos;
        this.facing = facing;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public EnumFacing getFacing() {
        return this.facing;
    }
}
