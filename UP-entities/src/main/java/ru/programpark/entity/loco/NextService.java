package ru.programpark.entity.loco;

// Фактический локомотив в аспекте данных о ТО
public interface NextService {
    Long getTimeOfServiceFact();
    Long getDistanceToService();
    Long getTimeToService();
    Long getServiceType();
}
