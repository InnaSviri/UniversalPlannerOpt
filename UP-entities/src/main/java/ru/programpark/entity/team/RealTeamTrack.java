package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Link;

/**
 * User: oracle
 * Date: 04.06.14
 * route([track(station(89), station(90), time_start(1349877600), time_end(1349888400),slot_id(123), locoState(0), loco(-1))...)
 */
public class RealTeamTrack extends BaseTeamTrack {
    Long slotId = -1L;
    Long timeStart = 0L;
    Long timeEnd = 0L;
    public Boolean specified = false;
    public Long trainId;

    public RealTeamTrack(Link link, State state, Long locoId, Long slotId, Long timeStart, Long timeEnd) {
        super(link, state, locoId);
        this.slotId = slotId;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RealTeamTrack that = (RealTeamTrack) o;

        if (slotId != null ? !slotId.equals(that.slotId) : that.slotId != null) return false;
        if (timeEnd != null ? !timeEnd.equals(that.timeEnd) : that.timeEnd != null) return false;
        if (timeStart != null ? !timeStart.equals(that.timeStart) : that.timeStart != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (slotId != null ? slotId.hashCode() : 0);
        result = 31 * result + (timeStart != null ? timeStart.hashCode() : 0);
        result = 31 * result + (timeEnd != null ? timeEnd.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RealTeamTrack");
        sb.append("{slotId=").append(slotId);
        sb.append(", timeStart=").append(timeStart);
        sb.append(", timeEnd=").append(timeEnd);
        sb.append('}');
        return sb.toString();
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
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
}
