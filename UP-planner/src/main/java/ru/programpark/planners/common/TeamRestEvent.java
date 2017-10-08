package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;

public class TeamRestEvent extends StationEvent implements Team.Event {

    public static Long TIME_UNTIL_REST_DEF = 36900L;
    public Team.Event prevEvent;
    public Long restTimeStart;
    public Long restTimeEnd;
    public Long timeUntilRest;
    public Boolean homeRest;

    public TeamRestEvent(long timeUntilRest) {
        super(-1, -1, -1, -1L, -1L);
        this.timeUntilRest = timeUntilRest;
    }

    public TeamRestEvent(Integer runIndex,
                         Integer locoFrameIndex, Integer teamFrameIndex,
                         Long stationId, Long restTimeStart, Long restTimeEnd) {
        super(runIndex, locoFrameIndex, teamFrameIndex, stationId, restTimeEnd);
        this.restTimeStart = restTimeStart;
        this.restTimeEnd = restTimeEnd;
    }

    public TeamRestEvent(Long stationId, Long restTimeStart, Long restTimeEnd) {
        this(SchedulingData.getCurrentFrame().runIndex,
             SchedulingData.getCurrentFrame().locoFrameIndex,
             SchedulingData.getCurrentFrame().teamFrameIndex,
             stationId, restTimeStart, restTimeEnd);
        this.timeUntilRest = TIME_UNTIL_REST_DEF;
    }

    public TeamRestEvent(Boolean homeRest, Long stationId, Long restTimeStart, Long restTimeEnd) {
        this(stationId, restTimeStart, restTimeEnd);
        this.homeRest = homeRest;
    }

    @Override public Station getStation() {
        return super.getStation();
    }

    protected String toString(String subFields) {
        String time = String.format("%d ⇝ %d", restTimeStart, restTimeEnd),
            sub = (subFields == null) ? "" : (", " + subFields);
        return super.toString(time + sub);
    }
}
