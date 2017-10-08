package ru.programpark.planners.check.input;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.TrainArrive;
import ru.programpark.entity.train.TrainDepart;
import ru.programpark.entity.train.TrainReady;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;

import java.io.PrintWriter;
/*
Реализован Раздел 2.2 документа "Автоматические тесты": Набор тестов по поездам
1. Поезда переданы в планировщик
2. Отсутствие дублирования поездов
3. Для поездов заданы атрибуты
4. Для поезда задан маршрут
5. Для поезда задано местоположение
6. Корректность факта о местоположении
7. Корректное время отправления поезда
8. Связь с локомотивом
 */

public class TrainChecker {
    private static PrintWriter writer = null;
    public static boolean beforeFilter = true;

    private static void log(String message) {
        if (!beforeFilter){
            writer = LoggingAssistant.getInputDataTestAfterFilterDetailsWriter();
        }
        if (writer == null)
            writer = LoggingAssistant.getInputDataTestBeforeFilterDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    public static boolean checkTrainExist_2_2_1(InputData input) {  //Поезда переданы в планировщик
        //Количество сообщений +train_info должно быть больше 0.
        boolean pass = true;
        String testName = "Тест 2.2.1 (Поезда переданы в планировщик)";

        if (input.trainInfoCount == 0){
            log(testName + " не пройден ");
            pass = false;
        }

        return pass;
    }

    public static boolean checkDuplicateFactTrains_2_2_2(InputData iData){   //   Отсутствие дублирования поездов
        //Нет сообщений train_info, для которых указаны одинаковые id поезда.
        // В случае ошибки выводить дублирующиеся id поездов. Найти id дублей
        boolean pass = true;
        String testName = "Тест 2.2.2 (Отсутствие дублирования поездов)";

        for(FactTrain train: iData.getFactTrains().values()) {
            long check = train.checkCorrectTrainInfo();
            if(check != 0) {
                if (pass)
                    log(testName + " не пройден");
                String errorMessage = " нет данных об ошибке.";
                if (check == -1) errorMessage = " дублируются сообщения trainInfo.";
                if (check == -2) errorMessage =  " не получено сообщение trainInfo.";
                log("Дублирование для поезда " + train.getId() + errorMessage);
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkTrainAttributesDefined_2_2_3(InputData iData){//Для поездов заданы атрибуты
        //Во всех сообщениях train_info присутствуют значения атрибутов: категория, номер, вес, длина.
        //В случае ошибки выводить id поездов, для которых ошибка наблюдается.
        boolean pass = true;
        String testName = "Тест 2.2.3 (Для поездов заданы атрибуты: категория, номер, вес, длина)";

        for (FactTrain fTrain: iData.getFactTrains().values()){
            if (fTrain.getWeight().equals(-1L)){
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении trainInfo не задан вес поезда. Для поезда " + fTrain.getId() +
                        " вес равен " + fTrain.getWeight());
                pass = false;
            }
            if (fTrain.getLength().equals(-1L)){
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении trainInfo не задан длина поезда. Для поезда " + fTrain.getId() +
                        " длина равна " + fTrain.getLength());
                pass = false;
            }
            if (fTrain.getTrainNum() <= 0L){
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении trainInfo не задан номер поезда. Для поезда " + fTrain.getId() +
                        " номер поезда равен " + fTrain.getTrainNum());
                pass = false;
            }
            if (fTrain.getCategory() <= 0L) {
                if (pass){
                    log(testName + " не пройден ");
                }
                log("В сообщении trainInfo не задан категория поезда. Для поезда " + fTrain.getId() +
                        " категория равна " + fTrain.getCategory());
                pass = false;
            }
        }

        return pass;
    }

    public static boolean checkRouteForFactTrainsDefined_2_2_4(InputData iData){ // Для поезда задан маршрут
        //В атрибуте routes сообщения train_info указан хотя бы один непустой маршрут.
        //В случае ошибки выводить id поездов, для которых ошибка наблюдается.
        boolean pass = true;
        String testName = "Тест 2.2.4 (Для всех поездов задан маршрут)";

        for (FactTrain fTrain: iData.getFactTrains().values()){
            if (fTrain.getMainRoute() == null || fTrain.getMainRoute().getLinkList().size() == 0){
                if (pass){
                    log(testName +" не пройден ");
                }
                log("В сообщении trainInfo не задан ни один маршрут. Для поезда " + fTrain.getId() +
                        " основной маршрут равен " + fTrain.getMainRoute());
                pass = false;
            }

        }

        return pass;
    }

    public static boolean checkLocationForFactTrainsDefined_2_2_5(InputData iData){//Для всех поездов задано местоположение
        // Для поезда из train_info есть соответствующий факт о местоположении (train_depart, train_arrive или train_ready).
        //В случае ошибки выводить id поездов, для которых ошибка наблюдается.
        boolean pass = true;
        String testName = "Тест 2.2.5 (Для всех поездов задано местоположение)";

        for (FactTrain fTrain: iData.getFactTrains().values()){
            if (fTrain.getTrainState() == null){
                if (pass){
                    log(testName +" не пройден ");
                }
                log("Для сообщения trainInfo не было передано сообщение train_arrive, train_depart, train_ready: " +
                        "trainId = " + fTrain.getId());
                pass = false;
            }

        }

        return pass;
    }

    public static boolean checkCorrectLocationForTrainFacts_2_2_6(InputData iData){//Корректность факта о местоположении
        /*
        В зависимости от местоположения:
        •	Участок из train_depart есть в первом маршруте из train_info.
        •	Станция из train_arrive есть в первом маршруте из train_info.
        •	Станция из train_ready есть в первом маршруте из train_info.
        В случае ошибки выводить id поездов, для которых ошибка наблюдается.
         */
        boolean pass = true;
        String testName = "Тест 2.2.6 (Корректность факта о местоположении)";

        for (FactTrain fTrain: iData.getFactTrains().values()){
            if (fTrain.getTrainState() instanceof TrainDepart) {
                if (!(fTrain.getMainRoute().containsLink(((TrainDepart) fTrain.getTrainState()).getLink()))) {
                    if (pass){
                        log(testName +" не пройден ");
                    }
                    log("Для поезда " + fTrain.getId() + " участка из train_depart нет в первом маршруте из train_info ");
                    pass = false;
                }
            }

            if (fTrain.getTrainState() instanceof TrainArrive) {
                if (!(fTrain.getMainRoute().containsStation(((TrainArrive) fTrain.getTrainState()).getLink().getTo()))) {
                    if (pass){
                        log(testName +" не пройден ");
                    }
                    log("Для поезда " + fTrain.getId() + " cтанции из train_arrive нет в первом маршруте из train_info ");
                    pass = false;
                }
            }

            if (fTrain.getTrainState() instanceof TrainReady) {
                if (!(fTrain.getMainRoute().containsStation(((TrainReady) fTrain.getTrainState()).getStation()))) {
                    if (pass){
                        log(testName +" не пройден ");
                    }
                    log("Для поезда " + fTrain.getId() + " cтанции из train_ready нет в первом маршруте из train_info ");
                    pass = false;
                }
            }
        }

        return pass;
    }

    public static boolean checkCorrectDepartureTimeForTrainFacts_2_2_7(InputData iData){
    //Корректное время отправления поезда
        /*
        Если для поезда задан факт train_depart, то разность между временем отправления поезда и текущим временем
        не должна превышать утроенное нормативное время хода
        на данном участке (указанное в link).
        В случае ошибки выводить id поезда, время отправления (из факта), разность между текущим временем и
        временем отправления (в часах и минутах) и нормативное
        время хода по участку.
         */
        boolean pass = true;
        String testName = "Тест 2.2.7 (Корректное время отправления поездов) не пройден";

        for (FactTrain fTrain: iData.getFactTrains().values()){
            if (fTrain.getTrainState() instanceof TrainDepart){
                Long startTime = fTrain.getTrainState().getTime();
                Long linkDuration = ((TrainDepart) fTrain.getTrainState()).getLink().getDuration(startTime);
                if (iData.getCurrentTime() - startTime >= 3*linkDuration) {
                    if (pass)
                        log(testName);
                    log("Для поезда " + fTrain.getId() + " задан факт train_depart, то разность между временем " +
                            "отправления поезда и текущим временем превышает утроенное нормативное время хода на " +
                            "данном участке (указанное в link).");
                    pass = false;
                }
            }
        }

        return pass;
    }

    public static boolean checkTrainLocoReference_2_2_8(InputData iData){ // Связь с локомотивом
        /*
        Если для поезда передан факт train_depart, то должен быть факт fact_loco о локомотиве, который следует с
         этим поездом на этом же участке.
        Время отправления поезда должно совпадать с временем отправления локомотива.
        В случае ошибки выводить id поезда, для которого ошибка наблюдается. Также надо выводить тип ошибки:
        •	Вообще нет сообщения о местоположении локомотива на участке.
        •	Местоположение локомотива отличается по типу (не на участке, а на станции с поездом или на станции без поезда).
        •	Локомотив находится на другом участке.
        •	Локомотив находится на этом участке, но с другим временем отправления.
         */

        boolean pass = true;
        String testName = "Тест 2.2.8 (Связь переданных поездов с локомотивом)";

        for (FactTrain fTrain: iData.getFactTrains().values()){
            if (fTrain.getTrainState() instanceof TrainDepart){
                boolean found = false;
                for(FactLoco fLoco : iData.getFactLocos().values()) {
                    try {
                        if (fTrain.getId().equals(fLoco.getTrainId())) {
                            if (fLoco.getTrack() == null)
                                continue;
                            if (!fLoco.getTrack().getTimeDepart().equals(fTrain.getTrainState().getTime())) {
                                if (pass)
                                    log(testName + " не пройден ");
                                log("Для train_depart и соотв. fact_loco времена не совпадают. У поезда " +
                                        fTrain.getId() + " время " +
                                        new Time(fTrain.getTrainState().getTime()).getTimeStamp() +
                                        " , а у локо " + fLoco.getId() + " - "
                                        + new Time(fLoco.getTrack().getTimeDepart()).getTimeStamp());
                                pass = false;
                            }
                            found = true;
                            break;
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

                if (!found) {
                    if (pass)
                        log(testName +" не пройден ");
                    log("Для train_depart нет fact_loco о локомотиве, который следует с этим поездом на этом же" +
                            " участке. Поезд " + fTrain.getId());
                    pass = false;
                }
            }
        }

        return true;
    }

    public static boolean correctTrainRoute_2_2_9(InputData iData){ //Корректность маршрутов поездов
        //Для каждой пары последовательных станций в маршруте поезда было передано сообщение +link
        // (link.isLinkMessage() == true).
        boolean pass = true;
        String testName = "Тест 2.2.9 (Корректность маршрутов поездов)";

        for (FactTrain fTrain: iData.getFactTrains().values()){
            for (Link link: fTrain.getMainRoute().getLinkList()){
                if (pass){
                    log(testName + " не пройден");
                }
                pass = false;
                if (!link.isLinkMessage()){
                    log("Для перегона " + link.getFrom().getName() + "-" + link.getTo().getName() +
                            " из маршрута поезда " + fTrain.getId() + " отсутствует сообщение +link");
                }
            }
        }

        return pass;
    }
}

