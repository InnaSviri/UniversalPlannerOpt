package scenarios;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.data.OutputData;
import ru.programpark.entity.data.Percepts;
import ru.programpark.entity.util.ResultParser;
import ru.programpark.planners.check.output.SlotLocoChecker;
import ru.programpark.planners.check.output.SlotTeamChecker;
import ru.programpark.planners.check.output.SlotTrainChecker;
import ru.programpark.planners.check.output.TrackChecker;
import ru.programpark.planners.handlers.VectorHandler;
import ru.programpark.planners.handlers.impl.VectorHandlerImpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class BaseTest {
    protected static VectorHandler planner;
    protected static ResultParser parser;
    protected static InputData input;
    protected static OutputData output;
    protected static PrintWriter writer;

    @BeforeClass
    public static void onetimeSetUp() throws IOException, URISyntaxException {
        planner = new VectorHandlerImpl();
        planner.createPlanner(false);
    }

    @AfterClass
    public static void oneTimeTearDown() {
        planner.removePlanner();
        planner = null;
        if (writer != null)
            writer.close();
    }

    protected static void onePlan(String resourceName) throws URISyntaxException, IOException {
        URL resourceUrl = BaseTest.class.getResource(resourceName);
        Path resourcePath = Paths.get(resourceUrl.toURI());
        Percepts percepts = new Percepts(resourcePath.toString());

        planner.startPlanning(percepts.getAddPercepts(), percepts.getDelPercepts());

        String[] results;
        while ((results = planner.getResults()) == null) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        parser = new ResultParser(Arrays.asList(results));
        input = planner.getInputAsData();
        output = planner.getResultsAsData();

        //LoggingAssistant.createTestLogDirectory(true);
    }

    @Test
    public void testCorrectTimeOnTrack_3_1_1() {//3.1.1 Отсутствие скачков во времени на перегонах
        /*
        Для всех поездов, локомотивов и бригад время прибытия на конечную станцию каждого трека больше
        времени отправления.
        В случае ошибки выводить тип сущности (поезд, локомотив, бригада), его id и участок маршрута,
        на котором произошла ошибка.
        Если для одной сущности случается несколько ошибок (на нескольких участках), то выводить все участки.
         */
        assertTrue(TrackChecker.checkCorrectTimeOnTrack_3_1_1(parser.slotTrains.values(),
                parser.slotLocos.values(), parser.slotTeams.values()));
    }

    /**
     * Отсутствие скачков во времени на стоянках
     */
    @Test
    public void testCorrectTimeOnStay_3_1_2() {//3.1.2  Отсутствие скачков во времени на стоянках
        /*
        Для всех поездов, локомотивов и бригад время отправления на следующий трек не меньше времени
        прибытия на предыдущий трек.
        В случае ошибки выводить тип сущности (поезд, локомотив, бригада), его id и станцию,
        на которой произошла ошибка.
        Если для одной сущности случается несколько ошибок (на нескольких станциях), то выводить все станции.
         */
        assertTrue(TrackChecker.checkCorrectTimeOnStay_3_1_2(parser.slotTrains.values(),
                parser.slotLocos.values(), parser.slotTeams.values()));
    }

    @Test
    public void testEqualsTimeInTrainAndLoco_3_1_3() {//3.1.3 Совпадение времен у поездов и локомотивов
        /*
        Если на участке маршрута локомотива указана ссылка на поезд, то времена отправления и прибытия на
        участок у локомотива должны совпадать с
        временами отправления и прибытия на этот же участок у этого поезда.
        В случае ошибки выводить пару id «поезд – локомотив», а также участок, на котором произошла ошибка,
        и времена отправления и прибытия,
        указанные для поезда и локомотива. Если для одной пары «поезд – локомотив» ошибка происходит на
        нескольких участках, то выводить их все.
         */
        assertTrue(TrackChecker.checkEqualsTimeInTrainAndLoco_3_1_3(parser.slotTrains, parser.slotLocos.values()));
    }

    @Test
    public void testEqualsTimeInLocoAndTeam_3_1_4() { //3.1.4 Совпадение времен у локомотивов и бригад
        /*
        Если на участке маршрута бригады указана ссылка на локомотив, то времена отправления и прибытия бригады на
        участок должны совпадать с временами
        отправления и прибытия этого локомотива на этот участок.
        Может случиться так, что локомотив несколько раз на своем маршруте проследует по этому участку.
        В этом случае проверять, что есть хотя бы один
        участок маршрута локомотива (совпадающий с участком бригады), на котором времена отправления и
        прибытия совпадают с указанными у бригады.
        В случае ошибки выводить пару id «локомотив – бригада», а также участок, на котором произошла ошибка,
        и времена отправления и прибытия, указанные для локомотива
        и бригады. Если для одной пары «локомотив – бригада» ошибка происходит на нескольких участках,
        то выводить их все.
         */
        assertTrue(TrackChecker.checkEqualsTimeInLocoAndTeam_3_1_4(input, parser.slotLocos, parser.slotTeams.values()));
    }

    @Test
    public void testLongStayForLocoChange_3_1_5() {//3.1.5  (Долгое время стоянок поездов на станциях смены локомотивов)
        /*
        На маршруте поезда выделить станции смены локомотивов. Разность между временем отправления поезда со станции
         смены локомотива и временем прибытия поезда
        на станцию смены локомотива не должно превышать 4 часа.
        Значение 4 часа выбрано сейчас произвольно (из следующих соображений: около 2 часов собственно на смену,
        час на поиск нитки, еще час на разные флуктуации).
        Возможно, затем его надо будет подкорректировать (например, согласовать с process_time и количеством ниток
        на участке, куда отправляется поезд после смены).
         */
        assertTrue(SlotLocoChecker.checkLongStayForLocoChange_3_1_5(parser.slotTrains.values(),
                parser.slotLocos.values()));
    }

    @Test
    public void testLongStayForTeamChange_3_1_6() {  //3.1.6 (Долгое время стоянок поездов на станциях смены бригад)
        /*
        На маршруте поезда выделить станции смены бригад. Разность между временем отправления поезда со станции
        смены бригады и временем прибытия поезда на
        станцию смены бригады не должно превышать 3 часов.
        Значение 3 часа выбрано сейчас произвольно (из следующих соображений: чуть меньше часа на смену бригады,
        час на поиск нитки, еще час на разные флуктуации).
        Возможно, затем его надо будет подкорректировать (например, согласовать с norm_time и количеством ниток
        на участке, куда отправляется поезд после смены).
        В случае ошибки выводить id поезда, станцию, время прибытия, время отправления и длительность стоянки.
         */
        assertTrue(SlotTeamChecker.checkLongStayForTeamChange_3_1_6(parser.slotTrains.values(),
                parser.slotLocos.values(), parser.slotTeams.values()));
    }

    @Test
    public void testAbnormalLength_3_1_7() {//3.1.7 Затянутые и слишком быстрые нитки графика
        /*
        Для каждого крупного участка (см. приложение) выбрать поезда, в маршруте которых есть этот участок
        (в маршруте есть обе станции в нужном порядке).
        Для каждого поезда посчитать время хода на этом крупном участке (разность времени прибытия на конечную
        станцию и времени отправления с начальной).
        Далее надо среди этих времен хода найти выбросы. Для этого для списка времен хода найти квартили.
        Проверить, что времена хода всех поездов по данному участку
        попадают в интервал (x_25-1.5∙(x_75-x_25 );x_75+1.5∙(x_75-x_25)).
        Квартили можно найти следующим образом. Отсортировать массив времен хода по возрастанию.
        Пусть n – количество элементов в массиве. Тогда x_25=x([n/4]) – элемент массива с индексом
        [n/4], а x_75=x([3n/4]) – элемент массива с индексом [3n/4].
        В случае ошибки выводить id поезда, для которого время хода на участке является выбросом,
        граничные станции крупного участка, время хода для данного
        поезда (в часах в минутах) и медианное время хода
        (серединный элемент массива времен ходов по этому участку) (в часах и минутах).
        Про выбросы и методы их обнаружения можно прочитать, например, тут: https://en.wikipedia.org/wiki/Outlier
         */
        assertTrue(SlotTrainChecker.checkAbnormalLength_3_1_7(input, parser.slotTrains.values()));
    }

    /**
     * Отправление локомотива раньше времени явки
     */
    @Test
    public void checkEarlyLoco_3_1_8() {//3.1.8 Отправление локомотива раньше времени явки
        /*
        Для каждого локомотива время отправления на первый участок маршрута не должно превышать
        время явки локомотива. Время явки локомотива – это:
        •	фактическое время отправления локомотива (если во входном сообщении fact_loco указано
        местоположение на перегоне с поездом);
        •	время прибытия на станцию (если во входном сообщении fact_loco указано местоположение на
        станции с поездом);
        •	время готовности локомотива (если во входном сообщении fact_loco указано местоположение на
        станции без поезда).
         */
        assertTrue(SlotLocoChecker.checkEarlyLoco_3_1_8(input, parser.slotLocos.values()));
    }

    /**
     * Отправление бригады раньше времени явки
     */
    @Test
    public void checkEarlyTeam_3_1_9() {//3.1.9 Отправление бригады раньше времени явки
        /*
        Для каждой бригады время отправления на первый участок маршрута не должно превышать время явки бригады.
        Время явки бригады – это:
        •	фактическое время отправления бригады (если во входном сообщении fact_team указано
        местоположение на перегоне с локомотивом);
        •	время прибытия на станцию (если во входном сообщении fact_team указано местоположение
        на станции с локомотивом);
        •	время готовности бригады (если во входном сообщении fact_team указано местоположение
        на станции без локомотива).
         */
        assertTrue(SlotTeamChecker.checkEarlyTeam_3_1_9(input, parser.slotTeams.values()));
    }

    @Test
    public void testCloseThread_3_1_10() {//3.1.10 близкорасположенные (< 10 min) треки
        /*
        Для каждого участка (link) определить список поездов, которые запланированы на этом участке.
        Проверить, что минимальное расстояние между временам отправления таких поездов не превышает 10 минут.
        В случае ошибки выводить id пар поездов, для которых наблюдается ошибка, участок, времена отправления
        поездов и разницу (в минутах) между временами отправления.
         */
        assertTrue(SlotTrainChecker.checkCloseThread_3_1_10(parser.slotTrains.values(), parser.slotLocos.values(),
                parser.slotTeams.values()));
    }

    @Test
    public void testCrossThread_3_1_11() {//3.1.11 Пересекающиеся нитки
        /*
        Для каждого участка (link) определить список поездов, которые запланированы на этом участке.
        Для каждой пары поездов определить времена отправления и прибытия.
        Проверить, что если время отправления одного поезда меньше времени отправления другого,
        то и время прибытия первого поезда меньше времени прибытия второго.
        В случае ошибки выводить id пар поездов, для которых наблюдается ошибка, участок,
        времена отправления и прибытия первого поезда, времена отправления и прибытия
        второго поезда.
         */
        assertTrue(SlotTrainChecker.checkCrossThread_3_1_11(input, parser.slotTrains.values()));
    }

    @Test
    public void testLocoRelocationProcessed_3_1_12() {//3.1.12 Выполнение регулировочных заданий
        /*
        Для каждого поступившего задания на регулирование посчитать количество поездов, которые
        соответствуют локомотивам резервом и в маршруте которых есть отправление
        с указанной в задании станции на участок в указанном направлении, причем время отправления
        лежит в интервале, указанном в задании.
        Проверить, что количество таких поездов не меньше количества, указанного в задании на регулирование.
        В случае ошибки вывести количество запланированных таких поездов и их id.
         */
        assertTrue(SlotLocoChecker.checkAllLocoRelocationsProcessed_3_1_12(input, parser.slotLocos));
    }

    @Test
    public void testFullTrainRoutesAssigned_3_1_13() {//3.1.13 Все поезда запланированы по всему маршруту
        /*
        Для каждого поезда, который поступил на вход планировщика и не был отсеян, определить список
        участков планирования,
        по которым должен проехать поезд (начиная от станции, на которой находился поезд на начало планирования).
        Проверить, что все эти участки присутствуют в результатах планирования в соответствующем сообщении slot_train.
        В случае ошибки выводить id поезда, для которого возвращен не весь маршрут.
         */
        assertTrue(SlotTrainChecker.checkFullTrainRoutesAssigned_3_1_13(input, parser.slotTrains));
    }

    @Test
    public void testTrainWeight_3_1_14() {//3.1.14  Учет весовых категорий локомотивов и поездов
        /*
        Для каждого поезда на каждом участке планирования ищем локомотив, который везет поезд на этом участке.
        Далее в справочнике весовых категорий (сообщения loco_tonnage) выбираем элемент, который относится
        к данному участку, серии и количеству секций данного локомотива.
        Смотрим у найденного элемента справочника значение максимально разрешенного веса поезда.
        Это значение из справочника не должно быть меньше значения веса поезда, который следует с локомотивом.

        Если было найдено более одной подходящей записи в справочнике, то требуется брать минимальное значение
        разрешенного веса из этих записей.

        Если не найдено ни одной подходящей записи в справочнике, то проверку на данном участке движения поезда
        проводить не следует.

        В случае ошибки выводить id поезда, id локомотива, участок, на котором произошла ошибка, вес поезда.
         */
        assertTrue(SlotTrainChecker.checkTrainsAccordingToWeight(parser.slotLocos, input));
    }

    @Test
    public void testTeamsAreNotSentWithUnassignedTrains_3_1_15() {//3.1.15
        /*
        У каждой бригады, пересылаемой пассажиром на грузовой нитке, есть локомотив, бригада и поезд.
         */
        assertTrue(SlotTeamChecker.checkAllPassTeamsSentWithTrainLocoAndTeam_3_1_30(parser.slotTeams, parser.slotLocos,
                parser.slotTrains));
    }

}
