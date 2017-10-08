package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Link;

/**
 * User: oracle
 * Date: 03.06.14
 * Перегон для локомотивная бригада
 * используется при парсинге сообщенияЖ
 * +fact_team(id(200000654), fact_time(1349877600),
 * location(track(station(200000130), station(20000162), depart_time(1349877600),locoState(0), loco(11288))), destination(0))
 */
public class BaseTeamTrack {
    Link link;
    public enum State {PASSENGER, AT_WORK, READY, NA, REST};
    private State state = State.NA;
    Long locoId;

    public BaseTeamTrack(Link link, State state, Long locoId) {
        this.link = link;
        this.state = state;
        this.locoId = locoId;
    }

    public BaseTeamTrack() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseTeamTrack that = (BaseTeamTrack) o;

        if (link != null ? !link.equals(that.link) : that.link != null) return false;
        if (locoId != null ? !locoId.equals(that.locoId) : that.locoId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = link != null ? link.hashCode() : 0;
        result = 31 * result + (locoId != null ? locoId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BaseTeamTrack");
        sb.append("{link=").append(link);
        sb.append(", locoState=").append(state);
        sb.append(", locoId=").append(locoId);
        sb.append('}');
        return sb.toString();
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Long getLocoId() {
        return locoId;
    }

    public void setLocoId(Long locoId) {
        this.locoId = locoId;
    }
}
