package myau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class PlayerListCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public PlayerListCommand() {
        super(new ArrayList<>(Arrays.asList("playerlist", "players")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ArrayList<String> playerNames = new ArrayList<>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            playerNames.add(info.getGameProfile().getName().replace("\u00a7", "&"));
        }
        if (playerNames.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sNo players&r", Myau.clientName));
        } else {
            ChatUtil.sendRaw(String.format(
                ChatColors.formatColor("%sPlayers:&r %s"),
                ChatColors.formatColor(Myau.clientName),
                String.join(", ", playerNames)
            ));
        }
    }
}
