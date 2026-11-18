package myau.util;

import java.util.ArrayList;
import java.util.Map;
import myau.data.EnchantmentData;
import myau.enums.ChatColors;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

public class ItemEnchantmentUtil {
    private static final Map<Integer, EnchantmentData> ENCHANTMENT_MAP = new EnchantmentMap();

    private static ChatColors getLevelColor(int level, int maxLevel) {
        if (level > maxLevel) {
            return ChatColors.LIGHT_PURPLE;
        }
        if (level == maxLevel) {
            return ChatColors.RED;
        }
        switch (level) {
            case 1:
                return ChatColors.AQUA;
            case 2:
                return ChatColors.GREEN;
            case 3:
                return ChatColors.YELLOW;
            case 4:
                return ChatColors.GOLD;
            default:
                return ChatColors.GRAY;
        }
    }

    public static ArrayList<String> getEnchantmentStrings(ItemStack stack) {
        ArrayList<String> lines = new ArrayList<>();
        NBTTagList tagList = stack.getItem() == Items.enchanted_book
            ? Items.enchanted_book.getEnchantments(stack)
            : stack.getEnchantmentTagList();
        if (tagList == null) {
            return lines;
        }
        for (int i = 0; i < tagList.tagCount(); ++i) {
            EnchantmentData data = ENCHANTMENT_MAP.get(tagList.getCompoundTagAt(i).getInteger("id"));
            if (data == null) {
                continue;
            }
            short level = tagList.getCompoundTagAt(i).getShort("lvl");
            lines.add(ChatColors.formatColor(String.format("&r%s%s%d&r", data.shortName, getLevelColor(level, data.maxLevel), (int) level)));
        }
        return lines;
    }
}
