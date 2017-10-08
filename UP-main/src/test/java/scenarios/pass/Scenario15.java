package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.util.LoggingAssistant;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.Assert.assertTrue;

/**
 * Created by oracle on 15.12.2015.
 * VP-4887: В сценарии проверяется, что подвязка локомотивов под поезда производится
 * с учетом веса поезда и весовых категорий локомотива.
 */
public class Scenario15 extends BaseTest{
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 15 (Учет весовых категорий поездов)";


    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        System.out.println("@@@" + message);
    }

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen15.test");
    }

    private boolean trainWithLoco(Long trainId, Long locoId){
        boolean result = false;
        SlotLoco loco = parser.slotLocos.get(locoId);

        if (loco == null) {
                result = false;
                if (locoId.equals(-1L)){
                    result = true;
                    slotLoco: for (SlotLoco locco: parser.slotLocos.values()){
                        for (SlotLoco.Track track: locco.route){
                            if (track.trainId.equals(trainId)){
                                result = false;
                                break slotLoco;
                            }
                        }
                    }
                }
        } else {
                for (SlotLoco.Track track : loco.route) {
                    if (track.trainId.equals(trainId)) {
                        if (!locoId.equals(-1L))
                            result = true;
                        break;
                    }
                }

                if (locoId.equals(-1L) && !result)
                    result = true;
        }


        if (!result)
            log("Локо " + locoId + " не следует " + " с поездом " + trainId + ",  как предполагает сценарий 15");

        return result;
    }

    @Test
    public void testWeight(){
        /*
        Задано пять поездов:
        поезда 2021 и 2023 с маленькими весами (1600 и 3100);
        поезда 2225 и 2227 с большими весами (5300 и 5400);
        поезд 2829 с очень большим весом (9800).
        Задано пять локомотивов:
        легкие (двухсекционные) локомотивы 206 и 207;
        тяжелые (трехсекционные) локомотивы 280, 290, 499.

        Необходимо, проверить что:
        Поезд 2021 (200020161949) следует с локомотивом 206 (200020161968)
        Поезд 2023 (200020161951) следует с локомотивом 207 (200020161977)
        Поезд 2225 (200020161953) следует с локомотивом 280 (200020161988)
        Поезд 2227 (200020161955) следует с локомотивом 290 (200020162000)
        Поезд 2829 (200020161957) следует без локомотива.
        */
        boolean result = true;

        result = trainWithLoco(200020161949L, 200020161968L) &&
                 trainWithLoco(200020161951L, 200020161977L) &&
                 trainWithLoco(200020161953L, 200020161988L) &&
                 trainWithLoco(200020161955L, 200020162000L) &&
                 trainWithLoco(200020161957L, -1L);

        assertTrue(result);
    }
}
