package myau.module.modules;

import myau.module.Module;

public class AntiObfuscate extends Module {
    public AntiObfuscate() {
        super("AntiObfuscate", false, 0, true);
    }

    public String stripObfuscated(String text) {
        return text.replaceAll("\u00a7k", "");
    }
}
