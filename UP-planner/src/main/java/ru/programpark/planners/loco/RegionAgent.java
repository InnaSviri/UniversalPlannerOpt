package ru.programpark.planners.loco;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Capacity;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.loco.BaseLocoTrack.State;
import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.train.TrainAttributes;
import ru.programpark.entity.train.WeightType;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Pair;
import ru.programpark.entity.util.ShortestPath;
import ru.programpark.planners.common.*;
import ru.programpark.planners.assignment.*;
import ru.programpark.planners.util.WaitRatingUtil;

import java.util.*;

public class RegionAgent {
    private Logger logger =
        LoggerFactory.getLogger(RegionAgent.class);

    private void logError(Exception e) {
        LoggingAssistant.logException(e);
        logger.error("Exception:" + e.getLocalizedMessage());
        e.printStackTrace();
    }

    private void CHECK_INTERRUPTED() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            logger.warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    private LocoRegion region;
    private InputData input;
    private SchedulingData data;
    private Long currentFrameStart, currentFrameEnd;
    private AssignmentProblem aspro;
    private LocoPlanner.Params params;

    RegionAgent() {}

    RegionAgent(LocoRegion region) {
        this.region = region;
        this.input = SchedulingData.getInputData();
        this.data = SchedulingData.getFrameData();
        String loggerId = RegionAgent.class.getName() + " #" + region.getId();
        this.logger = LoggerFactory.getLogger(loggerId);
        this.params = LocoPlanner.params();
        if (params.useAnnealing) this.aspro = new AnnealingAssignment();
        else if (params.useHybrid) this.aspro = new HybridAssignment();
        else this.aspro = new AuctionAssignment();
        aspro.setParams(new AssignmentParams(input));
    }

    private Regions.PlanTrain
    makePlanTrain(long locoId, Station stLoco, Station stFirst,
                  long locoTimeStart, long locoTimeEnd, long distance) {
        Regions.PlanTrain pt = new Regions.PlanTrain(region.getId());
        pt.locoId = locoId;
        pt.stationFrom = stLoco.getId();
        pt.stationTo = stFirst.getId();
        pt.timeStart = locoTimeStart;
        pt.timeEnd = locoTimeEnd;
        pt.distance = distance;
        pt.haulingDirection = false;
        return pt;
    }

    private Regions.PlanTrain planReserveTrain(Regions.PlanTrain request) {
        return LocoPlanner.regions().planReserveTrain(request);
    }

