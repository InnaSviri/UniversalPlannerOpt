package ru.programpark.planners.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Pair;
import ru.programpark.entity.util.Time;
import ru.programpark.planners.common.*;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 01.09.15
 * Time: 10:43
 * To change this template use File | Settings | File Templates.
 */
public class TeamRelocation {
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(TeamRelocation.class);
        return logger;
    }

    public static TeamPassDataWrapper ifTeamSentWhenWillItBeThere(Team team, Station to){
        Station from = team.lastEvent().getStation();

        if (team.lastEvent().getStation().equals(to) && !(team.lastEvent() instanceof PassEvent))
            return new TeamPassDataWrapper(team.timeOfPresence(), team.timeOfPresence());

        Slot slotPass = findPassSlot(team, to);
        Train train = findSlotTrain(team, to);

        long slotTimeEnd = -1L;
        long slotTimeStart = -1L;
        if (slotPass != null)
            for (SlotTrack track : slotPass.getRoute().values()) {
                if (to.equals(track.getLink().getTo()))
                    slotTimeEnd = track.getTimeEnd();
                if (from != null && from.equals(track.getLink().getFrom()))
                    slotTimeStart = track.getTimeStart();
            }

        if (slotTimeEnd != -1L && slotTimeStart != -1L){ //отправляем бригаду с пассажирским
            return new TeamPassDataWrapper(slotTimeEnd, slotTimeStart);
        } else if (train != null) { //отправляем бригаду с грузовым
            Pair<Integer, Integer> p1 = train.assignedAtIndex(team.lastEvent().getStation().getId(), to.getId());
            AssignEvent ev1 = ((AssignEvent) train.lastEvent(p1.getSecond()));
            slotTimeEnd = ev1.getTimeEnd();
            Pair<Integer, Integer> p2 = train.assignedAtIndex(team.lastEvent().getStation().getId(), from.getId());
            AssignEvent ev2 = ((AssignEvent) train.lastEvent(p2.getFirst()));
            slotTimeStart = ev2.getTimeStart();
            return new TeamPassDataWrapper(slotTimeEnd, slotTimeStart);
        } else {// отправляем в депо или под локо фиктивно(если не пришло пасс ниток на вход, то просто по кратчайшему пути)
            return null;//значит бригаду переслать нельзя в заданный временной интервал
        }
    }

    public static Slot findPassSlot(Team team, Station destination){
        InputData iData = SchedulingData.getInputData();
        Map<Long, Slot> passSlots = iData.getSlots_pass();// получаем все пассажирские нитки
        Slot bestPassSlot = null;
        long bestTimeDifferent = Long.MAX_VALUE, timeOfPresence = team.timeOfPresence();
        Station s = team.lastEvent().getStation();

        for (Slot passSlot : passSlots.values()) {// идем по ниткам, ищем те, на которой есть нужные станции
        // и все ок с временами отправления-прибытия
            long startTime = 0L;
            long endTime = 0L;
            for (SlotTrack track : passSlot.getRoute().values()) {
                if (track.getLink().getFrom().equals(s)) {
                    startTime = track.getTimeStart();
                }
                if (destination.equals(track.getLink().getTo())) {
                    endTime = track.getTimeEnd();
                }
            }
            if (startTime != 0L && endTime != 0L && (endTime > startTime) && (startTime >= timeOfPresence)) {
                if (startTime - timeOfPresence < bestTimeDifferent) {
                    bestTimeDifferent = startTime - timeOfPresence;
                    bestPassSlot = passSlot;
                }
            }
        }

        return bestPassSlot;
    }

    public static Train findSlotTrain(Team team, Station destination){
        long bestTimeDifference = Long.MAX_VALUE;
        Train bestTrain = null;
        SchedulingFrame frame = SchedulingData.getCurrentFrame();
        long timeOfPresence = team.timeOfPresence();
        Station s = team.lastEvent().getStation();

        if (s.equals(destination))
            return null;

        for (Train train: frame.data.getTrains()) {
            if (!train.assigned(s.getId(), destination.getId()))//поезд не проходит через станции, или проходит,
            // но не назначен локо или бригада
                continue;
            try {
                Train.Event ev = train.lastEvent(new StationPair(s.getId(), null));

                if (ev == null)
                    continue;
                long startTime = ((AssignEvent) ev).getTimeStart();
                if (startTime >= timeOfPresence) {
                    if (startTime - timeOfPresence < bestTimeDifference) {
                        bestTimeDifference = startTime - timeOfPresence;
                        bestTrain = train;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }

        return bestTrain;
    }

    // создание PassEvent под одну пересылку одной бригады пассажиром или "отдыхом" не перемещаясь...
    public static PassEvent sendTeamAsPass(Team team, Station from, Station to){
        InputData iData = SchedulingData.getInputData();
        long timeStart = team.timeOfPresence(), timeEnd = 0L;
        Slot slotPass = findPassSlot(team, to);
        Train train = findSlotTrain(team, to);
        PassEvent passEvent = new PassEvent();

        if (slotPass != null && slotPass.getRoute().get(team.lastEvent().getStation())!= null){
        //отправляем бригаду с пассажирским
            SlotTrack track = slotPass.getRoute().get(from);
            //отправляем бригаду с пассажирским
            while (track != null && !track.getLink().getFrom().equals(to)){
                passEvent.tracks.add(track);
                track = slotPass.getRoute().get(track.getLink().getTo());
            }
        } else if (train != null) { //отправляем бригаду с грузовым
            boolean attach = false;
            Pair<Integer, Integer> p = train.assignedAtIndex(from.getId(), to.getId());
            int j = 0;
            for (int i = p.getFirst(); i <= p.getSecond(); i++) {
                AssignEvent ev = ((AssignEvent) train.lastEvent(i));
                Link link = train.getRoute().get(i).getLink();
                SlotTrack slotTrack = new SlotTrack(link, ev.getTimeStart(), ev.getTimeEnd(), -1L);
                passEvent.tracks.add(slotTrack);
                passEvent.locos.add(new Pair<>(j++, ev.getLocoId()));
                if (link.getFrom().equals(from) && ev.getTimeStart() >= timeStart)
                    attach = true;
                if (attach && link.getTo().equals(to))
                    break;
            }
        } else {// отправляем в депо или под локо фиктивно(если не пришло пасс ниток
        // на вход, то просто по кратчайшему пути)
            List<Link> links = iData.getShortestPath().findRouteByDuration(from, to);
            for (Link link : links) {
                timeEnd = timeStart + link.getDuration(team.timeOfPresence());
                SlotTrack slotTrack = new SlotTrack(link, timeStart, timeEnd, -1L);
                passEvent.tracks.add(slotTrack);
                timeStart = timeEnd;
            }
            String msg = "Бригада отправлена без нитки или поезда. Team " + team.getId() + " на станции " +
                    team.lastEvent().getStation().getName() +
                         " в " + new Time(team.timeOfPresence()).getTimeStamp();
            LoggingAssistant.getTeamResultsWriter().println(msg);
            LOGGER().debug(msg);
        }

        return passEvent;
    }

    //отправка бригада пассажиром под локомотив
    public static void getTeamsInToBeSentAsPassForLoco(List<Decision<TeamSlot, Team>> decisions) {
        int counter = 0;
        for (Decision<TeamSlot, Team> decision : decisions) {
            Team team = decision.team;
            TeamSlot teamSlot = decision.teamSlot;
            if (!team.lastEvent().getStation().equals(teamSlot.route.get(0).getLink().getFrom())){
                //if () {
                    // Ищем возможность отправить бригаду под локомотив, пасс. ниткой или с грузовым поездом.
                    // Время прибытия должно быть не позже времени начала движения локомотива
                    PassEvent passEvent = sendTeamAsPass(team, team.lastEvent().getStation(),
                            teamSlot.route.get(0).getLink().getFrom());
                    team.addEvent(passEvent);
                    counter++;
                //}
            }
        }
        LOGGER().info("Назначено к отправке пассажирами под локомотив: " +
                LoggingAssistant.countingForm(counter, "бригада", "бригады", "бригад"));
    }
}
