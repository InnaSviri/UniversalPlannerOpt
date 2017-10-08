package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.raw_entities.SlotTrain;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.util.AnalyzeHelper;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class Scenario01 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 1 (Локомотивы – время хода, тяговые плечи, уход на ТО, повторное использование)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }
    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen01.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    /**
     * Локомотивы не выходят за пределы своих тяговых плеч
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    public void testLocoRegion() throws URISyntaxException, IOException {
        assertTrue(checkLocoRegion(parser.slotLocos.values()));
    }

    @Test
    public void testCorrectLocoChange() {
        assertTrue(checkCorrectLocoChange(parser.slotTrains.values(), parser.slotLocos.values()));
    }

    @Test
    public void testCorrectWorkingTime() {
        assertTrue(checkCorrectWorkingTime(parser.slotLocos.values()));
    }

    private boolean checkLocoRegion(Collection<SlotLoco> locos) {
        /*
        Для каждого участка планирования, входящего в маршрут локомотива, проверить, что этот участок входит хотя бы
        в одно тяговое плечо, заданное для этого локомотива. Повторить проверку для всех локомотивов.
        В случае ошибки вывести id локомотивов, для которых наблюдается ошибка. Для каждого локомотива вывести список
        ошибочных участков планирования (начальная станция, конечная станция) на маршруте.
        */
        String testName = "Тест сценария (Локомотивы не выходят за пределы своих тяговых плеч) ";
        boolean result = true;
        for (SlotLoco loco : locos) {
            FactLoco factLoco = input.getFactLocoById(loco.id);
            Set<StationPair> pairs = new HashSet<>();
            for (LocoRegion region : factLoco.getLocoRegions()) {
                for (Link link : input.getLinksByLocoRegion(region)) {
                    pairs.add(new StationPair(link));
                }
            }
            for (SlotLoco.Track track : loco.route) {
                if (track.state == 1 && !pairs.contains(new StationPair(track.stationFromId, track.stationToId))) {
                    if (result)
                        log(testName + " не пройден");
                    result = false;
                    log("Локомотив вышел за пределы своих тяговых плечей id: " + loco.id + " на участке " + track.stationFromId + "->" + track.stationToId);
                }
            }
        }

        return result;
    }

    private boolean checkCorrectLocoChange(Collection<SlotTrain> trains, Collection<SlotLoco> locos) {
        /*
        Для каждого поезда находим станции, на которых для этого поезда происходила смена локомотива.
        Берем участок планирования на маршруте поезда до станции смены и сразу после. Проверяем, что нет тягового плеча,
        в которое входят оба эти участка.В случае ошибки вывести id ошибочных поездов и станции смены локомотивов,
        на которых наблюдалась ошибка.
         */
        boolean result = true;
        String testName = "Тест сценария (Смена локомотива происходит на границе тяговых плеч)";
        //Идем по поезду, смотрим, какой локомотив был на предыдущем шаге
        for (SlotTrain train : trains) {
            BaseTrack previousTrack = null;
            for (BaseTrack track : train.route) {
                if (previousTrack != null) {
                    SlotLoco previousLoco = AnalyzeHelper.getSlotLocoBySlotTrainAndTrack(train, previousTrack, locos);
                    SlotLoco currentLoco = AnalyzeHelper.getSlotLocoBySlotTrainAndTrack(train, track, locos);
                    if ((previousLoco != null && currentLoco != null) //на этом треке был прицеплен другой локо
                            && !previousLoco.id.equals(currentLoco.id)) {
                        Collection<LocoRegion> previousLocoRegions =
                                getLocoRegionsByStationPair(new StationPair(previousTrack.stationFromId, previousTrack.stationToId));
                        Collection<LocoRegion> currentLocoRegions =
                                getLocoRegionsByStationPair(new StationPair(track.stationFromId, track.stationToId));
                        previousLocoRegions.retainAll(currentLocoRegions);
                        if (previousLocoRegions.size() > 0) {
                            result = false;
                        }
                    }
                }
                previousTrack = track;
            }
        }

        if (!result)
            log(testName + " не пройден");

        return result;
    }

    private Collection<LocoRegion> getLocoRegionsByStationPair(StationPair pair) {
        Collection<LocoRegion> foundRegions = new HashSet<>();
        Link link = input.getLinkByStationPair(pair);
        for (LocoRegion locoRegion : input.getLocoRegions().values()) {
            if (input.getLinksByLocoRegion(locoRegion).contains(link)) {
                foundRegions.add(locoRegion);
            }
        }
        return foundRegions;
    }

    private boolean checkCorrectWorkingTime(Collection<SlotLoco> locos) {
        /*
        На маршруте локомотива найти участок со state = 4 (прохождение ТО).
        Из предыдущих участков маршрута выбрать последний со state = 1, определить время прибытия на конечную станцию этого участка.
        Проверить, что разность между этим найденным временем и временем начала планирования не превышает переданное во
        входных данных время локомотива до ТО.
        В случае ошибки вывести id ошибочных локомотивов, их время работы (вычисленная разность из предыдущего абзаца) и переданное время до ТО.
        */
        boolean result = true;
        String testName = "Тест сценария (Рабочее время локомотива не превышает оставшееся время до ТО)";

        for (SlotLoco loco : locos) {
            List<SlotLoco.Track> buffer = new ArrayList<>();
            SlotLoco.Track previousTrack = null;
            for (SlotLoco.Track track : loco.route) {
                if (previousTrack != null && track.state == 4) {
                    break;
                }
                buffer.add(track);
                previousTrack = track;
            }
            if (buffer.size() !=  loco.route.size() && buffer.size() > 0) {
                FactLoco factLoco = input.getFactLocoById(loco.id);
                for (int i = buffer.size() - 1; i >= 0; i--) {
                    SlotLoco.Track track = buffer.get(i);
                    if (track.state == 1) {
                        if (track.timeEnd - input.getCurrentTime() > factLoco.getTimeToService()) {
                            result = false;
                        }
                    }
                }
            }
        }

        if (!result)
            log(testName + " не пройден");

        return result;
    }


}
