package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;

public class TeamDepartEvent extends ReferenceEvent implements Team.Event {
    public TeamDepartEvent(Integer runIndex,
                           Integer locoFrameIndex, Integer teamFrameIndex,
                           Long trainId, Integer index) {
        super(runIndex, locoFrameIndex, teamFrameIndex,
              trainId, index, index);
    }

    @Override public Station getStation() {
        Train.EventContainer track = (Train.EventContainer) dereference();
        return track.getLink().getTo();
    }
}
