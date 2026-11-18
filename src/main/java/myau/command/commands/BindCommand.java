package myau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.Myau;
import myau.command.Command;
import myau.module.Module;
import myau.util.ChatUtil;
import myau.util.KeyBindUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class BindCommand extends Command {
    public BindCommand() {
        super(new ArrayList<>(Arrays.asList("bind", "b")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 3) {
            if (args.size() == 2 && (args.get(1).equalsIgnoreCase("list") || args.get(1).equalsIgnoreCase("l"))) {
                List<Module> boundModules = Myau.moduleManager.modules.values().stream()
                    .filter(module -> module.getKey() != 0)
                    .collect(Collectors.toList());
                if (boundModules.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%sNo binds&r", Myau.clientName));
                    return;
                }
                ChatUtil.sendFormatted(String.format("%sBinds:&r", Myau.clientName));
                for (Module module : boundModules) {
                    String prefixColor = module.isHidden() ? "&8" : "&7";
                    ChatUtil.sendFormatted(String.format("%s\u00bb&r %s&r", prefixColor, module.formatModule()));
                }
                return;
            }
            String cmd = args.get(0).toLowerCase(Locale.ROOT);
            ChatUtil.sendFormatted(String.format("%sUsage: .%s <&omodule&r> <&okey&r>&r | .%s &olist&r", Myau.clientName, cmd, cmd));
            return;
        }
        String keyInput = args.get(2).toUpperCase();
        int keyIndex = Keyboard.getKeyIndex(keyInput);
        if (keyIndex == 0) {
            int mouseButton = Mouse.getButtonIndex(keyInput);
            if (mouseButton != -1) {
                keyIndex = mouseButton - 100;
            }
        }
        if (args.get(1).equals("*")) {
            for (Module module : Myau.moduleManager.modules.values()) {
                module.setKey(keyIndex);
            }
            ChatUtil.sendFormatted(String.format("%sBound all modules to &l[%s]&r", Myau.clientName, KeyBindUtil.getKeyName(keyIndex)));
            return;
        }
        Module module = Myau.moduleManager.getModule(args.get(1));
        if (module == null) {
            ChatUtil.sendFormatted(String.format("%sModule not found (&o%s&r)&r", Myau.clientName, args.get(1)));
            return;
        }
        module.setKey(keyIndex);
        ChatUtil.sendFormatted(String.format("%sBound &o%s&r to &l[%s]&r", Myau.clientName, module.getName(), KeyBindUtil.getKeyName(keyIndex)));
    }

    @Override
    public ArrayList<String> completeTab(ArrayList<String> args) {
        if (args.size() == 2) {
            ArrayList<String> completions = Myau.moduleManager.modules.values().stream()
                .map(module -> module.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(ArrayList::new));
            completions.add("list");
            completions.add("l");
            return completions;
        }
        if (args.size() == 3) {
            String target = args.get(1);
            if (!target.equals("*") && Myau.moduleManager.getModule(target) == null) {
                return new ArrayList<>();
            }
            ArrayList<String> keyNames = new ArrayList<>();
            for (int i = 0; i < 256; ++i) {
                String keyName = Keyboard.getKeyName(i);
                if (keyName != null) {
                    keyNames.add(keyName.toLowerCase(Locale.ROOT));
                }
            }
            for (int i = 0; i < Mouse.getButtonCount(); ++i) {
                String buttonName = Mouse.getButtonName(i);
                if (buttonName != null) {
                    keyNames.add(buttonName.toLowerCase(Locale.ROOT));
                }
            }
            return keyNames;
        }
        return new ArrayList<>();
    }
}
