package ru.programpark.entity.raw_entities;

import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.loco.LocoRelocation;

import java.util.ArrayList;
import java.util.List;

public class SlotLoco extends BaseSlot {
    public List<Track> route = new ArrayList<>();

    public class Track extends BaseTrack {
        public Long state;
        public Long trainId;
    }

    public boolean countsForRelocation(LocoRelocation r){
        boolean counts = false;

        for (Track track: route){
            if (track.state.equals(BaseLocoTrack.State.RESERVE.ordinal())) {
                if (track.stationToId.equals(r.getLinkTo().getTo().getId())) {
                    if (track.timeEnd >= r.getTime() && track.timeEnd <= r.getTime() + r.getInterval()) {
                        counts = true;
                        break;
                    }
                }
            }
        }

        return counts;
    }

    public boolean containsTeamTrack(SlotTeam.Track teamTrack){
        for (SlotLoco.Track locoTrack: route) {
            if (teamTrack.stationFromId.equals(locoTrack.stationFromId) && teamTrack.stationToId.equals(locoTrack.stationToId) &&
                    teamTrack.timeStart.equals(locoTrack.timeStart) && teamTrack.timeEnd.equals(locoTrack.timeEnd)) {
                return true;
            }
        }

        return false;
    }
}
