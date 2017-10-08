package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.util.Pair;

import java.util.List;
import java.util.ArrayList;

public class PassEvent extends BaseEvent implements Team.Event {
    public List<SlotTrack> tracks = new ArrayList<>();
    public List<Pair<Integer, Long>> locos = new ArrayList<>();

    public PassEvent(Integer runIndex, Integer locoFrameIndex,
                     Integer teamFrameIndex) {
        super(runIndex, locoFrameIndex, teamFrameIndex);
    }

    public PassEvent() {
        this(SchedulingData.getCurrentFrame().runIndex,
             SchedulingData.getCurrentFrame().locoFrameIndex,
             SchedulingData.getCurrentFrame().teamFrameIndex);
    }

    public Long getStartTime() {
        return tracks.isEmpty()
            ? SchedulingData.getInputData().getCurrentTime()
            : tracks.get(0).getTimeStart();
    }

    public Long getEndTime() {
        return tracks.isEmpty()
            ? SchedulingData.getInputData().getCurrentTime()
            : tracks.get(tracks.size() - 1).getTimeEnd();
    }

    @Override
    public Long getEventTime() {
        return getEndTime();
    }

    @Override public Station getStation() {
        return tracks.isEmpty() ? null :
                tracks.get(tracks.size() - 1).getLink().getTo();
    }

    protected String toString(String subFields) {
        String route = "", sub = "";
        if (! tracks.isEmpty()) {
            SlotTrack firstTrack = tracks.get(0),
                      lastTrack = tracks.get(tracks.size() - 1);
            route = String.format("%d@%d → … → %d@%d",
                                  firstTrack.getLink().getFrom().getId(),
                                  firstTrack.getTimeStart(),
                                  lastTrack.getLink().getTo().getId(),
                                  lastTrack.getTimeEnd());
            if (subFields != null) sub += ", ";
        }
        if (subFields != null) {
            sub += subFields;
        }
        return super.toString(route + sub);
    }
}
