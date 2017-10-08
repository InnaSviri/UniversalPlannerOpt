package ru.programpark.planners.common;

import ru.programpark.entity.fixed.StationPair;

public class TrainArriveEvent extends TrackEvent implements Train.Event {
    public TrainArriveEvent(Integer runIndex,
                            Integer locoFrameIndex, Integer teamFrameIndex,
                            StationPair stationPair, Long time) {
        super(runIndex, locoFrameIndex, teamFrameIndex,
              stationPair, Long.MIN_VALUE, time);
    }

    @Override public TrainArriveEvent
    shiftedBy(Long shiftTime, Integer runIndex,
              Integer locoFrameIndex, Integer teamFrameIndex) {
        return new TrainArriveEvent(runIndex, locoFrameIndex, teamFrameIndex,
                                    getStationPair(),
                                    getTimeEnd() + shiftTime);
    }

    @Override public TrainArriveEvent shiftedBy(Long shiftTime) {
        SchedulingFrame frame = SchedulingData.getCurrentFrame();
        return shiftedBy(shiftTime, frame.runIndex,
                         frame.locoFrameIndex, frame.teamFrameIndex);
    }

    @Override public Long getTrainReadyTime() {
        return getEventTime() + getStation().getProcessTime();
    }

    @Override public Long getLocoReadyTime() {
        return getEventTime() + getStation().getProcessTime();
    }

    @Override public Long getTeamReadyTime() {
        return getEventTime() + getStation().getNormTime();
    }
}
