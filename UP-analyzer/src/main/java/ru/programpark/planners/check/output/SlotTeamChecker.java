package ru.programpark.planners.check.output;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.raw_entities.SlotTrain;
import ru.programpark.entity.team.BaseTeamTrack;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.TrainCategory;
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
public class SlotTeamChecker {
    private static double TEAM_CHANGE_MINIMUM_TIME = 900L; //15 min
    public static long MAX_TEAM_STAY = 3 * 3600L;
    private static PrintWriter writer = null;
    private static NumberFormat formatter = new DecimalFormat("#0.00");

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }
    public static boolean checkLongStayForTeamChange_3_1_6(Collection<SlotTrain> trains, Collection<SlotLoco> locos, Collection<SlotTeam> teams) { //3.1.6  (Долгое время стоянок поездов на станциях смены бригад)";
        String testName = "Тест 3.1.6  (Долгое время стоянок (>" + MAX_TEAM_STAY + ") поездов на станциях смены бригад)";
        boolean pass = true;

        for (SlotTrain train : trains) {
            SlotLoco priorSlotLoco = null;
            SlotTeam priorSlotTeam = null;
            BaseTrack priorTrack = null;
            for (BaseTrack track : train.route) {
                SlotLoco loco = AnalyzeHelper.getSlotLocoBySlotTrainAndTrack(train, track, locos);
                SlotTeam team = AnalyzeHelper.getSlotTeamBySlotTrainAndTrack(train, track, locos, teams);
                if (loco!= null && team != null && priorSlotTeam != null) {
                    if (priorSlotLoco.id.equals(loco.id) && !priorSlotTeam.id.equals(team.id)) {
                        if (track.timeStart - priorTrack.timeEnd > MAX_TEAM_STAY) {
                            if (pass)
                                log(testName + " не пройден ");
                            pass = false;
                            log("Долгое время стоянки на смену бригады у поезда " + train.id
                                    + " c бригадой " + team.id + " на станции " + track.stationFromId
                                    + ". Поезд: приехал в " + new Time(priorTrack.timeEnd).getTimeStamp() + ", а уехал в " +
                                    new Time(track.timeStart).getTimeStamp() + ". Всего ушло на смену - " + formatter.format((track.timeStart - priorTrack.timeEnd)/3600.0) + " ч");
                        }
                    }
                }
                priorSlotLoco = loco;
                priorSlotTeam = team;
                priorTrack = track;
            }
        }

        return pass;
    }


    public static boolean checkEarlyTeam_3_1_9(InputData input, Collection<SlotTeam> teams) { //3.1.9    Отправление бригады раньше времени явки
        boolean pass = true;
        String testName = "Тест 3.1.9 (Отправление бригады раньше времени явки)";

        for (SlotTeam team : teams) {
            long factTime;
            FactTeam factTeam = input.getFactTeams().get(team.id);
            if (factTeam.getTrack() != null) {
                factTime = factTeam.getTrack().getDepartTime();
            } else if (factTeam.getTeamArrive() != null) {
                factTime = factTeam.getTeamArrive().getTime();
            } else {
                factTime = factTeam.getTimeOfFact();
            }

            if (team.route.size() > 0) {
                if (factTime > team.route.get(0).timeStart) {
                    if (pass)
                        log (testName + "не пройден");
                    pass = false;
                    log("Бригада " + team.id + " назначена в " + new Time(team.route.get(0).timeStart).getTimeStamp() + ", а время явки " + new Time(factTime).getTimeStamp());
                }
            }
        }

        return pass;
    }

    public static boolean allTeamsAsPassHaveLocoIdsOrSlotIdsSet_3_1_14(Collection<SlotTeam> teams){//3.1.14
        /* Для всех бригад пассажирами проставлена нитка пассажирского поезда или номер локомотива в случае отправки с грузовым поездом.
        Цикл по slotTeams
        Цикл по трекам
        Если по треку бригада следует пассажиром, то проверяем что проставлено либо slotId (пассажирская нитка), либо locoId (локомотив, который тянет грузовой поезд).
        */

        boolean pass = true;
        String testName = "Тест 3.1.14 (Для всех бригад пассажирами проставлена нитка пассажирского поезда или номер локомотива в случае отправки с грузовым поездом)";

        for (SlotTeam team: teams){
            for (SlotTeam.Track track: team.route) {
                if (track.state.equals(BaseTeamTrack.State.PASSENGER.ordinal())) {//следует пассажиром
                    if (track.locoId == -1L && track.slotId == -1L) {
                        if (pass)
                            log (testName + "не пройден");
                        pass = false;
                        String message = "Для бригады " + team.id + " не указан способ пересылки пассажиром, отсутствует и slotId и locoId: " + track.toString();
                        log(message);
                    }
                    if (track.locoId != -1L && track.slotId != -1L) {
                        if (pass)
                            log (testName + "не пройден");
                        pass = false;
                        String message = "Для бригады " + team.id + " двусмысленно указан способ пересылки пассажиром, задан и slotId и locoId: " + track.toString();
                        log(message);
                    }
                }
            }
        }

        return pass;
    }

    public static boolean AllUnassignedTeamsHaveWorklessTeamMessage_3_1_15(InputData input, Map<Long, SlotTeam> teams, Map<Long, FactTeam> worklessTeams){//3.1.15
        //Всем не назначенным бригадам соответствуют сообщения workless_team.
        //Идем по factTeams, проверяет нахождение бригады с id в slotTeams или worklessTeams. Если находим бригаду, которой нет ни там, ни там, то выводим сообщение.
        boolean pass = true;
        String testName = "Тест 3.1.15 (Всем не назначенным бригадам соответствуют сообщения workless_team)";

        for (FactTeam fTeam: input.getFactTeams().values()){
            if (teams.get(fTeam.getId()) == null && (!worklessTeams.containsKey(fTeam.getId()))){
                if (pass)
                    log (testName + "не пройден");
                pass = false;
                log("Фактическая бригада с id " + fTeam.getId() + " не привязана и для нее отсутствует сообщение workless_team");
            }
        }

        return pass;
    }

    public static boolean checkTeamPercent_3_1_16(InputData input, Map<Long, SlotTeam> slotTeams, TeamRegion region){ //3_1_16
        /*
         Посчитать общее количество запланированных поездов. Найти бригаду, которая едет с каждым поездом, определить для этой бригады депо приписки.
         Вычислить процент поездов, с которыми едет бригада с депо приписки Тайшет (2000036518) и с депо приписки Вихоревка (2000036796).
         Проверить, что процент тайшетских бригад лежит в диапазоне от 20 до 40, а процент вихоревских – от 60 до 80.
         В случае ошибки вывести фактически процент тайшетских и вихоревских бригад.
         */
        boolean pass = true;
        String testName = "Тест 3.1.16 (Процент заезда в каждом депо участка на каждом участке отличается от ожидаемого не более, чем на 10%)";
        List<Station> depots = new ArrayList<>(region.getPercentByDepot().keySet());
        Station from = depots.get(0);
        Station to = depots.get(1);
        Map<Station, Long> percentForDepot = new HashMap();
        Map<Station, Long> sentByDepot = new HashMap();
        List<SlotTeam> teams = narrowDownTeamForObserveLink(from.getId(), to.getId(), slotTeams);
        Integer allSent = teams.size();

        for (Station depot: depots){
            percentForDepot.put(depot, 0L);
            sentByDepot.put(depot, 0L);
        }

        for (SlotTeam team: teams){
            FactTeam factTeam = input.getFactTeams().get(team.id);
            Station depot = factTeam.getDepot();
            if (sentByDepot.get(depot) != null){
                long temp = sentByDepot.get(depot);
                sentByDepot.put(depot, ++temp);
            }
        }

        for (Station depot: depots){
            percentForDepot.put(depot, Math.round(sentByDepot.get(depot)*100.0/allSent));
        }

        if (percentForDepot.get(from) - region.getPercentByDepot().get(from) > 15.0 || percentForDepot.get(to) - region.getPercentByDepot().get(to) > 15.0) {
            if (pass)
                log(testName + " не пройден ");
            pass = false;
            log("Для teamServiceRegion " + region.getId() + " " + depots.get(0).getName() + "-"+ depots.get(1).getName() +" не выполняются условия по процентам заезда." +
                    percentForDepot.get(from) + " вместо ожидаемых " + region.getPercentByDepot().get(from) + " и " + percentForDepot.get(to) + " вместо ожидаемых " + region.getPercentByDepot().get(to));
        }

        return pass;
    }

    private static ArrayList<SlotTeam> narrowDownTeamForObserveLink(Long stFrom, Long stTo, Map<Long, SlotTeam> slotTeams) {
        ArrayList<SlotTeam> foundTeams = new ArrayList<>();

        for (SlotTeam team : slotTeams.values()) {
            if (team.passesStation(stFrom) && team.passesStation(stTo))
                foundTeams.add(team);
        }
        return foundTeams;
    }

    public static boolean checkTeamsDoNotExceedWorkingTimeLimit_3_1_17(Collection<SlotTeam> teams){  //Не превышается рабочее время бригад
        boolean pass = true;
        String testName = "Тест 3.1.17 (Не превышается рабочее время бригад)";

        for (SlotTeam team : teams) {
            boolean process = false;
            Long stationStart = 0L;
            Long stationStop = 0L;
            Long timeStart = 0L;
            Long timeEnd = 0L;
            SlotTeam.Track priorTrack = null;
            for (SlotTeam.Track track : team.route) {
                if (!process && track.state == 1) {
                    timeStart = track.timeStart;
                    stationStart = track.stationFromId;
                    process = true;
                }
                if (priorTrack != null) {
                    if (process) {
                        if (track.state == 4 || track.equals(team.route.get(team.route.size() - 1))) {
                            timeEnd = priorTrack.timeEnd;
                            stationStop = priorTrack.stationToId;
                            process = false;
                        }
                    }
                }

                if (!process && stationStart != 0L && stationStop != 0L) {
                    if (timeEnd - timeStart > (long)(10 * 3600)) {
                        if (pass)
                            log(testName + " не пройден ");
                        pass = false;
                        String message =  "Бригада  " + team.id + " на участке " + stationStart + " - " + stationStop + " отправилась в " +
                                            new Time(timeStart).getTimeStamp() + ", а прибыла в " + new Time(timeEnd).getTimeStamp() + ", чем превысила время работы. Время в пути составило "
                                            + (timeEnd - timeStart);
                        log(message);
                    }
                    stationStart = 0L;
                    stationStop = 0L;
                }
                priorTrack = track;
            }
        }

        return pass;
    }

    public static boolean checkStopLengthForLocoChange_3_1_25(InputData input, Map<Long, SlotTrain> slotTrains){//3.1.25
        //Для каждого slotTrain проверяется, что если он проходит станцию смены ЛБ, то стоянка там больше необходимого минимума для смены ЛБ.
        boolean pass = true;
        String testName = "Тест 3.1.25  (Стоянки на станциях смены ЛБ больше " + TEAM_CHANGE_MINIMUM_TIME + ")";
        SlotTrain.Track prevTrack = null;

        for (SlotTrain train: slotTrains.values()) {
            for (SlotTrain.Track track: train.route) {
                if (prevTrack == null)
                    continue;
                Station s = input.getStationById(track.stationFromId);
                Long stopTime = prevTrack.timeEnd - track.timeStart;
                if (s.getNormTime() > 0L) {//является станцией смены ЛБ
                    if (stopTime < TEAM_CHANGE_MINIMUM_TIME) {
                        if (pass)
                            log(testName + " не пройден");
                        pass = false;
                        FactTrain fTrain = input.getFactTrains().get(train.id);
                        TrainCategory cat = input.getTrainCategories().get(fTrain.getCategory());
                        int priority = -1;
                        if (cat != null)
                            priority = cat.getPriority();
                        log("Стоянка на станции " + s.getName() + " меньше необходимого минимума для смены ЛБ " + TEAM_CHANGE_MINIMUM_TIME +
                                ". Передано время смены ЛБ " + s.getNormTime() + ", а стоянка там - " + stopTime + ". Приоритет поезда - " + priority +
                                "(стоянка м.б. 1/2 от времени для смены бригады для приоритетных поездов)");
                    }
                }
                prevTrack = track;
            }
        }

        return pass;
    }

    public static boolean checkTeamsWorkWhereTheyAreAllowed_3_1_28(InputData iData, Map<Long, SlotTeam> sTeams){ //3.1.28 Все назначенные бригады работают на разрешенных участках
        /*
        Цикл по slotTeam
        Цикл по трекам
        Проверяем что трек входит в хоть один teamWorkRegion из соотв. factTeam
        Если нет - тест не пройден, выводим сообщение, что такая-то бригада назначена на такой-то трек, который отсутствует в наборе разрешенных.
        */
        boolean pass = true;
        String testName = "Тест 3.1.28  (Все назначенные бригады работают на разрешенных участках)";

        slotTeam: for (SlotTeam team: sTeams.values()) {
            track: for (SlotTeam.Track track: team.route) {
                if (track.state != BaseTeamTrack.State.AT_WORK.ordinal())
                    continue;
                FactTeam fTeam = iData.getFactTeamById(team.id);
                boolean linkInRegion = false;
                region: for (TeamRegion region: fTeam.getTeamWorkRegions()){
                    Link link = iData.getLinkByStationPair(new StationPair(track.stationFromId, track.stationToId));
                    if (region.containsLink(link)){
                        linkInRegion = true;
                        break region;
                    }
                }
                if (!linkInRegion) {
                    if (pass) {
                        log(testName + " не пройден");
                    }
                    pass = false;
                    log("Бригада " + team.id + " назначена на участок " + track.stationFromId + " - " + track.stationToId + ", но участок не входит ни в один TeamWorkRegion из списка FactTeam");
                }
            }
        }

        return  pass;
    }

    public static boolean checkAllPassTeamsSentWithTrainLocoAndTeam_3_1_30(Map<Long, SlotTeam> sTeams,
                                                                           Map<Long, SlotLoco> sLocos,
                                                                           Map<Long, SlotTrain> sTrains){
        boolean noTeamsSentWithTrainWithNoLoco = true;
        Set<Long> teamIds = new HashSet<>();
        String testName = "Тест 3.1.30  (Все бригады, пересылаемые пассажиром на грузовой нитке, едут с поездом, " +
                "локомотивом и бригадой)";

        for (SlotTeam team: sTeams.values()){
            for (SlotTeam.Track teamTrack : team.route){
                if (teamTrack.state.equals(BaseTeamTrack.State.PASSENGER.ordinal())){
                    SlotLoco loco = sLocos.get(teamTrack.locoId);
                    if (loco == null) {
                        noTeamsSentWithTrainWithNoLoco = false;
                        teamIds.add(team.id);
                    }

                    boolean locoFound = false;
                    boolean locoAssigned = false;
                    for (SlotLoco.Track locoTrack: loco.route){
                        if (locoTrack.stationFromId.equals(teamTrack.stationFromId) &&
                                locoTrack.stationToId.equals(teamTrack.stationToId) &&
                                locoTrack.timeStart.equals(teamTrack.timeStart) &&
                                locoTrack.timeEnd.equals(teamTrack.timeEnd)){
                            locoFound = true;
                            if ((locoFound) &&
                                    (!locoTrack.trainId.equals(-1L) && sTrains.get(locoTrack.trainId) != null)) {
                                locoAssigned = true;
                                break;
                            }
                        }
                    }

                    boolean teamDriverFound = false;

                    for (SlotTeam teamDriver: sTeams.values()) {
                        for (SlotTeam.Track teamDriverTrack : teamDriver.route) {
                            if (teamDriverTrack.locoId.equals(loco.id)) {
                                if (teamTrack.stationFromId.equals(teamDriverTrack.stationFromId) &&
                                        teamTrack.stationToId.equals(teamDriverTrack.stationToId) &&
                                        teamTrack.timeStart.equals(teamDriverTrack.timeStart) &&
                                        teamTrack.timeEnd.equals(teamDriverTrack.timeEnd)) {
                                    teamDriverFound = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!(locoFound && locoAssigned && teamDriverFound)) {
                        noTeamsSentWithTrainWithNoLoco = false;
                        teamIds.add(team.id);
                    }
                }
            }
        }

        if (!noTeamsSentWithTrainWithNoLoco) {
            log(testName + " не пройден");
            log(teamIds.toString());
        }

        return noTeamsSentWithTrainWithNoLoco;
    }
}
