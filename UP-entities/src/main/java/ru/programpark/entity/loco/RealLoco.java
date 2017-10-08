package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;

import java.util.ArrayList;
import java.util.List;

/**
 * User: oracle
 * Date: 10.06.14
 выходные данные по локомотивам
 +slot_loco(id(LocoId),
 route([track(station(Id1), station(Id2),time_start(TimeStart),time_end(TimeEnd),slot_id(SlotId),locoState(State), train(TrainId)),…]))
 */
public class RealLoco implements Comparable{
    Long realLocoId;
    List<RealLocoTrack> route;
    FactLoco fLoco = null;
    List<String> schedule = new ArrayList<>();

    public RealLoco() {
    }

    public RealLoco(Long locoId, List<RealLocoTrack> route, FactLoco fLoco) {
        this.realLocoId = locoId;
        this.route = route;
        this.fLoco = fLoco;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RealLoco realLoco = (RealLoco) o;

        if (realLocoId != null ? !realLocoId.equals(realLoco.realLocoId) : realLoco.realLocoId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return realLocoId != null ? realLocoId.hashCode() : 0;
    }

    public Long getRealLocoId() {
        return realLocoId;
    }

    public void setRealLocoId(Long realLocoId) {
        this.realLocoId = realLocoId;
    }

    public List<RealLocoTrack> getRoute() {
        return route;
    }

    public void setRoute(List<RealLocoTrack> route) {
        this.route = route;
    }

    @Override
    public String toString() {
        String str  = "";

        str += "tell(slot_loco(";
        str += "id(" + realLocoId + "),";
        str += "route([";
        for (RealLocoTrack track : route) {
            str += "track(";
            str += "station(" + track.getLink().getFrom().getId().toString() + "),";
            str += "station(" + track.getLink().getTo().getId().toString() + "),";
            str += "time_start(" + track.getTimeStart().toString() + "),";
            str += "time_end(" + track.getTimeEnd().toString() + "),";
            str += "slot_id(" + (track.getSlotId() == null ? -1 : track.getSlotId()) + "),";
            str += "locoState(" + (track.getState().equals(BaseLocoTrack.State.RESERVE) ? 0 : 1) + "),";
            str += "train(" + (track.getTrainId() == -2L ? "-1" : track.getTrainId().toString() ) + ")";
            str += "),";
        }

        str = str.substring(0, str.length() - 1);
        str += "])";
        str  += "))";

        return str;
    }

    public Long getRouteDuration(){
        if (route.size() != 0)
            return route.get(route.size()-1).getTimeEnd() - route.get(0).getTimeStart();
        else
            return 0L;
    }

    public Long getTotalRouteDuration(){
        Long duration = 0L;
        for (RealLocoTrack track: route){
            duration += track.getLink().getDefaultDuration();
        }
        return duration;
    }

    public Long getTotalRouteDistance(){
        Long distance = 0L;
        for (RealLocoTrack track: route){
            distance += track.getLink().getDistance();
        }
        return distance;
    }

    public boolean passesThroughStation(Station S){
        for (RealLocoTrack track: route){
            if (track.getLink().getFrom().equals(S)){
                return true;
            }
        }
        return false;
    }

    public Long getStartTimeOfRoute() {
        return route.get(0).getTimeStart();
    }

    public Long getEndTimeOfRoute() {
        return route.get(route.size()-1).getTimeEnd();
    }

    @Override
    public int compareTo(Object o) {
        if ((getStartTimeOfRoute() - ((RealLoco) o).getStartTimeOfRoute()) > 0)
            return 1;
        else if (( getStartTimeOfRoute() - ((RealLoco) o).getStartTimeOfRoute()) < 0)
            return -1;
        else {
            if ((realLocoId - ((RealLoco) o).getRealLocoId()) > 0)
                return 1;
            else if ((realLocoId - ((RealLoco) o).getRealLocoId()) < 0)
                return -1;
            else
                return 0;
        }
    }

    public FactLoco getfLoco() {
        return fLoco;
    }

    public void setfLoco(FactLoco fLoco) {
        this.fLoco = fLoco;
    }

    public List<String> getSchedule() {
        String str = "_______________LocoId = " + this.getRealLocoId() + "_________________________";
        schedule.add(str);
        for (RealLocoTrack track: route){
            str = track.getLink().getFrom().getName() + " " + new Time(track.getTimeStart()).getTimeStamp() + " " + track.getState() + " trainId: " + track.getTrainId();
            schedule.add(str);
            str = track.getLink().getTo().getName() + " " + new Time(track.getTimeEnd()).getTimeStamp() + " " +  track.getState() + " trainId: " + track.getTrainId();
            schedule.add(str);
        }
        if (fLoco != null){
            str = "Время локофакта: " + new Time(fLoco.getTimeOfLocoFact()).getTimeStamp();
            schedule.add(str);
        }

        return schedule;
    }

    public void printSchedule(boolean toFile){
        schedule = getSchedule();
        for (String s: schedule){
            if (toFile)
                LoggingAssistant.getLocoResultsWriter().println(s);
            else
                System.out.println(s);
        }
    }

    public Long getTimeStartAtStation(Station s){
        for (RealLocoTrack track: route){
            if (track.getLink().getFrom().equals(s))
                return track.getTimeStart();
        }

        return null;
    }

    public Long getTimeEndAtStation(Station s){
        for (RealLocoTrack track: route){
            if (track.getLink().getTo().equals(s))
                return track.getTimeEnd();
        }

        return null;
    }

    public boolean stopOnStation(Station s){
        Long t1 = 0L, t2 = 0L;

        for (RealLocoTrack track: route){
            if (track.getLink().getTo().equals(s)){
                t1 = track.getTimeEnd();
            }
            if (track.getLink().getFrom().equals(s)){
                t2 = track.getTimeStart();
                break;
            }
        }

        if (t2 - t1 > 0)
            return true;
        else
            return false;
    }
}
