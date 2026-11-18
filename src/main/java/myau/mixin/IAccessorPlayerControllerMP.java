package myau.mixin;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin(PlayerControllerMP.class)
public interface IAccessorPlayerControllerMP {
    @Accessor
    float getCurBlockDamageMP();

    @Accessor
    void setCurBlockDamageMP(float damage);

    @Accessor
    int getBlockHitDelay();

    @Accessor
    void setBlockHitDelay(int delay);

    @Accessor
    boolean getIsHittingBlock();

    @Accessor
    int getCurrentPlayerItem();

    @Accessor
    void setCurrentPlayerItem(int slot);

    @Invoker
    void callSyncCurrentPlayItem();
}
