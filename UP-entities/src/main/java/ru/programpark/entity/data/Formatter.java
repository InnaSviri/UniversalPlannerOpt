package ru.programpark.entity.data;


import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.loco.RealLoco;
import ru.programpark.entity.loco.RealLocoTrack;
import ru.programpark.entity.team.*;
import ru.programpark.entity.train.RealTrain;
import ru.programpark.entity.train.RealTrainTrack;
import ru.programpark.entity.util.BaseParser;
import ru.programpark.entity.util.TermFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Formatter extends TermFormatter {
    OutputData oDATA;

    public Formatter()
    throws IOException, BaseParser.ParseException {
        super();
        this.oDATA = new OutputData();
    }

    public Formatter(OutputData oData)
    throws IOException, BaseParser.ParseException {
        super();
        this.oDATA = oData;
    }

    public void setData(OutputData data) {
        this.oDATA = data;
    }

    {
        final NumberFormat<Long> currentIdTime = new NumberFormat<Long>() {
            public Long get() { return oDATA.getCurrentIdTime(); }
        };
        final NumberFormat<Long> currentIdOrd = new NumberFormat<Long>() {
            public Long get() { return oDATA.getCurrentIdOrd(); }
        };
        final StringFormat version = new StringFormat() {
            public String get() { return oDATA.getVersion(); }
        };
        addFormat("tell(plan_begin, id(Time, Ord))", null, "Time", currentIdTime, "Ord", currentIdOrd);
        addFormat("tell(plan_end, id(Time, Ord))", null, "Time", currentIdTime, "Ord", currentIdOrd);
        addFormat("tell(version(Ver))", null, "Ver", version);
    }

    private class RealTrainFormat extends MultiFormat<RealTrain> {
        @Override
        public Iterator<RealTrain> getIterator() {
            return oDATA.getSlotTrains().values().iterator();
        }

        private Long getId() {
            return object.getTrainId();
        }
        NumberFormat<Long> id = new NumberFormat<Long>() {
            public Long get() { return getId(); }
        };

        private Iterator<RealTrainTrack> getRouteIterator() {
            return object.getRoute().iterator();
        }
        MultiFormat<RealTrainTrack> track = new MultiFormat<RealTrainTrack>() {
            @Override
            public Iterator<RealTrainTrack> getIterator () {
                return getRouteIterator();
            }
        };

        NumberFormat<Long> trackFrom = new NumberFormat<Long>() {
            public Long get() { return track.object.getLink().getFrom().getId(); }
        };
        NumberFormat<Long> trackTo = new NumberFormat<Long>() {
            public Long get() { return track.object.getLink().getTo().getId(); }
        };
        NumberFormat<Long> trackTimeStart = new NumberFormat<Long>() {
            public Long get() { return track.object.getTimeStart(); }
        };
        NumberFormat<Long> trackTimeEnd = new NumberFormat<Long>() {
            public Long get() { return track.object.getTimeEnd(); }
        };
        NumberFormat<Long> trackSlotId = new NumberFormat<Long>() {
            public Long get() { return track.object.getSlotId(); }
        };
    }

    {
        final RealTrainFormat realTrain = new RealTrainFormat();
        addFormat("tell(slot_train(id(Id),                                 " +
                "                route([track(station(St1), station(St2)," +
                "                             time_start(TimeStart),     " +
                "                             time_end(TimeEnd),         " +
                "                             slot_id(SlotId))])))",
                realTrain,
                "Id", realTrain.id,
                "track", realTrain.track,
                "St1", realTrain.trackFrom,
                "St2", realTrain.trackTo,
                "TimeStart", realTrain.trackTimeStart,
                "TimeEnd", realTrain.trackTimeEnd,
                "SlotId", realTrain.trackSlotId);
    }


    private class RealLocoFormat extends MultiFormat<RealLoco> {
        @Override
        public Iterator<RealLoco> getIterator() {
            return oDATA.getSlotLocos().values().iterator();
        }

        private Long getId() {
            return object.getRealLocoId();
        }
        NumberFormat<Long> id = new NumberFormat<Long>() {
            public Long get() { return getId(); }
        };

        private Iterator<RealLocoTrack> getRouteIterator() {
            return object.getRoute().iterator();
        }
        MultiFormat<RealLocoTrack> track = new MultiFormat<RealLocoTrack>() {
            @Override
            public Iterator<RealLocoTrack> getIterator () {
                return getRouteIterator();
            }
        };

        NumberFormat<Long> trackFrom = new NumberFormat<Long>() {
            public Long get() { return track.object.getLink().getFrom().getId(); }
        };
        NumberFormat<Long> trackTo = new NumberFormat<Long>() {
            public Long get() { return track.object.getLink().getTo().getId(); }
        };
        NumberFormat<Long> trackTimeStart = new NumberFormat<Long>() {
            public Long get() { return track.object.getTimeStart(); }
        };
        NumberFormat<Long> trackTimeEnd = new NumberFormat<Long>() {
            public Long get() { return track.object.getTimeEnd(); }
        };
        NumberFormat<Long> trackSlotId = new NumberFormat<Long>() {
            public Long get() { return track.object.getSlotId(); }
        };
        NumberFormat<Integer> trackLocoState = new NumberFormat<Integer>() {
            public Integer get() {
                return track.object.getState().ordinal();
            }
        };
        NumberFormat<Long> trackTrainId = new NumberFormat<Long>() {
            public Long get() { return track.object.getTrainId(); }
        };
    }

    {
        final RealLocoFormat realLoco = new RealLocoFormat();
        addFormat("tell(slot_loco(id(Id),                                 " +
                  "               route([track(station(St1), station(St2)," +
                  "                            time_start(TimeStart),     " +
                  "                            time_end(TimeEnd),         " +
                  "                            slot_id(SlotId),           " +
                  "                            state(State),              " +
                  "                            train(TrainId))])))",
                  realLoco,
                  "Id", realLoco.id,
                  "track", realLoco.track,
                  "St1", realLoco.trackFrom,
                  "St2", realLoco.trackTo,
                  "TimeStart", realLoco.trackTimeStart,
                  "TimeEnd", realLoco.trackTimeEnd,
                  "SlotId", realLoco.trackSlotId,
                  "State", realLoco.trackLocoState,
                  "TrainId", realLoco.trackTrainId);
    }

    private class RealTeamFormat extends MultiFormat<RealTeam> {
        @Override
        public Iterator<RealTeam> getIterator() {
            return oDATA.getSlotTeams().values().iterator();
        }

        private Long getId() {
            return object.getId();
        }
        NumberFormat<Long> id = new NumberFormat<Long>() {
            public Long get() { return getId(); }
        };

        private Iterator<RealTeamTrack> getRouteIterator() {
            return object.getRoute().iterator();
        }
        MultiFormat<RealTeamTrack> track = new MultiFormat<RealTeamTrack>() {
            @Override
            public Iterator<RealTeamTrack> getIterator () {
                return getRouteIterator();
            }
        };

        NumberFormat<Long> trackFrom = new NumberFormat<Long>() {
            public Long get() { return track.object.getLink().getFrom().getId(); }
        };
        NumberFormat<Long> trackTo = new NumberFormat<Long>() {
            public Long get() { return track.object.getLink().getTo().getId(); }
        };
        NumberFormat<Long> trackTimeStart = new NumberFormat<Long>() {
            public Long get() { return track.object.getTimeStart(); }
        };
        NumberFormat<Long> trackTimeEnd = new NumberFormat<Long>() {
            public Long get() { return track.object.getTimeEnd(); }
        };
        NumberFormat<Long> trackSlotId = new NumberFormat<Long>() {
            public Long get() { Long slotId = track.object.getSlotId();
                return (slotId == null) ? -1L : slotId; }
        };
        NumberFormat<Integer> trackTeamState = new NumberFormat<Integer>() {
            public Integer get() {
                return track.object.getState().ordinal();
            }
        };
        NumberFormat<Long> trackLocoId = new NumberFormat<Long>() {
            public Long get() { return track.object.getLocoId(); }
        };
    }

    {
        final RealTeamFormat realTeam = new RealTeamFormat();
        addFormat("tell(slot_team(id(Id),                                 " +
                "               route([track(station(St1), station(St2)," +
                "                            time_start(TimeStart),     " +
                "                            time_end(TimeEnd),         " +
                "                            slot_id(SlotId),           " +
                "                            state(State),              " +
                "                            loco(LocoId))])))",
                realTeam,
                "Id", realTeam.id,
                "track", realTeam.track,
                "St1", realTeam.trackFrom,
                "St2", realTeam.trackTo,
                "TimeStart", realTeam.trackTimeStart,
                "TimeEnd", realTeam.trackTimeEnd,
                "SlotId", realTeam.trackSlotId,
                "State", realTeam.trackTeamState,
                "LocoId", realTeam.trackLocoId);
    }

    private class AnalyzeFormat extends MultiFormat<AnalyzeCriterion> {
        @Override
        public Iterator<AnalyzeCriterion> getIterator() {
            return new Iterator<AnalyzeCriterion>() {
                Iterator<AnalyzeCriterion> critIterator =
                    oDATA.getAnalyzeCriteria().values().iterator();
                @Override public boolean hasNext() {
                    return critIterator.hasNext();
                }
                @Override public AnalyzeCriterion next() {
                    AnalyzeCriterion next = critIterator.next();
                    ListFormat info = new ListFormat(next.getInfoString());
                    return next;
                }
                @Override public void remove() {
                    critIterator.remove();
                }
            };
        }

        NumberFormat<Long> id = new NumberFormat<Long>() {
            public Long get() { return object.getId(); }
        };
        NumberFormat<Double> value = new NumberFormat<Double>() {
            public Double get() { return object.getValue(); }
        };
        private Iterator<AnalyzerInfoEntry> getItemIterator() {
            return object.getInfo().iterator();
        }

        MultiFormat<AnalyzerInfoEntry> item = new MultiFormat<AnalyzerInfoEntry>() {
            @Override
            public Iterator<AnalyzerInfoEntry> getIterator () {
                return getItemIterator();
            }
        };

        NumberFormat<Long> trainId = new NumberFormat<Long>() {
            public Long get() { return item.object.trainId; }
        };
        NumberFormat<Long> locoId = new NumberFormat<Long>() {
            public Long get() { return item.object.locoId; }
        };
        NumberFormat<Long> teamId = new NumberFormat<Long>() {
            public Long get() { return item.object.teamId; }
        };
        NumberFormat<Long> stationFromId = new NumberFormat<Long>() {
            public Long get() { return item.object.stationFromId; }
        };
        NumberFormat<Long> stationToId = new NumberFormat<Long>() {
            public Long get() { return item.object.stationToId; }
        };
        NumberFormat<Long> stationAtId = new NumberFormat<Long>() {
            public Long get() { return item.object.stationAtId; }
        };
        NumberFormat<Long> timeStart = new NumberFormat<Long>() {
            public Long get() { return item.object.timeStart; }
        };
        NumberFormat<Long> timeEnd = new NumberFormat<Long>() {
            public Long get() { return item.object.timeEnd; }
        };
        NumberFormat<Long> timeDuration = new NumberFormat<Long>() {
            public Long get() { return item.object.timeDuration; }
        };
        NumberFormat<Long> factTime = new NumberFormat<Long>() {
            public Long get() { return item.object.factTime; }
        };
        NumberFormat<Long> byPassTrain = new NumberFormat<Long>() {
            public Long get() { return item.object.byPassTrain; }
        };
    }

    {
        final AnalyzeFormat analyze = new AnalyzeFormat();
        addFormat("tell(analyze(id(Id), value(Value), info([item(" +
                            " train(TrainId)," +
                            " loco(LocoId)," +
                            " team(TeamId)," +
                            " station_from(StationFromId)," +
                            " station_to(StationToId)," +
                            " station(StationAtId)," +
                            " time_start(TimeStart)," +
                            " time_end(TimeEnd)," +
                            " time_duration(TimeDuration)," +
                            " fact_time(FactTime)," +
                            " by_pass_train(ByPassTrain))])))",
                analyze,
                "Id", analyze.id,
                "Value", analyze.value,
                "item", analyze.item,
                "TrainId", analyze.trainId,
                "LocoId", analyze.locoId,
                "TeamId", analyze.teamId,
                "StationFromId", analyze.stationFromId,
                "StationToId", analyze.stationToId,
                "StationAtId", analyze.stationAtId,
                "TimeStart", analyze.timeStart,
                "TimeEnd", analyze.timeEnd,
                "TimeDuration", analyze.timeDuration,
                "FactTime", analyze.factTime,
                "ByPassTrain", analyze.byPassTrain);
    }

    private class AnalyzeItemFormat extends MultiFormat<AnalyzeCriterion> {
        @Override
        public Iterator<AnalyzeCriterion> getIterator() {
            return new Iterator<AnalyzeCriterion>() {
                Iterator<AnalyzeCriterion> critIterator =
                        oDATA.getAnalyzeCriteria().values().iterator();
                @Override public boolean hasNext() {
                    return critIterator.hasNext();
                }
                @Override public AnalyzeCriterion next() {
                    AnalyzeCriterion next = critIterator.next();
                    ListFormat info = new ListFormat(next.getInfoString());
                    return next;
                }
                @Override public void remove() {
                    critIterator.remove();
                }
            };
        }

        ListFormat infoString = new ListFormat() {
            @Override
            public BaseParser.Term unfold() {
                BaseParser.ListTerm term = new BaseParser.ListTerm();
                List<String> curinfo = object.getInfoString();
                int len = curinfo.size();
                term.elements = new ArrayList<BaseParser.Term>(len);
                for (int i = 0; i < len; i++) {
                    term.elements.add(new StringFormat(curinfo.get(i)));
                }
                return (BaseParser.Term) term;
            }
        };

        NumberFormat<Long> id = new NumberFormat<Long>() {
            public Long get() { return object.getId(); }
        };
        NumberFormat<Double> value = new NumberFormat<Double>() {
            public Double get() { return object.getValue(); }
        };
    }
    {
        final AnalyzeItemFormat log_analyze = new AnalyzeItemFormat();
        addFormat("item(id(Id), value(Value), info(InfoString))",
                log_analyze, "Id", log_analyze.id, "Value", log_analyze.value, "InfoString",
                log_analyze.infoString);
    }

    private class worklessTeamFormat extends MultiFormat<FactTeam> {
        @Override
        public Iterator<FactTeam> getIterator() {
            return oDATA.getWorklessTeams().values().iterator();
        }

        private Long getId() {
            return object.getId();
        }
        NumberFormat<Long> id = new NumberFormat<Long>() {
            public Long get() { return getId(); }
        };

    }

    {
        final worklessTeamFormat worklessTeam = new worklessTeamFormat();
        addFormat("tell(workless_team(id(Id)))", worklessTeam, "Id", worklessTeam.id);
    }

    private class failedTeamPassFormat extends MultiFormat<TeamPass> {
        @Override
        public Iterator<TeamPass> getIterator() {
            return oDATA.getFailedTeamPassList().iterator();
        }

        NumberFormat<Long> time = new NumberFormat<Long>() {
            public Long get() { return object.getTime();}
        };

        NumberFormat<Long> interval = new NumberFormat<Long>() {
            public Long get() { return object.getInterval();}
        };

        NumberFormat<Long> station = new NumberFormat<Long>() {
            public Long get() { return object.getStation().getId();}
        };

        NumberFormat<Long> regionId = new NumberFormat<Long>() {
            public Long get() {
                if (object.getTeamServiceRegionId() == null)
                    return -1L;
                return object.getTeamServiceRegionId();
            }
        };

        NumberFormat<Integer> dir = new NumberFormat<Integer>() {
            public Integer get() { return object.getDirection();}
        };

        NumberFormat<Long> depot = new NumberFormat<Long>() {
            public Long get() { return object.getDepot().getId();}
        };

        NumberFormat<Integer> number = new NumberFormat<Integer>() {
            public Integer get() { return object.getQuantityToSend();}
        };

        NumberFormat<Integer> sent = new NumberFormat<Integer>() {
            public Integer get() { return object.getQuantitySent();}
        };
    }

    {
        final failedTeamPassFormat failedTeamPass = new failedTeamPassFormat();
        addFormat("tell(fail_team_pass(interval(Time, Interval), station(St), team_service_region(RegId), direction(Dir), depot(Depot), number(N), sent(K)))", failedTeamPass, "Time", failedTeamPass.time,
                "Interval", failedTeamPass.interval, "St", failedTeamPass.station, "RegId", failedTeamPass.regionId,"Dir", failedTeamPass.dir, "Depot", failedTeamPass.depot, "N", failedTeamPass.number, "K", failedTeamPass.sent);

    }

    private class failedPinnedTeamFormat extends MultiFormat<PinnedTeam> {
        @Override
        public Iterator<PinnedTeam> getIterator() {
            return oDATA.getFailedPinnedTeamList().iterator();
        }

        NumberFormat<Long> stationId = new NumberFormat<Long>() {
            public Long get() { return object.getStationId();}
        };

        NumberFormat<Long> teamId = new NumberFormat<Long>() {
            public Long get() {
                return object.getTeamId();
            }
        };

        NumberFormat<Long> locoId = new NumberFormat<Long>() {
            public Long get() {
                return object.getLocoId();
            }
        };

        NumberFormat<Long> trainId = new NumberFormat<Long>() {
            public Long get() {
                return object.getTrainId();
            }
        };
    }

    {
        final failedPinnedTeamFormat failedPinnedTeam = new failedPinnedTeamFormat();
        addFormat("tell(fail_pinned_team(id(TeamId), station(StId), train(TrainId), loco(LocoId)))", failedPinnedTeam, "TeamId", failedPinnedTeam.teamId,
                  "StId", failedPinnedTeam.stationId, "TrainId", failedPinnedTeam.trainId, "LocoId", failedPinnedTeam.locoId);

    }


    private class actualTeamPercentForDepotOneFormat extends MultiFormat<TeamRegion> {
        @Override
        public Iterator<TeamRegion> getIterator() {
            return oDATA.getRegionsWithActualPercent().iterator();
        }

        NumberFormat<Long> id = new NumberFormat<Long>() {
            public Long get() { return object.getId();}
        };

        Station depot;
        NumberFormat<Long> station = new NumberFormat<Long>() {
            public Long get() {
                List<Station> depots = new ArrayList<>();
                depots.addAll(object.getPercentByDepot().keySet());
                depot = depots.get(0);
                return depot.getId();
            }
        };

        NumberFormat<Long> percent = new NumberFormat<Long>() {
            public Long get() {
                return (long) (object.getPercentByDepot().get(depot));
            }
        };

    }

    private class actualTeamPercentForDepotTwoFormat extends MultiFormat<TeamRegion> {
        @Override
        public Iterator<TeamRegion> getIterator() {
            return oDATA.getRegionsWithActualPercent().iterator();
        }

        NumberFormat<Long> id = new NumberFormat<Long>() {
            public Long get() { return object.getId();}
        };

        Station depot;
        NumberFormat<Long> station = new NumberFormat<Long>() {
            public Long get() {
                List<Station> depots = new ArrayList<>();
                depots.addAll(object.getPercentByDepot().keySet());
                depot = depots.get(1);
                return depot.getId();
            }
        };

        NumberFormat<Long> percent = new NumberFormat<Long>() {
            public Long get() {
                return (long) (object.getPercentByDepot().get(depot));
            }
        };

    }


    {
        final actualTeamPercentForDepotOneFormat percentOne = new actualTeamPercentForDepotOneFormat();
        addFormat("tell(actual_team_percent(service_region(Id), depot(StId), percent(P)))", percentOne,
                "Id", percentOne.id, "StId", percentOne.station, "P", percentOne.percent);

        final actualTeamPercentForDepotTwoFormat percentTwo = new actualTeamPercentForDepotTwoFormat();
        addFormat("tell(actual_team_percent(service_region(Id), depot(StId), percent(P)))", percentTwo,
                "Id", percentTwo.id, "StId", percentTwo.station, "P", percentTwo.percent);
    }

    private class configFromVectorFormat extends MultiFormat<Map.Entry<String, Number>> {
        @Override
        public Iterator<Map.Entry<String, Number>> getIterator() {
            return oDATA.getConfigFromVector().entrySet().iterator();
        }

        StringFormat stringVal = new StringFormat() {
            public String get() { return object.getKey();}
        };

        NumberFormat numVal = new NumberFormat() {
            public Number get() {
                return object.getValue();
            }
        };
    }

    {
        final configFromVectorFormat config = new configFromVectorFormat();
        addFormat("tell(config(String,Number))", config,
                "String", config.stringVal, "Number", config.numVal);
    }

}
