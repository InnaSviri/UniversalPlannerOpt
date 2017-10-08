package ru.programpark.planners.check.input;

/*
Реализован Раздел 2.4 документа "Автоматические тесты":  Набор тестов по бригадам
1. Бригады переданы в планировщик
2. Отсутствие дублирования бригад
3. Для бригад заданы необходимые атрибуты
4. Для бригады задано местоположение
5. Для бригады задано оставшееся рабочее время
6. Связь бригады с локомотивом на участке
7. Связь бригады с локомотивом на станции
8. Наличие бригад на всех участках обкатки
9. Покрытие бригадами всех участков планирования
 */


import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class TeamChecker {
    private static PrintWriter writer = null;
    public static boolean beforeFilter = true;

    private static void log(String message) {
        if (!beforeFilter)
            writer = LoggingAssistant.getInputDataTestAfterFilterDetailsWriter();
        if (writer == null)
            writer = LoggingAssistant.getInputDataTestBeforeFilterDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    public static boolean checkTeamAttrInfoRecieved_2_4_1(InputData iData){ //  Бригады переданы в планировщик
        //Количество сообщений +team_attributes должно быть больше 0
        boolean pass = true;
        String testName = "Тест 2.4.1 (Бригады переданы в планировщик)";
        if (iData.teamAttrCount == 0) {
            log(testName + " не пройден");
            pass = false;
        }
        return pass;
    }

    public static boolean checkTeamAttrDuplicates_2_4_2(InputData iData){  //Отсутствие дублирования бригад
        /*
        Нет сообщений team_attributes, для которых указаны одинаковые id бригад.
        В случае ошибки выводить дублирующиеся id бригад.   //
        */
        boolean pass = true;
        String testName = "Тест 2.4.2 (Отсутствие дублирования бригад)";

        for(FactTeam team: iData.getFactTeams().values()) {
            long check = team.checkCorrectTeamAttr();
            if(check != 0) {
                if (pass)
                    log(testName + " не пройден");
                String errorMessage = " нет данных об ошибке";
                if (check == -1) errorMessage = " дублируются сообщения team_attributes";
                if (check == -2) errorMessage =  " не получено сообщение team_attributes";
                log(testName + "Для бригады " + team.getId() + " " + errorMessage);
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkTeamAttrContainsInfo_2_4_3(InputData iData){ //Для бригад заданы необходимые атрибуты
        /*
        Во всех сообщениях team_attributes присутствуют атрибуты: депо приписки, хотя бы один участок обкатки, который передан в сообщении team_work_region.
        В случае ошибки выводить id бригад, для которых наблюдается ошибка.
         */
        boolean pass = true;
        String testName = "Тест 2.4.3 (Для всех бригад заданы необходимые атрибуты)";

        for (FactTeam fTeam: iData.getFactTeams().values()){
            if (fTeam.getDepot() == null){
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении team_attributes не задано депо приписки. У бригады " + fTeam.getId() + " депо " + fTeam.getDepot().getName());
                pass = false;
            }
            if (fTeam.getTeamWorkRegions().size() < 1){
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении team_attributes не задано ни одного участка обкатки teamWorkRegion. У бригады " + fTeam.getId() + " teamWorkRegions " + fTeam.getTeamWorkRegions());
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkForEachTeamAttrOneFactTeam_2_4_4(InputData iData){ //Для бригады задано местоположение
        /*
        Для каждой бригады передано ровно одно сообщение fact_team.
        В случае ошибки выводить id бригад, для которых наблюдается ошибка.
         */

        boolean pass = true;
        String testName = "Тест 2.4.4 (Для каждой бригады передано ровно одно сообщение fact_team)";

        if (iData.teamAttrCount != iData.getFactTeams().size()){
            log(testName + " не пройден ");
        }

        for(FactTeam team: iData.getFactTeams().values()) {
            long check = team.checkCorrectFactTeam();
            if(check != 0) {
                String errorMessage = " нет данных об ошибке";
                if (check == -1) errorMessage = " дублируются сообщения fact_team";
                if (check == -2) errorMessage =  " не получено сообщение fact_team";
                log(testName + " не пройден. Для бригады " + team.getId() + " " + errorMessage);
                pass = false;
            }
        }

        return pass; 
    }

    public static boolean checkForEachTeamAttrExistsFactTeamNextRest_2_4_5(InputData iData){//Для кажой бригады задано оставшееся рабочее время
        /*
        Для каждой бригады передано ровно одно сообщение fact_team_next_rest.
        В случае ошибки выводить id бригад, для которых наблюдается ошибка.
        */
        boolean pass = true;
        String testName = "Тест 2.4.5 (Для каждой бригады задано оставшееся рабочее время)";

        for(FactTeam team: iData.getFactTeams().values()) {
            long check = team.checkCorrectNextRest();
            if(check != 0) {
                if (pass)
                    log(testName + " не пройден ");
                String errorMessage = " нет данных об ошибке";
                if (check == -1) errorMessage = " дублируются сообщения fact_team_next_rest";
                if (check == -2) errorMessage =  " не получено сообщение fact_team_next_rest";
                log("Для бригады " + team.getId() + " " + errorMessage);
                pass = false;
            }
        }
        return pass;
    }

    public static boolean checkForFactTeamOnLinkExistsFactLoco_2_4_6(InputData iData){ // Связь бригады с локомотивом на участке
        /*
        Если для бригады в сообщении fact_team задано местоположение с локомотивом на участке, то должно быть сообщение
        о местоположении этого локомотива на этом же участке с таким же временем отправления.
        В случае ошибки выводить id бригад, для которых наблюдается ошибка. Также надо выводить тип ошибки:
        •	Вообще нет сообщения о местоположении локомотива.
        •	Сообщение о местоположении другого типа (на станции с поездом или на станции без поезда);
        •	Для локомотива передан другой участок.
        •	Для локомотива передан тот же участок, но другое время отправления.
         */
        boolean pass = true;
        String testName = "Тест 2.4.6 (Связь бригады с локомотивом на участке)";

        for (FactTeam fTeam: iData.getFactTeams().values()){
            if (fTeam.getTrack() != null){ //бригада находится на перегоне
                Long locoId = fTeam.getTrack().getLocoId();
                FactLoco fLoco = iData.getFactLocos().get(locoId);
                if (fLoco == null) {
                    if (pass){
                        log(testName + " не пройден ");
                    }
                    log("Для fact_team " + fTeam.getId() + "не найден соотв. fact_loco " + locoId);
                    pass = false;
                } else {
                    Long teamTime = fTeam.getTrack().getDepartTime();
                    if (fLoco.getTrack() == null){
                        if (pass){
                            log(testName + " не пройден ");
                        }
                        log("Для fact_team " + fTeam.getId() + " найден соотв. fact_loco " + fLoco.getId() + ", но типы фактов не совпадают");
                        pass = false;
                    } else {
                        if (!fLoco.getTrack().getLink().equals(fTeam.getTrack().getLink())){
                            if (pass){
                                log(testName + " не пройден ");
                            }
                            log("Для fact_team " + fTeam.getId() + "найден соотв. fact_loco " + fLoco.getId() + ", но перегоны не совпадают: "
                                    + " перегон у бригады " + fTeam.getTrack().getLink().getFrom().getName()
                                    + " - " + fTeam.getTrack().getLink().getTo().getName()
                                    + ", перегон у локо " + fLoco.getTrack().getLink().getFrom().getName()
                                    + ", " + fLoco.getTrack().getLink().getTo().getName());
                            pass = false;
                        }
                        Long locoTime = fLoco.getTrack().getTimeDepart();
                        if (!locoTime.equals(teamTime)) {
                            if (pass){
                                log(testName + " не пройден ");
                            }
                            log("Для fact_team " + fTeam.getId() + " найден соотв. fact_loco " + fLoco.getId() +", но времена не совпадают: время у бригады" +
                                    new Time(teamTime).getTimeStamp() + ", а у локо - " + new Time(locoTime).getTimeStamp());
                            pass = false;
                        }
                    }
                }
            }
        }

        return pass;
    }

    public static boolean checkForFactTeamOnStationExistsFactLoco_2_4_7(InputData iData){ // Связь бригады с локомотивом на станции
        /*
        Если для бригады в сообщении fact_team задано местоположение с локомотивом на станции, то должно быть сообщение
        о местоположении этого локомотива на этой же станции.
        В случае ошибки выводить id бригад, для которых наблюдается ошибка. Также надо выводить тип ошибки:
        •	Вообще нет сообщения о местоположении локомотива.
        •	Сообщение о местоположении другого типа (на участке, а не на станции).
        •	Для локомотива передана другая станция.
         */
        boolean pass = true;
        String testName = "Тест 2.4.7 (Связь бригады с локомотивом на станции)";

        for (FactTeam fTeam: iData.getFactTeams().values()){
            if (fTeam.getTeamArrive() != null){ //локомотив находится на станции
                Long locoId = fTeam.getTeamArrive().getId();
                FactLoco fLoco = iData.getFactLocos().get(locoId);
                if (fLoco == null) {
                    if (pass){
                        log(testName + " не пройден ");
                    }
                    log("Для fact_team " + fTeam.getId() +" не найден соотв. fact_loco " + locoId);
                    pass = false;
                } else {
                    if (fLoco.getStation() == null){
                        if (pass){
                            log(testName + " не пройден ");
                        }
                        log("Для fact_team " + fTeam.getId() +" найден соотв. fact_loco " + fLoco.getId()+ ", но типы фактов не совпадают");
                        pass = false;
                    } else {
                        if (fLoco.getStation().getId().equals(fTeam.getTeamArrive().getId())){
                            if (pass){
                                log(testName + " не пройден ");
                            }
                            log("Для fact_team " + fTeam.getId() + " найден соотв. fact_loco " +fLoco.getId()+ ", но станции не совпадают: у бригады " +
                                    fTeam.getTeamArrive().getId() + ", а у локо - " + fLoco.getStation().getId());
                            pass = false;
                        }
                    }
                }
            }
        }

        return pass;
    }

    public static boolean checkForEachTeamWorkRegionExistsTeam_2_4_8(InputData iData){//
        /*
        Для каждого участка обкатки (team_work_region) должна присутствовать хотя бы одна бригада, для которой задан этот участок обкатки.
        В случае ошибки выводить id участков обкатки, для которых нет бригад.
         */
        boolean pass = true;
        String testName = "Тест 2.4.8 (Наличие бригад на всех участках обкатки)";

        for (TeamRegion region: iData.getTeamWorkRegions().values()){
            boolean found = false;
            for (FactTeam fTeam: iData.getFactTeams().values()){
                if (fTeam.getTeamWorkRegions().contains(region)){
                    found = true;
                    break;
                }
            }
            if (!found){
                if (pass){
                    log(testName + " не пройден");
                }
                log("Для team_work_region " + region.getId() + " нет ни одной бригады ");
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkTeamWorkRegionConsistency_2_4_9(InputData iData){  //Покрытие бригадами всех участков планирования
        /*
        Для каждого участка планирования (link) составить список участков обкатки, в которые входит этот участок планирования. Проверить, что хотя бы для одного из этих участков обкатки (для каждого участка планирования)
        задана хотя бы одна бригада с таким участком обкатки.
        В случае ошибки выводить участки планирования (начальную и конечную станцию), для которых нет бригад.
         */
        boolean pass = true;
        String testName = "Тест 2.4.9 (Покрытие бригадами всех участков планирования)";
        for (Link link: iData.getLinks().values()) {
            Set<TeamRegion> set = getTeamWorkRegionsForLink(link, iData);
            boolean found = false;
            teamRegion: for (TeamRegion region : set) {
                factTeam: for (FactTeam fTeam : iData.getFactTeams().values()) {
                    if (fTeam.getTeamWorkRegions().contains(region)) {
                        found = true;
                        break teamRegion;
                    }
                }
            }
            if (!found) {
                if (pass){
                    log(testName + " не пройден");
                }
                log("Для перегона " + link.getFrom().getName() + " - " + link.getTo().getName()  + " нет ни одной бригады, которая могла бы на нем везти локомотив ");
                pass = false;
                break;
            }
        }

        return pass;
    }

    private static Set<TeamRegion> getTeamWorkRegionsForLink(Link link, InputData iData){
        Set<TeamRegion> set = new HashSet<TeamRegion>();

        for (TeamRegion region: iData.getTeamWorkRegions().values()){
            for (StationPair sp: region.getStationPairs()){
                if (sp.stationFromId.equals(link.getFrom().getId()) && sp.stationToId.equals(link.getTo().getId())){
                    set.add(region);
                }
                if (sp.stationFromId.equals(link.getTo().getId()) && sp.stationToId.equals(link.getFrom().getId())){
                    set.add(region);
                }
            }
        }

        return set;
    }
}

