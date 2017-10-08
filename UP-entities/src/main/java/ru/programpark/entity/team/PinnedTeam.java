package ru.programpark.entity.team;

/**
 * Created by oracle on 28.09.2015.
 * Объектная бригадная корректировка
 */
public class PinnedTeam {
    private Long teamId;// – идентификатор бригады, которая закрепляется за локомотивом.
    private Long locoId; //– идентификатор локомотива, к которому прикрепляется бригада.
    private Long trainId;// – идентификатор поезда, к которому прикрепляется бригада.
    private Long stationId;// StId – идентификатор станции, на которой происходит прикрепление.

    public PinnedTeam() {
    }

    public PinnedTeam(Long teamId) {
        this.teamId = teamId;
    }

    public PinnedTeam(Long teamId, Long locoId, Long trainId, Long stationId) {
        this.teamId = teamId;
        this.locoId = locoId;
        this.trainId = trainId;
        this.stationId = stationId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getLocoId() {
        return locoId;
    }

    public void setLocoId(Long locoId) {
        this.locoId = locoId;
    }

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }
}
