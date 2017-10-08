package ru.programpark.planners.handlers;


import ru.programpark.entity.data.InputData;
import ru.programpark.entity.data.OutputData;

public interface VectorHandler {
    /**
     * Создает планировщик
     * @param name - имя плагина (используется для логирования)
     * @param plannerType - тип планировщика
     */
    void createPlanner(String name, String plannerType);

    void createPlanner();
    void createPlanner(Boolean async);

    /**
     * Удаление планировщика
     */
    void removePlanner();

    /**
     * Запускает пересчет
     * @param addPercepts - перцепты для добавления
     * @param delPercepts - перцепты для удаления
     */
    void startPlanning(String[] addPercepts, String[] delPercepts);

    void startPlanning(InputData inputData);

    /**
     * Получение (и журналирование) результатов планирования
     * @return
     */
    String[] getResults();

    OutputData getResultsAsData();
    InputData getInputAsData();

    void logResults();
    void logResults(OutputData results);

}
