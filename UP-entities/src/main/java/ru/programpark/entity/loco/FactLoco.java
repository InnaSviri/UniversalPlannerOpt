package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Station;

import java.util.ArrayList;

/**
 * User: oracle
 * Date: 30.05.14
 * +fact_loco
 */
public class FactLoco implements Comparable<FactLoco>, Cloneable, LocoAttributes, NextService {

    //_____заполняется из +fact_loco_______
    Long Id;                     // соответствует real_id в сообщениях train_matching
    private boolean isDuplicateFactLoco = false; // было ли дублирование
    private boolean isSetFactLoco = false; // получено ли сообщение
    Long timeOfLocoFact;
    Station station;             // не-null, е. и т. е. локомотив находится на станции
    FactLocoArrive locoArrive;   // не-null, е. и т. е. локомотив находится на станции с поездом
    FactLocoTrack track;     // не-null, е. и т. е. локомотив находится на перегоне
    //_____заполняется из +fact_loco_next_service____
    private boolean isDuplicateNextService = false; // было ли дублирование
    private boolean isSetTimeUntilService = false; // получено ли сообщение
    Long timeOfServiceFact = -1L;
    Long distanceToService = -1L;
    Long timeToService = -1L;
    Long serviceType = -1L;
    //_____заполняется из +loco_attributes____
    private boolean isDuplicateLocoAttr = false; // было ли дублирование
    private boolean isSetLocoAttr = false; // получено ли сообщение
    SeriesPair seriesPair; // заводская серия локомотива (??Long or Int) + число секций
    ArrayList<LocoRegion> locoRegions; //идентификаторы тяговых плеч, на которых может работать локомотив.
    Station depotStation; // идентификатор станции, являющейся депо приписки локомотива

    boolean factLocoAttrRead = false;
    boolean factLocoNextServiceRead = false;

    private FactLoco() {}

    public FactLoco(Long id, Long timeOfLocoFact, FactLocoTrack track) {
        Id = id;
        this.timeOfLocoFact = timeOfLocoFact;
        this.station = null;
        this.track = track;
    }

    public FactLoco(Long id, Long timeOfLocoFact, Station station, FactLocoArrive locoArrive) {
        Id = id;
        this.timeOfLocoFact = timeOfLocoFact;
        this.station = station;
        this.locoArrive = locoArrive;
        this.track = null;
    }

