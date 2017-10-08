package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;

import lombok.Getter;
import lombok.NonNull;
import lombok.NoArgsConstructor;

// Событие, относящееся к какому-то перегону

@NoArgsConstructor(force=true)
public abstract class TrackEvent extends BaseEvent {
    @Getter @NonNull private final StationPair stationPair;
    @Getter @NonNull private final Long timeStart;
    @Getter @NonNull private final Long timeEnd;

    public TrackEvent(Integer runIndex,
                      Integer locoFrameIndex, Integer teamFrameIndex,
                      StationPair stationPair, Long timeStart, Long timeEnd) {
        super(runIndex, locoFrameIndex, teamFrameIndex);
        this.stationPair = stationPair;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
    }

    @Override public Long getEventTime() {
        return timeEnd;
    }

    @Override public String toString() {
        return toString(null);
    }

    protected Station getStation() {
        Long stId = stationPair.getStationToId();
        return SchedulingData.getFrameData().getStation(stId);
    }

    protected String toString(String subFields) {
        String st1 = stationPair.getStationFromId().toString(),
            st2 = stationPair.getStationToId().toString(),
            t1 = (timeStart > Long.MIN_VALUE) ? ("@" + timeStart) : "",
            t2 = (timeEnd < Long.MAX_VALUE) ? ("@" + timeEnd) : "",
            sub = (subFields == null) ? "" : (", " + subFields);
        return super.toString(st1 + t1 + " → " + st2 + t2 + sub);
    }
}
