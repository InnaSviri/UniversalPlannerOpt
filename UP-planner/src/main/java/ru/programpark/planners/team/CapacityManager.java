package ru.programpark.planners.team;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Capacity;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.TrainCategory;
import ru.programpark.entity.train.TrainDepart;
import ru.programpark.planners.common.SchedulingData;
import ru.programpark.planners.common.Train;

import java.util.*;

public class CapacityManager {
    private InputData input;
    private TeamPlanningParams params;

    CapacityManager() {
        this.input =  SchedulingData.getInputData();
        this.params = new TeamPlanningParams(input);
    }

    public Capacity foundCapacity(SlotTrack firstSlot, boolean manualBinding, TeamSlot teamSlot, Long timeOfPresence) {
        Train train = SchedulingData.getFrameData().getTrain(teamSlot.route.get(0).trainId);
        FactTrain factTrain = SchedulingData.getInputData().getFactTrains().get(train.getTrainId());
        Capacity foundCapacity = null;
        boolean reserve = false;
        boolean shifting = true;
        long shiftTime = 0L;
        if (manualBinding) {
            if (factTrain != null && factTrain.getTrainState() instanceof TrainDepart) {
                shifting = false;
            }
        } else {
            //Если время готовности бригады позже, чем время отправки локо (локо ждет бригаду),
            // прибавляем эту разницу к времени смещения
            if (timeOfPresence > firstSlot.getTimeStart()) {
                shiftTime += timeOfPresence - firstSlot.getTimeStart();
            }
            //если вызов не из ручной привязки, то необходимо добавить время на смену бригады
            //если категория поезда имеет приоритет 1, брать нормативное время в 0.5 от заданного для станции
            boolean locoChanged = train.locoChanged();
            if (!locoChanged) {
                if (factTrain != null && input.getTrainCategoryById(factTrain.getCategory()).getPriority() == 1) {
                    shiftTime += params.defaultStopTimeForTeam * 0.5;
                } else {
                    shiftTime += params.defaultStopTimeForTeam;
                }
            }
        }
        //Если поезда нет среди переданных фактов, то это может означать, что он резервный
        if (firstSlot != null && input.getFactTrains().get(firstSlot.trainId) == null) reserve = true;
        if (shifting && !reserve) {
            TrainCategory trainCategory = input.getTrainCategoryById(factTrain.getCategory());
            Integer categoryPriority = trainCategory != null ? trainCategory.getPriority() : 100;
            Integer trainPriority = factTrain.getPriority() != null ? factTrain.getPriority() : 100;
            foundCapacity = getFreeCapacity(firstSlot.getLink(), firstSlot.getTimeStart() + shiftTime, categoryPriority,
                    trainPriority);
            shiftTime += calcShiftTimeWithCapacity(firstSlot.getLink(), firstSlot.getTimeStart() + shiftTime,
                    firstSlot.trainId);
        }
        if (shifting && reserve) {
            foundCapacity = getFreeCapacityForReserveLoco(firstSlot.getLink(), firstSlot.getTimeStart() + shiftTime);
        }

        return foundCapacity;
    }

    private long calcShiftTimeWithCapacity(Link link, long startTime, Long trainId) {
        FactTrain factTrain = input.getFactTrains().get(trainId);
        TrainCategory trainCategory = input.getTrainCategoryById(factTrain.getCategory());
        Integer categoryPriority = trainCategory != null ? trainCategory.getPriority() : 100;
        Integer trainPriority = factTrain.getPriority() != null ? factTrain.getPriority() : 100;
        startTime -= 15L * 60L;
        long shiftTime = 0L;
        Capacity freeCapacity = getFreeCapacity(link, startTime, categoryPriority, trainPriority);
        if (freeCapacity != null) {
            shiftTime = freeCapacity.getStartTime() - startTime - (15L * 60L);
        }

        return shiftTime;
    }

