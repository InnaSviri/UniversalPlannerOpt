package ru.programpark.planners.util;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.raw_entities.SlotTrain;
import ru.programpark.entity.util.LoggingAssistant;

import java.util.*;

public class AnalyzeHelper {
    public static SlotLoco getSlotLocoBySlotTrainAndTrack(SlotTrain train, BaseTrack track, Collection<SlotLoco> locos) {

        for (SlotLoco loco : locos) {
            for (SlotLoco.Track locoTrack : loco.route) {
                if (checkEqualsStationIds(track, locoTrack) && locoTrack.trainId.equals(train.id)) {
                    return loco;
                }
            }
        }
        return null;
    }

    public static SlotTeam getSlotTeamBySlotTrainAndTrack(SlotTrain train, BaseTrack track, Collection<SlotLoco> locos, Collection<SlotTeam> teams) {
        SlotLoco loco = getSlotLocoBySlotTrainAndTrack(train, track, locos);

        if (loco == null)
            return null;

        for (SlotTeam team : teams) {
            for (SlotTeam.Track teamTrack : team.route) {
                try {
                    if (checkEqualsStationIds(track, teamTrack) && teamTrack.locoId.equals(loco.id)) {
                        return team;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LoggingAssistant.logException(e);
                }
            }
        }
        return null;
    }

    private static boolean checkEqualsStationIds(BaseTrack firstTrack, BaseTrack secondTrack) {
        return firstTrack.stationFromId.equals(secondTrack.stationFromId) && firstTrack.stationToId.equals(secondTrack.stationToId);
    }

    public static List<FactLoco> getFactLocoOnStation(InputData input, Long stationId) {
        List<FactLoco> locos = new ArrayList<>();

        for (FactLoco loco : input.getFactLocos().values()) {
            if (loco.getStation() != null && loco.getStation().getId().equals(stationId)) {
                locos.add(loco);
            }
        }
        return locos;
    }

    public static void sortFactLocosOrderByTimeToService(List<FactLoco> locos) {
        Collections.sort(locos, new Comparator<FactLoco>() {
            @Override
            public int compare(FactLoco o1, FactLoco o2) {
                return o1.getTimeToService().compareTo(o2.getTimeToService());
            }
        });
    }
    public static void sortFactLocosOrderByTimeOfLocoFact(List<FactLoco> locos) {
        Collections.sort(locos, new Comparator<FactLoco>() {
            @Override
            public int compare(FactLoco o1, FactLoco o2) {
                return o1.getTimeOfLocoFact().compareTo(o2.getTimeOfLocoFact());
            }
        });
    }

    public static void showSlotPassingOverStationPair(InputData input, StationPair stationPair) {
        class SlotHolder {
            Slot slot;
            Long timePassingOverStationPair;

            public SlotHolder(Slot slot, Long timePassingOverStationPair) {
                this.slot = slot;
                this.timePassingOverStationPair = timePassingOverStationPair;
            }
        }

        List<SlotHolder> slots = new ArrayList<>();

        for (Slot slot : input.getSlots().values()) {
            for (SlotTrack track : slot.getRoute().values()) {
                if (track.getLink().getStationPair().equals(stationPair)) {
                    slots.add(new SlotHolder(slot, track.getTimeStart()));
                }
            }
        }

        Collections.sort(slots, new Comparator<SlotHolder>() {
            @Override
            public int compare(SlotHolder o1, SlotHolder o2) {
                return o1.timePassingOverStationPair.compareTo(o2.timePassingOverStationPair);
            }
        });

        for (SlotHolder slot : slots) {
            System.out.println("@@@ Проходящий слот: " + slot.slot.getSlotId()
                    + " в " + slot.timePassingOverStationPair
                    + " округленно до " + (slot.timePassingOverStationPair - slot.timePassingOverStationPair % 600));
        }
    }
}
