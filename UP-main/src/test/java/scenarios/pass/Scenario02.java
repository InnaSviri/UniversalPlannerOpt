package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.check.output.SlotLocoChecker;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.Assert.assertTrue;

public class Scenario02 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 2 (Локомотивы – уход по очереди прибытия на станции Тайшет)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }
    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen02.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    @Test
    public void testLocoOrder() throws IOException, URISyntaxException { //3.1.23  Уход локомотивов по очереди готовности
        /*
        Отобрать все локомотивы, которые на начало планирования были на станции Тайшет (id = 2000036518). Дальнейшие проверки проводить только для них.
        Сообщения fact_loco, поступившие на вход планировщика, отсортировать по возрастанию времени готовности локомотива (значение атрибута fact_time).
        Локомотивы на выходе планировщика отсортировать по времени отправления (с Тайшета). Проверить, что порядок отправления локомотивов совпадает с порядком
        отсортированного списка fact_loco.
        В случае ошибки вывести id локомотивов, которые находятся не на своем месте в списке, вывести их время готовности и время отправления.
         */
        Long inspectedStation = 2000036518L; //Тайшет
        assertTrue(SlotLocoChecker.checkLocoOrder_3_1_23(input, parser.slotLocos, input.getStationById(inspectedStation)));
    }

}
