package ru.programpark.planners.check.output;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.loco.LocoRelocation;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.raw_entities.SlotTrain;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;
import ru.programpark.planners.util.AnalyzeHelper;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Created by oracle on 15.10.2015.
 */
public class SlotLocoChecker {
    private static double MIN_LOCO_CHANGE_TIME = 3600L;//1 hour
    public static long MAX_LOCO_CHANGE_TIME = 4 * 3600L;
    private static PrintWriter writer = null;
    private static NumberFormat formatter = new DecimalFormat("#0.00");

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    public static boolean checkLongStayForLocoChange_3_1_5(Collection<SlotTrain> trains, Collection<SlotLoco> locos) { //3.1.5  (Долгое время стоянок поездов на станциях смены локомотивов)
        String testName = "Тест 3.1.5  (Долгое время стоянок поездов (>" + MAX_LOCO_CHANGE_TIME +") на станциях смены локомотивов)";
        boolean pass = true;

        for (SlotTrain train : trains) {
            SlotLoco priorSlotLoco = null;
            BaseTrack priorTrack = null;
            for (BaseTrack track : train.route) {
                SlotLoco loco = AnalyzeHelper.getSlotLocoBySlotTrainAndTrack(train, track, locos);
                if (loco != null && priorSlotLoco != null) {
                    if (!priorSlotLoco.id.equals(loco.id)) {
                        if (track.timeStart - priorTrack.timeEnd > MAX_LOCO_CHANGE_TIME) {
                            if (pass)
                                log(testName + " не пройден ");
                            pass = false;
                            log("Долгое время стоянки на смену локо у поезда id:" + train.id + " на станции " + track.stationFromId
                                    + ", поезд приехал в " + new Time(priorTrack.timeEnd).getTimeStamp() + ", а уехал в " +  new Time(track.timeStart).getTimeStamp() +
                                    ". Всего ушло на смену - " + formatter.format((track.timeStart - priorTrack.timeEnd)/3600.0) + " ч");
                        }
                    }
                }
                priorSlotLoco = loco;
                priorTrack = track;
            }
        }
        return pass;
    }

    public static boolean checkEarlyLoco_3_1_8(InputData input, Collection<SlotLoco> locos) {
        boolean pass = true;
        String testName = "Тест 3.1.8  (Отправление локомотива раньше времени явки)";

        for (SlotLoco loco : locos) {
            long factTime;
            FactLoco factLoco = input.getFactLocos().get(loco.id);
            if (factLoco.getTrack() != null) {
                factTime = factLoco.getTrack().getTimeDepart();
            } else if (factLoco.getLocoArrive() != null) {
                factTime = factLoco.getLocoArrive().getTime();
            } else {
                factTime = factLoco.getTimeOfLocoFact();
            }

            if (loco.route.size() > 0) {
                if (factTime > loco.route.get(0).timeStart) {
                    if (pass)
                        log(testName + " не пройден ");
                    pass = false;
                    log("Локомотив " + loco.id + " начал работу в " + new Time(loco.route.get(0).timeStart).getTimeStamp() + " , а время явки - " + new Time(factTime).getTimeStamp());
                }
            }
        }

        return pass;
    }

    public static boolean checkAllLocoRelocationsProcessed_3_1_12(InputData iData, Map<Long, SlotLoco> sLocos){ // 3.1.12     Выполнение регулировочных заданий
        boolean pass = true;
        String testName = "Тест 3.1.12 (Выполнение всех регулировочных заданий)";

        relocation: for (LocoRelocation r: iData.getRelocations()) {
            int count = 0;
            slotLoco: for (SlotLoco loco: sLocos.values()) {
                if (loco.countsForRelocation(r)) {
                    count++;
                }
            }
            if (count < r.getNumber()) {
                if (pass){
                    log(testName + " не пройден");
                }
                pass = false;
                log("Рел. задание через станцию " + r.getLinkTo().getTo().getName() + " в интервале " + new Time(r.getTime()).getTimeStamp() + " - "
                        + new Time(r.getTime() + r.getInterval()).getTimeStamp() +  " не выволнено");
                log("необходимо было переслать " + r.getNumber() + " локомотивов резервом, но было переслано " + count);
            }
        }


        return pass;
    }

