package myau.property.properties;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import myau.property.Property;

public class PercentProperty extends Property<Integer> {
    private final Integer minimum;
    private final Integer maximum;

    public PercentProperty(String name, Integer value) {
        this(name, value, null);
    }

    public PercentProperty(String name, Integer value, BooleanSupplier visibleCheck) {
        this(name, value, 0, 100, visibleCheck);
    }

    public PercentProperty(String name, Integer value, Integer minimum, Integer maximum, BooleanSupplier visibleCheck) {
        super(name, value, v -> v >= minimum && v <= maximum, visibleCheck);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public String getValuePrompt() {
        return String.format("%d-%d%%", this.minimum, this.maximum);
    }

    @Override
    public String formatValue() {
        return String.format("&b%d%%", this.getValue());
    }

    @Override
    public boolean parseString(String input) {
        return this.setValue(Integer.parseInt(input.replace("%", "")));
    }

    @Override
    public boolean readJson(JsonObject jsonObject) {
        return this.setValue(jsonObject.get(this.getName()).getAsNumber().intValue());
    }

    @Override
    public void writeJson(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getValue());
    }
}
