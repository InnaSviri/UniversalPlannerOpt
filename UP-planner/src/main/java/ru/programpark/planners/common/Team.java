package ru.programpark.planners.common;

import lombok.Delegate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.loco.FactLocoArrive;
import ru.programpark.entity.loco.FactLocoTrack;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.team.*;
import ru.programpark.entity.util.Pair;
import ru.programpark.planners.team.TeamPlanningParams;
import ru.programpark.planners.team.TeamSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@EqualsAndHashCode
public class Team {

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(Team.class);
        return logger;
    }

    public interface Event extends CommonEventContainer.Event {
        Station getStation();
    }

    public class EventContainer extends CommonEventContainer {
        private final Stack<Event> eventStack = new Stack<>();
        @Override protected Stack<Event> getEventStack() {
            return eventStack;
        }

        private List<RealTeamTrack> toRealRoute(TeamAssignEvent event) {
            List<RealTeamTrack> rRoute = new ArrayList<>();
            Long locoId = event.getLocoId();
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
                    Long tStart = evt.getTimeStart(),
                        tEnd = evt.getTimeEnd();
                    BaseTeamTrack.State state = evt.getTeamState();
                    RealTeamTrack rTrack =
                        new RealTeamTrack(link, state, locoId, -1L,
                                          tStart, tEnd);
                    rRoute.add(rTrack);
                }
            }
            return rRoute;
        }

        private List<RealTeamTrack> toRealRoute(TeamRestEvent event) {
            List<RealTeamTrack> rRoute = new ArrayList<>();
            if (event.getTeamReadyTime() <
                    SchedulingData.getCurrentFrame().rangeEnd) {
                Station st = event.getStation();
                if (st != null) {
                    Link link =
                            new Link(st, st, event.restTimeEnd - event.restTimeStart,
                                    0, false);
                    rRoute.add(new RealTeamTrack(link, BaseTeamTrack.State.REST,
                            -1L, -1L, event.prevEvent.getEventTime(), event.prevEvent.getEventTime() + event.restTimeEnd - event.restTimeStart));
                }
            }
            return rRoute;
        }

        private List<RealTeamTrack> toRealRoute(PassEvent event) {
            List<RealTeamTrack> rRoute = new ArrayList<>();
            int i = 0, j = 0;
            Long locoId = -1L;
            for (SlotTrack eTrack : event.tracks) {
                if (j < event.locos.size()) {
                    Pair<Integer, Long> locoIndexId = event.locos.get(j);
                    if (locoIndexId.getFirst().equals(i++)) {
                        locoId = locoIndexId.getSecond();
                        ++j;
                    }
                }
                RealTeamTrack rTrack =
                    new RealTeamTrack(eTrack.getLink(),
                                      BaseTeamTrack.State.PASSENGER,
                                      locoId, eTrack.getSlotId(),
                                      eTrack.getTimeStart(),
                                      eTrack.getTimeEnd());
                rRoute.add(rTrack);
            }
            return rRoute;
        }

        /*List<RealTeamTrack> toRealRoute() {
            List<RealTeamTrack> rRoute = new ArrayList<>();
            for (Event evt : eventStack) {
                if (evt.isCancelled()) {
                    continue;
                } else if (evt instanceof TeamAssignEvent) {
                    rRoute.addAll(toRealRoute((TeamAssignEvent) evt));
                } else if (evt instanceof TeamRestEvent) {
                    rRoute.addAll(toRealRoute((TeamRestEvent) evt));
                } else if (evt instanceof PassEvent) {
                    rRoute.addAll(toRealRoute((PassEvent) evt));
                }
            }
            return rRoute;
        } */

        List<RealTeamTrack> toRealRoute() {
            List<RealTeamTrack> rRoute = new ArrayList<>();
            Event prevEvt = null;
            for (Event evt : eventStack) {
                if (evt.isCancelled()) {
                    continue;
                } else if (evt instanceof TeamAssignEvent) {
                    if (prevEvt instanceof TeamRestEvent && rRoute.size() > 1) //если бригада вышла после отдыха и была далее подвязана,
                        rRoute.get(rRoute.size() - 1).setTimeEnd(((TeamAssignEvent) evt).getStartTime());// тогда необ-мо изменить время предыдущего трека
                    if (rRoute.isEmpty() && eventStack.size() > 1 && eventStack.get(1) instanceof TeamReadyEvent)//только для бригад готовых на станции (из factTeam)
                        rRoute.add(initialReadyTrack(evt, ((TeamReadyEvent) eventStack.get(1)).getStation()));
                    rRoute.addAll(toRealRoute((TeamAssignEvent) evt));
                } else if (evt instanceof TeamRestEvent) {
                    ((TeamRestEvent) evt).prevEvent = (Team.Event) prevEvt;
                    rRoute.addAll(toRealRoute((TeamRestEvent) evt));
                    if (rRoute.size() > 0) {
                        if (rRoute.get(rRoute.size() - 1).getState().equals(BaseTeamTrack.State.REST)) {
                            rRoute.add(afterRestReadyTrack((TeamRestEvent) evt));
                        }
                    }
                } else if (evt instanceof PassEvent) {
                    if (rRoute.isEmpty() && eventStack.get(0) instanceof TeamReadyEvent)
                        rRoute.add(initialReadyTrack(evt, ((PassEvent) evt).tracks.get(0).getLink().getFrom()));
                    if (prevEvt instanceof TeamRestEvent) //если бригада вышла после отдыха и была далее отправлена пассажиром,
                        rRoute.get(rRoute.size() - 1).setTimeEnd(((PassEvent) evt).getStartTime());// тогда необ-мо изменить время предыдущего трека
                    rRoute.addAll(toRealRoute((PassEvent) evt));
                }
                prevEvt = evt;
            }
            return rRoute;
        }

        RealTeamTrack initialReadyTrack(Event evt, Station s){
            Link link = new Link(s, s, 0L, 0, false);
            Long tStart = 0L;
            if (evt instanceof TeamAssignEvent)
                tStart = ((TeamAssignEvent) evt).getStartTime();
            else if (evt instanceof PassEvent)
                tStart = ((PassEvent) evt).getStartTime();
            Long tEnd = tStart;
            if (evt instanceof TeamAssignEvent)
                tStart -= TeamPlanningParams.defaultStopTimeForTeam;//todo когда будет передаваться правильное время на смену бригад для станции, вычитать s.getNormTime();
            return new RealTeamTrack(link, BaseTeamTrack.State.READY, -1L, -1L, tStart, tEnd);
        }

        RealTeamTrack afterRestReadyTrack(TeamRestEvent evt){
            Station s = evt.getStation();
            Link link = new Link(s, s, 0L, 0, false);
            Long tStart = evt.prevEvent.getEventTime() + (evt.restTimeEnd - evt.restTimeStart);
            return new RealTeamTrack(link, BaseTeamTrack.State.READY, -1L, -1L, tStart, tStart);
        }
    }

    @Getter @NonNull private final FactTeam factTeam;
    @Getter @NonNull private EventContainer events;

    public Long getId() {
        return factTeam.getId();
    }

    @Delegate(types=TeamAttributes.class)
    private TeamAttributes getAttributes() {
        return factTeam;
    }

    
    // private void addEvent(Long time, FactTeamTrack track) {}
    // private void addEvent(Long time, Station station, FactTeamArrive arrive) {}
    // private void addEvent(Long time, §§§FactTeamNextRest§§§ rest) {}

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

    public TeamRestEvent lastRestEvent() {
        return (TeamRestEvent) (lastEvent(TeamRestEvent.class));
    }

    public TeamRestEvent lastReallyRestEvent() {
        TeamRestEvent evt = lastRestEvent();
        if (evt != null
                && evt.restTimeStart != null && evt.restTimeEnd != null
                && !evt.restTimeStart.equals(evt.restTimeEnd)) {
            return evt;
        } else {
            return null;
        }
    }

    public Long lastWorkTime() {
        Long startTime = null, endTime = null;
        for (CommonEventContainer.Event evt : events.getEventStack()) {
            if (evt.isCancelled()) {
                continue;
            } else if (evt instanceof TeamRestEvent) {
                startTime = endTime = null;
            } else if (evt instanceof TeamAssignEvent) {
                TeamAssignEvent aEvt = (TeamAssignEvent) evt;
                if (startTime == null) startTime = aEvt.getStartTime();
                endTime = aEvt.getEndTime();
            } /*else if (evt instanceof PassEvent) {
                PassEvent pEvt = (PassEvent) evt;
                if (startTime == null) startTime = pEvt.getStartTime();
                endTime = pEvt.getEndTime();
            }   */
        }
        return (startTime == null || endTime == null) ? 0 :
            (endTime - startTime);
    }

    public Long timeUntilRest() {
        TeamRestEvent teamRestEvent = lastRestEvent();
        if (teamRestEvent != null) {
            return teamRestEvent.timeUntilRest - lastWorkTime();
        } else {
            return 0L;
        }
    }

    public Long timeOfPresence() {
        Event event = lastEvent();
        if (event != null) {
            return event.getEventTime();
        } else {
            return 0L;
        }
    }

    public boolean teamAlreadyRested() {
        TeamRestEvent evt = lastRestEvent();
        return evt != null
                && evt.restTimeStart != null && evt.restTimeEnd != null
                && !evt.restTimeStart.equals(evt.restTimeEnd);
    }

    public List<Event>
    cancelEvents(Integer runIndex, Integer locoFrameIndex,
                 Integer teamFrameIndex) {
        return events.cancelEvents(runIndex, locoFrameIndex, teamFrameIndex);
    }

    public RealTeam toRealTeam() {
        List<RealTeamTrack> route = events.toRealRoute();
        return route.isEmpty() ? null :
            new RealTeam(getId(), route, factTeam.getDepot(), factTeam);
    }

    private Integer checkTrainEvent(Long trainId, StationPair stp, Class type, Long time) {
        Train train = SchedulingData.getFrameData().getTrain(trainId);
        if (train != null) {
            Train.Event tEvt = train.lastEvent(stp, type);
            if (tEvt != null) {
                Integer index = train.trackIndex(stp);
                Long tTime = (tEvt instanceof TrainArriveEvent)
                        ? ((TrainArriveEvent) tEvt).getTimeEnd()
                        : ((TrainDepartEvent) tEvt).getTimeStart();
                if (! tTime.equals(time)) {
                    LOGGER().warn("Несовпадение времени" +
                            " у фактической бригады " + trainId +
                            " и локомотива " + getId());
                    time = Math.max(time, tTime);
                    if (time > tTime)
                        train.addEvent(tEvt.shiftedBy(time - tTime, -1, -1, -1));
                }
                return index;
            } else {
                LOGGER().warn("Фактическая бригада " + getId() +
                        " содержит неверные данные о следовании" +
                        " с поездом " + trainId);
            }
        }
        return null;
    }

    private void addEvent(FactTeamTrack track) {
        //По локомотиву определить, какой он ведет поезд
        Long locoId = track.getLocoId();
        Long trainId = getFactTrainId(locoId);

        StationPair stp = StationPair.specifier(track.getLink());
        Long time = track.getDepartTime();
        Integer index =
                checkTrainEvent(trainId, stp, TrainDepartEvent.class, time);
        if (trainId != null && index != null)
            addEvent(new TeamDepartEvent(-1, -1, -1, trainId, index));
    }

    private void addEvent(Station station, FactTeamArrive arrive) {
        Long locoId = arrive.getId();
        Long trainId = getFactTrainId(locoId);
        StationPair stp = new StationPair(-1L, station.getId());
        Long time = arrive.getTime();
        Integer index =
                checkTrainEvent(trainId, stp, TrainArriveEvent.class, time);
        if (trainId != null && index != null)
            addEvent(new TeamArriveEvent(-1, -1, -1, trainId, index));
    }

    private void addEvent(Station station, Long timeOfFact) {
        TeamReadyEvent event = new TeamReadyEvent(-1, -1, -1, station.getId(), timeOfFact);
        addEvent(event);
    }

    private Long getFactTrainId(Long locoId) {
        Long trainId = -1L;
        FactLoco loco = SchedulingData.getInputData().getFactLocos().get(locoId);
        if (loco != null) {
            FactLocoArrive locoArrive = loco.getLocoArrive();
            FactLocoTrack locoTrack = loco.getTrack();
            if (locoArrive != null) {
                trainId = locoArrive.getId();
            } else if (locoTrack != null) {
                trainId = locoTrack.getTrainId();
            }
        }
        return trainId;
    }

    /**
     * Конструктор, создающий бригаду с необходимым набором событий
     */
    public Team(FactTeam factTeam) {
        this.factTeam = factTeam;
        this.events = new EventContainer();

        Long operationTime = 0L;
        Long operationStationId = 0L;
        int operation = 0;
        Boolean fromHomeLastPresence = true;
        Long lastRestStation = 0L;
        Long lastRestTime = 0L;
        //3. Находится на домашнем отдыхе, то создаем TeamRestEvent.
        //создать домашний отдых
        //создать явку после домашнего отдыха

        //2. Явка в депо приписки
        if (operation == 2) {
            //создать домашний отдых
            addEvent(
                    new TeamRestEvent(true, operationStationId, -1L, operationTime)
            );
            if (operationTime < SchedulingData.getInputData().getCurrentTime() + 7200L) {
                //создать явку после домашнего отдыха
                addEvent(
                        new TeamPresenceEvent(true, operationStationId, operationTime)
                );
            }
        }

        //5. Прикреплена к локо на станции
        if (operation == 5) {
            //создать отдых (в депо приписки либо в пункте оборота)
            addEvent(
                    new TeamRestEvent(fromHomeLastPresence, lastRestStation, 0L, lastRestTime)
            );
            //создать явку (в депо приписки либо в пункте оборота)
            //создать прикрепление к локо
        }

        //1. Отправлена и ведет локо
        //создать отдых (в депо приписки либо в пункте оборота)
        //создать явку (в депо приписки либо в пункте оборота)

        //0. Отправлена пассажиром
        //создать отдых (в депо приписки либо в пункте оборота)
        //создать явку (в депо приписки либо в пункте оборота)
        //Создать отправку пассажиром

        //6. Прибыла на станцию с локомотивом
        //создать отдых (в депо приписки либо в пункте оборота)
        //создать явку (в депо приписки либо в пункте оборота)

        //7. Прибыла на станцию пассажиром
        //создать отдых (в депо приписки либо в пункте оборота)
        //создать явку (в депо приписки либо в пункте оборота)
        //создать отправку пассажиром

        //4. Находится на отдыхе в пункте оборота
        //создать отдых в пункте оборота

        //8. Явка в пункте оборота
        //создать отдых в пункте оборота
        //создать явку в пункте оборота

        //9. Сдача локо
        //создать отдых (в депо приписки либо в пункте оборота)
        //создать явку (в депо приписки либо в пункте оборота)
        //создать сдачу локомотива


        long timeUntilRest = factTeam.getTimeUntilRest();
        if (factTeam.getTimeOfFact() < SchedulingData.getInputData().getCurrentTime()) {
            timeUntilRest += SchedulingData.getInputData().getCurrentTime() - factTeam.getTimeOfFact();
        }
        TeamRestEvent rest = new TeamRestEvent(timeUntilRest);
        addEvent(rest);

        if (factTeam.getTrack() != null) {
            addEvent(factTeam.getTrack());
        } else if (factTeam.getStation() != null) {
            if (factTeam.getTeamArrive() != null) {
                addEvent(factTeam.getStation(), factTeam.getTeamArrive());
            } else {
                addEvent(factTeam.getStation(), factTeam.getTimeOfFact());
            }
        }
    }

    public  boolean checkForPlanningOnStation(TeamSlot teamSlot){
    //отсеиваем, бригады, которые выходят на работу раньше, чем за 6 часов до начала локо слота и кот. не разрешено работать по участкам обкатки
        Long locoTime = teamSlot.route.get(0).getTimeStart();
        TeamPlanningParams params = new TeamPlanningParams(SchedulingData.getInputData());
        Event ev = lastEvent();
        if (ev.getStation() != null && ev.getEventTime() - locoTime <= params.maxTimeLocoWaitsForTeam && correspondenceInTeamRegion(teamSlot))
            return true;
        return false;
    }

    // Если хотя бы один участок маршрута, который соответствует участку обкатки ЛБ
    private boolean correspondenceInTeamRegion(TeamSlot teamSlot) {
        InputData iData = SchedulingData.getInputData();
        FactTeam factTeam = iData.getFactTeams().get(getId());
        for (TeamRegion teamRegion : factTeam.getTeamWorkRegions()) {
            for (StationPair pair : teamRegion.getStationPairs()) {
                for (SlotTrack slotTrack : teamSlot.route) {
                    if (slotTrack.getLink().getFrom().getId().equals(pair.stationFromId) && slotTrack.getLink().getTo().getId().equals(pair.stationToId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public Long getTeamWaitTime(boolean leaveFromHouse, boolean isNeedMovePassenger, long durationOfRelocationAsPassForLoco, SlotTrack firstSlot){
        Long teamWaitTime = 0L;
        if (leaveFromHouse && isNeedMovePassenger
                && (timeOfPresence() + durationOfRelocationAsPassForLoco) < firstSlot.getTimeStart())//бригада ждет локо
            teamWaitTime = firstSlot.getTimeStart() - (timeOfPresence() + durationOfRelocationAsPassForLoco);
        if (!leaveFromHouse && isNeedMovePassenger
                && lastEvent().getStation().equals(firstSlot.getLink().getFrom()) && timeOfPresence() < firstSlot.getTimeStart())//если бригада на нужной станции и ждет локо
            teamWaitTime = firstSlot.getTimeStart() - timeOfPresence();

        return teamWaitTime;
    }

}
