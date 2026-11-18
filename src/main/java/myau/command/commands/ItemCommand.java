package myau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ItemCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public ItemCommand() {
        super(new ArrayList<>(Arrays.asList("itemname", "item")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
        if (stack != null) {
            String displayName = stack.getDisplayName().replace('\u00a7', '&');
            String registryName = stack.getItem().getRegistryName();
            String nbtString = stack.hasTagCompound() ? stack.getTagCompound().toString().replace('\u00a7', '&') : "";
            ChatUtil.sendRaw(String.format("%s%s (%s) %s", ChatColors.formatColor(Myau.clientName), displayName, registryName, nbtString));
        }
    }
}
