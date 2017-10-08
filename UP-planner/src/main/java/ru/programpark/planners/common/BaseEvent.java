package ru.programpark.planners.common;

import lombok.Getter;
import lombok.Setter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

@RequiredArgsConstructor
@NoArgsConstructor(force=true)
public abstract class BaseEvent
implements CommonEventContainer.Event, Comparable<BaseEvent> {
    @Getter @NonNull private final Integer runIndex;
    @Getter @NonNull private final Integer locoFrameIndex;
    @Getter @NonNull private final Integer teamFrameIndex;
    @Getter @NonNull private boolean cancelled = false;

    @Override public Long getTrainReadyTime() {
        return getEventTime();
    }

    @Override public Long getLocoReadyTime() {
        return getEventTime();
    }

    @Override public Long getTeamReadyTime() {
        return getEventTime();
    }

    @Override public void cancel() {
        cancelled = true;
    }

    @Override public String toString() {
        return toString(null);
    }

    protected String toString(String subFields) {
        String cls = getClass().getSimpleName();
        String frame = (runIndex >= 0)
            ? ("#" + runIndex +
                   "-" + locoFrameIndex + "-" + teamFrameIndex +
                   (cancelled ? " ⌫" : "") + ", ")
            : "";
        return (cls + "{" + frame + subFields + "}");
    }

    public int compareTo(BaseEvent other) {
        int c;
        if ((c = this.getRunIndex().compareTo(other.getRunIndex())) != 0)
            return c;
        if ((c = this.getLocoFrameIndex().compareTo(other.getLocoFrameIndex())) != 0)
            return c;
        if ((c = this.getTeamFrameIndex().compareTo(other.getTeamFrameIndex())) != 0)
            return c;
        return 0;
    }

    public static <T extends BaseEvent> T min(T event1, T event2) {
        return event1.compareTo(event2) <= 0 ? event1 : event2;
    }

    public static <T extends BaseEvent> T max(T event1, T event2) {
        return event1.compareTo(event2) >= 0 ? event1 : event2;
    }
}
