package ru.programpark.entity.train;

import java.io.Serializable;

public class WeightType implements Serializable {
    public Long id;
    public Long minWeight;
    public Long maxWeight;

    public WeightType() {}

    public WeightType(Long id, Long minWeight, Long maxWeight) {
        this.id = id;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
    }

    public Long defaultWeight() {
        if (minWeight == null || maxWeight == null) {
            return null;
        } else if (Math.log10(maxWeight) >= 5) {
            return minWeight + 1000;
        } else {
            return (minWeight + maxWeight) / 2;
        }
    }

    @Override
    public boolean equals(Object other) {
        return (this == other) ||
            (other instanceof WeightType && id != null &&
                 id.equals(((WeightType) other).id));
    }

    @Override
    public int hashCode() {
        return (id == null) ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("WeightType{id=%d, minWeight=%d, maxWeight=%d}",
                             id, minWeight, maxWeight);
    }
}
