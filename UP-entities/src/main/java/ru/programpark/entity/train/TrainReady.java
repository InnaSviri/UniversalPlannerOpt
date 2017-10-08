package ru.programpark.entity.train;

import ru.programpark.entity.fixed.Station;


/**
  * Готовность поезда к отправлению
 */


//+train_ready(id(Id), station(StId), time(Time))
//StId – идентификатор станции.
//Id – уникальный идентификатор поезда.
//Time – время готовности поезда (в секундах).


public class TrainReady extends TrainState {

     private Station station;

     public TrainReady() {
     }

     public TrainReady(Long id, Long time, Station station) {
        super(id, time);
        this.station = station;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public String toString() {
        return "TrainArrive{" + station.getId() + ", " + getTime() + "}";
    }
}
