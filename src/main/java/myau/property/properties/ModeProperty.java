package myau.property.properties;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import myau.property.Property;

public class ModeProperty extends Property<Integer> {
    private final String[] modes;

    public ModeProperty(String name, Integer value, String[] modes) {
        this(name, value, modes, null);
    }

    public ModeProperty(String name, Integer value, String[] modes, BooleanSupplier visibleCheck) {
        super(name, value, visibleCheck);
        this.modes = modes;
    }

    @Override
    public String getValuePrompt() {
        return String.join(", ", this.modes);
    }

    public String getModeString() {
        int index = this.getValue();
        if (index < 0 || index >= this.modes.length) {
            return "";
        }
        return this.modes[index];
    }

    public String[] getModes() {
        return this.modes.clone();
    }

    @Override
    public String formatValue() {
        String mode = this.getModeString();
        return mode.isEmpty() ? "&4?" : String.format("&9%s", mode);
    }

    @Override
    public boolean parseString(String input) {
        String normalized = input.replace("_", "");
        for (int i = 0; i < this.modes.length; ++i) {
            if (normalized.equalsIgnoreCase(this.modes[i].replace("_", ""))) {
                return this.setValue(i);
            }
        }
        return false;
    }

    @Override
    public boolean readJson(JsonObject jsonObject) {
        return this.parseString(jsonObject.get(this.getName()).getAsString());
    }

    @Override
    public void writeJson(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getModeString());
    }
}
