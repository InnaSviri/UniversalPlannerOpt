package ru.programpark.planners.common;

import lombok.Getter;
import lombok.Setter;
import lombok.Delegate;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.team.BaseTeamTrack;
import ru.programpark.planners.train.TrainPlanner;

public class HintEvent extends TrackEvent implements Train.Event {
    @Getter private Long locoId;
    @Getter private BaseLocoTrack.State locoState;
    @Getter private Long teamId;
    @Getter private BaseTeamTrack.State teamState;

    private Train.Event priorEvent;

    @Delegate(types=Train.Event.class)
    public Train.Event getPriorEvent() { return priorEvent; }

    public HintEvent(Integer runIndex, Integer locoFrameIndex,
                     Integer teamFrameIndex, StationPair stationPair,
                     Long locoId, BaseLocoTrack.State locoState,
                     Long teamId, BaseTeamTrack.State teamState,
                     Train.Event priorEvent) {
        super(runIndex, locoFrameIndex, teamFrameIndex, stationPair,
              priorEvent.getEventTime(), priorEvent.getEventTime());
        this.priorEvent = priorEvent;
        this.locoId = locoId;
        this.locoState = locoState;
        this.teamId = teamId;
        this.teamState = teamState;
    }

    public HintEvent(StationPair stationPair,
                     Long locoId, BaseLocoTrack.State locoState,
                     Long teamId, BaseTeamTrack.State teamState,
                     Train.Event priorEvent) {
        this(SchedulingData.getCurrentFrame().runIndex,
             SchedulingData.getCurrentFrame().locoFrameIndex,
             SchedulingData.getCurrentFrame().teamFrameIndex,
             stationPair, locoId, locoState, teamId, teamState,
             priorEvent);
    }

    public HintEvent(StationPair stationPair, Long locoId, Long teamId,
                     Train.Event priorEvent) {
        this(stationPair,
             locoId, locoId == null ? null : BaseLocoTrack.State.WITH_TRAIN,
             teamId, teamId == null ? null : BaseTeamTrack.State.AT_WORK,
             priorEvent);
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
