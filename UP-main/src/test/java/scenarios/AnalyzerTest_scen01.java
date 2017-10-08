package scenarios;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.data.*;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.ResultParser;
import ru.programpark.planners.handlers.VectorHandler;
import ru.programpark.planners.handlers.impl.VectorHandlerImpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Created by oracle on 16.10.2015.
 */

public class AnalyzerTest_scen01 extends AnalyzerTest{

    @BeforeClass
    public static void onetimeSetUp()
            throws IOException, URISyntaxException {
        planner = new VectorHandlerImpl();
        planner.createPlanner(false);
        loadFromFile();
        LoggingAssistant.createTestLogDirectory(true);
    }

    public static void loadFromFile()                       // overridden in descendants
            throws IOException, URISyntaxException {
        //loadPlan("/AnalyzerCheckData/jason-FullPlannerPlugin_sc01.test");
        loadPlan("/2_11.test");
        loadCheckValues("/AnalyzerCheckData/scen01.chd");
    }

    @Test
    public void a() {}
}
