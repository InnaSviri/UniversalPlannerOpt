package scenarios;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.programpark.entity.data.*;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.ResultParser;
import ru.programpark.planners.handlers.VectorHandler;
import ru.programpark.planners.handlers.impl.VectorHandlerImpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Created by oracle on 16.10.2015.
 */

public class AnalyzerTest {

    protected static VectorHandler planner;
    protected static AnalyzerResultParser valuesParser;
    protected static ResultParser resultParser;
    protected static InputData input;
    protected static OutputData output;

    protected double accuracy = 0.01;

    @BeforeClass
    public static void onetimeSetUp()
            throws IOException, URISyntaxException {        // overridden in descendants
        planner = new VectorHandlerImpl();
        planner.createPlanner(false);
        loadFromFile();
        LoggingAssistant.createTestLogDirectory(true);
    }

    @AfterClass
    public static void oneTimeTearDown()  throws IOException, URISyntaxException {
        planner.logResults(output);
        planner.removePlanner();
        planner = null;
    }


    public static void loadFromFile()                       // overridden in descendants
            throws IOException, URISyntaxException {
        loadPlan("/AnalyzerCheckData/jason-FullPlannerPlugin_sc04.test");
        loadCheckValues("/AnalyzerCheckData/scen04.chd");

    }

    protected static void loadPlan(String resourceName) throws URISyntaxException, IOException {
        URL resourceUrl = AnalyzerTest.class.getResource(resourceName);
        Path resourcePath = Paths.get(resourceUrl.toURI());

        Percepts percepts = new Percepts(resourcePath.toString());

        planner.startPlanning(percepts.getAddPercepts(), percepts.getDelPercepts());

        while (planner.getResults() == null) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        input = planner.getInputAsData();
        resultParser =  new ResultParser(Arrays.asList(percepts.getAddPercepts()));
        output = new OutputData();
        output.setAnalyzeCriteria(new Analyzer(input, resultParser));
    }


    static protected void loadCheckValues(String resourceName)
            throws URISyntaxException, IOException {
        URL resourceUrl = AnalyzerTest.class.getResource(resourceName);
        Path resourcePath = Paths.get(resourceUrl.toURI());
        Percepts valuesPercepts = new Percepts(resourcePath.toString());
        valuesParser = new AnalyzerResultParser(valuesPercepts.getAddPercepts());
    }

    private boolean compareDouble(double a, double b, double accuracy) {
        return Math.abs(a-b) / Math.max(Math.abs(a),Math.abs(b)+0.000001) <= Math.abs(accuracy);
    }

    private void checkCriterion(long n) {
        double isReally = output.getAnalyzeCriteria().get(n).getValue();
        double mustBe = valuesParser.analyzerValues.get(n);
        boolean condition = compareDouble(mustBe, isReally, accuracy);
        if(!condition) {
            System.out.println("@@@ Analyzer Criterion Error, critNo: " + n + ", must be: " +
                    + mustBe + ", equals: " + isReally + ".");
        }
        assertTrue(condition);
    }

    //Процент незапланированных фактических поездов
    @Test
    public void AnalyzerTest_2() throws URISyntaxException, IOException {
        checkCriterion(2L);
    }

    @Test     //Процент поездов, обеспеченных локомотивами на горизонте в 6 часов
    public void AnalyzerTest_3() throws URISyntaxException, IOException {
        checkCriterion(3L);
    }

    //Процент локомотивов, обеспеченных бригадами на горизонте в 6 часов
    @Test
    public void AnalyzerTest_4() throws URISyntaxException, IOException {
        checkCriterion(4L);
    }


    //Процент поездов, обеспеченных локомотивами на горизонте в 24 часов
    @Test
    public void AnalyzerTest_5() throws URISyntaxException, IOException {
        checkCriterion(5L);
    }

    //Процент локомотивов, обеспеченных бригадами на горизонте в 24 часа
    @Test
    public void AnalyzerTest_6() throws URISyntaxException, IOException {
        checkCriterion(6L);
    }

    //Средне-суточная скорость поездов ходовая
    @Test
    public void AnalyzerTest_7() throws URISyntaxException, IOException {
        checkCriterion(7L);
    }

    //Средная скорость поездов техническая
    @Test
    public void AnalyzerTest_8() throws URISyntaxException, IOException {
        checkCriterion(8L);
    }

    //Время перемещения локомотива резервом общее время
    @Test
    public void AnalyzerTest_9() throws URISyntaxException, IOException {
        checkCriterion(9L);
    }

