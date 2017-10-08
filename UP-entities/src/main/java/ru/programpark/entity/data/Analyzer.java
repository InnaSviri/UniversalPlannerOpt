package ru.programpark.entity.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.*;
import ru.programpark.entity.raw_entities.*;
import ru.programpark.entity.team.*;
import ru.programpark.entity.train.*;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Pair;
import ru.programpark.entity.util.ResultParser;

import java.io.IOException;
import java.util.*;

public class Analyzer {
    static protected ResultParser results;
    InputData iData;
    TeamLocoSummaryData eData;
    Map<Long, FactTrain> fTrains;
    Map<Long, FactLoco> fLocos;
    Map<Long, FactTeam> fTeams;

    public static Set<StationPair> restrictStationPairs = new HashSet<StationPair>() {
        {
            add(new StationPair(2000036538L, 2000036518L));
            add(new StationPair(2000036518L, 2000036538L));
            add(new StationPair(2000036538L, 2000036532L));
            add(new StationPair(2000036532L, 2000036538L));
        }
    };

    public Analyzer(InputData iData, String [] results) {
        fillInputData(iData);
        try {
            this.results = new ResultParser(Arrays.asList(results));
            filterResults();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public Analyzer(InputData iData, ResultParser results) {
        this.results = results;
        filterResults();
    }

    protected void fillInputData(InputData iData) {
        this.iData = iData;
        this.eData = iData.geteData();
        fTrains = new HashMap<>(iData.getFactTrains());
        fLocos = new HashMap<>(iData.getFactLocos());
        fTeams = new HashMap<>(iData.getFactTeams());
                // далее удаляем из входов сущности, содержащие в своем маршруте
                // запрещенные участки планирования (Например, Тайшет-Юрты)
        List<Long> restrictedTrainIds = new ArrayList<>();
        for (FactTrain train: fTrains.values()) {
            aaa: for(Route route: train.getRoutes()) {
                for (Link link: route.getLinkList()) {
                    if (restrictStationPairs.contains(link.getStationPair()) &
                            !restrictedTrainIds.contains(train.getId())) {
                        restrictedTrainIds.add(train.getId());
                        break aaa;
                    }
                }
            }
        }
        for (Long trainId: restrictedTrainIds) {
            fTrains.remove(trainId);
        }
    }

    static protected void filterResults() {  // этот метод удаляет из results бригады,
                                            // локомотивы (и train.track), содержащие в своем маршруте
                                            // запрещенные участки планирования (Например, Тайшет-Юрты)

        // работа с поездами
        List<Long> infectedTrainIds = new ArrayList<>();
        for (SlotTrain train: results.slotTrains.values()) {
            for(SlotTrain.Track track: train.route) {
                if(restrictStationPairs.contains(track.getStationPair())
                & !infectedTrainIds.contains(train.id)) {
                    infectedTrainIds.add(train.id);
                    break;
                }
            }
        }
        // Теперь удалить из рута поездов "зараженные" участки
        for (Long trainId: infectedTrainIds) {
            SlotTrain train = results.slotTrains.get(trainId);
            results.slotTrains.remove(trainId);      // удалить зараженную сущность
            List<SlotTrain.Track> route = new ArrayList<>(train.route);
            for(SlotTrain.Track track: route) {
                if(restrictStationPairs.contains(track.getStationPair())) {
                    train.route.remove(track);
                }
            }
            results.slotTrains.put(trainId, train);  // вставить исцеленный поезд
        }

        // фильтрация локомотивов
        List<Long> restrictedLocoIds = new ArrayList<>();
        for (SlotLoco loco: results.slotLocos.values()) {
            for(SlotLoco.Track track: loco.route) {
                if(restrictStationPairs.contains(track.getStationPair())) {
                    restrictedLocoIds.add(loco.id);
                    break;
                }
            }
        }
        for (Long locoId: restrictedLocoIds) results.slotLocos.remove(locoId);

        // фильтрация бригад
        List<Long> restrictedTeamIds = new ArrayList<>();
        for (SlotTeam team: results.slotTeams.values()) {
            for(SlotTeam.Track track: team.route) {
                if(restrictStationPairs.contains(track.getStationPair())) {
                    restrictedTeamIds.add(team.id);
                    break;
                }
            }
        }
        for (Long teamId: restrictedTeamIds) results.slotTeams.remove(teamId);
    }

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(Analyzer.class);
        return logger;
    }

    private List<SlotLoco.Track> getLocoTracksByTrain(SlotTrain train) {
        List<SlotLoco.Track> tracks = new ArrayList<>();
        for (SlotLoco loco : results.slotLocos.values()) {
            int i = 0;
            for (SlotLoco.Track track : loco.route) {
                try {
                    if (track.trainId.equals(train.id)) {
                        tracks.add(track);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
        return tracks;
    }

    private List<SlotLoco> getLocosByTrain(SlotTrain train, Long endTime) {
        List<SlotLoco> locos = new ArrayList<>();
        for (SlotLoco loco : results.slotLocos.values()) {
            int i = 0;
            for (SlotLoco.Track track : loco.route) {
                try {
                    if (track.trainId.equals(train.id)
                            & track.timeStart < endTime
                            & !locos.contains(loco)) {
                        locos.add(loco);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
        return locos;
    }

    private List<SlotTeam.Track> getTeamByLoco(SlotLoco loco) {
        List<SlotTeam.Track> tracks = new ArrayList<>();

        for (SlotTeam team :  results.slotTeams.values()) {
            for (SlotTeam.Track track : team.route) {
                if (track.locoId.equals(loco.id)) {
                    tracks.add(track);
                }
            }
        }

        return tracks;
    }

    /**
     * Процент незапланированных поездов
     * 1.	Посчитать общее количество поездов, которые должны быть запланированы по объемным заданиям на планирование.
     * 2.	Посчитать количество поездов, возвращенных планировщиком, которые соответствуют объемным заданиям на планирование.
     * 3.	Вычислить количество поездов, которые не были запланированы по объемным заданиям. Вычислить процент незапланированных поездов от общего количества.
     *
     * @return
     */

    // пока нет данных, поэтому не реализовано (код внизу - не проверялся)
    public AnalyzeCriterion getDataForRow1() {
        int allTrainQuantity = iData.getOneTasks().size();
        int counter = 0;
        double rv;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(1L);

        try {
            for (SlotTrain train : results.slotTrains.values()) {
                if (train.id  == -1L) {
                    counter++;
                    analyzeCriterion.addInfoString(train.toString());
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.trainId =  train.id;
                    analyzeCriterion.addInfo(iEntry);

                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow1 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        if (allTrainQuantity == 0) {
            rv = 0.0;
        } else {
            rv = 1 - (double) counter / (double) allTrainQuantity;
        }
        analyzeCriterion.setValue(rv * 100.0);
        return analyzeCriterion;
    }


    /**
     * Процент запланированных фактических поездов
     * 1.	Посчитать общее количество поездов, по которым в планировщик были переданы факты
     *          (отправление, прибытие или завершение формирования).
     *          Вычесть поезда, для которых передан факт прибытия на конечную станцию маршрута.
     * 2.	Посчитать количество поездов, возвращенных планировщиком,
     *      которые соответствуют этим фактическим поездам.
     * 3.	Вычислить процент запланированных фактических поездов от общего количества.
     *
     *  @return
     */
    public AnalyzeCriterion getDataForRow2() {
        int allFactQuantity = fTrains.size();
        int counter = 0;
        double rv;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(2L);

        try {
            for (FactTrain fact : fTrains.values()) {
                if(results.slotTrains.containsKey(fact.getId())) {
                    counter++;
                } else {
                    analyzeCriterion.addInfoString(fact.getId());
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.trainId =  fact.getId();
                    analyzeCriterion.addInfo(iEntry);
                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow2 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        if (allFactQuantity == 0) {
            rv = 0.0;
        } else {
            rv = (double) counter / (double) allFactQuantity;
        }
        analyzeCriterion.setValue(100.0 * rv);
        return analyzeCriterion;
    }

    /**
     * Обеспечение поездов локомотивами на горизонте в 6 часов
     * 1.	Выбрать все поезда, отправление которых запланировано планировщиком от времени начала
     * планирования на горизонте в 6 часов.
     * 2.	Посчитать процент поездов, для которых на каждом участке,
     * на который запланировано отправление поезда на горизонте в 6 часов,
     * планировщиком возвращен прикрепленный локомотив.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow3(long time) {
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;

        int trainCounter = 0;
        int counter = 0;

        double rv;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(3L);

        Map<SlotTrain, List<SlotTrain.Track>> tracks = new HashMap<>();

        try {
            for (SlotTrain train : results.slotTrains.values()) {
                List<SlotTrain.Track> acceptedTrack = new ArrayList<>();
                for (SlotTrain.Track track : train.route) {
                    StationPair checkedPair = new StationPair(track.stationFromId, 
                            track.stationToId);
                    if (!restrictStationPairs.contains(checkedPair)
                            && between(track.timeStart, beginTime, endTime)) {
                        acceptedTrack.add(track);
                    }
                }
                tracks.put(train, acceptedTrack);
            }

            for (SlotTrain train : tracks.keySet()) {
                int trackQuantity = tracks.get(train).size();
                if (trackQuantity == 0) continue;
                trainCounter++;
                List<SlotLoco.Track> locosTrack = getLocoTracksByTrain(train);
                for (SlotTrain.Track track : tracks.get(train)) {
                    for (SlotLoco.Track locoTrack : locosTrack) {
                        if (track.getStationPair().equals(locoTrack.getStationPair()) /*&&
                                track.timeStart.equals(locoTrack.timeStart) &&
                                track.timeEnd.equals(locoTrack.timeEnd)*/) {
                            trackQuantity--;
                        }
                    }
                }
                if (trackQuantity == 0) {
                    counter++;
                } else {
                    analyzeCriterion.addInfoString(train.id);
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.trainId =  train.id;
                    analyzeCriterion.addInfo(iEntry);
                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow3 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        if (trainCounter == 0) {
            rv = -1.0;
        } else {
            rv = (double) counter / (double) trainCounter;
        }
        analyzeCriterion.setValue(rv * 100.0);
        return analyzeCriterion;
    }

    /**
     * Обеспечение локомотивов бригадами на горизонте в 6 часов
     * 1.	Выбрать все локомотивы, отправление которых запланировано планировщиком на горизонте в 6 часов от времени начала планирования.
     * 2.	Посчитать процент локомотивов, для которых на каждом участке, на который запланировано отправление локомотива 
     *      на горизонте в 6 часов, планировщиком возвращена прикрепленная бригада.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow4(long time) {

        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;//
        int locoCounter = 0;
        int assignedCounter = 0;

        double rv;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(4L);


        Map<SlotLoco, List<SlotLoco.Track>> locosTracks = new HashMap<>();

        try {
            for (SlotLoco loco : results.slotLocos.values()) {
                List<SlotLoco.Track> acceptedTracks = new ArrayList<>();
                for (SlotLoco.Track track : loco.route) {
                    StationPair checkedPair = new StationPair(track.stationFromId, track.stationToId);
                    if (!restrictStationPairs.contains(checkedPair)
                            && between(track.timeStart, beginTime, endTime)
                            && !track.stationFromId.equals(track.stationToId)) {
                        acceptedTracks.add(track);
                    }
                }
                locosTracks.put(loco, acceptedTracks);
            }

            locoCounter = locosTracks.size();
            for (SlotLoco loco : locosTracks.keySet()) {
                List<SlotLoco.Track> locoTracks = locosTracks.get(loco);

                boolean assigned = true;
                for (SlotLoco.Track track : locoTracks) {
                    if (!checkAssignedTeam(track)) {
                        assigned = false;
                        if (time == 21600L) {
                            //System.out.println("Необеспеченные бригадами локомотивы на 6-ти часовом интервале: " + loco.id + " " + track.getLink());
                            LOGGER().debug("Необеспеченные бригадами локомотивы на 6-ти часовом интервале: " + loco.id + " " +
                                    track.getStationPair());
                        } else if (time == 3600L * 18L) {
                            //System.out.println("Необеспеченные бригадами локомотивы на 24-х часовом интревале: " + loco.id + " " + track.getLink());
                            LOGGER().debug("Необеспеченные бригадами локомотивы на 18-х часовом интревале: " + loco.id + " "
                                    + track.getStationPair());
                        } else if (time == 3600L * 24L) {
                            LOGGER().debug("Необеспеченные бригадами локомотивы на 24-х часовом интревале: " + loco.id + " " +
                                    track.getStationPair());
                        }
                        String info = "loco(id(" + loco.id
                                + "),train(" + track.trainId
                                + "),track(from(" + track.stationFromId
                                + "),to(" + track.stationToId
                                + ")))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.trainId =  track.trainId;
                        iEntry.locoId = loco.id;
                        iEntry.stationFromId = track.stationFromId;
                        iEntry.stationToId = track.stationToId;
                        analyzeCriterion.addInfo(iEntry);
                        break;
                    }
                }

                if (assigned) {
                    assignedCounter++;
                }

            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow3 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        if (locoCounter == 0) {
            rv = 0D;
        } else {
            rv = (double) assignedCounter / (double) locoCounter;
        }
        analyzeCriterion.setValue(rv * 100.0);
        return analyzeCriterion;
    }

    private boolean between(long checkTime, long beginTime, long endTime) {
        return (checkTime >= beginTime && checkTime < endTime);
    }

    private boolean checkAssignedTeam(BaseTrack locoTrack) {
        for (SlotTeam team : results.slotTeams.values()) {
            for (BaseTrack track : team.route) {
                if (track.equalFilelds(locoTrack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Процент поездов, обеспеченных локомотивами на горизонте в 24 часа
     * 1.	Выбрать все поезда, отправление которых запланировано планировщиком от времени начала планирования на горизонте в 24 часа.
     * 2.	Посчитать процент поездов, для которых на каждом участке, на который запланировано отправление поезда на горизонте в 24 часа, планировщиком возвращен прикрепленный локомотив.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow5() {
        AnalyzeCriterion analyzeCriterion = getDataForRow3(86400L);
        analyzeCriterion.setId(5L);
        return analyzeCriterion;
    }

    /**
     * Процент локомотивов, обеспеченных бригадами на горизонте в 24 часа
     * 1.	Выбрать все локомотивы, отправление которых запланировано планировщиком на горизонте в 24 часа от времени начала планирования.
     * 2.	Посчитать процент локомотивов, для которых на каждом участке, на который запланировано отправление локомотива на горизонте в 24 часа, планировщиком возвращена прикрепленная бригада.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow6() {
        AnalyzeCriterion analyzeCriterion = getDataForRow4(86400L);
        analyzeCriterion.setId(6L);
        return analyzeCriterion;
    }

    /**
     * Средняя скорость поездов км/сут ходовая
     * 1.	Для всех запланированных поездов рассчитать общее время хода (время прибытия на последнюю запланированную станцию минус время
     * отправления на первый запланированный участок) и общее расстояние (сумма длин запланированных участков). По этим данным найти среднюю
     * скорость (частное от деления расстояния на время хода).
     *
     * @return
     */

    /**
     * Средняя скорость поездов км/ч техническая
     * 1.	Для всех запланированных поездов рассчитать общее время хода без учета стоянок поездов на станциях (просуммировать времена
     * хода по каждому участку) и общее расстояние (сумма длин запланированных участков). По этим данным найти техническую скорость
     * (частное от деления расстояния на время хода).
     *
     * @return
     */

    public Pair<AnalyzeCriterion, AnalyzeCriterion>  getDataForRow7_8(long time) {
        long allTime1 = 0;
        long allTime2 = 0;
        long allDistance = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;


        AnalyzeCriterion analyzeCriterion1 = new AnalyzeCriterion(7L);
        AnalyzeCriterion analyzeCriterion2 = new AnalyzeCriterion(8L);

        for (SlotTrain train : results.slotTrains.values()) {
            try {
                List<SlotTrain.Track> route = train.route;
                int totTracks = route.size();
                long finishTime = 0 ;
                if(totTracks > 0) {
                    for (SlotTrain.Track track : route) {
                        if(track.timeEnd > endTime) {           // маршрут длинее периода
                            finishTime = endTime;
                            if (track.timeStart < endTime) {    // текущий участок начинается внутри периода
                                allDistance += (endTime - track.timeStart) /
                                        (track.timeEnd - track.timeStart) *
                                        iData.getLinkByStationPair(track.getStationPair()).getDistance();
                                allTime2 += endTime - track.timeStart;
                            }
                            break;
                        } else {
                            allDistance += iData.getLinkByStationPair(track.getStationPair()).getDistance();
                            allTime2 += track.timeEnd - track.timeStart;
                            finishTime = track.timeEnd;
                        }
                    }
                    allTime1 += finishTime - Math.min(endTime,train.route.get(0).timeStart);
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow7_8 exception: " + e.getMessage() + " train: " + train.id);
                LoggingAssistant.logException(e);
            }
        }

        if (allTime1 == 0L) {
            analyzeCriterion1.setValue(-1.0);
        } else {
            analyzeCriterion1.setValue(1.0 * time * allDistance / allTime1);
        }

        if (allTime2 == 0L) {
            analyzeCriterion2.setValue(-1.0);
        } else {
            analyzeCriterion2.setValue(3600.0 * allDistance / allTime2);
        }

        return new Pair<>(analyzeCriterion1,analyzeCriterion2);
    }

    /**
     * Общее и среднее время на перемещение локомотивов резервом ч
     * 9:
     * 1.	Выбрать все фрагменты маршрутов локомотивов, на которых планировщиком запланировано следование резервом.
     * 2.	Посчитать время пересылки для каждого локомотива (разность между временем прибытия на конечную станцию и временем отправления с первой станции).
     * 3.	Посчитать сумму по всем таким локомотивам.
     * 10:
     * 1.	Сумму, полученную по критерию 9, разделить на количество локомотивов, запланированных к перемещению резервом.
     * Получить таким образом среднее время пересылки для каждого локомотива.
     *
     * @return
     */
    public Pair<AnalyzeCriterion, AnalyzeCriterion> getDataForRow9_10() {
        double rv;
        int counter = 0;
        AnalyzeCriterion analyzeCriterion1 = new AnalyzeCriterion(9L);
        AnalyzeCriterion analyzeCriterion2 = new AnalyzeCriterion(10L);
        long allTime = 0;

        for (SlotLoco loco : results.slotLocos.values()) {
            boolean b = false;
            boolean reserve = false;
            long startReserveTime = 0L;
            long trackEndTime = 0L;
            int index = -1;
            long locoTime = 0L;
            Long stationFromId = 0L;
            Long stationToId = 0L;
            try {
                for (SlotLoco.Track track : loco.route) {
                    index++;
                    boolean success = false;
                    if (track.state.intValue() == BaseLocoTrack.State.RESERVE.ordinal()) {
                        if (!reserve) {
                            startReserveTime = track.timeStart;
                            stationFromId = track.stationFromId;
                            reserve = true;
                        }
                        trackEndTime = track.timeEnd;
                        stationToId = track.stationToId;
                        b = true;

                        //Если это последний участок, и он резервный
                        if (index == loco.route.size() - 1) {
                            success = true;
                        }
                    } else {
                        //Если очередной участок не резервный, а предыдущий был резервным
                        //if (track.state != BaseLocoTrack.State.RESERVE) {
                        if (reserve) {
                            success = true;
                            reserve = false;
                        }
                    }
                    if (success) {
                        locoTime += (double) (trackEndTime - startReserveTime);
                        String info = "loco(id(" + loco.id
                                + "),from(" + stationFromId
                                + "),to(" + stationToId
                                + "),time(" + locoTime / 3600.0 + "))";
                        analyzeCriterion2.addInfoString(info);

                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.locoId = loco.id;
                        iEntry.stationFromId = track.stationFromId;
                        iEntry.stationToId = track.stationToId;
                        iEntry.timeDuration = locoTime;
                        analyzeCriterion2.addInfo(iEntry);
                    }
                }
                if (b) {
                    counter++;
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow9-10 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
            allTime += locoTime;
        }
        if (counter == 0) {
            rv = -1.0;
        } else {
            rv = allTime / (double) counter / 3600.0;
        }
        analyzeCriterion1.setValue(allTime / 3600.0);
        analyzeCriterion2.setValue(rv);
        return new Pair<>(analyzeCriterion1, analyzeCriterion2);
    }

    /**
     * Общее и среднее время пересылки бригад пассажирами
     * 11:
     * 1.	Выбрать все фрагменты маршрутов бригад, на которых планировщиком запланировано следование пассажирами.
     * 2.	Посчитать время пересылки для каждой бригады (разность между временем прибытия на конечную станцию и временем отправления с первой станции).
     * 3.	Посчитать сумму по всем таким бригадам и среднее время пересылки для каждой бригады
     * 12:
     * 1.	Сумму, полученную по критерию 11, разделить на количество бригад, запланированных к перемещению резервом.
     * Получить таким образом среднее время пересылки для каждой бригады.
     *
     * @return
     */
    public Pair<AnalyzeCriterion, AnalyzeCriterion> getDataForRow11_12() {
        double rv = 0;
        int counter = 0;
        AnalyzeCriterion analyzeCriterion1 = new AnalyzeCriterion(11L);
        AnalyzeCriterion analyzeCriterion2 = new AnalyzeCriterion(12L);
        long allTime = 0;

        for (SlotTeam team : results.slotTeams.values()) {
            boolean b = false;
            boolean passenger = false;
            long startPassengerTime = 0L;
            long trackEndTime = 0L;
            int index = -1;
            long teamTime = 0L;
            Long stationFromId = 0L;
            Long stationToId = 0L;
            try {
                for (SlotTeam.Track track : team.route) {
                    index++;
                    boolean success = false;
                    if (track.state.intValue() == BaseTeamTrack.State.PASSENGER.ordinal()) {
                        if (!passenger) {
                            startPassengerTime = track.timeStart;
                            stationFromId = track.stationFromId;
                            passenger = true;
                        }
                        trackEndTime = track.timeEnd;
                        stationToId = track.stationToId;
                        b = true;

                        //Если это последний участок, и он резервный
                        if (index == team.route.size() - 1) {
                            success = true;
                        }
                    } else {
                        //Если очередной участок не резервный, а предыдущий был резервным
                        //if (track.state != BaseTeamTrack.State.PASSENGER) {
                        if (passenger) {
                            success = true;
                            passenger = false;
                        }
                    }
                    if (success) {
                        teamTime += (double) (trackEndTime - startPassengerTime);
                        String info = "team(id(" + team.id
                                + "),from(" + stationFromId
                                + "),to(" + stationToId
                                + "),time(" + teamTime / 3600.0 + "))";
                        analyzeCriterion2.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.teamId = team.id;
                        iEntry.stationFromId = stationFromId;
                        iEntry.stationToId = stationToId;
                        iEntry.timeDuration = teamTime;
                        analyzeCriterion2.addInfo(iEntry);
                    }
                }
                if (b) {
                    counter++;
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow11-12 exception: " + e.getMessage() + " team: " + team.id);
                LoggingAssistant.logException(e);
            }
            allTime += teamTime;
        }
        if (counter == 0) {
            rv = -1.0;
        } else {
            rv = allTime / (double) counter / 3600.0;
        }
        analyzeCriterion1.setValue(allTime / 3600.0);
        analyzeCriterion2.setValue(rv);
        return new Pair<>(analyzeCriterion1, analyzeCriterion2);
    }

    /**
     * Привязка поезда на всем маршруте
     * 1.	Посчитать общее количество поездов, которые должны быть запланированы (по объемным задачам на планирование и по фактам о существующих поездах).
     * 2.	Посчитать количество поездов, которые запланированы до конечной станции маршрута.
     * 3.	Посчитать процент поездов, которые не допланированы до конечной станции маршрута.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow13() {
        double rv = 0;
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(13L);
        for (FactTrain train : fTrains.values()) {
            try {
                if(train.getMainRoute() != null) {
                    boolean success = false;
                    int size = train.getMainRoute().getLinkList().size();
                    if (size == 0) {
                        success = true;
                        break;
                    }
                    if (results.slotTrains.containsKey(train.getId())) {
                        SlotTrain rtrain = results.slotTrains.get(train.getId());
                        int rsize = rtrain.route.size();
                        if (rsize == 0 &&
                                rtrain.route.get(rsize - 1).stationToId.equals(
                                        train.getMainRoute().getLinkList().get(size - 1).getTo().getId())) {
                            success = true;
                        }
                        break;
                    }
                    if (!success) {
                        counter++;
                        analyzeCriterion.addInfoString(train.getId());
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.trainId = train.getId();
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow13 exception: " + e.getMessage() + " train: " + train.getId());
                LoggingAssistant.logException(e);
            }
        }

        if (results.slotLocos.size() == 0) {
            rv = 0.0;
        } else {
            rv = 100.0 * counter / (double) fTrains.size();
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Процент локомотивов, для которых нарушена целостность маршрута
     * 1.	Проверить, что конечная станциях каждого участка на маршруте локомотива является начальной станцией следующего участка на маршруте (то есть нет «скачков» между станциями).
     * 2.	Посчитать процент запланированных локомотивов, для которых это условие не выполняется.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow14() {
        double rv;
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(14L);
        for (SlotLoco loco : results.slotLocos.values()) {
            Long priorStationId = null;
            try {
                for (SlotLoco.Track track : loco.route) {
                    if (priorStationId != null) {
                        if (!priorStationId.equals(track.stationFromId)) {
                            counter++;
                            analyzeCriterion.addInfoString(loco.id);
                            AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                            iEntry.locoId = loco.id;
                            analyzeCriterion.addInfo(iEntry);
                            break;
                        }
                    }
                    priorStationId = track.stationToId;
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow14 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
        }

        if (results.slotLocos.size() == 0) {
            rv = 0.0;
        } else {
            rv = 100.0 * counter / (double) results.slotLocos.size();
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Процент бригад, для которых нарушена целостность маршрута
     * 1.	Проверить, что конечная станция каждого участка на маршруте бригады является начальной станцией следующего участка на маршруте (то есть нет «скачков» между станциями).
     * 2.	Посчитать процент запланированных бригад, для которых это условие не выполняется.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow15() {
        double rv = 0;
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(15L);

        for (SlotTeam team : results.slotTeams.values()) {
            Long priorStationId = null;
            try {
                for (SlotTeam.Track track : team.route) {
                    if (priorStationId != null) {
                        if (!priorStationId.equals(track.stationFromId)) {
                            counter++;
                            analyzeCriterion.addInfoString(team.id);
                            AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                            iEntry.teamId = team.id;
                            analyzeCriterion.addInfo(iEntry);
                            break;
                        }
                    }
                    priorStationId = track.stationToId;
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow15 exception: " + e.getMessage() + " team: " + team.id);
                LoggingAssistant.logException(e);
            }
        }
        if (results.slotTeams.size() == 0) {
            rv = 0.0;
        } else {
            rv = 100.0 * counter / (double) results.slotTeams.size();
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Средний простой поездов на станциях по суточному маршруту
     * 1.	Для каждой последовательной пары участков маршрута поезда вычислить разность между временем отправления на следующий участок и
     * временем прибытия на конечную станцию предыдущего участка. Так будет рассчитано время простоя поезда на каждой станции.
     * 2.	Вычислить сумму всех времен простоя, разделить на количество промежуточных станций в маршруте.
     * 3.	Вычислить среднее значения простоя для всех запланированных поездов.
     */
    public AnalyzeCriterion getDataForRow16(int time) {
        double rv = 0;
        int counter = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;


        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(16L);

        long allTime = 0;
        for (SlotTrain train : results.slotTrains.values()) {
            if(train.id > 4999 || train.id < 4200) {
                try {
                    long maxDelayTime = 0;
                    long delayTime = 0;
                    int nStops = 0;
                    SlotTrain.Track priorTrack = null;
                    for (SlotTrain.Track track : train.route) {
                        if (priorTrack != null &&
                                priorTrack.timeEnd <= endTime) {
                            counter++;
                            nStops++;
                            delayTime = Math.min(track.timeStart, endTime) - priorTrack.timeEnd;
                            allTime += delayTime;
                            if (delayTime > maxDelayTime) {
                                maxDelayTime = delayTime;
                            }
                        }
                        if (track.timeStart > endTime) break;
                        priorTrack = track;
                    }
                    String info = "train(id(" + train.id + "),time(" + maxDelayTime / 3600.0 + ")" +
                            "stops(" + nStops +
                            "))";
                    analyzeCriterion.addInfoString(info);
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.trainId = train.id;
                    iEntry.timeDuration = maxDelayTime;
                    analyzeCriterion.addInfo(iEntry);
                } catch (Exception e) {
                    LOGGER().error("getDataForRow16 exception: " + e.getMessage() + " train: " + train.id);
                    LoggingAssistant.logException(e);
                }
            }
        }
        if (counter == 0) {
            rv = 0.0;
        } else {
            rv = (double) allTime / (double) counter / 3600.0;
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    SlotLoco getLocoByTrack(SlotTeam.Track track) {
        for(SlotLoco loco : results.slotLocos.values()) {
            for (SlotLoco.Track locoTrack: loco.route) {
                if(locoTrack.equalFilelds(track))
                    return loco;
            }
        }
        return null;
    }

    SlotTrain getTrainByTrack(SlotLoco.Track track) {
        for(SlotTrain train : results.slotTrains.values()) {
            for (SlotTrain.Track trainTrack: train.route) {
                if(trainTrack.equalFilelds(track))
                    return train;
            }
        }
        return null;
    }


    /**
     * Средний простой поездов на станциях смены локомотивных бригад
     * 1.	Для каждого поезда составить список бригад, которые следуют с этим поездом (в состоянии state = 1 – везут локомотив, а не следуют
     * пассажирами).
     * 2.	Для каждой станции, на которой происходит смена бригады (то есть станция является конечной для одной бригады и начальной для другой)
     * вычислить время стоянки поезда на этой станции (разность времени отправления с этой станции и времени прибытия на эту станцию).
     * 3.	Вычислить сумму всех таких времен простоя, разделить на количество станций на всем маршруте поезда, на которых происходила смена
     * бригады.
     * 4.	Вычислить среднее значение простоя для всех запланированных поездов.
     *
     * @return
     */

    public AnalyzeCriterion getDataForRow17(int time) {
        long sumTime = 0L;
        int counter = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;


        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(17L);

        try {

            for (SlotTrain train : results.slotTrains.values()) {
                BaseTrack priorTrack = null;
                for (BaseTrack track : train.route) {
                    if (priorTrack != null) {
                        if (!priorTrack.timeEnd.equals(track.timeStart)) {
                            counter++;
                            if (track.timeStart <= endTime) {
                                sumTime += track.timeStart - priorTrack.timeEnd;
                            } else {
                                sumTime += endTime - priorTrack.timeEnd;
                                break;
                            }
                        }
                    }
                    priorTrack = track;
                }
            }

        } catch (Exception e) {
            LOGGER().error("getDataForRow17 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }
        double rv = 0;
        if (counter == 0) {
            rv = 0.0;
        } else {
            rv = (double) sumTime/*allTime*/ / (double) counter / 3600.0;
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Средний простой локомотивов на станциях без работы
     * 1.	Для каждого локомотива найти станции на маршруте, на которых происходила смена режима работы – менялось либо состояние локомотива,
     * либо поезд, с которым следует локомотив.
     * 2.	Для каждой такой станции вычислить время простоя локомотива (разность между временем отправления с этой станции и временем прибытия
     * на эту станцию).
     * 3.	Вычислить сумму всех таких времен, разделить на количество таких станций на маршруте локомотива.
     * 4.	Вычислить среднее значение простоя для всех запланированных локомотивов.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow18(int time) {
        int counter = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;


        double rv = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(18L);
        long allTime = 0L;

        for (SlotLoco loco : results.slotLocos.values()) {
            try {
                long locoMaxTime = 0L;
                SlotLoco.Track priorTrack = null;
                Long priorState = null;
                Long priorTrainId = null;
                long stopTime = 0L;
                boolean sameStation = false;
                for (SlotLoco.Track track : loco.route) {
                    if (priorTrack != null
                            && priorTrack.timeEnd <= endTime) {
                        if (priorState != track.state || !priorTrainId.equals(track.trainId)) {
                            if(track.stationFromId.equals(track.stationToId)) {  // стоит на станции
                                stopTime += Math.min(endTime,track.timeEnd)
                                        - Math.min(endTime,track.timeStart);
                                sameStation = true;
                            } else {
                                sameStation = false;
                            }
                            stopTime += Math.min(endTime,track.timeStart)
                                    - Math.min(endTime, priorTrack.timeEnd);  // учесть разницу времени прибытия и отправления
                            if (locoMaxTime < stopTime) {
                                locoMaxTime = stopTime;
                            }
                            if(!sameStation || endTime < track.timeEnd) {
                                counter++;
                                allTime += stopTime;
                                stopTime = 0L;
                            }
                        }
                    }
                    if(track.timeStart > endTime) break;
                    priorTrack = track;
                    priorState = track.state;
                    priorTrainId = track.trainId;
                }
                String info = "loco(id(" + loco.id + "),time(" + locoMaxTime / 3600.0 + "))";
                analyzeCriterion.addInfoString(info);
                AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                iEntry.locoId = loco.id;
                iEntry.timeDuration = locoMaxTime;
                analyzeCriterion.addInfo(iEntry);

            } catch (Exception e) {
                LOGGER().error("getDataForRow18 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
        }

        if (counter == 0) {
            rv = 0.0;
        } else {
            rv = (double) allTime / (double) counter / 3600.0;
        }

        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Среднее рабочее время локомотивов за сутки
     * 1.	Для каждого запланированного локомотива выделить участки маршрута, лежащие на горизонте в одни сутки от времени начала планирования,
     * на которых локомотив перемещался с поездом не резервом (state = 1).
     * 2.	Для каждого такого участка каждого локомотива посчитать время хода по участку (разность времени прибытия на конечную станцию и времени
     * отправления).
     * 3.	Вычислить сумму времен хода по каждому участку и каждому локомотиву. Выразить сумму в часах, разделить сумму в часах на количество
     * запланированных локомотивов.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow19(int time) {
        double rv = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(19L);
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;

        long allTime = 0L;
        for (SlotLoco loco : results.slotLocos.values()) {
            long locoTime = 0L;
            try {
                for (SlotLoco.Track track : loco.route) {
                    if(track.timeStart >= endTime) break;
                    if (track.state.intValue() == BaseLocoTrack.State.WITH_TRAIN.ordinal()) {
                        locoTime += Math.min(endTime,track.timeEnd) - track.timeStart;
                    }
                }
                allTime += locoTime;
                String info = "loco(id(" + loco.id + "),time(" + locoTime / 3600.0 + "))";
                analyzeCriterion.addInfoString(info);
                AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                iEntry.locoId = loco.id;
                iEntry.timeDuration = locoTime;
                analyzeCriterion.addInfo(iEntry);

            } catch (Exception e) {
                LOGGER().error("getDataForRow19 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
        }

        if (results.slotLocos.size() == 0) {
            rv = 0.0;
        } else {
            rv = (double) allTime / (double) results.slotLocos.size() / 3600.0;
        }

        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Среднее время работы локомотивной бригады
     * 1.	Для каждой запланированной бригады выделить фрагменты работы бригады: фрагменты маршрута, на которых бригада следовала с одним
     * локомотивом в рабочем состоянии (state = 1) (у одной бригады может быть несколько таких рабочих фрагментов).
     * 2.	Для каждого фрагмента посчитать рабочее время бригады: разность между временем прибытия на конечную станцию фрагмента и
     * временем отправления с начальной станции фрагмента.
     * 3.	Посчитать сумму рабочих времен для всех запланированных бригад, разделить сумму на количество запланированных бригад.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow20(int time) {
        double rv = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(20L);
        long allTime = 0L;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;
        int counter = 0;

        for (SlotTeam team : results.slotTeams.values()) {
            long teamTime = 0L;
            try {
                SlotTeam.Track startFragment = null;
                SlotTeam.Track endFragment = null;
                Iterator<SlotTeam.Track> iterator = team.route.iterator();
                boolean useTeam = false;
                while (iterator.hasNext()) {
                    SlotTeam.Track track = iterator.next();

                    if(track.state.intValue() == BaseTeamTrack.State.AT_WORK.ordinal()) {
                        if (startFragment == null) {
                            if(track.timeStart < endTime) { // если отрезок начинается внутри проверочного интервала
                                startFragment = track;
                            } else
                                break;      //  завершить работу с этой бригадой и перейти к слеюдущей
                        }
                        endFragment = track;        // текущий конец данного интервала работы
                        useTeam = true;             // данная бригада работала
                    }
                    if (startFragment != null &&
                            (track.state.intValue() != BaseTeamTrack.State.AT_WORK.ordinal()
                                    || !track.locoId.equals(endFragment.locoId) || !iterator.hasNext()
                            )
                            ) {                     // данный интервал работы на этом закончился
                        if (endFragment.timeEnd > endTime) {        // если уже вышли за пределы интервала расчета
                            teamTime += (endTime - startFragment.timeStart);
                            break;                  // закончить с данной бригадой и перейти к следующей
                        } else {
                            teamTime += (endFragment.timeEnd - startFragment.timeStart);
                        }
                        startFragment = null;       // сбросить значение начала интервала
                    }
                    /*
                    if (!iterator.hasNext() && startFragment != null
                            && track.state.intValue() == BaseTeamTrack.State.AT_WORK.ordinal()) {
                        useTeam = true;
                        if (endFragment.timeEnd > endTime) {
                            teamTime += (endTime - startFragment.timeStart);
                            break;
                        } else {
                            teamTime += (endFragment.timeEnd - startFragment.timeStart);
                        }
                    }
                    */
                }
                if (useTeam) {
                    counter++;
                    allTime += teamTime;
                    String info = "team(id(" + team.id + "),time(" + teamTime / 3600.0 + "))";
                    analyzeCriterion.addInfoString(info);
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.teamId = team.id;
                    iEntry.timeDuration = teamTime;
                    analyzeCriterion.addInfo(iEntry);
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow20 exception: " + e.getMessage() + " team: " + team.id);
                LoggingAssistant.logException(e);
            }
        }
        if (counter == 0) {
            rv = 0.0;
        } else {
            rv = 1.0 * allTime / counter / 3600.0;
        }

        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }


    public Long getTotalRouteDistance(List<? extends BaseTrack> route) {
        Long distance = 0L;
        for(BaseTrack track: route) {
            distance  += iData.getLinkByStationPair(
                    new StationPair(track.stationFromId, track.stationToId)).getDistance();
        }
        return distance;
    }

    /**
     * Превышение локомотивами норм работ до ТО
     * 1.	Для каждого локомотива посчитать запланированное время работы (разность между временем прибытия на конечную
     *      станцию и временем отправления с первой) и общий запланированный пробег (сумма длин всех участков на маршруте локомотива).
     * 2.	Сравнить вычисленные времена и пробеги с оставшимся до ТО-2 временем/пробегом каждого локомотива.
     * 3.	Посчитать количество локомотивов, для которых превышается время или пробег до ТО-2.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow21() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(21L);

        long timeWorking = 0L;

        for (SlotLoco loco : results.slotLocos.values()) {
            timeWorking = loco.route.get(loco.route.size() - 1).timeEnd - loco.route.get(0).timeStart;
            try {
                long timeToService = fLocos.get(loco.id).getTimeOfServiceFact();
                if (timeToService < timeWorking) {
                    counter++;
                    String info = "loco(id(" + loco.id +
                            "),timeWorking(" + timeWorking / 3600.0 +
                            "),timeToService(" + timeToService / 3600.0 +
                            "))";
                    analyzeCriterion.addInfoString(info);
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.locoId = loco.id;
                    iEntry.timeDuration = timeWorking;
                    analyzeCriterion.addInfo(iEntry);

                }
                long distToService = fLocos.get(loco.id).getDistanceToService();
                if (getTotalRouteDistance(loco.route) > distToService) {
                    counter++;
                    String info = "loco(id(" + loco.id +
                            "),distWorking(" + getTotalRouteDistance(loco.route) +
                            "),distToService(" + distToService +
                            "))";
                    analyzeCriterion.addInfoString(info);
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.locoId = loco.id;
                    analyzeCriterion.addInfo(iEntry);
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow21 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
        }

        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    /**
     * Превышение бригадами норм рабочего времени
     * 1.	Для каждой бригады посчитать запланированное время работы (разность между временем прибытия на конечной станции с
     * локомотивом и временем отправления с первой станции с этим локомотивом).
     * 2.	Сравнить вычисленное рабочее время с нормативным на данном УРЛБ.
     * 3.	Посчитать количество бригад, для которых вычисленное рабочее время больше нормативного на данном УРЛБ.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow22() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(22L);

        for (SlotTeam team : results.slotTeams.values()) {
            try {
                long locoId = 0L;
                long startTime = 0L;
                long endTime = 0L;
                long maxTime = 0;
                long allTime1 = 0;
                long allTime2 = 0;
                Set<TeamRegion> teamRegions = new HashSet<>();
                List<SlotTeam.Track> route = team.route;
                int length = route.size();
                if (length > 0) {
                    boolean free = true;
                    for (int i = 0; i < length; i++) {
                        SlotTeam.Track track = route.get(i);
                        allTime2 += track.timeEnd - track.timeStart;
                        if (track.state.intValue() == BaseTeamTrack.State.AT_WORK.ordinal()) {
                            allTime1 += track.timeEnd - track.timeStart;
                            if (free) {
                                startTime = track.timeStart;
                                free = false;
                            }
                            endTime = track.timeEnd;
                            teamRegions.addAll(getTeamWorkRegionByLink(
                                    iData.getLinkByStationPair(track.getStationPair())));       // добавить УОЛБ данного участка планирования
                        }
                        if ((!free &&
                                track.state.intValue() != BaseTeamTrack.State.AT_WORK.ordinal()
                                || i == length - 1)) {
                            free = true;
                            if (startTime != endTime) {
                                long workTime = endTime - startTime;
                                if(maxTime < workTime)
                                    maxTime = workTime;
                            }
                        }
                    }
                    long normaTime = Long.MAX_VALUE;
                    for (TeamRegion teamRegion : teamRegions) {     // выбирается минимальное разрешенное время
                        if (teamRegion.getWorkTimeWithoutRest() < normaTime) {
                            normaTime = teamRegion.getWorkTimeWithoutRest();
                        }
                    }
                    teamRegions.clear();
                    if(normaTime <= 0)
                        normaTime = 36000;
                    if (maxTime > normaTime) {
                        counter++;
                        String info = "team(id(" + team.id +
                                "),time(" + maxTime / 3600.0 +
                                "),normTime(" + normaTime / 3600.0 +
                                "))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.teamId = team.id;
                        iEntry.timeDuration = maxTime;
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow22 exception: " + e.getMessage() + " team: " + team.id);
                LoggingAssistant.logException(e);
            }
        }

        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    private Set<TeamRegion> getTeamWorkRegionByLink(Link workLink) {
        Set<TeamRegion> teamRegions = new HashSet<>();
        for (TeamRegion teamRegion : iData.getTeamWorkRegions().values()) {
            for (StationPair sp : teamRegion.getStationPairs()) {
                Link link = iData.getLinkByStationPair(sp);
                if (link.equals(workLink)) {
                    teamRegions.add(teamRegion);
                }
            }
        }

        return teamRegions;
    }

    /**
     * Выезд локомотивов за пределы своих тяговых плеч
     * 1.	Для каждого участка на маршруте локомотива, который вернул планировщик, проверить, входит ли этот участок в хотя бы одно из тяговых
     * плеч, заданных для данного локомотива.
     * 2.	Посчитать количество локомотивов, для которых это правило нарушается.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow23() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(23L);

        for (SlotLoco loco : results.slotLocos.values()) {
            try {
                for (SlotLoco.Track track : loco.route) {
                    boolean check = false;
                    for (LocoRegion region : iData.getStationById(track.stationToId).getRegions()) {
                        try {
                            if (fLocos.get(loco.id).getLocoRegions().contains(region)) {
                                check = true;
                                break;
                            }
                        } catch (Exception e) {
                            LOGGER().error(e.getMessage());
                            LoggingAssistant.logException(e);
                        }
                    }
                    if (!check) {
                        counter++;
                        analyzeCriterion.addInfoString(loco.id);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.locoId = loco.id;
                        analyzeCriterion.addInfo(iEntry);
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow23 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
        }
        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    /**
     * Выезд бригад за участки обкатки
     * 1.	Для каждого участка на маршруте бригады, который вернул планировщик, проверить, входит ли этот участок в хотя бы
     * один из обкаточных УРЛБ, заданных для данной бригады.
     * 2.	Посчитать количество бригад, для которых это правило нарушается.
     *
     * @return
     */

    public AnalyzeCriterion getDataForRow24() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(24L);

        for (SlotTeam team : results.slotTeams.values()) {
            try {
                for (SlotTeam.Track track : team.route) {
                    boolean check = false;
                    if (track.state.intValue() != BaseTeamTrack.State.AT_WORK.ordinal())
                        break;
                    outer:
                    for (TeamRegion region : fTeams.get(team.id).getTeamWorkRegions()) {
                        for (StationPair sp : region.getStationPairs()) {
                            if (track.getStationPair().equals(sp)) {
                                check = true;
                                break outer;
                            }
                        }
                    }
                    if (!check) {
                        counter++;
                        String info = "team(id(" + team.id +
                                "),link(from(" + track.stationFromId +
                                "),to(" +  track.stationToId +
                                ")))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.locoId = team.id;
                        iEntry.stationFromId = track.stationFromId;
                        iEntry.stationToId = track.stationToId;
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow24 exception: " + e.getMessage() + " team: " + team.id);
                LoggingAssistant.logException(e);
            }
        }

        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    /**
     * Смена локомотивов на граничных станциях тяговых плеч
     * 1.	Для всех поездов, для которых запланировано несколько локомотивов (есть несколько сообщений slot_loco, в которых локомотивы везут
     * данный поезд), определить станции смены локомотивов (станция смены – это конечная станция маршрута одного локомотива с
     * этим поездом и начальная станция маршрута другого локомотива с этим поездом).
     * 2.	Проверить, входит ли эта станция в список станций смены локомотивов (см. ).
     * 3.	Посчитать количество поездов, для которых условие 2 не выполняется хотя бы для одной станции смены локомотивов данного поезда.
     *
     * @return
     */

    public AnalyzeCriterion getDataForRow25() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(25L);

        for (SlotLoco loco : results.slotLocos.values()) {
            try {
                Long checkedTrainId = loco.route.get(0).trainId;
                Long prevStationId = loco.route.get(0).stationFromId;
                for (SlotLoco.Track track : loco.route) {
                    if (!track.trainId.equals(checkedTrainId)) {
                        //проверяем, является ли это станция станцией смены локо (precessTime > 0) или конечной станцией
                        if (!(iData.getStationById(prevStationId).getProcessTime() > 0
                                || prevStationId.equals(loco.route.get(loco.route.size() - 1).stationToId))) {  // по идее это условие никогда не выполнится
                            counter++;
                            String info = "train(id(" + checkedTrainId
                                    + "),old_loco(" + loco.id
                                    + "),station(" + prevStationId
                                    + "))";
                            analyzeCriterion.addInfoString(info);
                            AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                            iEntry.locoId = loco.id;
                            iEntry.stationAtId = prevStationId;
                            analyzeCriterion.addInfo(iEntry);
                        }
                        checkedTrainId = track.trainId;
                    }
                    prevStationId = track.stationToId;
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow25 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
        }

        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    /**
     * Целостность тяговых плеч
     * 1.	Посчитать количество участков планирования, для которых у граничных станций нет ни одного общего тягового плеча локомотивов.
     *
     * @return
     */

    // Я написал вариант короче - просто слежу, чтобы участки принадлежали УОЛ, иначе они и есть косячные - Такмаз
    public AnalyzeCriterion getDataForRow26() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(26L);

        // Проверяем все links на принадлежность какому-нибудь региону
        for (Link link : iData.getLinks().values()) {
            ArrayList<LocoRegion> linkEndsIntersection = new ArrayList<>(link.getTo().getRegions());
            linkEndsIntersection.retainAll(link.getFrom().getRegions());
            if (linkEndsIntersection.isEmpty()) {
                counter++;
                /*String info = "link(from(" + link.getFrom().getName()
                        + "),to(" + link.getTo().getName()
                        + "))";
                analyzeCriterion.addInfoString(info); */
                AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                iEntry.stationFromId= link.getFrom().getId();
                iEntry.stationToId =  link.getTo().getId();
                analyzeCriterion.addInfo(iEntry);
            }
        }

        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }


    /*
    // Старый Костин вариант
    public AnalyzeCriterion getDataForRow26_1() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(26L);


        for (Link link : iData.getLinks().values()) {
            try {
                List<Link> inputLinks = new ArrayList<>();
                List<Link> outputLinks = new ArrayList<>();
                for (Link checkedLink : iData.getLinks().values()) {
                    if (link.getFrom().equals(checkedLink.getTo())) {
                        inputLinks.add(checkedLink);
                    }
                    if (link.getTo().equals(checkedLink.getFrom())) {
                        outputLinks.add(checkedLink);
                    }
                }

                for (Link checkedLink : inputLinks) {
                    boolean check = false;
                    for (LocoRegion region : checkedLink.getTo().getRegions()) {
                        if (link.getFrom().getRegions().contains(region)) {
                            check = true;
                            break;
                        }
                    }
                    if (!check) {
                        counter++;
                    }
                }

                for (Link checkedLink : outputLinks) {
                    boolean check = false;
                    for (LocoRegion region : checkedLink.getFrom().getRegions()) {
                        if (link.getTo().getRegions().contains(region)) {
                            check = true;
                            break;
                        }
                    }
                    if (!check) {
                        counter++;
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow26 exception: " + e.getMessage() + " link: " + link);
                LoggingAssistant.logException(e);
            }
        }

        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    */

    /**
     * Количество участков планирования без участков обкатки бригад
     * 1. Посчитать количество участков планирования, для которых у граничных станций нет ни одного общего участка работы локомотивных бригад.
     *
     * @return
     */
    // Я написал вариант короче - просто слежу, чтобы участки принадлежали УОЛБ, иначе они и есть косячные - Такмаз
    public AnalyzeCriterion getDataForRow27() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(27L);

        HashSet<Link> coveredLinks = new HashSet<>();

        try {
            // Сначала формируем список принадлжащих  какому-нибуьдь региону линков
            for (TeamRegion teamRegion : iData.getTeamWorkRegions().values()) {
                for (StationPair sp : teamRegion.getStationPairs()) {
                    coveredLinks.add(iData.getLinkByStationPair(sp));
                }
            }

            // Потом проверяем все links на принадлежность какому-нибудь региону
            for (Link link : iData.getLinks().values()) {
                if (!coveredLinks.contains(link)) {
                    counter++;
                    /*String info = "link(from(" + link.getFrom().getName()
                            + "),to(" + link.getTo().getName()
                            + "))";
                    analyzeCriterion.addInfoString(info);*/
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.stationFromId= link.getFrom().getId();
                    iEntry.stationToId =  link.getTo().getId();
                    analyzeCriterion.addInfo(iEntry);
                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow27 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }


    // Старый Костин вариант
    public AnalyzeCriterion getDataForRow27_1() {
        int counter = 0;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(27L);


        Map<Station, Set<TeamRegion>> teamWorkRegionsOnStation = new HashMap<>();

        try {

            for (TeamRegion teamRegion : iData.getTeamWorkRegions().values()) {
                for (StationPair sp : teamRegion.getStationPairs()) {
                    Link link = iData.getLinkByStationPair(sp);
                    if (!teamWorkRegionsOnStation.containsKey(link.getFrom()))
                        teamWorkRegionsOnStation.put(link.getFrom(), new HashSet<TeamRegion>());
                    if (!teamWorkRegionsOnStation.containsKey(link.getTo()))
                        teamWorkRegionsOnStation.put(link.getTo(), new HashSet<TeamRegion>());
                }
                for (StationPair sp : teamRegion.getStationPairs()) {
                    Link link = iData.getLinkByStationPair(sp);
                    Set<TeamRegion> teamRegionsFrom = teamWorkRegionsOnStation.get(link.getFrom());
                    teamRegionsFrom.add(teamRegion);
                    teamWorkRegionsOnStation.put(link.getFrom(), teamRegionsFrom);

                    Set<TeamRegion> teamRegionsTo = teamWorkRegionsOnStation.get(link.getTo());
                    teamRegionsTo.add(teamRegion);
                    teamWorkRegionsOnStation.put(link.getTo(), teamRegionsTo);
                }
            }

            for (Link link : iData.getLinks().values()) {
                List<Link> inputLinks = new ArrayList<>();
                List<Link> outputLinks = new ArrayList<>();
                for (Link checkedLink : iData.getLinks().values()) {
                    if (link.getFrom().equals(checkedLink.getTo())) {
                        inputLinks.add(checkedLink);
                    }
                    if (link.getTo().equals(checkedLink.getFrom())) {
                        outputLinks.add(checkedLink);
                    }
                }

                for (Link checkedLink : inputLinks) {
                    boolean check = false;
                    Set<TeamRegion> inputRegions = teamWorkRegionsOnStation.get(checkedLink.getTo());
                    Set<TeamRegion> linkRegions = teamWorkRegionsOnStation.get(link.getFrom());

                    for (TeamRegion checkedRegion : inputRegions) {
                        if (linkRegions != null && linkRegions.contains(checkedRegion)) {
                            check = true;
                            break;
                        }
                    }
                    if (!check) {
                        counter++;
                    }
                }

                for (Link checkedLink : outputLinks) {
                    boolean check = false;
                    Set<TeamRegion> outputRegions = teamWorkRegionsOnStation.get(checkedLink.getFrom());
                    Set<TeamRegion> linkRegions = teamWorkRegionsOnStation.get(link.getTo());

                    for (TeamRegion checkedRegion : outputRegions) {
                        if (linkRegions != null && linkRegions.contains(checkedRegion)) {
                            check = true;
                            break;
                        }
                    }

                    if (!check) {
                        counter++;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow27 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    /**
     * Связность поездов и локомотивов
     * 1.	Посчитать количество поездов, которые на начало планирования находятся на перегоне (факт train_depart) и для которых нет связанного с
     * этим поездом локомотива, который бы тоже нахо
     * 2.	дился на этом перегоне. Посчитать процент таких поездов относительно общего количества поездов, находящихся на перегонах.
     *
     * @return
     */

    public AnalyzeCriterion getDataForRow28() {
        int allQuantity = 0;
        int counter = 0;
        Double rv;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(28L);

        for (FactTrain trainFact : fTrains.values()) {
            try {
                if (trainFact.getTrainState() instanceof TrainDepart) {
                    allQuantity++;
                    boolean check = false;
                    for (FactLoco factLoco : fLocos.values()) {
                        if (factLoco.getTrack() != null
                                && factLoco.getTrack().getRealTrainId().equals(trainFact.getId())) {
                            check = true;
                            break;
                        }
                    }

                    if (!check) {
                        counter++;
                        String info = "train(id(" + trainFact.getId().toString()
                                + "),track(" + ((TrainDepart) trainFact.getTrainState()).getLink().getFrom().getName()
                                + "," + ((TrainDepart) trainFact.getTrainState()).getLink().getTo().getName()
                                + "),time(" + trainFact.getTrainState().getTime().toString()
                                + "))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.trainId = trainFact.getId();
                        iEntry.stationFromId = ((TrainDepart) trainFact.getTrainState()).getLink().getFrom().getId();
                        iEntry.stationToId =  ((TrainDepart) trainFact.getTrainState()).getLink().getTo().getId();
                        iEntry.timeStart = trainFact.getTrainState().getTime();
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow28 exception: " + e.getMessage() + " trainFact: " + trainFact.getId());
                LoggingAssistant.logException(e);
            }
        }
        if (allQuantity == 0) {
            rv = 0.0;
        } else {
            rv = (100 * counter) / (double) allQuantity;
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /*  Отсутствие связности локомотивов с поездами
     1.	Посчитать количество локомотивов, которые везут поезд на перегоне, но соответствующий поезд (по переданным фактам) не находится на
     этом перегоне. Посчитать процент таких локомотивов относительно общего количества локомотивов, находящихся на перегонах.
     */
    public AnalyzeCriterion getDataForRow29() {
        int allQuantity = 0;
        int counter = 0;
        Double rv;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(29L);

        for (FactLoco factLoco : fLocos.values()) {
            try {
                if (factLoco.getTrack() != null && factLoco.getTrack().getState() == BaseLocoTrack.State.WITH_TRAIN) {
                    allQuantity++;
                    boolean check = false;
                    for (FactTrain trainFact : fTrains.values()) {
                        if (trainFact.getTrainState() instanceof TrainDepart && ((TrainDepart) trainFact.getTrainState()).getLink().equals(factLoco.getTrack().getLink())) {
                            check = true;
                            break;
                        }
                    }

                    if (!check) {
                        counter++;
                        // below added by ATakm
                        String info = "loco(id(" + factLoco.getId().toString()
                                + "),track(" + factLoco.getTrack().getLink().getFrom().getName()
                                + "," + factLoco.getTrack().getLink().getTo().getName()
                                + "),time(" + factLoco.getTrack().getTimeDepart().toString()
                                + "))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.locoId = factLoco.getId();
                        iEntry.stationFromId = factLoco.getTrack().getLink().getFrom().getId();
                        iEntry.stationToId = factLoco.getTrack().getLink().getTo().getId();
                        iEntry.timeStart = factLoco.getTrack().getTimeDepart();
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow29 exception: " + e.getMessage() + " factLoco: " + factLoco.getId());
                LoggingAssistant.logException(e);
            }
        }
        if (allQuantity == 0) {
            rv = 0.0;
        } else {
            rv = (100 * counter) / (double) allQuantity;
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Связность локомотивов и бригад (связанность локомотивов с бригадами
     * 1.	Посчитать количество локомотивов, которые на начало планирования находятся на перегоне и для которых нет связанной с этим
     * локомотивом бригады, которая тоже находилась бы на этом перегоне. Посчитать процент таких локомотивов относительно общего количества
     * локомотивов, находящихся на перегонах.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow30() {
        int allQuantity = 0;
        int counter = 0;
        Double rv;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(30L);

        for (FactLoco loco : fLocos.values()) {
            try {
                if (loco.getTrack() != null) {
                    allQuantity++;
                    boolean check = false;
                    for (FactTeam team : fTeams.values()) {
                        if (team.getTrack() != null && loco.getTrack().getLink().equals(team.getTrack().getLink())) {
                            check = true;
                            break;
                        }
                    }
                    if (!check) {
                        counter++;
                        // below added by ATakm
                        String info = "loco(id(" + loco.getId().toString()
                                + "),track(" + loco.getTrack().getLink().getFrom().getName()
                                + "," + loco.getTrack().getLink().getTo().getName()
                                + "),time(" + loco.getTrack().getTimeDepart().toString()
                                + "))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.locoId = loco.getId();
                        iEntry.stationFromId = loco.getTrack().getLink().getFrom().getId();
                        iEntry.stationToId = loco.getTrack().getLink().getTo().getId();
                        iEntry.timeStart = loco.getTrack().getTimeDepart();
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow30 exception: " + e.getMessage() + " FactLococo: " + loco.getId());
                LoggingAssistant.logException(e);
            }
        }

        if (allQuantity == 0) {
            rv = 0.0;
        } else {
            rv = (100 * counter) / (double) allQuantity;
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Связность локомотивов и бригад (связанность бригадов с локомотивами
     * 1.	Посчитать количество бригад, которые едут с локомотивом, но соответствующий локомотив не находится на этом перегоне.
     * Посчитать процент таких локомотивов относительно общего количества локомотивов, находящихся на перегонах.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow31() {

        int allQuantity = 0;
        int counter = 0;
        Double rv;
        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(31L);

        for (FactTeam team : fTeams.values()) {
            try {
                if (team.getTrack() != null) {
                    allQuantity++;
                    boolean check = false;
                    for (FactLoco loco : fLocos.values()) {
                        if (loco.getTrack() != null && team.getTrack().getLink().equals(loco.getTrack().getLink())) {
                            check = true;
                            break;
                        }
                    }

                    if (!check) {
                        counter++;
                        String info = "team(id(" + team.getId().toString()
                                + "),track(" + team.getTrack().getLink().getFrom().getName()
                                + "," + team.getTrack().getLink().getTo().getName()
                                + "),time(" + team.getTrack().getDepartTime().toString()
                                + "))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.teamId = team.getId();
                        iEntry.stationFromId = team.getTrack().getLink().getFrom().getId();
                        iEntry.stationToId = team.getTrack().getLink().getTo().getId();
                        iEntry.timeStart = team.getTrack().getDepartTime();
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow31 exception: " + e.getMessage() + " team: " + team.getId());
                LoggingAssistant.logException(e);
            }
        }

        if (allQuantity == 0) {
            rv = 0.0;
        } else {
            rv = (100 * counter) / (double) allQuantity;
        }
        analyzeCriterion.setValue(rv);
        return analyzeCriterion;
    }

    /**
     * Количество локомотивов без местоположения
     * 1.	Посчитать количество локомотивов, по которым в планировщик была передана общая информация, но по которым нет сообщения о
     * местоположении. Посчитать процент таких локомотивов от общего количества переданных в планировщик локомотивов.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow32() {
        int count = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(32L);

        try {
            for (FactLoco fLoco : fLocos.values()) {
                if (fLoco.getStation() == null && fLoco.getTrack() == null) {
                    count++;
                    analyzeCriterion.addInfoString(fLoco.getId());
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.locoId = fLoco.getId();
                    analyzeCriterion.addInfo(iEntry);
                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow33 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        analyzeCriterion.setValue(100.0 * count / fLocos.size());
        return analyzeCriterion;
    }

    /**
     * Количество локомотивов без оставшегося времени работы
     * 1.	Посчитать количество локомотивов, по которым в планировщик была передана общая информация, но по которым нет сообщения об
     * оставшемся времени работы. Посчитать процент таких локомотивов от общего количества переданных в планировщик локомотивов.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow33() {
        int count = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(33L);

        try {
            for (FactLoco fLoco : fLocos.values()) {
                if (fLoco.getTimeToService() < 0) {
                    count++;
                    analyzeCriterion.addInfoString(fLoco.getId());
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.locoId = fLoco.getId();
                    analyzeCriterion.addInfo(iEntry);
                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow33 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        analyzeCriterion.setValue(100.0 * count / fLocos.size());
        return analyzeCriterion;
    }

    /**
     * Количество бригад без местоположения
     * 1.	Посчитать количество бригад, по которым в планировщик была передана общая информация, но по которым нет сообщения о
     * местоположении. Посчитать процент таких бригад от общего количества переданных в планировщик бригад.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow34() {
        int count = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(34L);

        try {
            for (FactTeam fTeam : fTeams.values()) {
                if (fTeam.getStation() == null && fTeam.getTrack() == null) {
                    count++;
                    analyzeCriterion.addInfoString(fTeam.getId());
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.teamId = fTeam.getId();
                    analyzeCriterion.addInfo(iEntry);
                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow33 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        analyzeCriterion.setValue(100.0 * count / fTeams.size());
        return analyzeCriterion;
    }


    /**
     * Количество бригад без оставшегося рабочего времени
     * 1.	Посчитать количество бригад, по которым в планировщик была передана общая информация, но по которым нет сообщения об
     * оставшемся времени работы. Посчитать процент таких бригад от общего количества переданных в планировщик бригад.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow35() {
        int count = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(35L);

        try {
            for (FactTeam fTeam : fTeams.values()) {
                if (fTeam.getTimeUntilRest() < 0) {
                    count++;
                    analyzeCriterion.addInfoString(fTeam.getId());
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.teamId = fTeam.getId();
                    analyzeCriterion.addInfo(iEntry);
                }
            }
        } catch (Exception e) {
            LOGGER().error("getDataForRow35 exception: " + e.getMessage());
            LoggingAssistant.logException(e);
        }

        analyzeCriterion.setValue(100.0 * count / fTeams.size());
        return analyzeCriterion;
    }

    /**
     * Привязка бригады без нарушения правил по временным нормам работы
     * 1.	Найти последнюю операцию «Начало отдыха по месту жительства» или «Начало выходного дня» для каждой бригады.
     * Посчитать  количество бригад, у которых время между началом отдыха и стартом работы после этого отдыха (запланированное время отправления
     * на первый участок маршрута) меньше 16 часов.
     * 2.	Найти последнюю операцию «Начало отдыха в пункте оборота». Посчитать количество бригад, у которых время между началом отдыха и
     * стартом работы после этого отдыха (запланированное время отправления) меньше 3 часов.
     *
     * @return Доп.инфо:
     * <p/>
     * Список ID и  номеров бригад, для которых есть нарушения правил по временным нормам работы.
     */
    public AnalyzeCriterion getDataForRow36() {
        int count = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(36L);

        for (SlotTeam rTeam : results.slotTeams.values()) {
            for (SlotTeam.Track rTrack : rTeam.route) {
                if (rTrack.state.intValue() == BaseTeamTrack.State.REST.ordinal() &&        // Найти последнюю операцию «Начало отдыха в пункте оборота».
                        rTrack.timeEnd - rTrack.timeStart < 3 * 60 * 60) {    // Посчитать количество бригад, у которых время между началом отдыха и стартом работы после этого отдыха (запланированное время отправления) меньше 3 часов.
                    count++;
                    analyzeCriterion.addInfoString(rTeam.id);
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.teamId = rTeam.id;
                    analyzeCriterion.addInfo(iEntry);
                    break;
                }
            }
        }

        analyzeCriterion.setValue(1.0 * count);
        return analyzeCriterion;
    }

    /**
     * Наличие «старых» поездов
     * 1.	Посчитать количество поездов, находящихся в состоянии «Отправлен на перегон», у которых время последней операции меньше текущего
     * времени более чем на величину 2T, где Т – это нормативное время  хода по участку планирования, на который отправлен поезд.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow37() {
        /*
        1.	Посчитать количество поездов, находящихся в состоянии «Отправлен на перегон», у которых время последней
            операции меньше текущего времени  более чем на величину 2T, где Т – это нормативное время  хода по участку
            планирования, на который отправлен поезд.
         */
        int counter = 0;

        Long curTime = iData.getCurrentTime();

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(37L);

        for (FactTrain trainFact : fTrains.values()) {
            try {
                TrainState trainState = trainFact.getTrainState();
                if (trainState instanceof TrainDepart) {
                    List<Link> route = trainFact.getMainRoute().getLinkList();
                    if ((route.size() != 0) &&
                            (curTime - trainState.getTime() > 2 * route.get(0).getDuration(curTime))) {
                        counter++;
                        analyzeCriterion.addInfoString(trainFact.getId());
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.trainId = trainFact.getId();
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow37 exception: " + e.getMessage() + " trainFact: " + trainFact.getId());
            }
        }
        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    /**
     * Количество бригад без подвязки
     * 1.	Посчитать количество бригад, которые вышли по заставке в депо приписки (станция местоположения на начало планирования совпадает с
     * депо приписки), но в результате работы планировщика не были привязаны ни к одному поезду.
     *
     * @return Доп инфо
     * <p/>
     * Список ID, табельных номеров и фамилий машиниста бригад, которые остались без подвязки.
     * Для каждой бригады также указать депо приписки и предполагавшееся время явки.
     */
    public AnalyzeCriterion getDataForRow38(int time) {
        //int totHomeTeams = 0;
        /*Посчитать количество бригад, которые вышли по заставке в депо приписки (станция местоположения на начало планирования
        совпадает с депо приписки), но в результате работы планировщика не были привязаны ни к одному поезду.*/
        int totLooseHomeTeams = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(38L);

        for (FactTeam team : fTeams.values()) {
            try {
                if (team.getDepot().equals(team.getStation())
                        && team.getTimeOfFact() <= endTime) {
                    boolean slotted = false;
                    for (SlotTeam SlotTeam : results.slotTeams.values()) {
                        if (SlotTeam.id.equals(team.getId())) {
                            slotted = true;
                            break;
                        }
                    }
                    if (!slotted) {
                        totLooseHomeTeams++;
                        String info = "team_left(id(" + team.getId()
                                + "),station(" + team.getStation().getName()
                                + "),time(" + team.getTimeOfFact()
                                + "))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.teamId =  team.getId();
                        iEntry.stationAtId = team.getStation().getId();
                        iEntry.factTime = team.getTimeOfFact();
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow38 exception: " + e.getMessage() + " team: " + team.getId());
            }
        }

        analyzeCriterion.setValue(1.0 * totLooseHomeTeams);
        return analyzeCriterion;
    }

    /**
     * Количество локомотивов без подвязки в состоянии "ожидание работы"
     * 1.	Посчитать количество локомотивов, которые на момент начала планирования находились на станциях и были доступны для планирования,
     * но в результате работы планировщика не были подвязаны ни к одному поезду.
     *
     * @return Доп. инфо:
     * <p/>
     * Список Id и номеров локомотивов.
     * Для каждого локомотива также указать
     * серию, тяговое плечо и станцию местонахождения.
     */
    public AnalyzeCriterion getDataForRow39() {
        int totLooseLocos = 0;
        Collection<SlotLoco> rlocos = results.slotLocos.values();

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(39L);

        for (FactLoco loco : fLocos.values()) {
            try {
                boolean slotted = false;
                if (loco.getStation() != null) {
                    for (SlotLoco SlotLoco : rlocos) {
                        if (SlotLoco.id.equals(loco.getId())) {
                            slotted = true;
                            break;
                        }
                    }
                    if (!slotted) {
                        totLooseLocos++;
                        String info = "loco_lost(id(" + loco.getId().toString()
                                + "),station(" + loco.getStation().getName() + "))";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.locoId =  loco.getId();
                        iEntry.stationAtId = loco.getStation().getId();
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
                ;
            } catch (Exception e) {
                LOGGER().error("getDataForRow39 exception: " + e.getMessage() + " loco: " + loco.getId());
            }
        }

        analyzeCriterion.setValue(1.0 * totLooseLocos);
        return analyzeCriterion;
    }

    /**
     * Количество локомотивов, отправленных резервом (лучше "пересылок локомотивов резервом" - А.Такм.)
     * 1. Посчитать количество пересылок локомотивов резервом. Если локомотив проходит участок планирования в состоянии
     * «резервом», а следующий участок – также в состоянии «резервом», то считать это одной пересылкой резервом.
     * Таким образом, одна пересылка резервом заключена между следованиями локомотива с поездом,
     * прохождением ремонта или же границе (началом или концом) маршрута локомотива.
     *
     * @return Доп. инфо:
     * Список ID и номеров локомотивов, пересылающихся резервом.
     * Для каждого локомотива также указать
     * //серию,
     * //тяговое плечо,
     * станцию отправления резервом,
     * время отправления резервом,
     * станцию прибытия резервом,
     * время прибытия резервом.
     */
    public AnalyzeCriterion getDataForRow40() {
        int toReserveLocos = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(40L);

        for (SlotLoco loco : results.slotLocos.values()) {
            try {
                boolean reserved = false;
                String info = new String();
                AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                SlotLoco.Track last_reserve_track = null;
                for (SlotLoco.Track track : loco.route) {
                    if (track.state.intValue() == BaseLocoTrack.State.RESERVE.ordinal()) {
                        if (!reserved) {
                            reserved = true;
                            toReserveLocos++;
                            info += "loco_reserve(id(" + loco.id
                                    + "),from(station(" + track.stationFromId                  // станцию отправления резервом,
                                    + "),time(" + track.timeStart.toString();                              //  время отправления резервом,
                            iEntry.locoId =  loco.id;
                            iEntry.stationFromId = track.stationFromId;
                            iEntry.timeStart = track.timeStart;
                        }
                        last_reserve_track = track;
                    } else {
                        if (reserved) {
                            reserved = false;
                            if (last_reserve_track != null) {
                                info += "),to(station(" + last_reserve_track.stationToId    +      // станцию прибытия резервом,
                                        "),time(" + last_reserve_track.timeEnd.toString() + ")))";       // время прибытия резервом.
                                analyzeCriterion.addInfoString(info);
                                iEntry.stationToId = last_reserve_track.stationToId;
                                iEntry.timeEnd = last_reserve_track.timeEnd;
                                analyzeCriterion.addInfo(iEntry);
                            }
                            info = new String();
                            last_reserve_track = null;
                        }
                    }
                }
                if (last_reserve_track != null) {
                    info += "),to(station(" + last_reserve_track.stationToId  +      // станцию прибытия резервом,
                            "),time(" + last_reserve_track.timeEnd.toString() + ")))";       // время прибытия резервом.
                    analyzeCriterion.addInfoString(info);
                    iEntry.stationToId = last_reserve_track.stationToId;
                    iEntry.timeEnd = last_reserve_track.timeEnd;
                    analyzeCriterion.addInfo(iEntry);
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow40 exception: " + e.getMessage() + " loco: " + loco.id);
            }
        }

        analyzeCriterion.setValue(1.0 * toReserveLocos);
        return analyzeCriterion;
    }

    /**
     * Количество локомотивных бригад, отправленных пассажирами
     * 1. Посчитать количество пересылок бригад пассажирами. Если бригада проходит участок планирования в состоянии «пассажиром»,
     * а следующий участок – также в состоянии «пассажиром», что считать это одной пересылкой пассажиром. Таким образом,
     * одна пересылка пассажиром заключена между следованиями бригады с поездом, отдыхом или же границей
     * (началом или концом) маршрута бригады.
     *
     * @return Доп. инфо:
     * Список ID, табельных номеров и фамилий машиниста для бригады, следующей пассажиром.
     * Для каждой пересылки также указать способ пересылки (пассажирским или грузовым поездом),
     * станцию отправления пассажиром,
     * время отправления пассажиром,
     * станцию прибытия пассажиром,
     * время прибытия пассажиром.
     */
    public AnalyzeCriterion getDataForRow41() {
        int totPassTeams = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(41L);


        for (SlotTeam team : results.slotTeams.values()) {
            try {
                boolean pass = false;
                String info = new String();
                AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                SlotTeam.Track last_pass_track = null;
                for (SlotTeam.Track track : team.route) {
                    if (track.state.intValue() == BaseTeamTrack.State.PASSENGER.ordinal()) {
                        if (!pass) {
                            pass = true;
                            totPassTeams++;
                            info += "team_as_pass(id(" + team.id
                                    + "),pass_train(" + (track.locoId == -1)                        // способ пересылки (пассажирским или грузовым поездом),
                                            //iData.getSlots_pass().containsKey(track. getSlotId())   // способ пересылки (пассажирским или грузовым поездом),
                                    + "),from(station(" + track.stationFromId                 // станцию отправления пассажиром,
                                    + "),time(" + track.timeStart.toString();                              //  время отправления пассажиром,
                            iEntry.locoId =  team.id;
                            iEntry.stationFromId = track.stationFromId;
                            iEntry.timeStart = track.timeStart;
                        }
                        last_pass_track = track;
                    } else {
                        if (pass) {
                            pass = false;
                            if (last_pass_track != null) {
                                info += "),to(station(" + last_pass_track.stationToId +      // станцию прибытия пассажиром,
                                        "),time(" + last_pass_track.timeEnd.toString() + ")))";       // время прибытия пассажиром.
                                analyzeCriterion.addInfoString(info);
                                iEntry.stationToId = last_pass_track.stationToId;
                                iEntry.timeEnd = last_pass_track.timeEnd;
                                analyzeCriterion.addInfo(iEntry);
                            }
                            info = new String();
                            last_pass_track = null;
                        }
                    }
                }
                if (last_pass_track != null) {
                    info += "),to(station(" + last_pass_track.stationToId +      // станцию прибытия пассажиром,
                            "),time(" + last_pass_track.timeEnd.toString() + ")))";       // время прибытия пассажиром.
                    analyzeCriterion.addInfoString(info);
                    iEntry.stationToId = last_pass_track.stationToId;
                    iEntry.timeEnd = last_pass_track.timeEnd;
                    analyzeCriterion.addInfo(iEntry);
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow41 exception: " + e.getMessage() + " team: " + team.id);
            }
        }

        analyzeCriterion.setValue(1.0 * totPassTeams);
        return analyzeCriterion;
    }

    /**
     * Временное расстояние между поездами не меньше 10 минут
     * 1.	Для каждого участка планирования составить список поездов, которые запланированы на этом участке.
     * 2.	Посчитать количество поездов, для которых разность между их временами отправления на участок или между их
     * временами прибытия на конечную станцию участка меньше 10 минут.
     * 3.	При подсчете не учитывать поезда, которые образованы из локомотивов резервом (это поезда с id от 4200 до 4999).
     *
     * @return В дополнительной информации содержатся:
     * Список ID пар поездов, для которых нарушается условие.
     * Для каждой такой пары вывести участок планирования и времена отправления и прибытия для каждого поезда.
     */
    public AnalyzeCriterion getDataForRow42() {
        int counter = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(42L);

        for (Link link : iData.getLinks().values()) {
            HashMap<Long, SlotTrain> trainTimesStart = new HashMap<>();
            ArrayList<Long> timesStart = new ArrayList<>();
            HashMap<Long, SlotTrain> trainTimesEnd = new HashMap<>();
            ArrayList<Long> timesEnd = new ArrayList<>();
            ArrayList<StationPair> stationPairs = new ArrayList<>();
            int length = 0;
            try {
                for (SlotTrain SlotTrain : results.slotTrains.values()) {
                    Long trainId = SlotTrain.id;
                    if (trainId < 4200 && trainId > 4999) {
                        for (SlotTrain.Track track : SlotTrain.route) {
                            if (track.getStationPair().equals(link.getStationPair())) {
                                length++;
                                Long timeStart = track.timeStart;
                                Long timeEnd = track.timeEnd;
                                trainTimesStart.put(timeStart, SlotTrain);
                                timesStart.add(timeStart);
                                trainTimesEnd.put(timeEnd, SlotTrain);
                                timesEnd.add(timeEnd);
                                stationPairs.add(track.getStationPair());
                            }
                        }
                    }
                }
                Collections.sort(timesStart);
                Collections.sort(timesEnd);
                for (int i = 0; i < length - 1; i++) {
                    if (timesStart.get(i + 1) - timesStart.get(i) < 600) {
                        counter++;
                        String info = "start_time_conflict(link(from:" + link.getFrom().getId() + ",to: " + link.getTo().getId()
                                + "), train1: " + trainTimesStart.get(timesStart.get(i)).id
                                + ", start_time1: " + timesStart.get(i)
                                + "; train2: " + trainTimesStart.get(timesStart.get(i + 1)).id
                                + ", start_time2: " + timesStart.get(i + 1) + ")";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.trainId =  trainTimesStart.get(timesStart.get(i)).id;
                        iEntry.stationFromId = stationPairs.get(i).stationFromId;
                        iEntry.timeStart = timesStart.get(i);
                        analyzeCriterion.addInfo(iEntry);
                    } else if (timesEnd.get(i + 1) - timesEnd.get(i) < 600) // меньше 10 минут = 600 секунд, т.к. время в секундах
                    {
                        counter++;
                        String info = "end_time_conflict(link(from:" + link.getFrom().getId() + ",to: " + link.getTo().getId()
                                + "), train1: " + trainTimesEnd.get(timesEnd.get(i)).id
                                + ", end_time1: " + timesEnd.get(i)
                                + "; train2: " + trainTimesEnd.get(timesEnd.get(i + 1)).id
                                + ", end_time2: " + timesEnd.get(i + 1) + ")";
                        analyzeCriterion.addInfoString(info);
                        AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                        iEntry.trainId =  trainTimesStart.get(timesStart.get(i)).id;
                        iEntry.stationToId = stationPairs.get(i).stationToId;
                        iEntry.timeEnd = timesEnd.get(i);
                        analyzeCriterion.addInfo(iEntry);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow42 exception: " + e.getMessage() + " link: " + link.toString());
            }

        }
        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    /**
     * Количество локомотивов резервом, следующих в четном направлении
     * Посчитать количество локомотивов, пересылающихся резервом (можно считать по slot_loco, у которых в элементе
     * маршрута есть state = 0, или по поездам с номерами 4200…4299),
     * у которых для первого участка планирования на маршруте указан direction = 0.
     *
     * @return
     */


    public AnalyzeCriterion getDataForRow43() {
        /*
        Посчитать количество локомотивов, пересылающихся резервом (можно считать по slot_loco, у которых в элементе
        маршрута есть state = 0, или по поездам с номерами 4200…4299),
        у которых для первого участка планирования на маршруте указан direction = 0.
         */

        int evenDirectionReserveLocos = 0;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(43L);

        for (SlotLoco loco : results.slotLocos.values()) {
            try {
                boolean reserved = false;

                for (SlotLoco.Track track : loco.route) {
                    if (track.state.intValue() == BaseLocoTrack.State.RESERVE.ordinal()) {
                        if (!reserved) {
                            reserved = true;
                            if (iData.getLinkByStationPair(track.getStationPair()).isHaulingDirection()) {
                                evenDirectionReserveLocos++;
                                analyzeCriterion.addInfoString(loco.id);
                                AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                                iEntry.locoId =  loco.id;
                                analyzeCriterion.addInfo(iEntry);
                            }
                        }
                    } else {
                        reserved = false;
                    }
                }
            } catch (Exception e) {
                LOGGER().error("setDataForRow43 exception: " + e.getMessage() + " loco: " + loco.id);
            }
        }
        analyzeCriterion.setValue(1.0 * evenDirectionReserveLocos);
        return analyzeCriterion;
    }

    /**
     * Процент поездов, обеспеченных локомотивами на горизонте в 12 часов
     * 1.	Выбрать все поезда, отправление которых запланировано планировщиком от времени начала планирования на горизонте в 24 часа.
     * 2.	Посчитать процент поездов, для которых на каждом участке, на который запланировано отправление поезда на горизонте в 24 часа, планировщиком возвращен прикрепленный локомотив.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow44() {
        AnalyzeCriterion analyzeCriterion = getDataForRow3(43200L);
        analyzeCriterion.setId(44L);
        return analyzeCriterion;
    }

    /**
     * Процент локомотивов, обеспеченных бригадами на горизонте в 12 часов
     * 1.	Выбрать все локомотивы, отправление которых запланировано планировщиком на горизонте в 24 часа от времени начала планирования.
     * 2.	Посчитать процент локомотивов, для которых на каждом участке, на который запланировано отправление локомотива на горизонте в 24 часа, планировщиком возвращена прикрепленная бригада.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow45() {
        AnalyzeCriterion analyzeCriterion = getDataForRow4(43200L);
        analyzeCriterion.setId(45L);
        return analyzeCriterion;

    }

    /**
     * Процент поездов, обеспеченных локомотивами на горизонте в 18 часов
     * 1.	Выбрать все поезда, отправление которых запланировано планировщиком от времени начала планирования на горизонте в 24 часа.
     * 2.	Посчитать процент поездов, для которых на каждом участке, на который запланировано отправление поезда на горизонте в 24 часа, планировщиком возвращен прикрепленный локомотив.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow46() {
        AnalyzeCriterion analyzeCriterion = getDataForRow3(64800L);
        analyzeCriterion.setId(46L);
        return analyzeCriterion;
    }

    /**
     * Процент локомотивов, обеспеченных бригадами на горизонте в 18 часов
     * 1.	Выбрать все локомотивы, отправление которых запланировано планировщиком на горизонте в 24 часа от времени начала планирования.
     * 2.	Посчитать процент локомотивов, для которых на каждом участке, на который запланировано отправление локомотива на горизонте в 24 часа, планировщиком возвращена прикрепленная бригада.
     *
     * @return
     */
    public AnalyzeCriterion getDataForRow47() {
        AnalyzeCriterion analyzeCriterion = getDataForRow4(64800L);
        analyzeCriterion.setId(47L);
        return analyzeCriterion;
    }


    /**
     * Процент локомотивов, обеспеченных бригадами на горизонте в 18 часов
     * 1.	Выбрать все локомотивы, отправление которых запланировано планировщиком на горизонте в 24 часа от времени начала планирования.
     * 2.	Посчитать процент локомотивов, для которых на каждом участке, на который запланировано отправление локомотива на горизонте в 24 часа, планировщиком возвращена прикрепленная бригада.
     *
     * @return
     */

    public AnalyzeCriterion getDataForRow48() {
         /*
       1.	Посчитать количество локомотивов (сообщений slot_loco), которые вернул планировщик в результатах.
         */


        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(48L);
        analyzeCriterion.setValue(1.0 * results.slotLocos.size());
        return analyzeCriterion;
    }

    public AnalyzeCriterion getDataForRow49() {
         /*
       1.	Посчитать количество бригад (сообщений slot_team), которые вернул планировщик в результатах.
         */


        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(49L);
        analyzeCriterion.setValue(1.0 * results.slotTeams.size());
        return analyzeCriterion;
    }

    // Средний вес поезда (т)
    // Производительность локомотивов
    public Pair<AnalyzeCriterion,AnalyzeCriterion> getDataForRow50_51(long time) {
        /*
        ). Суммирование ведется по всем запланированным поездам, кроме поездов, которые соответствуют локомотивам резервом.
         */

        long totWeight =  0;
        long totLength = 0;

        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;



        AnalyzeCriterion analyzeCriterion1 = new AnalyzeCriterion(50L);
        AnalyzeCriterion analyzeCriterion2 = new AnalyzeCriterion(51L);

        List<SlotLoco> locosInvolved = new ArrayList<>();

        for (SlotTrain train : results.slotTrains.values()) {
            try {
                Long Id = train.id;
                if (!( Id >= 4200 &&  Id <= 4999)
                        ) {
                    long trainLength = 0;
                    for (SlotTrain.Track track : train.route) {
                        if(track.timeStart < endTime) {
                            int linkLength = iData.getLinkByStationPair(track.getStationPair()).getDistance();
                            trainLength += linkLength *
                                    (Math.min(endTime,track.timeEnd)  - track.timeStart)
                                    / (track.timeEnd - track.timeStart);
                            for (SlotLoco loco : getLocosByTrain(train,endTime)) {
                                if (!locosInvolved.contains(loco)) {
                                    locosInvolved.add(loco);
                                }
                            }
                        }
                    }
                    totLength += trainLength;
                    FactTrain fTrain = iData.getFactTrains().get(train.id);
                    int weight = 0;
                    if (fTrain != null)
                        weight = fTrain.getWeight().intValue();
                    totWeight += trainLength * weight;
                }

            } catch (Exception e) {
                LOGGER().error("setDataForRow50_51 exception: " + e.getMessage() + " train: " + train.id);
            }
        }
        if(totLength > 0) {
            analyzeCriterion1.setValue(1.0 * totWeight / totLength);
        } else {
            analyzeCriterion1.setValue(0.0);
        }

        int totLocos = locosInvolved.size();

        if(totLocos > 0) {
            analyzeCriterion2.setValue(1.0 * totWeight / totLocos / 1000.0);
        } else {
            analyzeCriterion2.setValue(0.0);
        }
        return new Pair<>(analyzeCriterion1,analyzeCriterion2);
    }

    //Среднесуточный полезный пробег локомотивов (км)
    // Среднесуточный процент полезного использования локомотивов
    public Pair<AnalyzeCriterion,AnalyzeCriterion>  getDataForRow52_53(long time) {
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;

        int mileage = 0;
        int usage = 0;


        AnalyzeCriterion analyzeCriterion1 = new AnalyzeCriterion(52L);
        AnalyzeCriterion analyzeCriterion2 = new AnalyzeCriterion(53L);

        for(SlotLoco loco: results.slotLocos.values()) {
            for(SlotLoco.Track track : loco.route) {
                try {
                    if(track.state.intValue() == BaseLocoTrack.State.WITH_TRAIN.ordinal() &&
                            track.timeEnd <= endTime ){
                        mileage += iData.getLinkByStationPair(track.getStationPair()).getDistance();
                        usage += track.timeEnd - track.timeStart;
                    }
                } catch (Exception e) {
                    LOGGER().error("getDataForRow52 exception: loco" + loco.id);
                    LoggingAssistant.logException(e);
                }
            }
        }


        int  nlocos = results.slotLocos.size();
        if (nlocos == 0) {
            analyzeCriterion1.setValue(0.0);
            analyzeCriterion2.setValue(0.0);
        } else {
            analyzeCriterion1.setValue(1.0 * mileage / nlocos);
            analyzeCriterion2.setValue(100.0 *  usage /  nlocos / time );
        }
        return new Pair<> (analyzeCriterion1,analyzeCriterion2);
    }

    //Среднее время стоянки поезда на технической станции
    /*
    1.	Составить список технических станций – это станции, на которых возможна смена локомотива или
        бригады (то есть станции, для которых заданы атрибуты process_time или norm_time).
    2.	Для каждого поезда составить список технических станций на суточном маршруте этого поезда.
    3.	Для каждой технической станции на маршруте поезда найти время стоянки поезда на этой станции
        (разность между временем отправления и временем прибытия на станцию).
    4.	Для всех поездов сложить найденные времена стоянок, разделить сумму на количество
        ненулевых стоянок (стоянок, у которых время стоянки > 0) на технических станциях всех поездов, вместе взятых.
     */
    public AnalyzeCriterion getDataForRow54(int time) {
        int counter = 0;
        int totStopTime = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(54L);

        for (SlotTrain train : results.slotTrains.values()) {
            try {
                List<SlotTrain.Track> route = train.route;
                int totTracks = route.size();
                if(totTracks > 0) {
                    for (int i = 1; i < totTracks; i++) {
                        SlotTrain.Track prevTrack = route.get(i-1);
                        SlotTrain.Track curTrack = route.get(i);
                        Station curStation = iData.getStationById(curTrack.stationFromId);
                        if(curStation.getProcessTime() > 0 || curStation.getNormTime() > 0){
                            long stopTime = curTrack.timeStart - prevTrack.timeEnd;
                            if(stopTime > 0 && prevTrack.timeEnd <= endTime) {
                                stopTime = Math.min(stopTime,endTime - prevTrack.timeEnd);
                                counter++;
                                totStopTime += stopTime;
                                analyzeCriterion.addInfoString("stop(name(" + curStation.getName()
                                        + "),time(" + stopTime
                                        + "),train(" + train.id + "))");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow54 exception: " + e.getMessage() + " train: " + train.id);
                LoggingAssistant.logException(e);
            }
        }

        if(counter == 0) {
            analyzeCriterion.setValue(0.0);
        } else {
            analyzeCriterion.setValue(1.0 * totStopTime / counter / 3600.0);
        }
        return analyzeCriterion;
    }

    //Средний простой локомотива на станции
    /*
    1.	Для каждого запланированного локомотива вычислить время стоянки на каждой станции на суточном
    маршруте (разность между временем отправления со станции и временем прибытия на станцию).
     2.	Просуммировать все времена стоянок по всем запланированным локомотивам,
    разделить на количество запланированных локомотивов (Такмаз: нет - сумму всех промежуточных станций
    для всех  локомотивов),  выразить получившееся значение в часах.

     */

    //Средний простой локомотива на станции в ожидании работы
    /*
    Для каждого локомотива на суточном маршруте найти станции, на которых происходит смена этапа работы локомотива.
    На каждой такой станции вычислить простой локомотива в ожидании работы. Он считается по формуле T=(T_D-T_A )-T_W,
    где TD – время отправления локомотива на следующий этап со станции, TA – время прибытия локомотива на станцию,
    TW – время на прицепку и отцепку локомотива. При этом если локомотив прибыл на станцию с поездом и уезжает
    с поездом (другим), то TW = времени обработки локомотива (process_time). Если же локомотив прибыл с поездом ИЛИ
    уезжает с поездом (другой этап же отличается), то TW – это половина времени обработки локомотива (process_time),
    так как в этом случае нужна или только прицепка, или только отцепка
     */

    public Pair<AnalyzeCriterion,AnalyzeCriterion> getDataForRow55_56(int time) {
        int counter1 = 0;
        int counter2 = 0;
        int totStopTime1 = 0;
        int totStopTime2 = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;

        AnalyzeCriterion analyzeCriterion1 = new AnalyzeCriterion(55L);
        AnalyzeCriterion analyzeCriterion2 = new AnalyzeCriterion(56L);

        for (SlotLoco loco: results.slotLocos.values()) {
            try {
                SlotLoco.Track prevTrack = null;
                for (SlotLoco.Track curTrack : loco.route) {
                    long stopTime = 0;
                    boolean sameStation = false;
                    if(prevTrack != null) {
                        if (prevTrack.timeEnd <= endTime) {
                            if (curTrack.stationFromId.equals(curTrack.stationToId)) {  // стоит на станции
                                stopTime += Math.min(endTime, curTrack.timeEnd)
                                        - Math.min(endTime, curTrack.timeStart);
                                sameStation = true;
                            }
                            stopTime += Math.min(endTime, curTrack.timeStart)
                                    - Math.min(endTime, prevTrack.timeEnd);  // учесть разницу времени прибытия и отправления
                            if (stopTime > 0) {
                                if (!sameStation) counter1++;
                                totStopTime1 += stopTime;
                                analyzeCriterion1.addInfoString("station(name(" +
                                        curTrack.stationFromId
                                        + "),time(" + stopTime / 3600.0
                                        + "),loco(" + loco.id + "))");
                                boolean isPrevTrain =
                                        (prevTrack.state.intValue() == BaseLocoTrack.State.WITH_TRAIN.ordinal());
                                boolean isCurTrain =
                                        (curTrack.state.intValue() == BaseLocoTrack.State.WITH_TRAIN.ordinal());
                                if ((isPrevTrain || isCurTrain) &&
                                        !curTrack.trainId.equals(prevTrack.trainId)) {
                                    if (!sameStation) counter2++;
                                    long procTime = iData.getStationById(curTrack.stationFromId).getProcessTime();
                                    long addTime;
                                    if (isCurTrain && isPrevTrain) {
                                        addTime = Math.max(0, stopTime - procTime);
                                    } else {
                                        addTime = Math.max(0, stopTime - procTime / 2);
                                    }
                                    totStopTime2 += addTime;

                                    analyzeCriterion2.addInfoString("station(name(" +
                                            curTrack.stationFromId
                                            + "),time(" + addTime / 3600.0
                                            + "),loco(" + loco.id + "))");
                                }
                            }
                        } else
                            break;
                    }
                    prevTrack = curTrack;
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow55_56 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
        }

        if(counter1 == 0) {
            analyzeCriterion1.setValue(0.0);
        } else {
            analyzeCriterion1.setValue(1.0 * totStopTime1 / counter1 / 3600.0);
        }
        if(counter2 == 0) {
            analyzeCriterion2.setValue(0.0);
        } else {
            analyzeCriterion2.setValue(1.0 * totStopTime2 / counter2 / 3600.0);
        }
        return new Pair<>(analyzeCriterion1,analyzeCriterion2);
    }


    //Среднее время отдыха бригад в пунктах оборота
    /*
    1.	Выделить все бригады, у которых был запланирован отдых в пункте оборота
        (есть участок маршрута со state = 4).
    2.	Для всех таких бригад найти время их фактического отдыха – разность
       между отправлением бригады на следующий после отдыха участок маршрута и временем начала отдыха.
    3.	Просуммировать времена фактического отдыха для всех отдыхавших бригад, разделить на
         количество отдыхавших бригад.
     */

    // Количество бригад с переотдыхом
    /*
    1.	Для каждой бригады, которую планировщик запланировал на отдых, рассчитать рабочее время бригады до ухода на
        отдых (разница между временем отправления и временем прибытия на этап работы до отдыха).
    2.	Для каждой бригады, которую планировщик запланировал на отдых, рассчитать время отдыха (разность между
        временем отправления бригады на следующий этап работы после отдыха и временем ухода на отдых).
    3.	Посчитать количество бригад, для которых время отдыха превышает рабочее время перед отдыхом.

     */

    public Pair<AnalyzeCriterion,AnalyzeCriterion> getDataForRow57_58(int time) {
        int counterRests = 0;
        int counterLongRests = 0;
        long totRestTime = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;
        long stationAtId = 0;

        AnalyzeCriterion analyzeCriterion1 = new AnalyzeCriterion(57L);
        AnalyzeCriterion analyzeCriterion2 = new AnalyzeCriterion(58L);

        for (SlotTeam team : results.slotTeams.values()) {
            try {
                SlotTeam.Track startWorkFragment = null;
                SlotTeam.Track startRestFragment = null;
                SlotTeam.Track endRestFragment = null;
                Iterator<SlotTeam.Track> iterator = team.route.iterator();
                boolean useTeam = false;
                long teamRestTime = 0L;
                long teamWorkTime = 0L;
                while (iterator.hasNext()) {
                    SlotTeam.Track track = iterator.next();

                    if(track.timeStart >= endTime) { // если отрезок начинается вне проверочного интервала
                        break;      //  завершить работу с этой бригадой и перейти к следующей
                    }
                    if(track.state.intValue() == BaseTeamTrack.State.AT_WORK.ordinal()
                            && startWorkFragment == null) {
                                startWorkFragment = track;
                    }
                    if(startWorkFragment != null && (
                            track.state.intValue() == BaseTeamTrack.State.REST.ordinal()
                            || track.state.intValue() == BaseTeamTrack.State.READY.ordinal())
                            ) {
                        endRestFragment = track;        // текущий конец данного интервала отдыха
                        if (startRestFragment == null) {
                                startRestFragment = track;
                        }
                        if (track.state.intValue() == BaseTeamTrack.State.REST.ordinal()) {
                            useTeam = true;             // данная бригада отдыхает
                        }
                    }
                    if (startRestFragment != null &&
                            (track.state.intValue() == BaseTeamTrack.State.AT_WORK.ordinal()
                                    || track.state.intValue() == BaseTeamTrack.State.PASSENGER.ordinal()
                                    || !iterator.hasNext()
                            )
                            ) {                     // данный интервал работы на этом закончился
                        if (endRestFragment.timeEnd > endTime) {        // если уже вышли за пределы интервала расчета
                            teamRestTime = (endTime - startRestFragment.timeStart);
                        } else {
                            teamRestTime = (endRestFragment.timeEnd - startRestFragment.timeStart);
                        }
                        break;                  // закончить с данной бригадой и перейти к следующей
                    }

                }
                if (useTeam) {
                    teamWorkTime = (startRestFragment.timeStart - startWorkFragment.timeStart);
                    counterRests++;
                    totRestTime += teamRestTime;
                    String info = "team(id(" + team.id + "),time(" + teamRestTime / 3600.0 + "))";
                    analyzeCriterion1.addInfoString(info);
                    AnalyzerInfoEntry iEntry = new AnalyzerInfoEntry();
                    iEntry.teamId = team.id;
                    iEntry.stationAtId = startRestFragment.stationFromId;
                    iEntry.timeDuration = teamRestTime;
                    analyzeCriterion1.addInfo(iEntry);
                    if(teamRestTime > teamWorkTime) {
                        counterLongRests++;
                        AnalyzerInfoEntry infoEntry = new AnalyzerInfoEntry();
                        infoEntry.teamId = team.id;
                        infoEntry.stationAtId = startRestFragment.stationFromId;
                        infoEntry.timeDuration = (teamRestTime - teamWorkTime);
                        analyzeCriterion2.addInfo(infoEntry);
                        String info2 = "team_rest(id(" + team.id +
                                "),station(" + startRestFragment.stationFromId
                                + "),workTime(" + teamWorkTime / 3600.0
                                + "),restTime(" + teamRestTime / 3600.0
                                + "))";
                        analyzeCriterion2.addInfoString(info2);
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow57_58 exception: " + e.getMessage() + " team: " + team.id);
                LoggingAssistant.logException(e);
            }
        }

        if(counterRests == 0) {
            analyzeCriterion1.setValue(0.0);
        } else {
            analyzeCriterion1.setValue(1.0 * totRestTime / counterRests / 3600.0);
        }
        analyzeCriterion2.setValue(1.0 * counterLongRests);
        return new Pair<>(analyzeCriterion1, analyzeCriterion2);
    }


    /*
    public Pair<AnalyzeCriterion,AnalyzeCriterion> getDataForRow57_58(int time) {
        int counterRests = 0;
        int counterLongRests = 0;
        long totRestTime = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;
        long stationAtId = 0;

        AnalyzeCriterion analyzeCriterion1 = new AnalyzeCriterion(57L);
        AnalyzeCriterion analyzeCriterion2 = new AnalyzeCriterion(58L);

        for (SlotTeam team : results.slotTeams.values()) {
            try {
                List<SlotTeam.Track> route =  team.route;
                int totTracks = route.size();
                if(totTracks > 0 ) {
                    long teamRestTime = 0;
                    long teamWorkTime = 0;
                    StationPair lastStationPair = null;
                    boolean rest = false;
                    for (int i = 1; i < totTracks; i++) {
                        SlotTeam.Track prevTrack = route.get(i-1);
                        SlotTeam.Track track = route.get(i);
                        if (prevTrack.state.intValue() == BaseTeamTrack.State.AT_WORK.ordinal()
                                && prevTrack.timeStart <= endTime) {
                            teamWorkTime += Math.min(endTime, track.timeStart)
                                    - prevTrack.timeStart;
                            lastStationPair = prevTrack.getStationPair();
                        }
                        if (prevTrack.state.intValue() == BaseTeamTrack.State.REST.ordinal()
                                && prevTrack.timeStart <= endTime) {
                            if(!rest) {
                                rest = true;
                                counterRests++;
                            }
                            long curRestTime = Math.min(endTime,track.timeStart)
                                    - prevTrack.timeStart;
                            teamRestTime += curRestTime;
                            totRestTime += curRestTime;
                            stationAtId = track.stationToId;
                            if(track.state.intValue() != BaseTeamTrack.State.REST.ordinal()
                                    ||  track.timeStart > endTime) {
                                if(teamRestTime > teamWorkTime
                                        && !restrictStationPairs.contains(lastStationPair)) {
                                    counterLongRests++;
                                    AnalyzerInfoEntry infoEntry = new AnalyzerInfoEntry();
                                    infoEntry.teamId = team.id;
                                    infoEntry.stationAtId = stationAtId;
                                    infoEntry.timeDuration = teamRestTime ;
                                    analyzeCriterion2.addInfo(infoEntry);
                                    analyzeCriterion2.addInfoString("team_rest(id(" + team.id +
                                            "),station(" + prevTrack.stationFromId
                                            + "),workTime(" + teamWorkTime / 3600.0
                                            + "),restTime(" + teamRestTime / 3600.0
                                            + "))");
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow57_58 exception: " + e.getMessage() + " team: " + team.id);
                LoggingAssistant.logException(e);
            }
        }

        if(counterRests == 0) {
            analyzeCriterion1.setValue(0.0);
        } else {
            analyzeCriterion1.setValue(1.0 * totRestTime / counterRests / 3600.0);
        }
        analyzeCriterion2.setValue(1.0 * counterLongRests);
        return new Pair<>(analyzeCriterion1, analyzeCriterion2);
    }
    */

    // Количество бригад, отправленных с оборота
    /*
    1.	Рассчитать количество бригад, для которых в плане запланировано прибытие на какую-либо
        станцию и последующая отправка обратно (на тот же участок планирования, но в обратном направлении)
        без каких-либо промежуточных операций (отправка на другие станции или отдых).
     */

    public AnalyzeCriterion getDataForRow59(int time) {
        int counter = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(59L);

        for (SlotTeam team : results.slotTeams.values()) {
            try {
                List<SlotTeam.Track> route = team.route;
                int totTracks = route.size();
                if (totTracks > 0) {
                    for (int i = 1; i < totTracks; i++) {
                        SlotTeam.Track prevTrack = route.get(i - 1);
                        SlotTeam.Track track = route.get(i);
                        if (prevTrack.timeStart.longValue() <= endTime
                                && prevTrack.state.intValue() != BaseTeamTrack.State.REST.ordinal()
                                && track.state.intValue() != BaseTeamTrack.State.REST.ordinal()
                                && track.stationFromId.equals(prevTrack.stationToId)
                                && track.stationToId.equals(prevTrack.stationFromId)) {
                            counter++;
                            AnalyzerInfoEntry infoEntry = new AnalyzerInfoEntry();
                            infoEntry.teamId = team.id;
                            infoEntry.stationAtId = track.stationFromId;
                            analyzeCriterion.addInfo(infoEntry);
                            analyzeCriterion.addInfoString("team_turn(id(" + team.id +
                                    "),station(" + prevTrack.stationFromId
                                    + "))");
                        }
                        if (track.timeStart > endTime) break;
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow57_58 exception: " + e.getMessage() + " team: " + team.id);
                LoggingAssistant.logException(e);
            }
        }
        analyzeCriterion.setValue(1.0 * counter);
        return analyzeCriterion;
    }

    // Количество резервных локомотивов без бригад
    /*
    1.	Найти все поезда, которые соответствуют пересылкам локомотивов резервом.
    2.	Посчитать количество таких поездов, для которых хотя бы на одном участке маршрута к соответствующему
        локомотиву не подвязана бригада (Поскольку один локомотив может пересылаться резервом несколько раз,
        то просто посчитать количество таких локомотивов недостаточно, поскольку, теоретически,
        к каждой такой пересылке резервом может не найтись бригады).
     */

    public AnalyzeCriterion getDataForRow60(int time) {
        int counter = 0;
        long beginTime = iData.getCurrentTime();
        long endTime = beginTime + time;

        AnalyzeCriterion analyzeCriterion = new AnalyzeCriterion(60L);

        for (SlotLoco loco : results.slotLocos.values()) {
            try {
                List<SlotLoco.Track> route = loco.route;
                int totTracks = route.size();
                if(totTracks > 0) {
                    boolean reserve = false;
                    long prevTrainId = route.get(0).trainId;
                    for (SlotLoco.Track track: route)  {
                        if(track.timeStart.longValue() <= endTime &&
                                track.state.intValue() == BaseLocoTrack.State.RESERVE.ordinal()
                                && (!reserve || track.trainId != prevTrainId)) {
                            boolean found = false;
                            reserve = true;
                            a: for(SlotTeam team: results.slotTeams.values()) {
                                for(SlotTeam.Track track1: team.route) {
                                    if(track.equalFilelds(track1)) {
                                        found = true;
                                        break a;
                                    }
                                }
                            }
                            if(!found) {
                                counter++;
                                AnalyzerInfoEntry infoEntry = new AnalyzerInfoEntry();
                                infoEntry.locoId = loco.id;
                                infoEntry.trainId = track.trainId;
                                infoEntry.stationFromId = track.stationFromId;
                                infoEntry.stationToId = track.stationToId;
                                infoEntry.timeStart = track.timeStart;
                                analyzeCriterion.addInfo(infoEntry);
                                analyzeCriterion.addInfoString("loco(id(" + loco.id +
                                        "),train(" + track.trainId +
                                        "),from(" + track.stationFromId +
                                        "),to(" + track.stationToId +
                                        "),timeStart(" + track.timeStart
                                        + "))");
                            }
                        }
                        if(track.state.intValue() != BaseLocoTrack.State.RESERVE.ordinal()) {
                            reserve = false;
                        }
                        prevTrainId = track.trainId;
                    }
                }
            } catch (Exception e) {
                LOGGER().error("getDataForRow60 exception: " + e.getMessage() + " loco: " + loco.id);
                LoggingAssistant.logException(e);
            }
        }

        if(counter == 0) {
            analyzeCriterion.setValue(0.0);
        } else {
            analyzeCriterion.setValue(1.0 * counter);
        }
        return analyzeCriterion;
    }



}
