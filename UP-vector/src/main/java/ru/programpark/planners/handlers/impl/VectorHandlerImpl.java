package ru.programpark.planners.handlers.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.data.OutputData;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;
import ru.programpark.planners.UniversalPlanner;
import ru.programpark.planners.handlers.VectorHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VectorHandlerImpl implements VectorHandler {
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(VectorHandlerImpl.class);
        return logger;
    }

    private static void safeSleep(long timeout) {
        try { Thread.sleep(timeout); } catch (InterruptedException e) {}
    }

    private enum Stage { NO_STAGE, IDLE, INPUT_READY, OUTPUT_READY,
                         FORCED_STOP, HALT };

    private abstract class Callback { abstract Stage call(); }

    private class PlannerThread extends Thread {
        String plannerName;
        UniversalPlanner plannerWrapper;
        InputData input;
        OutputData output;
        String[] addPercepts, delPercepts, results;
        Stage stage = Stage.NO_STAGE;
        public volatile Boolean stop = false;

        PlannerThread(String name) {
            setName("VectorHandlerImpl PlannerThread " + name);
            plannerName = name;
        }

        @Override public void run() {
            init();
            while (stage != Stage.HALT) {
                if (stage == Stage.INPUT_READY) {
                    setStage(new Callback() {
                        Stage call() {
                            plan();
                            return stop ? Stage.FORCED_STOP :
                                Stage.OUTPUT_READY;
                        }
                    });
                } else {
                    safeSleep(200);
                }
            }
            fini();
        }

        private void init() {
            plannerWrapper = new UniversalPlanner();
            System.out.println("VectorHandlerImpl: Создан " + plannerName + " планировщик " + new Time(System.currentTimeMillis()/1000).getTimeStamp());
            plannerWrapper.init();
            String path = "";
            if (LoggingAssistant.getLoggerProperties() == null) {
                // Запуск не через Main (т. е., скорее всего, из-под Вектора):
                // подгружаем особую конфигурацию логгера только для категорий
                // ru.programpark.entity, ru.programpark.planner.
                try {
                    Properties logProps = new Properties();
                    path = "/ru/programpark/planners/log4j.properties";
                    InputStream in =
                        VectorHandlerImpl.class.getResourceAsStream(path);
                    if (in != null) logProps.load(in);
                    LoggingAssistant.setLoggerProperties(logProps, true);
                } catch (IOException e) {
                    System.out.println("VectorHandlerImpl: Конфигурация логирования в " + path +
                                       " не найдена");
                }
            }
            setStage(Stage.IDLE);
        }

        private void plan() {
            System.out.println("VectorHandlerImpl: Запущен расчет " + plannerName + " планировщика...");
            if (addPercepts != null && delPercepts != null) {
                plannerWrapper.startPlanning(addPercepts, delPercepts);
                input = plannerWrapper.getInputAsData();
            } else if (input != null) {
                plannerWrapper.startPlanning(input);
            }
            output = plannerWrapper.getResultsAsData();
            results = plannerWrapper.getResults();
        }

        private void fini() {
            System.out.println("VectorHandlerImpl: Удаление " + plannerName + " планировщика...");
            plannerWrapper.fini();
            plannerWrapper = null;
            setStage(Stage.NO_STAGE);
            //LoggingAssistant.closeAllWriters();
        }

        synchronized void setStage(Callback callback) {
            stage = callback.call();
        }

        synchronized void setStage(Stage st) {
            stage = st;
        }

        Boolean atStage(Stage... options) {
            for (int i = 0; i < options.length; ++i) {
                if (stage.equals(options[i])) return true;
            }
            return false;
        }

        @Override
        public boolean isInterrupted() {
            return stop || super.isInterrupted();
        }

    }

    private PlannerThread thread = null;
    private Boolean async = true;

    private void createPlanner(String name, Boolean async) {
        if (this.thread != null && thread.isAlive()) removePlanner();
        this.async = async;
        (this.thread = new PlannerThread(name)).start();
        waitForResults();
    }

    @Override
    public void createPlanner(String name, String plannerType) {
        // ignore plannerType;
        createPlanner(name, true);
    }

    @Override
    public void createPlanner() {
        createPlanner("uni", true);
    }

    @Override
    public void createPlanner(Boolean async) {
        createPlanner("uni", async);
    }

    @Override
    public void removePlanner() {
        if (thread != null) {
            if (thread.atStage(Stage.INPUT_READY)) {
                thread.stop = true;
                thread.setStage(new Callback() {
                    Stage call() {
                        waitForResults();
                        return Stage.HALT;
                    };
                });
            } else {
                thread.setStage(Stage.HALT);
            }
            while (thread.isAlive()) safeSleep(50);
            thread = null;
        }
    }

    private void startPlanning(final String[] addPercepts,
                               final String[] delPercepts,
                               final InputData inputData) {
        if (thread != null) {
            thread.stop = false;
            waitForResults();
            thread.setStage(new Callback() {
                Stage call() {
                    thread.addPercepts = addPercepts;
                    thread.delPercepts = delPercepts;
                    thread.input = inputData;
                    return Stage.INPUT_READY;
                }
            });
        }
    }

    @Override
    public void startPlanning(String[] addPercepts, String[] delPercepts) {
        startPlanning(addPercepts, delPercepts, null);
    }

    @Override
    public void startPlanning(InputData inputData) {
        startPlanning(null, null, inputData);
    }

    private boolean haveResults() {
        return thread.atStage(Stage.OUTPUT_READY, Stage.FORCED_STOP,
                              Stage.IDLE);
    }

    private boolean waitForResults() {
        while (! haveResults()) safeSleep(65);
        return true;
    }

    private void setStageWithResults(Callback callback) {
        if (thread != null) {
            if (async ? haveResults() : waitForResults()) {
                thread.setStage(callback);
            }
        }
    }

    @Override
    public String[] getResults() {
        final String[][] pResults = {null};
        setStageWithResults(new Callback() {
            Stage call() {
                if (! async || thread.atStage(Stage.OUTPUT_READY))
                    pResults[0] = thread.results;
                return Stage.IDLE;
            }
        });
        return pResults[0];
    }

    @Override
    public OutputData getResultsAsData() {
        final OutputData[] pOutput = {null};
        setStageWithResults(new Callback() {
            Stage call() {
                if (! async || thread.atStage(Stage.OUTPUT_READY))
                    pOutput[0] = thread.output;
                return Stage.IDLE;
            }
        });
        return pOutput[0];
    }

    @Override
    public void logResults() {
        setStageWithResults(new Callback() {
            Stage call() {
                thread.plannerWrapper.logResults();
                return thread.stage;
            }
        });
    }

    @Override
    public void logResults(final OutputData results) {
        setStageWithResults(new Callback() {
            Stage call() {
                thread.plannerWrapper.logResults(results);
                return thread.stage;
            }
        });
    }

    @Override
    public InputData getInputAsData() {
        final InputData[] pInput = {null};
        setStageWithResults(new Callback() {
            Stage call() {
                pInput[0] = thread.input;
                return thread.stage;
            }
        });
        return pInput[0];
    }

}
