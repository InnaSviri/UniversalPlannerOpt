package ru.programpark.entity.fixed;

import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.train.WeightType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: olga
 * Date: 19.06.14 (norm_reserve and norm_time is ignored)
 *
 * +station(id(Id), loco_region(RegId), service([type(id(TypeId), duration(Time)),…]), norm_reserve([norm(weight_type(WType), N),…]),norm_time(T))
 * +station(id(2000479886), loco_region(0001),
    service([type(id(0), duration(7200)), type(id(1), duration(18000))]),
    norm_reserve([norm(weight_type(0), 5),
    norm(weight_type(1), 7)]),
    norm_time(7200))

 Id – идентификатор станции.
 RegId – идентификатор тягового плеча, которому принадлежит станция.
 TypeId  – идентификаторы типов ремонтов, которые можно проводить на данной станции.
 Time – длительность ремонта данного типа. В массиве service должны быть перечислены все типы ремонтов, которые можно производить на данной станции, с указанием длительности каждого ремонта.
 WType – идентификатор весового типа локомотива (0 – локомотив для легких поездов, 1 – локомотив для тяжелых поездов).
 N – нормативный резерв локомотивов на станции. Задается отдельно для каждого участка оборота локомотивов и для каждого весового типа.
 T – время нормативного простоя на станциях смены локомотивных бригад, в секундах. Если на станции нет смены локомотивных бригад, то T = 0.

 Если станция находится на стыке нескольких тяговых плеч, то в планировщик требуется передавать столько сообщений +station, скольким тяговым плечам она принадлежит. При этом будут изменяться, как минимум, RegId и N, а Id будет оставаться неизменным.

 */

public class Station implements Serializable {
    private Long id;
    private String name = "NA";
    private List<Station> directionStations = new ArrayList();
    private ArrayList<LocoRegion> regions = new ArrayList();
    private HashMap<Long, Long> serviceAvailable = new HashMap<>();
    private HashMap<WeightType, Long> normReserve = new HashMap<>();
    private Long processTime = 0L;
    private Long normTime = 0L;
    private Map<Integer, Long> onlyLocoStopTime = new HashMap<>(); // в зависимости от количества секций: 2-х, 3-х или 4-х секционный локо
    private Map<Integer, Long> onlyTeamStopTime = new HashMap<>(); // в зависимости от количества секций: 2-х, 3-х или 4-х секционный локо
    private Map<Integer, Long> teamLocoStopTime = new HashMap<>(); // в зависимости от количества секций: 2-х, 3-х или 4-х секционный локо
    private Map<Integer, Long> teamBackStopTime = new HashMap<>(); // в зависимости от количества секций: 2-х, 3-х или 4-х секционный локо
    private Map<Integer, Long> locoAfterServiceStopTime = new HashMap<>(); // в зависимости от количества секций: 2-х, 3-х или 4-х секционный локо
    private List<Line> lines = new ArrayList<>();
    private List<Link> links = new ArrayList<>(); //список исходящих линков из станции

    public Station() {
    }

    public Station(Long id) {
        this.id = id;
    }

    public Station(Long id, ArrayList<LocoRegion> regions, HashMap<Long, Long> serviceAvailable) {
        this.id = id;
        this.regions = regions;
        this.serviceAvailable = serviceAvailable;
        this.processTime = 0L;
    }

    public Station(Long id, Long processTime) {
        this.id = id;
        this.processTime = processTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Station station = (Station) o;

        if (id != null ? !id.equals(station.id) : station.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ArrayList<LocoRegion> getRegions() {
        return regions;
    }

    public void setRegions(ArrayList<LocoRegion> regions) {
        this.regions = regions;
    }

    public HashMap<Long, Long> getServiceAvailable() {
        return serviceAvailable;
    }

    public void setServiceAvailable(HashMap<Long, Long> serviceAvailable) {
        this.serviceAvailable = serviceAvailable;
    }

    public Long getProcessTime() {
        return processTime;
    }

    public void setProcessTime(Long processTime) {
        this.processTime = processTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Station");
        sb.append("{id=").append(id);
        if (! name.equals("NA")) sb.append(", name=").append(name);
        sb.append(", regions=").append(regions);
        sb.append(", serviceAvailable=").append(serviceAvailable);
        sb.append(", processTime=").append(processTime);
        sb.append('}');
        return sb.toString();
    }

    public String StringId(){
        final StringBuilder sb = new StringBuilder();
        sb.append("Station");
        sb.append("id").append(id);
        return sb.toString();
    }

    public boolean hasName() {
        return ! name.equals("NA");
    }

    public String getName() {
        if (name.equals("NA"))
            return id.toString();
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Station> getDirectionStations() {
        return directionStations;
    }

    public void setDirectionStations(List<Station> directionStations) {
        this.directionStations = directionStations;
    }

    public Long getNormTime() {
        return normTime;
    }

    public void setNormTime(Long normTime) {
        this.normTime = normTime;
    }

    public HashMap<WeightType, Long> getNormReserve() {
        return normReserve;
    }

    public void setNormReserve(HashMap<WeightType, Long> normReserve) {
        this.normReserve = normReserve;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public void addLine(Line line){
        this.lines.add(line);
    }

    public void delLine(Line line){
        if (this.lines.contains(line))
            this.lines.remove(line);
    }

    public List<Link> getLinks() {
        return links;
    }

    public Map<Integer, Long> getOnlyLocoStopTime() {
        return onlyLocoStopTime;
    }

    public void setOnlyLocoStopTime(Map<Integer, Long> onlyLocoStopTime) {
        this.onlyLocoStopTime = onlyLocoStopTime;
    }

    public Map<Integer, Long> getOnlyTeamStopTime() {
        return onlyTeamStopTime;
    }

    public void setOnlyTeamStopTime(Map<Integer, Long> onlyTeamStopTime) {
        this.onlyTeamStopTime = onlyTeamStopTime;
    }

    public void addLocoRegion(LocoRegion region){
        regions.add(region);
    }

    public Map<Integer, Long> getTeamLocoStopTime() {
        return teamLocoStopTime;
    }

    public void setTeamLocoStopTime(Map<Integer, Long> teamLocoStopTime) {
        this.teamLocoStopTime = teamLocoStopTime;
    }

    public Map<Integer, Long> getLocoAfterServiceStopTime() {
        return locoAfterServiceStopTime;
    }

    public void setLocoAfterServiceStopTime(Map<Integer, Long> locoAfterServiceStopTime) {
        this.locoAfterServiceStopTime = locoAfterServiceStopTime;
    }

    public Map<Integer, Long> getTeamBackStopTime() {
        return teamBackStopTime;
    }

    public void setTeamBackStopTime(Map<Integer, Long> teamBackStopTime) {
        this.teamBackStopTime = teamBackStopTime;
    }
}
