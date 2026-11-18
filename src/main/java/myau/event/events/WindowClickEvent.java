package myau.event.events;

import myau.event.EventCancellable;

public class WindowClickEvent extends EventCancellable {
    private final int windowId;
    private final int slotId;
    private final int mouseButton;
    private final int mode;

    public WindowClickEvent(int windowId, int slotId, int mouseButton, int mode) {
        this.windowId = windowId;
        this.slotId = slotId;
        this.mouseButton = mouseButton;
        this.mode = mode;
    }

    public int getWindowId() {
        return this.windowId;
    }

    public int getSlotId() {
        return this.slotId;
    }

    public int getMouseButton() {
        return this.mouseButton;
    }

    public int getMode() {
        return this.mode;
    }
}
