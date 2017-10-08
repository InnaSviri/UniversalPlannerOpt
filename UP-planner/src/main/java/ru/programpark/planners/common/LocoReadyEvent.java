package ru.programpark.planners.common;

public class LocoReadyEvent extends StationEvent implements Loco.Event {
    public LocoReadyEvent(Integer runIndex,
                          Integer locoFrameIndex, Integer teamFrameIndex,
                          Long stationId, Long time) {
        super(runIndex, locoFrameIndex, teamFrameIndex, stationId, time);
    }

    @Override public Long getLocoReadyTime() {
        return getLocoReserveReadyTime() + getStation().getProcessTime() / 2;
    }

    @Override public Long getLocoReserveReadyTime() {
        Long curTime = SchedulingData.getInputData().getCurrentTime();
        Long evTime = Math.max(getEventTime(), curTime);
        return evTime;
    }
}
