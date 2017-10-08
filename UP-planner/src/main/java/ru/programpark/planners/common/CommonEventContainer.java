package ru.programpark.planners.common;

import ru.programpark.entity.util.LoggingAssistant;

import lombok.Delegate;

import java.util.Stack;
import java.util.EmptyStackException;
import java.util.List;
import java.util.ArrayList;
import java.io.Writer;
import java.io.IOException;

public abstract class CommonEventContainer implements CommonEvent {

    // Событие, которое может быть помещено во вместилище
    public interface Event extends CommonEvent {
        // Индексы интервала, при обработке которого произошло событие
        Integer getRunIndex();
        Integer getLocoFrameIndex();
        Integer getTeamFrameIndex();
        // Отмена события
        void cancel();
        boolean isCancelled();
    }

    // История событий
    protected abstract Stack<Event> getEventStack();

    public Event addEvent(Event event) {
        getEventStack().push(event);
        return event;
    }

    private final Event NOT_CANCELLED = new BaseEvent(-1, -1, -1) {
        @Override public Long getEventTime() { return 0L; }
        @Override public boolean equals (Object o) {
            return ! ((Event) o).isCancelled();
        }
    };

    public boolean hasEvents() {
        return getEventStack().contains(NOT_CANCELLED);
    }

    @Delegate(types=CommonEvent.class)
    public <T extends Event> T lastEvent() throws EmptyStackException {
        int i = getEventStack().lastIndexOf(NOT_CANCELLED);
        if (i >= 0) {
            return (T) (getEventStack().get(i));
        } else {
            throw new EmptyStackException();
        }
    }

    private int eventVsFrame(Event event, Integer runIndex,
                             Integer locoFrameIndex, Integer teamFrameIndex) {
        if (runIndex != null) {
            int c = event.getRunIndex().compareTo(runIndex);
            if (c != 0) return c;
        }
        if (locoFrameIndex != null) {
            int c = event.getLocoFrameIndex().compareTo(locoFrameIndex);
            if (c != 0) return c;
        }
        if (teamFrameIndex != null) {
            int c = event.getTeamFrameIndex().compareTo(teamFrameIndex);
            if (c != 0) return c;
        }
        return 0;
    }

    // Последнее событие, зарегистрированное не позже данного интервала
    public <T extends Event> T
    lastEvent(Integer runIndex, Integer locoFrameIndex,
              Integer teamFrameIndex) {
        if (locoFrameIndex != null || teamFrameIndex != null) {
            Stack<Event> evtStack = getEventStack();
            for (int i = evtStack.size() - 1; i >= 0; --i) {
                Event evt = evtStack.get(i);
                if (! evt.isCancelled() &&
                        eventVsFrame(evt, runIndex, locoFrameIndex,
                                     teamFrameIndex) <= 0) {
                    return (T) evt;
                }
            }
        }
        return null;
    }

    public <T extends Event> T lastEvent(Class type) {
        Stack<Event> evtStack = getEventStack();
        for (int i = evtStack.size() - 1; i >= 0; --i) {
            Event evt = evtStack.get(i);
            if (! evt.isCancelled() && type.isInstance(evt)) {
                return (T) evt;
            }
        }
        return null;
    }

    public <T extends Event> T lastEvent(Event matchingEvent) {
        Stack<Event> evtStack = getEventStack();
        for (int i = evtStack.size() - 1; i >= 0; --i) {
            Event evt = evtStack.get(i);
            if (! evt.isCancelled() && matchingEvent.equals(evt)) {
                return (T) evt;
            }
        }
        return null;
    }

    // Первое событие, зарегистрированное не раньше данного интервала
    public <T extends Event> T
    firstEvent(Integer runIndex, Integer locoFrameIndex,
               Integer teamFrameIndex) {
        if (locoFrameIndex != null || teamFrameIndex != null) {
            for (Event evt : getEventStack()) {
                if (! evt.isCancelled() &&
                        eventVsFrame(evt, runIndex, locoFrameIndex,
                                     teamFrameIndex) >= 0) {
                    return (T) evt;
                }
            }
        }
        return null;
    }

    public <T extends Event> T firstEvent(Class type) {
        Stack<Event> evtStack = getEventStack();
        for (Event evt : getEventStack()) {
            if (! evt.isCancelled() && type.isInstance(evt)) {
                return (T) evt;
            }
        }
        return null;
    }

    public <T extends Event> T firstEvent(Event matchingEvent) {
        Stack<Event> evtStack = getEventStack();
        for (Event evt : getEventStack()) {
            if (! evt.isCancelled() && matchingEvent.equals(evt)) {
                return (T) evt;
            }
        }
        return null;
    }

    // Отменить события, зарегистрированные не раньше данного интервала
    public <T extends Event> List<T>
    cancelEvents(Integer runIndex, Integer locoFrameIndex,
                 Integer teamFrameIndex) {
        List<T> events = new ArrayList<>();
        if (locoFrameIndex != null || teamFrameIndex != null) {
           boolean cancel = false;
           for (Event evt : getEventStack()) {
               if (! cancel &&
                       eventVsFrame(evt, runIndex, locoFrameIndex,
                                    teamFrameIndex) >= 0) {
                   cancel = true;
               }
               if (cancel && ! evt.isCancelled()) {
                   evt.cancel();
                   events.add((T) evt);
               }
            }
        }
        return events;
    }

    public void logEvents(String prefix, Writer writer) {
        try {
            for (Event evt : getEventStack()) {
                writer.write(prefix + evt + "\n");
            }
        } catch (IOException e) {
            LoggingAssistant.logException(e);
        }
    }

}
