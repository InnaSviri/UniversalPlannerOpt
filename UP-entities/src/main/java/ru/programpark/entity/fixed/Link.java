package ru.programpark.entity.fixed;

import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.loco.LocoTonnage;
import ru.programpark.entity.loco.SeriesPair;
import ru.programpark.entity.slot.AveDurationSlotWrapper;
import ru.programpark.entity.util.Time;

import java.io.Serializable;
import java.util.*;

/**
 * User: oracle
 * Date: 19.05.14
 *
 * +link(track(station(2000479905),station(2000479894)),defaultDuration(7200), distance(150), push(0))
 Id1 – идентификатор начальной станции участка.
 Id2 – идентификатор конечной станции участка.
 Time – время хода по участку в направлении от Id1 к Id2 в секундах.
 Dist – расстояние между станциями Id1 и Id2 в километрах.
 Push – идентификатор участка подталкивания. Принимает значение 1, если на данном участке поезд необходимо подтолкнуть с помощью дополнительного локомотива, 0 – в обратном случае.
 */

public class Link implements Serializable, StationPair.Specifiable {
    private Station from;
    private Station to;
    private Integer direction = -1; // 0 — четное, 1 — нечетное, -1 — непонятно какое
    private Long defaultDuration;//time to traverse the link
    private Map<Integer, HashSet<AveDurationSlotWrapper>> durationMap = new HashMap<>(); // ключом является время начала 3-хчасового интервала: 0, 3, 6, 9, 12, 15, 18, 21
    private Map<SeriesPair, LocoTonnage> locoTonnages = new HashMap<>();
    private Integer distance;
    private Integer lines = 1; //number of lines
    private boolean push;
    private Integer lengthLimit;
    private static Integer defaultLengthLimit;
    private boolean linkMessage = false;
    private Map<Long, Capacity> originCapacities = new TreeMap<>(new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return o1.compareTo(o2);
        }
    });
    private Map<Long, Capacity> capacities = new TreeMap<>(new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return o1.compareTo(o2);
        }
    });


    public Link() {
        for (int i = 0; i <= 21; i += 3){
            durationMap.put(i, new HashSet<AveDurationSlotWrapper>());
        }
    }

    public Link(Station from, Station to, Long defaultDuration, Integer distance, boolean push) {
        this.from = from;
        this.to = to;
        this.defaultDuration = defaultDuration;
        this.distance = distance;
        this.push = push;
        for (int i = 0; i <= 21; i += 3){
            durationMap.put(i, new HashSet<AveDurationSlotWrapper>());
        }
    }

    public Link(Station from, Station to, Long defaultDuration, Integer distance, boolean push, Integer lengthLimit) {
        this.from = from;
        this.to = to;
        this.defaultDuration = defaultDuration;
        this.distance = distance;
        this.push = push;
        this.lengthLimit = lengthLimit;
        for (int i = 0; i <= 21; i += 3){
            durationMap.put(i, new HashSet<AveDurationSlotWrapper>());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Link link = (Link) o;

        if (from != null ? !from.equals(link.from) : link.from != null) return false;
        if (to != null ? !to.equals(link.to) : link.to != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
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

    public Long getDefaultDuration() {
        return defaultDuration;
    }

    public void setDefaultDuration(Long defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public boolean isPush() {
        return push;
    }

    public void setPush(boolean push) {
        this.push = push;
    }

    public Integer getLengthLimit() {
        return lengthLimit;
    }

    public void setLengthLimit(Integer lengthLimit) {
        this.lengthLimit = lengthLimit;
    }

    public static Integer getDefaultLengthLimit() {
        return defaultLengthLimit;
    }

    public static void setDefaultLengthLimit(Integer lengthLimit) {
        defaultLengthLimit = lengthLimit;
    }

    public Map<Long, Capacity> getOriginCapacities() {
        return originCapacities;
    }

    public void setOriginCapacities(Map<Long, Capacity> originCapacities) {
        this.originCapacities = originCapacities;
    }

    public Map<Long, Capacity> getCapacities() {
        return capacities;
    }

    public void setCapacities(Map<Long, Capacity> capacities) {
        this.capacities = capacities;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Link");
        sb.append("{from=").append(from);
        sb.append(", to=").append(to);
        sb.append(", direction=").append(direction);
        sb.append(", defaultDuration=").append(defaultDuration);
        sb.append(", distance=").append(distance);
        sb.append(", push=").append(push);
        if (lengthLimit != null) {
            sb.append(", lengthLimit=").append(lengthLimit);
        } else if (defaultLengthLimit != null) {
            sb.append(", defaultLengthLimit=").append(defaultLengthLimit);
        }
        if (originCapacities != null) sb.append(", originCapacities=").append(originCapacities);
        sb.append('}');
        return sb.toString();
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }

    // Является ли направление данного перегона грузовым? (Пока определяем
    // по чётности, потом надо это будет как-то получать в справочных данных.)
    public boolean isHaulingDirection() {
        return (direction % 2) == 0;
    }

    public Map<Integer, HashSet<AveDurationSlotWrapper>> getDurationMap() {
        return durationMap;
    }

    public void setDurationMap(Map<Integer, HashSet<AveDurationSlotWrapper>> durationMap) {
        this.durationMap = durationMap;
    }



    public Map<SeriesPair, LocoTonnage> getLocoTonnages() {
        return locoTonnages;
    }

    public LocoTonnage getLocoTonnage(SeriesPair serp) {
        return locoTonnages.get(serp);
    }

    public void addLocoTonnage(LocoTonnage tonnage) {
        locoTonnages.put(tonnage.getSeriesPair(), tonnage);
    }

    public void delLocoTonnage(LocoTonnage tonnage) {
        locoTonnages.remove(tonnage.getSeriesPair());
    }

    public Capacity getFreeCapacity(long timeStart) {
        for (Capacity capacity : getCapacities().values()) {
            if (capacity.getCapacity() != 0 && capacity.getStartTime() >= timeStart) {
                return capacity;
            }
        }
        return null;
    }

    public long calcShiftTimeWithCapacity(long startTime, Long trainId) {
        long shiftTime = 0L;
        Capacity freeCapacity = getFreeCapacity(startTime);
        if (freeCapacity != null) {
            takeCapacity(freeCapacity, trainId);
            shiftTime = freeCapacity.getStartTime() - startTime;
        }

        return shiftTime;
    }

    private Capacity takeCapacity(Capacity sourceCapacity, Long trainId) {
        for (Capacity capacity : getCapacities().values()) {
            if (capacity.equals(sourceCapacity)) {
                capacity.setCapacity(capacity.getCapacity() - 1);
                if (trainId != null) capacity.trainIds.add(trainId);
                return capacity;
            }
        }
        return null;
    }

    public boolean inRegion(LocoRegion region) {
        return from.getRegions().contains(region) &&
            to.getRegions().contains(region);
    }

    public boolean inAnyRegion(Collection<LocoRegion> regions) {
        if (regions != null) {
            for (LocoRegion region : regions) {
                if (this.inRegion(region)) return true;
            }
        }
        return false;
    }

    public StationPair getStationPair() {
        return new StationPair(getFrom().getId(), getTo().getId());
    }

    public Long getDuration(Long timeStart){
        Long duration = getAveDurationForHour(timeStart);
        Long timeEnd = timeStart + duration;

        Long T1 = duration;
        Long T2 = getAveDurationForHour(timeStart + duration); //T1 - duration in first time interval, T2 duration in next interval


        if (T1 > T2) {
            Long tBorder = (timeEnd / (3 * 3600)) * (3 * 3600);
            timeEnd = (long) (tBorder + (timeEnd - tBorder) * T2 * 1.0 / T1);
            duration = timeEnd - timeStart;
        }

        return duration;
    }

    private Long getAveDurationForHour(Long time){
        int hour = 0;
        Long sum = 0L;

        if (time < 24L)
            hour = time.intValue();
        else
            hour = new Time(time).getHour();
        hour = (hour / 3) * 3;

        for (AveDurationSlotWrapper a: getDurationMap().get(hour))
            sum += a.t;

        if (sum != 0L)
            return (sum/getDurationMap().get(hour).size());
        else
            return getDefaultDuration();
    }

    public void printAveDurationMap(){
        System.out.println("Default duration from " + getFrom().getName() + " to " + getTo().getName() + " is " + defaultDuration);
        for (int time: durationMap.keySet()){
            System.out.println("duration for " + time + " is " + getAveDurationForHour((long) time));
        }
    }

    public boolean isLinkMessage() {
        return linkMessage;
    }

    public void setLinkMessage(boolean linkMessage) {
        this.linkMessage = linkMessage;
    }

    public long getMaxDuration(){
        long maxValue = Long.MIN_VALUE;

        for (int key: durationMap.keySet()){
            for (AveDurationSlotWrapper w: durationMap.get(key)) {
                if (w.t > maxValue)
                    maxValue = w.t;
            }
        }

        return maxValue;
    }


    public long getMinDuration(){
        long minValue = Long.MAX_VALUE;
        for (int key: durationMap.keySet()){
            for (AveDurationSlotWrapper w: durationMap.get(key)) {
                if (w.t < minValue)
                    minValue = w.t;
            }
        }

        return minValue;
    }

    public Integer getLines() {
        return lines;
    }

    public void setLines(Integer lines) {
        this.lines = lines;
    }
}
