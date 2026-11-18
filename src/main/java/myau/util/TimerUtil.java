package myau.util;

public class TimerUtil {
    private long lastMS = 0L;

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public boolean hasTimeElapsed(long time) {
        return this.getElapsedTime() >= time;
    }

    public void setTime(long time) {
        this.lastMS = time;
    }
}
