package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.raw_entities.SlotTrain;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.util.AnalyzeHelper;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertTrue;

public class Scenario06 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 6 (Бригады – уход на отдых, езда в сторону депо приписки)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen06.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    /**
     * Бригады едут в сторону своего депо приписки
     */
    @Test
    public void testGoToHome() {
        checkCorrectChosenTeam(2000036932L, 2000036796L, 2000036796L); //Лена  -> Вихоревка
        checkCorrectChosenTeamFromList(2000036868L, 2000036518L, 2000036796L, 2000036518L);
        //Коршуниха -> Тайшет, Вихоревка, Тайшет
    }

    public boolean checkCorrectChosenTeam(Long stationFrom, Long stationTo, Long expectedDepot) {
        /*
        Проверить, что к поезду, который следует на маршруте Лена (2000036932) – Вихоревка (2000036796)
        привязана бригада с депо приписки Вихоревка.
        Проверить, что к поезду, который следует на маршруте Коршуниха (2000036868) – Тайшет (2000036518)
        привязаны сначала бригада с депо приписки
        Вихоревка, а затем бригада с депо приписки Тайшет.
         */
        boolean result = true;
        String testName = "Тест сценария 6 часть 1 (Проверить, что к поезду, который следует на маршруте " +
                "Лена (2000036932) – Вихоревка (2000036796) привязана бригада с депо приписки Вихоревка.)";
        SlotTrain chosenTrain = findTrainPassingOverStations(stationFrom, stationTo);
        if (chosenTrain == null) {
            result = false;
            log("Не найден поезду, следующий на участке " + stationFrom + "->" + stationTo);
            return result;
        }
        for (BaseTrack track : chosenTrain.route) {
            //Нашли 1-ый трек на наблюдаемом крупном участке, в нем должна быть использована бригада
            // с ожидаемым депо приписки
            if (track.stationFromId.equals(stationFrom)) {
                SlotTeam team = AnalyzeHelper.getSlotTeamBySlotTrainAndTrack(chosenTrain, track,
                        parser.slotLocos.values(), parser.slotTeams.values());
                if (team == null) {
                    if (result)
                        log(testName + " не пройден");
                    result = false;
                    log("У поезда id: " + chosenTrain.id + " следующего по участку " + stationFrom + "->"
                            + stationTo + " не найдена бригада");
                } else {
                    FactTeam fact = input.getFactTeams().get(team.id);
                    if (!fact.getDepot().getId().equals(expectedDepot)) {
                        if (result)
                            log(testName + " не пройден");
                        result = false;
                        log("Бригада следует не в свое депо приписки, поезд id: " + chosenTrain.id +
                                " на участке " + stationFrom + "->" + stationTo
                                + " бригада id " + team.id + " с депо приписки " + fact.getDepot().getId());
                    }
                }
            }
        }
        return result;
    }

    public boolean checkCorrectChosenTeamFromList(Long stationFrom, Long stationTo,
                                                  Long expectedDepotOne, Long expectedDepotTwo) {
        boolean result = true;
        SlotTrain chosenTrain = findTrainPassingOverStations(stationFrom, stationTo);
        String testName = "Тест сценария 6 часть 2 (Проверить, что к поезду, который следует на маршруте " +
                "Коршуниха (2000036868) – Тайшет (2000036518) привязаны сначала бригада с депо приписки\n" +
                "        Вихоревка, а затем бригада с депо приписки Тайшет.)";
        if (chosenTrain == null) {
            result = false;
            log("Не найден поезд, следующий на участке " + stationFrom + "->" + stationTo);
            return result;
        }

        SlotTeam firstTeam = null;
        SlotTeam secondTeam = null;
        boolean firstTeamCorrect = false;
        boolean secondTeamCorrect = false;
        for (BaseTrack track : chosenTrain.route) {
            SlotTeam team = AnalyzeHelper.getSlotTeamBySlotTrainAndTrack(chosenTrain, track,
                    parser.slotLocos.values(), parser.slotTeams.values());
            if (team == null) {
                if (result)
                    log(testName + " не пройден");
                result = false;
                log("У поезда id: " + chosenTrain.id + " следующего по участку " + stationFrom +
                        " - " + stationTo + " не найдена бригада");
            }

            if (firstTeam == null) {
                firstTeam = team;
            }
            if (firstTeam != null && !firstTeam.id.equals(team.id)) {
                secondTeam = team;
            }
        }

        if (firstTeam != null) {
            FactTeam fact = input.getFactTeams().get(firstTeam.id);
            firstTeamCorrect = fact.getDepot().getId().equals(expectedDepotOne);
        } else {
            if (result)
                log(testName + " не пройден");
            result = false;
            log("Первая бригада у поезда " + chosenTrain.id + " не обнаружена");
        }

        if (secondTeam != null) {
            FactTeam fact = input.getFactTeams().get(secondTeam.id);
            secondTeamCorrect = fact.getDepot().getId().equals(expectedDepotTwo);
        } else {
            if (result)
                log(testName + " не пройден");
            result = false;
            log("Вторая бригада у поезда " + chosenTrain.id + " не найдена");
        }

        if (!firstTeamCorrect || !secondTeamCorrect) {
            if (result)
                log(testName + " не пройден");
            result = false;
            if(!firstTeamCorrect){
                log("Первая бригада " + firstTeam.id + " не из Вихоревки (депо приписки)");
            }
            if (!secondTeamCorrect){
                log("Вторая бригада " + secondTeam.id + " не из Тайшета (депо приписки)");
            }
        }

        return result;
    }

    public SlotTrain findTrainPassingOverStations(Long stationFrom, Long stationTo) {
        SlotTrain chosenTrain = null;

        for (SlotTrain train : parser.slotTrains.values()) {
            boolean foundStationFrom = false;
            boolean foundStationTo = false;
            for (BaseTrack track : train.route) {
                if (!foundStationFrom) {
                    if (track.stationFromId.equals(stationFrom)) {
                        foundStationFrom = true;
                    }
                }
                if (foundStationFrom) {
                    if (track.stationToId.equals(stationTo)) {
                        foundStationTo = true;
                        break;
                    }
                }
            }
            if (foundStationFrom && foundStationTo) {
                chosenTrain = train;
                break;
            }
        }

        return chosenTrain;
    }

    /**
     * Бригады уходят на отдых и выходят на работу после отдыха
     */
    @Test
    public void testWorkAndRestRotation() {
        /*
        Бригады уходят на отдых и выходят на работу после отдыха
        Проверить, что как минимум для двух бригад планируется следующий режим работы: езда с поездом
        (state = 1), отдых (state = 4), снова езда с поездом (state = 1).
         */
        String testName = "Тест сценария 6 (Бригады уходят на отдых и выходят на работу после отдыха)";
        boolean result = true;
        int counter = 0;
        Set<Long> teams = new HashSet<>();

        for (SlotTeam team : parser.slotTeams.values()) {
            boolean firstWork = false;
            boolean rest = false;
            boolean secondWork = false;
            for (SlotTeam.Track track : team.route) {
                if (track.state == 1) firstWork = true;
                if (firstWork && track.state == 4) rest = true;
                if (rest && track.state == 1) secondWork = true;
            }

            if (firstWork && rest && secondWork) {
                counter++;
                teams.add(team.id);
            }
        }
        if (counter < 2) {
            if (result)
                log(testName + " не пройден");
            result = false;
            log("Обнаружено " + counter + " бригад с режимом работы (с поездом, отдых, с поездом). Id бригад: ");
            for (Long id: teams){
                log(id.toString());
            }
        }

        assertTrue(result);
    }
}
