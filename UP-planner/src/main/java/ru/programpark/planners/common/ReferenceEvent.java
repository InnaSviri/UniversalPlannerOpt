package ru.programpark.planners.common;

import lombok.Getter;
import lombok.Delegate;

public abstract class ReferenceEvent extends BaseEvent {    
    @Getter private final Long trainId;
    @Getter private final Integer startIndex;
    @Getter private final Integer endIndex;

    public ReferenceEvent(Integer runIndex,
                          Integer locoFrameIndex, Integer teamFrameIndex,
                          Long trainId, Integer startIndex, Integer endIndex) {
        super(runIndex, locoFrameIndex, teamFrameIndex);
        this.trainId = trainId;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Delegate(types=CommonEvent.class)
    protected CommonEvent dereference() {
        Train train = SchedulingData.getFrameData().getTrain(trainId);
        return (endIndex < 0) ?
            train.getPreRoute() : train.getRoute().get(endIndex);
    }

    public Long distance() {
        return SchedulingData.getFrameData().getTrain(trainId)
                             .distance(startIndex, endIndex);
    }

    public Long getStartTime() {
        Train.Event evt =
            SchedulingData.getFrameData().getTrain(trainId)
                          .lastEvent(startIndex);
        if (evt instanceof TrackEvent) {
            return ((TrackEvent) evt).getTimeStart();
        } else if (evt instanceof StationEvent) {
            return ((StationEvent) evt).getTime();
        } else {
            return null;
        }
    }

    public Long getEndTime() {
        Train.Event evt =
            SchedulingData.getFrameData().getTrain(trainId)
                          .lastEvent(endIndex);
        if (evt instanceof TrackEvent) {
            return ((TrackEvent) evt).getTimeEnd();
        } else if (evt instanceof StationEvent) {
            return ((StationEvent) evt).getTime();
        } else {
            return null;
        }
    }

    @Override public String toString() {
        return toString(null);
    }

    protected String toString(String subFields) {
        String id = trainId.toString(),
            start = startIndex.toString(),
            end = endIndex.toString(),
            sub = (subFields == null) ? "" : (", " + subFields);
        return super.toString(id + ":" + start + ".." + end + sub);
    }
}
