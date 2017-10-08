package ru.programpark.entity.train;

import ru.programpark.entity.fixed.Station;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * User: oracle
 * Date: 13.11.14
 * +pinned_train(id(TrainId), station(StId), slot(SlotId))
 */
public class PinnedTrain implements Serializable {
    private Long trainId;
    private Station station;
    private Long slotId;

    public PinnedTrain(Long trainId, Station station, Long slotId) {
        this.trainId = trainId;
        this.station = station;
        this.slotId = slotId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PinnedTrain that = (PinnedTrain) o;

        if (slotId != null ? !slotId.equals(that.slotId) : that.slotId != null) return false;
        if (station != null ? !station.equals(that.station) : that.station != null) return false;
        if (trainId != null ? !trainId.equals(that.trainId) : that.trainId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = trainId != null ? trainId.hashCode() : 0;
        result = 31 * result + (station != null ? station.hashCode() : 0);
        result = 31 * result + (slotId != null ? slotId.hashCode() : 0);
        return result;
    }

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PinnedTrain");
        sb.append("{trainId=").append(trainId);
        sb.append(", station=").append(station);
        sb.append(", slotId=").append(slotId);
        sb.append('}');
        return sb.toString();
    }
}
