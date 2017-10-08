package ru.programpark.planners.common;

import lombok.*;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.SeriesPair;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.loco.LocoTonnage;
import ru.programpark.entity.team.BaseTeamTrack;
import ru.programpark.entity.train.*;
import ru.programpark.entity.util.Pair;

import java.util.*;

@EqualsAndHashCode
public class Train {

    // Поездное событие
    public interface Event extends CommonEventContainer.Event {
        Event shiftedBy(Long shiftTime, Integer runIndex,
                        Integer locoFrameIndex, Integer teamFrameIndex);
        Event shiftedBy(Long shiftTime);
    }

    @RequiredArgsConstructor
    public class EventContainer extends CommonEventContainer {
        @Getter private final Link link;
        @Getter private final List<LocoRegionPriority> locoRegionPriorities;

        private final Stack<Event> eventStack = new Stack<>();
        @Override protected Stack<Event> getEventStack() {
            return eventStack;
        }
    }

    @Getter @Setter private Long trainId;
    private final FactTrain factTrain;
    private final Task task;

    @Getter @Setter private Long startTime = Long.MIN_VALUE;
    @Getter @Setter private Integer unassignedLocoIndex = 0;
    @Getter @Setter private Integer unassignedTeamIndex = 0;
    @Setter private Boolean tentative = null;

    @Getter private EventContainer preRoute;
    @Getter private List<EventContainer> route;

    private Map<StationPair, Integer> routeTrackMap;
    private Map<Long, Integer> routeStationMap;

    public boolean isTentative(boolean test) {
        return (tentative != null) && (tentative == test);
    }

    private Long initRoute(List<Link> links) {
        this.route = new LinkedList<>();
        this.routeTrackMap = new HashMap<>();
        this.routeStationMap = new HashMap<>();
        int i = 0;
        Long firstStationId = null;
        for (Link link : links) {
            route.add(new EventContainer(link, new ArrayList<LocoRegionPriority>()));
            StationPair stp = StationPair.specifier(link);
            routeTrackMap.put(stp, i);
            routeStationMap.put(stp.getStationToId(), i);
            if (i == 0) {
                Link link0 = new Link(link.getFrom(), link.getFrom(), 0L, 0, false);
                this.preRoute = new EventContainer(link0, null);
                firstStationId = stp.getStationFromId();
                routeStationMap.put(firstStationId, -1);
            }
            ++i;
        }
        return firstStationId;
    }

    private void addEvent(TrainState state) {
        if (state != null) this.startTime = state.getTime();
        if (state instanceof TrainReady) {
            Station st = ((TrainReady) state).getStation();
            addEvent(new TrainReadyEvent(-1, -1, -1, st.getId(), startTime));
        } else {
            StationPair stp;
            if (state instanceof TrainArrive) {
                stp = StationPair.specifier(((TrainArrive) state).getLink());
                addEvent(new TrainArriveEvent(-1, -1, -1, stp, startTime));
            } else if (state instanceof TrainDepart) {
                stp = StationPair.specifier(((TrainDepart) state).getLink());
                addEvent(new TrainDepartEvent(-1, -1, -1, stp, startTime));
            }
        }
    }

    private void addEvent(Long stationId, Long startTime) {
        this.startTime = startTime;
        preRoute.addEvent(new TrainReadyEvent(-1, -1, -1, stationId, startTime));
    }

    public void addEvent(Event event) {
        Integer index = null;
        if (event instanceof StationEvent) {
            Long stId = ((StationEvent) event).getStationId();
            index = routeStationMap.get(stId);
        } else if (event instanceof TrackEvent) {
            StationPair stp = ((TrackEvent) event).getStationPair();
            index = trackIndex(stp);
        }
        if (index != null)
            ((index.equals(-1)) ? preRoute : route.get(index)).addEvent(event);
    }

    public boolean hasEvents() {
        if (preRoute.hasEvents()) return true;
        for (EventContainer events :  route) {
            if (events.hasEvents()) return true;
        }
        return false;
    }

    public Event lastEvent(Integer index) {
        EventContainer events = (index < 0) ? preRoute : route.get(index);
        return (events != null && events.hasEvents()) ?
            (Event) (events.lastEvent()) : null;
    }

    public Event lastEvent(Integer index, Class type) {
        EventContainer events = (index < 0) ? preRoute : route.get(index);
        return (events != null && events.hasEvents()) ?
            (Event) (events.lastEvent(type)) : null;
    }

