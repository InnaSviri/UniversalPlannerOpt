package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Link;
import java.io.Serializable;

/**
 * User: oracle
 * Date: 03.07.15
 * Задание на пересылку локомотивов
 */
public class LocoRelocation implements Serializable {
    private Link linkFrom;
    private Link linkTo;
    private Long time;
    private Long interval;
    private Integer number;

    public LocoRelocation(Link linkFrom, Link linkTo,
                          Long time, Long interval, Integer number) {
        this.linkFrom = linkFrom;
        this.linkTo = linkTo;
        this.time = time;
        this.interval = interval;
        this.number = number;
    }

    public LocoRelocation() {
    }

    public Link getLinkFrom() {
        return linkFrom;
    }

    public void setLinkFrom(Link linkFrom) {
        this.linkFrom = linkFrom;
    }

    public Link getLinkTo() {
        return linkTo;
    }

    public void setLinkTo(Link linkTo) {
        this.linkTo = linkTo;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocoRelocation that = (LocoRelocation) o;

        if (linkFrom != null ? !linkFrom.equals(that.linkFrom) : that.linkFrom != null) return false;
        if (linkTo != null ? !linkTo.equals(that.linkTo) : that.linkTo != null) return false;
        if (interval != null ? !interval.equals(that.interval) : that.interval != null) return false;
        if (number != null ? !number.equals(that.number) : that.number != null) return false;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = time != null ? time.hashCode() : 0;
        result = 31 * result + (linkFrom != null ? linkFrom.hashCode() : 0);
        result = 31 * result + (linkTo != null ? linkTo.hashCode() : 0);
        result = 31 * result + (interval != null ? interval.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LocoRelocation");
        sb.append("{linkFrom=").append(linkFrom);
        sb.append(", linkTo=").append(linkTo);
        sb.append(", time=").append(time);
        sb.append(", interval=").append(interval);
        sb.append(", number=").append(number);
        sb.append('}');
        return sb.toString();
    }
}
