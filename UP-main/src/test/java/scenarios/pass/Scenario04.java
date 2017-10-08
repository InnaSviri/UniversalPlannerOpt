package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.team.TeamPlanningParams;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static junit.framework.Assert.assertTrue;

public class Scenario04 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 4 (Локомотивы – пересылка резервом под поезд, сдвиг поездов)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen04.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    @Test
    public void testUseReserveLoco() { //Локомотивы пересылаются резервом и затем подвязываются по поезд
        /*
        На маршруте каждого локомотива сначала должна следовать пересылка резервом до станции Тайшет (2000036518), а затем – следование с поездом.
        В случае ошибки выводить id локомотивов, для которых наблюдается ошибка.
         */
        boolean result = true;
        String testName = "Тест сценария 4 (Локомотивы пересылаются резервом и затем подвязываются по поезд)";
        Long inspectedStation = 2000036518L;//Тайшет
        for (SlotLoco loco : parser.slotLocos.values()) {
            boolean firstRouteCorrect = false;
            boolean nextRouteCorrect = false;
            for (SlotLoco.Track track : loco.route) {
                if (!firstRouteCorrect) {
                    if (track.stationToId.equals(inspectedStation) && track.state == 0) {
                        firstRouteCorrect = true;
                    }
                }
                if (firstRouteCorrect) {
                    if (track.state == 1) {
                        nextRouteCorrect = true;
                    }
                }

                if (firstRouteCorrect && nextRouteCorrect) {
                    break;
                }
            }
            if (!firstRouteCorrect || !nextRouteCorrect) {
                if (result)
                    log(testName + " не пройден");
                result = false;
                writer = LoggingAssistant.openWriter("resultTestDetails.log");
            }
        }

        assertTrue(result);
    }

    //Для локомотивов не завышено время стоянки
    @Test
    public void testLocoStay() {
        /*
        У каждого локомотива стоянка на станции Тайшет не должна превышать значение
        0.5 * process_time для Тайшета + 5800 секунд (на текущих данных это 4500 + 5800 = 13900).
        В случае ошибки выводить id локомотивов и фактическое время стоянки в Тайшете.
        */
        boolean result = true;
        Long inspectedStation = 2000036518L;//Тайшет
        String testName = "Тест сценария 4 (Для локомотивов не завышено время стоянки)";
        for (SlotLoco loco : parser.slotLocos.values()) {
            BaseTrack priorTrack = null;
            for (BaseTrack track : loco.route) {
                if (priorTrack != null) {
                    if (track.stationFromId.equals(inspectedStation)) {
                        Long processTimeOnStation = input.getStationById(inspectedStation).getProcessTime();
                        Long realStayTimeOnStation = track.timeStart - priorTrack.timeEnd;
                        if (realStayTimeOnStation > (0.5 * processTimeOnStation) + TeamPlanningParams.defaultStopTimeForTeam + 5800) {
                            if (result)
                                log(testName + " не пройден");
                            result = false;
                            log("Превышено время стоянки для локомотива" + loco.id + " фактическое время стоянки" + formatter.format(realStayTimeOnStation/3600.0));
                        }
                    }
                }
                priorTrack = track;
            }
        }
        assertTrue(result);
    }
}
