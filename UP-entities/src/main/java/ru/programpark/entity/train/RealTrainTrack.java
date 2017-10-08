package ru.programpark.entity.train;

import ru.programpark.entity.loco.FactLocoArrive;
import ru.programpark.entity.loco.RealLocoTrack;
import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.team.RealTeamTrack;
import ru.programpark.entity.team.BaseTeamTrack;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.data.InputData;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;


public class RealTrainTrack extends SlotTrack {
    public RealTrainTrack() {}

    public RealTrainTrack(OneTaskTrack track) {
        super(track);
    }

    public RealTrainTrack(RealTrainTrack track) {
        super((SlotTrack) track);
    }

    public RealTrainTrack(Link link, Long timeStart, Long timeEnd, Long slotId) {
        super(link, timeStart, timeEnd, slotId);
    }

    public RealLocoTrack toRealLocoTrack(Long trainId,
                                         BaseLocoTrack.State state) {
        if (state == BaseLocoTrack.State.TECH) trainId = -1L;
        return new RealLocoTrack(getLink(), state, trainId,
                                 getTimeStart(), getTimeEnd(),
                                 getSlotId());
    }

    public RealTeamTrack toRealTeamTrack(Long locoId,
                                         BaseTeamTrack.State state) {
        return new RealTeamTrack(getLink(), state, locoId, getSlotId(),
                                 getTimeStart(), getTimeEnd());
    }

    public FactLocoArrive toFactLocoArrive(Long trainId,
                                           BaseLocoTrack.State state) {
        return new FactLocoArrive(trainId, this.getTimeEnd(), state);
    }

    public void shiftTimes(long shift) {
        setTimeStart(getTimeStart() + shift);
        setTimeEnd(getTimeEnd() + shift);
    }
}
