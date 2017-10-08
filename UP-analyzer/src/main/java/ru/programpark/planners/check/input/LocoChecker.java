package ru.programpark.planners.check.input;

/*
Реализован Раздел 2.3 документа "Автоматические тесты":  Набор тестов по локомотивам
1. Локомотивы переданы в планировщик
2. Отсутствие дублирования локомотивов
3. Для локомотивов заданы атрибуты
4. Для локомотива задано местоположение
5. Для локомотива задано время до ТО-2
6. Связь локомотива с поездом на участке
7. Связь локомотива с поездом на станции
8. Связь локомотива с бригадой
9. Наличие локомотивов на всех тяговых плечах
10.Покрытие локомотивами всех участков планирования
11. Не нулевые значения времен до ремонта
 */

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.TrainArrive;
import ru.programpark.entity.train.TrainDepart;
import ru.programpark.entity.train.TrainState;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;
import ru.programpark.planners.util.SetUtil;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocoChecker {
    private static double NON_ZERO_SERVICE_RELATION = 0.9;
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

    public static boolean checkLocoAttrInfoRecieved_2_3_1(InputData iData){// Локомотивы переданы в планировщик
        //Количество сообщений +loco_attributes должно быть больше 0.
        boolean pass = true;
        String testName = "Тест 2.3.1 (Локомотивы переданы в планировщик)";
        if (iData.locoAttrCount == 0){
            log(testName + " не пройден ");
            pass = false;
        }

        return pass;
    }

    public static boolean checkLocoAttrDuplicates_2_3_2(InputData iData){// Отсутствие дублирования локомотивов
        /*
        Нет сообщений loco_attributes, для которых указаны одинаковые id локомотивов.
        В случае ошибки выводить дублирующиеся id локомотивов.
         */
        boolean pass = true;
        String testName = "Тест 2.3.2 (Отсутствие дублирования локомотивов)";

        for(FactLoco loco: iData.getFactLocos().values()) {
            long check = loco.checkCorrectLocoAttr();
            if(check != 0) {
                String errorMessage = " нет данных об ошибке";
                if (check == -1) errorMessage = " дублируются сообщения loco_attributes";
                if (check == -2) errorMessage =  " не получено сообщение loco_attributes";
                log(testName + " не пройден. Для локомотива " + loco.getId() + " " + errorMessage);
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkLocoAttrDefined_2_3_3(InputData iData){// Для локомотивов заданы атрибуты
        /*
        Во всех сообщения loco_attributes присутствуют атрибуты: серия, депо, количество секций,
        как минимум одно тяговое плечо. В случае ошибки выводить id локомотивов, для которых наблюдается ошибка.
        */
        boolean pass = true;
        String testName = "Тест 2.3.3 (Для всех локомотивов заданы атрибуты)";

        for (FactLoco fLoco: iData.getFactLocos().values()){
            if (fLoco.getDepotStation() == null){
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении loco_attributes не задано депо: fLocoId = " + fLoco.getId() +
                        " депо " + fLoco.getDepotStation().getName());
                pass = false;
            }
            if (fLoco.getLocoRegions() == null || fLoco.getLocoRegions().size() == 0) {
                if (pass) {
                    log(testName + " не пройден ");
                }
                log("В сообщении loco_attributes не задано ни одного тягового плеча: fLocoId = " + fLoco.getId() +
                        " locoRegions " + fLoco.getLocoRegions());
                pass = false;
            }
            if (fLoco.getSeries() == 0L){
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении loco_attributes не задана серия: fLocoId = " + fLoco.getId() +
                        " серия " + fLoco.getSeries());
                pass = false;
            }
            if (fLoco.getNSections() == 0L) {
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении loco_attributes не задана секция: fLocoId = " + fLoco.getId() +
                        " секция " + fLoco.getNSections());
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkForEachLocoAttrOneFactLoco_2_3_4(InputData iData){
    //Для всех локомотивов задано местоположение
        /*
        Для каждого локомотива передано ровно одно сообщение fact_loco.
        В случае ошибки выводить id локомотивов, для которых наблюдается ошибка.
        */
        boolean pass = true;
        String testName = "Тест 2.3.4 (Для всех локомотивов задано местоположение)";

        for(FactLoco loco: iData.getFactLocos().values()) {
            long check = loco.checkCorrectFactLoco();
            if(check != 0) {
                String errorMessage = " нет данных об ошибке";
                if (check == -1) errorMessage = " дублируются сообщения fact_loco";
                if (check == -2) errorMessage =  " не получено сообщение fact_loco";
                log("Для локомотива " + loco.getId() + " " + errorMessage);
                if (pass)
                    log(testName + " не пройден ");
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkForEachLocoAttrOneFactLocoNextService_2_3_5(InputData iData){
    //  Для локомотива задано время до ТО-2
        /*
        Для каждого локомотива передано ровно одно сообщение fact_loco_next_service.
        В случае ошибки выводить id локомотивов, для которых наблюдается ошибка.
         */
        boolean pass = true;
        String testName = "Тест 2.3.5 (Для локомотива задано время до ТО-2)";

        /*if (iData.fLocoNextServiceCount != iData.getFactLocos().size()){
            log(testName + " не пройден ");
        } */

        for(FactLoco loco: iData.getFactLocos().values()) {
            long check = loco.checkCorrectNextService();
            if (check != 0) {
                String errorMessage = " нет данных об ошибке.";
                if (check == -1) errorMessage = " дублируются сообщения fact_loco_next_service";
                if (check == -2) errorMessage = " не получено сообщение fact_loco_next_service";
                log("Для локомотива " + loco.getId() + " " + errorMessage);
                if (pass)
                    log(testName + " не пройден ");
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkForFactLocoWithTrainExistsTrainDepart_2_3_6(InputData iData){
    // Связь локомотива с поездом на участке
        /*
        Если для локомотива в сообщении fact_loco задано местоположение на участке с поездом, то
        должно быть сообщение train_depart для этого поезда с тем же участком и тем же временем отправления.
        В случае ошибки выводить id локомотивов, для которых наблюдается ошибка. Также надо выводить тип ошибки:
        •	Вообще нет сообщения о местоположении поезда.
        •	Сообщение о местоположении поезда другого типа (train_arrive или train_ready).
        •	Поезд находится на другом участке.
        •	Поезд следует по этому же участку, но с другим временем отправления.
         */
        boolean pass = true;
        String testName = "Тест 2.3.6 (Связь локомотива с поездом на участке)";

        for (FactLoco fLoco: iData.getFactLocos().values()){
            if (fLoco.getTrack() != null){ //локомотив находится на перегоне
                Long trainId = fLoco.getTrack().getTrainId();
                if (trainId == null)
                    continue;
                FactTrain fTrain = iData.getFactTrains().get(trainId);
                if (fTrain == null) {
                    if (pass){
                        log(testName + " не пройден ");
                    }
                    log("Для fact_loco не найден соотв. train_fact: " + fLoco.toString());
                    pass = false;
                } else {
                    Long locoTime = fLoco.getTrack().getTimeDepart();
                    Long trainTime = fTrain.getTrainState().getTime();
                    TrainState state = fTrain.getTrainState();

                    if (!(state instanceof TrainDepart)){
                        if (pass){
                            log(testName + " не пройден ");
                        }
                        log("Для fact_loco " + fLoco.getId() + " найден соотв. train_fact " + fTrain.getId() +
                                " , но типы фактов не совпадают ");
                        pass = false;
                    } else {
                        TrainDepart trainDepart = (TrainDepart) state;
                        if (!trainDepart.getLink().equals(fLoco.getTrack().getLink())){
                            if (pass){
                                log(testName + " не пройден ");
                            }
                            log("Для fact_loco " + fLoco.getId() +" найден соотв. train_fact " + fTrain.getId() +
                                    " , но перегоны не совпадают " );
                            pass = false;
                        }
                        if (!locoTime.equals(trainTime)) {
                            if (pass){
                                log(testName + " не пройден ");
                            }
                            log("Для fact_loco " + fLoco.getId() + " найден соотв. train_fact " + fTrain.getId() +
                                    " , но времена не совпадают: у локо "
                                    + new Time(locoTime).getTimeStamp() + ", а у поезда " +
                                    new Time(trainTime).getTimeStamp());
                            pass = false;
                        }
                    }
                }
            }
        }

        return pass;
    }

    public static boolean checkForFactLocoWithTrainExistsTrainArrive_2_3_7(InputData iData){
    //Связь локомотива с поездом на станции
        /*
        Если для локомотива в сообщении fact_loco задано местоположение на станции с поездом, то должно быть
        сообщение train_arrive для этого поезда с той же станцией.
        В случае ошибки выводить id локомотивов, для которых наблюдается ошибка. Также выводить тип ошибки:
        •	Вообще нет сообщения о местоположении поезда.
        •	Сообщение о местоположении поезда другого типа (train_depart или train_ready).
        •	Поезд находится на другой станции.
         */

        boolean pass = true;
        String testName = "Тест 2.3.7 (Связь локомотива с поездом на станции)";

        for (FactLoco fLoco: iData.getFactLocos().values()){
            if (fLoco.getLocoArrive() != null) {//локомотив прибыл на станцию с поездом
                Long trainId = fLoco.getLocoArrive().getId();
                FactTrain fTrain = iData.getFactTrains().get(trainId);
                if (fTrain == null) {
                    if (pass){
                        log(testName + " не пройден ");
                    }
                    log("Для fact_loco " + fLoco.getId() + " не найден соотв. train_fact " + trainId);
                    pass = false;
                } else {
                    TrainState state = fTrain.getTrainState();
                    if (!(state instanceof TrainArrive)) {
                        if (pass){
                            log(testName + " не пройден ");
                        }
                        log("Для fact_loco " + fLoco.getId() + " найден соотв. train_fact " + trainId +
                                " , но типы фактов не совпадают ");
                        pass = false;
                    } else {
                        TrainArrive trainArrive = (TrainArrive) state;
                        if (!trainArrive.getLink().getTo().equals(fLoco.getStation())) {
                            if (pass){
                                log(testName + " не пройден ");
                            }
                            log("Для fact_loco " + fLoco.getId() + " найден соотв. train_fact " + trainId +
                                    " , но станции не совпадают: у локо " + fLoco.getStation().getName() +
                                    ", а у поезда - " + trainArrive.getLink().getTo().getName() );
                            pass = false;
                        }
                    }
                }
            }
        }

        return pass;

    }

    public static boolean checkForEachFactLocoExistsFactTeam_2_3_8(InputData iData){ //Связь локомотива с бригадой
        /*
        Если для локомотива в сообщении fact_loco задано местоположение на участке с поездом, то должен быть факт
        fact_team о бригаде, которая следует с этим локомотивом на этом же участке.
        Время отправления бригады должно совпадать со временем отправления локомотива.
        В случае ошибки выводить id локомотива, для которого ошибка наблюдается. Также надо выводить тип ошибки:
        •	Вообще нет сообщения о местоположении бригады на участке.
        •	Местоположение бригады отличается по типу (не на участке, а на станции с локомотивом или на
         станции без локомотива).
        •	Бригада находится на другом участке.
        •	Бригада находится на этом участке, но с другим временем отправления.
         */

        boolean pass = true;
        String testName = "Тест 2.3.8 (Связь локомотива с бригадой)";

        for (FactLoco fLoco: iData.getFactLocos().values()){
            if (fLoco.getTrack() != null){ //локомотив находится на перегоне
                FactTeam foundFactTeam = null;
                for (FactTeam fTeam: iData.getFactTeams().values()){
                    if (fTeam.getTrack()!= null     // added by Atakmaz - нужна проверка чтоб не падало
                            && fTeam.getTrack().getLocoId().equals(fLoco.getId())){
                        foundFactTeam = fTeam;
                        break;
                    }
                }
                if (foundFactTeam == null) {
                    if (pass){
                        log(testName + " не пройден ");
                    }
                    log("Для fact_loco " + fLoco.getId() +" не найден соотв. факт о бригаде ");
                    pass = false;
                } else {
                    if (!foundFactTeam.getTrack().getLocoId().equals(fLoco.getId())){
                        if (pass){
                            log(testName + " не пройден ");
                        }
                        log("Для fact_loco " + fLoco.getId() +" найден соотв. team_depart "  +
                                foundFactTeam.getId() + " , но типы фактов не совпадают ");
                        pass = false;
                    } else {
                        if (!fLoco.getTrack().getLink().equals(foundFactTeam.getTrack().getLink())) {
                            if (pass){
                                log(testName + " не пройден ");
                            }
                            log("Для fact_loco " + fLoco.getId() + " найден соотв. team_depart " +
                                    foundFactTeam.getId() +" , но перегоны не совпадают ");
                            pass = false;
                        }
                        Long locoTime = fLoco.getTrack().getTimeDepart();
                        Long teamTime = foundFactTeam.getTrack().getDepartTime();
                        if (!locoTime.equals(teamTime)) {
                            if (pass){
                                log(testName + " не пройден ");
                            }
                            log("Для fact_loco " + fLoco.getId() + " найден соотв. team_depart " +
                                    foundFactTeam.getId() +" , но времена не совпадают: у локо "
                                    + new Time(locoTime).getTimeStamp() + ", а у бригады - " +
                                    new Time(teamTime).getTimeStamp());
                            pass = false;
                        }
                    }
                }
            }
        }

        return pass;

    }

    public static boolean checkForEachLocoRegionExistsFactLoco_2_3_9(InputData iData){
    //Наличие локомотивов на всех тяговых плечах
        /*
        Для каждого тягового плеча должен присутствовать хотя бы один локомотив, принадлежащий этому тяговому плечу.
        В случае ошибки выводить id тяговых плеч, для которых не передано локомотивов, а также список станций,
        которые принадлежат этому тяговому плечу.
         */
        boolean pass = true;
        String testName = "Тест 2.3.9 (Наличие локомотивов на всех тяговых плечах)";

        for (LocoRegion region: iData.getLocoRegions().values()){
            boolean found = false;
            for (FactLoco fLoco: iData.getFactLocos().values()){
                if (fLoco.getLocoRegions() == null) {
                    log("Для factLoco " + fLoco.getId() +" не задано ни одного плеча ");
                    break;
                }
                if (fLoco.getLocoRegions().contains(region)){
                    found = true;
                    break;
                }
            }
            if (!found){
                if (pass){
                    log(testName + " не пройден");
                }
                log("Для loco_region " + region.getId() +" нет ни одного локомотива ");
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkLocoRegionCoverage_2_3_10(InputData iData){//Покрытие локомотивами всех участков план-я
        /*
        Для каждого участка планирования (link) составить список тяговых плеч, в которые входит этот участок план-я.
        Проверить, что хотя бы для одного из этих тяговых плеч
        (для каждого участка планирования) задан хотя бы один локомотив с таким тяговым плечом.
        В случае ошибки выводить участки планирования (начальную и конечную станции), для которых нет локомотивов.
         */
        boolean pass = true;
        String testName = "Тест 2.3.10 (Покрытие локомотивами всех участков планирования)";

        for (Link link: iData.getLinks().values()) {
            Set<LocoRegion> regionSetFrom = new HashSet<>();
            List<LocoRegion> listFrom = link.getFrom().getRegions();
            regionSetFrom.addAll(listFrom);
            Set<LocoRegion> regionSetTo = new HashSet<>();
            List<LocoRegion> listTo = link.getTo().getRegions();
            regionSetTo.addAll(listTo);
            Set<LocoRegion> set = SetUtil.intersection(regionSetFrom, regionSetTo);
            boolean found = false;
            locoRegion: for (LocoRegion region : set) {
                factLoco: for (FactLoco fLoco : iData.getFactLocos().values()) {
                        if (fLoco.getLocoRegions()!= null && fLoco.getLocoRegions().contains(region)) {
                            found = true;
                            break locoRegion;
                        }
                }
            }
            if (!found) {
                if (pass){
                    log(testName + " не пройден");
                }
                log("Для перегона " + link.getFrom().getName() + " - " + link.getTo().getName() +
                        " нет ни одного локомотива, который может на нем везти поезд ");
                pass = false;
                break;
            }
        }

        return pass;
    }

    public static boolean checkTimeToServiceToEqualToZero_2_3_11(InputData iData){//
        /*
         True если у 90% переданных локомотивов стоит ненулевое время до ремонта.
         */
        boolean pass = true;
        int good = 0;
        String testName = "Тест 2.3.11 (у " + NON_ZERO_SERVICE_RELATION +
                "% переданных локомотивов стоит ненулевое время до ремонта)";

        for (FactLoco fLoco: iData.getFactLocos().values()){
            if (fLoco.getTimeToService() > 0L){
                good++;
            }
        }

        if (good < NON_ZERO_SERVICE_RELATION*iData.getFactLocos().size()){
            log(testName + " не пройден ");
            pass = false;
        }

        return pass;
    }
}