    public static boolean checkCorrectDirection(InputData input, Map<Long, SlotLoco> locos) {
        Long inspectedStation = 2000036154L;
        Long stationForEarlyLoco = 2000036932L;
        Long stationForLastLoco = 2000036192L;
        String testName =   "Тест сценария 3 (Локомотив едет из Северобайкальска в сторону ПТОЛ)";
        boolean result = true;

        List<FactLoco> inspectedLocos = AnalyzeHelper.getFactLocoOnStation(input, inspectedStation);
        AnalyzeHelper.sortFactLocosOrderByTimeToService(inspectedLocos);
        SlotLoco earlyLoco =  locos.get(inspectedLocos.get(0).getId());
        SlotLoco lastLoco = locos.get(inspectedLocos.get(inspectedLocos.size() - 1).getId());

        boolean checkEarlyLoco = false;
        for (BaseTrack track : earlyLoco.route) {
            if (track.stationToId.equals(stationForEarlyLoco)) {
                checkEarlyLoco = true;
                break;
            }
        }

        boolean checkLastLoco = false;
        for (BaseTrack track : lastLoco.route) {
            if (track.stationToId.equals(stationForLastLoco)) {
                checkLastLoco = true;
                break;
            }
        }

        result = checkEarlyLoco && checkLastLoco && (inspectedLocos.size() >= 2);
        if (!result)
            log(testName + " не пройден");

        return result;
    }

    public static boolean checkManualBindingWithAccordanceToTimeTilService_3_1_18(InputData iData, Map<Long, SlotLoco> sLocos){
        boolean pass= true;
        long workintTimeAccordingToFactLoco = 0L;
        Map<Long, FactLoco> fLocos = iData.getFactLocos();
        String testName = "Тест 3.1.18 (Для всех локомотивов в работе с поездом на начало планирования проверяется время до ТО)";

        for (FactLoco fLoco: fLocos.values()) {
            SlotLoco loco = null;
            if (fLoco.getLocoArrive() != null || fLoco.getTrack() != null){
                loco = sLocos.get(fLoco.getId());
                if (loco == null)
                    continue;
            } else
                continue;

            workintTimeAccordingToFactLoco = loco.route.get(loco.route.size() - 1).timeEnd - loco.route.get(0).timeStart;
            try {
                long timeToService = fLocos.get(loco.id).getTimeOfServiceFact();
                if (timeToService < workintTimeAccordingToFactLoco) {
                    if (pass) {
                        log(testName + " не пройден");
                    }
                    String info = " Для локомотива " + loco.id +
                            " полное рабочее время в соответствии с маршрутом из факт. локо составляет " + workintTimeAccordingToFactLoco / 3600.0 +
                            " ч, а время до ремонта передано " + timeToService / 3600.0 + " ч";
                    log(info);
                    pass = false;
                }
            } catch (Exception e) {
                LoggingAssistant.logException(e);
            }
        }

        return pass;
    }

    public static boolean checkLocoInOddDirection_3_1_20(InputData input, Collection<SlotTrain> trains){//3.1.20
        boolean result = true;
        String testName = "Тест 3.1.20 (Поток локомотивов в нечетном направлении)";

        int counter = 0;
        for (SlotTrain train : trains) {
            boolean reserv = train.id < 5000L;
            BaseTrack track = train.route.get(0);
            Link link = input.getLinkByStationPair(new StationPair(track.stationFromId, track.stationToId));
            boolean inspectedDirection = link.getDirection() == 1;

            if (reserv && inspectedDirection) {
                counter++;
            }
        }

        if (counter <= 10) {
            result = false;
            log(testName + " не пройден: Количество резервных поездов, следующих в нечетном направлении равно " + counter + " а надо > 10");
        }

        return result;

    }

