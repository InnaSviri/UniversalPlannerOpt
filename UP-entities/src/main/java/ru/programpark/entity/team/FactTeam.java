package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Station;

import java.util.ArrayList;
import java.util.List;

/**
 * User: oracle
 * Date: 03.06.14
 *
 * +fact_team(id(20000654), fact_time(1349877600), location(station(200000130)))
 TeamId – идентификатор бригады.
 FactTime – время, на которое актуален данный факт.
 LocStId – идентификатор станции, на которой в данный момент находится бригада.

 */
public class FactTeam implements Comparable<FactTeam>, Cloneable, TeamAttributes {
    //____comes from +fact_team_____
    private Long id;
    private boolean isDuplicateFactTeam = false; // было ли дублирование
    private boolean isSetFactTeam = false; // получено ли сообщение
    private Long timeOfFact;
    private Station station; //where team is located, either station is null
    private FactTeamTrack track;//or track is null
    // ___ из сообщения о нахождении бригады на станции с локомотивом
    private FactTeamArrive teamArrive;
    private Long locoId = -2L;//заполнен, если пришел факт о нахождении бригады на перегоне
    //_____comes from +team_attributes____
    private boolean isDuplicateTeamAttr = false; // было ли дублирование
    private boolean isSetTeamAttr = false; // получено ли сообщение
    private List<TeamRegion> teamWorkRegions = new ArrayList<>();
    private Station depot;
    private List <Long> locoSeries = new ArrayList<>(); // ? Long
    private List<Long> weightTypes = new ArrayList<>();
    private boolean allowedToWorkOnLongTrain = false;
    private boolean allowedToWorkOnHeavyTrain = false;
    private boolean isFake = false;
    //_____comes from +fact_team_next_rest____
    private boolean isDuplicateNextRest = false; // было ли дублирование
    private boolean isSetTimeUntilRest = false; // получено ли сообщение
    private Long restFactTime;     // время, на которое актуален данный факт.
    private Long timeUntilRest = -2L; // время до следующего отдыха (оставшееся рабочее время), в секундах.
    private Long timeOfPresence;//время явки бригады


    @Override
    public int compareTo(FactTeam o) {
        if (this.timeOfFact > o.getTimeOfFact()) // this > o
            return 1;
        if (this.timeOfFact < o.getTimeOfFact()) // this < o
            return -1;
        if (this.timeOfFact == o.getTimeOfFact()) // this >== o
            return 0;
        return 0;
    }

    //public enum TeamState {READY, REST, RETURN};
    //private TeamState state;

    public FactTeam() {
    }

    public FactTeam(Long id, Long timeOfFact, FactTeamTrack track) {
        this.id = id;
        this.timeOfFact = timeOfFact;
        this.track = track;
        //this.state = TeamState.READY;
        this.station = null;

        if (track != null){
            this.locoId = track.getLocoId();
        }
    }

    public FactTeam(Long id, Long timeOfFact, Station station, FactTeamArrive factTeamArrive) {
        this.id = id;
        this.timeOfFact = timeOfFact;
        this.station = station;
        this.teamArrive = factTeamArrive;
        this.track = null;
    }

