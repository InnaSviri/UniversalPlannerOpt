package ru.programpark.entity.fixed;

import java.io.Serializable;

/**
 * User: oracle
 * Date: 20.05.14
 */
public class StationPair implements Serializable {
    public Long stationFromId;
    public Long stationToId;

    public StationPair(Long stationFrom, Long stationTo) {
        this.stationFromId = stationFrom;
        this.stationToId = stationTo;
    }

    public StationPair(Link link) {
        this.stationFromId = link.getFrom().getId();
        this.stationToId = link.getTo().getId();
    }

    public interface Specifiable {
        Station getFrom();
        Station getTo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationPair that = (StationPair) o;

        if (stationFromId != null ? !stationFromId.equals(that.stationFromId) : that.stationFromId != null) return false;
        if (stationToId != null ? !stationToId.equals(that.stationToId) : that.stationToId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = stationFromId != null ? stationFromId.hashCode() : 0;
        result = 31 * result + (stationToId != null ? stationToId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("StationPair");
        sb.append("{stationFromId=").append(stationFromId);
        sb.append(", stationToId=").append(stationToId);
        sb.append('}');
        return sb.toString();
    }

    @Deprecated
    public Long getStationFromId() {
        return stationFromId;
    }

    @Deprecated
    public void setStationFromId(Long stationFromId) {
        this.stationFromId = stationFromId;
    }

    @Deprecated
    public Long getStationToId() {
        return stationToId;
    }

    @Deprecated
    public void setStationToId(Long stationToId) {
        this.stationToId = stationToId;
    }

    public static StationPair specifier (Specifiable o) {
        return new StationPair(o.getFrom().getId(), o.getTo().getId());
    }

    public boolean containsStation(Station s){
        if (stationToId.equals(s.getId()) || stationFromId.equals(s.getId()))
            return true;
        return false;
    }
}
