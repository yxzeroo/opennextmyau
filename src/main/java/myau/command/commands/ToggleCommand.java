package myau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.Myau;
import myau.command.Command;
import myau.module.Module;
import myau.util.ChatUtil;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super(new ArrayList<>(Arrays.asList("toggle", "t")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(String.format("%sUsage: .%s <&omodule&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
            return;
        }
        Module module = Myau.moduleManager.getModule(args.get(1));
        if (module == null) {
            ChatUtil.sendFormatted(String.format("%sModule not found (&o%s&r)&r", Myau.clientName, args.get(1)));
            return;
        }
        boolean shouldToggle = true;
        if (args.size() >= 3) {
            String stateArg = args.get(2);
            if (stateArg.equalsIgnoreCase("true") || stateArg.equalsIgnoreCase("on") || stateArg.equalsIgnoreCase("1")) {
                shouldToggle = !module.isEnabled();
            } else if (stateArg.equalsIgnoreCase("false") || stateArg.equalsIgnoreCase("off") || stateArg.equalsIgnoreCase("0")) {
                shouldToggle = module.isEnabled();
            }
        }
        if (shouldToggle && Myau.moduleManager.toggleModule(module)) {
            String status = module.isEnabled() ? "&a&lON" : "&c&lOFF";
            ChatUtil.sendFormatted(String.format("%s%s: %s&r", Myau.clientName, module.getName(), status));
        }
    }

    @Override
    public ArrayList<String> completeTab(ArrayList<String> args) {
        if (args.size() == 2) {
            return Myau.moduleManager.modules.values().stream()
                .map(module -> module.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return new ArrayList<>();
    }
}
