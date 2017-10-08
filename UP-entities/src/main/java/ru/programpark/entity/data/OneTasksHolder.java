package ru.programpark.entity.data;

import ru.programpark.entity.train.*;

import java.util.ArrayList;
import java.util.List;

/**
 * User: oracle
 * Date: 03.06.15
 */
public class OneTasksHolder {
    InputData DATA;

    public OneTasksHolder(InputData DATA) {
        this.DATA = DATA;
    }

    private void CHECK_INTERRUPTED() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            org.slf4j.LoggerFactory.getLogger(OneTasksHolder.class)
                     .warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    void processTasks() throws InterruptedException {
        for (Task task: DATA.getTasks().values()){
            CHECK_INTERRUPTED();
            for (int i = 0; i < task.getTrainQuantity(); i++){
                List<List<OneTaskTrack>> routes = new ArrayList<>();
                for (Route r: task.getRoutes()){
                    List<OneTaskTrack> newRoute = r.toOneTaskRoute();
                    routes.add(newRoute);
                }
                String id = task.getId().toString();
                id = id + (i >= 10 ? "" : "0") + Long.valueOf(i);
                OneTask oneTask = new OneTask(Long.parseLong(id), task.getStartTime(), task.getDuration(), routes,  task.getWeight(), OneTask.OneTaskType.TASK);
                if (oneTask.checkRoutes()){
                    DATA.addOneTask(oneTask);
                }
            }
        }
    }

    /**
     * Обработка фактов (формирование еще OneTask)
     */
    void processTrainFacts() throws InterruptedException {
        for (FactTrain fact: DATA.getFactTrains().values()) {
            CHECK_INTERRUPTED();
            OneTask task = createOneTaskFromFactTrain(fact, DATA.getCurrentTime());
            if (! task.getRoutes().isEmpty()) DATA.addOneTask(task);
        }
    }

    /**
     * Метод формирует OneTask из факта
     * @param fact
     * @param currentTime
     * @return
     */
    private OneTask createOneTaskFromFactTrain(FactTrain fact, long currentTime) {
        //Если не нашли подходящего OneTask, то создаем его сами!
        List<List<OneTaskTrack>> routes = new ArrayList<>();
        for (Route originRoute : fact.getRoutes()) {
            List<OneTaskTrack> route = new ArrayList<>();
            Boolean startRecord = false;
            //записываем, начиная со станции из линка
            for (int i = 0; i < originRoute.toOneTaskRoute().size(); i++) {
                OneTaskTrack taskTrack = originRoute.toOneTaskRoute().get(i);
                if (fact.getTrainState() instanceof TrainArrive) {
                    if (taskTrack.getLink().getFrom().equals(((TrainArrive) fact.getTrainState()).getLink().getTo())) {
                        startRecord = true;
                    }
                } else if (fact.getTrainState() instanceof TrainDepart) {
                    if (taskTrack.getLink().getFrom().equals(((TrainDepart) fact.getTrainState()).getLink().getFrom())) {
                        startRecord = true;
                    }
                } else if (fact.getTrainState() instanceof TrainReady) {
                    startRecord = true;
                }
                if (startRecord) {
                    route.add(taskTrack);
                }
            }
            if (route.size() > 0) {
                routes.add(route);
            }
        }
        OneTask.OneTaskType type = null;
        long startTime = 0;
        if (fact.getTrainState() instanceof TrainDepart){
            type = OneTask.OneTaskType.FACT_DEPART;
            startTime = fact.getTrainState().getTime();
        } else if (fact.getTrainState() instanceof TrainArrive){
            type = OneTask.OneTaskType.FACT_ARRIVE;
            startTime = fact.getTrainState().getTime();//Math.max(fact.getTrainState().getTime(), currentTime);
        }  if (fact.getTrainState() instanceof TrainReady){
            type = OneTask.OneTaskType.FACT_READY;
            startTime = fact.getTrainState().getTime();//Math.max(fact.getTrainState().getTime(), currentTime);
        }

        OneTask oneTask = new OneTask(fact.getId(), startTime, 0L, routes, fact.getWeight(), type);
        oneTask.setRealId(fact.getId());

        return oneTask;
    }

}
