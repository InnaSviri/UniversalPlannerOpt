package ru.programpark.planners.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Capacity;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Pair;
import ru.programpark.planners.assignment.*;
import ru.programpark.planners.common.*;
import ru.programpark.planners.util.MatrixOperationsUtil;
import ru.programpark.planners.util.WaitRatingUtil;

import java.util.*;

/**
 * User: oracle
 * Date: 31.07.15. Refactored: 01.12.2015
 * Вспомогательные функции TeamPlanner: получение общего назначения по станции, вычисление полезности и решение задачи о назначениях
 */
public class TeamAssignment {
    private InputData iData;
    private TeamPlanningParams params;
    private CapacityManager capManager;
    private AssignmentProblem assignmentProblem;
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(TeamAssignment.class);
        return logger;
    }

    public TeamAssignment() {
        this.iData = SchedulingData.getInputData();
        this.params = new TeamPlanningParams(iData);
        this.assignmentProblem = new AuctionAssignment();
        assignmentProblem.setParams(new AssignmentParams(iData));
        this.capManager = new CapacityManager();
    }

    // Получает на вход список бригадных слотов и готовых бригад. возвращаем соответствие
    public List<Decision<TeamSlot, Team>> getDecisions(final List<TeamSlot> teamSlots, final List<Team> teams,
                                                       long timeEndPlanningIteration) {
        List<Decision<TeamSlot, Team>> decisions = new ArrayList<>();
        Map<List<Integer>, PartialAssignment> utilities = new HashMap<>();
        LOGGER().info("Вычисление полезности для " +
                LoggingAssistant.countingForm(teamSlots.size(), "локомотива", "локомотивов") + ", " +
                LoggingAssistant.countingForm(teams.size(), "бригады", "бригад") + "...");

        for (int i = 0; i < teamSlots.size(); i++) {
            TeamSlot loco = teamSlots.get(i);
            Station locoLocation = loco.route.get(0).getLink().getFrom();
            Long locoStartTime = loco.route.get(0).getTimeStart();
            Train train = SchedulingData.getFrameData().getTrain(loco.route.get(0).trainId);
            if (!loco.checkForPlanningOnStation(timeEndPlanningIteration))
                continue; //отсеиваем пустые слоты и слоты, начало которых позже конца интервала
            for (int j = 0; j < teams.size(); j++) {
                Team team = teams.get(j);
                Station teamLocation = team.lastEvent().getStation();
                if (!team.checkForPlanningOnStation(loco))//отсеиваем, бригады, которые выходят на работу раньше,
                    continue; //чем за 6 часов до начала локо слота  и которым не разрешено работать по участкам обкатки
                SlotTrack firstSlot = loco.getFirstSlotTrack(); //Выбирается 1-ый слот, с которого будем начата работа
                boolean needToMoveAsPass = !teamLocation.equals(locoLocation);
                boolean leaveFromHouse = teamLocation.equals(team.getDepot());
                Long whenTeamWillBeRelocated =team.timeOfPresence(), whenDepartsAsPass = -1L;
                if (needToMoveAsPass) {
                    TeamPassDataWrapper wrapper = TeamRelocation.ifTeamSentWhenWillItBeThere(team, locoLocation);
                    if (wrapper == null)  //бригаду нельзя переслать
                        continue;
                    whenTeamWillBeRelocated = wrapper.whenArrives;
                    whenDepartsAsPass = wrapper.whenDeparts;
                    if (whenTeamWillBeRelocated - locoStartTime > params.maxTimeLocoWaitsForTeam)
                        continue;  //можно отправить но слишком поздно приедет к локо
                }
                //вычисляем, сколько времени потребуется бригаде до отправления(с учетом пересылки пассажирами под локо)
                Long start = Math.max(whenTeamWillBeRelocated, firstSlot.getTimeStart());
                Long teamWaitTime = team.getTeamWaitTime(leaveFromHouse, needToMoveAsPass,
                        whenTeamWillBeRelocated, firstSlot);
                //ищем пропускную способность
                Capacity capacity = capManager.foundCapacity(firstSlot, false, loco, start);
                //время сдвига локомотива
                Long shiftTime = (capacity == null) ? 0L : capacity.getStartTime() - firstSlot.getTimeStart();
                //Определяем станцию смену бригад
                Station changeStation = TeamChange.getTeamChangeStation(team, loco, whenTeamWillBeRelocated,
                        whenDepartsAsPass, shiftTime);

                if (changeStation == null)
                    continue; //не найдена станция смены бригады

                BaseLocoTrack.State locoState = ((AssignEvent) train.lastEvent(0, AssignEvent.class)).getLocoState();
                if (locoState == BaseLocoTrack.State.RESERVE) {
                    //нет в фактах и тасках -> резервный
                    boolean needShift = TeamChange.needShiftForReserveLoco(loco, changeStation);
                    if (!needShift) {
                        shiftTime = 0L;
                    }
                }

                //Вычисляем время движения до станции смены бригады
                long timeBeforeChange = TeamChange.getTimeBeforeChange(firstSlot, loco, shiftTime, changeStation);
                if (team.timeUntilRest() < teamWaitTime + timeBeforeChange)
                    continue; //Если время ожидания превысит оставшееся рабочее время, не используем эту бригаду
                long workTimeLeft = team.timeUntilRest() - timeBeforeChange - teamWaitTime;// Оставшееся рабочее время
                Double accountWaitTime = WaitRatingUtil.accountTeamWaitTime(teamSlots, team, loco,
                        whenTeamWillBeRelocated, timeEndPlanningIteration);
                if (accountWaitTime.equals(Double.NEGATIVE_INFINITY))//Добавить описание отсеиваемых бригад
                    continue;
                Pair<Double, String> uPair = getUtil(team, loco, accountWaitTime, workTimeLeft, needToMoveAsPass);
                PartialAssignment p = new PartialAssignment(new Integer[]{i,j}, uPair.getFirst());
                utilities.put(p.getIndices(), p);
            }
        }
        logDebug(utilities, decisions, teamSlots, teams);

        return decisions;
    }

    private Pair<Double, String> getUtil(Team team, TeamSlot loco, Double accountWaitTime, Long workTimeLeft,
                                         Boolean isNeedMovePassenger){
        TeamPlanningParams params = new TeamPlanningParams(SchedulingData.getInputData());
        String sumStr = "";
        Station locoLocation = loco.route.get(0).getLink().getFrom();
        Station teamLocation = team.lastEvent().getStation();
        double util = 0D;
        //Если необходимо перемещать пассажиром, то плохо
        if (isNeedMovePassenger) {
            util += params.coeff.K1;
            sumStr += "k1:" + params.coeff.K1;
        }
        util += accountWaitTime*params.coeff.ACCOUNT_WAIT_TIME;
        sumStr += " accountWaitTime:" + accountWaitTime;
        util += params.coeff.K4 * workTimeLeft; //учитываем оставшееся рабочее время
        sumStr += " k4:" + params.coeff.K4 * workTimeLeft;
        // Если бригада находится не в своем депо и едет в сторону, противоположную своему депо, то даем ей большой штраф
        boolean notDepotDirection = getNotDepotDirection(team, loco);
        if (notDepotDirection) {
            util += params.coeff.K5 * 300;
            sumStr += " k5:" + params.coeff.K5 * 300;
        }
        // Если бригада находится не в своем депо, то даем плюс ближайшим поездам ПОСЛЕ этой бригады (Войтенко 14.10.2015)
        if (!locoLocation.equals(team.getDepot()) && locoLocation.equals(teamLocation) &&
                loco.route.get(0).getTimeStart() > team.timeOfPresence()) {
            Long waitTime = loco.route.get(0).getTimeStart() - team.timeOfPresence();
            double waitUtil = 1 - waitTime / 15000.0;
            util += params.coeff.K6 * waitUtil;
            sumStr += " k6:" + params.coeff.K6 * waitUtil;
        }

        if (LOGGER().isTraceEnabled())
            LOGGER().trace(String.format("Полезность для локомотива %d, бригады %d: {%s} = %.6f",
                    loco.locoId, team.getId(), sumStr, util));
        else
            LOGGER().debug(String.format("Полезность для локомотива %d, бригады %d = %.6f",
                    loco.locoId, team.getId(), util));

        if (loco.locoId.equals(200021209753L)){
            System.out.println("@@@Полезность для локомотива " + loco.locoId + ", бригады " + team.getId() + " : " + util);
            System.out.println("@@@Локомотива находится на станции " + locoLocation.getId() + ", а бригада - " + teamLocation.getId());
        }

        return new Pair<>(util + 5, sumStr);
    }

    private boolean getNotDepotDirection(Team team, TeamSlot loco){
        boolean notDepotDirection = true;
        Station locoLocation = loco.route.get(0).getLink().getFrom();

        if (locoLocation.equals(team.getDepot())) {
            notDepotDirection = false;
        } else {
            for (SlotTrack slotTrack : loco.route) {
                if (slotTrack.getLink().getTo().equals(team.getDepot())) {
                    notDepotDirection = false;
                    break;
                }
            }
        }

        return notDepotDirection;
    }

    private void logDebug(Map<List<Integer>, PartialAssignment> utilities, List<Decision<TeamSlot, Team>> decisions,
                          List<TeamSlot> teamSlots, List<Team> teams){
        if (utilities.size() > 0) {
            LOGGER().info("Расчёт назначений...");
            List<Integer> dimensions = new ArrayList<>();
            dimensions.add(teamSlots.size());
            dimensions.add(teams.size());
            PartialAssignmentMap utility = new PartialAssignmentMap(dimensions, utilities);
            LOGGER().trace("totalUtility = " + utility);
            List<PartialAssignmentMap> result = assignmentProblem.decisions(utility);
            double [][] u = MatrixOperationsUtil.assignmentArrayTo2DMatrix(utility);
            LOGGER().trace(Arrays.deepToString(u));
            if (result.size() > 0) {
                PartialAssignmentMap bestDecision = result.get(0);
                LOGGER().trace("bestDecision = " + bestDecision);
                for (PartialAssignment assignment : bestDecision.partialAssignments()) {
                    decisions.add(new Decision<>(teamSlots.get(assignment.getIndices().get(0)),
                            teams.get(assignment.getIndices().get(1))));
                }
            } else
                LOGGER().info("Не найдено решений");
        } else
            LOGGER().info("Не найдено полезных назначений");
        if (decisions.size() > 0) {
            if (LOGGER().isDebugEnabled()) {
                String assignIds = null;
                for (Decision<TeamSlot, Team> decision : decisions)
                    assignIds = ((assignIds == null) ? "" : (assignIds + ", ")) +
                            String.format("%d ↔ %d", decision.teamSlot.locoId, decision.team.getId());
                LOGGER().debug("Привязка локомотивов к бригадам: " + assignIds);
            } else
                LOGGER().info("Найдено " + LoggingAssistant.countingForm(decisions.size(),
                        "назначение", "назначения", "назначений"));
        }

    }
}