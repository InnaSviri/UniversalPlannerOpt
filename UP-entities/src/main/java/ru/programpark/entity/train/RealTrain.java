package ru.programpark.entity.train;

import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.loco.LocoTonnage;
import ru.programpark.entity.loco.RealLoco;
import ru.programpark.entity.loco.RealLocoTrack;
import ru.programpark.entity.loco.SeriesPair;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * User: oracle
 * Date: 21.05.14
 * Last edit: 21.07.2014
 * +slot_train(id(123),route([track(station(89), station(90),time_start(1349877600), time_end(1349888400),slot_id(1)),track(station(90), station(91), time_start(1349992000), time_end(1349995600),slot_id(222))]))
 Id – уникальный идентификатор поезда.
 Id1 – идентификатор начальной станции участка.
 Id2 – идентификатор конечной станции участка.
 TimeStart – время отправления со станции Id1 на станцию Id2 в рамках данной нитки (в секундах).
 TimeEnd – время приема на станции Id2 со станции Id1 (в секундах).
 SlotId – идентификатор нитки, к которой относится движение по данному участку.
 В маршруте должны быть перечислены все участки в порядке следования и указаны нитки для каждого участка. Поезд может менять нитки в процессе движения. По одной нитке на разных участках могут ездить разные поезда.
 */
public class RealTrain implements Comparable, Serializable {
    private Long trainId;
    private Long virtualTrainId = -1L;
    private List<RealTrainTrack> route;
    private Long timeOfArrivalAtStationS = -1L;
    private Long timeOfDepartureFromStationS = -1L;
    private Station s = null;
    public enum Tag {START, TRANSIT, END};
    private Tag tag = Tag.TRANSIT;
    List<String> schedule = new ArrayList<>();
    private FactTrain fTrain = null;
    private OneTask oneTask = null;
    private Long lastTimeShift = 0L;

    public RealTrain() {
    }

    public RealTrain(Long trainId, Long virtualTrainId, List<RealTrainTrack> route) {
        this.trainId = trainId;
        this.virtualTrainId = virtualTrainId;
        this.route = route;
    }

