package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
team_work_region
team_service_region
 */


public class TeamRegion {
    private Long id;
    private ArrayList<StationPair> stationPairs = new ArrayList<>();
    private Long WorkTimeWithRest;
    private Long WorkTimeWithoutRest;
    private Map<Station, Integer> percentByDepot = new HashMap<>();

    public TeamRegion() {
    }

    public TeamRegion(Long id) {
        this.id = id;
    }

    public TeamRegion(Long id, ArrayList<StationPair> stationPairs, Long workTimeWithRest, Long workTimeWithoutRest) {
        this.id = id;
        this.stationPairs = stationPairs;
        WorkTimeWithRest = workTimeWithRest;
        WorkTimeWithoutRest = workTimeWithoutRest;
    }

    public TeamRegion(Long id, ArrayList<StationPair> stationPairs, Long workTimeWithRest, Long workTimeWithoutRest, Map<Station, Integer> percentByDepot) {
        this.id = id;
        this.stationPairs = stationPairs;
        WorkTimeWithRest = workTimeWithRest;
        WorkTimeWithoutRest = workTimeWithoutRest;
        this.percentByDepot = percentByDepot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ArrayList<StationPair> getStationPairs() {
        return stationPairs;
    }

    public void setStationPairs(ArrayList<StationPair> stationPairs) {
        this.stationPairs = stationPairs;
    }

    public Long getWorkTimeWithRest() {
        return WorkTimeWithRest;
    }

    public void setWorkTimeWithRest(Long workTimeWithRest) {
        WorkTimeWithRest = workTimeWithRest;
    }

    public Long getWorkTimeWithoutRest() {
        return WorkTimeWithoutRest;
    }

    public void setWorkTimeWithoutRest(Long workTimeWithoutRest) {
        WorkTimeWithoutRest = workTimeWithoutRest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TeamRegion that = (TeamRegion) o;

        if (WorkTimeWithRest != null ? !WorkTimeWithRest.equals(that.WorkTimeWithRest) : that.WorkTimeWithRest != null)
            return false;
        if (WorkTimeWithoutRest != null ? !WorkTimeWithoutRest.equals(that.WorkTimeWithoutRest) : that.WorkTimeWithoutRest != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (stationPairs != null ? !stationPairs.equals(that.stationPairs) : that.stationPairs != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (stationPairs != null ? stationPairs.hashCode() : 0);
        result = 31 * result + (WorkTimeWithRest != null ? WorkTimeWithRest.hashCode() : 0);
        result = 31 * result + (WorkTimeWithoutRest != null ? WorkTimeWithoutRest.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TeamRegion");
        sb.append("{id=").append(id);
        sb.append(", seriesPairs=").append(stationPairs);
        sb.append(", WorkTimeWithRest=").append(WorkTimeWithRest);
        sb.append(", WorkTimeWithoutRest=").append(WorkTimeWithoutRest);
        sb.append('}');
        return sb.toString();
    }

    public void addStationPair(StationPair sp){
        this.stationPairs.add(sp);
    }

    public Map<Station, Integer> getPercentByDepot() {
        return percentByDepot;
    }

    public void setPercentByDepot(Map<Station, Integer> percentByDepot) {
        this.percentByDepot = percentByDepot;
    }

    public void addDepotPercent(Station depot, Integer percent){
        this.percentByDepot.put(depot, percent);
    }

    public boolean containsStation(Station s){
        for (StationPair pair: stationPairs){
            if (pair.stationFromId.equals(s.getId()) || pair.stationToId.equals(s.getId())){
                return true;
            }
        }
        return false;
    }

    public boolean containsLink(Link link){
        Station s1 = link.getFrom();
        Station s2 = link.getTo();
        for (StationPair pair: stationPairs){
            if (pair.stationFromId.equals(s1.getId()) && pair.stationToId.equals(s2.getId())){
                return true;
            }
        }

        return false;
    }
}
