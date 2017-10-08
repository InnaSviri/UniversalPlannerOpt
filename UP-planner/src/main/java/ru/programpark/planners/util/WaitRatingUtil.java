package ru.programpark.planners.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.fixed.Station;
import ru.programpark.planners.common.Loco;
import ru.programpark.planners.common.SchedulingData;
import ru.programpark.planners.common.Team;
import ru.programpark.planners.common.Train;
import ru.programpark.planners.team.TeamPlanningParams;
import ru.programpark.planners.team.TeamSlot;

import java.util.List;
import java.util.Map;

public abstract class WaitRatingUtil {
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(WaitRatingUtil.class);
        return logger;
    }

    public static double accountTeamWaitTime(List<TeamSlot> teamSlots, Team readyTeam, TeamSlot loco,
                                             Long whenTeamWillBeRelocated, Long timeEndPlanningIteration) {
        /* Если T L > T i (бригада приходит позже времени готовности локомотива), то значение по этому критерию U = 0.
        Если T L <= T i, то качестве слагаемого ФП по этому критерию берем значение U = (T 0 - T L) / exp(k*(T i - T 0)).
        Тогда для самого раннего
        подходящего локомотива это значение будет равно (T 0 - T L) — времени ожидания бригадой локомотива.
        И оно будет тем больше, чем раньше бригада
        готова на станции к отправлению. Для следующих локомотивов это значение будет убывать по экспоненте,
        коэффициент экспоненты k можно подобрать
        экспериментально (если все времена выражать в секундах, то в качестве k предлагается взять 1/3600).
        Теперь это слагаемое в общей функции полезности будет работать не как штраф, а как выгода
        (весовой коэффициент — положительный). Нормировка на
        единицу обязательна.*/
        if (readyTeam.equals(null) || readyTeam.lastEvent().getStation().equals(null) || loco.equals(null)) {
        //временная затычка - падало на строке !team.station.equals(sLocoStart) todo
            LOGGER().debug("В accountTeamWaitTime пришли невалидные данные: team = " + readyTeam +  ", loco = " + loco);
            return 0.0;
        }

        double k = 1.0/3600;
        long Tteam = readyTeam.timeOfPresence(); // TL — время готовности бригады
        long Tloco = 0L;// T i — время готовности к отправлению i-го доступного локомотива
        long Tearliest = Long.MAX_VALUE;// Пусть T 0 — время готовности самого раннего локомотива,
        // но не раньше времени готовности бригады (T 0 = min(T i; T i >= T L)).
        TeamPlanningParams params = new TeamPlanningParams(SchedulingData.getInputData());
        long tMax = timeEndPlanningIteration + params.maxTimeLocoWaitsForTeam;

        for (TeamSlot teamSlot: teamSlots){
            if (teamSlot.route.size() == 0) continue;
            //учитываем только использующиеся в расчете локомотивы
            if (teamSlot.route.get(0).getTimeStart() > timeEndPlanningIteration) {
                continue;
            }

            Tloco = teamSlot.route.get(0).getTimeStart();
            Station sLocoStart = teamSlot.route.get(0).getLink().getFrom();
            if (!readyTeam.lastEvent().getStation().equals(sLocoStart)){ //Не забыть учесть, что бригада может
            // изначально находиться не на той станции, с которой стартует локомотив, и может пересылаться пассажиром.
                //Тогда время ее готовности в этом случае — это время прибытия ее пассажиром.

                if (whenTeamWillBeRelocated > 0){// -1L если не найден ни один поезд и ни одна нитка...
                    Tteam = whenTeamWillBeRelocated;
                } else {
                    LOGGER().debug("В accountTeamWaitTime: -1000 возвращено в качестве totalUtility");
                    return -1000.0;  // такого быть уже не должно, перенесено до вызова задачи о назначениях
                }
            }
            if (Tloco <= Tearliest /*&& Tloco >= Tteam*/){//но не раньше времени готовности
            // бригады (T 0 = min(T i; T i >= T L)).
                Tearliest = Tloco;
                LOGGER().trace(String.format("tearliest loco id:%d", teamSlot.locoId));
            }
        }

        long Tcrit = Tearliest - 2 * 3600;  // Параметр нужен, чтобы бригады с древним временем явки
        // не перебивали по полезности оборотные бригады
        if (Tteam > tMax){
            return 0.0;
        } else {
            double a = 0.0;
            // Чем ранее доступна бригада, тем больше множитель перед экспонентой
            // Зависимость множителя от времени готовности бригады - линейная
            // Для старых бригад тоже линейная зависимость, но с много меньшим углом наклона
            if (Tteam >= Tcrit) {
                a = tMax - Tteam;
            } else {
                a = (10 * tMax - 9 * Tcrit - Tteam) / 10;
            }
            // U = (T 0 - T L) / exp(k*(T i - T 0))
            double u = a / Math.exp(k * (loco.route.get(0).getTimeStart() - Tearliest));
            LOGGER().trace(String.format("Локомотив %d Бригада %d" + " (tMax - Tteam) %d, exp: %f",
                    loco.locoId, readyTeam.getId(),
                    (tMax - Tteam), Math.exp(k*(loco.route.get(0).getTimeStart() - Tearliest))
            ));
            return u/10000.0;

        }
    }

    public static double accountLocoWaitTime(List<Train> trains, Loco loco,
                                             Train train, Long timeEndPlanningIteration) {
        long tLoco = loco.lastEvent().getLocoReadyTime();
        long tTrain = 0L; // T_i — время готовности к отправлению i-го доступного поезда
        long tMin = Long.MAX_VALUE; // T_0 — время готовности самого раннего поезда
        long tMax = timeEndPlanningIteration + (6L * 3600L);

        for (Train tr: trains){
            long tTr = tr.lastEvent(tr.getUnassignedLocoIndex() - 1).getTrainReadyTime();
            if (tTr < tMin) tMin = tTr;
            if (tr == train) tTrain = tTr;
        }

        return accountLocoWaitTime(tLoco, tTrain, tMin, tMax);
    }

    public static double accountLocoWaitTime(Map<Train, Long> trainTimes, Loco loco,
                                             Train train, Long timeEndPlanningIteration) {
        return accountLocoWaitTime(loco.lastEvent().getLocoReadyTime(),
                                   trainTimes.get(train),
                                   trainTimes.values().iterator().next(),
                                   timeEndPlanningIteration + (6L * 3600L));
    }


    public static double accountLocoWaitNorm(Long timeStartPlanningIteration,
                                             Long timeEndPlanningIteration) {
        return ((timeEndPlanningIteration - timeStartPlanningIteration) +
                    2L * 3600 + 6L * 3600) / 10000.0;
    }

    public static double accountLocoWaitTime(Long tLoco, Long tTrain,
                                             Long tMin, Long tMax) {
        double k = 1.0/3600;
        // Параметр нужен, чтобы бригады с древним временем явки не
        // перебивали по полезности оборотные бригады:
        long tCrit = Math.min(tMin - 2 * 3600, tMax);
        if (tLoco > tMax){
            return 0.0;
        } else {
            double a = (tLoco >= tCrit) ? tMax - tLoco : (10 * tMax - 9 * tCrit - tLoco) / 10;
            double s = k * (tTrain - tMin);
            double u = a / Math.exp(s);
            return u/10000.0;
        }
    }

}
