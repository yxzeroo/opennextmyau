package myau.event;

import java.lang.reflect.Method;

public class EventListener {
    private final Object target;
    private final Method method;
    private final int priority;

    public EventListener(Object target, Method method, int priority) {
        this.target = target;
        this.method = method;
        this.priority = priority;
    }

    public Object getTarget() {
        return this.target;
    }

    public Method getMethod() {
        return this.method;
    }

    public int getPriority() {
        return this.priority;
    }
}
