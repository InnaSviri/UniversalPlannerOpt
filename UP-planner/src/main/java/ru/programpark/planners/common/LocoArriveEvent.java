package ru.programpark.planners.common;

public class LocoArriveEvent extends ReferenceEvent implements Loco.Event {
    public LocoArriveEvent(Integer runIndex,
                           Integer locoFrameIndex, Integer teamFrameIndex,
                           Long trainId, Integer index) {
        super(runIndex, locoFrameIndex, teamFrameIndex,
              trainId, index, index);
    }

    @Override public Long getStationId() {
        Train.EventContainer track = (Train.EventContainer) dereference();
        return track.getLink().getTo().getId();
    }

    @Override public Long getLocoReserveReadyTime() {
        return getLocoReadyTime();
    }
}
