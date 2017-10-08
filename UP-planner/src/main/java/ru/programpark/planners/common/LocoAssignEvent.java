package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;

import lombok.Getter;

public class LocoAssignEvent extends ReferenceEvent implements Loco.Event {
    @Getter private boolean relocation;

    public LocoAssignEvent(Integer runIndex,
                           Integer locoFrameIndex, Integer teamFrameIndex,
                           Long trainId, Integer startIndex, Integer endIndex) {
        super(runIndex, locoFrameIndex, teamFrameIndex,
              trainId, startIndex, endIndex);
    }

    public LocoAssignEvent(Long trainId, Integer startIndex, Integer endIndex) {
        this(SchedulingData.getCurrentFrame().runIndex,
             SchedulingData.getCurrentFrame().locoFrameIndex,
             SchedulingData.getCurrentFrame().teamFrameIndex,
             trainId, startIndex, endIndex);
    }

    public LocoAssignEvent(Long trainId, Integer startIndex, Integer endIndex,
                           Boolean relocation) {
        this(trainId, startIndex, endIndex);
        this.relocation = relocation;
    }

    private Station getEndStation() {
        Train.EventContainer track = (Train.EventContainer) dereference();
        return track.getLink().getTo();
    }

    @Override public Long getStationId() {
        return getEndStation().getId();
    }

    @Override public Train.EventContainer dereference() {
        return (Train.EventContainer) (super.dereference());
    }

    @Override public Long getLocoReserveReadyTime() {
        return getLocoReadyTime() - getEndStation().getProcessTime() / 2;
    }
}
