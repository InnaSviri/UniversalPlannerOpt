package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;

/**
 * User: oracle
 * Date: 03.06.14
 *
 * +loco_tonnage(series(65554), sections(3), track(station(2000019067), station(2000085812)), max_train_weight(6300))
 */
public class LocoTonnage implements StationPair.Specifiable {
    SeriesPair seriesPair;
    Station from;
    Station to;
    Long maxWeight;

    public LocoTonnage() {
    }

    public LocoTonnage(SeriesPair seriesPair, Station from, Station to, Long maxWeight) {
        this.seriesPair = seriesPair;
        this.from = from;
        this.to = to;
        this.maxWeight = maxWeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocoTonnage that = (LocoTonnage) o;

        if (from != null ? !from.equals(that.from) : that.from != null) return false;
        if (maxWeight != null ? !maxWeight.equals(that.maxWeight) : that.maxWeight != null) return false;
        if (seriesPair != null ? !seriesPair.equals(that.seriesPair) : that.seriesPair != null) return false;
        if (to != null ? !to.equals(that.to) : that.to != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = seriesPair != null ? seriesPair.hashCode() : 0;
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (maxWeight != null ? maxWeight.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LocoTonnage");
        sb.append("{seriesPair=").append(seriesPair);
        sb.append(", from=").append(from);
        sb.append(", to=").append(to);
        sb.append(", maxWeight=").append(maxWeight);
        sb.append('}');
        return sb.toString();
    }

    public SeriesPair getSeriesPair() {
        return seriesPair;
    }

    public void setSeriesPair(SeriesPair seriesPair) {
        this.seriesPair = seriesPair;
    }

    public Station getFrom() {
        return from;
    }

    public void setFrom(Station from) {
        this.from = from;
    }

    public Station getTo() {
        return to;
    }

    public void setTo(Station to) {
        this.to = to;
    }

    public Long getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(Long maxWeight) {
        this.maxWeight = maxWeight;
    }
}