    public Event lastEvent(Integer index, Integer runIndex,
                           Integer locoFrameIndex, Integer teamFrameIndex) {
        EventContainer events = (index < 0) ? preRoute : route.get(index);
        return (events != null && events.hasEvents())
            ? (Event) (events.lastEvent(runIndex, locoFrameIndex,
                                        teamFrameIndex))
            : null;
    }

    public Event firstEvent(Integer index, Integer runIndex,
                            Integer locoFrameIndex, Integer teamFrameIndex) {
        EventContainer events = (index < 0) ? preRoute : route.get(index);
        return (events != null && events.hasEvents())
            ? (Event) (events.firstEvent(runIndex, locoFrameIndex,
                                         teamFrameIndex))
            : null;
    }

    private EventContainer trackEvents(StationPair stp) {
        Integer index = trackIndex(stp);
        if (index != null) {
            return index.equals(-1) ? preRoute : route.get(index);
        } else {
            return null;
        }
    }

    public Event lastEvent(StationPair stp) {
        EventContainer events = trackEvents(stp);
        return (events != null && events.hasEvents()) ?
            (Event) (events.lastEvent()) : null;
    }

    public Event lastEvent(StationPair stp, Class type) {
        EventContainer events = trackEvents(stp);
        return (events != null && events.hasEvents()) ?
            (Event) (events.lastEvent(type)) : null;
    }

    public Event lastEvent(StationPair stp, Integer runIndex,
                           Integer locoFrameIndex, Integer teamFrameIndex) {
        EventContainer events = trackEvents(stp);
        return (events != null && events.hasEvents())
            ? (Event) (events.lastEvent(runIndex, locoFrameIndex,
                                        teamFrameIndex))
            : null;
    }

    public Event firstEvent(StationPair stp, Integer runIndex,
                            Integer locoFrameIndex, Integer teamFrameIndex) {
        EventContainer events = trackEvents(stp);
        return (events != null && events.hasEvents())
            ? (Event) (events.firstEvent(runIndex, locoFrameIndex,
                                         teamFrameIndex))
            : null;
    }

    public Station getStationFrom(Integer index) {
        return route.get(index).getLink().getFrom();
    }

    public Station getStationTo(Integer index) {
        return route.get(index).getLink().getTo();
    }

    public Long duration(Integer startIndex, Integer endIndex) {
        AssignEvent firstAssign =
            (AssignEvent) lastEvent(startIndex, AssignEvent.class);
        AssignEvent lastAssign =
            (AssignEvent) lastEvent(endIndex, AssignEvent.class);
        return (firstAssign == null || lastAssign == null) ? null :
            (lastAssign.getTimeEnd() - firstAssign.getTimeStart());
    }

    private Map<Pair<Integer, Integer>, Long> distanceCache = new HashMap<>();
    public Long distance(Integer startIndex, Integer endIndex) {
        Pair<Integer, Integer> key = new Pair<>(startIndex, endIndex);
        Long dist = distanceCache.get(key);
        if (dist == null) {
            dist = 0L;
            for (int i = startIndex; i <= endIndex; ++i) {
                dist += route.get(i).getLink().getDistance();
            }
            distanceCache.put(key, dist);
        }
        return dist;
    }

    public Long maxWeight(Integer startIndex, Integer endIndex,
                          SeriesPair serp) {
        Long maxWeight = null;
        for (int i = startIndex; i <= endIndex; ++i) {
            LocoTonnage tonnage = route.get(i).getLink().getLocoTonnage(serp);
            if (tonnage != null) {
                maxWeight = (maxWeight == null) ? tonnage.getMaxWeight() :
                    Math.min(maxWeight, tonnage.getMaxWeight());
            }
        }
        return maxWeight;
    }

    public Long avgNormTime(Integer startIndex, Integer endIndex) {
        Long sum = 0L;
        int count = 0;
        for (int i = startIndex; i < endIndex; ++i) {
            Long normTime = route.get(i).getLink().getTo().getNormTime();
            if (normTime > 0L) {
                sum += normTime;
                count += 1;
            }
        }
        return (count > 0) ? (sum / count) : 3600L;
    }

    public Long shiftBy(Long shiftTime, Integer indexStart) {
        AssignEvent prevEvt = null;
        Long newShiftTime = shiftTime;
        for (int i = indexStart; i < route.size(); ++i) {
            EventContainer events = route.get(i);
            if (events.hasEvents()) {
                AssignEvent evt = (AssignEvent) (events.lastEvent(AssignEvent.class));
                if (i != indexStart && prevEvt != null) {
                    newShiftTime = prevEvt.getTimeEnd() - evt.getTimeStart();
                }
                prevEvt = evt.shiftedBy(newShiftTime);
                events.addEvent(prevEvt);
            }
        }
        return newShiftTime;
    }

