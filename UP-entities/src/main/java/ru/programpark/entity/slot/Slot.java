package ru.programpark.entity.slot;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.raw_entities.SlotTeam;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * User: oracle
 * Date: 19.05.14
 *  * Никти вариантного графика (грузовые поезда)
 +slot(id(SlotId), category(TrainCat), route([track(station(Id1), station(Id2), time_start(TimeStart), time_end(TimeEnd)), …]))

 SlotId – уникальный идентификатор нитки.
 TrainCat – категория поезда, для которой предназначена нитка.
 Id1 – идентификатор начальной станции участка.
 Id2 – идентификатор конечной станции участка.
 TimeStart – время отправления со станции Id1 на станцию Id2 в рамках данной нитки (в секундах).
 TimeEnd – время приема на станции Id2 со станции Id1 (в секундах).
 Для нитки должен быть указан весь маршрут, которому она соответствует.
 В сообщениях slot должны передаваться только нитки для движения грузовых поездов.

 * +slot_pass(id(SlotId), category(TrainCat), route([track(station(Id1), station(Id2), time_start(TimeStart), time_end(TimeEnd)), …]))

 */
public class Slot implements Serializable {
    Long slotId;
    Long trainCategory;
    LinkedHashMap<Station, SlotTrack> route; // contains slotId inside


    public Slot(Long slotId, Long trainCategory, LinkedHashMap<Station, SlotTrack> route) {
        this.slotId = slotId;
        this.trainCategory = trainCategory;
        this.route = route;
    }

    public Slot() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Slot thatSlot = (Slot) o;

        if (route != null ? !route.equals(thatSlot.route) : thatSlot.route != null) return false;
        if (trainCategory != null ? !trainCategory.equals(thatSlot.trainCategory) : thatSlot.trainCategory != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = trainCategory != null ? trainCategory.hashCode() : 0;
        result = 31 * result + (route != null ? route.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Slot");
        sb.append("{trainCategory=").append(trainCategory);
        sb.append(", route=").append(route);
        sb.append('}');
        return sb.toString();
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public Long getTrainCategory() {
        return trainCategory;
    }

    public void setTrainCategory(Long trainCategory) {
        this.trainCategory = trainCategory;
    }

    public LinkedHashMap<Station, SlotTrack> getRoute() {
        return route;
    }

    public void setRoute(LinkedHashMap<Station, SlotTrack> route) {
        this.route = route;
    }

    public void addRouteTrack(SlotTrack track) {
        if (route == null) {
            route = new LinkedHashMap<Station, SlotTrack>();
        }
        track.setSlotId(slotId);
        route.put(track.getLink().getFrom(), track);
    }

    public boolean containsStation(Station s){
        boolean contains = false;

        if (route.get(s) != null) {
            contains = true;
        } else {
            for (Station s1: route.keySet()){
                SlotTrack sTrack = route.get(s1);
                if (sTrack.getLink().getTo().equals(s)){
                    contains = true;
                }
            }
        }
        return contains;
    }

    public boolean containsTeamTrack(SlotTeam.Track teamTrack){
        for (SlotTrack slotTrack: route.values()) {
            if (teamTrack.stationFromId.equals(slotTrack.getLink().getFrom().getId()) && teamTrack.stationToId.equals(slotTrack.getLink().getTo().getId()) &&
                    teamTrack.timeStart.equals(slotTrack.getTimeStart()) && teamTrack.timeEnd.equals(slotTrack.getTimeEnd())) {
                return true;
            }
        }

        return false;
    }
}
