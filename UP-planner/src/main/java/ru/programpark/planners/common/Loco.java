package ru.programpark.planners.common;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.loco.*;
import ru.programpark.entity.loco.BaseLocoTrack.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.NonNull;
import lombok.Delegate;
import lombok.EqualsAndHashCode;
import ru.programpark.entity.train.TrainArrive;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

@EqualsAndHashCode
public class Loco {

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(Loco.class);
        return logger;
    }


    public interface Event extends CommonEventContainer.Event {
        public Long getStationId();
        public Long getLocoReserveReadyTime();
    }

    public class EventContainer extends CommonEventContainer {
        private final Stack<Event> eventStack = new Stack<>();
        @Override protected Stack<Event> getEventStack() {
            return eventStack;
        }

        private List<RealLocoTrack> toRealRoute(LocoAssignEvent event) {
            List<RealLocoTrack> rRoute = new ArrayList<>();
            Long trainId = event.getTrainId();
            Train train = SchedulingData.getFrameData().getTrain(trainId);
            Integer startIndex = event.getStartIndex(),
                endIndex = event.getEndIndex();
            for (int i = startIndex; i <= endIndex; ++i) {
                Train.EventContainer track = train.getRoute().get(i);
                Link link = track.getLink();
                AssignEvent evt =
                    (AssignEvent) (track.lastEvent(AssignEvent.class));
                if (evt != null) {
                    Long tStart = evt.getTimeStart(), tEnd = evt.getTimeEnd();
                    State state = evt.getLocoState();
                    RealLocoTrack rTrack =
                        new RealLocoTrack(link, state, trainId,
                                          tStart, tEnd, -1L);
                    rRoute.add(rTrack);
                }
            }
            return rRoute;
        }

        private RealLocoTrack toRealRoute(LocoServiceEvent event) {
            Station st = SchedulingData.getFrameData()
                                       .getStation(event.getStationId());
            Link link = new Link(st, st, event.getDuration(), 0, false);
            Long tStart = event.getStartTime(), tEnd = event.getEndTime();
            return new RealLocoTrack(link, State.TECH, -1L,
                                     tStart, tEnd, -1L);
        }

        List<RealLocoTrack> toRealRoute() {
            List<RealLocoTrack> rRoute = new ArrayList<>();
            for (Event evt : eventStack) {
                if (evt.isCancelled()) {
                    continue;
                } else if (evt instanceof LocoAssignEvent) {
                    rRoute.addAll(toRealRoute((LocoAssignEvent) evt));
                } else if (evt instanceof LocoServiceEvent) {
                    LocoServiceEvent sEvt = (LocoServiceEvent) evt;
                    if (sEvt.getDuration() > 0L)
                        rRoute.add(toRealRoute(sEvt));
                }
            }
            return rRoute;
        }
    }

    @NonNull private final FactLoco factLoco;
    @Getter @NonNull private EventContainer events;

    public Long getId() {
        return factLoco.getId();
    }

    @Delegate(types=LocoAttributes.class)
    public LocoAttributes getAttributes() {
        return factLoco;
    }

    private Integer checkTrainEvent(Long trainId, StationPair stp, Class type,
                                    Long time) {
        Train train = SchedulingData.getFrameData().getTrain(trainId);
        if (train != null) {
            Train.Event tEvt = train.lastEvent(stp, type);
            if (tEvt != null) {
                Integer index = train.trackIndex(stp);
                Long tTime = (tEvt instanceof TrainArriveEvent)
                    ? ((TrainArriveEvent) tEvt).getTimeEnd()
                    : (tEvt instanceof TrainDepartEvent)
                    ? ((TrainDepartEvent) tEvt).getTimeStart()
                    : ((TrainReadyEvent) tEvt).getTime();
                if (! tTime.equals(time)) {
                    LOGGER().warn("Несовпадение времени" +
                                  " у фактического поезда " + trainId +
                                  " и локомотива " + getId());
                    time = Math.max(time, tTime);
                    if (time > tTime)
                        train.addEvent(tEvt.shiftedBy(time - tTime, -1, -1, -1));
                }
                return index;
            } else {
                LOGGER().warn("Фактический локомотив " + getId() +
                              " содержит неверные данные о следовании" +
                              " с поездом " + trainId);
            }
        }
        return null;
    }

    private void addEvent(FactLocoTrack track) {
        Long trainId = track.getTrainId();
        StationPair stp = StationPair.specifier(track.getLink());
        Long time = track.getTimeDepart();
        Integer index =
            checkTrainEvent(trainId, stp, TrainDepartEvent.class, time);
        if (index != null)
            addEvent(new LocoDepartEvent(-1, -1, -1, trainId, index));
    }

    private void addEvent(Station station, FactLocoArrive arrive) {
        Long trainId = arrive.getId();
        StationPair stp = new StationPair(-1L, station.getId());
        Long time = arrive.getTime();
        Integer index =
            checkTrainEvent(trainId, stp, TrainArriveEvent.class, time);
        if (index == null) { //пытаемся найти поезд в состоянии ready
            if ((index = checkTrainEvent(trainId, stp, TrainReadyEvent.class, time)) != null) {
                TrainArriveEvent aEvt = new TrainArriveEvent(-1, -1, -1, stp, time);
                SchedulingData.getFrameData().getTrain(trainId).addEvent(aEvt);
            }
        }
        if (index != null)
            addEvent(new LocoArriveEvent(-1, -1, -1, trainId, index));
        //Если если loco arrive для вообще несуществующего поезда, то создаем loco ready
        if (index == null &&
                SchedulingData.getFrameData().getTrain(trainId) == null) {
            addEvent(station, time);
        }
    }

    private void addEvent(Station station, Long time) {
        addEvent(new LocoReadyEvent(-1, -1, -1, station.getId(), time));
    }

    private void addEvent(Station station, NextService service) {
        Long decal =
            lastEvent().getEventTime() - service.getTimeOfServiceFact();
        LocoServiceEvent evt =
            new LocoServiceEvent(-1, -1, -1, station.getId(), 0L,
                                 service.getTimeToService() - decal,
                                 service.getDistanceToService(),
                                 service.getServiceType(),
                                 lastEvent());
        addEvent(evt);
    }

    public void addEvent(Event event) {
        events.addEvent(event);
    }

    public boolean hasEvents() {
        return events.hasEvents();
    }

    public List<Event> allEvents() {
        List<Event> evts = new ArrayList<>();
        for (CommonEventContainer.Event evt : events.getEventStack()) {
            if (! evt.isCancelled()) evts.add((Event) evt);
        }
        return evts;
    }

    public Event lastEvent() {
        return events.lastEvent();
    }

    public Event lastEvent(Class type) {
        return events.lastEvent(type);
    }

    public Event lastEvent(Integer runIndex, Integer locoFrameIndex,
                           Integer teamFrameIndex) {
        return events.lastEvent(runIndex, locoFrameIndex, teamFrameIndex);
    }

    public Event firstEvent(Integer runIndex, Integer locoFrameIndex,
                            Integer teamFrameIndex) {
        return events.firstEvent(runIndex, locoFrameIndex, teamFrameIndex);
    }

    public LocoServiceEvent lastServiceEvent() {
        return (LocoServiceEvent) (lastEvent(LocoServiceEvent.class));
    }

    public Event lastNonServiceEvent() {
        Event evt = lastEvent(ReferenceEvent.class);
        if (evt == null) evt = lastEvent(LocoReadyEvent.class);
        return evt;
    }

    public List<ReferenceEvent> unassignedTeamEvents() {
        List<ReferenceEvent> unassignedTeamEvents = new ArrayList<>();
        ReferenceEvent nonAssignEvent = null;
        for (CommonEventContainer.Event evt : events.getEventStack()) {
            if (! evt.isCancelled() && evt instanceof ReferenceEvent) {
                ReferenceEvent rEvt = (ReferenceEvent) evt;
                Train train = SchedulingData.getFrameData()
                                            .getTrain(rEvt.getTrainId());
                if (train.getUnassignedTeamIndex() <= rEvt.getEndIndex()) {
                    if (rEvt instanceof LocoAssignEvent) {
                        unassignedTeamEvents.add(rEvt);
                    } else {
                        nonAssignEvent = rEvt;
                    }
                }
            }
        }
        if (unassignedTeamEvents.isEmpty() && nonAssignEvent != null)
            unassignedTeamEvents.add(nonAssignEvent);
        return unassignedTeamEvents;
    }

    public Station getStation() {
        Event evt = lastNonServiceEvent();
        return (evt == null) ? null :
            SchedulingData.getFrameData().getStation(evt.getStationId());
    }

    public void updateService(ReferenceEvent event) {
        LocoServiceEvent sEvt = lastServiceEvent();
        Long timeToService = sEvt.getTimeToService() -
            (event.getEventTime() - sEvt.getTime());
        Long distToService = sEvt.getDistToService() - event.distance();
        sEvt = new LocoServiceEvent(-1L, 0L, timeToService, distToService,
                                    sEvt.getServiceType(), (Event) event);
        addEvent(sEvt);
    }

    public List<Event>
    cancelEvents(Integer runIndex, Integer locoFrameIndex,
                 Integer teamFrameIndex) {
        return events.cancelEvents(runIndex, locoFrameIndex, teamFrameIndex);
    }

    public RealLoco toRealLoco() {
        List<RealLocoTrack> route = events.toRealRoute();
        return (route.isEmpty()) ? null :
            new RealLoco(getId(), route, factLoco);
    }

    public String toString() {
        String ret = "Loco{" + getId() + ": ";
        int i = 0;
        for (CommonEvent cEvt : events.getEventStack()) {
            Event evt = (Event) cEvt;
            if (i > 0) ret += ", ";
            if (evt.isCancelled()) {
                continue;
            } else if (evt instanceof LocoArriveEvent) {
                ret += "◦→" + evt.getStationId();
            } else if (evt instanceof LocoDepartEvent) {
                ret += evt.getStationId() + "→◦";
            } else if (evt instanceof LocoAssignEvent) {
                ret += "•→" + evt.getStationId();
            } else if (evt instanceof LocoReadyEvent) {
                ret += "‣" + evt.getStationId();
            } else if (evt instanceof LocoServiceEvent) {
                LocoServiceEvent sEvt = (LocoServiceEvent) evt;
                if (sEvt.getDuration() > 0L) {
                    ret += "⬢" + evt.getStationId();
                } else {
                    ret += "⬡" + sEvt.getTimeToService() +
                        " " + sEvt.getDistToService();
                }
            }
            if (evt instanceof ReferenceEvent) {
                ReferenceEvent rEvt = (ReferenceEvent) evt;
                ret += " " + rEvt.getTrainId() + "[" +
                    rEvt.getStartIndex() + ".." +
                    rEvt.getEndIndex() + "]";
            }
            ++i;
        }
        ret += "}";
        return ret;
    }

    public Loco(FactLoco factLoco) {
        this.factLoco = factLoco;
        this.events = new EventContainer();
        FactLocoTrack track;
        Station st;
        if ((track = factLoco.getTrack()) != null) {
            st = track.getLink().getFrom();
            addEvent(track);
        } else if ((st = factLoco.getStation()) != null) {
            if (factLoco.getLocoArrive() != null) {
                addEvent(st, factLoco.getLocoArrive());
            } else {
                addEvent(st, factLoco.getTimeOfLocoFact());
            }
        }
        if (hasEvents() && factLoco.getTimeOfServiceFact() >= 0L) {
            addEvent(st, (NextService) factLoco);
        }
    }

    public Loco(Loco proto) {
        this.factLoco = proto.factLoco;
        this.events = new EventContainer();
    }
}
