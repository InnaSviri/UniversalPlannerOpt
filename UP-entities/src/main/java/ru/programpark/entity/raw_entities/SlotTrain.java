package ru.programpark.entity.raw_entities;

import ru.programpark.entity.fixed.Station;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SlotTrain extends BaseSlot{
    public List<SlotTrain.Track> route = new ArrayList<>();

    public class Track extends BaseTrack implements Comparable<SlotTrain.Track>{
        public Long trainId = -1L;

        @Override
        public int compareTo(Track o) {
            if (this.timeStart < o.timeStart) {
                return -1;
            } else {
                if (this.timeStart > o.timeStart)
                    return 1;
                else
                    return 0;
            }
        }
    }

    public Long getTrainLength() {
        if (route.size() == 0) {
            return 0L;
        }
        return route.get(route.size() - 1).timeEnd - route.get(0).timeStart;
    }

    public boolean stopsOnStation(Station s){
        boolean passes = false;
        boolean arrives = false;
        boolean departs = false;
        Track prev = null;

        for (Track track: route) {
            if (track.stationFromId.equals(s.getId())) {
                departs = true;
            }
            if (track.stationToId.equals(s.getId())) {
                arrives = true;
            }
            if (prev != null && prev.timeEnd.equals(track.timeStart)){
                departs = false;
                arrives = false;
            }
            prev = track;
        }

        passes = departs && arrives;

        return passes;
    }

    public Long timeArriveAtStation(Long sId){
        Long t = -1L;

        for (Track track: route) {
            if (track.stationToId.equals(sId)) {
                return track.timeEnd;
            }
        }

        return t;
    }

    public Long timeDepartFromStation(Long sId){
        Long t = -1L;

        for (Track track: route) {
            if (track.stationFromId.equals(sId)) {
                return track.timeStart;
            }
        }

        return t;
    }

    public boolean noTeam (Long stationFromId, Long timeStart, Collection<SlotLoco> locos, Collection<SlotTeam> teams){
        Long locoId = -1L;

        for (SlotLoco loco: locos){
            for (SlotLoco.Track track: loco.route){
                if (track.stationFromId.equals(stationFromId) && track.timeStart.equals(timeStart) && track.trainId.equals(this.id)){
                    locoId = loco.id;
                    break;
                }
            }
        }

        if (locoId.equals(-1L)) { //не привязан локомотив, значит и бригада тоже
            return true;
        } else {
            for (SlotTeam team: teams){
                for (SlotTeam.Track track: team.route){
                    if (track.stationFromId.equals(stationFromId) && track.timeStart.equals(timeStart) && track.locoId.equals(locoId)){
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
