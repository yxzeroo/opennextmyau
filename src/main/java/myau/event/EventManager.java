package myau.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EventManager {
    private final Map<Class<? extends Event>, List<EventListener>> registry = new LinkedHashMap<>();

    public void register(Object target) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventTarget.class)
                && method.getParameterTypes().length == 1
                && Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                Class<? extends Event> eventClass = method.getParameterTypes()[0].asSubclass(Event.class);
                EventListener listener = new EventListener(target, method, method.getAnnotation(EventTarget.class).priority());
                if (!listener.getMethod().isAccessible()) {
                    listener.getMethod().setAccessible(true);
                }
                List<EventListener> listeners = this.registry.get(eventClass);
                if (listeners != null) {
                    listeners.add(listener);
                    listeners.sort(Comparator.comparingInt(EventListener::getPriority).reversed());
                } else {
                    List<EventListener> newList = new ArrayList<>();
                    newList.add(listener);
                    this.registry.put(eventClass, newList);
                }
            }
        }
    }

    public Event call(Event event) {
        List<EventListener> listeners = this.registry.get(event.getClass());
        if (listeners != null) {
            for (EventListener listener : listeners) {
                try {
                    listener.getMethod().invoke(listener.getTarget(), event);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored) {
                }
            }
        }
        return event;
    }
}
