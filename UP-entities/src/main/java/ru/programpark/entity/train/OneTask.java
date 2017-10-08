package ru.programpark.entity.train;

import java.io.Serializable;
import java.lang.Exception;import java.lang.Integer;import java.lang.Long;import java.lang.Override;import java.lang.String;import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: oracle
 * Date: 10.07.14
 *
 */
public class OneTask implements Serializable {
    Long Id;   //либо taskId#, либо номер реального поезда
    Long realId = -1L; //будет проставлен только у тех oneTask, что сформированы из fact/train
    Long startTime;
    Long duration;
    List<List<OneTaskTrack>> routes;
    List<OneTaskTrack> preRoute = null;//уже привязанный кусок маршрута, который после подвязки нитки необходимо перед возвращением результата прикрепить в начало перед routes
    Long weight;
    public static enum OneTaskType {FACT_READY, FACT_ARRIVE, FACT_DEPART, TASK};
    OneTaskType type;
    Integer routeIndex = 0;

    public OneTask(Long id, Long startTime, Long duration, List<List<OneTaskTrack>> routes, Long weight, OneTaskType type) {
        Id = id;
        this.startTime = startTime;
        this.duration = duration;
        this.routes = routes;
        this.weight = weight;
        this.type = type;
    }

    public List<OneTaskTrack> getMainRoute() {
        if (routes == null || routes.size() <= routeIndex || routes.size() == 0 || routes.get(0) == null)
            return null;
        else
            return routes.get(routeIndex);
    }

    public List<OneTaskTrack> getFinalMainRoute(){
        List<OneTaskTrack> res = new ArrayList<>();

        if (preRoute.size() > 0){
            for (OneTaskTrack track: preRoute){
                res.add(track);
            }
        }

        for (OneTaskTrack track: getMainRoute()){
            res.add(track);
        }

        return res;
    }

    public boolean checkRoutes(){
        if (routes == null || routes.size() == 0 || getMainRoute() == null)//случается если в маршруте только одна станция, например, такие факты игнорируем
            return false;
        return true;
    }

    public boolean checkOneTask(){
        if (this.getMainRoute() == null || this.getMainRoute().get(0) == null || this.getMainRoute().size()==0)
            return false;
        else
            return true;
    }