    public List<List<Event>>
    cancelEvents(Integer indexStart, Integer runIndex,
                 Integer locoFrameIndex, Integer teamFrameIndex) {
        List<List<Event>> cancelled = new ArrayList<>();
        int i;
        for (i = indexStart; i < route.size(); ++i) {
            List<Event> evts =
                route.get(i).cancelEvents(runIndex, locoFrameIndex,
                                          teamFrameIndex);
            if (! evts.isEmpty()) cancelled.add(evts);
        }
        for (i = indexStart; i < unassignedTeamIndex; ++i) {
            if (findAssign(i, null, null, -1L, null) == null) {
                unassignedTeamIndex = i;
                break;
            }
        }
        for (; i < unassignedLocoIndex; ++i) {
            if (findAssign(i, -1L, null, null, null) == null) {
                unassignedLocoIndex = i;
                break;
            }
        }
        if (isTentative(false) && unassignedLocoIndex == 0)
            setTentative(true);
        return cancelled;
    }

    public void updateAssign(Integer startIndex, Integer endIndex,
                             Long locoId, BaseLocoTrack.State locoState,
                             Long teamId, BaseTeamTrack.State teamState) {
        for (int i = startIndex; i <= endIndex; ++i) {
            EventContainer events = route.get(i);
            AssignEvent evt =
                (AssignEvent) (events.lastEvent(AssignEvent.class));
            if (evt != null) {
                evt = evt.shiftedBy(0L);
                if (locoId != null) {
                    evt.setLocoId(locoId);
                    evt.setLocoState(locoState);
                }
                if (teamId != null) {
                    evt.setTeamId(teamId);
                    evt.setTeamState(teamState);
                }
                events.addEvent(evt);
            }
        }
    }

    public AssignEvent findAssign(Integer index,
                                  Long locoId, BaseLocoTrack.State locoState,
                                  Long teamId, BaseTeamTrack.State teamState) {
        EventContainer events = route.get(index);
        for (CommonEventContainer.Event evt : events.getEventStack()) {
            if (! evt.isCancelled() && evt instanceof AssignEvent) {
                AssignEvent aEvt = (AssignEvent) evt;
                if ((locoId != null &&
                         (locoId < 0
                              ? aEvt.getLocoId() != null
                              : locoId.equals(aEvt.getLocoId())) &&
                         (locoState == null ||
                              locoState.equals(aEvt.getLocoState()))) ||
                      (teamId != null &&
                           (teamId < 0
                                ? aEvt.getTeamId() != null
                                : teamId.equals(aEvt.getTeamId())) &&
                           (teamState == null ||
                                teamState.equals(aEvt.getTeamState())))) {
                    return aEvt;
                }
            }
        }
        return null;
    }

    public boolean locoChanged() {
        int index = getUnassignedTeamIndex();
        if (index == 0 || index == route.size())
                return false;
        AssignEvent curAssignEvent = (AssignEvent) lastEvent(index, AssignEvent.class);
        AssignEvent prevAssignEvent = (AssignEvent) lastEvent(index - 1, AssignEvent.class);
        boolean locoChanged = curAssignEvent != null && prevAssignEvent != null
                && curAssignEvent.getLocoId() != null && prevAssignEvent.getLocoId() != null
                && !curAssignEvent.getLocoId().equals(prevAssignEvent.getLocoId());
        return locoChanged;
    }

    public Integer trackIndex(StationPair pair) {
        Integer index = routeTrackMap.get(pair);
        if (index == null && pair.getStationToId() != null) {
            index = routeStationMap.get(pair.getStationToId());
        }
        if (index == null && pair.getStationFromId() != null) {
            index = routeStationMap.get(pair.getStationFromId());
            if (index != null) {
                if (++index >= route.size()) index = null;
            }
        }
        return index;
    }

    public RealTrain toRealTrain() {
        if (isTentative(true)) return null;
        List<RealTrainTrack> rRoute = new ArrayList<>();
        for (EventContainer track : route) {
            AssignEvent evt =
                (AssignEvent) (track.lastEvent(AssignEvent.class));
            if (evt != null) {
                RealTrainTrack rTrack =
                    new RealTrainTrack(track.getLink(), evt.getTimeStart(),
                                       evt.getTimeEnd(), -1L);
                rRoute.add(rTrack);
            }
        }
        return (rRoute.isEmpty()) ? null :
            new RealTrain(trainId, trainId, rRoute);
    }

    @Delegate(types=TrainAttributes.class)
    public TrainAttributes getAttributes() {
        return factTrain;
    }

