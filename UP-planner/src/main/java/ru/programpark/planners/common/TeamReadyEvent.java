package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;

public class TeamReadyEvent extends StationEvent implements Team.Event {
    public TeamReadyEvent(Integer runIndex,
                          Integer locoFrameIndex, Integer teamFrameIndex,
                          Long stationId, Long time) {
        super(runIndex, locoFrameIndex, teamFrameIndex, stationId, time);
    }

    @Override public Station getStation() {
        return super.getStation();
    }
}