    public static boolean checkLocoOrder_3_1_23(InputData input, Map<Long, SlotLoco> slotLocos, Station inspectedStation){
        boolean pass = true;
        String testName = "Тест 3.1.23  (Уход локомотивов по очереди готовности на станциях смены локо)";

        List<FactLoco> inspectedLocos = AnalyzeHelper.getFactLocoOnStation(input, inspectedStation.getId());
        if (inspectedLocos.size() > 0){
            Collections.sort(inspectedLocos, new Comparator<FactLoco>() {
                @Override
                public int compare(FactLoco o1, FactLoco o2) {
                    return o1.getTimeOfLocoFact().compareTo(o2.getTimeOfLocoFact());
                }
            });
        } else
            return true;

        List<SlotLoco> checkedLocos = new ArrayList<>();
        for (FactLoco loco : inspectedLocos) {
            checkedLocos.add(slotLocos.get(loco.getId()));
        }
        Collections.sort(checkedLocos, new Comparator<SlotLoco>() {
            @Override
            public int compare(SlotLoco o1, SlotLoco o2) {
                if (o1 == null)
                    return -1;
                if (o2 == null)
                    return 1;
                if (o1 != null && o2 != null && o1.route.size() == 0 || o2.route.size() == 0) {
                    return 0;
                }
                return o1.route.get(0).timeStart.compareTo(o2.route.get(0).timeStart);
            }
        });

        if (inspectedLocos.size() == checkedLocos.size()) {
            for (int i = 0; i < inspectedLocos.size(); i++) {
                FactLoco expectedFactLoco = inspectedLocos.get(i);
                SlotLoco expectedLoco = slotLocos.get(inspectedLocos.get(i).getId());
                FactLoco actualFactLoco = input.getFactLocos().get(expectedLoco.id);
                if (!expectedFactLoco.getId().equals(checkedLocos.get(i).id)) {
                    if (pass)
                        log(testName + " не проходит на станции " + inspectedStation.getName());
                    pass = false;
                    log("Локомотив " + actualFactLoco.getId() + " находится не на своем месте id " + checkedLocos.get(i).id
                            + " время готовности из fact_loco " + new Time(actualFactLoco.getTimeOfLocoFact()).getTimeStamp()
                            + ", а реальное время отправления - " + new Time(expectedLoco.route.get(0).timeStart).getTimeStamp());
                }
            }
        } else {
            if (pass)
                log(testName + " не проходит");
            pass = false;
        }

        return pass;
    }

    public static boolean checkStopLengthForLocoChange_3_1_24(InputData input, Map<Long, SlotTrain> slotTrains){ //3.1.24
        //Для каждого slotTrain проверяется, что если он проходит станцию смены локо, то стоянка там больше необходимого минимума для смены локо.
        boolean pass = true;
        String testName = "Тест 3.1.24  (Стоянки на станциях смены локо больше " + MIN_LOCO_CHANGE_TIME + ")";
        SlotTrain.Track prevTrack = null;

        for (SlotTrain train: slotTrains.values()) {
            for (SlotTrain.Track track: train.route) {
                if (prevTrack == null)
                    continue;
                Station s = input.getStationById(track.stationFromId);
                Long stopTime = prevTrack.timeEnd - track.timeStart;
                if (s.getProcessTime() > 0L) {//является станцией смены локо
                    if (stopTime < MIN_LOCO_CHANGE_TIME) {
                        if (pass)
                            log(testName + " не пройден");
                        pass = false;
                        log("Стоянка на станции " + s.getName() + " меньше необходимого минимума для смены локо " + MIN_LOCO_CHANGE_TIME +
                                ". Передано время смены локо  " + s.getProcessTime() + ", а стоянка там - " + stopTime);
                    }
                }
                prevTrack = track;
            }
        }

        return pass;
    }

