package myau.property;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import myau.module.Module;

public abstract class Property<T> {
    private final String name;
    private final T defaultValue;
    private final Predicate<T> validator;
    private final BooleanSupplier visibleCheck;
    private T value;
    private Module owner;

    protected Property(String name, T value, BooleanSupplier visibleCheck) {
        this(name, value, null, visibleCheck);
    }

    protected Property(String name, T value, Predicate<T> validator, BooleanSupplier visibleCheck) {
        this.name = name;
        this.defaultValue = value;
        this.validator = validator;
        this.visibleCheck = visibleCheck;
        this.value = value;
        this.owner = null;
    }

    public String getName() {
        return this.name;
    }

    public abstract String getValuePrompt();

    public boolean isVisible() {
        return this.visibleCheck == null || this.visibleCheck.getAsBoolean();
    }

    public T getValue() {
        return this.value;
    }

    public abstract String formatValue();

    public boolean setValue(T value) {
        if (this.validator != null && !this.validator.test(value)) {
            return false;
        }
        this.value = value;
        if (this.owner != null) {
            this.owner.verifyValue(this.name);
        }
        return true;
    }

    public void reset() {
        this.setValue(this.defaultValue);
    }

    public void registerTo(Module module) {
        this.owner = module;
    }

    public abstract boolean parseString(String input);

    public abstract boolean readJson(JsonObject jsonObject);

    public abstract void writeJson(JsonObject jsonObject);
}
