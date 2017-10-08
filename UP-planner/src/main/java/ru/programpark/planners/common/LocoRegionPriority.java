package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.loco.LocoRegion;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public class LocoRegionPriority implements Comparable<LocoRegionPriority> {
    private LocoRegion region;
    private Integer routeLength;
    private Integer routeMatch;
    private Integer routeMismatch;
    private Integer routeEndBonus;
    private Integer handicap;

    public LocoRegionPriority(LocoRegion region, Integer routeLength,
                              Integer routeMatch, Integer routeMismatch,
                              Integer routeEndBonus) {
        this.region = region;
        this.routeLength = routeLength;
        this.routeMatch = routeMatch;
        this.routeMismatch = routeMismatch;
        this.routeEndBonus = routeEndBonus;
        this.handicap = 0;
    }

    public static LocoRegionPriority NO_REGION_PRIORITY =
        new LocoRegionPriority(new LocoRegion(-1L), 0, 0, 0, 0);

    public LocoRegion getRegion() { return region; }

    public void setHandicap(Integer handicap) { this.handicap = handicap; }

    private static final long scale = 1000000L;

    public Long badness() {
        long m = routeMatch.longValue();
        long q = routeMismatch.longValue();
        long b = routeEndBonus.longValue();
        long h = handicap.longValue();
        return new Long(((q + 1) * scale) / (m + q) - ((b - h) * scale));
    }

    private String badnessCalc() {
        long m = routeMatch.longValue();
        long q = routeMismatch.longValue();
        long b = routeEndBonus.longValue();
        long h = handicap.longValue();
        return String.format("((%d+1)*%d)/(%d+%d)-((%d-%d)*%d)",
                             q, scale, m, q, b, h, scale);
    }

    @Override
    public String toString() {
        return "LocoRegionPriority{region=" + getRegion().getId() +
            ", length=" + routeLength +
            ", badness=" + badnessCalc() + "=" + badness() +
            "}";
    }

    public void extend(Integer routeEndBonus) {
        this.routeMatch++;
        this.routeMismatch--;
        this.routeEndBonus = routeEndBonus;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof LocoRegionPriority) {
            LocoRegionPriority that = (LocoRegionPriority) o;
            return region.equals(that.region);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return region.hashCode();
    }

    @Override
    public int compareTo(LocoRegionPriority that) {
        if (this.equals(that)) {
            return 0;
        } else {
            int bCmp = Long.compare(this.badness(), that.badness());
            return (bCmp == 0)
                ? Integer.compare(this.routeLength, that.routeLength)
                : bCmp;
        }
    }

    private static List<LocoRegionPriority>
    updatePriorities(Train.EventContainer track,
                     int trainRouteOffset, int trainRouteLength,
                     List<LocoRegionPriority> prevPriorities) {
        List<LocoRegionPriority> priorities = track.getLocoRegionPriorities();
        Station stFrom = track.getLink().getFrom();
        Station stTo = track.getLink().getTo();
        Integer endBonus = (trainRouteOffset == trainRouteLength - 1) ? 1 : 0;
        List<LocoRegion> stFromRegions = stFrom.getRegions();
        List<LocoRegion> stToRegions = stTo.getRegions();
        if (stToRegions != null) {
            for (LocoRegionPriority rp : prevPriorities) {
                if (stToRegions.contains(rp.getRegion())) {
                    rp.extend(endBonus);
                    priorities.add(rp);
                }
            }
            if (stFromRegions != null) {
                for (LocoRegion region : stFromRegions) {
                    if (stToRegions.contains(region)) {
                        int regRouteLength =
                            SchedulingData.getInputData()
                                          .getLinksByLocoRegion(region)
                                          .size();
                        LocoRegionPriority rp =
                            new LocoRegionPriority(region, regRouteLength, 1,
                                                   trainRouteLength - 1,
                                                   endBonus);
                        if (! priorities.contains(rp)) priorities.add(rp);
                    }
                }
            }
        }
        return priorities;
    }

    public static void initPriorities(Train train, LocoRegion region) {
        for (Train.EventContainer track : train.getRoute()) {
            LocoRegionPriority rp = new LocoRegionPriority(region, 1, 1, 0, 0);
            track.getLocoRegionPriorities().add(rp);
        }
    }

    public static void initPriorities(Train train) {
        List<Train.EventContainer> route = train.getRoute();
        if (! route.isEmpty()) {
            int len = route.size();
            List<LocoRegionPriority> curPriorities =
                Collections.<LocoRegionPriority>emptyList();
            Train.EventContainer track;
            for (int i = 0; i < len; ++i) {
                List<LocoRegionPriority> prevPriorities = curPriorities;
                track = route.get(i);
                curPriorities = updatePriorities(track, i, len, prevPriorities);
            }
            for (int i = 0; i < len; ++i) {
                track = route.get(i);
                curPriorities = track.getLocoRegionPriorities();
                if (curPriorities.isEmpty()) {
                    curPriorities.add(NO_REGION_PRIORITY);
                } else {
                    Collections.<LocoRegionPriority>sort(curPriorities);
                }
            }
        }
    }

    public static boolean demote(Train train, LocoRegion region) {
        List<Train.EventContainer> route = train.getRoute();
        LocoRegionPriority rpToDemote = null;
        boolean reordered = false;
        route: for (Train.EventContainer track : route) {
            List<LocoRegionPriority> rps = track.getLocoRegionPriorities();
            if (rpToDemote == null) {
                rps: for (LocoRegionPriority rp : rps) {
                    if (rp.getRegion().equals(region)) {
                        rp.setHandicap(10);
                        rpToDemote = rp;
                        break rps;
                    }
                }
            }
            if (rpToDemote != null) {
                if (rps.remove(rpToDemote)) {
                    if (! reordered && ! rps.isEmpty()) reordered = true;
                    rps.add(rpToDemote);
                } else {
                    break route;
                }
            }
        }
        return reordered;
    }

    public static List<LocoRegionPriority>
    filter(List<LocoRegionPriority> priorities, Collection<LocoRegion> mask) {
        List<LocoRegionPriority> ret = new ArrayList<LocoRegionPriority>(priorities);
        for (LocoRegionPriority rp : priorities) {
            if (! mask.contains(rp.getRegion())) ret.remove(rp);
        }
        return ret;
    }

    public static Integer
    endOfSlot(Train train, Integer index, Collection<LocoRegion> mask) {
        List<Train.EventContainer> route = train.getRoute();
        while (index < route.size()) {
            Train.EventContainer track = route.get(index);
            List<LocoRegionPriority> rps =
                filter(track.getLocoRegionPriorities(), mask);
            if (rps.isEmpty()) break;
            ++index;
        }
        return --index;
    }

    public static Integer
    endOfSlot(Train train, Integer index, LocoRegion region) {
        return endOfSlot(train, index,
                         Collections.<LocoRegion>singletonList(region));
    }
}
