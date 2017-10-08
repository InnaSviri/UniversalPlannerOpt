package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;

/**
 * Явка бригады
 */
public class TeamPresenceEvent extends StationEvent implements Team.Event {

    public boolean fromHomePresence; //true = явка в депо приписки, false = явка в пункте оборота

    public TeamPresenceEvent(Integer runIndex, Integer locoFrameIndex, Integer teamFrameIndex,
                             Long stationId, Long time) {
        super(runIndex, locoFrameIndex, teamFrameIndex, stationId, time);
    }

    public TeamPresenceEvent(boolean fromHomePresence, Long stationId, Long time) {
        super(-1, -1, -1, stationId, time);
        this.fromHomePresence = fromHomePresence;
    }

    @Override
    public Station getStation() {
        return super.getStation();
    }
}