    //Время перемещения локомотива резервом среднее время
    @Test
    public void AnalyzerTest_10() throws URISyntaxException, IOException {
        checkCriterion(10L);
    }

    //Время пересылки бригад пассажирами общее время
    @Test
    public void AnalyzerTest_11() throws URISyntaxException, IOException {
        checkCriterion(11L);
    }

    //Время пересылки бригад пассажирами среднее время
    @Test
    public void AnalyzerTest_12() throws URISyntaxException, IOException {
        checkCriterion(12L);
    }

    //Процент поездов, запланированных не до конца маршрута
    @Test
    public void AnalyzerTest_13() throws URISyntaxException, IOException {
        checkCriterion(13L);
    }

    //Процент локомотивов, для которых нарушена целостность маршрута
    @Test
    public void AnalyzerTest_14() throws URISyntaxException, IOException {
        checkCriterion(14L);
    }

    //Процент бригад, для которых нарушена целостность маршрута
    @Test
    public void AnalyzerTest_15() throws URISyntaxException, IOException {
        checkCriterion(15L);
    }

    //Средний простой поездов на станциях по маршруту, мин
    @Test
    public void AnalyzerTest_16() throws URISyntaxException, IOException {
        checkCriterion(16L);
    }

    //Средний простой поездов на станциях смены локомотивных бригад, мин
    @Test
    public void AnalyzerTest_17() throws URISyntaxException, IOException {
        checkCriterion(17L);
    }

    //Средний простой локомотивов на станциях без работы, мин
    @Test
    public void AnalyzerTest_18() throws URISyntaxException, IOException {
        checkCriterion(18L);
    }

    //Среднее рабочее время локомотивов за сутки, мин
    @Test
    public void AnalyzerTest_19() throws URISyntaxException, IOException {
        checkCriterion(19L);
    }

    //Среднее время работы локомотивной бригады, мин
    @Test
    public void AnalyzerTest_20() throws URISyntaxException, IOException {
        checkCriterion(20L);
    }

    //Количество локомотивов с превышением пробега и времени работы до ТО-2
    @Test
    public void AnalyzerTest_21() throws URISyntaxException, IOException {
        checkCriterion(21L);
    }

    //Количество бригад с превышением норм рабочего времени
    @Test
    public void AnalyzerTest_22() throws URISyntaxException, IOException {
        checkCriterion(22L);
    }

    //Количество локомотивов, выезжающих за пределы своих тяговых плеч
    @Test
    public void AnalyzerTest_23() throws URISyntaxException, IOException {
        checkCriterion(23L);
    }

    //Количество бригад, выезжающих за свои участки обкатки
    @Test
    public void AnalyzerTest_24() throws URISyntaxException, IOException {
        checkCriterion(24L);
    }

    //Количество поездов, для которых смена локомотивов происходит не на границе тягового плеча
    @Test
    public void AnalyzerTest_25() throws URISyntaxException, IOException {
        checkCriterion(25L);
    }

    //Количество участков планирования без тяговых плеч
    @Test
    public void AnalyzerTest_26() throws URISyntaxException, IOException {
        checkCriterion(26L);
    }

    //Количество участков планирования без участков обкатки бригад
    @Test
    public void AnalyzerTest_27() throws URISyntaxException, IOException {
        checkCriterion(27L);
    }

    //Отсутствие связности поездов и локомотивов
    @Test
    public void AnalyzerTest_28() throws URISyntaxException, IOException {
        checkCriterion(28L);
    }

    //Отсутствие связности локомотивов и поездов
    @Test
    public void AnalyzerTest_29() throws URISyntaxException, IOException {
        checkCriterion(29L);
    }

    //Отсутствие связности локомотивов и бригад
    @Test
    public void AnalyzerTest_30() throws URISyntaxException, IOException {
        checkCriterion(30L);
    }

    //Отсутствие связности бригад и локо
    @Test
    public void AnalyzerTest_31() throws URISyntaxException, IOException {
        checkCriterion(31L);
    }

    //System.out.println("Количество локомотивов без местоположения: " + analyzer.getDataForRow32());
    @Test
    public void AnalyzerTest_32() throws URISyntaxException, IOException {
        checkCriterion(32L);
    }

    //System.out.println("Количество локомотивов без оставшегося времени работы: " + analyzer.getDataForRow33());
    @Test
    public void AnalyzerTest_33() throws URISyntaxException, IOException {
        checkCriterion(33L);
    }

