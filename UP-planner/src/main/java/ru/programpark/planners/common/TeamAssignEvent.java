package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;
import lombok.Getter;

public class TeamAssignEvent extends ReferenceEvent implements Team.Event {
    @Getter Long locoId;

    public TeamAssignEvent(Integer runIndex,
                           Integer locoFrameIndex, Integer teamFrameIndex,
                           Long locoId, Long trainId,
                           Integer startIndex, Integer endIndex) {
        super(runIndex, locoFrameIndex, teamFrameIndex,
              trainId, startIndex, endIndex);
        this.locoId = locoId;
    }

    public TeamAssignEvent(Long locoId, Long trainId,
                           Integer startIndex, Integer endIndex) {
        this(SchedulingData.getCurrentFrame().runIndex,
             SchedulingData.getCurrentFrame().locoFrameIndex,
             SchedulingData.getCurrentFrame().teamFrameIndex,
             locoId, trainId, startIndex, endIndex);
    }

    @Override public Station getStation() {
        Train.EventContainer track = (Train.EventContainer) dereference();
        return track.getLink().getTo();
    }
}
