package ru.programpark.entity.data;

import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.loco.RealLoco;
import ru.programpark.entity.team.*;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.OneTask;
import ru.programpark.entity.train.RealTrain;
import ru.programpark.entity.util.Pair;

import java.util.*;

/**
 * User: oracle
 * Date: 22.05.14
 */
public class OutputData {
    private Long currentIdTime = System.currentTimeMillis() / 1000L;
    private Long currentIdOrd = 0L;

    // Частная копия оперативных фактов из входных данных, которые можно
    // безопасно менять, не подвергая опасности целостность входных данных:
    private Map<Long, FactTrain> factTrains = new HashMap<>();
    private Map<Long, FactLoco> factLocos = new HashMap<>();
    private Map<Long, FactTeam> factTeams = new HashMap();

    public Map<Long, OneTask> oneTasks = new HashMap<>();
    Map<Long, RealTrain> slotTrains = new HashMap();
    Map<Long, RealLoco> slotLocos = new HashMap();
    Map<Long, RealTeam> slotTeams = new HashMap();
    Map<Long, FactTeam> worklessTeams = new HashMap();
    Map<Long, AnalyzeCriterion> analyzeCriteria = new TreeMap<>();
    List<TeamPass> failedTeamPassList = new ArrayList<>();
    List<PinnedTeam> failedPinnedTeamList = new ArrayList<>();
    List<TeamRegion> regionsWithActualPercent = new ArrayList<>();
    private Map<String, Number> configFromVector = new HashMap<>();

    private String version = Version.string();

    public OutputData() {
    }

    public OutputData(Long currentIdTime, Long currentIdOrd) {
        this.currentIdTime = currentIdTime;
        this.currentIdOrd = currentIdOrd;
    }

    public Long getCurrentIdTime() {
        return currentIdTime;
    }

    public Long getCurrentIdOrd() {
        return currentIdOrd;
    }

    public void setCurrentIdTime(Long time) {
        this.currentIdTime = time;
    }

    public void setCurrentIdOrd(Long ord) {
        this.currentIdOrd = ord;
    }


    public Map<Long, FactTrain> getFactTrains() {
        return factTrains;
    }

    public Map<Long, FactLoco> getFactLocos() {
        return factLocos;
    }

    public Map<Long, FactTeam> getFactTeams() {
        return factTeams;
    }


    public Map<Long, OneTask> getOneTasks() {
        return oneTasks;
    }

    public Map<Long, RealLoco> getSlotLocos() {
        return slotLocos;
    }

    public Map<Long, RealTeam> getSlotTeams() {
        return slotTeams;
    }


    /*public static class Results {
        private static List<String> results = new ArrayList<String>();

        public static void reset() {
            results.clear();
        }

        public static void add(String res) { results.add(res); }
        public static void addAll(Collection<String> res) { results.addAll(res); }

        public static void formatAll(TermFormatter formatter, String key) {
            formatter.formatAll(key, results);
        }

    } */

    public void addSlotTrain(RealTrain train){
        slotTrains.put(train.getTrainId(), train);
    }

    public void delSlotTrain(RealTrain train){
        slotTrains.remove(train.getTrainId());
    }

    public void addSlotLoco(RealLoco loco){
        Long id = loco.getRealLocoId();
        RealLoco extant = slotLocos.get(id);
        if (extant == null) {
            slotLocos.put(id, loco);
        } else {
            extant.getRoute().addAll(loco.getRoute());
        }
    }

    public void addSlotTeam(RealTeam team){
        slotTeams.put(team.getId(), team);
    }

    public Map<Long, RealTrain> getSlotTrains() {
        return slotTrains;
    }

    public String getVersion() {
        return version;
    }

    public void setSlotTrains(Map<Long, RealTrain> slotTrains) {
        this.slotTrains = slotTrains;
    }

    public void setSlotLocos(Map<Long, RealLoco> slotLocos) {
        this.slotLocos = slotLocos;
    }

    public void setSlotTeams(Map<Long, RealTeam> slotTeams) {
        this.slotTeams = slotTeams;
    }

    public Map<Long, AnalyzeCriterion> getAnalyzeCriteria() {
        return analyzeCriteria;
    }

    public void setAnalyzeCriteria(Map<Long, AnalyzeCriterion> analyzeCriteria) {
        this.analyzeCriteria = analyzeCriteria;
    }

