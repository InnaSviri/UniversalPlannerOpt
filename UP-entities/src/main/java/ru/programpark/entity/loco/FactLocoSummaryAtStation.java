package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Station;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 29.12.14
 *
 */
public class FactLocoSummaryAtStation {
    private Station S;
    private List<FactLoco> locosWithoutTrain = new ArrayList<>();
    private List<FactLoco> locosWithTrain = new ArrayList<>();
    private List<FactLoco> locosArriving = new ArrayList<>();

    public FactLocoSummaryAtStation(Station S, Map<Long, FactLoco> factLocos){
        this.S = S;
        getFactLocosAtStation(S, factLocos);
    }

    public void getFactLocosAtStation(Station station, Map<Long, FactLoco> factLocos) {
        for (FactLoco fl: factLocos.values()){
            if ((fl.getStation() != null && fl.getStation().equals(station))){
                if (fl.getLocoArrive() == null) {
                    locosWithoutTrain.add(fl);
                } else {
                    locosWithTrain.add(fl);
                }
            }
            if (fl.getTrack() != null && fl.getTrack().getLink().getTo().equals(station)){
                locosArriving.add(fl);
            }
        }
    }

    public Station getS() {
        return S;
    }

    public List<FactLoco> getLocosWithoutTrain() {
        return locosWithoutTrain;
    }

    public List<FactLoco> getLocosWithTrain() {
        return locosWithTrain;
    }

    public List<FactLoco> getLocosArriving() {
        return locosArriving;
    }
}