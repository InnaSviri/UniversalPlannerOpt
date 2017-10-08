package ru.programpark.entity.util;

import ru.programpark.entity.data.AnalyzeCriterion;
import ru.programpark.entity.raw_entities.BaseTrack;
import ru.programpark.entity.raw_entities.SlotLoco;
import ru.programpark.entity.raw_entities.SlotTeam;
import ru.programpark.entity.raw_entities.SlotTrain;
import ru.programpark.entity.team.FactTeam;

import java.io.IOException;
import java.util.*;

public class ResultParser extends MatchingParser {
    public Map<Long, SlotTrain> slotTrains = new HashMap<>();
    public Map<Long, SlotLoco> slotLocos = new HashMap<>();
    public Map<Long, SlotTeam> slotTeams = new HashMap<>();
    public Map<Long, FactTeam> worklessTeams = new HashMap<>();
    public SlotTrain trainTrackCreator = new SlotTrain();
    public SlotLoco locoTrackCreator = new SlotLoco();
    public SlotTeam teamTrackCreator = new SlotTeam();
    public Map<Long, AnalyzeCriterion> criterions = new HashMap<>();

    public ResultParser(List<String> input) throws IOException, ParseException {
        parseAll(input, Datum.Op.add, true);
    }

    {
        addPattern("tell(plan_begin,_)");
        addPattern("tell(fail_team_pass(interval(_,_),station(_),team_service_region(_),direction(_),depot(_),number(_),sent(_)))");
        addPattern("tell(version(_))");
        addPattern("tell(actual_team_percent(service_region(_),depot(_),percent(_)))");
        addPattern("tell(plan_end,id(_,_))");
    }
    //workless_team
   {
        WorklessTeamSetter wTeam = new WorklessTeamSetter();
        addPattern("tell(workless_team(id(Id)))", wTeam, "Id", wTeam.id);
    }

