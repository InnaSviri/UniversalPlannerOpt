package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.check.output.SlotTeamChecker;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class Scenario05 extends BaseTest {
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    String scenarioName = "Сценарий 5 (Бригады – обкатка, учет рабочего времени, очередь прибытия)";

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        onePlan("/scen05.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    /**
     * Бригады не выходят за пределы своих участков обкатки
     * Для каждого участка планирования, входящего в маршрут бригады (кроме участков со state = 4),
     * проверить, что этот участок входит хотя бы в один участок обкатки, заданный для бригады.
     */
    @Test
    public void testTeamWorkingOnCorrectTeamRegion() {
        /*boolean result = true;
        String testName = "Тест сценария 5 (Бригады не выходят за пределы своих участков обкатки)";

        for (SlotTeam team : parser.slotTeams.values()) {
            for (SlotTeam.Track track : team.route) {
                if (track.state != 4 && !canUseThisTrack(team.id, track)) {
                    if (result)
                        log(testName + " не пройден");
                    result = false;
                    log("Бригада следует по участку маршрута, не входящему в участок обкатки teamId: " + team.id + " участок " + track.stationFromId + "->" + track.stationToId);
                }
            }
        } */

        assertTrue(SlotTeamChecker.checkTeamsWorkWhereTheyAreAllowed_3_1_28(input, parser.slotTeams));
    }

    private boolean canUseThisTrack(Long teamId, BaseTrack track) {
        FactTeam factTeam = input.getFactTeams().get(teamId);
        for (TeamRegion teamRegion : factTeam.getTeamWorkRegions()) {
            for (StationPair pair : teamRegion.getStationPairs()) {
                if (track.stationFromId.equals(pair.stationFromId) &&
                        track.stationToId.equals(pair.stationToId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Не превышается рабочее время бригад
     */
    @Test
    public void testTimeUntilRestUtilization_3_1_17() { //3.1.17  Не превышается рабочее время бригад
         /*
         Для каждой бригады требуется выделить этапы ее работы между отдыхом (участок маршрута со state = 4).
         Для каждого этапа определить время отправления на этот этап (это либо время отправления бригады на самый первый участок маршрута,
         либо время отправления бригады на первый участок после отдыха) и время завершения этапа (это либо время прибытия на последний участок маршрута,
         либо время прибытия на участке перед отдыхом). Для каждого этапа работы рассчитать длительность (разность между временем прибытия и временем отправления на этап),
         проверить, что она не превышает 10 часов.
         В случае ошибки вывести id бригад, начальную и конечную станцию этапа работы с превышением и длительность этого этапа.
         */
        assertTrue(SlotTeamChecker.checkTeamsDoNotExceedWorkingTimeLimit_3_1_17(parser.slotTeams.values()));
    }

    /**
     * Уход бригад по порядку готовности
     */
    @Test
    public void testTeamUsedOrder() {
        /*
        Сообщения fact_team, поступившие на вход планировщика, отсортировать по возрастанию времени готовности бригады (значение атрибута fact_time).
        Бригады на выходе планировщика отсортировать по времени отправления. Проверить, что порядок отправления бригад совпадает с порядком отсортированного списка fact_team.
        Эту проверку провести только для бригад, которые на начало планирования находились на станциях Тайшет (2000036518) и Вихоревка (2000036796).
        Проверку проводить отдельно для бригад с каждой из этих станций.
        В случае ошибки вывести id бригад, которые находятся не на своем месте в списке, вывести их время готовности и время отправления.
         */
        checkTeamUsedOrderOnStation(2000036518L); //Тайшет
        checkTeamUsedOrderOnStation(2000036796L); //Вихоревка
    }

    private boolean checkTeamUsedOrderOnStation(Long stationId) {
        boolean result = true;
        List<FactTeam> facts = new ArrayList<>();
        List<SlotTeam> actuals = new ArrayList<>();
        for (FactTeam fact : input.getFactTeams().values()) {
            if (fact.getStation() != null && fact.getStation().getId().equals(stationId)) {
                facts.add(fact);
            }
        }
        Collections.sort(facts, new Comparator<FactTeam>() {
            @Override
            public int compare(FactTeam o1, FactTeam o2) {
                if (o1 == null || o2 == null) return 0;
                return o1.getTimeOfFact().compareTo(o2.getTimeOfFact());
            }
        });

        for (FactTeam fact : facts) {
            actuals.add(parser.slotTeams.get(fact.getId()));
        }

        if (facts.size() != actuals.size()){
            log("Количество фактических и привязанных бригад не совпадает, поэтому нет смысла проверять порядок ухода");
            return false;
        }

        Collections.sort(actuals, new Comparator<SlotTeam>() {
            @Override
            public int compare(SlotTeam o1, SlotTeam o2) {
                if (o1 == null || o2 == null) return 0;
                return o1.route.get(0).timeStart.compareTo(o2.route.get(0).timeStart);
            }
        });

        for (int i = 0; i < facts.size(); i++) {
            if (actuals.get(i) == null) {
                log("Бригада teamId: " + facts.get(i).getId() + " не использована при назначениях");
                continue;
            }
            if (!facts.get(i).getId().equals(actuals.get(i).id)) {
                result = false;
                FactTeam actualTeam = input.getFactTeams().get(actuals.get(i).id);
                log("Бригада не на своем месте в списке teamId: " + actuals.get(i).id + " время готовности " + actualTeam.getTimeOfFact()
                        + " время отправления " + actuals.get(i).route.get(0).timeStart);
            }
        }

        return result;
    }
}
