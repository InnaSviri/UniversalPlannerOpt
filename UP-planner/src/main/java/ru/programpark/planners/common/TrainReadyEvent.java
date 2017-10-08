package ru.programpark.planners.common;

public class TrainReadyEvent extends StationEvent implements Train.Event {
    public TrainReadyEvent(Integer runIndex,
                           Integer locoFrameIndex, Integer teamFrameIndex,
                           Long stationId, Long time) {
        super(runIndex, locoFrameIndex, teamFrameIndex, stationId, time);
    }

    @Override public TrainReadyEvent
    shiftedBy(Long shiftTime, Integer runIndex,
              Integer locoFrameIndex, Integer teamFrameIndex) {
        return new TrainReadyEvent(runIndex, locoFrameIndex, teamFrameIndex,
                                   getStationId(), getTime() + shiftTime);
    }

    @Override public TrainReadyEvent shiftedBy(Long shiftTime) {
        SchedulingFrame frame = SchedulingData.getCurrentFrame();
        return shiftedBy(shiftTime, frame.runIndex,
                         frame.locoFrameIndex, frame.teamFrameIndex);
    }

    @Override public Long getTrainReadyTime() {
        return getEventTime() + getStation().getProcessTime() / 2;
    }
}
