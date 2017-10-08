package ru.programpark.entity.raw_entities;

import ru.programpark.entity.fixed.StationPair;

public class BaseTrack {
    public Long stationFromId;
    public Long stationToId;
    public Long timeStart;
    public Long timeEnd;
    public Long slotId = -1L;


    public StationPair getStationPair() {
        return new StationPair(stationFromId, stationToId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())  return false;

        BaseTrack baseTrack = (BaseTrack) o;

        if (slotId != null ? !slotId.equals(baseTrack.slotId) : baseTrack.slotId != null) return false;
        if (stationFromId != null ? !stationFromId.equals(baseTrack.stationFromId) : baseTrack.stationFromId != null)
            return false;
        if (stationToId != null ? !stationToId.equals(baseTrack.stationToId) : baseTrack.stationToId != null)
            return false;
        if (timeEnd != null ? !timeEnd.equals(baseTrack.timeEnd) : baseTrack.timeEnd != null) return false;
        if (timeStart != null ? !timeStart.equals(baseTrack.timeStart) : baseTrack.timeStart != null) return false;

        return true;
    }

    public boolean equalFilelds(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        BaseTrack baseTrack = (BaseTrack) o;

        if (slotId != null ? !slotId.equals(baseTrack.slotId) : baseTrack.slotId != null) return false;
        if (stationFromId != null ? !stationFromId.equals(baseTrack.stationFromId) : baseTrack.stationFromId != null)
            return false;
        if (stationToId != null ? !stationToId.equals(baseTrack.stationToId) : baseTrack.stationToId != null)
            return false;
        if (timeEnd != null ? !timeEnd.equals(baseTrack.timeEnd) : baseTrack.timeEnd != null) return false;
        if (timeStart != null ? !timeStart.equals(baseTrack.timeStart) : baseTrack.timeStart != null) return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = stationFromId != null ? stationFromId.hashCode() : 0;
        result = 31 * result + (stationToId != null ? stationToId.hashCode() : 0);
        result = 31 * result + (timeStart != null ? timeStart.hashCode() : 0);
        result = 31 * result + (timeEnd != null ? timeEnd.hashCode() : 0);
        result = 31 * result + (slotId != null ? slotId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BaseTrack{");
        sb.append("stationFromId=").append(stationFromId);
        sb.append(", stationToId=").append(stationToId);
        sb.append(", timeStart=").append(timeStart);
        sb.append(", timeEnd=").append(timeEnd);
        sb.append(", slotId=").append(slotId);
        sb.append('}');
        return sb.toString();
    }
}
