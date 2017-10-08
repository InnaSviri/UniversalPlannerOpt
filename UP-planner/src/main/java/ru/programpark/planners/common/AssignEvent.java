package ru.programpark.planners.common;

import lombok.Getter;
import lombok.Setter;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.team.BaseTeamTrack;

public class AssignEvent extends TrackEvent implements Train.Event {
    @Getter @Setter private Long locoId;
    @Getter @Setter private BaseLocoTrack.State locoState;
    @Getter @Setter private Long teamId;
    @Getter @Setter private BaseTeamTrack.State teamState;

    public AssignEvent(Integer runIndex,
                       Integer locoFrameIndex, Integer teamFrameIndex,
                       StationPair stationPair, Long timeStart, Long timeEnd) {
        super(runIndex, locoFrameIndex, teamFrameIndex,
              stationPair, timeStart, timeEnd);
        this.locoId = this.teamId = null;
    }

    public AssignEvent(StationPair stationPair, Long timeStart, long timeEnd) {
        super(SchedulingData.getCurrentFrame().runIndex,
              SchedulingData.getCurrentFrame().locoFrameIndex,
              SchedulingData.getCurrentFrame().teamFrameIndex,
              stationPair, timeStart, timeEnd);
        this.locoId = this.teamId = null;
    }

    @Override public AssignEvent
    shiftedBy(Long shiftTime, Integer runIndex,
              Integer locoFrameIndex, Integer teamFrameIndex) {
        Long shiftedTimeStart = getTimeStart();
        Long shiftedTimeEnd = getTimeEnd();
        if (shiftTime > 0) {
            shiftedTimeStart += shiftTime;
            Link link = SchedulingData.getLink(this.getStationPair());
            Long duration = link.getDuration(shiftedTimeStart);
            shiftedTimeEnd = shiftedTimeStart + duration;
        }

        AssignEvent shifted =
            new AssignEvent(runIndex, locoFrameIndex, teamFrameIndex,
                            getStationPair(), shiftedTimeStart, shiftedTimeEnd);
        shifted.setLocoId(locoId);
        shifted.setLocoState(locoState);
        shifted.setTeamId(teamId);
        shifted.setTeamState(teamState);
        return shifted;
    }

    @Override public AssignEvent shiftedBy(Long shiftTime) {
        SchedulingFrame frame = SchedulingData.getCurrentFrame();
        return shiftedBy(shiftTime, frame.runIndex,
                         frame.locoFrameIndex, frame.teamFrameIndex);
    }

    private Long processTime() {
        Long pTime = getStation().getProcessTime();
        return (locoState == BaseLocoTrack.State.RESERVE)
            ? (pTime / 2) // После пересылки резервом как правило не
                          // требуется отцеплять локомотив
            : pTime;
    }

    @Override public Long getTrainReadyTime() {
        return getEventTime() + processTime();
    }

    @Override public Long getLocoReadyTime() {
        return getEventTime() + processTime();
    }

    @Override public Long getTeamReadyTime() {
        return getEventTime();// + getStation().getNormTime();
    }

    @Override public String toString() {
        return toString(null);
    }

    protected String toString(String subFields) {
        String fields = null;
        if (locoId != null || teamId != null || subFields != null) {
            String loco = (locoId == null) ? "" : (", L=" + locoId),
                team = (teamId == null) ? "" : (", T=" + teamId),
                sub = (subFields == null) ? "" : (", " + subFields);
            fields = (loco + team + sub).substring(2);
        }
        return super.toString(fields);
    }
}
