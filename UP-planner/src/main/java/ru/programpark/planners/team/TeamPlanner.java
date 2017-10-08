package ru.programpark.planners.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Capacity;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.team.*;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.TrainArrive;
import ru.programpark.entity.train.TrainCategory;
import ru.programpark.entity.util.ShortestPath;
import ru.programpark.planners.common.*;
import ru.programpark.planners.train.TrainPlanner;

import java.util.*;

public class TeamPlanner {
    private InputData inputData;
    private SchedulingFrame frame;
    public static TeamPlanningParams params;
    private List<TeamSlot> teamSlots = null;
    private static TeamPlanner thePlanner = null;
    private TeamAssignment teamAssign;
    private BulkTeamAssign bulkTeamAsign;
    public static boolean DISABLED = (System.getProperty("planners.disable.teamplanner") != null);
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(TeamPlanner.class);
        return logger;
    }

    public static synchronized TeamPlanner instance() {
        if (thePlanner == null) thePlanner = new TeamPlanner();
        return thePlanner;
    }

    public static synchronized TeamPlanner reinstance() {
        return (thePlanner = new TeamPlanner());
    }

    public static synchronized TeamPlanner uninstance() {
        return (thePlanner = null);
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            LOGGER().warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    public SchedulingFrame plan(SchedulingFrame frame) throws InterruptedException {
        this.teamAssign = new TeamAssignment();
        this.bulkTeamAsign = new BulkTeamAssign();

        if (! DISABLED) {
            LOGGER().info("Планирование бригад в интервале " + frame.teamFrameStart + "—" + frame.teamFrameEnd + " ("
                    + frame.teamFrameIndex + ")...");
            this.inputData = SchedulingData.getInputData();
            this.params = new TeamPlanningParams(inputData);
            this.frame = frame;
            if (teamSlots == null)
                teamSlots = new ArrayList<>();
            teamSlots = TeamSlot.initTeamSlot(frame.data);
            if (frame.locoFrameIndex == 0 && frame.teamFrameIndex == 0)
                manualBinding();
            else if (frame.teamFrameStart < inputData.getCurrentTime() + params.teamSchedulingRange)
                teamPlanning(frame.teamFrameStart, frame.teamFrameEnd);
        }

        return frame.nextTeamFrame();
    }

    private void teamPlanning(Long tStart, Long tEnd) throws InterruptedException {
        //планируем бригады, которые по факту не ведут локо
        //раскладываем team slot по станциям начала
        Map<Station, List<TeamSlot>> teamSlotsByStation = formTeamSlotByStation();
        teamObjectCorrections(); //делаем объектные корректировки по бригадам
        TeamRest.preventiveSendToRestWhereTheyAre();

        Set<Team> teamsInSolution = new HashSet<>();
        for (Map.Entry<Station, List<TeamSlot>> teamSlotsOnStation : teamSlotsByStation.entrySet()) {//идем по станциям
            checkInterrupted();
            //оставляем только те бригады, которые могут быть привязаны вплоть до окончания фремени интервала + 6 часов
            List<Team> planningTeams = filterTeamsOnStation(teamSlotsOnStation.getValue(), tEnd);
            List<Decision<TeamSlot, Team>> decisions = null;
            if (params.bulkPlanning) {
                decisions = bulkTeamAsign.getDecisions(teamSlotsOnStation.getKey(), teamSlotsOnStation.getValue(), planningTeams, tEnd);
            } else {
                //Решаем задачу о назначениях на станции
                decisions = teamAssign.getDecisions(teamSlotsOnStation.getValue(), planningTeams, tEnd);
            }
            teamsInSolution.addAll(getTeamsInDecision(decisions));
            //Если найдены бригады, которые необходимо переслать пассажирами под локомотив
            TeamRelocation.getTeamsInToBeSentAsPassForLoco(decisions);
            //Формируем результат планирования decisions и обновляем
            Map<Decision<Long, Long>, Boolean> result = updateStateOfAssignedTeams(decisions, false, false);
        }
        LOGGER().info("Отправка бригад на отдых...");
        TeamRest.sendTeamsAsPassHomeToRest(teamsInSolution, tStart);
        TeamRest.sendTeamsToRestWhereTheyAre(teamsInSolution, tEnd);
    }

    private Map<Station, List<TeamSlot>> formTeamSlotByStation(){
        Map<Station, List<TeamSlot>> teamSlotsByStation = new HashMap<>();
        for (TeamSlot teamSlot : teamSlots) {
            if (teamSlot.route.size() == 0)
                continue;
            List<TeamSlot> listInMap = teamSlotsByStation.get(teamSlot.route.get(0).getLink().getFrom());
            if (listInMap == null)
                listInMap = new ArrayList<>();
            listInMap.add(teamSlot);
            teamSlotsByStation.put(teamSlot.route.get(0).getLink().getFrom(), listInMap);
        }

        return teamSlotsByStation;
    }

    private List<Team> filterTeamsOnStation(List<TeamSlot> teamSlotsOnStation, Long timeEnd) {
        List<Team> planningTeams = new ArrayList<>();
        planningTeams.clear();//формируем список бригад, которые могут быть назначены на данной станции
        planningTeams.addAll(SchedulingData.getFrameData().getTeams());
        for (TeamSlot teamSlot : teamSlotsOnStation) { //избавляемся от треков с идентичной станцией начала и конца
            Iterator<SlotTrack> iterator = teamSlot.route.iterator();
            while (iterator.hasNext()) {
                SlotTrack track = iterator.next();
                if (track.getLink().getFrom().equals(track.getLink().getTo()))
                    iterator.remove();
            }
        }
        Iterator<Team> iter = planningTeams.iterator();
        while (iter.hasNext()) { //оставляем только те бригады, которые могут быть привязаны
            Team team = iter.next(); //вплоть до окончания фремени интервала + 6 часов
            if (team.timeOfPresence() > timeEnd + params.maxTimeLocoWaitsForTeam) {
                iter.remove();
            } else {
                ShortestPath sp = inputData.getShortestPath();
                if (team.lastEvent().getStation() == null) {
                    iter.remove();
                } else {
                    long dur = sp.findDuration(team.lastEvent().getStation(),
                            teamSlotsOnStation.get(0).route.get(0).getLink().getFrom());
                    if (dur > params.maxTimeLocoWaitsForTeam)
                        iter.remove();//отбрасываем бригады, кот. не успеют доехать за 6 часов к началу первого teamSlot
                }
            }
        }

        return planningTeams;
    }

    private void teamObjectCorrections(){
        for (Train train : frame.data.getTrains()) {
            int index = train.getUnassignedTeamIndex();
            if (index < train.getRoute().size()) {
                Train.Event event = train.lastEvent(index);
                if (event instanceof HintEvent) {
                    HintEvent hintEvent = (HintEvent) event;
                    Long locoId = hintEvent.getLocoId();
                    Long teamId = hintEvent.getTeamId();
                    if (teamId != null) { // Если null — то это подсказка от предварит. планирования лок-в.
                        Team team = frame.data.getTeam(teamId);
                        TeamSlot teamSlot = getTeamSlotByLocoId(locoId);
                        Map<Decision<Long, Long>, Boolean> result = updateStateOfAssignedTeams(
                                new ArrayList<>(Arrays.asList(new Decision<>(teamSlot, team))), true, false);
                        if (result.size() == 0)
                            LOGGER().warn("Не удалось произвести корректировочное назначение бригады " +
                                    teamId + " на поезд " + train.getTrainId() + ", локомотив " + locoId +
                                    " на станции " + train.getStationTo(index));
                    }
                }
            }
        }
    }

    public void cancelEvents(Team team, Integer runIndex, Integer locoFrameIndex, Integer teamFrameIndex) {
        if (team == null) return;
        LOGGER().debug("Отмена событий для бригады " + team.getId() + " начиная с интервала #" + runIndex + "-" +
                locoFrameIndex + "-" + teamFrameIndex);
        List<Team.Event> cancelled = team.cancelEvents(runIndex, locoFrameIndex, teamFrameIndex);
        for (Team.Event evt : cancelled) {
            if (evt instanceof TeamAssignEvent) {
                Long trainId = ((TeamAssignEvent) evt).getTrainId();
                Integer start = ((TeamAssignEvent) evt).getStartIndex();
                Train train = SchedulingData.getFrameData().getTrain(trainId);
                TrainPlanner.instance().cancelEvents(train, start, runIndex, evt.getLocoFrameIndex(),
                        evt.getTeamFrameIndex());
            }
        }
    }

    public void updateEventsForCorrections() {
        InputData input = SchedulingData.getInputData();
        SchedulingData data = SchedulingData.getFrameData();
        for (PinnedTeam pTeam : input.getPinnedTeamList()) {
            Long trainId = pTeam.getTrainId(), stId = pTeam.getStationId(),
                    locoId = pTeam.getLocoId(), teamId = pTeam.getTeamId();
            Train train = data.getTrain(trainId);
            Team team = data.getTeam(teamId);
            if (train == null) {
                LOGGER().warn("Корректировка указывает неизвестный поезд " + trainId);
                continue;
            } else if (team == null) {
                LOGGER().warn("Корректировка поезда " + trainId + " указывает неизвестную бригаду " + teamId);
                continue;
            }
            StationPair stp = new StationPair(stId, null);
            Integer index = train.trackIndex(stp);
            if (index == null || index < 0) {
                LOGGER().warn("Корректировка поезда " + trainId + " указывает станцию " + stId +
                        ", отсутствующую в его маршруте");
                continue;
            } else if (data.getLoco(locoId) == null) {
                LOGGER().warn("Корректировка поезда " + trainId + " указывает неизвестный локомотив " + locoId);
                continue;
            }
            AssignEvent aEvt = train.findAssign(index, null, null, -1L, null);
            if (aEvt != null) {
                Integer runIndex = aEvt.getRunIndex(),
                        locoFrameIndex = aEvt.getLocoFrameIndex(), teamFrameIndex = aEvt.getTeamFrameIndex();
                if (locoFrameIndex == 0 && teamFrameIndex == 0) {
                    LOGGER().warn("Корректировка поезда " + trainId + " не может быть произведена, т. к." +
                            " отменяет фактическое назначение");
                    continue;
                } else {
                    cancelEvents(team, runIndex, locoFrameIndex, teamFrameIndex);
                    TrainPlanner.instance().cancelEvents(train, index, runIndex, locoFrameIndex, teamFrameIndex);
                }
            }
            stp = StationPair.specifier(train.getRoute().get(index).getLink());
            train.addEvent(new HintEvent(stp, locoId, teamId, train.lastEvent(index)));
        }
    }

    /**
     * Метод обновляет бригады, которые были использованы при движении локо путем определения станции смены бригады,
     * т.е. меняется и бригада (нахождение на новой станции) и меняется слот (удаляется использованный фрагмент маршрута),
     * меняются teamSlots (убираем запланированные до конца бригадные слоты),
     * меняются teams (проработавшие бригады оказываются на новой станции и с уменьшенным остатком времени до отдыха
     */
    public int counterForNotAssignedTeam = 0;
    public StringBuffer sb = new StringBuffer();
    private Map<Decision<Long, Long>, Boolean> updateStateOfAssignedTeams(List<Decision<TeamSlot, Team>> decisions,
                                                                          boolean firstBinding, boolean manualBinding) {
        LOGGER().info("Обновление данных по привязанным бригадам...");
        Map<Decision<Long, Long>, Boolean> result = new HashMap<>();
        sortDecisionsAccountingTrainPriority(decisions);

        Set<Team> teamsInSolution = new HashSet<>();
        for (Decision<TeamSlot, Team> decision : decisions) {
            TeamSlot teamSlot = decision.teamSlot;
            Team team = decision.team;
            if (teamSlot.route.isEmpty())
                continue;
            Train train = frame.data.getTrain(teamSlot.route.get(0).trainId);
            SlotTrack firstSlot = teamSlot.route.get(0);
            CapacityManager capacityManager = new CapacityManager();
            boolean changeTeamForArrivingTrain = false;
            FactTrain factTrain = inputData.getFactTrains().get(teamSlot.route.get(0).trainId);
            //Если ручная привязка, то делаем дополнительные изменения, если train_arrive, то можно менять бригаду
            // в случае, если не хватит времени на работу до станции смены бригады
            if (factTrain != null && firstBinding && factTrain.getTrainState() instanceof TrainArrive)
                changeTeamForArrivingTrain = true;

            //продолжаем цикл до тех пор, пока не кончится отведенное рабочее время
            Capacity temporaryCapacity = capacityManager.foundCapacity(firstSlot, firstBinding, teamSlot,
                    Math.max(team.lastEvent().getEventTime(), firstSlot.getTimeStart()));
                    //team.lastEvent().getEventTime());
            Long temporaryShiftTime = 0L;
            if (temporaryCapacity != null)
                temporaryShiftTime = temporaryCapacity.getStartTime() - firstSlot.getTimeStart();

            Long whenTeamWillBeRelocated = team.timeOfPresence(), whenDepartsAsPass = -1L;
            if (team.lastEvent() instanceof PassEvent) {
                whenTeamWillBeRelocated = ((PassEvent) team.lastEvent()).getEndTime();
                whenDepartsAsPass = ((PassEvent) team.lastEvent()).getStartTime();
            }
            Station stationForChangeTeam = TeamChange.getTeamChangeStation(team, teamSlot,
                    whenTeamWillBeRelocated, whenDepartsAsPass, /*temporaryShiftTime*/0L);
            if (stationForChangeTeam == null && firstBinding && !changeTeamForArrivingTrain) {
                counterForNotAssignedTeam++;
                sb.append("" + team.getId() + "\n");
            }

            //Если не удалось найти станцию смены бригады и это не случай с train_arrive
            //обрезаем маршрут до след. станции смены бригады
            if (stationForChangeTeam == null && !changeTeamForArrivingTrain) {
                boolean found = false;
                Iterator<SlotTrack> iter = teamSlot.route.iterator();
                while (iter.hasNext()) {
                    SlotTrack track = iter.next();
                    if (found)
                        break;
                    if (track.getLink().getTo().getNormTime() != 0L)
                        found = true;
                    iter.remove();
                }
            }

            //В любом случае, если не удлось найти станцию смены бригады, данную бригаду не подвязываем
            if (stationForChangeTeam == null)
                continue;
            Capacity сapacity = capacityManager.foundCapacity(firstSlot, firstBinding, teamSlot, team.timeOfPresence());
            Integer categoryPriority = 100;
            Integer trainPriority = 100;
            if (factTrain != null) {
                TrainCategory trainCategory = inputData.getTrainCategories().get(factTrain.getCategory());
                categoryPriority = trainCategory.getPriority() != null ? trainCategory.getPriority() : 100;
                trainPriority = factTrain.getPriority() != null ? factTrain.getPriority() : 100;
            }

            long shiftTime = 0L;
            BaseLocoTrack.State locoState = ((AssignEvent) train.lastEvent(0, AssignEvent.class)).getLocoState();
            if (сapacity != null && !(locoState == BaseLocoTrack.State.RESERVE)) {
                Map<Long, Capacity> shiftCapacities = capacityManager.getFreeCapacityMap(firstSlot.getLink(),
                        сapacity.getStartTime(), train.getTrainId(), categoryPriority, trainPriority);
                Set<Long> trainIds = new HashSet<>();
                trainIds.addAll(сapacity.trainIds);
                capacityManager.takeCapacities(firstSlot, shiftCapacities);
                if (shiftCapacities.size() > 1) {
                    capacityManager.reassignTrains(shiftCapacities);
                    for (Map.Entry<Long, Capacity> trainCapacity : shiftCapacities.entrySet()) {
                        if (trainCapacity.getKey().equals(train.getTrainId()))
                            continue;
                        Train shiftTrain = frame.data.getTrain(trainCapacity.getKey());
                        int startIndex = -1;
                        for (Train.EventContainer eventContainer : shiftTrain.getRoute()) {
                            startIndex++;
                            if (eventContainer.getLink().equals(firstSlot.getLink()))
                                break;
                        }
                        if (startIndex != -1) {
                            long oldTimeStart = ((TrackEvent) shiftTrain.getRoute().get(startIndex).
                                    lastEvent(AssignEvent.class)).getTimeStart();
                            shiftTrain.shiftBy(trainCapacity.getValue().getStartTime() - oldTimeStart, startIndex);
                        }
                    }
                }
            }

            if (сapacity != null)  //Если есть пропускная способность
                shiftTime = сapacity.getStartTime() - firstSlot.getTimeStart();

            boolean needShift = false;
            if (locoState == BaseLocoTrack.State.RESERVE) {
                needShift = TeamChange.needShiftForReserveLoco(teamSlot, stationForChangeTeam);
                if (!needShift) {
                    shiftTime = 0L;
                }
            }

            //последовательно двигаемся по маршруту, обновляя Team, TeamSlot
            Iterator<SlotTrack> iterator = teamSlot.route.iterator();
            Long currentTrainiId = null;
            Integer firstIndex = -1;
            Integer lastIndex = -1;

            while (iterator.hasNext()) {
                SlotTrack slotTrack = iterator.next();
                if (firstIndex == -1) firstIndex = slotTrack.trackIndex;
                lastIndex = slotTrack.trackIndex;
                currentTrainiId = slotTrack.trainId;
                //оказывается, не вышли за пределы рабочего времени - прибавляем трек к результату, отрезаем из teamSlot
                //текущий трек, изменяем team (уменьшаем оставшиеся рабочее время и меняем станцию
                iterator.remove();
                if (slotTrack.getLink().getTo().equals(stationForChangeTeam))
                    break; //Смотрим, вдруг эта станция явдяется конечной станцией для бригад, тогда все
            }

            Loco loco = frame.data.getLoco(teamSlot.locoId);
            List<ReferenceEvent> shiftedTrainEvents = loco.unassignedTeamEvents();
            Map<Long, Integer> unassignedIndexes = new HashMap<>();

            for (ReferenceEvent event : shiftedTrainEvents) {
                Train unassignedTrain = frame.data.getTrain(event.getTrainId());
                unassignedIndexes.put(event.getTrainId(), unassignedTrain.getUnassignedTeamIndex());
            }

            for (ReferenceEvent shiftedTrainEvent : shiftedTrainEvents) {
                Train shiftedTrain = frame.data.getTrain(shiftedTrainEvent.getTrainId());
                if (shiftTime == 0L) {
                    long startTime = Math.max(firstSlot.getTimeStart(), team.timeOfPresence());
                    shiftTime = startTime - firstSlot.getTimeStart();
                    if (manualBinding)
                        shiftTime = 0L;

                }
                if (shiftTime != 0)
                    shiftTime = shiftedTrain.shiftBy(shiftTime, unassignedIndexes.get(shiftedTrain.getTrainId()));
            }

            TeamAssignEvent event = new TeamAssignEvent(teamSlot.locoId, currentTrainiId, firstIndex, lastIndex);
            team.getEvents().addEvent(event);
            train.updateAssign(firstIndex, lastIndex, null, null, team.getId(), BaseTeamTrack.State.AT_WORK);
            train.setUnassignedTeamIndex(lastIndex + 1);


            for (int i = firstIndex; i <= lastIndex; i++) {
                TrackEvent trackEvent = (TrackEvent) train.lastEvent(i, TrackEvent.class);
                if (trackEvent != null && locoState != BaseLocoTrack.State.RESERVE) {
                    trackEvent.getTimeStart();
                    long timeStart = TrainPlanner.CAPACITY_PERIOD*(trackEvent.getTimeStart()/TrainPlanner.CAPACITY_PERIOD);
                    Link link = inputData.getLinks().get(trackEvent.getStationPair());
                    Capacity capacity = link.getCapacities().get(timeStart);
                    if (capacity != null && capacity.getCapacity() > 0)
                        capacity.setCapacity(capacity.getCapacity() - 1);
                }
            }

            teamsInSolution.add(team);
            result.put(new Decision<>(decision.teamSlot.locoId, team.getId()), true);
        }
        TeamRest.sendTeamsToHomeRest(teamsInSolution, frame);

        return result;
    }

    // ручная привязка
    public void manualBinding() {//Сначала обрабатываем бригады, которые по факту ведут локо, они должны
    // продолжать свою работу как можно дольше, то есть либо до станции смены бригады, либо до окончания слота
        List<Decision<TeamSlot, Team>> facts = new ArrayList<>();
        for (Team readyTeam : SchedulingData.getFrameData().getTeams()) {
            FactTeam factTeam = inputData.getFactTeams().get(readyTeam.getId());
            Long locoId = 0L;
            if (factTeam.getLocoId() != null && factTeam.getLocoId() > 0L)
                locoId = factTeam.getLocoId();
            if (factTeam.getTrack() != null)
                locoId = factTeam.getTrack().getLocoId();
            if (locoId == 0L)
                continue;
            //Если бригада может ехать дальше, то пытаемся вручную привязать. Если не может, то не привязываем
            //Если найденный факт о бригаде привязан к существующему локомотиву, то как бы создаем решение
            //о назначении бригады на локо
            for (TeamSlot teamSlot : teamSlots) {
                if (teamSlot.locoId.equals(locoId)) {
                    if (factTeam.getTeamArrive() == null || !(factTeam.getTeamArrive() instanceof FactTeamArrive)) {
                        facts.add(new Decision<>(teamSlot, readyTeam));
                    } else {//надо проверить, если ли станция смены бригады, если эта бригада будет по прежнему вести
                            //этот локо, если есть, то бригаду подвязываем
                        Station station = TeamChange.getTeamChangeStation(readyTeam, teamSlot, 0L, -1L, 0L);
                        if (station != null)
                            facts.add(new Decision<>(teamSlot, readyTeam));
                    }
                }
            }
        }
        updateStateOfAssignedTeams(facts, true, true);
    }

    private TeamSlot getTeamSlotByLocoId(Long locoId) {
        for (TeamSlot teamSlot : teamSlots)
            if (teamSlot.locoId.equals(locoId))
                return teamSlot;

        return null;
    }

    private Set<Team> getTeamsInDecision(List<Decision<TeamSlot, Team>> decisions) {
        Set<Team> readyTeamsInSolution = new HashSet<>();

        for (Decision<TeamSlot, Team> decision : decisions) {
            Team readyTeam = decision.team;
            readyTeamsInSolution.add(readyTeam);
        }

        return readyTeamsInSolution;
    }

    private void sortDecisionsAccountingTrainPriority(List<Decision<TeamSlot, Team>> decisions){
        Collections.sort(decisions, new Comparator<Decision<TeamSlot, Team>>() {
            @Override
            public int compare(Decision<TeamSlot, Team> o1, Decision<TeamSlot, Team> o2) {
                FactTrain train1 = inputData.getFactTrains().get(o1.teamSlot.trainId);
                FactTrain train2 = inputData.getFactTrains().get(o2.teamSlot.trainId);
                int result = 0;

                if (train1 != null && train2 != null) {
                    //сравнение поездов по приоритету категории
                    Integer categoryPriorioty1 = inputData.getTrainCategoryById(train1.getCategory()).getPriority();
                    Integer categoryPriorioty2 = inputData.getTrainCategoryById(train2.getCategory()).getPriority();
                    result = categoryPriorioty1.compareTo(categoryPriorioty2);
                    //дополнительное сравнение по приоритету поезда
                    if (result == 0) {
                        Integer trainPriorioty1 = (train1.getPriority() == null) ? params.defaultTrainPriority : train1.getPriority();
                        Integer trainPriorioty2 = (train2.getPriority() == null) ? params.defaultTrainPriority : train2.getPriority();
                        result = trainPriorioty1.compareTo(trainPriorioty2);
                    }
                    //резерв с большим приоритетом
                } else if (train1 != null) {
                    result = 1;
                } else if (train2 != null) {
                    result = -1;
                }
                //далее сортировать поезда по времени готовности к отправлению
                if (result == 0)
                    result = o1.teamSlot.route.get(0).getTimeStart().compareTo(o2.teamSlot.route.get(0).getTimeStart());
                //далее сортировать поезда по времени локомотивного факта
                if (result == 0) {
                    FactLoco locoOne = inputData.getFactLocos().get(o1.teamSlot.locoId);
                    FactLoco locoTwo = inputData.getFactLocos().get(o2.teamSlot.locoId);
                    result = locoOne.getTimeOfLocoFact().compareTo(locoTwo.getTimeOfLocoFact());
                }
                //далее сортировать поезда по времени поездного факта
                if (result == 0 && train1 != null && train2 != null) {
                    if (train1.getTrainState() != null && train2.getTrainState() != null) {
                        result = train1.getTrainState().getTime().compareTo(train2.getTrainState().getTime());
                    } else if (train1.getTrainState() != null ) {
                        result = -1;
                    } else if (train2.getTrainState() != null) {
                        result = 1;
                    }
                }

                return result;
            }
        });
    }
}