    public void setAnalyzeCriteria(Analyzer analyzer) {

        analyzeCriteria.clear();

        //Процент незапланированных  поездов
        //analyzeCriteria.put(1L, analyzer.getDataForRow1()); // - пока нет данных
        //Процент незапланированных фактических поездов
        analyzeCriteria.put(2L, analyzer.getDataForRow2());
        //Процент поездов, обеспеченных локомотивами на горизонте в 6 часов
        analyzeCriteria.put(3L, analyzer.getDataForRow3(21600L));
        //Процент локомотивов, обеспеченных бригадами на горизонте в 6 часов
        analyzeCriteria.put(4L, analyzer.getDataForRow4(21600L));
        //Процент поездов, обеспеченных локомотивами на горизонте в 24 часов
        analyzeCriteria.put(5L, analyzer.getDataForRow5());
        //Процент локомотивов, обеспеченных бригадами на горизонте в 24 часа
        analyzeCriteria.put(6L, analyzer.getDataForRow6());
        //Средне-суточная скорость поездов ходовая
        Pair<AnalyzeCriterion,AnalyzeCriterion> analyzeCriterionPair7_8 = analyzer.getDataForRow7_8(86400L);
        analyzeCriteria.put(7L, analyzeCriterionPair7_8.getFirst());
        //Средная скорость поездов техническая
        analyzeCriteria.put(8L, analyzeCriterionPair7_8.getSecond());
        //Время перемещения локомотива резервом общее время
        Pair<AnalyzeCriterion,AnalyzeCriterion> analyzeCriterionPair9_10 = analyzer.getDataForRow9_10();
        analyzeCriteria.put(9L, analyzeCriterionPair9_10.getFirst());
        //Время перемещения локомотива резервом среднее время
        analyzeCriteria.put(10L, analyzeCriterionPair9_10.getSecond());
        //Время пересылки бригад пассажирами общее время
        Pair<AnalyzeCriterion,AnalyzeCriterion> analyzeCriterionPair11_12 = analyzer.getDataForRow11_12();
        analyzeCriteria.put(11L, analyzeCriterionPair11_12.getFirst());
        //Время пересылки бригад пассажирами среднее время
        analyzeCriteria.put(12L, analyzeCriterionPair11_12.getSecond());
        //Процент поездов, запланированных не до конца маршрута
        analyzeCriteria.put(13L, analyzer.getDataForRow13());
        //Процент локомотивов, для которых нарушена целостность маршрута
        analyzeCriteria.put(14L, analyzer.getDataForRow14());
        //Процент бригад, для которых нарушена целостность маршрута
        analyzeCriteria.put( 15L, analyzer.getDataForRow15());
        //Средний простой поездов на станциях по маршруту, мин
        analyzeCriteria.put(16L, analyzer.getDataForRow16(86400));
        //Средний простой поездов на станциях смены локомотивных бригад, мин
        analyzeCriteria.put(17L, analyzer.getDataForRow17(86400));
        //Средний простой локомотивов на станциях без работы, мин
        analyzeCriteria.put(18L, analyzer.getDataForRow18(86400));
        //Среднее рабочее время локомотивов за сутки, мин
        analyzeCriteria.put(19L, analyzer.getDataForRow19(86400));
        //Среднее время работы локомотивной бригады, мин
        analyzeCriteria.put(20L, analyzer.getDataForRow20(86400));
        //Количество локомотивов с превышением пробега и времени работы до ТО-2
        analyzeCriteria.put( 21L, analyzer.getDataForRow21());
        //Количество бригад с превышением норм рабочего времени
        analyzeCriteria.put(22L, analyzer.getDataForRow22());
        //Количество локомотивов, выезжающих за пределы своих тяговых плеч
        analyzeCriteria.put(23L ,analyzer.getDataForRow23());
        //Количество бригад, выезжающих за свои участки обкатки
        analyzeCriteria.put(24L,analyzer.getDataForRow24());
        //Количество поездов, для которых смена локомотивов происходит не на границе тягового плеча
        analyzeCriteria.put(25L, analyzer.getDataForRow25());
        //Количество участков планирования без тяговых плеч
        analyzeCriteria.put(26L, analyzer.getDataForRow26());
        //Количество участков планирования без участков обкатки бригад
        analyzeCriteria.put(27L,analyzer.getDataForRow27());
        //Отсутствие связности поездов и локомотивов
        analyzeCriteria.put(28L, analyzer.getDataForRow28());
        //Отсутствие связности локомотивов и поездов
        analyzeCriteria.put(29L, analyzer.getDataForRow29());
        //Отсутствие связности локомотивов и бригад
        analyzeCriteria.put(30L, analyzer.getDataForRow30());
        //Отсутствие связности бригад и локо
        analyzeCriteria.put(31L, analyzer.getDataForRow31());
        //System.out.println("Количество локомотивов без местоположения: " + analyzer.getDataForRow32());
        analyzeCriteria.put(32L, analyzer.getDataForRow32());
        //System.out.println("Количество локомотивов без оставшегося времени работы: " + analyzer.getDataForRow33());
        analyzeCriteria.put(33L, analyzer.getDataForRow33());
        //System.out.println("Количество бригад без местоположения: " + analyzer.getDataForRow34());
        analyzeCriteria.put(34L, analyzer.getDataForRow34());
        //System.out.println("Количество бригад без оставшегося рабочего времени: " + analyzer.getDataForRow35());
        analyzeCriteria.put(35L, analyzer.getDataForRow35());
        //Привязка бригады без нарушения правил по временным нормам работы
        analyzeCriteria.put(36L, analyzer.getDataForRow36());
        // Количество «старых» поездов
        analyzeCriteria.put(37L, analyzer.getDataForRow37());
        //Количество бригад без подвязки
        analyzeCriteria.put(38L, analyzer.getDataForRow38(86400));
        // Количество локомотивов без подвязки в состоянии "ожидание работы"
        analyzeCriteria.put(39L, analyzer.getDataForRow39());
        //Количество пересылок локомотивов резервом
        analyzeCriteria.put(40L, analyzer.getDataForRow40());
        //Количество локомотивных бригад, отправленных пассажирами
        analyzeCriteria.put(41L, analyzer.getDataForRow41());
        //Временное расстояние между поездами не меньше 10 минут
        analyzeCriteria.put(42L, analyzer.getDataForRow42());
        // Количество локомотивов резервом, следующих в четном направлении
        analyzeCriteria.put(43L, analyzer.getDataForRow43());
        //Процент поездов, обеспеченных локомотивами на горизонте в 12 часов
        analyzeCriteria.put(44L, analyzer.getDataForRow44());
        //Процент локомотивов, обеспеченных бригадами на горизонте в 12 часов
        analyzeCriteria.put(45L, analyzer.getDataForRow45());
        //Процент поездов, обеспеченных локомотивами на горизонте в 18 часов
        analyzeCriteria.put(46L, analyzer.getDataForRow46());
        //Процент локомотивов, обеспеченных бригадами на горизонте в 18 часов
        analyzeCriteria.put(47L, analyzer.getDataForRow47());
        //Используемый парк локомотивов
        analyzeCriteria.put(48L, analyzer.getDataForRow48());
        //Используемый парк бригад
        analyzeCriteria.put(49L, analyzer.getDataForRow49());
        // Средний вес поезда (т)
        Pair<AnalyzeCriterion,AnalyzeCriterion> analyzeCriterionPair50_51 = analyzer.getDataForRow50_51(86400L);
        analyzeCriteria.put(50L, analyzeCriterionPair50_51.getFirst());
        // Производительность локомотивов
        analyzeCriteria.put(51L, analyzeCriterionPair50_51.getSecond());
        //Среднесуточный полезный пробег локомотивов (км)
        Pair<AnalyzeCriterion,AnalyzeCriterion> analyzeCriterionPair52_53 =
                analyzer.getDataForRow52_53(86400);
        analyzeCriteria.put(52L, analyzeCriterionPair52_53.getFirst());
        // Среднесуточный процент полезного использования локомотивов
        analyzeCriteria.put(53L, analyzeCriterionPair52_53.getSecond());
        //Среднее время стоянки поезда на технической станции
        analyzeCriteria.put(54L, analyzer.getDataForRow54(86400));
        //Средний простой локомотива на станции
        Pair<AnalyzeCriterion,AnalyzeCriterion> analyzeCriterionPair55_56 =
                analyzer.getDataForRow55_56(86400);
        analyzeCriteria.put(55L, analyzeCriterionPair55_56.getFirst());
        // Средний простой локомотива на станции в ожидании работы
        analyzeCriteria.put(56L, analyzeCriterionPair55_56.getSecond());
        // Среднее время отдыха бригад в пунктах оборота
        Pair<AnalyzeCriterion,AnalyzeCriterion> analyzeCriterionPair57_58 =
                analyzer.getDataForRow57_58(86400);
        analyzeCriteria.put(57L, analyzeCriterionPair57_58.getFirst());
        // СКоличество бригад с переотдыхом
        analyzeCriteria.put(58L, analyzeCriterionPair57_58.getSecond());
        // Количество бригад, отправленных с оборота
        analyzeCriteria.put(59L, analyzer.getDataForRow59(86400));
        // Количество резервных локомотивов без бригад
        analyzeCriteria.put(60L, analyzer.getDataForRow60(86400));
    }

