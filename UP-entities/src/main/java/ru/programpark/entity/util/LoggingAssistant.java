package ru.programpark.entity.util;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;


/**
 * User: oracle
 * Date: 26.09.14
 */
public class LoggingAssistant {
    private static Properties loggerProperties = null;
    public static int numberOfRunsToStore = 15; // ~ количество запусков планировщиков в течение последних 7 суток, запуск ОПП и ТПП считаются отдельно
    private static long timerOn = 0L, timerOff = 0L;
    private static PrintWriter inputWriter, inputDataTestWriter, inputDataTestBeforeFilterDetailsWriter, inputDataTestAfterFilterDetailsWriter, filterWriter, resultsWriter, resultsTestDetailsWriter, teamResultsWriter, locoResultsWriter, trainResultsWriter, eventWriter, analyzeWriter, timerWriter;
    static { System.setProperty("ru.programpark.entity.logdir", "."); }
    private static Logger logger = null, extLogger = null;
    private static boolean extLoggerEnabled = false;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(LoggingAssistant.class);
        return logger;
    }
    public static Logger EXTERNAL_LOGGER() {
        if (extLogger == null) extLogger = extLoggerEnabled ? LoggerFactory.getLogger("JADEPlanner") : NOPLogger.NOP_LOGGER;
        return extLogger;
    }

    public static final String
            BASE_DIR_NAME = "log",
            ARCHIVE_DIR_NAME = "uni_planner_log",
            TIMER_LOG_NAME = "timer.log",
            RESULTS_LOG_NAME = "RESULTS.log",
            INPUT_LOG_NAME = "inputData.log",
            TEAM_RESULTS_LOG_NAME = "teamSchedule.log",
            LOCO_RESULTS_LOG_NAME = "locoSchedule.log",
            TRAIN_RESULTS_LOG_NAME = "trainSchedule.log",
            INPUT_DATA_TEST_NAME = "inputDataTest.log",
            INPUT_DATA_TEST_BEFORE_FILTER_DETAILS_NAME = "inputDataTestBeforeFilterDetails.log",
            INPUT_DATA_TEST_AFTER_FILTER_DETAILS_NAME = "inputDataTestAfterFilterDetails.log",
            FILTER_NAME = "filteredInputData.log",
            EVENT_LOG_NAME = "event.log",
            RESULTS_TEST_DETAILS_LOG_NAME = "resultTestDetails.log",
            ANALYZE_LOG_NAME = "analyze.log";
    private static File BASE_DIR = (new File(BASE_DIR_NAME)).getAbsoluteFile(), ARCHIVE_DIR = new File(BASE_DIR, ARCHIVE_DIR_NAME), LOG_DIR = null;
    private static final String TIME_FORMAT = "yyyyMMdd_HHmmss", TIME_FORMAT_RE = "^\\d{8}_\\d{6}$";
    private static SimpleDateFormat formatter = new SimpleDateFormat(TIME_FORMAT);
    private static String timestamp() { return formatter.format(Calendar.getInstance().getTime()); }

    private static void createArchiveDirectory() {
        if (!BASE_DIR.exists() && !BASE_DIR.isFile()) BASE_DIR.mkdir();
        if (!ARCHIVE_DIR.exists() && !ARCHIVE_DIR.isFile()) ARCHIVE_DIR.mkdir();
    }

    public static void createLogDirectory() {
        if (LOG_DIR != null) closeAllWriters();
        createArchiveDirectory();
        LOG_DIR = new File(ARCHIVE_DIR, timestamp());
        LOG_DIR.mkdir();
        try {
            File lastLink = new File(ARCHIVE_DIR, "_last");
            if (lastLink.exists() && Files.isSymbolicLink(lastLink.toPath()))
                lastLink.delete();
            if (! lastLink.exists())
                Files.createSymbolicLink(lastLink.toPath(), LOG_DIR.toPath());
        } catch (Exception e) {}
        openAllWriters();
    }

    public static void createTestLogDirectory(boolean append) {
        if (LOG_DIR != null) closeAllWriters();
        LOG_DIR = new File(ARCHIVE_DIR, "testing");
        LOG_DIR.mkdir();
        openAllWriters(append);
    }
    private static PrintWriter openWriter(File dir, String name, boolean append) {
        try {
            return new PrintWriter(new FileWriter(new File(dir, name), append));
        } catch (Exception e){
            e.printStackTrace();
            logException(e);
            return null;
        }
    }
    private static PrintWriter openWriter(String name, boolean append) {return openWriter(LOG_DIR, name, append);}
    public static PrintWriter openWriter(String name) {return openWriter(LOG_DIR, name, true);}
    private static void openAllWriters() {openAllWriters(false);}
    private static void openAllWriters(boolean append) {
        try {
            resultsTestDetailsWriter = openWriter(RESULTS_TEST_DETAILS_LOG_NAME);
            timerWriter = openWriter(TIMER_LOG_NAME, append);
            resultsWriter = openWriter(RESULTS_LOG_NAME, append);
            inputWriter = openWriter(INPUT_LOG_NAME, append);
            teamResultsWriter = openWriter(TEAM_RESULTS_LOG_NAME, append);
            locoResultsWriter = openWriter(LOCO_RESULTS_LOG_NAME, append);
            trainResultsWriter = openWriter(TRAIN_RESULTS_LOG_NAME, append);
            eventWriter = openWriter(EVENT_LOG_NAME, append);
            inputDataTestWriter = openWriter(INPUT_DATA_TEST_NAME, append);
            inputDataTestBeforeFilterDetailsWriter = openWriter(INPUT_DATA_TEST_BEFORE_FILTER_DETAILS_NAME, append);
            inputDataTestAfterFilterDetailsWriter = openWriter(INPUT_DATA_TEST_AFTER_FILTER_DETAILS_NAME, append);
            analyzeWriter = openWriter(ANALYZE_LOG_NAME, append);
            filterWriter = openWriter(FILTER_NAME, append);
        } catch (Exception e){ logException(e); }
    }

    public static void configureLogger() {
        if (loggerProperties != null && LOG_DIR != null) {
            System.setProperty("ru.programpark.entity.logdir", LOG_DIR.getPath());
            PropertyConfigurator.configure(loggerProperties);
        }
        String msg = "Логи помещены в директорию " + LOG_DIR.getPath();
        System.out.println("LoggingAssistant: " + msg);
        EXTERNAL_LOGGER().info(msg);
    }

    private static int exceptionIndex = 0;
    public static void logException(Exception e1) {
        exceptionIndex = (exceptionIndex + 1) % 10000;
        try {
            PrintWriter wr;
            String ts = timestamp();
            String fn = String.format("Exception_%s_%04d.log", ts, exceptionIndex);
            if (LOG_DIR != null) {
                wr = openWriter(fn);
            } else {
                createArchiveDirectory();
                wr = new PrintWriter(new File(ARCHIVE_DIR, "uniPlannerLogging_" + fn));
            }
            wr.println("LoggingAssistant.logException: timeStamp " + TIME_FORMAT + ":" + ts);
            e1.printStackTrace(wr);
            wr.close();
        } catch (Exception e2) {logException(e1);}
    }

    private static void recursiveDelete(File file) throws Exception{
        String msg;
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            for (int i = 0; i < contents.length; ++i) recursiveDelete(contents[i]);
            msg = "Удаление директории: ";
        } else msg = "Удаление файла: ";
        LOGGER().debug(msg + file.getAbsolutePath());
        file.delete();
    }

    public static void clearArchive() throws Exception{
            if (ARCHIVE_DIR.exists() && ARCHIVE_DIR.isDirectory()) {
                File[] prevLogDirs = ARCHIVE_DIR.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.matches(TIME_FORMAT_RE);
                            }
                        });
                int nToDelete = prevLogDirs.length - numberOfRunsToStore;
                if (nToDelete > 0) {
                    String msg = "Очистка хранилища логов " + ARCHIVE_DIR.getPath();
                    LOGGER().info(msg);
                    EXTERNAL_LOGGER().info(msg);
                    Arrays.sort(prevLogDirs);
                    for (int i = 0; i < nToDelete; ++i) {
                        try {
                            recursiveDelete(prevLogDirs[i]);
                        } catch (Exception e) {e.printStackTrace();}
                    }
                }
            }
    }

    public static void setTimerOn() {
        timerOn = new Date().getTime();
    }

    public static String countingForm(Number n, String singular, String dual, String plural) {
        int d = (int) (n.longValue() % 100L);
        String form = (d / 10 == 1 || d % 10 == 0 || d % 10 > 4) ? plural : (d % 10 == 1) ? singular : dual;
        return form.contains("%d") ? String.format(form, n) : String.format("%d %s", n, form);
    }

    public static String countingForm(Number n, String singular, String plural) {
        return countingForm(n, singular, plural, plural);
    }

    public static void closeAllWriters(){
        try {
            LoggingAssistant.printTimeOfExec();
            timerWriter.close();
            resultsWriter.close();
            resultsTestDetailsWriter.close();
            inputWriter.close();
            teamResultsWriter.close();
            locoResultsWriter.close();
            trainResultsWriter.close();
            inputDataTestWriter.close();
            eventWriter.close();
            inputDataTestBeforeFilterDetailsWriter.close();
            inputDataTestAfterFilterDetailsWriter.close();
            analyzeWriter.close();
            filterWriter.close();
            copyFileToParentDirectory(new File(LOG_DIR + System.getProperty("file.separator") + INPUT_DATA_TEST_NAME));
            copyFileToParentDirectory(new File(LOG_DIR + System.getProperty("file.separator") + RESULTS_TEST_DETAILS_LOG_NAME));
            copyFileToParentDirectory(new File(LOG_DIR + System.getProperty("file.separator") + INPUT_DATA_TEST_BEFORE_FILTER_DETAILS_NAME));
        } catch (Exception e){logException(e);}
    }

    public static void printTimeOfExec(){
        try {
            timerOff = new Date().getTime();
            long runtime = (timerOff - timerOn) / 1000L;
            String msg = String.format("Полное время планирования: %d с. = %.1f мин.", runtime, runtime/60.0);
            System.out.println("LoggingAssistant: " + msg);
            LOGGER().info(msg);
            EXTERNAL_LOGGER().info(msg);
            timerWriter.write(msg);
        } catch (Exception e) {logException(e);}
    }

    private static void copyFileToParentDirectory(File source){
        String name = source.getName();
        File dest = new File(source.getAbsoluteFile().getParentFile().getParent() + System.getProperty("file.separator") + name);
        try {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e){logException(e);}
    }

    public static void setLoggerProperties(Properties props, boolean extEnabled) {
        loggerProperties = props;
        logger = extLogger = null;
        extLoggerEnabled = extEnabled;
    }

    public static PrintWriter getResultsWriter() {
        return resultsWriter;
    }
    public static PrintWriter getInputWriter() {
        return inputWriter;
    }
    public static PrintWriter getTeamResultsWriter() {
        return teamResultsWriter;
    }
    public static PrintWriter getLocoResultsWriter() {
        return locoResultsWriter;
    }
    public static PrintWriter getTrainResultsWriter() {
        return trainResultsWriter;
    }
    public static PrintWriter getEventWriter() {
        return eventWriter;
    }
    public static PrintWriter getInputDataTestBeforeFilterDetailsWriter() {return inputDataTestBeforeFilterDetailsWriter;}
    public static PrintWriter getInputDataTestAfterFilterDetailsWriter() {return inputDataTestAfterFilterDetailsWriter;}
    public static PrintWriter getInputDataTestWriter() {
        return inputDataTestWriter;
    }
    public static PrintWriter getResultsTestDetailsWriter() {
        return resultsTestDetailsWriter;
    }
    public static PrintWriter getFilterWriter() {
        return filterWriter;
    }
    public static PrintWriter getAnalyzeWriter() {
        return analyzeWriter;
    }
    public static Properties getLoggerProperties() {
        return loggerProperties;
    }
}

