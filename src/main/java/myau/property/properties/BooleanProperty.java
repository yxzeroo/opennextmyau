package myau.property.properties;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import myau.property.Property;

public class BooleanProperty extends Property<Boolean> {
    public BooleanProperty(String name, Boolean value) {
        this(name, value, null);
    }

    public BooleanProperty(String name, Boolean value, BooleanSupplier visibleCheck) {
        super(name, value, visibleCheck);
    }

    @Override
    public String getValuePrompt() {
        return "true/false";
    }

    @Override
    public String formatValue() {
        return this.getValue() ? "&atrue" : "&cfalse";
    }

    @Override
    public boolean parseString(String input) {
        if (input == null) {
            return this.setValue(!this.getValue());
        }
        if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("on") || input.equalsIgnoreCase("1")) {
            return this.setValue(true);
        }
        if (input.equalsIgnoreCase("false") || input.equalsIgnoreCase("off") || input.equalsIgnoreCase("0")) {
            return this.setValue(false);
        }
        return false;
    }

    @Override
    public boolean readJson(JsonObject jsonObject) {
        return this.setValue(jsonObject.get(this.getName()).getAsBoolean());
    }

    @Override
    public void writeJson(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getValue());
    }
}