    public Long getAbsTimeToRetreatToRest(){
        return restFactTime + timeUntilRest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FactTeam factTeam = (FactTeam) o;

        if (id != null ? !id.equals(factTeam.id) : factTeam.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTimeOfFact() {
        return timeOfFact;
    }

    public void setTimeOfFact(Long timeOfFact) {
        this.timeOfFact = timeOfFact;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public FactTeamTrack getTrack() {
        return track;
    }

    public void setTrack(FactTeamTrack track) {
        this.track = track;
    }

    public void markFactTeamSet() {
        if(isSetFactTeam)
            isDuplicateFactTeam = true;
        else
            isSetFactTeam = true;
    }

    public int checkCorrectFactTeam() {
        if (isDuplicateFactTeam) return -1;
        if (!isSetFactTeam) return -2;
        return 0;
    }

    public List<TeamRegion> getTeamWorkRegions() {
        return teamWorkRegions;
    }

    public void setTeamWorkRegions(ArrayList<TeamRegion> teamWorkRegions) {
        this.teamWorkRegions = teamWorkRegions;
    }

    public Station getDepot() {
        return depot;
    }

    public void setDepot(Station depot) {
        this.depot = depot;
    }

    public void markTeamAttrSet() {
        if(isSetTeamAttr)
            isDuplicateTeamAttr = true;
        else
            isSetTeamAttr = true;
    }

    public int checkCorrectTeamAttr() {
        if (isDuplicateTeamAttr) return -1;
        if (!isSetTeamAttr) return -2;
        return 0;
    }


    public void setLocoSeries(ArrayList<Long> locoSeries) {
        this.locoSeries = locoSeries;
    }

    public List<Long> getWeightTypes() {
        return weightTypes;
    }

    public void setWeightTypes(ArrayList<Long> weightTypes) {
        this.weightTypes = weightTypes;
    }

    public boolean isAllowedToWorkOnLongTrain() {
        return allowedToWorkOnLongTrain;
    }

    public void setAllowedToWorkOnLongTrain(boolean allowedToWorkOnLongTrain) {
        this.allowedToWorkOnLongTrain = allowedToWorkOnLongTrain;
    }

    public boolean isAllowedToWorkOnHeavyTrain() {
        return allowedToWorkOnHeavyTrain;
    }

    public void setAllowedToWorkOnHeavyTrain(boolean allowedToWorkOnHeavyTrain) {
        this.allowedToWorkOnHeavyTrain = allowedToWorkOnHeavyTrain;
    }

    /*public TeamState getState() {
        return state;
    }

    public void setState(TeamState state) {
        this.state = state;
    }

    public void initializeTeamState() {
        if (!depot.equals(null) && depot.equals(station))
            state = TeamState.READY;
        else
            state = TeamState.RETURN;
    } */

    public boolean isFake() {
		return isFake;
	}

	public void setFake(boolean isFake) {
		this.isFake = isFake;
	}

	public Long getRestFactTime() {
        return restFactTime;
    }

    public void setRestFactTime(Long restFactTime) {
        this.restFactTime = restFactTime;
    }

    public Long getTimeUntilRest() {
        return timeUntilRest;
    }

    public void setTimeUntilRest(Long timeUntilRest) {
        this.timeUntilRest = timeUntilRest;
    }

    public void markTimeUntilRestSet() {
        if(isSetTimeUntilRest)
            isDuplicateNextRest = true;
        else
            isSetTimeUntilRest = true;
    }

    public int checkCorrectNextRest() {
        if (isDuplicateNextRest) return -1;
        if (!isSetTimeUntilRest) return -2;
        return 0;
    }

    public List<Long> getLocoSeries() {
        return locoSeries;
    }

    public Long getLocoId() {
        return locoId;
    }

    public void setLocoId(Long locoId) {
        this.locoId = locoId;
    }

    public FactTeamArrive getTeamArrive() {
        return teamArrive;
    }

    public void setTeamArrive(FactTeamArrive teamArrive) {
        this.teamArrive = teamArrive;
        this.locoId = teamArrive.getId();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FactTeam");
        sb.append("{id=").append(id);
        sb.append(", timeOfFact=").append(timeOfFact);
        sb.append(", station=").append(station);
        sb.append(", track=").append(track);
        sb.append(", teamArrive=").append(teamArrive);
        sb.append(", locoId=").append(locoId);
        sb.append(", teamWorkRegions=").append(teamWorkRegions);
        sb.append(", depot=").append(depot);
        sb.append(", locoSeries=").append(locoSeries);
        sb.append(", weightTypes=").append(weightTypes);
        sb.append(", allowedToWorkOnLongTrain=").append(allowedToWorkOnLongTrain);
        sb.append(", allowedToWorkOnHeavyTrain=").append(allowedToWorkOnHeavyTrain);
        sb.append(", restFactTime=").append(restFactTime);
        sb.append(", timeUntilRest=").append(timeUntilRest);
        //sb.append(", locoState=").append(state);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Object clone() {
        FactTeam copy = new FactTeam();
        copy.id = this.id;
        copy.timeOfFact = this.timeOfFact;
        copy.station = this.station; 
        copy.track = this.track;
        copy.teamArrive = this.teamArrive;
        copy.locoId = this.locoId;
        copy.teamWorkRegions = this.teamWorkRegions;
        copy.depot = this.depot;
        copy.locoSeries = this.locoSeries; 
        copy.weightTypes = this.weightTypes;
        copy.allowedToWorkOnLongTrain = this.allowedToWorkOnLongTrain;
        copy.allowedToWorkOnHeavyTrain = this.allowedToWorkOnHeavyTrain;
        copy.restFactTime = this.restFactTime;     
        copy.timeUntilRest = this.timeUntilRest; 
        copy.timeOfPresence = this.timeOfPresence;
        return (Object) copy;
    }

    public void addLocoSeries(Long series){
        this.locoSeries.add(series);
    }

    public void addTeamWorkRegion(TeamRegion twr){
        this.teamWorkRegions.add(twr);
    }

    public void addWeightType(Long wt){
        this.weightTypes.add(wt);
    }

    public Long getTimeOfPresence() {
        return timeOfPresence;
    }

    public void setTimeOfPresence(Long timeOfPresence) {
        this.timeOfPresence = timeOfPresence;
    }

    public void setTeamWorkRegions(List<TeamRegion> teamWorkRegions) {
        this.teamWorkRegions = teamWorkRegions;
    }

    public void setLocoSeries(List<Long> locoSeries) {
        this.locoSeries = locoSeries;
    }

    public void setWeightTypes(List<Long> weightTypes) {
        this.weightTypes = weightTypes;
    }

    public List<TeamRegion> findTeamRegion(){
        List<TeamRegion> result = new ArrayList<>();
        if (station != null) {
            for (TeamRegion region: teamWorkRegions){
                if (region.containsStation(station)){
                    result.add(region);
                }
            }
        }
        if (track!=null) {
            for (TeamRegion region: teamWorkRegions){
                if (region.containsLink(track.getLink())){
                    result.add(region);
                }
            }
        }
        return result;
    }

    public String checkForExclusion() {
        if (getTeamWorkRegions() == null || getTeamWorkRegions().isEmpty()) {
            return "не определены участки обкатки";
        }/* else if (getLocoSeries() == null || getLocoSeries().isEmpty()) {
            return "не определены серии локомотивов";
        }*/ /* else if (getTimeUntilRest().equals(0L)) {
            return "объявлено нулевое время до отдыха";
        } */ else {
            return null;
        }
    }

}