    public Map<Long, FactTeam> getWorklessTeams() {
        return worklessTeams;
    }

    public void setWorklessTeams(Map<Long, FactTeam> fTeams) {
        for (FactTeam fTeam: fTeams.values()){
            if (!slotTeams.containsKey(fTeam.getId()))
                worklessTeams.put(fTeam.getId(), fTeam);
        }
    }

    public void printAllTeamSchedules(boolean toFile){
        for (RealTeam rTeam: getSlotTeams().values()){
            rTeam.printSchedule(toFile);
        }
    }

    public void printAllLocoSchedules(boolean toFile){
        for (RealLoco rLoco: getSlotLocos().values()){
            rLoco.printSchedule(toFile);
        }
    }

    public void printAllTrainSchedules(boolean toFile){
        for (RealTrain rTrain: getSlotTrains().values()){
            rTrain.printSchedule(toFile);
        }
    }

    public void printAllSchedules(boolean toFile){
        printAllTrainSchedules(toFile);
        printAllLocoSchedules(toFile);
        printAllTeamSchedules(toFile);
    }

    public List<TeamPass> getFailedTeamPassList() {
        return failedTeamPassList;
    }

    public void setFailedTeamPassList(List<TeamPass> failedTeamPassList) {
        this.failedTeamPassList = failedTeamPassList;
    }