    public int compareTo(FactLoco o) {
        if (this.timeOfLocoFact > o.getTimeOfLocoFact()) // this > o
            return 1;
        if (this.timeOfLocoFact < o.getTimeOfLocoFact()) // this < o
            return -1;
        if (this.timeOfLocoFact == o.getTimeOfLocoFact()) // this >== o
            return 0;

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactLoco factLoco = (FactLoco) o;
        if (Id != null ? !Id.equals(factLoco.Id) : factLoco.Id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Id != null ? Id.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FactLoco");
        sb.append("{Id=").append(Id);
        sb.append(", timeOfLocoFact=").append(timeOfLocoFact);
        sb.append(", station=").append(station);
        sb.append(", track=").append(track);
        sb.append(", locoArrive=").append(locoArrive);
        sb.append(", timeOfServiceFact=").append(timeOfServiceFact);
        sb.append(", distanceToService=").append(distanceToService);
        sb.append(", timeToService=").append(timeToService);
        sb.append(", serviceType=").append(serviceType);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Object clone() {
        FactLoco copy = new FactLoco();
        copy.Id = this.Id;
        copy.timeOfLocoFact = this.timeOfLocoFact;
        copy.station = this.station;
        copy.locoArrive = this.locoArrive;
        copy.track = this.track;
        copy.timeOfServiceFact = this.timeOfServiceFact;
        copy.distanceToService = this.distanceToService;
        copy.timeToService = this.timeToService;
        copy.serviceType = this.serviceType;
        copy.seriesPair = this.seriesPair;
        copy.locoRegions = this.locoRegions;
        copy.depotStation = this.depotStation;
        copy.factLocoAttrRead = this.factLocoAttrRead;
        copy.factLocoNextServiceRead = this.factLocoNextServiceRead;
        return (Object) copy;
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public Long getTimeOfLocoFact() {
        return timeOfLocoFact;
    }

    public void setTimeOfLocoFact(Long timeOfLocoFact) {
        this.timeOfLocoFact = timeOfLocoFact;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public FactLocoArrive getLocoArrive() {
        return locoArrive;
    }

    public void setLocoArrive(FactLocoArrive locoArrive) {
        this.locoArrive = locoArrive;
    }

    public FactLocoTrack getTrack() {
        return track;
    }

    public void setTrack(FactLocoTrack track) {
        this.track = track;
    }

    public void markFactLocoSet() {
        if(isSetFactLoco)
            isDuplicateFactLoco = true;
        else
            isSetFactLoco = true;
    }

    public int checkCorrectFactLoco() {
        if (isDuplicateFactLoco) return -1;
        if (!isSetFactLoco) return -2;
        return 0;
    }

    public void markLocoAttrSet() {
        if(isSetLocoAttr)
            isDuplicateLocoAttr = true;
        else
            isSetLocoAttr = true;
    }

    public int checkCorrectLocoAttr() {
        if (isDuplicateLocoAttr) return -1;
        if (!isSetLocoAttr) return -2;
        return 0;
    }


    public Long getTrainId() {
        if (locoArrive != null) {
            return locoArrive.getId();
        } else if (track != null) {
            return track.getTrainId();
        } else {
            return null;
        }
    }

    public Long getTimeOfServiceFact() {
        return timeOfServiceFact;
    }

    public void setTimeOfServiceFact(Long timeOfServiceFact) {
        this.timeOfServiceFact = timeOfServiceFact;
    }

    public Long getDistanceToService() {
        return distanceToService;
    }

    public void setDistanceToService(Long distanceToService) {
        this.distanceToService = distanceToService;
    }

    public Long getTimeToService() {
        return timeToService;
    }

    public void setTimeToService(Long timeToService) {
        this.timeToService = timeToService;
    }

    public Long getServiceType() {
        return serviceType;
    }

    public void setServiceType(Long serviceType) {
        this.serviceType = serviceType;
    }

    public void markTimeUntilServiceSet() {
        if(isSetTimeUntilService)
            isDuplicateNextService = true;
        else
            isSetTimeUntilService = true;
    }

    public int checkCorrectNextService() {
        if (isDuplicateNextService) return -1;
        if (!isSetTimeUntilService) return -2;
        return 0;
    }

    public SeriesPair getSeriesPair() {
        return seriesPair;
    }

    public void setSeriesPair(SeriesPair seriesPair) {
        this.seriesPair = seriesPair;
    }

    public Integer getSeries() {
        return (seriesPair == null) ? 0 : seriesPair.getSeries();
    }

    public void setSeries(Integer series) {
        if (seriesPair == null) {
            seriesPair = new SeriesPair(series, 1);
        } else {
            seriesPair.setSeries(series);
        }
    }

    public Integer getNSections() {
        return (seriesPair == null) ? 0 : seriesPair.getSection();
    }

    public void setNSections(Integer n) {
        if (seriesPair == null) {
            seriesPair = new SeriesPair(0, n);
        } else {
            seriesPair.setSection(n);
        }
    }

    public ArrayList<LocoRegion> getLocoRegions() {
        return locoRegions;
    }

    public void setLocoRegions(ArrayList<LocoRegion> locoRegions) {
        this.locoRegions = locoRegions;
    }

    public void addLocoRegion(LocoRegion region) {
        if (locoRegions == null) {
            locoRegions = new ArrayList<LocoRegion>();
        }
        locoRegions.add(region);
    }

    public Station getDepotStation() {
        return depotStation;
    }

    public void setDepotStation(Station depotStation) {
        this.depotStation = depotStation;
    }

    public boolean isFactLocoAttrRead() {
        return factLocoAttrRead;
    }

    public void setFactLocoAttrRead(boolean factLocoAttrRead) {
        this.factLocoAttrRead = factLocoAttrRead;
    }

    public boolean isFactLocoNextServiceRead() {
        return factLocoNextServiceRead;
    }

    public void setFactLocoNextServiceRead(boolean factLocoNextServiceRead) {
        this.factLocoNextServiceRead = factLocoNextServiceRead;
    }

    public String checkForExclusion() {
        if (getLocoRegions() == null || getLocoRegions().isEmpty()) {
            return "не определены локомотивные плечи";
        } else if (getSeriesPair() == null) {
            return "не определена серия";
        } /* else if (getTimeToService().equals(0L)) {
            return "объявлено нулевое время до ремонта";
        } */ else {
            return null;
        }
    }

}