    public boolean isAssigned(){
        try{
            if (getMainRoute().get(0).getSlotId() == null ||
                    getMainRoute().get(0).getSlotId() == -1L ||
                    getMainRoute().get(0).getSlotId() == Long.MIN_VALUE) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public Integer getRouteIndex() {
        return routeIndex;
    }

    public void setRouteIndex(Integer routeIndex) {
        this.routeIndex = routeIndex;
    }

    public List<OneTaskTrack> getPreRoute() {
        return preRoute;
    }

    public void setPreRoute(List<OneTaskTrack> preRoute) {
        this.preRoute = preRoute;
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public List<List<OneTaskTrack>> getRoutes() {
        return routes;
    }

    public void setRoutes(List<List<OneTaskTrack>> routes) {
        this.routes = routes;
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    public OneTaskType getType() {
        return type;
    }

    public void setType(OneTaskType type) {
        this.type = type;
    }

    public Long getRealId() {
        return realId;
    }

    public void setRealId(Long realId) {
        this.realId = realId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OneTask oneTask = (OneTask) o;

        if (Id != null ? !Id.equals(oneTask.Id) : oneTask.Id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Id != null ? Id.hashCode() : 0;
    }

    public String toString() {
        String ret = "OneTask{Id=" + Id +
            ((realId >= 0L) ? (", realId=" + realId) : "") +
            ", startTime=" + startTime + ", duration=" + duration +
            ", weight=" + weight +
            ", preRoute=" + routeToString(preRoute);
        int i = 0;
        for (List<OneTaskTrack> route : routes) {
            ret += ", routes[" + (i++) + "]=" + routeToString(route);
        }
        ret += "}";
        return ret;
    }

    public String trackTimeToString(Long time, String prefix) {
        return (time >= 0) ? (prefix + time) : "";
    }

    public String routeToString(List<OneTaskTrack> route) {
        if (route == null) return "null";
        String ret = null;
        Long prevTimeEnd = null;
        for (OneTaskTrack track : route) {
            if (ret == null) {
                ret = "(" + track.getLink().getFrom().getId() +
                    trackTimeToString(track.getTimeStart(), "@");
            } else if (! track.getTimeStart().equals(prevTimeEnd)) {
                ret += trackTimeToString(track.getTimeStart(),
                                         ((prevTimeEnd >= 0L) ? "—" : ""));
            }
            ret += " → " + track.getLink().getTo().getId() +
                trackTimeToString((prevTimeEnd = track.getTimeEnd()), "@");
        }
        ret += ")";
        return ret;
    }

    public String toTellString() {
        String str = "";
        str = "tell(slot_train(id(" + Id+"),";
        str += "route([";
        if (preRoute!=null){
            for (OneTaskTrack taskTrack : preRoute) {
                if (taskTrack.getSlotId() != null && taskTrack.getSlotId().longValue() != -1L && !taskTrack.getSlotId().equals(Long.MIN_VALUE)) {
                    str += "track(";
                    str += "station(" + taskTrack.getLink().getFrom().getId() + "),";
                    str += "station(" + taskTrack.getLink().getTo().getId() + "),";
                    str += "time_start(" + taskTrack.getTimeStart() + "),";
                    str += "time_end(" + taskTrack.getTimeEnd() + "),";
                    str += "slot_id(" + (taskTrack.getSlotId() == -2L ? -1L : taskTrack.getSlotId()) + ")";
                    str += "),";
                } else {
                    break;
                }
            }
        }
        if (this.getMainRoute()!=null){
            for (OneTaskTrack taskTrack : this.getMainRoute()) {
               if (taskTrack.getSlotId() != null && taskTrack.getSlotId().longValue() != -1L && !taskTrack.getSlotId().equals(Long.MIN_VALUE)) {
                    str += "track(";
                    str += "station(" + taskTrack.getLink().getFrom().getId() + "),";
                    str += "station(" + taskTrack.getLink().getTo().getId() + "),";
                    str += "time_start(" + taskTrack.getTimeStart() + "),";
                    str += "time_end(" + taskTrack.getTimeEnd() + "),";
                    str += "slot_id(" + (taskTrack.getSlotId() == -2L ? -1L : taskTrack.getSlotId()) + ")";
                    str += "),";
                } else {
                   break;
               }
            }
        }
        //str = printRoute(preRoute, str); //not empty when OneTask was formed after +cancelled() was parsed
        //str = printRoute(this.getMainRoute(), str);
        str = str.substring(0, str.length() - 1);
        str += "])";
        str += "))";
        return str;
    }

    public String toTellWithDateString() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String str = "";
        str = "tell(slot_train(id(" + Id+"),";
        str += "time_start + " + formatter.format(new Date(startTime * 1000L)) + ",";
        str += "route([";
        if (preRoute!=null){
            for (OneTaskTrack taskTrack : preRoute) {
                if (taskTrack.getSlotId() != null && taskTrack.getSlotId().longValue() != -1L && !taskTrack.getSlotId().equals(Long.MIN_VALUE)) {
                    str += "track(";
                    str += "station(" + taskTrack.getLink().getFrom().getId() + "),";
                    str += "station(" + taskTrack.getLink().getTo().getId() + "),";
                    str += "time_start(" + formatter.format(new Date((long)taskTrack.getTimeStart() * 1000L) )  + "),";
                    str += "time_end(" + formatter.format(new Date((long)taskTrack.getTimeEnd() * 1000L) ) + "),";
                    str += "slot_id(" + (taskTrack.getSlotId() == -2L ? -1L : taskTrack.getSlotId()) + ")";
                    str += "),";
                } else {
                    break;
                }
            }
        }
        if (this.getMainRoute()!=null){
            for (OneTaskTrack taskTrack : this.getMainRoute()) {
                //if (taskTrack.getSlotId() != null && taskTrack.getSlotId().longValue() != -1L && !taskTrack.getSlotId().equals(Long.MIN_VALUE)) {
                    str += "track(";
                    str += "station(" + taskTrack.getLink().getFrom().getId() + "),";
                    str += "station(" + taskTrack.getLink().getTo().getId() + "),";
                    str += "time_start(" + formatter.format(new Date(taskTrack.getTimeStart() * 1000L)) + " " + taskTrack.getTimeStart() + "),";
                    str += "time_end(" + formatter.format(new Date(taskTrack.getTimeEnd() * 1000L)) + "),";
                    str += "slot_id(" + (taskTrack.getSlotId() == -2L ? -1L : taskTrack.getSlotId()) + ")";
                    str += "),";
                //} else {
                //    break;
                //}
            }
        }
        //str = printRoute(preRoute, str); //not empty when OneTask was formed after +cancelled() was parsed
        //str = printRoute(this.getMainRoute(), str);
        str = str.substring(0, str.length() - 1);
        str += "])";
        str += "))";
        return str;
    }


    public String toOneTaskString() {
        String str = "";
        str = "oneTask(id(" + Id+"),";
        str += "route([";
        if (preRoute!=null){
            for (OneTaskTrack taskTrack : preRoute) {
                //if (taskTrack.getSlotId() != null && taskTrack.getSlotId().longValue() > 0) {
                    str += "track(";
                    str += "station(" + taskTrack.getLink().getFrom().getId() + "),";
                    str += "station(" + taskTrack.getLink().getTo().getId() + "),";
                    str += "time_start(" + taskTrack.getTimeStart() + "),";
                    str += "time_end(" + taskTrack.getTimeEnd() + "),";
                    str += "slot_id(" + taskTrack.getSlotId() + ")";
                    str += "),";
                //}
            }
        }
        if (this.getMainRoute()!=null){
            for (OneTaskTrack taskTrack : this.getMainRoute()) {
                //if (taskTrack.getSlotId() != null && taskTrack.getSlotId().longValue() > 0) {
                    str += "track(";
                    str += "station(" + taskTrack.getLink().getFrom().getId() + "),";
                    str += "station(" + taskTrack.getLink().getTo().getId() + "),";
                    str += "time_start(" + taskTrack.getTimeStart() + "),";
                    str += "time_end(" + taskTrack.getTimeEnd() + "),";
                    str += "slot_id(" + taskTrack.getSlotId() + ")";
                    str += "),";
                //}
            }
        }
        //str = printRoute(preRoute, str); //not empty when OneTask was formed after +cancelled() was parsed
        //str = printRoute(this.getMainRoute(), str);
        str = str.substring(0, str.length() - 1);
        str += "])";
        str += "))";
        return str;
    }


    public String printRoute(List<OneTaskTrack> route, String str){
        if (route!=null){
            for (OneTaskTrack taskTrack : route) {
                if (taskTrack.getSlotId() != null && taskTrack.getSlotId().longValue() > 0) {
                    str += "track(";
                    str += "station(" + taskTrack.getLink().getFrom().getId() + "),";
                    str += "station(" + taskTrack.getLink().getTo().getId() + "),";
                    str += "time_start(" + taskTrack.getTimeStart() + "),";
                    str += "time_end(" + taskTrack.getTimeEnd() + "),";
                    str += "slot_id(" + taskTrack.getSlotId() + ")";
                    str += "),";
                }
            }
        }
        return str;
    }

}
