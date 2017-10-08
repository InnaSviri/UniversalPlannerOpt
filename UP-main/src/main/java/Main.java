import org.apache.log4j.LogManager;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.data.OutputData;
import ru.programpark.entity.data.Percepts;
import ru.programpark.entity.data.Version;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.handlers.VectorHandler;
import ru.programpark.planners.handlers.impl.VectorHandlerImpl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * User: olga
 * Date: 1.06.15
 * В качестве параметра жарнику передается имя входного файла
 */
public class Main {
    private static Main instance = new Main();

    public static void main(Main inst, String... args) {
        instance = inst;
        main(args);
    }

    public static void main(String... args) {
        List<String> files = new ArrayList();
        Properties logProps = loadLogProps(null, null);
        int i = 0;
        boolean summary = false, serial = false, corr = false, version = false;
        while (i < args.length) {
            if (args[i].charAt(0) == '-') {
                if (args[i].equals("-v") || args[i].equals("-version")) {
                    version = true;
                } else if (args[i].equals("-m") || args[i].equals("-serial")) {
                    //запуск нескольких файлов последовательно
                    serial = true;
                } else if (args[i].equals("-c") || args[i].equals("-corrections")) {
                    //запуск нескольких файлов последовательно в режиме корректировок
                    serial = corr = true;
                } else if (args[i].equals("-s") || args[i].equals("-summary")) {
                    summary = true;
                } else if (i >= args.length - 1) {
                    System.err.println("Missing argument for " + args[i]);
                    System.exit(1);
                } else if (args[i].equals("-log4j")) {
                    logProps = loadLogProps(args[++i], logProps);
                } /* ... */ else {
                    System.err.println("Unknown option: " + args[i] + " (ignored)");
                }
            } else {
                files.add(args[i]);
            }
            ++i;
        }
        if (version) {
            System.out.println(Version.string());
        }
        if (files.size() > 0) {
            LoggingAssistant.setLoggerProperties(logProps, false);
            instance.run(files, summary, serial, corr);
            System.exit(0);
        } else if (!version) {
            System.err.println("No input files were specified in args");
            System.exit(1);
        }
    }

    public static Properties loadLogProps(String file, Properties defaults) {
        Properties props = new Properties(defaults);
        try {
            InputStream in;
            if (file == null) {
                file = "/" + LogManager.DEFAULT_CONFIGURATION_FILE;
                in = Main.class.getResourceAsStream(file);
            } else {
                in = new FileInputStream(file);
            }
            if (in != null) props.load(in);
        } catch (Exception e) {
            System.err.println("Error loading config props from " + file);
        }
        return props;
    }

    public VectorHandler planner;
    private InputData iData;
    private OutputData oData;

    private Long interruptAt = null;
    private boolean setInterruptAt() {
        if (interruptAt == null) {
            interruptAt = Long.MAX_VALUE;
            long interrupt = -1L;
            String interruptProp = System.getProperty("planners.interrupt.after");
            if (interruptProp != null) {
                try {
                    interrupt = Long.parseLong(interruptProp);
                    System.out.println("Main.oneRun: Планирование будет прервано через " +
                                       interrupt + " сек.");
                    interruptAt = System.currentTimeMillis() + interrupt * 1000;
                } catch (NumberFormatException e) {}
            }
        }
        return (interruptAt < Long.MAX_VALUE);
    }

    public void run(List<String> files, boolean summary, boolean serial, boolean corr) {
        planner = new VectorHandlerImpl();
        planner.createPlanner(setInterruptAt());
        if (serial) {
            boolean firstTime = true;
            for (String file : files) {
                for (int batchIndex = 0;; ++batchIndex) {
                    Percepts percepts = new Percepts(file, batchIndex);
                    if (percepts.areEmpty()) {
                        if (batchIndex == 0)
                            System.out.println("Main.run: Файл " + file + " не содержит данных");
                        break;
                    }
                    if (firstTime) firstTime = false;
                    else if (! corr) planner.createPlanner(setInterruptAt());
                    System.out.println("Main.run: Планирование по данным " + file +
                                           " [" + batchIndex + "]");
                    oneRun(percepts, summary);
                    System.gc();
                    try { Thread.sleep(500); } catch (InterruptedException  e) {}
                }
            }
        } else {
            oneRun(new Percepts(files.toArray(new String[files.size()])), summary);
        }
        planner.removePlanner();
    }

    public void oneRun(Percepts percepts, boolean summary) {
        System.out.println("Main.oneRun: Начало цикла операций планировщика");
        planner.startPlanning(percepts.getAddPercepts(), percepts.getDelPercepts());
        iData = planner.getInputAsData();
        String[] results;
        while ((results = planner.getResults()) == null) {
            if (System.currentTimeMillis() > interruptAt) {
                break;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                LoggingAssistant.logException(e);
            }
        }
        if (results != null) {
            /*if (!summary) {
                for (String str: results) {
                    System.out.println(str);
                }
            } */
            oData = planner.getResultsAsData();
        }
        System.out.println("Main.oneRun: Конец цикла операций планировщика");
    }

}