    public Long getWeightTypeId() {
        return (task == null) ? null : task.getWeight();
    }

    public String toString() {
        String ret = "Train{" + getTrainId() + ": ";
        int i = 0;
        for (EventContainer track : route) {
            if (i > 0) ret += ", ";
            ret += i + " ";
            if (unassignedLocoIndex.equals(i)) ret += "Ⓛ";
            if (unassignedTeamIndex.equals(i)) ret += "Ⓣ";
            ret += track.getLink().getFrom().getId() + "→" + track.getLink().getTo().getId();
            AssignEvent aEvt = (AssignEvent) (track.lastEvent(AssignEvent.class));
            if (aEvt != null) {
                if (aEvt.getLocoId() != null)
                    ret += " " + aEvt.getLocoId();
                if (aEvt.getTeamId() != null)

                    ret += " " + aEvt.getTeamId();
            }
            ++i;
        }
        ret += "}";
        return ret;
    }

    public Train(Train proto) {
        this.trainId = proto.trainId;
        this.startTime = proto.startTime;
        this.preRoute = new EventContainer(proto.preRoute.getLink(), null);
        this.route = new ArrayList<EventContainer>(proto.route.size());
        for (int i = 0; i < proto.route.size(); ++i) {
            EventContainer track = proto.route.get(i);
            route.add(new EventContainer(track.getLink(),
                                         track.getLocoRegionPriorities()));
        }
        this.routeTrackMap = proto.routeTrackMap;
        this.routeStationMap = proto.routeStationMap;
        this.factTrain = proto.factTrain;
        this.task = proto.task;
    }

    public Train(GeneralizedTask gTask, int serial) {
        Long firstStation = null;
        try {
            firstStation = initRoute(gTask.getMainRoute().getLinkList());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (gTask instanceof FactTrain) {
            this.factTrain = (FactTrain) gTask;
            this.task = new Task();
            addEvent(((FactTrain) gTask).getTrainState());
            setTrainId(gTask.getId());
        } else {
            assert gTask instanceof Task;
            this.factTrain = new FactTrain();
            this.task = (Task) gTask;
            int q = gTask.getTrainQuantity();
            long m = (long) (Math.pow(10.0, Math.ceil(Math.log10((double) q)) + 1));
            long id = gTask.getId() * m + serial + 1;
            long t0 = ((Task) gTask).getStartTime();
            double f = Math.ceil(((Task) gTask).getDuration() / 3600);
            if (q > 1) {
                double l = Math.floor(q / f);
                double r = q % f;
                double d = r / f;
                int i, s; double c;
                for (i = 0, s = 0, c = 0d; i < f; ++i) {
                    double g = l + d + c;
                    if ((s += (int) Math.round(g)) > serial) break;
                    t0 += 3600;
                    c = g - Math.round(g);
                }
            } else {
                t0 += 3600 * (new Random(id)).nextInt((int) f);
            }
            if (firstStation != null) addEvent(firstStation, t0);
            setTrainId(id);
        }
    }

    public Train(Long id, List<Link> links, Long startTime) {
        setTrainId(id);
        setStartTime(startTime);
        Long firstStation = initRoute(links);
        if (firstStation != null) addEvent(firstStation, startTime);
        this.factTrain = new FactTrain();
        this.task = new Task();
    }

    public boolean assigned(Long from, Long to){ //проходит ли маршрут поезда через 2 станции и при этом назначен локомотив и бригада
        Integer indexDep = routeStationMap.get(from), indexArrival = routeStationMap.get(to);

        if (indexDep == null || indexArrival == null)
            return false;

        if (indexDep < 0  || indexArrival < 0) //поезд по факту ушел из from
            return false;

        if (indexDep >= unassignedLocoIndex || indexDep >= unassignedTeamIndex)
            return false;

        if (indexArrival >= unassignedLocoIndex || indexArrival >= unassignedTeamIndex)
            return false;

        if (indexArrival < indexDep) //поезд идет из to в from
            return false;

        return true;
    }

    public Pair<Integer, Integer> assignedAtIndex(Long from, Long to){ //волзвращает индекс начала и конца полного назначения, если оно есть, иначе null
        Integer indexDep = routeStationMap.get(from), indexArrival = routeStationMap.get(to);

        if (indexDep == null || indexArrival == null)
            return null;

        if (indexDep >= unassignedLocoIndex || indexDep >= unassignedTeamIndex)
            return null;

        if (indexArrival >= unassignedLocoIndex || indexArrival >= unassignedTeamIndex)
            return null;

        return new Pair<>(indexDep, indexArrival);
    }
}
