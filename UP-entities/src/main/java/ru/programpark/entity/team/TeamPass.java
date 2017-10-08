package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Station;

/**
 * User: oracle
 * Date: 07.08.15
 * Содержит входную информацю из сообщения:
 * +team_pass(interval(Time, 3600), station(StId), direction(DirId), depot(DepotId), number(quantityToSend))
 */
public class TeamPass {
    private Long time;
    private Long interval;
    private Station station;
    private Integer direction;
    private Station depot;
    private Long teamServiceRegionId;
    private Integer quantityToSend = 0;//number of teams to be sent as passengers from station to depot in the given time period
    private Integer quantitySent = 0;//actually sent according with this teamPass

    public TeamPass(Long time, Long internal, Station station, Integer direction, Station depot, Integer quantityToSend) {
        this.time = time;
        this.interval = internal;
        this.station = station;
        this.direction = direction;
        this.depot = depot;
        this.quantityToSend = quantityToSend;
    }

    public TeamPass(Long time) {
        this.time = time;
    }

    public TeamPass() {
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }

    public Station getDepot() {
        return depot;
    }

    public void setDepot(Station depot) {
        this.depot = depot;
    }

    public Integer getQuantityToSend() {
        return quantityToSend;
    }

    public void setQuantityToSend(Integer quantityToSend) {
        this.quantityToSend = quantityToSend;
    }

    public Integer getQuantitySent() {
        return quantitySent;
    }

    public void setQuantitySent(Integer quantitySent) {
        this.quantitySent = quantitySent;
    }

    public Long getTeamServiceRegionId() {
        return teamServiceRegionId;
    }

    public void setTeamServiceRegionId(Long teamServiceRegionId) {
        this.teamServiceRegionId = teamServiceRegionId;
    }
}
