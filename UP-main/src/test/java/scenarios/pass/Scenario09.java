package scenarios.pass;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.raw_entities.SlotTrain;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.TrainDepart;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static junit.framework.Assert.assertTrue;

public class Scenario09 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String testName = "Сценарий 9 (Учет фактически привязанных локомотивов и бригад)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        System.out.println("@@@" + message);
    }

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen09.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    //Количество поездов, локомотивов и бригад на выходе соответствует количеству на входе
    @Test
    public void testAllTrainsLocosTeamsAssigned() {
        /*
        Посчитать количество поездов, локомотивов и бригад во входных данных. Проверить, что эти количества полностью совпадают с количествами поездов,
        локомотивов и бригад на выходе.
        В случае ошибки вывести id поездов (локомотивов, бригад) которые присутствуют на входе и не присутствуют на выходе и наоборот.

         */
        boolean result = true;

        for (FactTrain factTrain : input.getFactTrains().values()) {
            SlotTrain train = parser.slotTrains.get(factTrain.getId());
            if (train == null) {
                if (result)
                    log(testName + " не пройден");
                result = false;
                log("Поезд с id: " + factTrain.getId() + " отсутствует в выходных данных");
            } else if (factTrain.getTrainState() instanceof TrainDepart){
                if (!train.route.get(0).timeStart.equals(factTrain.getTrainState().getTime())){
                    if (result)
                        log(testName + " не пройден");
                    result = false;
                    log("Поезд с id: " + factTrain.getId() + " отправлен в " +
                            (new Time(train.route.get(0).timeStart)).getTimeStamp() +
                            ", а по факту он ушел в " + factTrain.getTrainState().getTime());
                }
            }
        }

        for (FactLoco factLoco : input.getFactLocos().values()) {
            SlotLoco loco = parser.slotLocos.get(factLoco.getId()) ;
            if (loco == null) {
                if (result)
                    log(testName + " не пройден");
                result = false;
                log("Локомотив с id: " + factLoco.getId() + " отсутствует в выходных данных");
            } else {
                FactTrain train = input.getFactTrainById(loco.route.get(0).trainId);
                if (train != null && train.getTrainState() instanceof TrainDepart &&
                        !loco.route.get(0).timeStart.equals(train.getTrainState().getTime())){
                    if (result)
                        log(testName + " не пройден");
                    result = false;
                    log("Локо с id: " + factLoco.getId() + " отправлен в " +
                            (new Time(loco.route.get(0).timeStart)).getTimeStamp() +
                            ", а поезд, который он тянет, по факту ушел в " + train.getTrainState().getTime());
                }
            }
        }

        for (FactTeam factTeam : input.getFactTeams().values()) {
            SlotTeam team = parser.slotTeams.get(factTeam.getId());
            if (team == null) {
                if (result)
                    log(testName + " не пройден");
                result = false;
                log("Бригада с id: " + factTeam.getId() + " отсутствует в выходных данных");
            } else {
                FactLoco loco = input.getFactLocoById(team.route.get(0).locoId);
                if (loco != null) {
                    FactTrain train = input.getFactTrainById(loco.getTrainId());
                    if (train != null && train.getTrainState() instanceof TrainDepart &&
                            !team.route.get(0).timeStart.equals(train.getTrainState().getTime())) {
                        if (result)
                            log(testName + " не пройден");
                        result = false;
                        log("Бригада с id: " + factTeam.getId() + " отправлена в " +
                                (new Time(team.route.get(0).timeStart)).getTimeStamp() +
                                ", а поезд, который везет бригада, по факту ушел в " + train.getTrainState().getTime());
                    }
                }
            }
        }

        assertTrue(result);
    }

    // Проверить, что проценты привязки локомотивов к поездам и бригад к локомотивам на всех горизонтах равны 100%.
    @Test
    public void testPercentOfAssignedTrainsLocosAndTeams() {
        double criterion2 = parser.criterions.get(2L).getValue();//% запланированных поездов
        double criterion5 = parser.criterions.get(5L).getValue();//% поездов запланированных, обеспеченных локомотивами  на горизонте в 24 часов
        double criterion6 = parser.criterions.get(6L).getValue();//% запланированных локомотивов, обеспеченных бригадами на горизонте в 24 часов
        Assert.assertTrue(Math.abs(criterion2 - 100.0) < 0.000001);
        Assert.assertTrue(Math.abs(criterion5 - 100.0) < 0.000001);
        Assert.assertTrue(Math.abs(criterion6 - 100.0) < 0.000001);
    }
}
