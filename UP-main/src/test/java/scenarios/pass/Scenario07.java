package scenarios.pass;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.team.BaseTeamTrack;
import ru.programpark.entity.util.LoggingAssistant;
import scenarios.BaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Scenario07 extends BaseTest {
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
        onePlan("/scen07.test");
        writer = LoggingAssistant.openWriter("scenarios.log");
    }

    public static boolean checkFirstPassThenWithLoco(SlotTeam team, Long stationId1, Long stationId2){
        if (!(team.route.get(0).stationFromId.equals(stationId1)))
            return false;

        if (!team.passesStation(stationId2))
            return false;

        boolean pass = false, work = false;
        for (SlotTeam.Track track: team.route){
            if (track.state == BaseTeamTrack.State.PASSENGER.ordinal() && track.stationToId.equals(stationId2))
                pass = true;
            if (track.state == BaseTeamTrack.State.AT_WORK.ordinal() && track.stationFromId.equals(stationId2))
                work = true;
        }

        return pass && work;
    }

    // Бригады пересылаются пассажирами для вывоза поездов
    @Test
    public void testTeamsSentAsPassForLoco() {
        /*
        На маршруте двух бригад сначала должно быть следование пассажиром со станции Вихоревка (2000036796) до станций Тайшет (2000036518) и
        Коршуниха (2000036868), а затем – следование с поездом.
        Необходимо, проверить, что 2 бригады следуют пассажиром (twoPass). Затем одна из них сначала пассажиром из Вихоревки до Тайшета(firstTeamStations),
        потом с поездом (firstTeamWithTrain).
        Другая из Вихоревки пассажиром до Коршунихи (secondTeamStations), далее с поездом (SecondTeamWithTrain).
         */
        boolean one = false, two = false;
        for (SlotTeam team: parser.slotTeams.values()){
            if (team.passesStation(2000036518L) && !one)
                one = checkFirstPassThenWithLoco(team, 2000036796L, 2000036518L);
            if (team.passesStation(2000036868L) && !two)
                two = checkFirstPassThenWithLoco(team, 2000036796L, 2000036868L);
        }

        assert(one && two);
    }

    @Ignore
    @Test
    public void testNoTeamChangeOverTime() {// Для бригад не завышено время смены
        /*  Уточнить, нужен ли отдых.
         */
    }
}
