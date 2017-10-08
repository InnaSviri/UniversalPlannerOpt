package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;

import lombok.Getter;
import lombok.NonNull;

// Событие, относящееся к какой-то станции

public abstract class StationEvent extends BaseEvent {
    @Getter @NonNull private final Long stationId;
    @Getter @NonNull private final Long time;

    public StationEvent(Integer runIndex,
                        Integer locoFrameIndex, Integer teamFrameIndex,
                        Long stationId, Long time) {
        super(runIndex, locoFrameIndex, teamFrameIndex);
        this.stationId = stationId;
        this.time = time;
    }

    @Override public Long getEventTime() {
        return time;
    }

    protected Station getStation() {
        return SchedulingData.getFrameData().getStation(stationId);
    }

    @Override public String toString() {
        return toString(null);
    }

    protected String toString(String subFields) {
        String st = stationId.toString(),
            t = "@" + time,
            sub = (subFields == null) ? "" : (", " + subFields);
        return super.toString(st + t + sub);
    }
}