    private class WorklessTeamSetter extends Setter<FactTeam>{
        public void add(FactTeam w) {
            worklessTeams.put(w.getId(), w);
        }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                object.setId(term.longValue());
                return true;
            }
        };

    }

    //slot_train
    {
        SlotTrainSetter slotTrainSetter = new SlotTrainSetter();
        addPattern("tell(slot_train(id(Id),route([track(station(From),station(To),time_start(TimeStart),time_end(TimeEnd),slot_id(SlotId))])))",
                slotTrainSetter, "Id", slotTrainSetter.id,
                "track", slotTrainSetter.track, "From", slotTrainSetter.track.from, "To", slotTrainSetter.track.to,
                "TimeStart", slotTrainSetter.track.timeStart, "TimeEnd", slotTrainSetter.track.timeEnd,
                "SlotId", slotTrainSetter.track.slotId);
    }

    private class SlotTrainSetter extends Setter<SlotTrain> {
        @Override
        public void add(SlotTrain datum) {
            slotTrains.put(datum.id, datum);
        }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                object.id = term.longValue();
                return true;
            }
        };

        TrainTrackWrapper track = new TrainTrackWrapper() {
            @Override
            public void close() {
                object.route.add(this.track);
                super.close();
            }
        };
    }

    private abstract class BaseTrackWrapper extends Wrapper {
        abstract protected BaseTrack getTrack();

        @Override public void open() {
        }
        @Override public void close() {
        }

        Matcher<NumberTerm> from = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                getTrack().stationFromId = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> to = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                getTrack().stationToId = term.longValue();
                return true;
            }
        };
        Matcher<NumberTerm> timeStart = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                getTrack().timeStart = term.longValue();
                return true;
            }
        };
        Matcher<NumberTerm> timeEnd = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                getTrack().timeEnd = term.longValue();
                return true;
            }
        };
        Matcher<NumberTerm> slotId = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                getTrack().slotId = term.longValue();
                return true;
            }
        };
    }

    private class TrainTrackWrapper extends BaseTrackWrapper {
        SlotTrain.Track track;

        @Override
        protected BaseTrack getTrack() {
            return track;
        }

        @Override public void open() {
            super.open();
            track = trainTrackCreator.new Track();
        }
        @Override public void close() {
            track = null;
        }

    }

    //slot_loco
    {
        SlotLocoSetter slotLocoSetter = new SlotLocoSetter();
        addPattern("tell(slot_loco(id(Id),route([track(station(From),station(To),time_start(TimeStart),time_end(TimeEnd),slot_id(SlotId),state(State),train(TrainId))])))",
                slotLocoSetter, "Id", slotLocoSetter.id,
                "track", slotLocoSetter.track, "From", slotLocoSetter.track.from, "To", slotLocoSetter.track.to,
                "TimeStart", slotLocoSetter.track.timeStart, "TimeEnd", slotLocoSetter.track.timeEnd,
                "SlotId", slotLocoSetter.track.slotId,
                "State", slotLocoSetter.track.state, "TrainId", slotLocoSetter.track.trainId);

    }
    private class SlotLocoSetter extends Setter<SlotLoco> {
        @Override
        public void add(SlotLoco datum) {
            slotLocos.put(datum.id, datum);
        }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                object.id = term.longValue();
                return true;
            }
        };

        LocoTrackWrapper track = new LocoTrackWrapper() {
            @Override
            public void close() {
                object.route.add(this.track);
                super.close();
            }
        };
    }

    private class LocoTrackWrapper extends BaseTrackWrapper {
        SlotLoco.Track track;
        @Override
        protected BaseTrack getTrack() {
            return track;
        }

        @Override
        public void open() {
            super.open();
            track = locoTrackCreator.new Track();
        }

        @Override
        public void close() {
            track = null;
        }

        Matcher<NumberTerm> state = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                track.state = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> trainId = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                track.trainId = term.longValue();
                return true;
            }
        };
    }

    //slot_team
    {
        SlotTeamSetter slotTeamSetter = new SlotTeamSetter();
        addPattern("tell(slot_team(id(Id),route([track(station(From),station(To),time_start(TimeStart),time_end(TimeEnd),slot_id(SlotId),state(State),loco(LocoId))])))",
                slotTeamSetter, "Id", slotTeamSetter.id,
                "track", slotTeamSetter.track, "From", slotTeamSetter.track.from, "To", slotTeamSetter.track.to,
                "TimeStart", slotTeamSetter.track.timeStart, "TimeEnd", slotTeamSetter.track.timeEnd,
                "SlotId", slotTeamSetter.track.slotId,
                "State", slotTeamSetter.track.state, "LocoId", slotTeamSetter.track.locoId);

    }
    private class SlotTeamSetter extends Setter<SlotTeam> {
        @Override
        public void add(SlotTeam datum) {
            slotTeams.put(datum.id, datum);
        }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                object.id = term.longValue();
                return true;
            }
        };

        TeamTrackWrapper track = new TeamTrackWrapper() {
            @Override
            public void close() {
                object.route.add(this.track);
                super.close();
            }
        };
    }

    private class TeamTrackWrapper extends BaseTrackWrapper {
        SlotTeam.Track track;
        @Override
        protected BaseTrack getTrack() {
            return track;
        }

        @Override
        public void open() {
            super.open();
            track = teamTrackCreator.new Track();
        }

        @Override
        public void close() {
            track = null;
        }

        Matcher<NumberTerm> state = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                track.state = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> locoId = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                track.locoId = term.longValue();
                return true;
            }
        };
    }

    //analyze
    {
        CriterionSetter setter = new CriterionSetter();
        addPattern("tell(analyze(id(Id),value(Value),_))", setter,
                "Id", setter.id, "Value", setter.value);
    }

    private class CriterionSetter extends Setter<AnalyzeCriterion> {
        @Override
        public void add(AnalyzeCriterion datum) {
            criterions.put(datum.getId(), datum);
        }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                object.setId(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> value = new Matcher<NumberTerm>() {
            @Override
            public Boolean match(NumberTerm term) {
                object.setValue(term.doubleValue());
                return true;
            }
        };
    }



}