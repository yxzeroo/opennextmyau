package myau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.Myau;
import myau.command.Command;
import myau.module.Module;
import myau.util.ChatUtil;

public class ShowCommand extends Command {
    public ShowCommand() {
        super(new ArrayList<>(Arrays.asList("show", "s", "unhide")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(String.format("%sUsage: .%s <&omodule&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
            return;
        }
        if (args.get(1).equals("*")) {
            for (Module module : Myau.moduleManager.modules.values()) {
                module.setHidden(false);
            }
            ChatUtil.sendFormatted(String.format("%sAll modules are no longer hidden in HUD&r", Myau.clientName));
            return;
        }
        Module module = Myau.moduleManager.getModule(args.get(1));
        if (module == null) {
            ChatUtil.sendFormatted(String.format("%sModule &o%s&r not found&r", Myau.clientName, args.get(1)));
            return;
        }
        if (!module.isHidden()) {
            ChatUtil.sendFormatted(String.format("%s&o%s&r is not hidden in HUD&r", Myau.clientName, module.getName()));
            return;
        }
        module.setHidden(false);
        ChatUtil.sendFormatted(String.format("%s&o%s&r is no longer hidden in HUD&r", Myau.clientName, module.getName()));
    }

    @Override
    public ArrayList<String> completeTab(ArrayList<String> args) {
        if (args.size() == 2) {
            return Myau.moduleManager.modules.values().stream()
                .filter(Module::isHidden)
                .map(module -> module.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return new ArrayList<>();
    }
}
