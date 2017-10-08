package ru.programpark.planners.common;

public class LocoDepartEvent extends ReferenceEvent implements Loco.Event {
    public LocoDepartEvent(Integer runIndex,
                           Integer locoFrameIndex, Integer teamFrameIndex,
                           Long trainId, Integer index) {
        super(runIndex, locoFrameIndex, teamFrameIndex,
              trainId, index, index);
    }

    @Override public Long getStationId() {
        Train.EventContainer track = (Train.EventContainer) dereference();
        return track.getLink().getFrom().getId();
    }

    @Override public Long getLocoReserveReadyTime() {
        return getLocoReadyTime();
    }
}
