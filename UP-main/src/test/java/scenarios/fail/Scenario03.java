package scenarios.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.check.output.SlotLocoChecker;
import ru.programpark.planners.util.AnalyzeHelper;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;

public class Scenario03 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 3 (Локомотивы – учет оставшегося времени до ТО)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen03.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    @Test
    public void testCorrectDirection() {//Локомотив едет из Северобайкальска в сторону ПТОЛ
        /*
        Выбрать локомотивы, находящиеся на начало планирования в Северобайкальске (id = 2000036154). Локомотив с меньшим оставшимся временем до
        ТО обозначим как «Локо1», с большим – как «Локо2». Проверить, что Локо1 следует с поездом до станции Лена (id = 2000036932), а
        Локо2 – до станции Новый Уоян (id = 2000036192).
        В случае ошибки вывести id локомотива, id нужной станции, в направлении которой он должен ехать (Лена или Новый Уоян), id фактической станции,
        в направлении которой он едет.
         */
        //assertTrue(checkCorrectDirection(parser.slotLocos));
        assertTrue(SlotLocoChecker.checkCorrectDirection(input, parser.slotLocos));
    }

    @Test
    public void testTimeUntilServiceUtilization() {//Локомотив использует время до ТО по максимум
        /*
        Выбрать локомотивы, находящиеся на начало планирования в Коршунихе (id = 2000036868). Локомотив с меньшим оставшимся временем до ТО
        обозначим как «Локо1», с большим – как «Локо2». Проверить, что Локо1 следует с поездом до станции Тайшет (2000036518), а затем – с поездом до
        станции Вихоревка (2000036796). Локо2 следует с поездом до станции Лена (2000036932).
        В случае ошибки вывести id локомотива, id нужной станции, в направлении которой он должен ехать (Тайшет или Лена), id фактической станции,
        в направлении которой он едет.
         */
        assertTrue(checkTimeUntilServiceUtilization(parser.slotLocos));
    }



    private boolean checkTimeUntilServiceUtilization(Map<Long, SlotLoco> locos) {
        /*
        Выбрать локомотивы, находящиеся на начало планирования в Коршунихе (id = 2000036868).
        Локомотив с меньшим оставшимся временем до ТО обозначим как «Локо1», с большим – как «Локо2».
        Проверить, что Локо1 следует с поездом до станции  Тайшет (2000036518),
        а затем – с поездом до станции Вихоревка (2000036796). Локо2 следует с поездом до станции Лена (2000036932).
        В случае ошибки вывести id локомотива, id нужной станции, в направлении которой он должен ехать (Тайшет или Лена),
        id фактической станции, в направлении которой он едет.
         */
        String testName = "Тест сценария 3 (Локомотив использует время до ТО по максимум)";
        boolean result = true;
        Long inspectedStation = 2000036868L; //Коршунихе
        List<FactLoco> inspectedLocos = AnalyzeHelper.getFactLocoOnStation(input, inspectedStation);
        if  (inspectedLocos.size() < 2) {
            log(testName + " не может быть исполнен: на станции Коршуниха количество локомотивов : "  + inspectedLocos.size());
            return false;
        }

        AnalyzeHelper.sortFactLocosOrderByTimeToService(inspectedLocos);

        {
            SlotLoco loco = locos.get(inspectedLocos.get(0).getId());
            Long firstCheckpointStationTo = 2000036518L; //Тайшет
            Long lastCheckpointStationTo = 2000036796L; //Вихоревка
            boolean checkLoco = false;
            boolean checkFirstCheckpoint = false;
            boolean checkLastCheckpoint = false;
            for (BaseTrack track : loco.route) {
                if (track.stationToId.equals(firstCheckpointStationTo)) {
                    checkFirstCheckpoint = true;
                }
                if (track.stationToId.equals(lastCheckpointStationTo)) {
                    checkLastCheckpoint = true;
                }
            }
            checkLoco = checkFirstCheckpoint && checkLastCheckpoint;
            if (!checkLoco) {
                result = false;
                log(testName + " не пройден");
                if (!checkFirstCheckpoint)
                    log("Локо " + loco.id + " с наименьшим оставшимся временем до ТО в Коршунихе изначально не движется в направлении Тайшет");
                if (!checkLastCheckpoint)
                    log("Локо " + loco.id + " с наименьшим оставшимся временем до ТО в Коршунихе не движется в направлении Вихоревка");
            }
        }

        {
            SlotLoco loco = locos.get(inspectedLocos.get(inspectedLocos.size() - 1).getId());
            Long locoDistination = 2000036932L; //Лена
            boolean checkLoco = false;
            try {
                for (BaseTrack track : loco.route) {
                    if (track.stationToId.equals(locoDistination)) checkLoco = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!checkLoco) {
                if (result)
                    log(testName + " не пройден");
                result = false;
                log("Локо " + loco.id + " с наибольшим оставшимся временем до ТО в Коршунихе не движется в направлении Лена");
            }
        }

        return result;
    }
}
