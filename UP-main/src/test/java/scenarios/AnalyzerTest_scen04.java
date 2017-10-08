package scenarios;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.ShortestPath;
import ru.programpark.planners.handlers.impl.VectorHandlerImpl;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by oracle on 16.10.2015.
 */

public class AnalyzerTest_scen04 extends AnalyzerTest{

    @BeforeClass
    public static void onetimeSetUp()
            throws IOException, URISyntaxException {
        planner = new VectorHandlerImpl();
        planner.createPlanner(false);
        loadFromFile();
        LoggingAssistant.createTestLogDirectory(true);
    }

    public static void loadFromFile()
            throws IOException, URISyntaxException {
        loadPlan("/AnalyzerCheckData/jason-FullPlannerPlugin_sc04.test");
        loadCheckValues("/AnalyzerCheckData/scen04.chd");
    }

    /*@Test
    public void GetDistance() throws URISyntaxException, IOException {
        System.out.println(input.getShortestPath().findDistance(
                input.getStationById(2000036868L), input.getStationById(2000036518L)).longValue()); //2000036228 - Таксимо
    }*/
}