    private Capacity getFreeCapacity(Link link, long timeStart, Integer categoryPriority, Integer trainPriority) {
        for (Capacity capacity : link.getCapacities().values()) {
            if (capacity.getStartTime() >= timeStart) {
                if (capacity.getCapacity() == 0) {
                    Map<Long, FactTrain> factTrains = new HashMap<>();
                    for (Long trainId : capacity.trainIds) {
                        FactTrain factTrain = input.getFactTrains().get(trainId);
                        if (factTrain == null) continue;
                        TrainCategory trainCategory = input.getTrainCategories().get(factTrain.getCategory());
                        Integer earlyCategoryPriority = trainCategory != null ? trainCategory.getPriority() : 100;
                        Integer earlyTrainPriority = factTrain.getPriority() != null ? factTrain.getPriority() : 100;
                        if (earlyCategoryPriority > categoryPriority) {
                            factTrains.put(factTrain.getId(), factTrain);
                        } else if (earlyCategoryPriority.equals(categoryPriority)) {
                            if (earlyTrainPriority > trainPriority) {
                                factTrains.put(factTrain.getId(), factTrain);
                            }
                        }
                    }
                    if (factTrains.size() > 0) {
                        return capacity;
                    }
                } else if (capacity.getCapacity() > 0) {
                    return capacity;
                }
            }
        }
        return null;
    }

    private Capacity getFreeCapacityForReserveLoco(Link link, long timeStart) {
        for (Capacity capacity : link.getCapacities().values()) {
            if (capacity.getStartTime() >= timeStart) {
                return capacity;
            }
        }
        return null;
    }

    public Map<Long, Capacity> getFreeCapacityMap(Link link, long timeStart, Long trainId, Integer categoryPriority,
                                                  Integer trainPriority) {
        Map<Long, Capacity> result = new HashMap<>();

        for (Capacity capacity : link.getCapacities().values()) {
            if (capacity.getStartTime() >= timeStart) {
                if (capacity.getCapacity() == 0) {
                    Map<Long, FactTrain> factTrains = new HashMap<>();
                    for (Long currentTrainId : capacity.trainIds) {
                        if (currentTrainId.equals(trainId)) continue;
                        FactTrain factTrain = input.getFactTrains().get(currentTrainId);
                        boolean shift = false;
                        if (factTrain == null) continue;
                        TrainCategory trainCategory = input.getTrainCategories().get(factTrain.getCategory());
                        Integer earlyCategoryPriority = trainCategory != null ? trainCategory.getPriority() : 100;
                        Integer earlyTrainPriority = factTrain.getPriority() != null ? factTrain.getPriority() : 100;
                        if (earlyCategoryPriority >= categoryPriority) {
                            factTrains.put(factTrain.getId(), factTrain);
                            shift = true;
                        } else if (earlyCategoryPriority.equals(categoryPriority)) {
                            if (earlyTrainPriority >= trainPriority) {
                                factTrains.put(factTrain.getId(), factTrain);
                                shift = true;
                            }
                        }
                        if (shift) {
                            result.putAll(getFreeCapacityMap(link, capacity.getStartTime(), currentTrainId,
                                    earlyCategoryPriority, earlyTrainPriority));
                        }
                    }
                    if (factTrains.size() > 0) {
                        result.put(trainId, capacity);
                        break;
                    }
                } else if (capacity.getCapacity() > 0) {
                    result.put(trainId, capacity);
                    break;
                }
            }
        }

        return result;
    }

    public void takeCapacities(SlotTrack firstSlot, Map<Long, Capacity> capacities) {
        for (Map.Entry<Long, Capacity> entry : capacities.entrySet()) {
            takeCapacity(firstSlot.getLink(), entry.getValue(), entry.getKey());
        }
    }

    private Capacity takeCapacity(Link link, Capacity sourceCapacity, Long trainId) {
        for (Capacity capacity : link.getCapacities().values()) {
            if (capacity.equals(sourceCapacity)) {
                capacity.setCapacity(capacity.getCapacity() - 1);
                if (trainId != null) capacity.trainIds.add(trainId);
                return capacity;
            }
        }
        return null;
    }

    public void reassignTrains(Map<Long, Capacity> capacities) {
        for (Map.Entry<Long, Capacity> entry : capacities.entrySet()) {
            entry.getValue().setCapacity(entry.getValue().getCapacity() - 1);
            entry.getValue().trainIds.add(entry.getKey());
        }
    }

}
