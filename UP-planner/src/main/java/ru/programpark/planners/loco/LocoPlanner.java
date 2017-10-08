package ru.programpark.planners.loco;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.data.OutputData;
import ru.programpark.entity.train.RealTrain;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.common.*;
import ru.programpark.planners.train.TrainPlanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LocoPlanner {
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(LocoPlanner.class);
        return logger;
    }

    private void CHECK_INTERRUPTED() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            LOGGER().warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    private void logError(Exception e) {
        LoggingAssistant.logException(e);
        LOGGER().error("Exception:" + e.getLocalizedMessage());
        e.printStackTrace();
    }

    // Параметры планирования
    class Params {
        private long param(InputData input, String key, long mult) {
            return input.getConfigParam("loco", key).longValue() * mult;
        }
        private long param(InputData input, String key) {
            return param(input, key, 1);
        }
        private double coeffParam(InputData input, String key, double signum) {
            double coeff =
                input.getConfigParam("loco/coeff", key).doubleValue();
            if (Math.signum(coeff) == signum) {
                return coeff;
            } else {
                throw new RuntimeException("Коэффициент " + key +
                    " функции полезности назначения локомотивов задан" +
                    " с неправильным знаком");
            }
        }
        private long allowanceParam(InputData input, String key, long mult) {
            return (long) (input.getConfigParam("allowance", key).doubleValue() *
                               mult);
        }
        private final static long H = 3600L;
        private final static double POS = 1d;
        private final static double NEG = -1d;

        // Диапазон номеров псевдопоездов для пересылки резервом
        long minReserveTrainId, maxReserveTrainId;
        // Префикс идентификатора псевдопоездов для пересылки резервом
        long reserveTrainIdPrefix;
        // Величина счётчика псевдопоездов
        int reserveTrainIdSeqDigits;
        // Макс. число туров назначений
        int maxAssignmentTurns;
        // Условно наибольшее время движения поезда по маршруту в пределах
        // одного плеча (используется для нормирования)
        long maxTimeAlongRoute;
        // Порог ожидания поезда локомотивом либо сдвига поезда, за которым
        // назначение считается нецелесообразным
        long maxWaitingTime, maxShiftTime;
        // Порог времени, за которым следует отставка локомотива на ТО
        long minTimeToService;
        // Время и расстояние до ТО-2, выставляемые после прохождения
        // предыдущего
        long initTimeToService, initDistToService;
        // Использовать ли алгоритм имитации отжига для решения ЗН?
        boolean useAnnealing, useHybrid;
        // Поправка на вес лок-ва по умолчанию
        long defaultWeightTypeCorrection;
        // Приоритет поезда по умолчанию
        Integer defaultTrainPriority;

        // Коэффициенты функции полезности
        class Coeff { double tt, tts, ntts, wt, st, uw, tg, rp, ou; }
        Coeff coeff;

        // Допуски
        class Allowance { long teamChange, teamChangePeriod, slotSearch,
                               additional; }
        Allowance allowance;

        Params(InputData input) {
            maxTimeAlongRoute  = param(input, "max_time_along_route", H);
            maxWaitingTime     = param(input, "max_waiting_time", H);
            maxShiftTime       = param(input, "max_shift_time", H);
            minTimeToService   = param(input, "min_time_to_service", H);
            initTimeToService  = param(input, "init_time_to_service", H);
            initDistToService  = param(input, "init_dist_to_service");
            defaultWeightTypeCorrection = param(input, "default_weight_type_correction");
            defaultTrainPriority = input.getConfigParam("train/default_priority").intValue();

            coeff = new Coeff();
            coeff.tt      = coeffParam(input, "TT_run_along_route", POS);
            coeff.tts     = coeffParam(input, "TTS_run_to_service", NEG);
            coeff.ntts    = coeffParam(input, "NTTS_time_to_service", NEG);
            coeff.wt      = coeffParam(input, "WT_waiting_time", NEG);
            coeff.st      = coeffParam(input, "ST_shift_time", NEG);
            coeff.uw      = coeffParam(input, "UW_weight_type_correction", POS);
            coeff.tg      = coeffParam(input, "TG_run_to_start", NEG);
            coeff.rp      = coeffParam(input, "RP_reserve_penalty", NEG);
            coeff.ou      = coeffParam(input, "OU_order_util", POS);

            allowance = new Allowance();
            allowance.teamChange = allowanceParam(input, "team_change",  H);
            allowance.teamChangePeriod = allowanceParam(input, "team_change_period", H);
            allowance.slotSearch = allowanceParam(input, "slot_search", H);
            allowance.additional = allowanceParam(input, "additional", H);

            maxAssignmentTurns = (int) param(input, "max_assignment_turns");
            useAnnealing = param(input, "annealing") > 0L;
            useHybrid = param(input, "hybrid") > 0L;
            minReserveTrainId = param(input, "min_reserve_loco_train_id");
            maxReserveTrainId = param(input, "max_reserve_loco_train_id");
            reserveTrainIdSeqDigits = (int) param(input, "reserve_loco_train_id_seq_digits");
            reserveTrainIdPrefix = input.getConfigParam("train/reserve_loco_prefix").longValue();
        }

        private String[] describe() {
            String[] descs = {
                String.format("Пороги отсечения ЛП: " +
                                  "WT_max=%d; ST_max=%d, NTTS_min=%d",
                              maxWaitingTime, maxShiftTime, minTimeToService),
                String.format("Коэффициенты ф-ции полезности ЛП: " +
                                  "TT=%.3f, TTS=%.3f, NTTS=%.3f, WT=%.3f, " +
                                  "ST=%.3f, OU=%.3f, UW=%.3f, TG=%.3f, RP=%.3f",
                              coeff.tt, coeff.tts, coeff.ntts, coeff.wt,
                              coeff.st, coeff.ou, coeff.uw, coeff.tg, coeff.rp),
                String.format("Временные допуски: " +
                                  "на смену бригады %d каждые %d, " +
                                  "дополнительный %d",
                              allowance.teamChange, allowance.teamChangePeriod,
                              allowance.additional)
            };
            return descs;
        }
    }
    private Params params;

    private CurrentRunners currentRunners;
    private Regions regions;
    private Relocations relocations;

    private Boolean preplanning = false;
    public SchedulingFrame preplan(SchedulingFrame frame)
    throws InterruptedException {
        preplanning = true;
        try { plan(frame); } finally { preplanning = false; }
        return frame.nextFrame();
    }

    public SchedulingFrame plan(SchedulingFrame frame)
    throws InterruptedException {
        CHECK_INTERRUPTED();
        if (frame.locoFrameIndex == 0) {
            if (preplanning) {
                params = new Params(SchedulingData.getInputData());
                currentRunners = new CurrentRunners();
                regions = new Regions();
                relocations = new Relocations();
                if (LOGGER().isDebugEnabled()) {
                    String[] descs = params.describe();
                    for (int i = 0; i < descs.length; ++i)
                        LOGGER().debug(descs[i]);
                }
                LOGGER().info("Установка приоритетов локомотивных плеч...");
                for (Train train : frame.data.getTrains())
                    LocoRegionPriority.initPriorities(train);
                regions.init(frame);
            }
            CHECK_INTERRUPTED();
            LOGGER().info((preplanning ? "Предварительная" : "Повторная") +
                          " привязка локомотивов, фактически следующих с поездами...");
            currentRunners.assign(frame, preplanning);
        } else {
            LOGGER().info((preplanning ? "Предварительная" : "Повторная") +
                          " привязка на тяговых плечах в интервале " +
                          frame.locoFrameStart + "—" + frame.locoFrameEnd +
                          " (" + frame.locoFrameIndex + ")...");
            regions.assign(frame, preplanning);
            if (preplanning && frame.locoFrameEnd > frame.rangeEnd) {
                LOGGER().info("Привязка по регулировочным заданиям...");
                relocations.process();
            }
        }
        return frame.nextLocoFrame();
    }

    public void cancelEvents(Loco loco, Integer runIndex,
                             Integer locoFrameIndex, Integer teamFrameIndex) {
        if (loco == null) return;
        LOGGER().debug("Отмена событий для локомотива " + loco.getId() +
                           " начиная с интервала #" + runIndex +
                           "-" + locoFrameIndex + "-" + teamFrameIndex);
        List<Loco.Event> cancelled =
            loco.cancelEvents(runIndex, locoFrameIndex, teamFrameIndex);
        for (Loco.Event evt : cancelled) {
            if (evt instanceof LocoAssignEvent) {
                Long trainId = ((LocoAssignEvent) evt).getTrainId();
                Integer start = ((LocoAssignEvent) evt).getStartIndex();
                Train train = SchedulingData.getFrameData().getTrain(trainId);
                TrainPlanner.instance()
                            .cancelEvents(train, start, runIndex,
                                          evt.getLocoFrameIndex(),
                                          evt.getTeamFrameIndex());
            }
        }
    }

    private Loco preplannedLoco(Loco loco) {
        Loco preplanned = new Loco(loco);
        Loco.EventContainer events = loco.getEvents();
        Loco.Event evt = events.firstEvent(-1, -1, -1);
        if (evt != null) preplanned.addEvent(evt);
        LocoServiceEvent sEvt = events.firstEvent(LocoServiceEvent.class);
        if (sEvt != null) preplanned.addEvent(sEvt);
        return preplanned;
    }

    public void resetLocosToPreplanned(SchedulingData data) {
        for (Loco loco : data.getLocos()) {
            data.putLoco(preplannedLoco(loco));
        }
    }

    private static LocoPlanner thePlanner = null;

    public static synchronized LocoPlanner instance() {
        if (thePlanner == null) thePlanner = new LocoPlanner();
        return thePlanner;
    }

    public static synchronized LocoPlanner reinstance() {
        return (thePlanner = new LocoPlanner());
    }

    public static synchronized LocoPlanner uninstance() {
        return (thePlanner = null);
    }

    public static CurrentRunners currentRunners() {
        return instance().currentRunners;
    }

    public static Regions regions() {
        LocoPlanner plnr = instance();
        if (plnr.regions == null)
            LOGGER().error("Неожиданный нуль в поле regions объекта " + plnr);
        return plnr.regions;
    }

    public static Relocations relocations() {
        return instance().relocations;
    }

    public static Params params() {
        return instance().params;
    }

}