    public RealTrain(Long trainId, Long virtualTrainId, List<RealTrainTrack> route, FactTrain fTrain, OneTask oneTask) {
        this.trainId = trainId;
        this.virtualTrainId = virtualTrainId;
        this.route = route;
        this.fTrain = fTrain;
        this.oneTask = oneTask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RealTrain realTrain = (RealTrain) o;

        if (s != null ? !s.equals(realTrain.s) : realTrain.s != null) return false;
        if (route != null ? route.equals(realTrain.route) : realTrain.route != null) return false;
        if (tag != realTrain.tag) return false;
        if (timeOfArrivalAtStationS != null ? !timeOfArrivalAtStationS.equals(realTrain.timeOfArrivalAtStationS) : realTrain.timeOfArrivalAtStationS != null)
            return false;
        if (timeOfDepartureFromStationS != null ? !timeOfDepartureFromStationS.equals(realTrain.timeOfDepartureFromStationS) : realTrain.timeOfDepartureFromStationS != null)
            return false;
        if (trainId != null ? !trainId.equals(realTrain.trainId) : realTrain.trainId != null) return false;
        if (virtualTrainId != null ? !virtualTrainId.equals(realTrain.virtualTrainId) : realTrain.virtualTrainId != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = trainId != null ? trainId.hashCode() : 0;
        result = 31 * result + (virtualTrainId != null ? virtualTrainId.hashCode() : 0);
        result = 31 * result + (route != null ? route.hashCode() : 0);
        result = 31 * result + (timeOfArrivalAtStationS != null ? timeOfArrivalAtStationS.hashCode() : 0);
        result = 31 * result + (timeOfDepartureFromStationS != null ? timeOfDepartureFromStationS.hashCode() : 0);
        result = 31 * result + (s != null ? s.hashCode() : 0);
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        return result;
    }

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public List<RealTrainTrack> getRoute() {
        return route;
    }

    public void setRoute(List<RealTrainTrack> route) {
        this.route = route;
    }

    public void addRouteTrack(RealTrainTrack track) {
        if (route == null) {
            route = new ArrayList<RealTrainTrack>();
        }
        route.add(track);
    }

    public RealTrainTrack getFirstTrack() {
        return (route == null || route.isEmpty()) ? null :
            route.get(0);
    }

    public RealTrainTrack getLastTrack() {
        return (route == null || route.isEmpty()) ? null :
            route.get(route.size() - 1);
    }

    public Long getVirtualTrainId() {
        return virtualTrainId;
    }

    public void setVirtualTrainId(Long virtualTrainId) {
        this.virtualTrainId = virtualTrainId;
    }

    @Override public String toString() {
        String routeDesc = null;
        for (RealTrainTrack track : route) {
            routeDesc = (routeDesc == null) ? "" : (routeDesc + ", ");
            String regions = null;
            routeDesc += "RealTrainTrack{" + track.getLink().getFrom().getId() +
                "@" + track.getTimeStart() +
                " → " + track.getLink().getTo().getId() +
                "@" + track.getTimeEnd() +
                (regions == null ? "" : (", regions " + regions)) + "}";
        }
        return "RealTrain{trainId=" + getTrainId() +
            ", tag=" + getTag() +
            ", route=[" + routeDesc + "]" +
            (lastTimeShift > 0L ? (", lastTimeShift=" + lastTimeShift) : "") +
            "}";
    }

    public Long getDistanceAlongRoute() {
        long dist = 0L;
        for (RealTrainTrack track : route) {
            dist += track.getLink().getDistance();
        }
        return dist;
    }

    public Long getMaxWeightAlongRoute(SeriesPair serp) {
        long maxWeight = 0L;
        for (RealTrainTrack track : route) {
            LocoTonnage tonnage = track.getLink().getLocoTonnage(serp);
            maxWeight = Math.max(maxWeight, tonnage.getMaxWeight());
        }
        return maxWeight;
    }

    public Long getAbsTimeAfterTraversingLink(Link lastLink){
        for (RealTrainTrack track: route){
            if (track.getLink().equals(lastLink))
                return track.getTimeEnd();
        }

        return 0L;
    }

    public boolean passesThroughStation(Station S){
        for (RealTrainTrack track: route){
            if (track.getLink().getFrom().equals(S) || track.getLink().getTo().equals(S)){
                return true;
            }
        }
        return false;
    }

    public Long getTimeOfArrivalAtStationS() {
        return timeOfArrivalAtStationS;
    }

    public void setTimesAtStationS(Station s) {
        int i = 0;
        for (RealTrainTrack track: route){
            if (track.getLink().getTo().equals(s)){
                this.timeOfArrivalAtStationS = track.getTimeEnd();
                if (i == route.size() - 1){
                    tag = Tag.END;
                }
            }
            if (track.getLink().getFrom().equals(s)){
                this.timeOfDepartureFromStationS = track.getTimeStart();
                if (i == 0){
                    tag = Tag.START;
                }
            }
            i++;
        }

    }

    public Station getS() {
        return s;
    }

    public void setS(Station s) {
        this.s = s;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    @Override
    public int compareTo(Object o) { // <0 < object o. >0 > object o
        /*
        if (this.getTag().equals(Tag.START)){
            if (!((RealTrain) o).getTag().equals(Tag.START)){
                return ;
            }
        }
        */

        return ((int) (this.getTimeForSorting() - ((RealTrain)o).getTimeForSorting()));
    }

    public Long getTimeForSorting(){
        Long time;
        if (timeOfDepartureFromStationS != -1L)
            time = timeOfDepartureFromStationS;
        else
            time = timeOfArrivalAtStationS;
        return time;
    }

    public Long getTimeOfDepartureFromStationS() {
        return timeOfDepartureFromStationS;
    }

    public List<String> getSchedule() {
        String str = "_______________TrainId = " + this.getTrainId() + "_________________________";
        schedule.add(str);
        for (RealTrainTrack track: route){
            str = track.getLink().getFrom().getName() + " " + new Time(track.getTimeStart()).getTimeStamp();
            schedule.add(str);
            str = track.getLink().getTo().getName() + " " + new Time(track.getTimeEnd()).getTimeStamp();
            schedule.add(str);
        }

        return schedule;
    }

    public void printSchedule(boolean toFile){
        schedule = getSchedule();
        for (String s: schedule){
            if (toFile)
                LoggingAssistant.getTrainResultsWriter().println(s);
            else
                System.out.println(s);
        }
    }

    public FactTrain getfTrain() {
        return fTrain;
    }

    public void setfTrain(FactTrain fTrain) {
        this.fTrain = fTrain;
    }

    public OneTask getOneTask() {
        return oneTask;
    }

    public void setOneTask(OneTask oneTask) {
        this.oneTask = oneTask;
    }

    public Long getTimeStartAtStation(Station s){
        for (RealTrainTrack track: route){
            if (track.getLink().getFrom().equals(s))
                return track.getTimeStart();
        }

        return null;
    }

    public Long getTimeEndAtStation(Station s){
        for (RealTrainTrack track: route){
            if (track.getLink().getTo().equals(s))
                return track.getTimeEnd();
        }

        return null;
    }

    public boolean stopOnStation(Station s){
        Long t1 = 0L, t2 = 0L;

        for (RealTrainTrack track: route){
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

    public RealTrain croppedTrain(int low, int high) {
        List<RealTrainTrack> croppedRoute = new ArrayList<>();
        for (RealTrainTrack track : route.subList(low, high)) {
            croppedRoute.add(new RealTrainTrack(track));
        }
        return new RealTrain(trainId, virtualTrainId, croppedRoute,
                             null, oneTask);
    }

    public void shiftTimes(long shift, int offset) {
        int i = 0;
        for (RealTrainTrack track : route) {
            if (i++ >= offset) track.shiftTimes(shift);
        }
        lastTimeShift = shift;
    }

    public void shiftTimes(long shift, Link startLink) {
        boolean shifting = false;
        for (RealTrainTrack track : route) {
            if (startLink == null || track.getLink().equals(startLink)) shifting = true;
            if (shifting) track.shiftTimes(shift);
        }
    }

    public void shiftTimes(RealLoco loco, Link link) {
        boolean shifting = false;
        for (RealLocoTrack locoTrack : loco.getRoute()) {
            if (locoTrack.getTrainId().equals(trainId) && locoTrack.getLink().equals(link)) {
                shifting = true;
            }
            if (shifting) {
                for (SlotTrack track : route) {
                    if (track.getLink().equals(locoTrack.getLink())) {
                        track.setTimeStart(locoTrack.getTimeStart());
                        track.setTimeEnd(locoTrack.getTimeEnd());
                    }
                }
            }
        }
    }

    public void shiftTimes(RealTrain croppedCopy) {
        int i = 0;
        lastTimeShift = 0L;
        RealTrainTrack firstOfCopy = croppedCopy.getFirstTrack();
        for (RealTrainTrack track : route) {
            Link link = track.getLink();
            if (link.equals(firstOfCopy.getLink())) {
                lastTimeShift =
                    firstOfCopy.getTimeStart() - track.getTimeStart();
            }
            if (lastTimeShift > 0L) track.shiftTimes(lastTimeShift);
        }
    }

    public void setLastTimeShift(long shift) {
        lastTimeShift = shift;
    }

    public long getLastTimeShift() {
        return lastTimeShift;
    }

    public RealTrainTrack popServiceTrack() {
        RealTrainTrack lastTrack = null;
        if (! route.isEmpty()) {
            lastTrack = getLastTrack();
            Station stFrom = lastTrack.getLink().getFrom();
            Station stTo = lastTrack.getLink().getTo();
            if (stFrom.equals(stTo)) {
                route.remove(route.size() - 1);
            } else {
                lastTrack = null;
            }
        }
        return lastTrack;
    }
}
