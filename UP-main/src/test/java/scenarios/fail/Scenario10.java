package scenarios.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.check.output.SlotLocoChecker;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static junit.framework.Assert.assertTrue;

public class Scenario10 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 10 (Данные на вторые сутки – вывоз локомотивов со станций скопления)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen10.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    @Test
    public void testLocoInOddDirection() {// Поток локомотивов в нечетном направлении
        /*
        Посчитать количество поездов, которые соответствуют локомотивам резервом (id < 5000), следующих в нечетном направлении
        (у первого участка планирования на маршруте таких поездов direction = 1). Проверить, что таких поездов > 10.
        В случае ошибки вывести количество таких поездов.
         */
        assertTrue(SlotLocoChecker.checkLocoInOddDirection_3_1_20(input, parser.slotTrains.values()));
    }
}
