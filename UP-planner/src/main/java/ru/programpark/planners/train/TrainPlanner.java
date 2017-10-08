package ru.programpark.planners.train;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Capacity;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.team.BaseTeamTrack;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.common.*;
import ru.programpark.planners.loco.LocoPlanner;
import ru.programpark.planners.team.TeamPlanner;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TrainPlanner {
    InputData inputData;
    public static Long CAPACITY_PERIOD = 300L;
    public static Long LINE_PERIOD = 7200L;

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(TrainPlanner.class);
        return logger;
    }

    private void CHECK_INTERRUPTED() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            LOGGER().warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    public Map<StationPair, Link> calcCapacities() {
        InputData inputData = SchedulingData.getInputData();
        Map<StationPair, Link> links = inputData.getLinks();
        Map<Long, Slot> slots = inputData.getSlots();

        LOGGER().info("Расчёт пропускных способностей...");
        for (Slot slot : slots.values()) {
            for (SlotTrack track : slot.getRoute().values()) {
                Link link = links.get(new StationPair(track.getLink().getFrom().getId(), track.getLink().getTo().getId()));
                Long startInterval = (track.getTimeStart() / CAPACITY_PERIOD) * CAPACITY_PERIOD;
                Capacity originCapacity = link.getOriginCapacities().get(startInterval);
                Capacity capacity = link.getCapacities().get(startInterval);
                if (originCapacity == null) {
                    originCapacity = new Capacity(startInterval, CAPACITY_PERIOD.intValue(), 1, link.getFrom(), link.getTo());
                    capacity = new Capacity(startInterval, CAPACITY_PERIOD.intValue(), 1, link.getFrom(), link.getTo());
                    originCapacity.slots = slot.getSlotId().toString();
                    link.getOriginCapacities().put(startInterval, originCapacity);
                    link.getCapacities().put(startInterval, capacity);
                } else {
                    originCapacity.setCapacity(originCapacity.getCapacity() + 1);
                    originCapacity.slots = originCapacity.slots + "," + slot.getSlotId().toString();
                    capacity.setCapacity(capacity.getCapacity() + 1);
                }
            }
        }
        //printCapacity(inputData);
        return links;
    }

    public void plan() throws InterruptedException {
        CHECK_INTERRUPTED();
        LOGGER().info("Планирование поездов...");
        long[] trainCounts = SchedulingData.getInputData().countFactTrains();
        String trainCountsMsg = "Фактические поезда: " + trainCounts[0] + " train_arrive, " +
            trainCounts[1] + " train_ready, " + trainCounts[2] + " train_depart";
        LOGGER().info(trainCountsMsg);
        LoggingAssistant.EXTERNAL_LOGGER().info(trainCountsMsg);

        long curTime = SchedulingData.getInputData().getCurrentTime();
        for (Train train : SchedulingData.getFrameData().getTrains()) {
            CHECK_INTERRUPTED();
            long time;
            if (train.getPreRoute().hasEvents()) {
                Train.Event evt = train.lastEvent(-1);
                if (evt instanceof TrainDepartEvent) {
                    time = evt.getEventTime();
                } else {
                    time = Math.max(evt.getEventTime(), curTime);
                }
            } else {
                time = Long.MIN_VALUE;
            }
            train.setStartTime(time);
            int index = -1;
            for (Train.EventContainer events : train.getRoute()) {
                if (time == Long.MIN_VALUE) {
                    ++index;
                    if (events.hasEvents()) {
                        if (events.lastEvent() instanceof TrainDepartEvent) {
                            time = events.getEventTime();
                            train.setStartTime(time);
                        } else {
                            time = Math.max(events.getEventTime(), curTime);
                            train.setStartTime(time);
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                Link link = events.getLink();
                long duration = link.getDuration(time);
                events.addEvent(new AssignEvent(StationPair.specifier(link), time, (time += duration)));
                //if (train.getTrainId() == 200021038902L) {
                //    System.out.println("Train " + train.getTrainId() + ", link " + link + ", times: from " + (time - duration) + " to " + time);
                //}
            }
            if (index >= 0) {
                train.setUnassignedLocoIndex(index);
                train.setUnassignedTeamIndex(index);
            }
        }
    }

    public void cancelEvents(Train train, Integer indexStart, Integer runIndex,
                             Integer locoFrameIndex, Integer teamFrameIndex) {
        if (train == null) return;
        LOGGER().debug("Отмена событий для поезда " + train.getTrainId() +
                           " на перегонах " + indexStart +
                           ".." + (train.getRoute().size() - 1) +
                           " начиная с интервала #" + runIndex +
                           "-" + locoFrameIndex + "-" + teamFrameIndex);
        Map<Long, AssignEvent> locoEvts = new HashMap<>();
        Map<Long, AssignEvent> teamEvts = new HashMap<>();
        List<List<Train.Event>> cancelled =
            train.cancelEvents(indexStart, runIndex,
                               locoFrameIndex, teamFrameIndex);
        for (List<Train.Event> evts : cancelled) {
            for (Train.Event evt : evts) {
                if (evt instanceof AssignEvent) {
                    AssignEvent aEvt = (AssignEvent) evt;
                    Long locoId = aEvt.getLocoId(),
                         teamId = aEvt.getTeamId();
                    if (locoId != null) {
                        if (locoEvts.containsKey(locoId)) {
                            AssignEvent aEvt2 = locoEvts.get(locoId);
                            locoEvts.put(locoId, BaseEvent.min(aEvt, aEvt2));
                        } else {
                            locoEvts.put(locoId, aEvt);
                        }
                    }
                    if (teamId != null) {
                        if (teamEvts.containsKey(teamId)) {
                            AssignEvent aEvt2 = teamEvts.get(teamId);
                            teamEvts.put(teamId, BaseEvent.min(aEvt, aEvt2));
                        } else {
                            teamEvts.put(teamId, aEvt);
                        }
                    }
                }
            }
        }
        SchedulingData data = SchedulingData.getFrameData();
        for (Map.Entry<Long, AssignEvent> idEvt : locoEvts.entrySet()) {
            Loco loco = data.getLoco(idEvt.getKey());
            AssignEvent aEvt = idEvt.getValue();
            LocoPlanner.instance().cancelEvents(loco, runIndex, aEvt.getLocoFrameIndex(), aEvt.getTeamFrameIndex());
        }
        for (Map.Entry<Long, AssignEvent> idEvt : teamEvts.entrySet()) {
            Team team = data.getTeam(idEvt.getKey());
            AssignEvent aEvt = idEvt.getValue();
            TeamPlanner.instance().cancelEvents(team, runIndex, aEvt.getLocoFrameIndex(), aEvt.getTeamFrameIndex());
        }
        if (! train.hasEvents() || train.isTentative(true)) {
            data.delTrain(train);
            LOGGER().warn("Поезд " + train.getTrainId() + " удалён вследствие отмены всех привязок");
        }
    }

    private Train preplannedTrain(Train train) {
        Train preplanned = new Train(train);
        Long locoId = -1L;
        for (int i = -1; i < preplanned.getRoute().size(); ++i) {
            Train.EventContainer track, track0;
            if (i < 0) {
                track = preplanned.getPreRoute();
                track0 = train.getPreRoute();
            } else {
                track = preplanned.getRoute().get(i);
                track0 = train.getRoute().get(i);
            }
            Train.Event evt = track0.lastEvent(new TrackEvent() {
                @Override public boolean equals(Object x) {
                    return (x instanceof TrainArriveEvent ||
                            x instanceof TrainReadyEvent ||
                            x instanceof TrainDepartEvent);
                }
            });
            if (evt != null) track.addEvent(evt);
            AssignEvent aEvt = track0.firstEvent(AssignEvent.class);
            if (aEvt != null) {
                track.addEvent(aEvt);
                if (aEvt.getLocoId() != null) {
                    if (aEvt.getLocoId().equals(locoId)) continue;
                    locoId = aEvt.getLocoId();
                }
                if (aEvt.getLocoState() != null &&
                        aEvt.getLocoState().equals(BaseLocoTrack.State.RESERVE)) {
                    HintEvent hEvt =
                        new HintEvent(aEvt.getRunIndex(),
                                      aEvt.getLocoFrameIndex(),
                                      aEvt.getTeamFrameIndex(),
                                      aEvt.getStationPair(),
                                      locoId, aEvt.getLocoState(),
                                      null, null, aEvt);
                    track.addEvent(hEvt);
                    preplanned.setTentative(i == 0);
                }
            }
        }
        return preplanned;
    }

    public void resetTrainsToPreplanned(SchedulingData data) {
        for (Train train : data.getTrains()) {
            data.putTrain(preplannedTrain(train));
        }
    }

    public Train planAuxTrain(Long id, List<Link> route, Long timeStart,
                              Long locoId, BaseLocoTrack.State locoState,
                              Long teamId, BaseTeamTrack.State teamState) {
        Train train = new Train(id, route, timeStart);
        Long time = timeStart;
        for (Train.EventContainer events : train.getRoute()) {
            Link link = events.getLink();
            long duration = link.getDuration(time);
            AssignEvent evt = new AssignEvent(StationPair.specifier(link),
                                              time, (time += duration));
            evt.setLocoId(locoId);
            evt.setLocoState(locoState);
            evt.setTeamId(teamId);
            evt.setTeamState(teamState);
            events.addEvent(evt);
        }
        if (locoId != null) train.setUnassignedLocoIndex(route.size());
        if (teamId != null) train.setUnassignedTeamIndex(route.size());
        return train;
    }

    private static TrainPlanner thePlanner = null;

    public static synchronized TrainPlanner instance() {
        if (thePlanner == null) thePlanner = new TrainPlanner();
        return thePlanner;
    }

    public static synchronized TrainPlanner reinstance() {
        return (thePlanner = new TrainPlanner());
    }

    public static synchronized TrainPlanner uninstance() {
        return (thePlanner = null);
    }

}

//public static void printCapacity(InputData inputData) {
        /*StationPair pair = new StationPair(2000036518L, 2000036784L);
        Link link = inputData.getLinkByStationPair(pair);
        Iterator<Capacity> iterator = link.getCapacities().values().iterator();
        Capacity firstCapacity = iterator.next();
        Capacity lastCapacity = null;
        while (iterator.hasNext()) {
            lastCapacity = iterator.next();
        }
        long timeStart = firstCapacity.getStartTime();
        if(lastCapacity == null) {
            lastCapacity = firstCapacity;
        }
        long timeEnd = lastCapacity.getStartTime();
        SimpleDateFormat formatter_date = new SimpleDateFormat("dd:MM");
        SimpleDateFormat formatter_time = new SimpleDateFormat("HH:mm");
        String firstStr = "";
        String secondStr = "";
        String thirdStr = "";
        while (timeStart <= timeEnd) {
            Date date = new Date(timeStart * 1000L);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.HOUR, -1);
            firstStr += formatter_date.format(cal.getTime()) + "|";
            secondStr += formatter_time.format(cal.getTime())+ "|";
            Capacity capacity = link.getCapacities().get(timeStart);
            if (capacity == null) {
                thirdStr += "     |";
            } else {
                thirdStr += "    " + capacity.getCapacity() + "|";
            }
            timeStart += 600L;
        }
        System.out.println(firstStr);
        System.out.println(secondStr);
        System.out.println(thirdStr);
        System.out.print("\n");*/
//}

/*

    public void plan(InputData inputData, OutputData outputData) {
        this.inputData = inputData;
        Map<Long, OneTask> oneTasks = inputData.getOneTasks();

        LOGGER().info("Планирование поездов...");
        for (OneTask oneTask : oneTasks.values()) {
            if (oneTask.getType() == OneTask.OneTaskType.TASK) {
                continue;
            }
            RealTrain realTrain = planOneTask(oneTask);
            if (realTrain != null) {
                outputData.getSlotTrains().put(realTrain.getTrainId(), realTrain);
            }
        }
    }

    public RealTrain planOneTask(OneTask oneTask) {
        OneTaskTrack prevTrack = null;
        boolean trainCut = false;
        Iterator<OneTaskTrack> trackIterator = null;
        trackIterator = oneTask.getMainRoute().iterator();
        while (trackIterator.hasNext()) {
            OneTaskTrack track = trackIterator.next();
            track.setSlotId(-1L);
            if (!trainCut) {
                if (prevTrack != null) {
                    track.setTimeStart(prevTrack.getTimeEnd());
                } else {
                    track.setTimeStart(oneTask.getStartTime());
                }
                if (prevTrack == null &&
                        (oneTask.getType() == OneTask.OneTaskType.FACT_DEPART || oneTask.getType() == OneTask.OneTaskType.FACT_READY)) {
                    track.setTimeEnd(track.getTimeStart() + track.getLink().getAveDurationForHour(track.getTimeStart()));
                }
                else if (prevTrack != null && track.getLink().getFrom().getNormTime() == 0) {
                    track.setTimeEnd(track.getTimeStart() + track.getLink().getAveDurationForHour(track.getTimeStart()));
                } else {
                    long timeStart = track.getTimeStart();
                    track.setTimeStart(timeStart + track.getLink().getFrom().getNormTime());

                    Capacity freeCapacity = getFreeCapacity(track.getLink(), track.getTimeStart());

                    while (freeCapacity != null && !checkLine(track.getLink().getFrom(), freeCapacity.getStartTime())) {
                        freeCapacity = getFreeCapacity(track.getLink(), track.getTimeStart(), freeCapacity);
                    }

                    if (freeCapacity != null) {
                        //Если данная станция это станция смены бригад
                        //то проверяем, будет ли на стации во время прибытия поезда свободный станционный путь
                        if (!checkLine(track.getLink().getFrom(), freeCapacity.getStartTime())) {
                            LOGGER().warn("Нет свободных путей на станции " + track.getLink().getFrom().getId() +
                                              " для выполнения задания " + oneTask.getId());
                        } else {
                            List<LineBusy> lineBusies = lineBusyByStation.get(track.getLink().getFrom());
                            if (lineBusies == null) {
                                lineBusies = new ArrayList<>();
                            }
                            //занимаем станционный путь
                            lineBusies.add(new LineBusy(track.getLink().getFrom(), freeCapacity.getStartTime(), freeCapacity.getStartTime() + LINE_PERIOD));
                            lineBusyByStation.put(track.getLink().getFrom(), lineBusies);
                        }
                    }

                    Capacity capacity = takeCapacity(track.getLink(), freeCapacity, oneTask.getId());

                    if (capacity != null) {
                        track.setTimeStart(capacity.getStartTime());
                        track.setTimeEnd(track.getTimeStart() + track.getLink().getAveDurationForHour(track.getTimeStart()));
                    } else {
                        //Опрелеляем, были ли раньше интервалы
                        //основываясь на наличии интервалов в originCapacity
                        Capacity originCapacity = takeOriginCapacity(track);
                        if (originCapacity == null) {
                            //если раньне не было интервалов, то прокладываем маршрут
                            track.setTimeEnd(track.getTimeStart() + track.getLink().getAveDurationForHour(track.getTimeStart()));
                        } else {
                            //если раньше были интервалы, но они кончились, то прекращаем планирование поезда и обрезаем лишние треки
                            trainCut = true;
                        }
                    }

                }
                prevTrack = track;
            }
            if (trainCut) {
                //обрезаем лишние интервалы
                trackIterator.remove();
            }
        }

        if (oneTask.getMainRoute().size() == 0) {
            return null;
        }
        List<RealTrainTrack> trainTracks = new LinkedList<>();
        for (OneTaskTrack track : oneTask.getMainRoute()) {
            RealTrainTrack trainTrack = new RealTrainTrack(track.getLink(), track.getTimeStart(), track.getTimeEnd(), track.getSlotId());
            trainTracks.add(trainTrack);
        }
        RealTrain realTrain = new RealTrain(oneTask.getId(), oneTask.getId(), trainTracks, inputData.getFactTrainById(oneTask.getId()), oneTask);
        //outputData.getSlotTrains().put(realTrain.getTrainId(), realTrain);
        return realTrain;
    }

    public void basePlan(InputData inputData, OutputData outputData)
    throws InterruptedException {
        this.inputData = inputData;
        CHECK_INTERRUPTED();
        List<OneTask> oneTasks = new ArrayList<>();
        oneTasks.addAll(inputData.getOneTasks().values());
        Collections.sort(oneTasks, new Comparator<OneTask>() {
            @Override
            public int compare(OneTask o1, OneTask o2) {
                return o1.getStartTime().compareTo(o2.getStartTime());
            }
        });

        LOGGER().info("Планирование поездов...");
        long[] trainCounts = inputData.countFactTrains();
        String trainCountsMsg = "Фактические поезда: " + trainCounts[0] + " train_arrive, " +
            trainCounts[1] + " train_ready, " + trainCounts[2] + " train_depart";
        LOGGER().info(trainCountsMsg);
        LoggingAssistant.EXTERNAL_LOGGER().info(trainCountsMsg);

        for (OneTask oneTask : oneTasks) {
            CHECK_INTERRUPTED();
            if (oneTask.getType() == OneTask.OneTaskType.TASK) {
                continue;
            }
            if (inputData.getFactTrains().get(oneTask.getId()) == null) {
                continue;
            }
            LOGGER().trace("Задание: " + oneTask);
            RealTrain realTrain = planBaseOneTask(inputData, oneTask);
            if (realTrain != null) {
                outputData.getSlotTrains().put(realTrain.getTrainId(), realTrain);
            }
        }

        printCapacity(inputData);
    }


    public RealTrain planBaseOneTask(InputData inputData, OneTask oneTask) {
        if (oneTask == null || oneTask.getMainRoute() == null) {
            return null;
        }
        this.inputData = inputData;
        List<RealTrainTrack> trainTracks = new LinkedList<>();
        OneTaskTrack priorTrack = null;
        long trackTimeStart = 0L;
        long trackTimeEnd = 0L;
        for (OneTaskTrack track : oneTask.getMainRoute()) {

            if (priorTrack == null) {
                FactLoco factLoco = null;
                for (FactLoco loco : inputData.getFactLocos().values()) {
                    Long trainId = null;
                    if (loco.getLocoArrive() != null) {
                        trainId = loco.getLocoArrive().getId();
                    }
                    if (loco.getTrack() != null) {
                        trainId = loco.getTrack().getTrainId();
                    }
                    if (trainId != null && oneTask.getId().equals(trainId)) {
                        factLoco = loco;
                        break;
                    }
                }
                if (oneTask.getType() == OneTask.OneTaskType.FACT_READY) {
                    trackTimeStart = oneTask.getStartTime();

                    if (trackTimeStart < inputData.getCurrentTime()) {
                        trackTimeStart = inputData.getCurrentTime();
                    }
                        //timeStart += calcShiftTimeWithCapacity(track.getLink(), timeStart, oneTask.getId());
                } else if (oneTask.getType() == OneTask.OneTaskType.FACT_ARRIVE) {
                    trackTimeStart = oneTask.getStartTime();
                    if (oneTask.getMainRoute().size() > 1) {
                        trackTimeStart = oneTask.getStartTime();
                        if (trackTimeStart < inputData.getCurrentTime()) {
                            trackTimeStart = inputData.getCurrentTime();
                        }

                        if (!check(factLoco, oneTask.getMainRoute().get(0))) {
                            trackTimeStart += track.getLink().getFrom().getNormTime();
                        }

                        //timeStart += calcShiftTimeWithCapacity(track.getLink(), timeStart, oneTask.getId());
                    }

                } else {
                    trackTimeStart = oneTask.getStartTime();
                }
            } else {
                trackTimeStart = priorTrack.getTimeEnd();
            }
            track.setTimeStart(trackTimeStart);
            trackTimeEnd = trackTimeStart + track.getLink().getAveDurationForHour(track.getTimeStart());
            track.setTimeEnd(trackTimeEnd);
            track.trainId = oneTask.getId();
            RealTrainTrack trainTrack = new RealTrainTrack(track);
            trainTracks.add(trainTrack);
            priorTrack = track;
        }
        inputData.getOneTasks().put(oneTask.getId(), oneTask);
        RealTrain realTrain = new RealTrain(oneTask.getId(), oneTask.getId(), trainTracks, inputData.getFactTrainById(oneTask.getId()), oneTask);
        return realTrain;
    }

    private long calcShiftTimeWithCapacity(Link link, long startTime, Long trainId) {
        startTime -= 15L * 60L;
        long shiftTime = 0L;
        Capacity freeCapacity = getFreeCapacity(link, startTime);
        if (freeCapacity != null) {
            takeCapacity(link, freeCapacity, trainId);
            shiftTime = freeCapacity.getStartTime() - startTime;
        }

        return shiftTime;
    }


    private Capacity takeCapacity(OneTaskTrack track) {
        for (Capacity capacity : track.getLink().getCapacities().values()) {
            if (capacity.getCapacity() != 0 && capacity.getStartTime() >= track.getTimeStart()) {
                capacity.setCapacity(capacity.getCapacity() - 1);
                return capacity;
            }
        }
        return null;
    }

    private Capacity takeCapacity(Link link, Capacity sourceCapacity, Long trainId) {
        for (Capacity capacity : link.getCapacities().values()) {
            if (capacity.equals(sourceCapacity)) {
                capacity.setCapacity(capacity.getCapacity() - 1);
                if (trainId != null) capacity.trainIds.add(trainId);
                return capacity;
            }
        }
        return null;
    }

    private Capacity getFreeCapacity(Link link, long timeStart) {
        for (Capacity capacity : link.getCapacities().values()) {
            if (capacity.getCapacity() != 0 && capacity.getStartTime() >= timeStart) {
                return capacity;
            }
        }
        return null;
    }

    private Capacity getFreeCapacity(Link link, long timeStart, Capacity priorCapacity) {
        boolean foundPriorCapacity = false;
        for (Capacity capacity : link.getCapacities().values()) {
            if (capacity.equals(priorCapacity)) {
                foundPriorCapacity = true;
                continue;
            }
            if (capacity.getCapacity() != 0 && capacity.getStartTime() >= timeStart && foundPriorCapacity) {
                return capacity;
            }
        }
        return null;
    }


    private Capacity takeOriginCapacity(OneTaskTrack track) {
        for (Capacity capacity : track.getLink().getOriginCapacities().values()) {
            if (capacity.getCapacity() != 0 && capacity.getStartTime() >= track.getTimeStart()) {
                capacity.setCapacity(capacity.getCapacity() - 1);
                return capacity;
            }
        }
        return null;
    }

    private boolean checkLine(Station station, long timeStart) {
        List<LineBusy> lineBusies = lineBusyByStation.get(station);
        if (lineBusies == null) {
            return true;
        }
        int counter = 0;
        for (LineBusy lineBusy : lineBusies) {
            if (lineBusy.timeStart <= timeStart && lineBusy.timeEnd >= timeStart) {
                counter++;
            }
        }
        if (counter >= station.getLines().size()) {
            return false;
        } else {
            return true;
        }
    }

    private Map<Station, List<LineBusy>> lineBusyByStation = new HashMap<>();

    / **
      * Класс, содержаший информацию о занятых станционных путях
      * должен быть поиск по станции и времени
      * /
    private class LineBusy {
        Station station;
        long timeStart;
        long timeEnd;

        private LineBusy(Station station, long timeStart, long timeEnd) {
            this.station = station;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LineBusy lineBusy = (LineBusy) o;

            if (timeEnd != lineBusy.timeEnd) return false;
            if (timeStart != lineBusy.timeStart) return false;
            if (station != null ? !station.equals(lineBusy.station) : lineBusy.station != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = station != null ? station.hashCode() : 0;
            result = 31 * result + (int) (timeStart ^ (timeStart >>> 32));
            result = 31 * result + (int) (timeEnd ^ (timeEnd >>> 32));
            return result;
        }
    }

    public void updateRealTeamTimes(OutputData outputData) {
        for (RealLoco loco : outputData.getSlotLocos().values()) {
            for (RealLocoTrack track : loco.getRoute()) {
                RealTeamTrack currentTeamTrack = null;
                findTeamTrack : for (RealTeam team : outputData.getSlotTeams().values()) {
                    for (RealTeamTrack teamTrack : team.getRoute()) {
                        if (track.getTrainId().equals(teamTrack.trainId)
                                && loco.getRealLocoId().equals(teamTrack.getLocoId())
                                && track.getLink().equals(teamTrack.getLink())) {
                            currentTeamTrack = teamTrack;
                            break findTeamTrack;
                        }
                    }
                }

                if (currentTeamTrack != null) {
                    currentTeamTrack.setTimeStart(track.getTimeStart());
                    currentTeamTrack.setTimeEnd(track.getTimeEnd());
                }
            }
        }
    }

*/