    //System.out.println("Количество бригад без местоположения: " + analyzer.getDataForRow34());
    @Test
    public void AnalyzerTest_34() throws URISyntaxException, IOException {
        checkCriterion(34L);
    }

    //System.out.println("Количество бригад без оставшегося рабочего времени: " + analyzer.getDataForRow35());
    @Test
    public void AnalyzerTest_35() throws URISyntaxException, IOException {
        checkCriterion(35L);
    }

    //Привязка бригады без нарушения правил по временным нормам работы
    @Test
    public void AnalyzerTest_36() throws URISyntaxException, IOException {
        checkCriterion(36L);
    }

    // Количество «старых» поездов
    @Test
    public void AnalyzerTest_37() throws URISyntaxException, IOException {
        checkCriterion(37L);
    }

    //Количество бригад без подвязки
    @Test
    public void AnalyzerTest_38() throws URISyntaxException, IOException {
        checkCriterion(38L);
    }

    // Количество локомотивов без подвязки в состоянии "ожидание работы"
    @Test
    public void AnalyzerTest_39() throws URISyntaxException, IOException {
        checkCriterion(39L);
    }

    //Количество пересылок локомотивов резервом
    @Test
    public void AnalyzerTest_40() throws URISyntaxException, IOException {
        checkCriterion(40L);
    }

    //Количество локомотивных бригад, отправленных пассажирами
    @Test
    public void AnalyzerTest_41() throws URISyntaxException, IOException {
        checkCriterion(41L);
    }

    //Временное расстояние между поездами не меньше 10 минут
    @Test
    public void AnalyzerTest_42() throws URISyntaxException, IOException {
        checkCriterion(42L);
    }

    // Количество локомотивов резервом, следующих в четном направлении
    @Test
    public void AnalyzerTest_43() throws URISyntaxException, IOException {
        checkCriterion(43L);
    }

    //Процент поездов, обеспеченных локомотивами на горизонте в 12 часов
    @Test
    public void AnalyzerTest_44() throws URISyntaxException, IOException {
        checkCriterion(44L);
    }

    //Процент локомотивов, обеспеченных бригадами на горизонте в 12 часов
    @Test
    public void AnalyzerTest_45() throws URISyntaxException, IOException {
        checkCriterion(45L);
    }

    //Процент поездов, обеспеченных локомотивами на горизонте в 18 часов
    @Test
    public void AnalyzerTest_46() throws URISyntaxException, IOException {
        checkCriterion(46L);
    }

    //Процент локомотивов, обеспеченных бригадами на горизонте в 18 часов
    @Test
    public void AnalyzerTest_47() throws URISyntaxException, IOException {
        checkCriterion(47L);
    }

    //Используемый парк локомотивов
    @Test
    public void AnalyzerTest_48() throws URISyntaxException, IOException {
        checkCriterion(48L);
    }

    //Используемый парк бригад
    @Test
    public void AnalyzerTest_49() throws URISyntaxException, IOException {
        checkCriterion(49L);
    }

    // Средний вес поезда (т)
    @Test
    public void AnalyzerTest_50() throws URISyntaxException, IOException {
        checkCriterion(50L);
    }

    // Производительность локомотивов
    @Test
    public void AnalyzerTest_51() throws URISyntaxException, IOException {
        checkCriterion(51L);
    }

    //Среднесуточный полезный пробег локомотивов (км)
    @Test
    public void AnalyzerTest_52() throws URISyntaxException, IOException {
        checkCriterion(52L);
    }

    // Среднесуточный процент полезного использования локомотивов
    @Test
    public void AnalyzerTest_53() throws URISyntaxException, IOException {
        checkCriterion(53L);
    }

    //Среднее время стоянки поезда на технической станции
    @Test
    public void AnalyzerTest_54() throws URISyntaxException, IOException {
        checkCriterion(54L);
    }

    //Средний простой локомотива на станции
    @Test
    public void AnalyzerTest_55() throws URISyntaxException, IOException {
        checkCriterion(55L);
    }

    // Средний простой локомотива на станции в ожидании работы
    @Test
    public void AnalyzerTest_56() throws URISyntaxException, IOException {
        checkCriterion(56L);
    }

    // Среднее время отдыха бригад в пунктах оборота
    @Test
    public void AnalyzerTest_57() throws URISyntaxException, IOException {
        checkCriterion(57L);
    }

}
