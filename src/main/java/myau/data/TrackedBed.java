package myau.data;

import java.util.Objects;

public class TrackedBed {
    private final int hash;
    public final int itemKey;
    public final double x;
    public final double y;
    public final double z;

    public TrackedBed(int itemKey, double x, double y, double z) {
        this.itemKey = itemKey;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hash = Objects.hash(itemKey, (int) x, (int) y, (int) z);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }
        TrackedBed that = (TrackedBed) other;
        return this.itemKey == that.itemKey
            && (int) this.x == (int) that.x
            && (int) this.y == (int) that.y
            && (int) this.z == (int) that.z;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}
