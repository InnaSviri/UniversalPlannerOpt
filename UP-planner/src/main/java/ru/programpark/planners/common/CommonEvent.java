package ru.programpark.planners.common;

// Единый интерфейс событий, реализуемый перцепционными фактами,
// назначениями локомотивов и бригад, сдвигами и пр.

public interface CommonEvent {
    // Время совершения события
    Long getEventTime();
    // Время готовности поезда после данного события
    Long getTrainReadyTime();
    // Время готовности локомотива; практически всегда будет совпадать
    // со временем готовности поезда
    Long getLocoReadyTime();
    // Время готовности бригады
    Long getTeamReadyTime();
}
