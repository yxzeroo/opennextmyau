package myau.event.events;

import myau.event.EventCancellable;

public class SwapItemEvent extends EventCancellable {
    private final int slot;
    private final int direction;

    public SwapItemEvent(int slot, int direction) {
        this.slot = slot;
        this.direction = Math.min(Math.max(direction, -1), 1);
    }

    public int setSlot(int currentSlot) {
        if (this.slot >= 0 && this.slot <= 8) {
            return this.slot;
        }
        return Math.floorMod(currentSlot - this.direction, 9);
    }
}
