package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Station;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 29.12.14
 *
 */
public class FactTeamsSummaryAtStation {
    private Station S;
    private List<FactTeam> teamsWithoutLoco = new ArrayList<>();
    private List<FactTeam> teamsWithLoco = new ArrayList<>();
    private List<FactTeam> teamsArriving = new ArrayList<>();

    public FactTeamsSummaryAtStation(Station S, Map<Long, FactTeam> factTeams){
        this.S = S;
        getFactTeamsAtStation(S, factTeams);
    }

    public void getFactTeamsAtStation(Station station, Map<Long, FactTeam> factTeams) {
        for (FactTeam t: factTeams.values()){
            if ((t.getStation() != null && t.getStation().equals(station))){
                if (t.getTeamArrive() == null) {
                    teamsWithoutLoco.add(t);
                } else {
                    teamsWithLoco.add(t);
                }
            }
            if (t.getTrack() != null && t.getTrack().getLink().getTo().equals(station)){
                teamsArriving.add(t);
            }
        }
    }

    public Station getS() {
        return S;
    }

    public List<FactTeam> getTeamsWithoutLoco() {
        return teamsWithoutLoco;
    }

    public List<FactTeam> getTeamsWithLoco() {
        return teamsWithLoco;
    }

    public List<FactTeam> getTeamsArriving() {
        return teamsArriving;
    }
}
