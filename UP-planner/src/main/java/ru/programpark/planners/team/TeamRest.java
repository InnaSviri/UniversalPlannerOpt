package ru.programpark.planners.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.common.*;

import java.util.Set;

/**
 * Created by oracle on 26.11.2015.
 */
public class TeamRest {
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(TeamRest.class);
        return logger;
    }

    /**
     * добавить событие по пересылке бригад пассажирами В ДЕПО ПРИПИСКИ // условие из 4.3.10.2  стр. 23 или
     * добавить событие длительного отдыха (до конца горизонта) // условие из 4.3.9  стр. 22
     */
    public static void sendTeamsAsPassHomeToRest(Set<Team> teamsInSolution, Long timeStartPlanningIteration) {
        int counter = 0;

        for (Team team : SchedulingData.getFrameData().getTeams()) {
            if (team.lastEvent().getStation() == null) continue;
            if (!teamsInSolution.contains(team)) {
                if (!team.lastEvent().getStation().equals(team.getDepot())) {
                    if (team.teamAlreadyRested()) {//пассажирами В ДЕПО ПРИПИСКИ условие из 4.3.10.2  стр. 23
                        /*Допустим, время отдыха бригады составило t и бригада вышла с отдыха в момент времени T.
                         Тогда если бригада отправляется пассажиром в депо приписки,
                         то надо проверять, что время отправления (по нитке или с грузовым поездом) лежит в
                         и нтервале (T; T + t). Если же время отправления позже, чем T + t,
                         то отправлять бригаду без нитки в момент времени Т.
                         Отправлять бригаду домой, если с момента окончания отдыха прошло больше времени,
                         чем бригада отдыхала*/
                        TeamRestEvent restEvent = team.lastReallyRestEvent();
                        Long tLimitToBeSent = team.timeOfPresence() + (restEvent.restTimeEnd - restEvent.restTimeStart);
                        if (timeStartPlanningIteration > tLimitToBeSent) {
                            PassEvent passEvent = TeamRelocation.sendTeamAsPass(team, team.lastEvent().getStation(),
                                    team.getDepot());

                            Long tStartRest = passEvent.tracks.isEmpty() ?
                                    team.lastEvent().getEventTime() : passEvent.getEndTime();
                            TeamRestEvent rEvent = new TeamRestEvent(team.getDepot().getId(), tStartRest,
                                    SchedulingData.getCurrentFrame().rangeEnd);
                            team.addEvent(passEvent);
                            team.addEvent(rEvent);
                            counter++;
                        }
                    }
                }
            }
        }

        LOGGER().info("Назначено к отправке на отдых на станцию приписки: " +
                LoggingAssistant.countingForm(counter, "бригада", "бригады", "бригад"));
    }

    public static void sendTeamsToHomeRest(Set<Team> teamsInSolution, SchedulingFrame frame) {
        int nAtHome = 0;
        for (Team team : SchedulingData.getFrameData().getTeams()) {//Проверим, не приехала ли бригада домой,
        // если да, то удаляем
            if (teamsInSolution.contains(team) && team.lastEvent().getStation().equals(team.getDepot())) {
                if (team.lastWorkTime() > 0) {
                    Long tStartRest = team.lastEvent().getEventTime();
                    TeamRestEvent rEvent = new TeamRestEvent(team.getDepot().getId(), tStartRest, frame.rangeEnd);
                    rEvent.timeUntilRest = 0L;
                    team.addEvent(rEvent);
                    ++nAtHome;
                }
            }
        }
        LOGGER().info("Ушло на отдых по окончании маршрута на станции приписки: " +
                LoggingAssistant.countingForm(nAtHome, "бригада", "бригады", "бригад"));
    }

    public static void preventiveSendToRestWhereTheyAre() {
        InputData iData = SchedulingData.getInputData();
        for (Team team : SchedulingData.getFrameData().getTeams()) {
            if (team.lastEvent().getStation() == null) continue;
            FactTeam factTeam = iData.getFactTeamById(team.getId());
            Set<Station> nearestTeamChangeStations =
                    TeamChange.findAllNearestTeamChangeStations(team.lastEvent().getStation());
            boolean haveAvailableStations = false;
            for (Station station : nearestTeamChangeStations) {
                boolean correctWorkRegion = false;
                for (TeamRegion region : factTeam.getTeamWorkRegions()) {
                    if (region.containsStation(station)) {
                        correctWorkRegion = true;
                        break;
                    }
                }
                if (!correctWorkRegion) continue;

                long duration = iData.getShortestPath().findDuration(team.lastEvent().getStation(), station) +
                        team.lastEvent().getStation().getNormTime();
                if (duration < team.timeUntilRest()) {
                    haveAvailableStations = true;
                    break;
                }
            }

            if (!haveAvailableStations) {//отправляем на отдых заранее
                sendTeamToRestWhereTheyAre(team);
                LOGGER().debug(String.format("Бригада %d назначена к отправке на отдых на текущей станции %d",
                        team.getId(), team.lastEvent().getStation().getId()));
            }
        }
    }

    private static boolean sendTeamToRestWhereTheyAre(Team team) {
        InputData iData = SchedulingData.getInputData();
        TeamPlanningParams params = new TeamPlanningParams(iData);
        Long tRestBegin = team.lastEvent().getEventTime() + params.technologicalTimeForLocoReturnBeforeRest;
        Long restDuration = Math.max(team.lastWorkTime() / 2, TeamPlanner.params.minRest);
        Long tRestEnd = tRestBegin + restDuration;
        TeamRestEvent event = new TeamRestEvent(team.lastEvent().getStation().getId(), tRestBegin, tRestEnd);
        if (team != null) {
            team.addEvent(event);
            return true;
        }
        return false;
    }

    public static void sendTeamsToRestWhereTheyAre(Set<Team> teamsInSolution, Long timeEndPlanningIteration) {
        int counter = 0;
        InputData iData = SchedulingData.getInputData();
        TeamPlanningParams params = new TeamPlanningParams(iData);

        for (Team team : SchedulingData.getFrameData().getTeams()) {
            if (team.lastEvent().getStation() == null) continue;
            if (!teamsInSolution.contains(team)) {
                if (!team.lastEvent().getStation().equals(team.getDepot())) {
                    TeamRestEvent evt = (TeamRestEvent) team.lastEvent(TeamRestEvent.class);
                    boolean teamAlreadyRested = (evt != null && evt.restTimeStart != null && evt.restTimeEnd != null &&
                            !evt.restTimeStart.equals(evt.restTimeEnd));
                    if (!teamAlreadyRested) {// отправляем на отдых в пункте оборота из 4.3.9  стр. 22
                        if ((timeEndPlanningIteration > team.timeOfPresence()) &&
                                ((timeEndPlanningIteration - team.timeOfPresence()) > params.inactThreshold)) {
                            sendTeamToRestWhereTheyAre(team);
                            counter++;
                        }
                    }
                }
            }
        }

        LOGGER().info("Назначено к отправке на отдых на текущей станции: " +
                LoggingAssistant.countingForm(counter, "бригада", "бригады", "бригад"));
    }

}
