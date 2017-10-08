package ru.programpark.entity.fixed;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Capacity implements Serializable {
    private Long startTime;
    private Integer duration;
    private Integer capacity;
    private Station stationFrom;
    private Station stationTo;
    public Set<Long> trainIds = new HashSet<>();
    public String slots = "";

    public Capacity() {
    }

    public Capacity(Long startTime, Integer duration, Integer capacity, Station stationFrom, Station stationTo) {
        this.startTime = startTime;
        this.duration = duration;
        this.capacity = capacity;
        this.stationFrom = stationFrom;
        this.stationTo = stationTo;
    }

    public Capacity(Capacity sourceCapacity) {
        this.startTime = sourceCapacity.getStartTime();
        this.duration = sourceCapacity.getDuration();
        this.capacity = sourceCapacity.getCapacity();
        this.stationFrom = sourceCapacity.getStationFrom();
        this.stationTo = sourceCapacity.getStationTo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Capacity capacity1 = (Capacity) o;

        if (capacity != null ? !capacity.equals(capacity1.capacity) : capacity1.capacity != null) return false;
        if (duration != null ? !duration.equals(capacity1.duration) : capacity1.duration != null) return false;
        if (startTime != null ? !startTime.equals(capacity1.startTime) : capacity1.startTime != null) return false;
        if (stationFrom != null ? !stationFrom.equals(capacity1.stationFrom) : capacity1.stationFrom != null)
            return false;
        if (stationTo != null ? !stationTo.equals(capacity1.stationTo) : capacity1.stationTo != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startTime != null ? startTime.hashCode() : 0;
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        result = 31 * result + (capacity != null ? capacity.hashCode() : 0);
        result = 31 * result + (stationFrom != null ? stationFrom.hashCode() : 0);
        result = 31 * result + (stationTo != null ? stationTo.hashCode() : 0);
        return result;
    }

    public Station getStationFrom() {
        return stationFrom;
    }

    public void setStationFrom(Station stationFrom) {
        this.stationFrom = stationFrom;
    }

    public Station getStationTo() {
        return stationTo;
    }

    public void setStationTo(Station stationTo) {
        this.stationTo = stationTo;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM HH:mm");
        final StringBuilder sb = new StringBuilder();
        sb.append(formatter.format(new Date(startTime * 1000L)));
        sb.append(" : ").append(capacity);
        return sb.toString();
    }
}
