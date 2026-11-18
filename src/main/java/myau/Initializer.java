package myau;

import myau.mixin.IAccessorMinecraft;
import net.minecraft.client.Minecraft;

public class Initializer {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void preInit() {
        ((IAccessorMinecraft) mc).getLogger().info("Meow!");
    }
}
