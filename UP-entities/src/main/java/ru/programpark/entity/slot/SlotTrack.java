package ru.programpark.entity.slot;

import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Track;
import ru.programpark.entity.loco.RealLocoTrack;
import ru.programpark.entity.team.RealTeamTrack;
import ru.programpark.entity.train.OneTaskTrack;

/**
 * Date: 30.05.14
 * +slot_train
 */
public class SlotTrack extends Track {
    private Long slotId = -1L;
    public Boolean specified = false;
    public Long trainId;
    public Integer trackIndex = -1;

    public SlotTrack() {
    }

    public SlotTrack(SlotTrack slotTrack) {
        super(slotTrack.getLink(), new Long(slotTrack.getTimeStart()), new Long(slotTrack.getTimeEnd()));
        this.slotId = slotTrack.slotId;
        this.trainId = slotTrack.trainId;
        this.trackIndex = slotTrack.trackIndex;
    }

    public SlotTrack(Link link, Long timeStart, Long timeEnd, Long slotId) {
        super(link, timeStart, timeEnd);
        this.slotId = slotId;
    }

    public SlotTrack(Link link, Long trainId, Long timeStart, Long timeEnd, Long slotId) {
        this(link, timeStart, timeEnd, slotId);
        this.trainId = trainId;
    }

    public SlotTrack(OneTaskTrack track) {
        super(track.getLink(), track.getTimeStart(), track.getTimeEnd());
        this.slotId = track.getSlotId();
        this.trainId = track.trainId;
    }

    public SlotTrack(RealLocoTrack track) {
        super(track.getLink(), track.getTimeStart(), track.getTimeEnd());
        this.slotId = track.getSlotId();
        this.trainId = track.getTrainId();
    }

    public SlotTrack(RealTeamTrack track) {
        super(track.getLink(), track.getTimeStart(), track.getTimeEnd());
        this.slotId = track.getSlotId();
        this.trainId = track.trainId;
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            SlotTrack that = (SlotTrack) o;
            return (slotId == null ? that.slotId == null : slotId.equals(that.slotId));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (31 * super.hashCode() + (slotId != null ? slotId.hashCode() : 0));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SlotTrack");
        sb.append("{link=").append(getLink());
        sb.append(", timeStart=").append(getTimeStart());
        sb.append(", timeEnd=").append(getTimeEnd());
        sb.append(", slotId=").append(getSlotId());
        sb.append('}');
        return sb.toString();
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }
}
