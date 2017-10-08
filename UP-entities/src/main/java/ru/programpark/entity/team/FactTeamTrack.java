package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Link;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 03.06.14
 * Time: 15:32
 * To change this template use File | Settings | File Templates.
 */
public class FactTeamTrack extends BaseTeamTrack {
    Long departTime;
    //locoId is realLocoID

    public FactTeamTrack(Link link, State state, Long locoId, Long departTime) {
        super(link, state, locoId);
        this.departTime = departTime;
    }

    public FactTeamTrack() {
    }

    public Long getDepartTime() {
        return departTime;
    }

    public void setDepartTime(Long departTime) {
        this.departTime = departTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FactTeamTrack");
        sb.append("{departTime=").append(departTime);
        sb.append('}');
        return sb.toString();
    }
}
