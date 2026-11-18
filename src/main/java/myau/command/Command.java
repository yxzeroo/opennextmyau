package myau.command;

import java.util.ArrayList;
import java.util.Locale;

public abstract class Command {
    public final ArrayList<String> names;

    public Command(ArrayList<String> names) {
        this.names = new ArrayList<>(names.size());
        for (String name : names) {
            this.names.add(name.toLowerCase(Locale.ROOT));
        }
    }

    public abstract void runCommand(ArrayList<String> args);

    public ArrayList<String> completeTab(ArrayList<String> args) {
        return new ArrayList<>();
    }
}