    public void clear() {
        oneTasks.clear();
        factTrains.clear();
        factLocos.clear();
        factTeams.clear();
        slotTrains.clear();
        slotLocos.clear();
        slotTeams.clear();
        worklessTeams.clear();
        analyzeCriteria.clear();
        failedTeamPassList.clear();
        failedPinnedTeamList.clear();
        failedTeamPassList.clear();
    }

    public void addAll(OutputData data) {
        oneTasks.putAll(data.getOneTasks());
        factTrains.putAll(data.getFactTrains());
        factLocos.putAll(data.getFactLocos());
        factTeams.putAll(data.getFactTeams());
        for (RealTrain train : data.getSlotTrains().values()) {
            // Переносим только новые поезда, созданные под локомотивы
            // резервом:
            if (! slotTrains.containsKey(train.getTrainId()))
                addSlotTrain(train);
        }
        // for (RealLoco loco : data.getSlotLocos().values())
        //     addSlotLoco(loco);
        for (RealTeam team : data.getSlotTeams().values())
            addSlotTeam(team);
        worklessTeams.putAll(data.getWorklessTeams());
    }

    public void addSlotLocos(OutputData data) {
        for (RealLoco loco : data.getSlotLocos().values())
            addSlotLoco(loco);
    }

    public List<TeamRegion> getRegionsWithActualPercent() {
        return regionsWithActualPercent;
    }

    public void setRegionsWithActualPercent(List<TeamRegion> regionsWithActualPercent) {
        this.regionsWithActualPercent = regionsWithActualPercent;
    }

    public List<PinnedTeam> getFailedPinnedTeamList() {
        return failedPinnedTeamList;
    }

    public void setFailedPinnedTeamList(List<PinnedTeam> failedPinnedTeamList) {
        this.failedPinnedTeamList = failedPinnedTeamList;
    }

    public void setConfigFromVector(Map<String, Number> configFromVector) {
        this.configFromVector = configFromVector;
    }

    public Map<String, Number> getConfigFromVector() {
        return configFromVector;
    }
}
