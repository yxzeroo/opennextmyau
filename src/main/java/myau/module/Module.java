package myau.module;

import myau.data.Box;
import myau.util.KeyBindUtil;

public abstract class Module {
    public final String name;
    public final boolean defaultEnabled;
    public final int defaultKey;
    public final boolean defaultHidden;
    public boolean enabled;
    public int key;
    public boolean hidden;
    public Box<Boolean> keyBindState;

    public Module(String name, boolean defaultEnabled, int defaultKey) {
        this(name, defaultEnabled, defaultKey, false);
    }

    public Module(String name, boolean defaultEnabled, int defaultKey, boolean defaultHidden) {
        this.name = name;
        this.enabled = this.defaultEnabled = defaultEnabled;
        this.key = this.defaultKey = defaultKey;
        this.hidden = this.defaultHidden = defaultHidden;
        this.keyBindState = new Box<>(false);
    }

    public String getName() {
        return this.name;
    }

    public String formatModule() {
        String keyPrefix = this.key == 0 ? "" : String.format("&l[%s] &r", KeyBindUtil.getKeyName(this.key));
        String status = this.enabled ? "&a&lON" : "&c&lOFF";
        return String.format("%s%s &r(%s&r)", keyPrefix, this.name, status);
    }

    public String[] getSuffix() {
        return new String[0];
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnabled();
            } else {
                this.onDisabled();
            }
        }
    }

    public void resetEnabled() {
        this.setEnabled(this.defaultEnabled);
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void resetKey() {
        this.setKey(this.defaultKey);
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void resetHidden() {
        this.setHidden(this.defaultHidden);
    }

    public Box<Boolean> getKeyBindState() {
        return this.keyBindState;
    }

    public void setKeyBindState(Box<Boolean> keyBindState) {
        this.keyBindState = keyBindState;
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String propertyName) {
    }
}