    public static boolean checkReserveLocoCrossing_3_1_26(InputData input, Map<Long, SlotLoco> slotLocos){ //3.1.26
        // Резервные локомотивы не движутся во встречном направлении на горизонте планирования.
        // Составляем набор SlotLoco.track треков резервом и проверяем на возможное пересечение всех со всеми. Время не учитывается.
        boolean pass = true;
        String testName = "Тест 3.1.26  (Резервные локомотивы не движутся во встречном направлении на горизонте планирования)";
        Set<SlotLoco.Track> reserveTracks = new HashSet<>();

        for (SlotLoco loco: slotLocos.values()) {
            for (SlotLoco.Track track: loco.route) {
                if (track.state.equals(BaseLocoTrack.State.RESERVE.ordinal())) {
                    reserveTracks.add(track);
                }
            }
        }

        for (SlotLoco.Track trackOne: reserveTracks){
            for (SlotLoco.Track trackTwo: reserveTracks){
                if (trackOne.stationFromId.equals(trackTwo.stationToId) && trackOne.stationToId.equals(trackTwo.stationFromId)) {
                    if (pass)
                        log(testName + " не пройден");
                    pass = false;
                    log("На участке " + trackOne.stationFromId + "-" + trackOne.stationToId + " локомотивы следуют резервом во встречном направлении на горизонте планирования. См. поезда " +
                        trackOne.trainId + " и " + trackTwo.trainId);
                }
            }
        }

        return pass;
    }

    public static boolean checkReserveLocoInOddDirectionHaveTeams_3_1_27(InputData input, Map<Long, SlotLoco> slotLocos, Map<Long, SlotTeam> slotTeams){ //3.1.27 Под все локомотивы в нечетном направлении подвязаны бригады.
        /*
         Создаем карту по станции отправления, кладем туда SlotTeam.Track треков.
         Цикло по slotLocos
         Цикл по трекам
         Проверяем что локо следует резервом на данном треке
         Проверяем что линк из трека нечетный
         Проверяем есть ли SlotTeam.Track с locoId из slotLoco
         Если нет, тест не пройден, для данного локо и трека выводим сообщения, что не подвязана бригада
         */
        boolean pass = true;
        String testName = "Тест 3.1.27  (Под все локомотивы в нечетном направлении подвязаны бригады)";
        Map<Long, List<SlotTeam.Track>> teamTracksByStationFromId = new HashMap<>();

        for (SlotTeam team: slotTeams.values()){
            for (SlotTeam.Track track: team.route){
                if (teamTracksByStationFromId.get(track.stationFromId) == null)
                    teamTracksByStationFromId.put(track.stationFromId, new ArrayList<SlotTeam.Track>());
                teamTracksByStationFromId.get(track.stationFromId).add(track);
            }

        }

        for (SlotLoco loco: slotLocos.values()){
            for (SlotLoco.Track locoTrack: loco.route){
                if (locoTrack.state.equals(BaseLocoTrack.State.RESERVE.ordinal())){//локо следует резервом
                    Link link = input.getLinkByStationPair(new StationPair(locoTrack.stationFromId, locoTrack.stationToId));
                    if (link.getDirection().equals(1L)){ //направление нечетное
                        for (SlotTeam.Track teamTrack: teamTracksByStationFromId.get(locoTrack.stationFromId)) {
                            if (!teamTrack.locoId.equals(loco.id)) {
                                if (pass) {
                                    log(testName + " не пройден");
                                }
                                pass = false;
                                log("Для резервного локомотива " + loco.id + " на нечетном участке " + locoTrack.stationFromId + " - " + locoTrack.stationToId + " не подвязана бригада");
                            }
                        }
                    }
                }
            }
        }

        return pass;
    }
}
