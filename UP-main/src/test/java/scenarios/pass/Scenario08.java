package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.check.output.SlotTrainChecker;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static junit.framework.Assert.assertTrue;

public class Scenario08 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 8 (Поезда – уход по очереди готовности)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen08.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    @Test
    public void testAllTrainOrder() {// Поезда отправляются в правильном порядке
              /*
        Отсортировать поезда во входных данных по времени факта (прибытие arrive_time – для поездов с train_arrive,
        готовность fact_time – для поездов с train_ready).
        Отсортировать поезда на выходе планировщика по времени отправления. Проверить, что порядок отправления
        поездов соответствует порядку в отсортированном списке
        поездов из входных данных.
        В случае ошибки вывести id поездов, которые находятся не на своем месте в списке выходных поездов,
        для каждого поезда указать время из входного факта и
        время отправления.
         */
        assertTrue(SlotTrainChecker.checkAllTrainOrder_3_1_21(input, parser.slotTrains.values()));
    }
}
