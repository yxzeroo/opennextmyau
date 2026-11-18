package myau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;

public class EnemyCommand extends Command {
    public EnemyCommand() {
        super(new ArrayList<>(Arrays.asList("enemy", "e", "target")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() >= 2) {
            String cmd = args.get(0).toLowerCase(Locale.ROOT);
            switch (args.get(1).toLowerCase(Locale.ROOT)) {
                case "add": {
                    if (args.size() < 3) {
                        ChatUtil.sendFormatted(String.format("%sUsage: .%s add <&oname&r>&r", Myau.clientName, cmd));
                        return;
                    }
                    String added = Myau.enemyManager.add(args.get(2));
                    if (added == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is already in your enemy list&r", Myau.clientName, args.get(2)));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your enemy list&r", Myau.clientName, added));
                    return;
                }
                case "remove": {
                    if (args.size() < 3) {
                        ChatUtil.sendFormatted(String.format("%sUsage: .%s remove <&oname&r>&r", Myau.clientName, cmd));
                        return;
                    }
                    String removed = Myau.enemyManager.remove(args.get(2));
                    if (removed == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is not in your enemy list&r", Myau.clientName, args.get(2)));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your enemy list&r", Myau.clientName, removed));
                    return;
                }
                case "list": {
                    ArrayList<String> enemies = Myau.enemyManager.getPlayers();
                    if (enemies.isEmpty()) {
                        ChatUtil.sendFormatted(String.format("%sNo enemies&r", Myau.clientName));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sEnemies:&r", Myau.clientName));
                    for (String enemy : enemies) {
                        ChatUtil.sendRaw(String.format(ChatColors.formatColor("   &o%s&r"), enemy));
                    }
                    return;
                }
                case "clear": {
                    Myau.enemyManager.clear();
                    ChatUtil.sendFormatted(String.format("%sCleared your enemy list&r", Myau.clientName));
                    return;
                }
            }
        }
        ChatUtil.sendFormatted(String.format("%sUsage: .%s <&oadd&r/&oremove&r/&olist&r/&oclear&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
    }

    @Override
    public ArrayList<String> completeTab(ArrayList<String> args) {
        if (args.size() == 2) {
            return new ArrayList<>(Arrays.asList("add", "remove", "list", "clear"));
        }
        if (args.size() == 3) {
            String action = args.get(1).toLowerCase(Locale.ROOT);
            if (action.equals("add")) {
                return null;
            }
            if (action.equals("remove")) {
                return Myau.enemyManager.getPlayers();
            }
        }
        return new ArrayList<>();
    }
}
