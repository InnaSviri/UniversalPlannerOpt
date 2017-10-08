package ru.programpark.planners.utils;

import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.slot.SlotTrack;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class CapacityUtils {
    public static Slot createSlot(LinkedHashSet<Link> links, Long slotId, Long timeStart) {
        LinkedHashMap<Station, SlotTrack> tracks = new LinkedHashMap<>();

        Long nextStationStartTime = 0L;
        for (Link link : links) {
            SlotTrack slotTrack = null;
            if (nextStationStartTime == 0L) {
                slotTrack = new SlotTrack(link, timeStart, timeStart + link.getDefaultDuration(), slotId);
                nextStationStartTime = timeStart + link.getDefaultDuration();
            } else {
                slotTrack = new SlotTrack(link, nextStationStartTime, nextStationStartTime + link.getDefaultDuration(), slotId);
                nextStationStartTime = nextStationStartTime + link.getDefaultDuration();
            }
            if (slotTrack != null) {
                tracks.put(link.getFrom(), slotTrack);
            } else {
                break;
            }
        }

        Slot slot = new Slot(slotId, 0L, tracks);
        return slot;
    }

}
