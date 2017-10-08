package ru.programpark.planners.check.output;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.loco.LocoTonnage;
import ru.programpark.entity.loco.SeriesPair;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.raw_entities.SlotTrain;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.TrainCategory;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class SlotTrainChecker {
    private static PrintWriter writer = null;
    private static NumberFormat formatter = new DecimalFormat("#0.00");

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    public static long MIN_THREAD_TIME = 10L * 60L;

    private static Map<StationPair, Collection<SlotTrain>> getTrainByLinks(Collection<SlotTrain> trains) {
        Map<StationPair, Collection<SlotTrain>> trainByLinks = new HashMap<>();
        for(SlotTrain train : trains) {
            for (SlotTrain.Track track : train.route) {
                StationPair stationPair = new StationPair(track.stationFromId, track.stationToId);
                Collection<SlotTrain> currentTrains = trainByLinks.get(stationPair);
                if (currentTrains == null) currentTrains = new HashSet<>();
                SlotTrain cutTrain = new SlotTrain();
                cutTrain.id = train.id;
                cutTrain.route.add(track);
                currentTrains.add(cutTrain);
                trainByLinks.put(stationPair, currentTrains);
            }
        }
        return trainByLinks;
    }

    //Участки, на которых ищeтся выбросы функции распределения времени хода поезда по участку
    private static Collection<StationPair> observeLinks = new ArrayList<StationPair>() {{
        add(new StationPair(2000036518L, 2000036796L)); //ТАЙШЕТ - ВИХОРЕВКА
        add(new StationPair(2000036796L, 2000036518L)); //ВИХОРЕВКА - ТАЙШЕТ

        add(new StationPair(2000036796L, 2000036868L)); //ВИХОРЕВКА - КОРШУНИХА-АНГАРСКАЯ
        add(new StationPair(2000036868L, 2000036796L)); //КОРШУНИХА-АНГАРСКАЯ - ВИХОРЕВКА

        add(new StationPair(2000036868L, 2000036932L)); //КОРШУНИХА-АНГАРСКАЯ - ЛЕНА
        add(new StationPair(2000036932L, 2000036868L)); //ЛЕНА - КОРШУНИХА-АНГАРСКАЯ

        add(new StationPair(2000036932L, 2000036154L));  //ЛЕНА - СЕВЕРОБАЙКАЛЬСК
        add(new StationPair(2000036154L, 2000036932L));  //СЕВЕРОБАЙКАЛЬСК - ЛЕНА

        add(new StationPair(2000036154L, 2000036192L));  //СЕВЕРОБАЙКАЛЬСК - НОВЫЙ УОЯН
        add(new StationPair(2000036192L, 2000036154L)); //НОВЫЙ УОЯН - СЕВЕРОБАЙКАЛЬСК

        add(new StationPair(2000036192L, 2000036228L)); //НОВЫЙ УОЯН - ТАКСИМО
        add(new StationPair(2000036228L, 2000036192L)); //ТАКСИМО - НОВЫЙ УОЯН
    }};

    private static boolean checkStops(InputData iData, SlotTrain train){
        boolean res = false;
        SlotTrain.Track prev = null;

        for (SlotTrain.Track track: train.route) {
            if (prev != null && !prev.timeEnd.equals(track.timeStart))
                log(String.format("Стоянка на " + iData.getStationById(track.stationFromId).getName() +
                        " у поезда " + train.id + " длиной в " +
                        formatter.format((track.timeStart - prev.timeEnd) / 3600.0)) + " ч");
            prev = track;
        }
        return res;
    }

    private static Long getNonStopDuration(InputData iData, SlotTrain train){
        long res = 0L;

        for (SlotTrain.Track track: train.route){
            Link link = iData.getLinkByStationPair(new StationPair(track.stationFromId, track.stationToId));
            res += link.getDuration(track.timeStart);
        }

        return res;
    }

    private static ArrayList<SlotTrain> createFakeTrainForObserveLink(StationPair observeLink,
                                                                      Collection<SlotTrain> trains) {
        ArrayList<SlotTrain> foundTrains = new ArrayList<>();
        for (SlotTrain train : trains) {
            SlotTrain fakeTrain = new SlotTrain();
            fakeTrain.id = train.id;

            for (SlotTrain.Track track : train.route) {
                if (track.stationFromId.equals(observeLink.stationFromId)) {
                    fakeTrain.route.add(track);
                } else {
                    if (fakeTrain.route.size() > 0) {
                        fakeTrain.route.add(track);
                    }
                    if (fakeTrain.route.size() > 0 && track.stationToId.equals(observeLink.stationToId)) {
                        foundTrains.add(fakeTrain);
                        break;
                    }
                }
            }
        }
        return foundTrains;
    }

    public static boolean checkAbnormalLength_3_1_7(InputData iData, Collection<SlotTrain> trains) {  //3.1.7
        boolean pass = true;
        String testName = "Тест 3.1.7 (Затянутые и слишком быстрые нитки графика)";

        for (StationPair observeLink : observeLinks) {
            ArrayList<SlotTrain> foundTrains = createFakeTrainForObserveLink(observeLink, trains);
            if (foundTrains.size() == 0) continue;
            Collections.sort(foundTrains, new Comparator<SlotTrain>() {
                @Override
                public int compare(SlotTrain o1, SlotTrain o2) {
                    return o1.getTrainLength().compareTo(o2.getTrainLength());
                }
            });

            int firstQuartaIndex = foundTrains.size() / 4;
            long firstQuarta = foundTrains.get(firstQuartaIndex).getTrainLength();
            int secondQuartaIndex = (foundTrains.size() * 2) / 4;
            long secondQuarta = foundTrains.get(secondQuartaIndex).getTrainLength();
            int threeQuartaIndex = (foundTrains.size() * 3) / 4;
            long threeQuarta = foundTrains.get(threeQuartaIndex).getTrainLength();
            for (SlotTrain train : foundTrains) {
                Long trainLength = train.getTrainLength();
                if (!(trainLength >= firstQuarta - 1.5 * (threeQuarta - firstQuarta) && (trainLength <= threeQuarta +
                        1.5 * (threeQuarta - firstQuarta))) && !trainLength.equals(getNonStopDuration(iData, train))) {
                    if (pass)
                        log(testName + " не пройден");
                    pass = false;
                    log("Выброс по длительности для поезда " + train.id + " на отрезке маршрута " +
                            iData.getStationById(observeLink.stationFromId).getName() + " - " +
                            iData.getStationById(observeLink.stationToId).getName() + " время хода для поезда " +
                            formatter.format(train.getTrainLength()/3600.0) +
                            " ч, а медианное время хода - " + formatter.format(secondQuarta/3600.0) + " ч");
                    log("Время хода по данному отрузку маршрута: " +
                            formatter.format(getNonStopDuration(iData, train)/3600.0) + " ч");
                    checkStops(iData, train);  // для лога, где именно остановка если она таки есть
                }
            }
        }

        return pass;
    }

    //Слишком близкие нитки
    public static boolean checkCloseThread_3_1_10(Collection<SlotTrain> trains, Collection<SlotLoco> locos,
                                                  Collection<SlotTeam> teams) {//Слишком близкие нитки (меньше 10 минут)
        String testName = "Тест 3.1.10 (Слишком близкие нитки < 10 минут)";
        boolean pass = true;

        Map<StationPair, Collection<SlotTrain>> trainByLinks = getTrainByLinks(trains);
        for (Map.Entry<StationPair, Collection<SlotTrain>> entry : trainByLinks.entrySet()) {
            List<SlotTrain> currentTrainList = new ArrayList<>(entry.getValue());
            Collections.sort(currentTrainList, new Comparator<SlotTrain>() {
                @Override
                public int compare(SlotTrain o1, SlotTrain o2) {
                    return o1.route.get(0).timeStart.compareTo(o2.route.get(0).timeStart);
                }
            });
            Long priorTimeStart = 0L, timeStart = 0L;
            SlotTrain priorTrain = null;
            for (SlotTrain train : currentTrainList) {
                if (priorTimeStart != 0L) {
                    timeStart = train.timeDepartFromStation(entry.getKey().stationFromId);
                    if (timeStart - priorTimeStart < MIN_THREAD_TIME &&
                            !train.noTeam(entry.getKey().stationFromId, timeStart, locos, teams) &&
                            !priorTrain.noTeam(entry.getKey().stationFromId, priorTimeStart, locos, teams)) {
                        if (pass)
                            log(testName + " не пройден");
                        pass = false;
                        log("Слишком близкие нитки для поездов " + priorTrain.id + "," + train.id + " на участке " +
                                entry.getKey().stationFromId + " - " + entry.getKey().stationToId
                                + " времена ухода со станции : " + new Time(priorTimeStart).getTimeStamp() + " и " +
                                new Time(timeStart).getTimeStamp() + ", разница составляет " +
                                formatter.format((timeStart - priorTimeStart) / 3600.0) + " ч");
                    }
                }
                priorTimeStart = timeStart;
                priorTrain = train;
            }
        }
        return pass;
    }

    public static boolean checkCrossThread_3_1_11(InputData iData, Collection<SlotTrain> trains) {
    //3.1.11 Пересекающиеся нитки
        boolean pass = true;
        String testName = "Тест 3.1.11 (Нет пересекающихся ниток, отсутствуют обгоны на перегонах)";
        Map<Link, List<SlotTrain.Track>> tracksByLink = new HashMap();

        for (SlotTrain train: trains){
            for (SlotTrain.Track track: train.route){
                Link link = iData.getLinkByStationPair(new StationPair(track.stationFromId, track.stationToId));
                if (tracksByLink.get(link) == null) {
                    tracksByLink.put(link, new ArrayList<SlotTrain.Track>());
                }
                track.trainId = train.id;
                tracksByLink.get(link).add(track);
            }
        }

        for (Link link: tracksByLink.keySet()) {
            SlotTrain.Track prev = null;
            for (SlotTrain.Track track: tracksByLink.get(link)) {
                if (prev != null) {
                    if ((prev.timeStart > track.timeStart && prev.timeEnd < track.timeEnd) ||
                            (track.timeStart > prev.timeStart && track.timeEnd < prev.timeEnd)) {
                        if (pass)
                            log(testName + " не пройден");
                        pass = false;
                        if (prev.timeStart > track.timeStart && prev.timeEnd < track.timeEnd)
                            log("На перегоне " + link.getFrom().getName() + " - " + link.getTo().getName() + " поезд "
                                    + prev.trainId + " обогнал поезд " + track.trainId);
                        if (track.timeStart > prev.timeStart && track.timeEnd < prev.timeEnd)
                            log("На перегоне " + link.getFrom().getName() + " - " + link.getTo().getName() + " поезд " +
                                    track.trainId + " обогнал поезд " + prev.trainId);
                        log("Время хода для поезда " + track.trainId + ": " + new Time(track.timeStart).getTimeStamp() +
                                " - " + new Time(track.timeEnd).getTimeStamp());
                        log("Время хода для поезда " + prev.trainId + ": " + new Time(prev.timeStart).getTimeStamp() +
                                " - " + new Time(prev.timeEnd).getTimeStamp());
                    }
                }
                prev = track;
            }
        }

        return pass;
    }

    public static boolean checkFullTrainRoutesAssigned_3_1_13(InputData input, Map<Long, SlotTrain> trains){ //3.1.13
        /*
        Для каждого поезда, который поступил на вход планировщика и не был отсеян, определить список участков
        планирования, по которым должен проехать поезд (начиная от станции, на которой находился поезд на
        начало планирования).
        Проверить, что все эти участки присутствуют в результатах планирования в соответствующем сообщении slot_train.
        В случае ошибки выводить id поезда, для которого возвращен не весь маршрут.
         */
        boolean pass = true;
        String testName = "Тест 3.1.13 (Все поезда запланированы по всему маршруту)";

        List<FactTrain> factTrains = new ArrayList<>();
        factTrains.addAll(input.getFactTrains().values());
        factTrains.addAll(input.getFictitousFactTrainsFromTasks().values());

        for (FactTrain fTrain: factTrains){
            SlotTrain train =  trains.get(fTrain.getId());
            if (train == null)
                continue;
            int i = 0;
            boolean startRoute = false;
            try {
                for (Link link : fTrain.getMainRoute().getLinkList()) {
                    if (link.getFrom().getId().equals(fTrain.getFirstStationId())) {
                        startRoute = true;
                    }
                    if (startRoute) {
                        SlotTrain.Track track = null;
                        if (i < train.route.size())
                            track = train.route.get(i);
                        if (track == null || !link.getFrom().getId().equals(track.stationFromId) ||
                                !link.getTo().getId().equals(track.stationToId)) {
                            if (pass)
                                log(testName + " не пройден");
                            pass = false;
                            log("У фактического поезда и запланированного поезда " + train.id +
                                    " расхождение в маршруте , начиная с перегона факт. поезда " +
                                    link.getFrom().getName() + "-" + link.getTo().getName());
                            break;
                        }
                        i++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return pass;
    }

    private static List<SlotTrain> getTrainsPassingStation(Collection<SlotTrain> trains, Station s){
    // Планирование поездов по всему маршруту
        List<SlotTrain> passingTrains = new ArrayList<>();

        for (SlotTrain train: trains){
            if (train.stopsOnStation(s))
                passingTrains.add(train);
        }

        return passingTrains;
    }

    public static boolean checkNoOvertakingOnStations_3_1_19(InputData iData, Collection<SlotTrain> trains){
    // 3.1.19 Поезда не обгоняют друг друга на станциях
        /*
        - Цикл по всем станциям
        - Сделать выборку по всем поездам, которые проходят эту станцию.
        - Отсортировать полученные поезда по времени прибытия на эту станцию.
        - Отсортировать полученные поезда по времени отправления с этой станции.
        - Проверить, что два порядки в отсортированных списках одинаковы.
        В случае ошибки вывести id поездов, которые находятся не на своем месте и станцию на которой зафиксирован обгон.
        */
        boolean pass = true;
        String testName = "Тест 3.1.19 (Поезда не обгоняют друг друга на станциях, кроме более приоритетных)";

        for (Station st: iData.getStations().values()){
            final Station s = st;
            List<SlotTrain> passingTrains = getTrainsPassingStation(trains, st);
            List<SlotTrain> trainListArrive = new ArrayList<>(passingTrains);

            Collections.sort(trainListArrive, new Comparator<SlotTrain>() {
                @Override
                public int compare(SlotTrain o1, SlotTrain o2) {
                    return o1.timeArriveAtStation(s.getId()).compareTo(o2.timeArriveAtStation(s.getId()));
                }
            });

            List<SlotTrain> trainListDepart = new ArrayList<>(passingTrains);
            Collections.sort(trainListDepart, new Comparator<SlotTrain>() {
                @Override
                public int compare(SlotTrain o1, SlotTrain o2) {
                    return o1.timeDepartFromStation(s.getId()).compareTo(o2.timeDepartFromStation(s.getId()));
                }
            });

            int i = 0;
            for (SlotTrain trainArrive: trainListArrive){
                SlotTrain trainDepart = trainListDepart.get(i);
                if (!trainArrive.equals(trainDepart)) {
                    FactTrain fTrain = iData.getFactTrains().get(trainArrive.id);
                    TrainCategory cat = (iData.getTrainCategories() == null || fTrain == null ||
                            fTrain.getCategory() == null) ? null: iData.getTrainCategories().get(fTrain.getCategory());
                    int priorityArrive = (cat == null || cat.getPriority() == null) ? Integer.MAX_VALUE: cat.getPriority();
                    fTrain = iData.getFactTrains().get(trainDepart.id);
                    cat = (iData.getTrainCategories() == null || fTrain == null ||
                            fTrain.getCategory() == null)  ? null : iData.getTrainCategories().get(fTrain.getCategory());
                    int priorityDepart = (cat == null || cat.getPriority() == null) ? Integer.MAX_VALUE: cat.getPriority();
                    if (priorityDepart < priorityArrive) {  // 1 - главный приоритет
                        continue;
                    } else {
                        if (pass)
                            log(testName + " не пройден");
                        pass = false;
                        log("На станции " + st.getName() + " поезд " + trainDepart.id + " обогнал поезд " +
                                trainArrive.id);
                        log("Приоритет поезда " + trainArrive.id + ": " + priorityArrive + " , прибыл в " +
                                new Time(trainArrive.timeArriveAtStation(s.getId())).getTimeStamp() +
                                ", убыл в " + new Time(trainArrive.timeDepartFromStation(s.getId())).getTimeStamp());
                        log("Приоритет поезда " + trainDepart.id + ": " + priorityArrive + " , прибыл в " +
                                new Time(trainDepart.timeArriveAtStation(s.getId())).getTimeStamp() +
                                ", убыл в " + new Time(trainDepart.timeDepartFromStation(s.getId())).getTimeStamp());
                    }
                }
                i++;
            }
        }

        return pass;
    }

    public static boolean checkAllTrainOrder_3_1_21(InputData input, Collection<SlotTrain> slotTrains){
    //Поезда отправляются в правильном порядке
        /*
        Отсортировать поезда во входных данных по времени факта (прибытие arrive_time – для поездов с train_arrive,
        готовность fact_time – для поездов с train_ready).
        Отсортировать поезда на выходе планировщика по времени отправления. Проверить, что порядок отправления поездов
        соответствует порядку в отсортированном списке
        поездов из входных данных.
        В случае ошибки вывести id поездов, которые находятся не на своем месте в списке выходных поездов, для каждого
        поезда указать время из входного факта и
        время отправления.
         */
        boolean pass = true;
        String testName = "Тест 3.1.21 (Поезда отправляются в правильном порядке)";

        List<FactTrain> factTrains = new ArrayList<>(input.getFactTrains().values());
        factTrains.addAll(input.getFictitousFactTrainsFromTasks().values());
        Collections.sort(factTrains, new Comparator<FactTrain>() {
            @Override
            public int compare(FactTrain o1, FactTrain o2) {
                return o1.getTrainState().getTime().compareTo(o2.getTrainState().getTime());
            }
        });

        List<SlotTrain> trains = new ArrayList<>(slotTrains);
        Collections.sort(trains, new Comparator<SlotTrain>() {
            @Override
            public int compare(SlotTrain o1, SlotTrain o2) {
                return o1.route.get(0).timeStart.compareTo(o2.route.get(0).timeStart);
            }
        });

        if (!(factTrains.size() > 0) || !(trains.size() > 0) || !(factTrains.size() == trains.size()))
            return false;

        for (int i = 0; i < factTrains.size(); i++) {
            FactTrain factTrain = factTrains.get(i);
            SlotTrain train = trains.get(i);
            FactTrain actualFactTrain = input.getFactTrains().get(train.id);
            if (factTrain != null && !factTrain.getId().equals(train.id)) {
                if (pass)
                    log (testName + " не пройден");
                pass = false;
                String message = " Поезд " + factTrain.getId() + " отправился не в свое время id: " + train.id +
                        " время из fact_train " + new Time(actualFactTrain.getTrainState().getTime()).getTimeStamp() +
                        ", a реальное время отправления - " + new Time(train.route.get(0).timeStart).getTimeStamp();
                log(message);
            }
        }

        return pass;
    }

    public static boolean checkNumberOfSlotTrainsEachHourEqualsToNumberOfSlots_3_1_22(InputData input,
                                                                                      Collection<SlotTrain> trains){
        //3.1.22
        /*  Количество ниток соответствует количеству отправленных поездов в каждом часу на горизонте планирования
        Проверяем что на каждом часу на горизонте планирования на каждой станции количество ниток и количество
        назначенных поездов совпадает.
         */
        boolean pass = true;
        String testName = "Тест 3.1.22 (Количество ниток соответствует количеству отправленных поездов в каждом " +
                "часу на горизонте планирования на станциях смены бригады или локо)";
        Collection<Slot> slots = input.getSlots().values();

        for (Station s: input.getStations().values()) {
            if (s.getNormTime() == 0L && s.getProcessTime() == 0L)
                continue;//проверяем только для больших станций

            Map<Long, List<Slot>> slotsByHour = new HashMap<>();
            Map<Long, List<SlotTrain>> trainsByHour = new HashMap<>();
            for (Slot slot : slots) {
                if (slot.getRoute().get(s) == null)
                    continue;
                //Long timeForMap сделать по аналогии с расчетов времени хода в зависимости от часа в сутках.
                Long timeStart = (slot.getRoute().get(s).getTimeStart() / 3600) * 3600;
                if (slotsByHour.get(timeStart) == null)
                    slotsByHour.put(timeStart, new ArrayList<Slot>());
                slotsByHour.get(timeStart).add(slot);
            }

            for (SlotTrain train : trains) {
                Long time = 0L;
                for (SlotTrain.Track track : train.route) {
                    if (track.stationFromId.equals(s.getId())) {
                        time = (track.timeStart/3600)*3600;
                        break;
                    }
                }
                if (time == 0L)
                    continue;
                if (trainsByHour.get(time) == null)
                    trainsByHour.put(time, new ArrayList<SlotTrain>());
                trainsByHour.get(time).add(train);
            }

            for (Long time : trainsByHour.keySet()) {
                int nSlots = slotsByHour.get(time) == null ? 0 : slotsByHour.get(time).size();
                int nTrains = trainsByHour.get(time) == null ? 0 : trainsByHour.get(time).size();
                if (slotsByHour.get(time) == null || nSlots < nTrains) {
                    if (pass)
                        log(testName + " не пройден ");
                    pass = false;
                    Long endTime = time + 3600L;
                    log("На станции " + s.getName() + " неравное количество ниток и назначенных поездов в " +
                            "часовом интервале " + new Time(time).getTimeStamp() + " - "
                            + new Time(endTime).getTimeStamp() + ". Поездов " + nTrains + ", a ниток " + nSlots);
                }
            }
        }

        return pass;
    }


    public static boolean checkTrainPriority_3_1_29(InputData iData, Map<Long, SlotTrain> trains){
    //3.1.29 Учет приоритетов грузовых поездов при пропуске через все станции
         /*
        По каждой станции делаем выборку отправления поезда (трека).
        Сортируем по увеличению по времени отправления.
        Затем проверяем, что по каждой станции с увеличением времени,
        приоритет поезда уменьшается, иначе тест не пройден и выводим сообщения на какой станции это не так.
        */
        boolean pass = true;
        String testName = "Тест 3.1.29  (Учет приоритетов грузовых поездов при пропуске через все станции)";
        Map<Station, List<SlotTrain.Track>> trainsByStation = new HashMap<>();

        for (Long t = iData.getCurrentTime(); t <= iData.getCurrentTime() + 24*3600L; t = t + 2*3600L) {//каждые 2 часа
            trainsByStation.clear();
            for (SlotTrain train : trains.values()) {
                Long time = 0L;
                if (train.route.get(0).timeStart <= t) {
                    for (SlotTrain.Track track : train.route) {
                        Station s = iData.getStationById(track.stationFromId);
                        track.trainId = train.id;
                        if (trainsByStation.get(s) == null)
                            trainsByStation.put(s, new ArrayList<SlotTrain.Track>());
                        trainsByStation.get(s).add(track);
                    }
                }
            }
            //сортируем по увеличению времени отправления на каждой станции
            for (Station s: trainsByStation.keySet()){
                List<SlotTrain.Track> tracks = trainsByStation.get(s);
                Collections.sort(tracks);
                int lastPriority = -1;
                long lastTrainId = -1L;
                long lastTimeStart = -1L;
                for (SlotTrain.Track track: tracks) {
                    if (lastPriority == -1)
                        continue;
                    FactTrain fTrain = iData.getFactTrains().get(track.trainId);
                    TrainCategory cat = (iData.getTrainCategories() == null || fTrain == null
                            || fTrain.getCategory() == null) ? null : iData.getTrainCategories().get(fTrain.getCategory());
                    int priority = (cat == null || cat.getPriority() == null) ? Integer.MAX_VALUE : cat.getPriority();
                    if (priority > lastPriority) {
                        if (pass)
                            log(testName + " не пройден");
                        pass = false;
                        log("На станции " + s.getName() + " поезд " + lastTrainId + " с приоритетом " + lastPriority +
                                " был отправлен раньше (" + new Time(lastTimeStart).getTimeStamp() + ") , чем поезд " +
                                track.trainId + " c приорритетом " + priority +
                                ", который был отправлен в " + new Time(track.timeStart).getTimeStamp());
                    }
                    lastPriority = priority;
                    lastTrainId = track.trainId;
                    lastTimeStart = track.timeStart;
                }
            }
        }

        return  pass;
    }

    public static boolean checkTrainsAccordingToWeight(Map<Long, SlotLoco> sLocos, InputData iData){
        /*
        Для каждого поезда на каждом участке планирования ищем локомотив, который везет поезд на этом участке.
        Далее в справочнике весовых категорий (сообщения loco_tonnage) выбираем элемент, который относится
        к данному участку, серии и количеству секций данного локомотива.
        Смотрим у найденного элемента справочника значение максимально разрешенного веса поезда.
        Это значение из справочника не должно быть меньше значения веса поезда, который следует с локомотивом.

        Если было найдено более одной подходящей записи в справочнике, то требуется брать минимальное значение
        разрешенного веса из этих записей.

        Если не найдено ни одной подходящей записи в справочнике, то проверку на данном участке движения поезда
        проводить не следует.

        В случае ошибки выводить id поезда, id локомотива, участок, на котором произошла ошибка, вес поезда.
         */
        Map<Long, FactTrain> fTrains = iData.getFactTrains();
        Map<Long, FactLoco> fLocos = iData.getFactLocos();
        boolean result = true;
        String testName = "Тест 3.1.14 (Учет весовых категорий локомотивов и поездов)";
        Long prevTrainId = -1L;

        for (SlotLoco loco: sLocos.values()) {
            FactLoco fLoco = fLocos.get(loco.id);
            for (SlotLoco.Track track: loco.route){
                if (track.trainId.equals(prevTrainId))
                    continue;
                prevTrainId = track.trainId;
                FactTrain train = fTrains.get(track.trainId);
                if (train == null)
                    continue;
                Link link = iData.getLinkByStationPair(new StationPair(track.stationFromId, track.stationToId));
                LocoTonnage locoTonnage =link.getLocoTonnage(new SeriesPair(fLoco.getSeries(), fLoco.getNSections()));
                if (locoTonnage == null)
                    continue;
                else {
                    if (locoTonnage.getMaxWeight() < train.getWeight()) {
                        if (result)
                            log(testName + " не пройден");
                        result = false;
                        log("Локомотив " + loco.id + " c поездом " + train.getId() + " на участке " +
                                track.stationFromId + " - " + track.stationToId +
                                " может везти поезд, не тяжелее, чем " + locoTonnage.getMaxWeight() +
                                ", а запланировано, что будет везти поезд массой " + train.getWeight());
                    }

                }
                train.getWeight();
            }
        }

        return result;
    }

}
