package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Link;

/**
 * User: oracle
 * Date: 20.05.14
 route([track(station(Id1), station(Id2),time_start(TimeStart),time_end(TimeEnd),slot_id(SlotId),locoState(State), train(TrainId)),…]))
 */

//slot_loco
public class RealLocoTrack extends BaseLocoTrack {
    private Long timeStart;
    private Long timeEnd;
    private Long slotId = -1L;
    public Boolean specified = false;

    public RealLocoTrack() {
    }

    public RealLocoTrack(Link link, State state, Long trainId, Long timeStart, Long timeEnd, Long slotId) {
        super(link, state, trainId);
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.slotId = slotId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RealLocoTrack");
        sb.append("{timeStart=").append(timeStart);
        sb.append(", timeEnd=").append(timeEnd);
        sb.append(", slotId=").append(slotId);
        sb.append('}');
        return sb.toString();
    }

    public Long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Long timeStart) {
        this.timeStart = timeStart;
    }

    public Long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }
}

