package ru.programpark.planners.team;

import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.planners.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamSlot {
    public Long trainId;
    public Long locoId;
    public List<SlotTrack> route = new ArrayList<>();
    public List<SlotTrack> originalRoute = new ArrayList<>();

    public TeamSlot(Long trainId, Long locoId, List<SlotTrack> route) {
        this.trainId = trainId;
        this.locoId = locoId;
        this.route = route;
        for (SlotTrack track : route) {
            SlotTrack originalTrack = new SlotTrack(track);
            originalRoute.add(originalTrack);
        }
    }

    public static List<TeamSlot> initTeamSlot(SchedulingData frameData) {
        {
            Map<Long, TeamSlot> teamSlotMap = new HashMap<>();
            for (Loco loco : frameData.getLocos()) {
                List<ReferenceEvent> assignEvents = loco.unassignedTeamEvents();

                ReferenceEvent assignEvent = null;
                if (assignEvents.size() > 0) {
                    assignEvent = assignEvents.get(0);
                }

                if (assignEvent == null) {//Если вдруг нет события назначения локо (нет назначения локо на текущей итерации),//то в бригадные слоты ничего не добавляем
                    continue;
                }

                Long trainId = assignEvent.getTrainId();
                Train train = frameData.getTrain(trainId);
                List<SlotTrack> addTracks = new ArrayList<>();
                for (int i = train.getUnassignedTeamIndex(); i < train.getUnassignedLocoIndex(); i++) {
                    TrackEvent trackEvent = train.getRoute().get(i).lastEvent(TrackEvent.class);
                    Link link = frameData.getInputData().getLinks().get(trackEvent.getStationPair());
                    SlotTrack slotTrack = new SlotTrack(link, trainId, trackEvent.getTimeStart(), trackEvent.getTimeEnd(), -1L);
                    slotTrack.trackIndex = train.trackIndex(trackEvent.getStationPair());
                    addTracks.add(slotTrack);
                }
                TeamSlot teamSlot = new TeamSlot(trainId, loco.getId(), addTracks);
                teamSlotMap.put(teamSlot.locoId, teamSlot);
            }
            return new ArrayList<>(teamSlotMap.values());
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TeamSlot");
        sb.append("{trainId=").append(trainId);
        sb.append(", locoId=").append(locoId);
        sb.append(", route=").append(route);
        sb.append('}');
        return sb.toString();
    }

    public boolean checkForPlanningOnStation(long timeEndPlanningIteration){  //отсеиваем пустые слоты или слоты, начало которых позже конца интервала
        if (!route.isEmpty() && route.get(0).getTimeStart() <= timeEndPlanningIteration)
            return true;
        return false;
    }

    public SlotTrack getFirstSlotTrack(){
        SlotTrack firstSlot = null;
        for (SlotTrack track : route) {
            if (!track.specified) {
                firstSlot = track;
                break;
            }
        }

        return  firstSlot;
    }
}
