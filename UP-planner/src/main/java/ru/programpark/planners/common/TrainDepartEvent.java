package ru.programpark.planners.common;

import ru.programpark.entity.fixed.StationPair;

public class TrainDepartEvent extends TrackEvent implements Train.Event {
    public TrainDepartEvent(Integer runIndex,
                            Integer locoFrameIndex, Integer teamFrameIndex,
                            StationPair stationPair, Long time) {
        super(runIndex, locoFrameIndex, teamFrameIndex,
              stationPair, time, Long.MAX_VALUE);
    }

    @Override public TrainDepartEvent
    shiftedBy(Long shiftTime, Integer runIndex,
              Integer locoFrameIndex, Integer teamFrameIndex) {
        return new TrainDepartEvent(runIndex, locoFrameIndex, teamFrameIndex,
                                    getStationPair(),
                                    getTimeStart() + shiftTime);
    }

    @Override public TrainDepartEvent shiftedBy(Long shiftTime) {
        SchedulingFrame frame = SchedulingData.getCurrentFrame();
        return shiftedBy(shiftTime, frame.runIndex, frame.locoFrameIndex,
                         frame.teamFrameIndex);
    }

    @Override public Long getEventTime() {
        return getTimeStart();
    }
}
