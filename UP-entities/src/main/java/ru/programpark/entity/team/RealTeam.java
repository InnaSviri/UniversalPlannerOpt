package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;

import java.util.ArrayList;
import java.util.List;

/**
 * User: oracle
 * Date: 04.06.14
 * выход бригадной части
 * +slot_team(id(123),
             route([track(station(89), station(90), time_start(1349877600), time_end(1349888400),slot_id(123), locoState(0), loco(-1))...)
 */
public class RealTeam implements Comparable {
    Long id;//уникальный идентификатор бригады.
    List<RealTeamTrack> route;
    Station depot;
    FactTeam fTeam = null;
    boolean twoNightsInARow = false;
    List<String> schedule = new ArrayList<>();

    public RealTeam(Long id, FactTeam fTeam) {
        this.id = id;
        this.fTeam = fTeam;
        this.route = new ArrayList<>();
        this.depot = fTeam.getDepot();
    }

    public RealTeam(Long id, List<RealTeamTrack> route, Station depot, FactTeam fTeam) {
        this.id = id;
        this.route = route;
        this.depot = depot;
        this.fTeam = fTeam;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RealTeam realTeam = (RealTeam) o;

        if (id != null ? !id.equals(realTeam.id) : realTeam.id != null) return false;
        if (route != null ? !route.equals(realTeam.route) : realTeam.route != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (route != null ? route.hashCode() : 0);
        return result;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<RealTeamTrack> getRoute() {
        return route;
    }

    public void setRoute(List<RealTeamTrack> route) {
        this.route = route;
    }

    public Long getRouteDuration(){
        if (route.size() == 0) return 0L;
        return route.get(route.size()-1).getTimeEnd() - route.get(0).getTimeStart();
    }

    public Station getDepot() {
        return depot;
    }

    public void setDepot(Station depot) {
        this.depot = depot;
    }

    public boolean isTwoNightsInARow() {
        return twoNightsInARow;
    }

    public void setTwoNightsInARow(boolean twoNightsInARow) {
        this.twoNightsInARow = twoNightsInARow;
    }

    @Override
    public String toString() {
        String str = "";

        str += "tell(slot_team(";
        str += "id(" + id.toString() + "),";
        str += "route([";

        for (RealTeamTrack track : route) {
            str += "track(";
            str += "station(" + track.getLink().getFrom().getId().toString() + "),";
            str += "station(" + track.getLink().getTo().getId().toString() + "),";
            str += "time_start(" + track.getTimeStart().toString() + "),";
            str += "time_end(" + track.getTimeEnd().toString() + "),";
            str += "slot_id(" + (track.getSlotId() == null ? -1 : track.getSlotId())  + "),";
            str += "locoState(" + (track.getState().equals(BaseTeamTrack.State.PASSENGER) ? 0 : 1) + "),";
            str += "loco(" + track.getLocoId() + ")";
            str += "),";
        }
        str = str.substring(0, str.length() - 1);

        str += "])";
        str += "))";

        return str;
    }

    public void insertRoute(List<RealTeamTrack> newRoute){
        boolean insertBeforeExistingRoute = false;

        if (newRoute.get(0).getTimeStart() < route.get(0).getTimeStart())
            insertBeforeExistingRoute = true;

        if (insertBeforeExistingRoute){
            ArrayList<RealTeamTrack> temp = new ArrayList<>(route);
            route = newRoute;

            for (RealTeamTrack rTrack: temp){
                route.add(rTrack);
            }
        } else {
            for (RealTeamTrack rTrack: newRoute){
                route.add(rTrack);
            }
        }
    }

    public Long getRouteTimeEnd() {
        if (route.size() == 0) return 0L;
        return route.get(route.size() - 1).getTimeEnd();
    }

    public Long getRouteTimeStart(){
        return route.get(0).getTimeStart();
    }

    public Long getStartTimeOfRoute() {
        //return route.get(0).getTimeOneDayBack();
        if (route != null && route.size() != 0) {
            return route.get(0).getTimeStart();
        } else {
            return Long.MIN_VALUE;
        }
    }

    public Long getEndTimeOfRoute() {
        if (route.size()!=0)
            return route.get(route.size()-1).getTimeEnd();
        else
            return 0L;
    }

    public Station getDestination() {
        if (route.size()!=0)
            return route.get(route.size()-1).getLink().getTo();
        else
            return null;
    }

    @Override
    public int compareTo(Object o) {
        RealTeam team = (RealTeam) o;
        int res = getStartTimeOfRoute().compareTo(team.getStartTimeOfRoute());
        if (res == 0) {
            res = getId().compareTo(team.getId());
        }
        return res;
    }

    public FactTeam getfTeam() {
        return fTeam;
    }

    public void setfTeam(FactTeam fTeam) {
        this.fTeam = fTeam;
    }

    public void mergeRoute(RealTeam newTeam){
        List<RealTeamTrack> tracks = getRoute();
        tracks.addAll(newTeam.getRoute());
        setRoute(tracks);
    }

    public Long getTeamWorkTime(){
        Long workDuration = 0L;

        for (RealTeamTrack track: getRoute()){
            if (track.getState().equals(BaseTeamTrack.State.AT_WORK)){
                workDuration += (track.getTimeEnd() - track.getTimeStart());
            } else
                break;
        }

        return (workDuration);
    }

    public boolean teamAlreadyHasBeenState(BaseTeamTrack.State state) {
        for (RealTeamTrack track: getRoute()){
                if (track.getState().equals(state)){
                    return true;
                }
            }

        return false;
    }

    public boolean teamAlreadyWorked() {
        return teamAlreadyHasBeenState(BaseTeamTrack.State.AT_WORK);
    }

    public boolean teamAlreadyRested(){
        return teamAlreadyHasBeenState(BaseTeamTrack.State.REST);
    }

    public Long getTeamNextRestTime(Long minRest){//TeamPlanner.params.minRest
        Long t_work = getTeamWorkTime();
        return (t_work/2 > minRest ? t_work/2 : minRest);
    }

    public Long getTeamPrevRestTime(){
        Long tStart = 0L, tEnd = 0L;
        boolean restBegin = false;
        RealTeamTrack trackPrev = null;
        for (RealTeamTrack track: getRoute()){
                if (!restBegin && track.getState().equals(BaseTeamTrack.State.REST)){
                    restBegin = true;
                    tStart = track.getTimeStart();
                    tEnd = track.getTimeEnd();
                }
                if (restBegin && !track.getState().equals(BaseTeamTrack.State.REST)){
                    if (trackPrev != null)
                        tEnd = trackPrev.getTimeEnd();
                    else
                        tEnd = track.getTimeStart();
                }
                trackPrev = track;
        }

        return (tEnd - tStart);
    }

    public List<String> getSchedule() {
        String str = "_____________ TeamId = " + this.getId() + "______________________";
        schedule.add(str);
        str = "Время заставки бригады : " + new Time(fTeam.getTimeOfFact()).getTimeStamp();
        schedule.add(str);
        for (RealTeamTrack track: route){
            str = track.getLink().getFrom().getName() + " " + new Time(track.getTimeStart()).getTimeStamp() + " " + track.getState() + " locoId:" + track.getLocoId() + " slotId: " + track.slotId;
            schedule.add(str);
            str = track.getLink().getTo().getName() + " " + new Time(track.getTimeEnd()).getTimeStamp() + " " + track.getState() + " locoId:" + track.getLocoId() + " slotId: " + track.slotId;
            schedule.add(str);
        }
        str = "Депо бригады: " + fTeam.getDepot().getName();
        schedule.add(str);
        str = "Длительность маршрута: " + getRouteDuration()/3600.0 + " ч";
        schedule.add(str);
        str = "Начальное врмея до отдыха: " + fTeam.getTimeUntilRest()/3600.0 + " ч";
        schedule.add(str);

        return schedule;
    }

    public void printSchedule(boolean toFile){
        schedule = getSchedule();
        for (String s: schedule){
            if (toFile)
                LoggingAssistant.getTeamResultsWriter().println(s);
            else
                System.out.println(s);
        }
    }

    public boolean PartialRouteAsPassenger(Station from, Station to){
        boolean routeStarted = false;
        for (RealTeamTrack track: route){
            if (routeStarted){
                if (!track.getState().equals(BaseTeamTrack.State.PASSENGER)){
                    return false;
                } else if (track.getLink().getTo().equals(to)){
                    return true;
                }
            }
            if (track.getLink().getFrom().equals(from) && track.getState().equals(BaseTeamTrack.State.PASSENGER)){
                routeStarted = true;
            }
        }

        return false;
    }


    public boolean passesStation(Long stId){
        for (RealTeamTrack track:route){
            if (track.getLink().getFrom().getId().equals(stId))
                return true;
        }

        return false;
    }
}
