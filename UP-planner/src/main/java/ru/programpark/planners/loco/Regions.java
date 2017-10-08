package ru.programpark.planners.loco;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.loco.BaseLocoTrack.State;
import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.ShortestPath;
import ru.programpark.entity.util.FilterIterator;
import ru.programpark.planners.common.*;
import ru.programpark.planners.team.TeamPlanner;
import ru.programpark.planners.train.TrainPlanner;

import java.io.Serializable;
import java.util.*;

public class Regions {

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(Regions.class);
        return logger;
    }

    private void CHECK_INTERRUPTED() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            LOGGER().warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    private static void logError(Exception e) {
        LoggingAssistant.logException(e);
        LOGGER().error("Exception:" + e.getLocalizedMessage());
        e.printStackTrace();
    }

    private InputData input;
    private SchedulingData data;
    private SchedulingFrame currentFrame;
    private LocoPlanner.Params params;
    private Map<Long, RegionAgent> regionAgents;
    private Boolean preplanning = false;

    public Regions() {
        this.input = SchedulingData.getInputData();
        this.data = SchedulingData.getFrameData();
    }

    private static final long seed = 1372764451251218432L; // См. https://xkcd.com/221/
    private static Random random = new Random(seed);

    private Map<Long, RegionAgent> makeRegionAgents() {
        Map<Long, RegionAgent> agents = new HashMap<>();
        for (LocoRegion region : input.getLocoRegions().values()) {
            agents.put(region.getId(), new RegionAgent(region));
        }
        return agents;
    }

    // Полезные данные для сообщений между главным агентом и агентами плечей
    // о привязке локомотивов
    static class Assign implements Serializable {
        Long regionId;
        Assign(Long regionId) {
            this.regionId = regionId;
        }

        // При отправке агенту плеча — удобоназначаемые поезда; при возврате
        // от агента плеча — неназначенные поезда.
        List<Long> trains = null;
        // При отправке — удобоназначаемые локомотивы; при возврате —
        // неназначенные локомотивы.
        List<Long> locos = null;
        // При возврате — назначение локомотивов на поезда и параметры
        // запросов на планирование пересылки резервом и отставки на ТО.
        LinkedHashMap<Long, List<Long>> map = null;

        static Assign forRequest(Long regionId) {
            Assign assign = new Assign(regionId);
            assign.trains = new ArrayList<Long>();
            assign.locos = new ArrayList<Long>();
            return assign;
        }

        static Assign forResponse(Long regionId) {
            Assign assign = new Assign(regionId);
            assign.trains = new ArrayList<Long>();
            assign.locos = new ArrayList<Long>();
            assign.map = new LinkedHashMap<Long, List<Long>>();
            return assign;
        }
    }

    // Полезные данные для сообщений между агентами плечей и главным агентом
    // о планировании дополнительных поездов (для локомотивов резервом и/или
    // отставки на ТО)
    static class PlanTrain implements Serializable {
        Long regionId;
        PlanTrain(Long regionId) { this.regionId = regionId; }

        // Локомотив
        Long locoId;
        // Станции, между которыми локомотив пересылается резервом
        Long stationFrom, stationTo;
        // Диапазон времени, в к-й должен укладываться план
        Long timeStart, timeEnd;
        // При возврате — длина маршрута поезда, или 0, если его не удалось
        // запланировать
        Long distance;
        // При возврате — является ли направление маршрута грузовым
        Boolean haulingDirection;
    }

    private boolean isTeamAssigned(Train.EventContainer events) {
        if (TeamPlanner.DISABLED || preplanning) {
            return true;        // Тривиальным образом
        } else {
            AssignEvent evt = (AssignEvent) events.lastEvent(AssignEvent.class);
            long teamSchedRangeEnd =
                (TeamPlanner.params == null) ? currentFrame.rangeEnd :
                (currentFrame.rangeStart + TeamPlanner.params.teamSchedulingRange);
            return (evt == null || evt.getLocoId() == null ||
                        evt.getTeamId() != null ||
                        evt.getEventTime() >= teamSchedRangeEnd);
        }
    }

    private int distributeTrains(Map<Long, Assign> assigns, int turn,
                                 boolean mayFilter) {
        // Поезд помещается в список удобоназначаемых слотов для первого плеча,
        // через которое он проходит, если время начала движения поезда
        // находится в пределах текущего интервала планирования, либо если оно
        // лежит до начала интервала, и это первый интервал, либо если оно лежит
        // до начала интервала, и поезд не удалось назначить в предыдущем
        // интервале.  (Если какой-то перегон на маршруте поезда принадлежит
        // нескольким плечам, выбирается плечо с наибольшим приоритетом.)
        int nSelected = 0;
        for (Train train : data.getTrains()) {
            Integer startIndex = train.getUnassignedLocoIndex();
            if (startIndex < 0 || startIndex >= train.getRoute().size())
                continue;
            Train.EventContainer firstTrack = train.getRoute().get(startIndex);
            long tStart;
            if (train.isTentative(true)) {
                tStart = train.getStartTime();
            } else {
                Train.EventContainer prevTrack;
                if (startIndex > 0) {
                    prevTrack = train.getRoute().get(startIndex - 1);
                    if (! isTeamAssigned(prevTrack)) {
                        // Надо подождать, пока БП не назначит бригады на весь
                        // фрагмент поезда на предыдущем плече.
                        continue;
                    }
                } else {
                    prevTrack = train.getPreRoute();
                }
                tStart = prevTrack.getTrainReadyTime();
            }
            List<LocoRegionPriority> priorities =
                firstTrack.getLocoRegionPriorities();
            if (mayFilter && turn > priorities.size() * 2)
                continue;       // Перепробовали все плечи по 2 раза — без толку
            long regId = priorities.get(0).getRegion().getId();
            if (regId < 0L) {
                LOGGER().warn("Поезд " + train.getTrainId() +
                              " не может быть отнесён к к.-л. плечу");
                train.setUnassignedLocoIndex(train.getRoute().size());
            } else if (tStart < currentFrame.locoFrameEnd &&
                    tStart < currentFrame.rangeEnd) {
                Assign assign = assigns.get(regId);
                if (assign == null) {
                    assign = Assign.forRequest(regId);
                    assigns.put(regId, assign);
                }
                assign.trains.add(train.getTrainId());
                ++nSelected;
            } else if (tStart >= currentFrame.rangeEnd) {
                LOGGER().warn("Поезд " + train.getTrainId() +
                    " следует через тяговое плечо " + regId +
                    " вне времени планирования: " +
                    String.format("%d ≥ %d", tStart, currentFrame.rangeEnd));
                train.setUnassignedLocoIndex(train.getRoute().size());
            }
        }
        return nSelected;
    }

    private List<LocoRegion>
    locoDistributaryRegions(Loco loco, Boolean firstTurn) {
        // TBD: выбор плеча с ослабленным ограничением (согласно параметру),
        // при котором годится как плечо, на котором собственно находится
        // локомотив, так и все соседние.
        List<LocoRegion> regions0 = loco.getLocoRegions();
        if (regions0 == null) {
            return Collections.<LocoRegion>emptyList();
        } else if (firstTurn) {
            Long stId = loco.firstEvent(-1, -1, -1).getStationId();
            Station st = data.getStation(stId);
            if (st != null) {
                List<LocoRegion> regions = new ArrayList<>(regions0);
                regions.retainAll(st.getRegions());
                return regions;
            } else {
                return Collections.<LocoRegion>emptyList();
            }
        } else {
            return regions0;
        }
    }

    private int distributeLocos(Map<Long, Assign> assigns, int turn) {
        // Локомотив, курсирующий в пределах одного плеча, помещается в список
        // удобоназначаемых локомотивов для данного плеча.  Локомотив,
        // курсирующий в пределах нескольких плечей, в первом туре назначений
        // отдаётся тому плечу, на котором находится согласно фактическим
        // данным, а если находится на стыковой станции — то одному из плечей
        // случайно, с вероятностью, пропорциональной количеству локомотивных
        // слотов для этого плеча. В последующих турах он отдаётся любому из
        // плечей, на которых курсирует, вне зависимости от своего
        // местонахождения.
        int nSelected = 0;
        for (Loco loco : data.getLocos()) {
            LocoAssignEvent aEvt =
                (LocoAssignEvent) (loco.lastEvent(LocoAssignEvent.class));
            Long tStart;
            if (aEvt == null) {
                tStart = loco.lastEvent().getLocoReadyTime();
            } else if (! isTeamAssigned(aEvt.dereference())) {
                // Надо подождать, пока БП не назначит бригады на тот
                // фрагмент поезда, где его вёз данный локомотив.
                continue;
            } else if (aEvt.getLocoFrameIndex() < currentFrame.locoFrameIndex) {
                tStart = aEvt.getLocoReadyTime();
            } else {
                continue;       // Локомотив уже привязан в этом интервале
            }
            if (tStart < currentFrame.rangeEnd) {
                List<LocoRegion> locoRegions =
                    locoDistributaryRegions(loco, turn == 1);
                Long regId = -1L;
                if (locoRegions.size() == 1) {
                    regId = locoRegions.get(0).getId();
                } else {
                    int nTrains = 0, _rand;
                    Assign asg;
                    for (LocoRegion reg : locoRegions) {
                        if ((asg = assigns.get(reg.getId())) != null) {
                            nTrains += asg.trains.size();
                        }
                    }
                    _rand = (nTrains > 0) ? random.nextInt(nTrains) : 0;
                    nTrains = 0;
                    for (LocoRegion reg : locoRegions) {
                        if (nTrains > _rand) break;
                        regId = reg.getId();
                        if ((asg = assigns.get(regId)) != null) {
                            nTrains += asg.trains.size();
                        }
                    }
                }
                if (regId >= 0L) {
                    Assign assign = assigns.get(regId);
                    if (assign == null) {
                        assign = Assign.forRequest(regId);
                        assigns.put(regId, assign);
                    }
                    assign.locos.add(loco.getId());
                    ++nSelected;
                } else {
                    LOGGER().warn("Локомотив " + loco.getId() +
                                  " не может быть отнесён к к.-л. плечу");
                }
            } else if (tStart >= currentFrame.rangeEnd) {
                LOGGER().warn("Время готовности локомотива " + loco.getId() +
                              " находится за горизонтом планирования: " +
                              tStart + " ≥ " + currentFrame.rangeEnd);
            }
        }
        return nSelected;
    }

    // Номер поезда для локомотивов, отправляемых резервом, из настраиваемого
    // диапазона:
    long paramReserveLocoTrainId(long prevLongId, int direction) {
        long minId = params.minReserveTrainId;
        long maxId = params.maxReserveTrainId;
        long prefix = params.reserveTrainIdPrefix;
        long m = (long) Math.pow(10d, Math.ceil(Math.log10(maxId + 1d)));
        long mSeq = (long) Math.pow(10d, params.reserveTrainIdSeqDigits);
        long prevId = prevLongId % m;
        long seq = (prevLongId / m) % mSeq;
        long id = prevId + 1;
        if (prevId < minId || prevId >= maxId) {
            id = minId;
            ++seq;
        }
        id += seq * m + prefix * m * mSeq;
        // Номера на 0 и 9 могут приходиться на границы диапазонов подкатегорий
        // резерва, поэтому мы их не используем.
        if (id % 10 == 9) ++id;
        if (id % 10 == 0) ++id;
        // Если известна чётность поезда, требуется соблюдать её в номере
        if (id >= 0 && direction >= 0 && id % 2 != direction)
            id = paramReserveLocoTrainId(id, direction);
        return id;
    }

    private Long trainId = -1L;

    private Train planReserveTrain(Long locoId, Long stFromId, Long stToId,
                                   Long timeStart, Long timeEnd) {
        Station stFrom = input.getStationById(stFromId);
        Station stTo = input.getStationById(stToId);
        List<Link> route =
            input.getShortestPath().findRouteByDuration(stFrom, stTo);
        return route.isEmpty() ? null :
            planReserveTrain(locoId, route, timeStart);
    }

    Train planReserveTrain(Long locoId, List<Link> route, Long timeStart) {
        Link link0 = route.get(0);
        if (link0 == null || link0.getDirection() == null) {
            LOGGER().warn("Обнаружена ошибка в данных перегона: " + link0);
            return null;
        }
        int dir = link0.getDirection() % 2;
        if (dir < 0)
            LOGGER().warn("Для пересылки резервом лок-ва " + locoId +
                          " будет создан поезд с номером произвольной" +
                          " четности, т. к. для его начального перегона" +
                          " не указано направление");
        this.trainId = paramReserveLocoTrainId(this.trainId, dir);
        return TrainPlanner.instance()
                           .planAuxTrain(trainId, route, timeStart,
                                         locoId, State.RESERVE, null, null);
    }

    PlanTrain planReserveTrain(PlanTrain pt) {
        Train train = planReserveTrain(pt.locoId, pt.stationFrom, pt.stationTo,
                                       pt.timeStart, pt.timeEnd);
        PlanTrain ptResponse = new PlanTrain(pt.regionId);
        ptResponse.stationFrom = pt.stationFrom;
        ptResponse.stationTo = pt.stationTo;
        ptResponse.haulingDirection = false;
        if (train == null) {
            ptResponse.timeStart = pt.timeStart;
            ptResponse.timeEnd = pt.timeEnd;
            ptResponse.distance = 0L;
        } else {
            Integer endIndex = train.getUnassignedLocoIndex() - 1;
            AssignEvent firstAssign = (AssignEvent)
                (train.lastEvent(0, AssignEvent.class));
            AssignEvent lastAssign = (AssignEvent)
                (train.lastEvent(endIndex, AssignEvent.class));
            Long tReady = lastAssign.getTrainReadyTime();
            Long duration =
                lastAssign.getTimeEnd() - firstAssign.getTimeStart();
            ptResponse.timeStart = tReady - duration;
            ptResponse.timeEnd = tReady;
            ptResponse.distance = train.distance(0, endIndex);
            ptResponse.haulingDirection = train.getRoute().get(0)
                                               .getLink().isHaulingDirection();
        }
        return ptResponse;
    }

    void estimativeShift(Train train, Integer startIndex, Integer endIndex) {
        if (endIndex < train.getRoute().size() - 1) {
            Long duration = train.duration(startIndex, endIndex),
                 normTime = train.avgNormTime(startIndex, endIndex),
                 teamPeriod = params.allowance.teamChangePeriod - 3600L,
                 slotSearch = params.allowance.slotSearch,
                 nTeamChanges = duration / teamPeriod + 1;
            train.shiftBy(nTeamChanges * (normTime + slotSearch),
                          endIndex + 1);
        }
    }

    private boolean doAssign(Long trainId, List<Long> locoData,
                             Assign reAssign) {
        Long locoId = locoData.get(0);
        Loco loco = data.getLoco(locoId);
        LocoRegion region = data.getLocoRegion(reAssign.regionId);
        if (locoData.size() >= 6 && locoData.get(2) >= 0L) {
            // Поезд под пересылку резервом
            Train reserveTrain =
                planReserveTrain(locoData.get(0),
                                 locoData.get(2), locoData.get(3),
                                 locoData.get(4), locoData.get(5));
            if (reserveTrain == null) return false;
            LocoRegionPriority.initPriorities(reserveTrain, region);
            LocoAssignEvent evt =
                new LocoAssignEvent(reserveTrain.getTrainId(),
                                    0, reserveTrain.getRoute().size() - 1);
            loco.addEvent(evt);
            data.putTrain(reserveTrain);
            doAssign(trainId, locoData.subList(0, 2), reAssign);
        } else if (locoData.size() >= 3 &&
                       locoData.get(2).intValue() == State.RESERVE.ordinal()) {
            // Поезд под пересылку резервом, назначаемый по подсказке
            Train train = data.getTrain(trainId);
            Integer startIndex = train.getUnassignedLocoIndex();
            Integer endIndex = train.getRoute().size() - 1;
            Long shiftTime = locoData.get(1);
            train.updateAssign(startIndex, endIndex,
                               locoId, State.RESERVE, null, null);
            train.shiftBy(shiftTime, startIndex);
            LocoAssignEvent evt =
                new LocoAssignEvent(trainId, startIndex, endIndex);
            loco.addEvent(evt);
            loco.updateService(evt);
            planService(locoId);
            train.setUnassignedLocoIndex(endIndex + 1);
            train.setTentative(false);
        } else {
            // Поезд под основное назначение
            Train train = data.getTrain(trainId);
            Integer startIndex = train.getUnassignedLocoIndex();
            Integer endIndex0 =
                LocoRegionPriority.endOfSlot(train, startIndex, region);
            Integer endIndex =
                endIndexForImpendingService(train, loco, startIndex, endIndex0);
            Long shiftTime = locoData.get(1);
            Long totalShiftTime = shiftTime;
            if (startIndex > 0) {
                Train.Event refEvt = train.lastEvent(startIndex - 1);
                totalShiftTime +=
                    refEvt.getTrainReadyTime() - refEvt.getEventTime();
            }
            train.updateAssign(startIndex, endIndex,
                               locoId, State.WITH_TRAIN, null, null);
            train.shiftBy(totalShiftTime, startIndex);
            if (shiftTime > 0L) {
                LOGGER().debug("Отправление поезда " + trainId +
                               " на плече " + reAssign.regionId +
                               " сдвинуто на " + totalShiftTime + " с.");
            }
            if (preplanning) estimativeShift(train, startIndex, endIndex);
            LocoAssignEvent evt =
                new LocoAssignEvent(trainId, startIndex, endIndex);
            loco.addEvent(evt);
            if (endIndex0 == endIndex) {
                loco.updateService(evt);
            } else {
                planService(loco, train.getStationTo(endIndex),
                            loco.lastServiceEvent().getServiceType());
            }
            train.setUnassignedLocoIndex(endIndex + 1);
//          if (input.getTrainStates().containsKey(trainId)) {
//              VirtualLoco vLoco = new VirtualLoco();
//              vLoco.setVirtualLocoId(locoId);
//              vLoco.setRealLoco(rLoco);
//              currentFrame.data.addVirtualLoco(vLoco);
//          }
        }
        return true;
    }

    private Integer
    endIndexForImpendingService(Train train, Loco loco, Integer startIndex,
                                Integer endIndex) {
        LocoServiceEvent sEvt = loco.lastServiceEvent();
        Long tts = sEvt.getTimeToService(),
             dts = sEvt.getDistToService(),
             duration = train.duration(startIndex, endIndex),
             distance = train.distance(startIndex, endIndex);
        if (tts > duration * 2 && dts > distance * 2)
            return endIndex;
        Station stEnd = train.getStationTo(endIndex);
        Long servType = sEvt.getServiceType();
        Station stServ = new StationWithService(servType);
        ShortestPath shortest = input.getShortestPath();
        Class<? extends FilterIterator<Link>> noHaulingDirFilter =
            StationWithService.NoHaulingDirectionFilter.class;
        tts -= duration + stEnd.getProcessTime() + params.allowance.slotSearch;
        dts -= distance;
        if (shortest.findDuration(stEnd, stServ, noHaulingDirFilter) < tts &&
                shortest.findDistance(stEnd, stServ, noHaulingDirFilter) < dts)
            return endIndex;
        for (int i = endIndex; i >= startIndex; --i) {
            Station st = train.getStationTo(i);
            if (st.getServiceAvailable().containsKey(servType))
                return i;
        }
        return endIndex;
    }

    private void planService(Long locoId) {
        Loco loco = data.getLoco(locoId);
        LocoServiceEvent sEvt = loco.lastServiceEvent();
        if (sEvt == null || sEvt.getTimeToService() >= params.minTimeToService)
            return;
        Long servType = sEvt.getServiceType();
        Station stLoco = loco.getStation();
        StationWithService stServ = new StationWithService(servType);
        if (! stServ.equals(stLoco))
            return;
        planService(loco, stLoco, stServ.shortestServiceType(servType));
    }

    private void planService(Long locoId, Assign reAssign) {
        Loco loco = data.getLoco(locoId);
        LocoServiceEvent sEvt = loco.lastServiceEvent();
        if (sEvt == null || sEvt.getTimeToService() >= params.minTimeToService)
            return;
        LocoRegion region = data.getLocoRegion(reAssign.regionId);
        Long servType = sEvt.getServiceType();
        Station stLoco = loco.getStation();
        StationWithService stServ = new StationWithService(servType);
        ShortestPath shortest = input.getShortestPath();
        Class<? extends FilterIterator<Link>> noHaulingDirFilter =
            StationWithService.NoHaulingDirectionFilter.class;
        List<Link> routeToServ =
            // Сначала пытаемся найти станцию ТО в порожнем направлении
            shortest.findRouteByDuration(stLoco, stServ, noHaulingDirFilter);
        if (routeToServ.isEmpty() && stServ.getId() == null) {
            // Если не вышло, ищем её в любом направлении
            routeToServ = shortest.findRouteByDuration(stLoco, stServ);
        }
        if (routeToServ.isEmpty()) {
            // Если опять не вышло — ничего тут не поделаешь
            if (stServ.getId() == null) return;
        } else {
            Train reserveTrain =
                planReserveTrain(loco.getId(), routeToServ,
                                 sEvt.getLocoReadyTime());
            if (reserveTrain == null) return;
            LocoRegionPriority.initPriorities(reserveTrain, region);
            LocoAssignEvent evt =
                new LocoAssignEvent(reserveTrain.getTrainId(),
                                    0, routeToServ.size() - 1);
            loco.addEvent(evt);
            data.putTrain(reserveTrain);
        }
        planService(loco, stServ, stServ.shortestServiceType(servType));
    }

    private void planService(Loco loco, Station serviceStation,
                             Long serviceType) {
        LocoServiceEvent sEvt =
            new LocoServiceEvent(serviceStation.getId(),
                                 serviceStation.getServiceAvailable()
                                               .get(serviceType),
                                 params.initTimeToService,
                                 params.initDistToService,
                                 serviceType, loco.lastNonServiceEvent());
        loco.addEvent(sEvt);
        LOGGER().debug("Локомотив " + loco.getId() +
                           " проходит ТО " + serviceType +
                           " на станции " + serviceStation.getId() +
                           " от " + sEvt.getStartTime() +
                           " до " + sEvt.getEndTime());
    }

    private int finalizeAssigns(Map<Long, Assign> reAssigns, int turn) {
        int nAssigned = 0;
        for (Assign reAssign : reAssigns.values()) {
            // Поезда, которые удалось назначить, добавляются в выходные данные
            // интервала и сдвигаются, если необходимо; заодно обновляются факты
            // по локомотивам:
            for (Map.Entry<Long, List<Long>> trainLoco :
                     reAssign.map.entrySet()) {
                Long trainId = trainLoco.getKey();
                List<Long> locoData = trainLoco.getValue();
                if (doAssign(trainId, locoData, reAssign)) ++nAssigned;
            }
            // Для всех поездов, которые не удалось назначить на плече,
            // приоритет этого плеча подавляется:
            for (Long trainId : reAssign.trains) {
                Train train = data.getTrain(trainId);
                LocoRegion reg = data.getLocoRegion(reAssign.regionId);
                LocoRegionPriority.demote(train, reg);
            }
            // Все локомотивы, которые не удалось назначить на 1 туре,
            // пытаемся отставить на ТО:
            if (turn == 1) {
                for (Long locoId : reAssign.locos) {
                    planService(locoId, reAssign);
                }
            }
        }
        return nAssigned;
    }

    void init(SchedulingFrame frame) throws InterruptedException {
        CHECK_INTERRUPTED();
        this.currentFrame = frame;
        this.params = LocoPlanner.params();
        this.regionAgents = makeRegionAgents();
    }

    void assign(SchedulingFrame frame, Boolean preplanning)
    throws InterruptedException {
        CHECK_INTERRUPTED();
        this.currentFrame = frame;
        this.preplanning = preplanning;
        if (regionAgents.isEmpty()) return;
        Map<Long, Assign> assigns = new HashMap<>();
        int maxTurns = params.maxAssignmentTurns;
        int nAssigned = 0;
        boolean mayFilter = false;
        for (int turn = 1; turn <= maxTurns; ++turn) {
            CHECK_INTERRUPTED();
            assigns.clear();
            int nTrains, nLocos;
            if ((nTrains = distributeTrains(assigns, turn, mayFilter)) == 0 ||
                    (nLocos = distributeLocos(assigns, turn)) == 0)
                break;
            LOGGER().info("" + turn + " тур назначений на " +
                              LoggingAssistant.countingForm(assigns.size(),
                                      "тяговом плече", "тяговых плечах") +
                              ": всего " +
                              LoggingAssistant.countingForm(nTrains,
                                      "поезд", "поезда", "поездов") + ", " +
                              LoggingAssistant.countingForm(nLocos,
                                      "локомотив", "локомотива",
                                      "локомотивов") + "...");
            for (Map.Entry<Long, Assign> regAssign : assigns.entrySet()) {
                RegionAgent agent = regionAgents.get(regAssign.getKey());
                regAssign.setValue(agent.assign(regAssign.getValue(), preplanning));
            }
            int nAssignedThisTurn = finalizeAssigns(assigns, turn);
            nAssigned += nAssignedThisTurn;
            mayFilter = mayFilter || (nAssignedThisTurn == 0);
        }
        LOGGER().info("Произведено " +
                          LoggingAssistant.countingForm(nAssigned,
                              "назначение", "назначения", "назначений"));
    }

}
