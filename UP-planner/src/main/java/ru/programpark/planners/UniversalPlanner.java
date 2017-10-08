package ru.programpark.planners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.*;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.ResultParser;
import ru.programpark.planners.check.input.DictionaryChecker;
import ru.programpark.planners.check.input.LocoChecker;
import ru.programpark.planners.check.input.TeamChecker;
import ru.programpark.planners.check.input.TrainChecker;
import ru.programpark.planners.check.output.SlotLocoChecker;
import ru.programpark.planners.check.output.SlotTeamChecker;
import ru.programpark.planners.check.output.SlotTrainChecker;
import ru.programpark.planners.check.output.TrackChecker;
import ru.programpark.planners.common.*;
import ru.programpark.planners.loco.LocoPlanner;
import ru.programpark.planners.team.TeamPercent;
import ru.programpark.planners.team.TeamPlanner;
import ru.programpark.planners.train.TrainPlanner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UniversalPlanner {
    InputData iData;
    OutputData oData;
    SchedulingData sData;
    TrainPlanner trainPlanner;
    LocoPlanner locoPlanner;
    TeamPlanner teamPlanner;

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(UniversalPlanner.class);
        return logger;
    }

    public void init() {
        trainPlanner = TrainPlanner.reinstance();
        locoPlanner = LocoPlanner.reinstance();
        teamPlanner = TeamPlanner.reinstance();
    }

    public void fini() {
        iData = null;
        oData = null;
        sData = SchedulingData.fini();
        trainPlanner = TrainPlanner.uninstance();
        locoPlanner = LocoPlanner.uninstance();
        teamPlanner = TeamPlanner.uninstance();
    }

    private interface InputDataWrapper {
        InputData unwrap(InputData priorData);
    }

    private InputData getDefaultData() {
        String defaultDataPath = "resource:/ru/programpark/entity/default.percepts";
        try {
            Percepts defaults = new Percepts(defaultDataPath);
            Parser parser = new Parser(defaults.getAddPercepts(), defaults.getDelPercepts());
            LOGGER().debug("Данные по умолчанию загружены из " + defaultDataPath);
            return parser.getData();
        } catch (Exception e) {
            LOGGER().debug(defaultDataPath + " не найден, поэтому данные по умолчанию не загружены.");
            return null;
        }
    }

    private void startPlanning(InputDataWrapper iDataWrapper) {
        try {
            LoggingAssistant.createLogDirectory();
            LoggingAssistant.configureLogger();
            LoggingAssistant.setTimerOn();

            InputData newData = iDataWrapper.unwrap(iData);
            if (newData == null) {
                String msg = "startPlanning(): пустые входные данные";
                LoggingAssistant.EXTERNAL_LOGGER().error(msg);
                System.err.println("UniversalPlanner." + msg);
                return;
            } else if (iData == null) {
                iData = newData; // Первичное планирование
                if (!testInputData(iData, true)){
                    String inputInvalidMsg = "Набор тестов на входные данные не пройден. Детали в " +
                            LoggingAssistant.INPUT_DATA_TEST_BEFORE_FILTER_DETAILS_NAME;
                    System.out.println(inputInvalidMsg);
                    LoggingAssistant.EXTERNAL_LOGGER().warn(inputInvalidMsg);
                }
                logInputDataTestPreProcess(iData);
                iData.preprocess(getDefaultData());
                logInputDataTestPostProcess(iData);
                testInputData(iData, false);
            } else {
                // Перепланирование с учётом корректировок
                iData.updateForCorrections(newData);
            }
            oData = new OutputData();

            startPlanning();

            oData.setConfigFromVector(iData.getConfigFromVector());
            TeamPercent percentUtil = new TeamPercent();
            Map<Long, TeamRegion> percentDev = percentUtil.getActualTeamPercent(iData, oData);
            List<TeamRegion> regions = new ArrayList<>();
            regions.addAll(percentDev.values());
            oData.setRegionsWithActualPercent(regions);
            oData.setWorklessTeams(iData.getFactTeams());

            String resultCountsMsg = "По " +
                LoggingAssistant.countingForm(iData.getOneTasks().size(), "заданию", "заданиям") + " запланировано " +
                LoggingAssistant.countingForm(oData.getSlotTrains().size(), "поезд", "поезда", "поездов") + ", " +
                LoggingAssistant.countingForm(oData.getSlotLocos().size(), "локомотив", "локомотива", "локомотивов") + ", " +
                LoggingAssistant.countingForm(oData.getSlotTeams().size(), "бригада", "бригады", "бригад");
            LOGGER().info(resultCountsMsg);
            System.out.println("@@@ " + resultCountsMsg);
            LoggingAssistant.EXTERNAL_LOGGER().info(resultCountsMsg);
            oData.printAllSchedules(true);//печать всех назначений в человеческом виде в лог
            logResults();
        } catch (InterruptedException e) {
            LOGGER().warn("Планирование прервано.");
        } catch (Exception e) {
            LoggingAssistant.logException(e);
            e.printStackTrace();
        } finally {
            LoggingAssistant.closeAllWriters();
            try {
                LoggingAssistant.clearArchive();
            } catch (Exception e){
                LoggingAssistant.logException(e);
            }
        }
    }

    public void startPlanning(final String[] addPercepts, final String[] delPercepts) {
        startPlanning(new InputDataWrapper() {
            @Override public InputData unwrap(InputData priorData) {
                try {
                    Parser parser = new Parser(addPercepts, delPercepts, priorData);
                    return parser.getData();
                } catch (Exception e) {
                    LoggingAssistant.logException(e);
                    return null;
                }
            }
        });
    }

    public void startPlanning(final InputData iData) {
        startPlanning(new InputDataWrapper() {
            @Override public InputData unwrap(InputData priorData) {
                return iData;
            }
        });
    }

    private long rangeParam(String key) {
        long nHrs = iData.getConfigParam("range", key).longValue();
        return nHrs * 3600L;
    }

    private SchedulingFrame initialSchedulingFrame() {
        long range = rangeParam("length");
        long locoFrame = rangeParam("loco_frame");
        long teamFrame = rangeParam("team_frame");
        SchedulingFrame priorFrame = SchedulingData.getCurrentFrame();
        int runIndex = (priorFrame == null) ? 0 : (priorFrame.runIndex + 1);
        if (range > 0L && locoFrame > 0L && teamFrame > 0L && range % locoFrame == 0L && locoFrame % teamFrame == 0L) {
            return new SchedulingFrame(runIndex, range, iData.getCurrentTime(),
                                       locoFrame, teamFrame);
        } else {
            throw new RuntimeException("Неверно заданы параметры интервалов" +
                " планирования: range(" + range + "), loco_frame(" + locoFrame +
                "), team_frame(" + teamFrame + ")");
        }
    }

    private void resetDataToPreplanned() {
        trainPlanner.resetTrainsToPreplanned(sData);
        locoPlanner.resetLocosToPreplanned(sData);
    }

    // Собственно цикл планирования
    private void startPlanning() throws InterruptedException {
        try {
            boolean primaryPlanning = false;
            if (sData == null) {
                primaryPlanning = true;
                sData = SchedulingData.init(iData);
            }
            SchedulingFrame frame0 = initialSchedulingFrame(),
                    frame = frame0;
            SchedulingData.setCurrentFrame(frame);
            if (primaryPlanning) {
                trainPlanner.calcCapacities();
                trainPlanner.plan();
            }
            teamPlanner.updateEventsForCorrections();
            if (primaryPlanning) {
                do {
                    frame = locoPlanner.preplan(frame);
                    SchedulingData.setCurrentFrame(frame);
                } while (frame.locoFrameIndex < SchedulingFrame.LAST_INDEX);
                logEvents(frame.data, true);
                resetDataToPreplanned();
                SchedulingData.setCurrentFrame((frame = frame0));
            } else {
                frame = frame.nextFrame();
                SchedulingData.setCurrentFrame(frame);
            }
            do {
                frame = locoPlanner.plan(frame);
                SchedulingData.setCurrentFrame(frame);
                int curLocoIndex = frame.locoFrameIndex;
                do {
                    frame = teamPlanner.plan(frame);
                    SchedulingData.setCurrentFrame(frame);
                } while (frame.locoFrameIndex == curLocoIndex);
            } while (frame.locoFrameIndex < SchedulingFrame.LAST_INDEX);
            oData = frame.data.dump();
        } finally {
            logEvents(SchedulingData.getFrameData(), false);
        }

        oData.setCurrentIdTime(iData.getCurrentIdTime());
        oData.setCurrentIdOrd(iData.getCurrentIdOrd());
        oData.setAnalyzeCriteria(new Analyzer(iData, getResults()));
        logAnalyze(oData);
    }

    public OutputData getResultsAsData() {
        return oData;
    }

    public String[] getResults() {
        if (oData != null) {
            try {
                List<String> results = new ArrayList();
                Formatter formatter = new Formatter(oData);
                formatter.formatAll("plan_begin", results);
                formatter.formatAll("slot_train", results);
                formatter.formatAll("slot_loco", results);
                formatter.formatAll("slot_team", results);
                formatter.formatAll("workless_team", results);
                formatter.formatAll("fail_team_pass", results);
                formatter.formatAll("analyze", results);
                formatter.formatAll("current_id", results);
                formatter.formatAll("version", results);
                formatter.formatAll("actual_team_percent", results);
                formatter.formatAll("config", results);
                formatter.formatAll("plan_end", results);
                return results.toArray(new String[results.size()]);
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private void println(String string, Writer writer) {
        try {
            writer.write(string + "\n");
        } catch (IOException e) {
            LoggingAssistant.logException(e);
        }
    }

    public void logResults(OutputData oData) {
        try {
            Formatter formatter = new Formatter(oData);
            Writer rWriter = LoggingAssistant.getResultsWriter();
            formatter.formatAll("plan_begin", rWriter);
            formatter.formatAll("slot_train", rWriter);
            formatter.formatAll("slot_loco", rWriter);
            formatter.formatAll("slot_team", rWriter);
            formatter.formatAll("workless_team", rWriter);
            formatter.formatAll("fail_team_pass", rWriter);
            formatter.formatAll("analyze", rWriter);
            formatter.formatAll("current_id", rWriter);
            formatter.formatAll("version", rWriter);
            formatter.formatAll("actual_team_percent", rWriter);
            formatter.formatAll("config", rWriter);
            formatter.formatAll("plan_end", rWriter);
        } catch (Exception e){
            LoggingAssistant.logException(e);
        }
    }

    public void logAnalyze(OutputData oData){
        try {
            List<String> results = new ArrayList();
            PrintWriter rWriter = LoggingAssistant.getAnalyzeWriter();
            Formatter formatter = new Formatter(oData);
            formatter.formatAll("item", results);
            for (String s: results) {
                rWriter.println(s);
            }
        } catch (Exception e) {
            LoggingAssistant.logException(e);
        }
    }

    public void logResults() {
        if (!testOutputData(getResults()))
            System.out.println("Набор тестов на выходные данные не пройден. Детали в " +
                    LoggingAssistant.RESULTS_TEST_DETAILS_LOG_NAME);
        logResults(this.oData);
    }

    private void logEvents(SchedulingData data, boolean preplanning) {
        Writer evWriter = LoggingAssistant.getEventWriter();
        println(preplanning
                    ? "======== События предпланирования ========\n"
                    : "======== События повторного планирования ========\n",
                evWriter);
        for (Train train : data.getTrains()) {
            String trainPrefix = "Train " + train.getTrainId() + "\t";
            int l = train.getRoute().size();
            for (int i = -1; i < l; ++i) {
                Train.EventContainer events =
                    ((i < 0) ? train.getPreRoute() : train.getRoute().get(i));
                events.logEvents(trainPrefix + i + "\t", evWriter);
            }
            println("", evWriter);
        }
        for (Loco loco : data.getLocos()) {
            loco.getEvents().logEvents("Loco " + loco.getId() + "\t", evWriter);
            println("", evWriter);
        }
        for (Team team : data.getTeams()) {
            team.getEvents().logEvents("Team " + team.getId() + "\t", evWriter);
            println("", evWriter);
        }
    }

    public InputData getInputAsData(){
        return iData;
    }


    public static void logInputDataTestPreProcess(InputData iData){
        LoggingAssistant.getInputDataTestWriter().println(iData.stationCount + "," + iData.linkCount + "," +
                                                            iData.lineCount + "," + iData.trainInfoCount + "," +
                                                            iData.locoAttrCount + "," + iData.teamAttrCount + "," +
                                                            iData.getTeamWorkRegions().size() + "," +
                                                            iData.getTeamServiceRegions().size());

        LoggingAssistant.getInputDataTestWriter().println(
                iData.trainArriveCount + iData.trainDepartCount + iData.trainReadyCount + "," + //a
                iData.trainDepartCount + "," + //b
                iData.getTrainDepartWitMissingLocoCount() + "," + //c
                iData.getCorruptedTrainDepartCount() + "," + // d
                iData.fLocoCount + "," + //e
                iData.getFactLocoWithTrainCount() + "," + //f
                iData.getFactLocosWithMissingTrainCount() + "," + //g
                iData.getCorruptedFactLocosCount() + "," + //h
                iData.getFactLocosOnTrackCount() + "," + //i
                iData.getFactLocosWithMissingTeamsCount() +"," + //j
                iData.getFactLocosWithIncorrectTeamsCount() +"," + //k
                iData.getFactTeams().size()+"," + //l
                iData.getFactTeamsWithLocoCount() +"," + //m
                iData.getFactTeamsWithMissingLocosCount() +"," + //n
                iData.getFactTeamsWithIncorrectLocosCount());  //o

        LoggingAssistant.getInputDataTestWriter().println("Количество принятых сообщений +station: " +
                iData.stationCount);
        LoggingAssistant.getInputDataTestWriter().println("Количество принятых сообщений +link: " +
                iData.linkCount);
        LoggingAssistant.getInputDataTestWriter().println("Количество принятых сообщений +line: " +
                iData.lineCount);
        LoggingAssistant.getInputDataTestWriter().println("Количество принятых сообщений +train_info: " +
                iData.trainInfoCount);
        LoggingAssistant.getInputDataTestWriter().println("Количество принятых сообщений +loco_attributes: " +
                iData.locoAttrCount);
        LoggingAssistant.getInputDataTestWriter().println("Количество принятых сообщений +team_attributes: " +
                iData.teamAttrCount);
        LoggingAssistant.getInputDataTestWriter().println("");

        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Station (станция) до добавления" +
                " дефолтных: " + iData.getStations().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Link (участок планирования) " +
                "до добавления дефолтных: " + iData.getLinks().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов LocoRegion (локомотивное плечо) до " +
                "добавления дефолтных: " + iData.getLocoRegions().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов TeamRegion (Work - участок обкатки " +
                "бригады) до добавления дефолтных: " + iData.getTeamWorkRegions().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов TeamRegion (Service - " +
                "участок обслуживания бригады) до добавления дефолтных: " + iData.getTeamServiceRegions().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Slot (грузовые нитки) " +
                "до добавления дефолтных: " + iData.getSlots().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Slot (пассажирские нитки) " +
                "до добавления дефолтных: " + iData.getSlots_pass().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Lines (пути) " +
                "до добавления дефолтных: " + iData.lineCount);
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов FactTrain " +
                "до фильтрации: " + iData.getFactTrains().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов FactLoco " +
                "до фильтрации: " + iData.getFactLocos().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов FactTeam " +
                "до фильтрации: " + iData.getFactTeams().size());
    }

    public static void logInputDataTestPostProcess(InputData iData){
        LoggingAssistant.getInputDataTestWriter().println("");
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Station (станция)" +
                " после добавления дефолтных: " + iData.getStations().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Link (участок планирования)" +
                " после добавления дефолтных: " + iData.getLinks().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов LocoRegion (локомотивное плечо)" +
                " после добавления дефолтных: " + iData.getLocoRegions().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов TeamRegion (Work - участок обкатки " +
                "бригады) после добавления дефолтных: " + iData.getTeamWorkRegions().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов TeamRegion (Service - участок " +
                "обслуживания бригады) после добавления дефолтных: " + iData.getTeamServiceRegions().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Slot (грузовые нитки) после" +
                " добавления дефолтных: " + iData.getSlots().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Slot (пассажирские нитки) " +
                "после добавления дефолтных: " + iData.getSlots_pass().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов Lines (пути) " +
                "после добавления дефолтных: " + iData.lineCount);
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов FactTrain " +
                "после фильтрации: " + iData.getFactTrains().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов FactLoco " +
                "после фильтрации: " + iData.getFactLocos().size());
        LoggingAssistant.getInputDataTestWriter().println("Количество объектов FactTeam " +
                "после фильтрации: " + iData.getFactTeams().size());
        LoggingAssistant.getInputDataTestWriter().close();
    }

    public boolean testInputData(InputData input, boolean beforeFilter){
        boolean res = true;

        try {
            //2.1	Набор тестов по справочной информации
            DictionaryChecker.beforeFilter = beforeFilter;
            res = DictionaryChecker.checkStationQuantity_2_1_1(input) && res;
            res = DictionaryChecker.checkStationChangeTeam_2_1_2(input) && res;
            res = DictionaryChecker.checkStationChangeLoco_2_1_3(input) && res;
            res = DictionaryChecker.checkLocoRegion_2_1_4(input) && res;
            res = DictionaryChecker.checkTeamWorkRegion_2_1_5(input) && res;
            res = DictionaryChecker.checkStopLengthForLocoChange_2_1_6(input) && res;
            res = DictionaryChecker.checkStopLengthForTeamChange_2_1_7(input) && res;

            //2.2	Набор тестов по поездам
            TrainChecker.beforeFilter = beforeFilter;
            res = TrainChecker.checkTrainExist_2_2_1(input) && res;
            res = TrainChecker.checkDuplicateFactTrains_2_2_2(input) && res;
            res = TrainChecker.checkTrainAttributesDefined_2_2_3(input) && res;
            res = TrainChecker.checkRouteForFactTrainsDefined_2_2_4(input) && res;
            res = TrainChecker.checkLocationForFactTrainsDefined_2_2_5(input) && res;
            res = TrainChecker.checkCorrectLocationForTrainFacts_2_2_6(input) && res;
            res = TrainChecker.checkCorrectDepartureTimeForTrainFacts_2_2_7(input) && res;
            res = TrainChecker.checkTrainLocoReference_2_2_8(input) && res;

            //2.3	Набор тестов по локомотивам
            LocoChecker.beforeFilter = beforeFilter;
            res = LocoChecker.checkLocoAttrInfoRecieved_2_3_1(input) && res;
            res = LocoChecker.checkLocoAttrDuplicates_2_3_2(input) && res;
            res = LocoChecker.checkLocoAttrDefined_2_3_3(input) && res;
            res = LocoChecker.checkForEachLocoAttrOneFactLoco_2_3_4(input) && res;
            res = LocoChecker.checkForEachLocoAttrOneFactLocoNextService_2_3_5(input) && res;
            res = LocoChecker.checkForFactLocoWithTrainExistsTrainDepart_2_3_6(input) && res;
            res = LocoChecker.checkForFactLocoWithTrainExistsTrainArrive_2_3_7(input) && res;
            res = LocoChecker.checkForEachFactLocoExistsFactTeam_2_3_8(input) && res;
            res = LocoChecker.checkForEachLocoRegionExistsFactLoco_2_3_9(input) && res;
            res = LocoChecker.checkLocoRegionCoverage_2_3_10(input) && res;
            res = LocoChecker.checkTimeToServiceToEqualToZero_2_3_11(input) && res;

            //2.4	Набор тестов по бригадам
            TeamChecker.beforeFilter = beforeFilter;
            res = TeamChecker.checkTeamAttrInfoRecieved_2_4_1(input) && res;
            res = TeamChecker.checkTeamAttrDuplicates_2_4_2(input) && res;
            res = TeamChecker.checkTeamAttrContainsInfo_2_4_3(input) && res;
            res = TeamChecker.checkForEachTeamAttrOneFactTeam_2_4_4(input) && res;
            res = TeamChecker.checkForEachTeamAttrExistsFactTeamNextRest_2_4_5(input) && res;
            res = TeamChecker.checkForFactTeamOnLinkExistsFactLoco_2_4_6(input) && res;
            res = TeamChecker.checkForFactTeamOnStationExistsFactLoco_2_4_7(input) && res;
            res = TeamChecker.checkForEachTeamWorkRegionExistsTeam_2_4_8(input) && res;
            res = TeamChecker.checkTeamWorkRegionConsistency_2_4_9(input) && res;
        } catch (Exception e) {
            LoggingAssistant.logException(e);
        }

        if (beforeFilter)
            LoggingAssistant.getInputDataTestBeforeFilterDetailsWriter().close();
        else
            LoggingAssistant.getInputDataTestAfterFilterDetailsWriter().close();

        return res;
    }

    public boolean testOutputData(String[] results){
        boolean res = true;
        ResultParser parser = null;

        try {
            parser = new ResultParser(Arrays.asList(results));
            res = TrackChecker.checkCorrectTimeOnTrack_3_1_1(parser.slotTrains.values(),
                    parser.slotLocos.values(), parser.slotTeams.values()) && res;
            res = TrackChecker.checkCorrectTimeOnStay_3_1_2(parser.slotTrains.values(),
                    parser.slotLocos.values(), parser.slotTeams.values()) && res;
            res = TrackChecker.checkEqualsTimeInTrainAndLoco_3_1_3(parser.slotTrains,
                    parser.slotLocos.values()) && res;
            res = TrackChecker.checkEqualsTimeInLocoAndTeam_3_1_4(iData,
                    parser.slotLocos, parser.slotTeams.values()) && res;
            res = SlotLocoChecker.checkLongStayForLocoChange_3_1_5(parser.slotTrains.values(),
                    parser.slotLocos.values())&& res;
            res = SlotTeamChecker.checkLongStayForTeamChange_3_1_6(parser.slotTrains.values(),
                    parser.slotLocos.values(), parser.slotTeams.values()) && res;
            res = SlotTrainChecker.checkAbnormalLength_3_1_7(iData, parser.slotTrains.values()) && res;
            res = SlotLocoChecker.checkEarlyLoco_3_1_8(iData, parser.slotLocos.values()) && res;
            res = SlotTeamChecker.checkEarlyTeam_3_1_9(iData, parser.slotTeams.values()) && res;
            res = SlotTrainChecker.checkCloseThread_3_1_10(parser.slotTrains.values(),
                    parser.slotLocos.values(), parser.slotTeams.values()) && res;
            res = SlotTrainChecker.checkCrossThread_3_1_11(iData, parser.slotTrains.values()) && res;
            res = SlotLocoChecker.checkAllLocoRelocationsProcessed_3_1_12(iData, parser.slotLocos) && res;
            res = SlotTrainChecker.checkFullTrainRoutesAssigned_3_1_13(iData, parser.slotTrains) && res;
            res = SlotTeamChecker.allTeamsAsPassHaveLocoIdsOrSlotIdsSet_3_1_14(parser.slotTeams.values()) && res;
            res = SlotTeamChecker.AllUnassignedTeamsHaveWorklessTeamMessage_3_1_15(iData, parser.slotTeams,
                    parser.worklessTeams) && res;

            for (TeamRegion region: iData.getTeamServiceRegions().values()){
                res = SlotTeamChecker.checkTeamPercent_3_1_16(iData, parser.slotTeams, region) && res;
            }

            res = SlotTeamChecker.checkTeamsDoNotExceedWorkingTimeLimit_3_1_17(parser.slotTeams.values()) && res;
            res = SlotLocoChecker.checkManualBindingWithAccordanceToTimeTilService_3_1_18(iData, parser.slotLocos)
                    && res;
            res = SlotTrainChecker.checkNoOvertakingOnStations_3_1_19(iData, parser.slotTrains.values()) && res;
            res = SlotLocoChecker.checkLocoInOddDirection_3_1_20(iData, parser.slotTrains.values()) && res;
            res = SlotTrainChecker.checkAllTrainOrder_3_1_21(iData, parser.slotTrains.values()) && res;
            res = SlotTrainChecker.checkNumberOfSlotTrainsEachHourEqualsToNumberOfSlots_3_1_22(iData,
                    parser.slotTrains.values()) && res;

            for (Station s: iData.getStations().values()) {
                if (s.getNormTime() == 0L)
                    continue;
                res = SlotLocoChecker.checkLocoOrder_3_1_23(iData, parser.slotLocos, s) && res;
            }

            res = SlotLocoChecker.checkStopLengthForLocoChange_3_1_24(iData, parser.slotTrains) && res;
            res = SlotTeamChecker.checkStopLengthForLocoChange_3_1_25(iData, parser.slotTrains) && res;
            res = SlotLocoChecker.checkReserveLocoCrossing_3_1_26(iData, parser.slotLocos) && res;
            res = SlotLocoChecker.checkReserveLocoInOddDirectionHaveTeams_3_1_27(iData, parser.slotLocos,
                    parser.slotTeams) && res;
            res = SlotTeamChecker.checkTeamsWorkWhereTheyAreAllowed_3_1_28(iData, parser.slotTeams) && res;
            res = SlotTrainChecker.checkTrainPriority_3_1_29(iData, parser.slotTrains) && res;
            res = SlotTeamChecker.checkAllPassTeamsSentWithTrainLocoAndTeam_3_1_30(parser.slotTeams,
                    parser.slotLocos, parser.slotTrains) && res;
            res = SlotTrainChecker.checkTrainsAccordingToWeight(parser.slotLocos, iData) && res;
        } catch (Exception e){
            LoggingAssistant.logException(e);
            return false;
        }

        return res;
    }
}
