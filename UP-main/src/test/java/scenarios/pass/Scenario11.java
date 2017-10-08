package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.planners.check.output.SlotTeamChecker;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.Assert.assertTrue;

public class Scenario11 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 11 (Учет процентов заезда бригад на участке Тайшет - Вихоревка)";

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen11.test");
    }

    @Test
    public void testTeamPercent() {//Процент заезда на участке Тайшет – Вихоревка
        /*
         Посчитать общее количество запланированных поездов. Найти бригаду, которая едет с каждым поездом, определить для этой бригады депо приписки.
         Вычислить процент поездов, с которыми едет бригада с депо приписки Тайшет (2000036518) и с депо приписки Вихоревка (2000036796).
         Проверить, что процент тайшетских бригад лежит в диапазоне от 20 до 40, а процент вихоревских – от 60 до 80.
         В случае ошибки вывести фактически процент тайшетских и вихоревских бригад.
         */
        TeamRegion region = input.getTeamServiceRegionById(1L); //из дефолтовых данных
        assertTrue(SlotTeamChecker.checkTeamPercent_3_1_16(input, parser.slotTeams, region));
    }
}
