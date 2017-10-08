package ru.programpark.planners.check.input;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.util.SetUtil;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/*
Реализован Раздел 2.1 документа "Автоматические тесты":  Набор тестов для справочной информации
1. Количество станций
2. Станции смены бригад
3. Станции смены локомотивов
4. Покрытие тяговыми плечами
5. Покрытие участками обкатки бригад
 */
public class DictionaryChecker {
    private static double CHANGE_TEAM_RELATION = 0.1;
    private static double CHANGE_LOCO_RELATION = 0.05;
    private static double TEAM_CHANGE_MINIMUM_TIME = 900L; //15 min
    private static double LOCO_CHANGE_MINIMUM_TIME = 3600L;//1 hour
    private static PrintWriter writer = null;
    public static boolean beforeFilter = true;

    private static void log(String message) {
        if (!beforeFilter){
            writer = LoggingAssistant.getInputDataTestAfterFilterDetailsWriter();
        }
        if (writer == null)
            writer = LoggingAssistant.getInputDataTestBeforeFilterDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }


    public static boolean checkStationQuantity_2_1_1(InputData input) {  // Положительное количество станций
        //Количество объектов station не совпадает количеству пришедших сообщений +station
        boolean pass = true;
        String testName = "Тест 2.1.1 (Положительное количество станций)";
        if (input.stationCount == 0) {
            log(testName + " не пройден");
            pass = false;   // Количество сообщений +station должно быть больше 0.
        }

        return pass;
    }

    public static boolean checkStationChangeTeam_2_1_2(InputData input) { //Задано достаточно станций смены бригад
        //Количество станций, для которых значение атрибута norm_time больше 0, должно быть не меньше 10% от общего количества станций.*/
        int changeTeamStationQuantity = 0;
        boolean pass = true;
        String testName = "Тест 2.1.2 (Задано достаточно станций смены бригад: минимум " + CHANGE_TEAM_RELATION + " от общего числа станций )";
        for (Station station : input.getStations().values()) {
            if (station.getNormTime() > 0)
                changeTeamStationQuantity++;
        }
        if (input.uniqueStations.size()== 0 || 1.0*changeTeamStationQuantity / input.uniqueStations.size() < CHANGE_TEAM_RELATION) {
            log(testName + " не пройден");
            pass = false;
        }

        return pass;
    }

    public static boolean checkStationChangeLoco_2_1_3(InputData input) {  //Задано достаточно станций смены локомотивов
        //Количество станций, для которых задан process_time больше 0, должно быть не меньше 5% от общего количества станций.
        int changeLocoStationQuantity = 0;
        boolean pass = true;
        String testName = "Тест 2.1.3 (Задано достаточно станций смены локомотивов: минимум " + CHANGE_LOCO_RELATION + " от общего числа станций)";

        for (Station station : input.getStations().values()) {
            if (station.getProcessTime() > 0)
                changeLocoStationQuantity++;
        }

        if (input.uniqueStations.size() <=0 || (1.0*changeLocoStationQuantity /input.uniqueStations.size()) < CHANGE_LOCO_RELATION){
            pass = false;
            log(testName + " не пройден");
        }
        return pass;
    }

    public static boolean checkLocoRegion_2_1_4(InputData input) { //Покрытие перегонов тяговыми плечами
        //Для каждого участка планирования (link) должно быть тяговое плечо, в которое входят обе граничные станции участка (то есть присутствуют сообщения
        // +station для граничных станций участка, в которых указан один и тот же loco_region).
        /*
        - Проход по всем участкам планирования, добавляя их в  set<Link>
        - Проверка, что для каждого link пересечение множеств тяговых плеч начальной и конечной станций непустое.
        Если есть пустое множество – вывод сообщения
         */
        boolean pass = true;
        String testName = "Тест 2.1.4 (Покрытие перегонов тяговыми плечами)";

        for (Link link : input.getLinks().values()) {
            Set<LocoRegion> locoRegionsFrom = new HashSet();
            locoRegionsFrom.addAll(link.getFrom().getRegions());
            Set<LocoRegion> locoRegionsTo = new HashSet();
            locoRegionsTo.addAll(link.getTo().getRegions());
            Set<LocoRegion> U = new HashSet<>();
            U = SetUtil.intersection(locoRegionsFrom, locoRegionsTo);
            if (U.size() == 0) {
                if (pass)
                    log(testName + " не пройден");
                pass = false;
                log("Для станций перегона " + link.getFrom().getName() + " - " + link.getTo().getName() + " нет ни одного общего тягового плеча");
            }
        }
        return pass;
    }

    public static boolean checkTeamWorkRegion_2_1_5(InputData input) {  // Покрытие перегонов участками обкатки бригад
        //Для каждого участка планирования (link) должен быть участок обкатки бригад, куда входит этот участок планирования
        // (то есть должно быть сообщение team_work_region, в атрибуте tracks которого указан данный участок).
        boolean pass = true;
        String testName = "Тест 2.1.5 (Покрытие перегонов участками обкатки бригад)";

        for (Link link : input.getLinks().values()) {
            boolean foundTeamWorkRegion = false;
            if(link.getFrom().getId().equals(link.getTo().getId()))
                foundTeamWorkRegion = true;
            else
                exit: for (TeamRegion teamRegion : input.getTeamWorkRegions().values()) {
                    for (StationPair pair : teamRegion.getStationPairs()) {
                        if (pair.equals(new StationPair(link))) {
                            foundTeamWorkRegion = true;
                            break exit;
                        }
                    }
                }
            if (!foundTeamWorkRegion) {
                if (pass)
                    log(testName + " не пройден");
                String msg = "Для участка " + link.getFrom().getId() + " - " + link.getTo().getId() + " нет сообщения team_work_region, в атрибуте tracks которого указан данный участок";
                log(msg);
                pass = false;
            }
        }
        return pass;
    }

    public static boolean checkStopLengthForLocoChange_2_1_6(InputData input){
        boolean pass = true;
        String testName = "Тест 2.1.6 (Длительность стоянки под смену локомотива на всех станция смены локо >= "+ LOCO_CHANGE_MINIMUM_TIME + ")";

        for (Station s: input.getStations().values()){
            if (s.getProcessTime() > 0L){//является станцией смены локо
                if (s.getProcessTime() < LOCO_CHANGE_MINIMUM_TIME){
                    if (pass)
                        log(testName + " не пройден");
                    pass = false;
                    log("У станции " + s.getName() + " передано время смены локо, меньшее необходимого минимума. Передано " + s.getProcessTime() + ", а минимум - " + LOCO_CHANGE_MINIMUM_TIME);
                }
            }
        }

        return pass;
    }

    public static boolean checkStopLengthForTeamChange_2_1_7(InputData input){
        boolean pass = true;
        String testName = "Тест 2.1.7 (Длительность стоянки под смену ЛБ на всех станциях смены ЛБ >= "+ TEAM_CHANGE_MINIMUM_TIME + ")";

        for (Station s: input.getStations().values()){
            if (s.getNormTime() > 0L){//является станцией смены ЛБ
                if (s.getNormTime() < TEAM_CHANGE_MINIMUM_TIME){
                    if (pass)
                        log(testName + " не пройден");
                    pass = false;
                    log("У станции " + s.getName() + " передано время смены ЛБ, меньшее необходимого минимума. Передано " + s.getNormTime() + ", а минимум - " + TEAM_CHANGE_MINIMUM_TIME);
                }
            }
        }

        return pass;
    }
}