    private Map<Long, List<Long>>
    assignByHints(List<Train> trains, List<Loco> locos)
    throws InterruptedException {
        CHECK_INTERRUPTED();
        Map<Long, List<Long>> map = new LinkedHashMap<>();
        logger.info("Назначение по подсказкам...");
        int nAssigned = 0;
        Map<Long, Loco> locoMap = new HashMap<>();
        for (Loco loco : locos) locoMap.put(loco.getId(), loco);
        Iterator<Train> trainIter = trains.iterator();
        while (trainIter.hasNext()) {
            Train train = trainIter.next();
            HintEvent hEvt = (HintEvent)
                train.lastEvent(train.getUnassignedLocoIndex(),
                                HintEvent.class);
            if (hEvt == null || hEvt.getLocoId() == null) continue;
            Long hStId = hEvt.getStationPair().stationFromId;
            Loco loco = locoMap.get(hEvt.getLocoId());
            if (loco != null &&
                    loco.getStation().getId().equals(hStId)) {
                List<Long> locoData =
                    encodeLocoData(train, loco, hEvt.getLocoState());
                map.put(train.getTrainId(), locoData);
                ++nAssigned;
                locoMap.remove(loco.getId());
                trainIter.remove();
            }
        }
        locos.retainAll(locoMap.values());
        logger.info(LoggingAssistant.countingForm(nAssigned,
                        "Назначен по подсказкам %d поезд",
                        "Назначены по подсказкам %d поезда",
                        "Назначено по подсказкам %d поездов"));
        return map;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Кеш информации о предполагаемых операциях на станции
    private class StationOps {
        SortedSet<Pair<Loco, Regions.PlanTrain>> locoAvailability =
            new TreeSet<>(new Comparator<Pair<Loco, Regions.PlanTrain>>() {
                @Override public int compare(Pair<Loco, Regions.PlanTrain> a,
                                             Pair<Loco, Regions.PlanTrain> b) {
                    int c = Long.compare(a.getSecond().timeEnd,
                                         b.getSecond().timeEnd);
                    if (c != 0) return c;
                    c = Long.compare(a.getFirst().lastEvent().getEventTime(),
                                     b.getFirst().lastEvent().getEventTime());
                    if (c != 0) return c;
                    c = Long.compare(a.getFirst().getId(),
                                     b.getFirst().getId());
                    if (c != 0) return c;
                    return Integer.compare(System.identityHashCode(a),
                                           System.identityHashCode(b));
                }
                @Override public boolean equals(Object other) {
                    return false;
                }
            });

        SortedSet<Train> trainAvailability =
            new TreeSet<>(new Comparator<Train>() {
                @Override public int compare(Train a, Train b) {
                    Integer ia = a.getUnassignedLocoIndex();
                    Integer ib = b.getUnassignedLocoIndex();
                    int c = Long.compare(trainTimeStart(a, ia),
                                         trainTimeStart(b, ib));
                    if (c != 0) return c;
                    Train.Event ea = a.lastEvent(-1);
                    Train.Event eb = b.lastEvent(-1);
                    if (ea != null && eb != null)
                        c = Long.compare(ea.getEventTime(),
                                         eb.getEventTime());
                    if (c != 0) return c;
                    c = Long.compare(a.getTrainId(), b.getTrainId());
                    if (c != 0) return c;
                    return Integer.compare(System.identityHashCode(a),
                                           System.identityHashCode(b));
                }
                @Override public boolean equals(Object other) {
                    return false;
                }
            });

        SortedSet<Train> trainAvailabilityWithPriority =
            new TreeSet<>(new Comparator<Train>() {
                private Integer categoryPriority(Train train) {
                    TrainAttributes attrs = train.getAttributes();
                    return (attrs == null) ? null :
                        input.getTrainCategoryById(attrs.getCategory())
                             .getPriority();
                }
                private Integer trainPriority(Train train) {
                    TrainAttributes attrs = train.getAttributes();
                    return (attrs == null) ? null :
                        (attrs.getPriority() != null) ? attrs.getPriority() :
                        params.defaultTrainPriority;
                }
                @Override public int compare(Train a, Train b) {
                    Integer pa = categoryPriority(a), pb = categoryPriority(b);
                    if (pa != null && pb != null) {
                        int c = pa.compareTo(pb);
                        if (c != 0) return c;
                        pa = trainPriority(a); pb = trainPriority(b);
                        assert(pa != null && pb != null);
                        c = pa.compareTo(pb);
                        if (c != 0) return c;
                    } else if (pa != null) {
                        return 1;
                    } else if (pb != null) {
                        return -1;
                    }
                    return trainAvailability.comparator().compare(a, b);
                }
                @Override public boolean equals(Object other) {
                    return false;
                }
            });

        Map<Train, Long> trainStartTimes = new LinkedHashMap<>();

        Map<Pair<Train, Loco>, LocoUtility> utilities = new LinkedHashMap<>();

        Regions.PlanTrain addLoco(Loco loco, Regions.PlanTrain reserve) {
            locoAvailability.add(new Pair<>(loco, reserve));
            return reserve;
        }

        // Когда пересылка резервом не требуется
        Regions.PlanTrain addLoco(Loco loco) {
            Regions.PlanTrain extant = locoReserve(loco);
            if (extant == null) {
                Long id = loco.getId();
                Station st = loco.getStation();
                Long time = loco.lastEvent().getLocoReadyTime();
                Regions.PlanTrain noReserve =
                    makePlanTrain(id, st, st, time, time, 0L);
                return addLoco(loco, noReserve);
            } else {
                return extant;
            }
        }

        Regions.PlanTrain locoReserve(Loco loco) {
            for (Pair<Loco, Regions.PlanTrain> lpt : locoAvailability) {
                if (lpt.getFirst() == loco) return lpt.getSecond();
            }
            return null;
        }

        Train addTrain(Train train) {
            trainAvailabilityWithPriority.add(train);
            //trainAvailability.add(train);
            return train;
        }

        void estimateTrainStartTimes() {
            Map<Link, TreeMap<Long, Capacity>> capacities = new HashMap<>();
            Long locoTime = locoAvailability.isEmpty() ? 0L :
                locoAvailability.first().getSecond().timeEnd;
            SortedSet<Pair<Train, Long>> startTimes =
                new TreeSet<>(new Comparator<Pair<Train, Long>>() {
                    @Override public int compare(Pair<Train, Long> a,
                                                 Pair<Train, Long> b) {
                        int c = Long.compare(a.getSecond(), b.getSecond());
                        return (c != 0) ? c :
                            trainAvailability.comparator()
                                             .compare(a.getFirst(), b.getFirst());
                    }
                    @Override public boolean equals(Object other) {
                        return false;
                    }
                });
            for (Train tr: trainAvailabilityWithPriority) {
                Integer index = tr.getUnassignedLocoIndex();
                long trTime = tr.isTentative(true) ? tr.getStartTime() :
                    tr.lastEvent(index - 1).getTrainReadyTime();
                Long timeAvail = Math.max(trTime, locoTime);
                Link trLink = tr.getRoute().get(index).getLink();
                TreeMap<Long, Capacity> linkCapacities =
                    capacities.get(trLink);
                if (linkCapacities == null) {
                    linkCapacities = new TreeMap<>(trLink.getCapacities());
                    capacities.put(trLink, linkCapacities);
                }
                // Если не найдется нитка, то ставим в конец списка (по
                // очереди времени готовности):
                Long startTime = currentFrameEnd + params.maxTimeAlongRoute;
                for (Map.Entry<Long, Capacity> trCap :
                         linkCapacities.entrySet()) {
                    Capacity capacity = trCap.getValue();
                    if (capacity.getCapacity() != 0 &&
                            capacity.getStartTime() >= timeAvail) {
                        capacity = new Capacity(capacity);
                        capacity.setCapacity(capacity.getCapacity() - 1);
                        startTime = capacity.getStartTime();
                        trCap.setValue(capacity);
                        break;
                    }
                }
                startTimes.add(new Pair<>(tr, startTime));
            }
            for (Pair<Train, Long> trTime : startTimes)
                trainStartTimes.put(trTime.getFirst(), trTime.getSecond());
        }

        LocoUtility addUtility(Train train, Loco loco, LocoUtility utility) {
            utilities.put(new Pair<>(train, loco), utility);
            return utility;
        }

        LocoUtility getUtility(Train train, Loco loco) {
            return utilities.get(new Pair<>(train, loco));
        }
    }

    StationOps stationOps(Station st, Map<Station, StationOps> map) {
        StationOps stOps = map.get(st);
        if (stOps == null) {
            stOps = new StationOps();
            map.put(st, stOps);
        }
        return stOps;
    }

    StationOps allStationOps(Map<Station, StationOps> map) {
        return stationOps(null, map);
    }

    void cacheStationOpsUtility(Integer trainIndex, Train train,
                                Integer locoIndex, Loco loco,
                                Map<Station, StationOps> map,
                                LocoUtility maxima) {
        LocoUtility util =
            new LocoUtility(trainIndex, train, locoIndex, loco, map);
        maxima.updateMaxima(util);
    }

    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    // Узел дерева расчёта функции полезности
    private class LocoUtility implements Comparable<LocoUtility> {
        int trainIndex, locoIndex;
        double waitingTime, shiftTime, runToStart, runToService,
            timeAlongRoute, newTimeToService, weightTypeCorrection,
            reserveInHaulingDirectionPenalty, cachedValue, orderUtil;
        long newDistToService, trainTimeStart, trainTimeEnd, locoTimeStart;
        Station stationFirst, stationLast, stationLoco;
        List<LocoUtility> contUtilities;
        LocoUtility maxima;
        Regions.PlanTrain reserveTrainPT;

        LocoUtility(boolean constantMaxima) {
            trainIndex = locoIndex = -1;
            if (constantMaxima) {
                waitingTime = params.maxWaitingTime;
                shiftTime = params.maxShiftTime;
                runToStart = timeAlongRoute =
                    (double) (params.maxTimeAlongRoute);
                runToService = newTimeToService =
                    (double) (params.initTimeToService);
                weightTypeCorrection =
                    input.getMaxWeightType().defaultWeight();
                orderUtil =
                    WaitRatingUtil.accountLocoWaitNorm(currentFrameStart,
                                                       currentFrameEnd);
                maxima = this;
            } else {
                waitingTime = shiftTime = runToStart = runToService =
                    timeAlongRoute = newTimeToService = weightTypeCorrection =
                        orderUtil = Double.NaN;
                maxima = new LocoUtility(true);
            }
        }

        LocoUtility(Integer trainIndex, Train train,
                    Integer locoIndex, Loco loco,
                    Map<Station, StationOps> stationOpsMap) {
            this(train, loco, stationOpsMap,
                 train.getUnassignedLocoIndex(),
                 slotEndIndex(train, train.getUnassignedLocoIndex()));
            this.trainIndex = trainIndex;
            this.locoIndex = locoIndex;
        }

        LocoUtility(Train train, Loco loco,
                    Map<Station, StationOps> stationOpsMap,
                    Integer startIndex, Integer endIndex) {
            this(train, loco, stationOpsMap,
                 train.getStationFrom(startIndex),
                 train.getStationTo(endIndex),
                 trainTimeStart(train, startIndex),
                 train.duration(startIndex, endIndex),
                 train.distance(startIndex, endIndex),
                 train.maxWeight(startIndex, endIndex, loco.getSeriesPair()),
                 loco.getStation(),
                 loco.lastEvent().getLocoReadyTime());
        }

        LocoUtility(Train train, Loco loco,
                    Map<Station, StationOps> stationOpsMap,
                    Station stationFirst, Station stationLast,
                    Long trainTimeStart, Long duration,
                    Long distance, Long maxWeight,
                    Station stationLoco, Long locoTimeStart) {
            this.stationFirst = stationFirst;
            this.stationLast = stationLast;
            this.trainTimeStart = trainTimeStart;
            this.trainTimeEnd = trainTimeStart + duration;
            this.stationLoco = stationLoco;
            this.locoTimeStart = locoTimeStart;

            StationOps stationOps = stationOps(stationFirst, stationOpsMap);
            setRunToService(train, loco, stationOps);
            setRunToStartEtc(train, loco, stationOps);
            setTimeAlongRoute();
            setWaitingOrShiftTime();
            setNewTimeDistToService(loco, distance);
            setWeightTypeCorrection(train, maxWeight);
            // setOrderUtil вызывается только после того, как будут известны
            // все времена готовности поездов на станции и созданы все
            // соответствующие полезности.

            this.contUtilities = new ArrayList<>();
            this.cachedValue = Double.NaN;
            this.maxima = new LocoUtility(true);
            stationOps.addTrain(train);
            stationOps.addUtility(train, loco, this);
        }

        void updateMaxima(LocoUtility utility) {
            if (maxima != this) { // Не константные максимумы
                waitingTime = Double.isNaN(waitingTime)
                    ? utility.waitingTime
                    : Math.max(waitingTime, utility.waitingTime);
                shiftTime = Double.isNaN(shiftTime)
                    ? utility.shiftTime
                    : Math.max(shiftTime, utility.shiftTime);
                runToStart = Double.isNaN(runToStart)
                    ? utility.runToStart
                    : Math.max(runToStart, utility.runToStart);
                runToService = Double.isNaN(runToService)
                    ? utility.runToService
                    : Math.max(runToService, utility.runToService);
                timeAlongRoute = Double.isNaN(timeAlongRoute)
                    ? utility.timeAlongRoute
                    : Math.max(timeAlongRoute, utility.timeAlongRoute);
                newTimeToService = Double.isNaN(newTimeToService)
                    ? utility.newTimeToService
                    : Math.max(newTimeToService, utility.newTimeToService);
                weightTypeCorrection = Double.isNaN(weightTypeCorrection)
                        ? utility.weightTypeCorrection
                        : Math.max(weightTypeCorrection, utility.weightTypeCorrection);
                orderUtil = Double.isNaN(orderUtil)
                        ? utility.orderUtil
                        : Math.max(orderUtil, utility.orderUtil);
            }
            utility.maxima = this;
        }

        // Время от окончания маршрута до ТО
        void setRunToService(Train train, Loco loco, StationOps stationOps) {
            ShortestPath shortest = input.getShortestPath();
            if (loco.lastServiceEvent() == null) {
                this.runToService = Double.POSITIVE_INFINITY;
            } else {
                Long srvType = loco.lastServiceEvent().getServiceType();
                Station stServ = new StationWithService(srvType);
                this.runToService =
                    (double) (shortest.findDuration(stationLast, stServ));
            }
        }

        // Пересылка резервом
        void setRunToStartEtc(Train train, Loco loco, StationOps stationOps) {
            this.reserveTrainPT = stationOps.locoReserve(loco);
            if (stationLoco.equals(stationFirst)) {
                this.runToStart = 0.0;
                if (reserveTrainPT == null)
                    this.reserveTrainPT = stationOps.addLoco(loco);
            } else if (reserveTrainPT == null) {
                ShortestPath shortest = input.getShortestPath();
                long reserveTime0 =
                    shortest.findDuration(stationLoco, stationFirst);
                long processTime =
                    stationFirst.getProcessTime();
                long reserveTime =
                    reserveTime0 + processTime / 2 +
                        params.allowance.teamChange *
                            (reserveTime0 / params.allowance.teamChangePeriod) +
                        params.allowance.additional;
                long locoTimeEnd = trainTimeStart - processTime / 2;
                this.locoTimeStart = loco.lastEvent().getLocoReserveReadyTime();
                this.reserveTrainPT =
                    makePlanTrain(loco.getId(), stationLoco, stationFirst,
                                  locoTimeStart, locoTimeEnd, 0L);
                Regions.PlanTrain pt = planReserveTrain(reserveTrainPT);
                this.locoTimeStart = this.reserveTrainPT.timeEnd = pt.timeEnd;
                this.reserveTrainPT.distance = pt.distance;
                this.reserveTrainPT.haulingDirection = pt.haulingDirection;
                stationOps.addLoco(loco, reserveTrainPT);
                if (reserveTrainPT.distance > 0L) {
                    this.runToStart = (double) reserveTime;
                    if (reserveTrainPT.haulingDirection)
                        this.reserveInHaulingDirectionPenalty = 1.0;
                } else {
                    this.runToStart = Double.POSITIVE_INFINITY;
                }
            } else {
                this.locoTimeStart = reserveTrainPT.timeEnd;
                this.runToStart = locoTimeStart - reserveTrainPT.timeStart;
                if (reserveTrainPT.haulingDirection)
                    this.reserveInHaulingDirectionPenalty = 1.0;
            }
        }

        // Время движения по маршруту
        void setTimeAlongRoute() {
            this.timeAlongRoute = (double) (trainTimeEnd - trainTimeStart);
        }

        // Декремент времени и расстояния до ремонта
        void setNewTimeDistToService(Loco loco, Long distAlongRoute) {
            if (loco.lastServiceEvent() != null) {
                long locoTTS = loco.lastServiceEvent().getTimeToService(),
                    locoDTS = loco.lastServiceEvent().getDistToService();
                this.newTimeToService =
                    Math.max((double) locoTTS - timeAlongRoute - runToStart,
                             0.0);
                this.newDistToService =
                    Math.max(locoDTS - distAlongRoute - reserveTrainPT.distance,
                             0L);
            } else {
                this.newTimeToService = 0.0;
                this.newDistToService = 0L;
            }
        }

        // Ожидание и сдвиг
        void setWaitingOrShiftTime() {
            long shiftTime = locoTimeStart - trainTimeStart;
            double waitingOrShiftTime = Math.abs((double) shiftTime);
            if (trainTimeStart > locoTimeStart) {
                this.waitingTime = waitingOrShiftTime;
            } else {
                this.shiftTime = waitingOrShiftTime;
            }
        }

        // Поправка на весовой тип локомотива
        void setWeightTypeCorrection(Train train, Long maxWeight) {
            if (maxWeight == null || maxWeight <= 0L) {
                this.weightTypeCorrection = params.defaultWeightTypeCorrection;
            } else {
                Long trainWeight = train.getWeight();
                if (trainWeight == null) {
                    Long wtId = train.getWeightTypeId();
                    WeightType wt = (wtId == null) ? null :
                        input.getWeightTypeById(wtId);
                    trainWeight = (wt == null) ? 0L : wt.defaultWeight();
                }
                this.weightTypeCorrection = (trainWeight > maxWeight) ?
                    Double.NEGATIVE_INFINITY : maxWeight - trainWeight;
            }
        }

        // Поправка на взаимный порядок прибытия поездов и локомотивов
        // на станцию
        void setOrderUtil(Train train, Loco loco, StationOps stationOps) {
            // int trainRank = stationOps.trainRank(train),
            //     locoRank = stationOps.locoRank(loco);
            // this.orderUtil = - Math.pow(trainRank - locoRank, 2);
            this.orderUtil =
                WaitRatingUtil.accountLocoWaitTime(stationOps.trainStartTimes,
                                                   loco, train,
                                                   currentFrameEnd);
        }

        void setOrderUtil(double value) {
            this.orderUtil = value;
        }

        /*
        double[] accruedTimes() {
            if (Double.isNaN(accruedTimeAlongRoute) ||
                    Double.isNaN(accruedWaitingTimeSq) ||
                    Double.isNaN(accruedShiftTimeSq)) {
                accruedTimeAlongRoute = timeAlongRoute;
                accruedWaitingTimeSq = waitingTimeSq;
                accruedShiftTimeSq = shiftTimeSq;
                double attenuation = 1.0;
                double attenSum = 0.0;
                double cttSum = 0.0;
                double cwtSum = 0.0;
                double cstSum = 0.0;
                for (LocoUtility cont : contUtilities) {
                    double[] cttwtst = cont.accruedTimes();
                    double ctt = cttwtst[0] * attenuation;
                    double cwt = cttwtst[1] * attenuation;
                    double cst = cttwtst[2] * attenuation;
                    if (ctt < timeAlongRoute / 100.0 &&
                            cwt < waitingTimeSq / 100.0 &&
                            cst < shiftTimeSq / 100.0)
                        break;
                    cttSum += ctt;
                    cwtSum += cwt;
                    cstSum += cst;
                    attenSum += attenuation;
                    attenuation /= Math.E;
                }
                if (attenSum > .000001) {
                    accruedTimeAlongRoute += cttSum / attenSum;
                    accruedWaitingTimeSq += cwtSum / attenSum;
                    accruedShiftTimeSq += cstSum / attenSum;
                }
            }
            double[] accruedTimes =
                {accruedTimeAlongRoute, accruedWaitingTimeSq,
                 accruedShiftTimeSq};
            return accruedTimes;
        }
        */

        private double norm(double x, double max, double exp) {
            return (max < .000001 || Double.isInfinite(x)) ? x :
                       (Math.pow(x / max, exp) * 100.0);
        }

        private double value(StringBuilder sb) {
            if (isNegInfinite()) {
                if (sb != null) sb.append("-∞");
                return Double.NEGATIVE_INFINITY;
            }
            /*
            // При сортировке для каждого продолжения вычисляется значение
            // (а стало быть и accruedTimeAlongRoute с accruedWaitingTime).
            Collections.<LocoUtility>sort(contUtilities);
            */
            double tt = norm(timeAlongRoute, maxima.timeAlongRoute, 1.0);
            // double wt = norm(waitingTime, maxima.waitingTime, 2.0);
            double st = norm(shiftTime, maxima.shiftTime, 2.0);
            double tg = norm(runToStart, maxima.runToStart, 1.0);
            double tts = norm(runToService, maxima.runToService, 1.0);
            double ntts = norm(newTimeToService, maxima.newTimeToService, 0.5);
            double uw = norm(weightTypeCorrection, maxima.weightTypeCorrection, 1.0);
            double rp = norm(reserveInHaulingDirectionPenalty, 1.0, 1.0);
            double ou = norm(orderUtil, maxima.orderUtil, 1.0);
            double val = tt * params.coeff.tt
                + tts * params.coeff.tts
                + ntts * params.coeff.ntts
                // + wt * params.coeff.wt
                + st * params.coeff.st
                + uw * params.coeff.uw
                + tg * params.coeff.tg
                + rp * params.coeff.rp
                + ou * params.coeff.ou
                ;
            if (sb != null) {
                sb.append("" + tt + " * " + params.coeff.tt);
                sb.append(" + " + tts + " * " + params.coeff.tts);
                sb.append(" + " + ntts + " * " + params.coeff.ntts);
                sb.append(" + " + st + " * " + params.coeff.st);
                sb.append(" + " + uw + " * " + params.coeff.uw);
                sb.append(" + " + tg + " * " + params.coeff.tg);
                sb.append(" + " + rp + " * " + params.coeff.rp);
                sb.append(" + " + ou + " * " + params.coeff.ou);
                sb.append(" = " + val);
            }
            return val;
        }

        double value(boolean forceRecalc) {
            if (Double.isNaN(cachedValue) || forceRecalc)
                cachedValue = value(null);
            return cachedValue;
        }

        String describeValue() {
            StringBuilder sb = new StringBuilder();
            value(sb);
            return sb.toString();
        }

        boolean isNegInfinite() {
            return Double.isInfinite(weightTypeCorrection) ||
                Double.isInfinite(runToStart) ||
                newTimeToService <= 0.0 ||
                newDistToService <= 0L;
        }

        @Override public int compareTo(LocoUtility that) {
            return Double.compare(this.value(false), that.value(false));
        }

        @Override public String toString() {
            return "LocoUtility{" +
                String.format("T0=%d, L0=%d, TT=%.0f, TTS=%.0f, NTTS=%.0f," +
                              " NDTS=%d, WT=%.0f, ST=%.0f, OU=%f," +
                              " Uw=%.0f, TG=%.0f",
                              trainTimeStart, locoTimeStart,
                              timeAlongRoute, runToService,
                              newTimeToService, newDistToService,
                              waitingTime, shiftTime, orderUtil,
                              weightTypeCorrection, runToStart) +
                (reserveTrainPT != null
                     ? String.format(", RP=%.0f", reserveInHaulingDirectionPenalty)
                     : "") +
                "}";
        }
    }
    ////////////////////////////////////////////////////////////////////////////


    private Long trainTimeStart(Train train, Integer startIndex) {
        if (startIndex == 0) {
            AssignEvent firstAssign = (AssignEvent)
                (train.lastEvent(startIndex, AssignEvent.class));
            return firstAssign.getTimeStart();
        } else {
            AssignEvent preAssign = (AssignEvent)
                (train.lastEvent(startIndex - 1, AssignEvent.class));
            return preAssign.getTrainReadyTime();
        }
    }

    private Integer slotEndIndex(Train train, Integer startIndex) {
        return LocoRegionPriority.endOfSlot(train, startIndex, region);
    }

    // Отсутствует ли возможность назначить loco на train?
    private String irrelevant(Train train, Loco loco, boolean noShifts) {
        long waitingTime =
            trainTimeStart(train, train.getUnassignedLocoIndex()) -
                loco.lastEvent().getLocoReadyTime();
        if (waitingTime < 0L && noShifts) {
            return "локомотив не будет готов к отправлению поезда";
        } else if (waitingTime <= - params.maxShiftTime) {
            return String.format("время сдвига поезда %.02f ч. превышает %d ч.",
                                 (- waitingTime / 3600.0),
                                 params.maxShiftTime / 3600L);
        } else if (waitingTime >= params.maxWaitingTime) {
            return String.format("время ожидания локомотива %.02f ч. превышает %d ч.",
                                 waitingTime / 3600.0,
                                 params.maxWaitingTime / 3600L);
        } else {
            return null;
        }
    }

    // Полезности
    private PartialAssignmentMap
    utilities(List<Train> trains, List<Loco> locos,
              Map<Station, StationOps> stationOpsMap)
    throws InterruptedException {
        int nTrains = trains.size(), nLocos = locos.size();
        LocoUtility maxima = new LocoUtility(false);
        for (int i = 0; i < nTrains; ++i) {
            CHECK_INTERRUPTED();
            Train train = trains.get(i);
            if (train.isTentative(true) || train.isTentative(false)) {
                logger.trace("Условный поезд " + train.getTrainId() +
                                 " исключен из общего порядка назначений");
                continue;
            }
            for (int j = 0; j < nLocos; ++j) {
                CHECK_INTERRUPTED();
                Loco loco = locos.get(j);
                String cause;
                if ((cause = irrelevant(train, loco, false)) == null) {
                    cacheStationOpsUtility(i, train, j, loco, stationOpsMap, maxima);
                } else {
                    logger.trace("Назначение на поезд " + train.getTrainId() +
                                 " локомотива " + loco.getId() +
                                 " нецелесообразно: " + cause);
                }
            }
        }
        List<Integer> dims = new ArrayList<>(2);
        Collections.addAll(dims, nTrains, nLocos);
        PartialAssignmentMap pa = new PartialAssignmentMap(dims);
        StationOps allStationOps = allStationOps(stationOpsMap);
        for (Map.Entry<Station, StationOps> entry : stationOpsMap.entrySet()) {
            Station station = entry.getKey();
            StationOps stationOps = entry.getValue();
            if (station == null) continue;
            logger.trace("******** Полезности на станции " +
                             station.getId() + " ********");
            stationOps.estimateTrainStartTimes();
            for (Map.Entry<Pair<Train, Loco>, LocoUtility> tlu :
                     stationOps.utilities.entrySet()) {
                Train train = tlu.getKey().getFirst();
                Loco loco = tlu.getKey().getSecond();
                LocoUtility utility = tlu.getValue();
                if (loco.getStation().equals(station)) {
                    utility.setOrderUtil(train, loco, stationOps);
                    maxima.updateMaxima(utility);
                } else {
                    utility.setOrderUtil(0.0);
                }
                if (! utility.isNegInfinite())
                    allStationOps.addUtility(train, loco, utility);
                logger.trace("Поезд " + train.getTrainId() +
                             " [" + utility.trainIndex +
                             "], лок. " + loco.getId() +
                             " [" + utility.locoIndex +
                             "]: " + utility);
            }
        }
        logger.debug("******** Значения полезностей ********");
        for (LocoUtility utility : allStationOps.utilities.values()) {
            Integer[] inds = {utility.trainIndex, utility.locoIndex};
            Double val = utility.value(true);
            pa.addPartialAssignment(new PartialAssignment(inds, val));
            logger.debug("U[" + utility.trainIndex + ", " +
                         utility.locoIndex + "] = " +
                         (logger.isTraceEnabled()
                              ? utility.describeValue()
                              : String.format("%.6f", utility.cachedValue)));
        }
        return pa.isEmpty() ? null : pa;
    }

    // Отдельная итерация расчёта назначений включает: 1) вычисление матрицы
    // полезностей для данных поездов и локомотивов; 2) решение задачи о
    // назначениях; 3) в случае необходимости — планирование перемещений
    // локомотивов резервов; 4) составление таблицы назначений; 5) обновление
    // списков неназначенных поездов и локомотивов, в т. ч. обновление данных по
    // локомотивам для учёта их назначений.
    private Map<Long, List<Long>>
    assignByUtility(List<Train> trains, List<Loco> locos)
    throws InterruptedException {
        logger.info("Вычисление функции полезности для " +
                        LoggingAssistant.countingForm(trains.size(),
                            "поезда", "поездов") +
                        ", " +
                        LoggingAssistant.countingForm(locos.size(),
                            "локомотива", "локомотивов") +
                        "...");
        Map<Station, StationOps> stationOpsMap = new HashMap<>();
        PartialAssignmentMap utilities = utilities(trains, locos, stationOpsMap);
        if (utilities == null) {
            logger.info("Не найдено назначений с полезностью, отличной от -∞");
            return null;
        }
        CHECK_INTERRUPTED();
        Map<Long, List<Long>> map = new LinkedHashMap<>();
        logger.info("Расчёт назначений...");
        logger.trace("utilities = " + utilities);
        PartialAssignmentMap solution = aspro.decision(utilities);
        logger.trace("solution = " + solution);
        CHECK_INTERRUPTED();
        int nAssigned = 0;
        for (PartialAssignment p : solution.partialAssignments()) {
            if (p.getUtil() > Double.NEGATIVE_INFINITY) {
                List<Integer> indices = p.getIndices();
                Train train = trains.get(indices.get(0));
                Loco loco = locos.get(indices.get(1));
                LocoUtility utility =
                    allStationOps(stationOpsMap).getUtility(train, loco);
                trains.set(indices.get(0), null);
//              loco = contLoco(train, loco, cachedUtil);
                locos.set(indices.get(1), null);
                List<Long> locoData = encodeLocoData(train, loco, utility);
                map.put(train.getTrainId(), locoData);
                ++nAssigned;
            }
        }
        trains.removeAll(Collections.singleton(null));
        locos.removeAll(Collections.singleton(null));
        logger.info("Найдено " +
            LoggingAssistant.countingForm(nAssigned,
                "назначение", "назначения", "назначений") +
            " с полезностью, отличной от -∞");
        return map;
    }

    private void selectUnassigned(Regions.Assign assign, List<Train> trains,
                                  List<Loco> locos) {
        for (Long trainId : assign.trains) {
            Train train = data.getTrain(trainId);
            trains.add(train);
        }
        for (Long locoId : assign.locos) {
            Loco loco = data.getLoco(locoId);
            locos.add(loco);
        }
    }

    private List<Long> encodeLocoData(Train train, Loco loco, State state) {
        List<Long> data = new ArrayList<>();
        data.add(loco.getId());
        long shiftTime =
            loco.lastEvent().getLocoReadyTime() -
                trainTimeStart(train, train.getUnassignedLocoIndex());
        data.add(Math.max(shiftTime, 0L));
        data.add((long) state.ordinal());
        return data;
    }

    private List<Long> encodeLocoData(Train train, Loco loco, LocoUtility utility) {
        List<Long> data = new ArrayList<>();
        data.add(loco.getId());
        data.add((utility.shiftTime > 0.0)
                     ? (utility.locoTimeStart - utility.trainTimeStart)
                     : 0L);
        boolean haveResv = (utility.reserveTrainPT != null &&
                                utility.reserveTrainPT.distance > 0L);
        if (haveResv) {
            data.add(utility.reserveTrainPT.stationFrom);
            data.add(utility.reserveTrainPT.stationTo);
            data.add(utility.reserveTrainPT.timeStart);
            data.add(utility.reserveTrainPT.timeEnd);
        }
        return data;
    }

    // Полный тур назначений
    Regions.Assign assign(Regions.Assign assign, Boolean preplanning)
    throws InterruptedException {
        CHECK_INTERRUPTED();
        logger.debug("Начало тура");
        List<Train> trains = new ArrayList<>();
        List<Loco> locos = new ArrayList<>();
        this.currentFrameStart = data.getCurrentFrame().locoFrameStart;
        this.currentFrameEnd = data.getCurrentFrame().locoFrameEnd;
        selectUnassigned(assign, trains, locos);
        assign = Regions.Assign.forResponse(assign.regionId);
        Map<Long, List<Long>> assignMap;
        if (logger.isTraceEnabled()) {
            logger.trace(">>>>>>>>>>>>>>>>>>> ПОЕЗДА >>>>>>>>>>>>>>>>>>>");
            for (Train train : trains) {
                logger.trace(train.toString());
            }
            logger.trace(">>>>>>>>>>>>>>>>> ЛОКОМОТИВЫ >>>>>>>>>>>>>>>>>");
            for (Loco loco : locos) {
                logger.trace(loco.toString());
            }
            logger.trace(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        }
        if (! preplanning) {
            assignMap = assignByHints(trains, locos);
            if (assignMap != null) assign.map.putAll(assignMap);
        }
        assignMap = assignByUtility(trains, locos);
        if (assignMap != null) assign.map.putAll(assignMap);
        if (logger.isDebugEnabled()) {
            String assignIds = null;
            for (Map.Entry<Long, List<Long>> trainLoco :
                     assign.map.entrySet()) {
                assignIds = ((assignIds == null) ? "" : (assignIds + ", ")) +
                    String.format("%d ↔ %d %+d",
                                  trainLoco.getKey(),
                                  trainLoco.getValue().get(0),
                                  trainLoco.getValue().get(1));
            }
            logger.debug("Привязка поездов к локомотивам: " + assignIds);
        }
        logger.trace("<<<<<<<<<<<<<<<<<<< ПОЕЗДА <<<<<<<<<<<<<<<<<<<");
        for (Train train : trains) {
            logger.trace(train.toString());
            Long trainId = train.getTrainId();
            assign.trains.add(trainId);
        }
        logger.trace("<<<<<<<<<<<<<<<<< ЛОКОМОТИВЫ <<<<<<<<<<<<<<<<<");
        for (Loco loco : locos) {
            logger.trace(loco.toString());
            assign.locos.add(loco.getId());
        }
        logger.trace("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        logger.debug("Конец тура");
        return assign;
    }
}
