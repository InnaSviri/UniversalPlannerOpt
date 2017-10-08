package ru.programpark.entity.team;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 13.04.15
 * Time: 14:00
 * To change this template use File | Settings | File Templates.
 */
public class FactTeamArrive {
    private Long id;
    private Long time;
    private BaseTeamTrack.State teamState;

    public FactTeamArrive() {
    }

    public FactTeamArrive(Long id, Long time, BaseTeamTrack.State teamState) {
        this.id = id;
        this.time = time;
        this.teamState = teamState;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public BaseTeamTrack.State getTeamState() {
        return teamState;
    }

    public void setTeamState(BaseTeamTrack.State teamState) {
        this.teamState = teamState;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FactTeamArrive");
        sb.append("{id=").append(id);
        sb.append(", time=").append(time);
        sb.append(", teamState=").append(teamState);
        sb.append('}');
        return sb.toString();
    }
}
