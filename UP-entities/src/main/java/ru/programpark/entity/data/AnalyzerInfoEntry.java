package ru.programpark.entity.data;

/**
 * Created by oracle on 22.10.2015.
 */

public class AnalyzerInfoEntry {
    public Long trainId;
    public Long locoId;
    public Long teamId;
    public Long stationFromId;
    public Long stationToId;
    public Long stationAtId;
    public Long timeStart;
    public Long timeEnd;
    public Long timeDuration;
    public Long factTime;
    public Long byPassTrain;

    public AnalyzerInfoEntry() {
        Long defaultValue = -1L;
        this.trainId = defaultValue;
        this.locoId = defaultValue;
        this.teamId = defaultValue;
        this.stationFromId = defaultValue;
        this.stationToId = defaultValue;
        this.stationAtId = defaultValue;
        this.timeStart = defaultValue;
        this.timeEnd = defaultValue;
        this.timeDuration = defaultValue;
        this.factTime = defaultValue;
        this.byPassTrain = defaultValue;
    }

}


    /* format:
       item(train(trainId),
        loco(locoId),
        team(teamId),
        station_from(stationFromId),
        station_to(stationToId),
        station(stationAtId),
        time_start(timeStart),
        time_end(timeEnd),
        time_duration(timeDuration),
		fact_time(factTime),
        by_pass_train(byPassTrain))
     */

