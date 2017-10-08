package ru.programpark.entity.data;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.fixed.Line;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.*;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.team.*;
import ru.programpark.entity.train.*;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.MatchingParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User:  olga
 * Date: 22.05.2014
 */
public class Parser extends MatchingParser{
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(Parser.class);
        return logger;
    }

    private final InputData DATA = new InputData();

    public Parser() throws IOException, ParseException {
        super();
    }

    public Parser(String[] addPercepts, String[] delPercepts)
    throws IOException, ParseException {
        this(addPercepts, delPercepts, null);
    }

    public Parser(String[] addPercepts, String[] delPercepts,
                  InputData priorData)
    throws IOException, ParseException {
        super();
        if (priorData != null) DATA.addAllReferenceData(priorData);
        parseAll(Arrays.asList(addPercepts), Parser.Datum.Op.add, true);
        parseAll(Arrays.asList(delPercepts), Parser.Datum.Op.del, true);
        for (String s: addPercepts){
            LoggingAssistant.getInputWriter().write(s + "\n");
        }
        for (String s: delPercepts){
            LoggingAssistant.getInputWriter().write(s + "\n");
        }
    }

    public InputData getData() {
        return DATA;
    }

    //////// Служебные отметки и неиспользуемые данные /////////////////////////
    {
        final Matcher<NumberTerm> matchCurrentTime =
            new Matcher<NumberTerm>() {
                public Boolean match(NumberTerm term) {
                    DATA.setCurrentTime(term.longValue());
                    return true;
                }
            };
        addPattern("current_time(Time)", new TrivialAdder<Object>() {}, "Time", matchCurrentTime);

        final Matcher<NumberTerm> matchCurrentIdTime =
            new Matcher<NumberTerm>() {
                public Boolean match(NumberTerm term) {
                    DATA.setCurrentIdTime(term.longValue());
                    return true;
                }
            };
        final Matcher<NumberTerm> matchCurrentIdOrd =
            new Matcher<NumberTerm>() {
                public Boolean match(NumberTerm term) {
                    DATA.setCurrentIdOrd(term.longValue());
                    return true;
                }
            };
        addPattern("current_id(Time, N)", new TrivialAdder<Object>() {},
                   "Time", matchCurrentIdTime, "N", matchCurrentIdOrd);

        // unused data // add team, loco, train - not used any more
        //addPattern("line(id(_),station(_),out(_),in(_))"); added
        //addPattern("line(id(_),station(_),out(_),in(_),type(_),length(_))"); added
        addPattern("push_series(track(_, _),main_series(_, _),push_series(_))");
        //addPattern("process(station(_),_)");  added
        //addPattern("process(station(_),track(station(_),station(_)),_)");  added
        //addPattern("slot_pass(id(_),category(_),route(_))");  added
        //addPattern("train_length_limit(track(station(_), station(_)), short_max(_))"); added
        //addPattern("train_length_limit(default, short_max(_))"); added
        //addPattern("priority_station(_)"); added
        //addPattern("task(id(_), interval(_, _), routes(_), weight_type(_), _)");  added
        //addPattern("train_info(id(_), category(_), weight(_), length(_), route(_))");  added
        //addPattern("team_attributes(id(_),attributes(_))"); added
        //addPattern("fact_team(id(_), fact_time(_), location(station(_)))"); added
        //addPattern("fact_team(id(_), fact_time(_), location(track(_)))"); added
        //addPattern("fact_team(id(_),fact_time(_),location(station(_),arrive_time(_),state(_),loco(_)))"); added
        //addPattern("fact_team_next_rest(id(_),fact_time(_),time_to_rest(_))"); added
        //addPattern("fact_team_presence(id(_),fact_time(_), station(_))"); added
        //addPattern("loco_relocation(interval(_,_), track_from(station(_),station(_)), track_to(station(_),station(_)), number(_))"); added
        //addPattern("loco_relocation(interval(_,_), station(_), direction(_), number(_))"); added
        addPattern("team_reserve_region(id(_),tracks([track(station(_), station(_),attributes([_]))])," +
                "work_time(with_rest(_),without_rest(_)))");
        addPattern("tell(slot_train(id(_), route(_)))");
        addPattern("slot_train(id(_), route(_))");
        addPattern("tell(slot_loco(id(_), route(_)))");
        addPattern("slot_loco(id(_), route(_))");
        addPattern("tell(slot_team(id(_), route(_)))");
        addPattern("slot_team(id(_), route(_))");
        addPattern("train_matching(virtual_id(_),real_id(_))");
        addPattern("loco(id(_),loco_region(_),route(_),weight_type(_))");
        addPattern("team(id(_),team_region(_),route(_))");
        addPattern("tell(fail_team_pass(interval(_,_),station(_),team_service_region(_),direction(_),depot(_)," +
                "number(_),sent(_)))");
        addPattern("tell(version(_))");
        addPattern("tell(actual_team_percent(service_region(_),depot(_),percent(_)))");
        addPattern("tell(plan_end,id(_,_))");
        addPattern("tell(config(_,_))");
        addPattern("tell(A)", new TrivialAdder<Object>() {},
                   "A", new Matcher<CompoundTerm>() {
                    @Override
                    public Boolean match(CompoundTerm term) {
                        parse(term);
                        return true;
                    }
                }
        );
    }


    //////// Станции и перегоны и станционные пути ////////////////////////////////////////////////
    private class StationSetter extends Setter<Station> {
        public void add(Station st) { DATA.addStation(st); }
        public void del(Station st) { DATA.delStation(st); }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object = DATA.getStationById(term.longValue());
                DATA.primaryMessageCount++;
                DATA.stationCount++;
                DATA.uniqueStations.add(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> regionId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                LocoRegion region = DATA.getLocoRegionById(term.longValue());
                object.addLocoRegion(region);
                return true;
            }
        };

        Matcher<NumberTerm> normTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setNormTime(term.longValue());
                return true;
            }
        };

        private Long curServiceType;
        Matcher<NumberTerm> serviceType = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                curServiceType = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> serviceDuration = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.getServiceAvailable().put(curServiceType, term.longValue());
                return true;
            }
        };

        private Integer N;
        Matcher<NumberTerm> section = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                N = term.intValue();
                return true;
            }
        };

        Matcher<NumberTerm> onlyTeamTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.getOnlyTeamStopTime().put(N, term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> teamLocoTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.getTeamLocoStopTime().put(N, term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> teamBackTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.getTeamBackStopTime().put(N, term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> onlyLocoTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.getOnlyLocoStopTime().put(N, term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> locoAfterServiceTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.getLocoAfterServiceStopTime().put(N, term.longValue());
                return true;
            }
        };
    }

    private class StationAuxSetter extends TrivialAdder<Station> {
        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                DATA.primaryMessageCount++;
                object = DATA.getStationById(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> priority = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                id.match(term);
                DATA.addPriorityStation(object);
                return true;
            }
        };

        Matcher<StringTerm> name = new Matcher<StringTerm>() {
            public Boolean match(StringTerm term) {
                if (object != null) DATA.mapStationName(object, term.value);
                return true;
            }
        };

        Matcher<NumberTerm> processTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                if (object != null) object.setProcessTime(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> normTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                if (object != null) object.setNormTime(term.longValue());
                return true;
            }
        };
    }

    {
        final StationSetter stOld = new StationSetter();
        addPattern("station(id(Id), loco_region(RegId)," +
                        "service([type(id(ServiceType),duration(ServiceDuration))])," +
                        "norm_reserve([_]), norm_time(NormTime))",
                stOld,
                "Id", stOld.id,
                "RegId", stOld.regionId,
                "ServiceType", stOld.serviceType,
                "ServiceDuration", stOld.serviceDuration,
                "NormTime", stOld.normTime);

        final StationSetter st = new StationSetter();
        addPattern("station(id(Id), loco_region(RegId)," +
                        "service([type(id(ServiceType),duration(ServiceDuration))])," +
                        "norm_reserve([_]), stop_time(" +
                        "only_team([time(sec(Sec), TimeOT)])," +
                        "team_loco([time(sec(Sec), TimeTL)])," +
                        "team_back([time(sec(Sec), TimeTB)])," +
                        "only_loco([time(sec(Sec), TimeOL)])," +
                        "loco_after_service([time(sec(Sec), TimeLAS)])" +
                        "))",
                st,
                "Id", st.id,
                "RegId", st.regionId,
                "ServiceType", st.serviceType,
                "ServiceDuration", st.serviceDuration,
                "Sec", st.section, "TimeOT", st.onlyTeamTime,
                "TimeTL", st.teamLocoTime,
                "TimeTB", st.teamBackTime,
                "TimeOL", st.onlyLocoTime,
                "TimeLAS", st.locoAfterServiceTime);

        final StationAuxSetter priorityStation = new StationAuxSetter();
        addPattern("priority_station(Id)", priorityStation,"Id", priorityStation.priority);

        final StationAuxSetter stationName = new StationAuxSetter();
        addPattern("station_name(Id, Name)", stationName,"Id", stationName.id,"Name", stationName.name);

        final StationAuxSetter process = new StationAuxSetter();
        addPattern("process(station(Id), Time)", process,"Id", process.id,"Time", process.processTime);

        // Нужно только в default.percepts
        final StationAuxSetter norm = new StationAuxSetter();
        addPattern("norm_time(station(Id), Time)", norm, "Id", norm.id, "Time", norm.normTime);

        final StationAuxSetter processWithTrack = new StationAuxSetter();
        addPattern("process(station(Id), track(station(_), station(_)), Time)",processWithTrack,"Id",
                processWithTrack.id,"Time", processWithTrack.processTime);
    }

    private class LinkSetter extends Setter<Link> {
        public void add(Link lnk) { DATA.addLink(lnk); }
        public void del(Link lnk) { DATA.delLink(lnk); }

        Matcher<NumberTerm> trackFrom = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                DATA.primaryMessageCount++;
                Station st1 = DATA.getStationById(term.longValue());
                object.setFrom(st1);
                object.setLinkMessage(true);
                return true;
            }
        };

        Matcher<NumberTerm> trackTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st2 = DATA.getStationById(term.longValue());
                object.setTo(st2);
                return true;
            }
        };

        Matcher<NumberTerm> duration = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setDefaultDuration(term.longValue());
                DATA.linkCount++;
                return true;
            }
        };

        Matcher<NumberTerm> distance = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setDistance(term.intValue());
                return true;
            }
        };

        Matcher<NumberTerm> push = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setPush(term.booleanValue());
                return true;
            }
        };

        Matcher<NumberTerm> direction = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                List<Link> links = object.getFrom().getLinks();
                object.setDirection(term.intValue());
                links.add(object);
                return true;
            }
        };

        Matcher<NumberTerm> lines = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setLines(term.intValue());
                return true;
            }
        };
    }

    {
        final LinkSetter link = new LinkSetter();
        addPattern("link(track(station(St1), station(St2)),attributes([duration(Dur), distance(Dist),push(Push), " +
                        "direction(Dir)]))", link,
                "St1", link.trackFrom, "St2", link.trackTo,
                "Dur", link.duration, "Dist", link.distance,
                "Push", link.push, "Dir", link.direction);

        final LinkSetter linkNew = new LinkSetter();
        addPattern("link(track(station(St1), station(St2)),attributes([duration(Dur), distance(Dist),push(Push), " +
                        "direction(Dir), lines(L)]))", linkNew,
                "St1", linkNew.trackFrom, "St2", linkNew.trackTo,
                "Dur", linkNew.duration, "Dist", linkNew.distance,
                "Push", linkNew.push, "Dir", linkNew.direction, "L", linkNew.lines);
    }

    private class LineSetter extends Setter<Line> {
        public void add(Line line) { DATA.addLine(line); }
        public void del(Line line) { DATA.delLine(line); }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setId(term.longValue());
                DATA.primaryMessageCount++;
                DATA.lineCount++;
                return true;
            }
        };

        Matcher<NumberTerm> station = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st = DATA.getStationById(term.longValue());
                object.setStation(st);
                add(object);
                st.addLine(object);
                return true;
            }
        };

        Matcher<NumberTerm> length = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setLength(term.longValue());
                return true;
            }
        };
    }

    {
        final LineSetter line1 = new LineSetter();
        addPattern("line(id(Id),station(St),length(L))",line1,"Id", line1.id, "St", line1.station,"L", line1.length);
        final LineSetter line2 = new LineSetter();
        addPattern("line(id(Id),station(St),out(_),in(_),type(_),length(L))",line2,"Id", line2.id, "St",
                line2.station,"L", line2.length);
    }

    ////////////// Задания на пересылку локомотивов ///////////////////////
    private class RelocationSetter extends TrivialAdder<LocoRelocation> {
        Matcher<NumberTerm> time = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object = new LocoRelocation();
                DATA.primaryMessageCount++;
                object.setTime(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> interval = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setInterval(term.longValue());
                return true;
            }
        };

        private long stFromId;
        Matcher<NumberTerm> stationFrom = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                stFromId = term.longValue();
                return true;
            }
        };

        private long stToId;
        Matcher<NumberTerm> stationTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                stToId = term.longValue();
                return true;
            }
        };

        private Link getLink() {
            StationPair stp = new StationPair(stFromId, stToId);
            return DATA.getLinkByStationPair(stp);
        }

        private LocoRelocation relocObject() {
            return object;
        }

        Wrapper trackFrom = new TrivialAdder<Link>() {
            public void add(Link noLink) {
                relocObject().setLinkFrom(getLink());
            }
        };

        Wrapper trackTo = new TrivialAdder<Link>() {
            public void add(Link noLink) {
                relocObject().setLinkTo(getLink());
            }
        };

        private Link getLink(Station stFrom, int dir) {
            for (Link link : stFrom.getLinks()) {
                if (link.getDirection() == dir) return link;
            }
            return null;
        }

        Matcher<NumberTerm> direction = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                int dir = term.intValue();
                Station stFrom = DATA.getStationById(stFromId);
                Link linkTo = getLink(stFrom, dir);
                Link linkToFrom = getLink(stFrom, (dir == 0) ? 1 : 0);
                Link linkFrom = null;
                if (linkToFrom != null) {
                    for (Link link : linkToFrom.getTo().getLinks()) {
                        if (link != null && link.getTo() == stFrom) {
                            linkFrom = link;
                        }
                    }
                }
                if (linkFrom == null || linkTo == null) {
                    return false;
                } else {
                    object.setLinkFrom(linkFrom);
                    object.setLinkTo(linkTo);
                    return true;
                }
            }
        };

        Matcher<NumberTerm> number = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setNumber(term.intValue());
                DATA.addRelocation(object);
                return true;
            }
        };
    }

    {
        final RelocationSetter relocationStation = new RelocationSetter();
        addPattern("loco_relocation(interval(Time,Interval),station(St),direction(Dir), number(Number))",
                   relocationStation,
                   "Time", relocationStation.time,
                   "Interval", relocationStation.interval,
                   "St", relocationStation.stationFrom,
                   "Dir", relocationStation.direction,
                   "Number", relocationStation.number);

        final RelocationSetter relocationTracks = new RelocationSetter();
        addPattern("loco_relocation(interval(Time,Interval), track_from(station(StFrom),station(StTo)), " +
                        "track_to(station(StFrom),station(StTo)), number(Number))",
                    relocationTracks,
                   "Time", relocationTracks.time,
                   "Interval", relocationTracks.interval,
                   "track_from", relocationTracks.trackFrom,
                   "track_to", relocationTracks.trackTo,
                   "StFrom", relocationTracks.stationFrom,
                   "StTo", relocationTracks.stationTo,
                   "Number", relocationTracks.number);
    }

    //////// Тяговые плечи /////////////////////////////////////////////////////
    private class RegionSetter extends TrivialAdder<LocoRegion> {
        Matcher<NumberTerm> regId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object = DATA.getLocoRegionById(term.longValue());
                DATA.primaryMessageCount++;
                DATA.locoRegionCount++;
                return true;
            }
        };

        private Integer curSeries;
        Matcher<NumberTerm> locoSeries = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                curSeries = term.intValue();
                return true;
            }
        };
        Matcher<NumberTerm> locoSections = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                SeriesPair serp =
                        new SeriesPair(curSeries, term.intValue());
                object.addSeriesPair(serp);
                return true;
            }
        };

        Matcher<NumberTerm> depot = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.addDepot(term.longValue());
                return true;
            }
        };
    }

    {
        final RegionSetter locoRegion = new RegionSetter();
        addPattern("loco_region(id(RegId),series([series(id(Series),sections(NSections))]),depot([station(Depot)]))",
                    locoRegion,"RegId", locoRegion.regId,"Series", locoRegion.locoSeries,"NSections",
                    locoRegion.locoSections,"Depot", locoRegion.depot);
    }

    //////////////// Участки оборота и обслуживания локомотивных бригад ///////////////////////
    private class TeamRegionSetter extends TrivialAdder<TeamRegion> {
        private boolean isService;

        TeamRegionSetter(boolean isService) {
            super();
            this.isService = isService;
        }

        Matcher<NumberTerm> Id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                DATA.primaryMessageCount++;
                if (isService){
                    object = DATA.getTeamServiceRegionById(term.longValue());
                } else {
                    object = DATA.getTeamWorkRegionById(term.longValue());
                }
                return true;
            }
        };

        Long from = 0L;
        Matcher<NumberTerm> From = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                from = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> To = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                StationPair sp = new StationPair(from, term.longValue());
                object.addStationPair(sp);
                return true;
            }
        };

        Matcher<NumberTerm> WithRest = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setWorkTimeWithRest(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> WithoutRest = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setWorkTimeWithoutRest(term.longValue());
                return true;
            }
        };
    }

    {
        final TeamRegionSetter teamServiceRegionNew = new TeamRegionSetter(true);
        final TeamRegionSetter teamWorkRegionNew = new TeamRegionSetter(false);
        addPattern("team_service_region(id(Id), tracks([track(station(From),station(To))]), " +
                        "work_time(with_rest(WithRest),without_rest(WithoutRest)))", teamServiceRegionNew,
                        "Id", teamServiceRegionNew.Id, "From", teamServiceRegionNew.From, "To", teamServiceRegionNew.To,
                        "WithRest", teamServiceRegionNew.WithRest, "WithoutRest", teamServiceRegionNew.WithoutRest);
        addPattern("team_work_region(id(Id), tracks([track(station(From),station(To))]), " +
                        "work_time(with_rest(WithRest),without_rest(WithoutRest)))", teamWorkRegionNew,
                        "Id", teamWorkRegionNew.Id, "From", teamWorkRegionNew.From, "To", teamWorkRegionNew.To,
                        "WithRest", teamWorkRegionNew.WithRest, "WithoutRest", teamWorkRegionNew.WithoutRest);
        //attributes([])
        final TeamRegionSetter teamServiceRegionOld = new TeamRegionSetter(true);
        final TeamRegionSetter teamWorkRegionOld = new TeamRegionSetter(false);
        addPattern("team_service_region(id(Id), tracks([track(station(From),station(To), attributes(_))]), " +
                        "work_time(with_rest(WithRest),without_rest(WithoutRest)))", teamServiceRegionOld,
                        "Id", teamServiceRegionOld.Id, "From", teamServiceRegionOld.From, "To", teamServiceRegionOld.To,
                        "WithRest", teamServiceRegionOld.WithRest, "WithoutRest", teamServiceRegionOld.WithoutRest);
        addPattern("team_work_region(id(Id), tracks([track(station(From),station(To), attributes(_))]), " +
                        "work_time(with_rest(WithRest),without_rest(WithoutRest)))", teamWorkRegionOld,
                        "Id", teamWorkRegionOld.Id, "From", teamWorkRegionOld.From, "To", teamWorkRegionOld.To,
                        "WithRest", teamWorkRegionOld.WithRest, "WithoutRest", teamWorkRegionOld.WithoutRest);
    }

    private class TeamPercentSetter extends TrivialAdder<TeamRegion> {
        Matcher<NumberTerm> Id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object = DATA.getTeamServiceRegionById(term.longValue());
                return true;
            }
        };

        Station s;
        Matcher<NumberTerm> depot = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                s = DATA.getStationById(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> percent = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.addDepotPercent(s, term.intValue());
                return true;
            }
        };
    }

    {
        final TeamPercentSetter teamPercentSetter = new TeamPercentSetter();
        addPattern("team_percent(service_region(Id), depot(St), percent(Percent))", teamPercentSetter,
                    "Id", teamPercentSetter.Id, "St", teamPercentSetter.depot, "Percent", teamPercentSetter.percent);
    }

    //////// Весовые типы //////////////////////////////////////////////////////
    private class WeightTypeSetter extends Setter<WeightType> {
        public void add(WeightType wt) { DATA.addWeightType(wt); }
        public void del(WeightType wt) { DATA.delWeightType(wt); }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.id = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> minWeight = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.minWeight = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> maxWeight = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.maxWeight = term.longValue();
                return true;
            }
        };
    }

    {
        final WeightTypeSetter weightType = new WeightTypeSetter();
        addPattern("weight_type(id(Id),min_weight(Min),max_weight(Max))",
                   weightType,
                   "Id", weightType.id,
                   "Min", weightType.minWeight,
                   "Max", weightType.maxWeight);
    }

    //////// Масса локомотивов /////////////////////////////////////////////////
    private class TonnageSetter extends Setter<LocoTonnage> {
        public void add(LocoTonnage tn) { DATA.addLocoTonnage(tn); }
        public void del(LocoTonnage tn) { DATA.delLocoTonnage(tn); }

        private Integer curSeries;
        Matcher<NumberTerm> locoSeries = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                DATA.primaryMessageCount++;
                curSeries = term.intValue();
                return true;
            }
        };
        Matcher<NumberTerm> locoSections = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                SeriesPair serp =
                        new SeriesPair(curSeries, term.intValue());
                object.setSeriesPair(serp);
                return true;
            }
        };
        Matcher<NumberTerm> trackFrom = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st1 = DATA.getStationById(term.longValue());
                object.setFrom(st1);
                return true;
            }
        };
        Matcher<NumberTerm> trackTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st2 = DATA.getStationById(term.longValue());
                object.setTo(st2);
                return true;
            }
        };
        Matcher<NumberTerm> maxWeight = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setMaxWeight(term.longValue());
                return true;
            }
        };
    };

    {
        final TonnageSetter tonnage = new TonnageSetter();
        addPattern("loco_tonnage(series(Series), sections(NSections),track(station(St1), " +
                        "station(St2)),max_train_weight(Tonnage))",tonnage,
                        "Series", tonnage.locoSeries,"NSections", tonnage.locoSections,"St1", tonnage.trackFrom,
                        "St2", tonnage.trackTo,"Tonnage", tonnage.maxWeight);
    }

    //////// Задачи на планирование ////////////////////////////////////////////
    private class RouteWrapper extends Wrapper {
        Route route;
        Long from;

        public void open() { route = new Route(new ArrayList<Link>());
                             from = -1L; }
        public void close() { route = null; }

        Matcher<NumberTerm> station = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                if (from >= 0L) {
                    StationPair stp =
                        new StationPair(from, term.longValue());
                    Link link = DATA.getLinkByStationPair(stp);
                    route.getLinkList().add(link);
                }
                from = term.longValue();
                return true;
            }
        };
    }

    private class TaskSetter extends Setter<Task> {
        public void add(Task t) {
                DATA.addTask(t);
        }
        public void del(Task t) {
                DATA.delTask(t);
        }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                DATA.primaryMessageCount++;
                object.setId(term.longValue());
                return true;
            }
        };


        RouteWrapper route = new RouteWrapper() {
            public void close() {
                object.addRoute(this.route);
                super.close();
            }
        };

        Matcher<NumberTerm> matchWeightType = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setWeight(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> time = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setStartTime(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> duration = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setDuration(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> quantity = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTrainQuantity(term.intValue());
                return true;
            }
        };
    }

    {
        final TaskSetter task = new TaskSetter();
        addPattern("task(id(Id), interval(Time, Dur),routes([route([station(St)])]),weight_type(WT), Quantity)",
                    task,"Id", task.id, "Time", task.time, "Dur", task.duration,"route", task.route, "St",
                    task.route.station,"WT", task.matchWeightType, "Quantity", task.quantity);
    }

    //////// Нитки /////////////////////////////////////////////////////////////
    private class SlotTrackWrapper extends Wrapper {
        SlotTrack slotTrack;
        private Long stationFrom;

        public void open() { slotTrack = new SlotTrack(); }
        public void close() { slotTrack = null; }

        Matcher<NumberTerm> trackFrom = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                stationFrom = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> trackTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                StationPair stp =
                        new StationPair(stationFrom, term.longValue());
                Link link = DATA.getLinkByStationPair(stp);
                slotTrack.setLink(link);
                return true;
            }
        };

        Matcher<NumberTerm> trackTimeStart = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                slotTrack.setTimeStart(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> trackTimeEnd = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                slotTrack.setTimeEnd(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> slotId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                slotTrack.setSlotId(term.longValue());
                return true;
            }
        };
    }

    private class SlotSetter extends Setter<Slot> {
        private boolean isPass;

        SlotSetter(boolean isPass) {
            super();
            this.isPass = isPass;
        }
        public void add(Slot sl) {
            if (!isPass){
                DATA.addSlot(sl);
            } else {
                DATA.addPassSlot(sl);
            }
        }
        public void del(Slot sl) {
            if (!isPass) {
                DATA.delSlot(sl);
            } else {
                DATA.delPassSlot(sl);
            }
        }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                DATA.primaryMessageCount++;
                object.setSlotId(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> trainCat = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTrainCategory(term.longValue());
                return true;
            }
        };

        SlotTrackWrapper track = new SlotTrackWrapper() {
            public void close() {
                object.addRouteTrack(this.slotTrack);
                super.close();
            }
        };
    }

    {
        final SlotSetter slot = new SlotSetter(false);
        addPattern("slot(id(SlotId),category(TrainCat),route([track(station(StFrom), station(StTo)," +
                        "time_start(TimeStart),time_end(TimeEnd))]))",
                        slot,"SlotId", slot.id, "TrainCat", slot.trainCat,"track", slot.track,"StFrom",
                        slot.track.trackFrom, "StTo", slot.track.trackTo,"TimeStart", slot.track.trackTimeStart,
                        "TimeEnd", slot.track.trackTimeEnd);

        final SlotSetter slotPass = new SlotSetter(true);
        addPattern("slot_pass(id(SlotId),category(TrainCat),route([track(station(StFrom), station(StTo)," +
                        "time_start(TimeStart),time_end(TimeEnd))]))",
                        slotPass,"SlotId", slotPass.id, "TrainCat", slotPass.trainCat,"track", slotPass.track,"StFrom",
                        slotPass.track.trackFrom, "StTo", slotPass.track.trackTo,"TimeStart",
                        slotPass.track.trackTimeStart,"TimeEnd", slotPass.track.trackTimeEnd);
    }

    //////// Фактические поезда //////////////////
    private class TrainStateSetter extends Setter<TrainState> {
        public void open() { /* TrainState is abstract, don't instantiate. */ }
        public void add(TrainState state) {
            DATA.addTrainState(state);
            DATA.getFactTrainById(state.getId()).setTrainState(state);
        }
        public void del(TrainState state) {
            DATA.delTrainState(state);
            DATA.getFactTrainById(state.getId()).setTrainState(null);
        }

        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                DATA.primaryMessageCount++;
                ((TrainState) object).setId(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> time = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                ((TrainState) object).setTime(term.longValue());
                return true;
            }
        };

        Station stationLocation;

        Matcher<NumberTerm> station = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                stationLocation = DATA.getStationById(term.longValue());
                return true;
            }
        };

        Link linkLocation;
        private Long stationFrom;

        Matcher<NumberTerm> trackFrom = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                stationFrom = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> trackTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                StationPair stp =
                        new StationPair(stationFrom, term.longValue());
                linkLocation = DATA.getLinkByStationPair(stp);
                return true;
            }
        };
    }

    private class TrainReadySetter extends TrainStateSetter {
        public void open() { object = new TrainReady(); }

        Wrapper location = new Wrapper() {
            public void open() {};
            public void close() {
                ((TrainReady) object).setStation(stationLocation);
                DATA.trainReadyCount++;
            }
        };
    }

    private class TrainDepartSetter extends TrainStateSetter {
        public void open() { object = new TrainDepart(); }

        Wrapper location = new Wrapper() {
            public void open() {};
            public void close() {
                ((TrainDepart) object).setLink(linkLocation);
                DATA.trainDepartCount++;
            }
        };
    }

    private class TrainArriveSetter extends TrainStateSetter {
        public void open() { object = new TrainArrive(); }

        Wrapper location = new Wrapper() {
            public void open() {};
            public void close() {
                ((TrainArrive) object).setLink(linkLocation);
                DATA.trainArriveCount++;
            }
        };
    }


    public class TrainInfoSetter extends TrivialAdder<FactTrain> {
        Matcher<NumberTerm> id = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object = DATA.getFactTrainById(term.longValue());
                DATA.primaryMessageCount++;
                DATA.trainInfoCount++;
                object.markTrainInfoSet();
                return true;
            }
        };

        Matcher<NumberTerm> trainNum = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTrainNum(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> category = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setCategory(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> weigth = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setWeight(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> length = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setLength(term.longValue());
                return true;
            }
        };
        
        Matcher<NumberTerm> priority = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setPriority(term.intValue());
                return true;
            }
        };

        RouteWrapper route = new RouteWrapper() {
            public void close() {
                object.addRoute(this.route);
                super.close();
            }
        };

    }

    {
        final TrainInfoSetter trainFactSetterOld = new TrainInfoSetter(){
            public void add(FactTrain tf) { DATA.addTrainFact(tf); }
            public void del(FactTrain tf) { DATA.delTrainFact(tf); }
        };
        addPattern("train_info(id(Id), category(Cat),weight(Weight), length(Length),routes([route([station(St1)])]))",
                    trainFactSetterOld,"Id", trainFactSetterOld.id,"Cat", trainFactSetterOld.category,"Weight",
                    trainFactSetterOld.weigth, "Length", trainFactSetterOld.length,"route",
                    trainFactSetterOld.route,"St1", trainFactSetterOld.route.station);

        final TrainInfoSetter trainFactSetterNew = new TrainInfoSetter(){
            public void add(FactTrain tf) { DATA.addTrainFact(tf); }
            public void del(FactTrain tf) { DATA.delTrainFact(tf); }
        };
        addPattern("train_info(id(Id), number(TrainNum), category(Cat),weight(Weight), length(Length)," +
                        "routes([route([station(St1)])]))",
                trainFactSetterNew,"Id", trainFactSetterNew.id, "TrainNum", trainFactSetterNew.trainNum,"Cat",
                trainFactSetterNew.category,"Weight", trainFactSetterNew.weigth,
                "Length", trainFactSetterNew.length,"route", trainFactSetterNew.route,"St1",
                trainFactSetterNew.route.station);
        
        final TrainInfoSetter trainFactSetterWithPriority = new TrainInfoSetter(){
            public void add(FactTrain tf) { DATA.addTrainFact(tf); }
            public void del(FactTrain tf) { DATA.delTrainFact(tf); }
        };
        addPattern("train_info(id(Id), number(TrainNum), category(Cat),priority(PrId),weight(Weight), " +
                        "length(Length),routes([route([station(St1)])]))",
        		trainFactSetterWithPriority,"Id", trainFactSetterWithPriority.id, 
        		"TrainNum", trainFactSetterWithPriority.trainNum,
        		"Cat", trainFactSetterWithPriority.category,
        		"PrId", trainFactSetterWithPriority.priority,
        		"Weight", trainFactSetterWithPriority.weigth,
                "Length", trainFactSetterWithPriority.length,
                "route", trainFactSetterWithPriority.route,
                "St1", trainFactSetterWithPriority.route.station);

        final TrainReadySetter trainReady = new TrainReadySetter();
        addPattern("train_ready(id(Id), station(St), time(Time))",trainReady,
                   "Id", trainReady.id, "Time", trainReady.time,"St", trainReady.station,"station",
                    trainReady.location);

        final TrainDepartSetter trainDepart = new TrainDepartSetter();
        addPattern("train_depart(id(Id), track(station(St1), station(St2)),time(Time))",trainDepart,
                   "Id", trainDepart.id, "Time", trainDepart.time,"St1", trainDepart.trackFrom, "St2",
                    trainDepart.trackTo,"track", trainDepart.location);

        final TrainArriveSetter trainArrive = new TrainArriveSetter();
        addPattern("train_arrive(id(Id), track(station(St1), station(St2)),time(Time))",trainArrive,
                   "Id", trainArrive.id, "Time", trainArrive.time,"St1", trainArrive.trackFrom, "St2",
                    trainArrive.trackTo,"track", trainArrive.location);
    }

    //////// Фактические локомотивы ////////////////////////////////////////////
    private class FactLocoSetter extends TrivialAdder<FactLoco> {
        public void close() { super.close(); }

        Matcher<NumberTerm> locoId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object = DATA.getFactLocoById(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> factTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTimeOfLocoFact(term.longValue());
                return true;
            }
        };

        FactLocoTrack.State term2state(NumberTerm term) {
            FactLocoTrack.State[] states =
                    FactLocoTrack.State.class.getEnumConstants();
            int ord = term.intValue();
            for (int i = 0; i < states.length; ++i) {
                if (ord == states[i].ordinal()) return states[i];
            }
            return FactLocoTrack.State.NA;
        }
    }

    private class LocoAtStationSetter extends FactLocoSetter {
        private FactLocoArrive factLocoArrive = null;

        public void close() {
            object.setLocoArrive(factLocoArrive);
            super.close();
        }
        public void del(FactLoco loco) {
            DATA.delFactLoco(loco);
        }

        Matcher<NumberTerm> station = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st = DATA.getStationById(term.longValue());
                object.setStation(st);
                object.markFactLocoSet();
                DATA.primaryMessageCount++;
                DATA.fLocoCount++;
                return true;
            }
        };

        Matcher<NumberTerm> arriveTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factLocoArrive = new FactLocoArrive();
                factLocoArrive.setTime(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> locoState = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factLocoArrive.setLocoState(term2state(term));
                return true;
            }
        };

        Matcher<NumberTerm> trainId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factLocoArrive.setId(term.longValue());
                return true;
            }
        };
    }

    {
        final LocoAtStationSetter factLocoAtStation = new LocoAtStationSetter();
        addPattern("fact_loco(id(LocoId), fact_time(FactTime),location(station(St)))",factLocoAtStation,
                   "LocoId", factLocoAtStation.locoId,"FactTime", factLocoAtStation.factTime,"St",
                    factLocoAtStation.station);

        final LocoAtStationSetter factLocoAtStationWithTrain = new LocoAtStationSetter();
        addPattern("fact_loco(id(LocoId), fact_time(FactTime),location(station(St), arrive_time(ArrTime)," +
                        "state(State), train(TrainId)))",
                    factLocoAtStationWithTrain,"LocoId", factLocoAtStationWithTrain.locoId, "FactTime",
                    factLocoAtStationWithTrain.factTime,"St", factLocoAtStationWithTrain.station,
                    "ArrTime", factLocoAtStationWithTrain.arriveTime,"State", factLocoAtStationWithTrain.locoState,
                    "TrainId", factLocoAtStationWithTrain.trainId);
    }

    private class LocoAtTrackSetter extends FactLocoSetter {
        private FactLocoTrack factLocoTrack = null;
        private Long stationFrom;

        public void close() {
            object.setTrack(factLocoTrack);
            super.close();
        }
        public void del(FactLoco loco) {
            DATA.delFactLoco(loco);
        }

        Matcher<NumberTerm> trackFrom = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factLocoTrack = new FactLocoTrack();
                stationFrom = term.longValue();
                object.markFactLocoSet();
                DATA.primaryMessageCount++;
                DATA.fLocoCount++;
                return true;
            }
        };

        Matcher<NumberTerm> trackTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                StationPair stp =
                        new StationPair(stationFrom, term.longValue());
                Link link = DATA.getLinkByStationPair(stp);
                factLocoTrack.setLink(link);
                return true;
            }
        };

        Matcher<NumberTerm> trackTimeDepart = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factLocoTrack.setTimeDepart(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> locoState = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factLocoTrack.setState(term2state(term));
                return true;
            }
        };

        Matcher<NumberTerm> trainId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factLocoTrack.setTrainId(term.longValue());
                return true;
            }
        };
    }

    {
        final LocoAtTrackSetter factLocoAtTrack = new LocoAtTrackSetter();
        addPattern("fact_loco(id(LocoId), fact_time(FactTime),location(track(station(St1), station(St2)," +
                        "depart_time(DepTime),state(State),train(TrainId))))",
                    factLocoAtTrack,"LocoId", factLocoAtTrack.locoId,"FactTime", factLocoAtTrack.factTime,"St1",
                    factLocoAtTrack.trackFrom, "St2", factLocoAtTrack.trackTo,"DepTime",
                    factLocoAtTrack.trackTimeDepart,"State", factLocoAtTrack.locoState,
                    "TrainId", factLocoAtTrack.trainId);
    }

    private class LocoNextServiceSetter extends FactLocoSetter {
        Matcher<NumberTerm> factTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTimeOfServiceFact(term.longValue());
                DATA.fLocoNextServiceCount++;
                return true;
            }
        };

        Matcher<NumberTerm> distTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setDistanceToService(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> timeTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTimeToService(term.longValue());
                object.markTimeUntilServiceSet();
                return true;
            }
        };

        Matcher<NumberTerm> serviceType = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setServiceType(term.longValue());
                return true;
            }
        };
    }

    {
        final LocoNextServiceSetter locoNextService = new LocoNextServiceSetter();
        addPattern("fact_loco_next_service(id(LocoId), fact_time(FactTime),next_service(dist_to(Dist),time_to(Time)," +
                        "type(TypeId)))", locoNextService,"LocoId", locoNextService.locoId,"FactTime",
                        locoNextService.factTime, "Dist", locoNextService.distTo,"Time", locoNextService.timeTo,
                        "TypeId", locoNextService.serviceType);
    }

    private class LocoAttributesSetter extends FactLocoSetter {
        Matcher<NumberTerm> series = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setSeries(term.intValue());
                return true;
            }
        };

        Matcher<NumberTerm> sections = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setNSections(term.intValue());
                return true;
            }
        };

        Matcher<NumberTerm> region = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.addLocoRegion(DATA.getLocoRegionById(term.longValue()));

                return true;
            }
        };

        Matcher<NumberTerm> depot = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st = DATA.getStationById(term.longValue());
                object.setDepotStation(st);
                object.markLocoAttrSet();
                DATA.locoAttrCount++;
                return true;
            }
        };
    }

    {
        final LocoAttributesSetter locoAttrs = new LocoAttributesSetter();
        addPattern("loco_attributes(id(LocoId),attributes([series(S), sections(N),loco_regions([id(RegId1)])," +
                        "depot(station(St))]))", locoAttrs,"LocoId", locoAttrs.locoId,"S", locoAttrs.series, "N",
                        locoAttrs.sections,"RegId1", locoAttrs.region, "St", locoAttrs.depot);
    }

    ///////// Фактические бригады — по аналогии с локомотивами /////////////////
    private class FactTeamSetter extends TrivialAdder<FactTeam> {
        public void close() { super.close(); }

        Matcher<NumberTerm> teamId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object = DATA.getFactTeamById(term.longValue());
                DATA.primaryMessageCount++;
                return true;
            }
        };

        Matcher<NumberTerm> factTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTimeOfFact(term.longValue());
                return true;
            }
        };

        FactTeamTrack.State term2state(NumberTerm term) {
            FactTeamTrack.State[] states =
                    FactTeamTrack.State.class.getEnumConstants();
            int ord = term.intValue();
            for (int i = 0; i < states.length; ++i) {
                if (ord == states[i].ordinal()) return states[i];
            }
            return FactTeamTrack.State.NA;
        }
    }

    private class TeamAtStationSetter extends FactTeamSetter {
        private FactTeamArrive factTeamArrive = null;

        public void close() {
            if (factTeamArrive != null) object.setTeamArrive(factTeamArrive);
            super.close();
        }
        public void del(FactTeam team) {
            DATA.delFactTeam(team);
        }

        Matcher<NumberTerm> station = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st = DATA.getStationById(term.longValue());
                object.setStation(st);
                object.markFactTeamSet();
                return true;
            }
        };

        Matcher<NumberTerm> arriveTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factTeamArrive = new FactTeamArrive();
                factTeamArrive.setTime(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> teamState = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factTeamArrive.setTeamState(term2state(term));
                return true;
            }
        };

        Matcher<NumberTerm> locoId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factTeamArrive.setId(term.longValue());
                return true;
            }
        };
    }

    {
        final TeamAtStationSetter factTeamAtStation = new TeamAtStationSetter();
        addPattern("fact_team(id(TeamId), fact_time(FactTime),location(station(St)))",factTeamAtStation,
                   "TeamId", factTeamAtStation.teamId,"FactTime", factTeamAtStation.factTime,"St",
                factTeamAtStation.station);

        final TeamAtStationSetter factTeamAtStationWithLoco = new TeamAtStationSetter();
        addPattern("fact_team(id(TeamId), fact_time(FactTime),location(station(St), arrive_time(ArrTime),state(State),"
                        + " loco(LocoId)))",factTeamAtStationWithLoco,
                    "TeamId", factTeamAtStationWithLoco.teamId,"FactTime", factTeamAtStationWithLoco.factTime,"St",
                    factTeamAtStationWithLoco.station,
                    "ArrTime", factTeamAtStationWithLoco.arriveTime, "State", factTeamAtStationWithLoco.teamState,
                    "LocoId", factTeamAtStationWithLoco.locoId);
    }

    private class TeamAtTrackSetter extends FactTeamSetter {
        private FactTeamTrack factTrack = null;
        private Long stationFrom;

        public void close() {
            object.setTrack(factTrack);
            super.close();
        }
        public void del(FactTeam team) {
            DATA.delFactTeam(team);
        }

        Matcher<NumberTerm> trackFrom = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factTrack = new FactTeamTrack();
                stationFrom = term.longValue();
                object.markFactTeamSet();
                return true;
            }
        };

        Matcher<NumberTerm> trackTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                StationPair stp =
                        new StationPair(stationFrom, term.longValue());
                Link link = DATA.getLinkByStationPair(stp);
                factTrack.setLink(link);
                return true;
            }
        };

        Matcher<NumberTerm> trackTimeDepart = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factTrack.setDepartTime(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> teamState = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factTrack.setState(term2state(term));
                return true;
            }
        };

        Matcher<NumberTerm> locoId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                factTrack.setLocoId(term.longValue());
                return true;
            }
        };
    }

    {
        final TeamAtTrackSetter teamAtTrack = new TeamAtTrackSetter();
        addPattern("fact_team(id(TeamId), fact_time(FactTime),location(track(station(St1), station(St2)," +
                        "depart_time(DepTime),state(State), loco(LocoId))))",
                    teamAtTrack,"TeamId", teamAtTrack.teamId,"FactTime", teamAtTrack.factTime,"St1",
                    teamAtTrack.trackFrom,"St2", teamAtTrack.trackTo,
                    "DepTime", teamAtTrack.trackTimeDepart,"State", teamAtTrack.teamState,"LocoId", teamAtTrack.locoId);
    }

    private class TeamNextRestSetter extends FactTeamSetter {
        Matcher<NumberTerm> restFactTime = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setRestFactTime(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> timeTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                if (term.longValue() == 0L) {
                    object.setTimeUntilRest(10*3600L);
                } else {
                    object.setTimeUntilRest(term.longValue());
                }
                object.markTimeUntilRestSet();
                DATA.fTeamNextRestCount++;
                return true;
            }
        };
    }

    {
        final TeamNextRestSetter teamNextRest = new TeamNextRestSetter();
        addPattern("fact_team_next_rest(id(TeamId),fact_time(FactTimeOfRest),time_to_rest(TimeRest))",
                    teamNextRest, "TeamId", teamNextRest.teamId,"FactTimeOfRest", teamNextRest.restFactTime,
                    "TimeRest", teamNextRest.timeTo);
    }

    private class TeamAttributesSetter extends FactTeamSetter{
        Matcher<NumberTerm> locoSeries = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.addLocoSeries(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> weightType = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.addWeightType(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> teamWorkRegion = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.addTeamWorkRegion(DATA.getTeamWorkRegionById(term.longValue()));
                return true;
            }
        };

        Matcher<NumberTerm> depot = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st = DATA.getStationById(term.longValue());
                object.markTeamAttrSet();
                object.setDepot(st);
                DATA.teamAttrCount++;
                return true;
            }
        };

        Matcher<NumberTerm> longTrain = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setAllowedToWorkOnLongTrain(term.booleanValue());
                return true;
            }
        };

        Matcher<NumberTerm> heavyTrain = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setAllowedToWorkOnHeavyTrain(term.booleanValue());
                return true;
            }
        };
        
        Matcher<NumberTerm> fakeId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setFake(term.booleanValue());
                return true;
            }
        };
    }

    {
        final TeamAttributesSetter teamAttrs = new TeamAttributesSetter();
        addPattern("team_attributes(id(TeamId),attributes([team_work_regions([id(RegId)]), " +
                        "depot(station(StId)),loco_series([id(Series)])," +
                   "weight_types([id(WType)]),long_train(IsLong),heavy_train(IsHeavy),fake(FakeId)]))",teamAttrs,
                   "TeamId", teamAttrs.teamId,
                   "RegId", teamAttrs.teamWorkRegion,
                   "StId", teamAttrs.depot,
                   "Series", teamAttrs.locoSeries,
                   "WType", teamAttrs.weightType,
                   "IsLong", teamAttrs.longTrain,"IsHeavy", teamAttrs.heavyTrain, "FakeId", teamAttrs.fakeId);
    }

    private class TeamPresenceSetter extends FactTeamSetter{
        Matcher<NumberTerm> timeOfPresence = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTimeOfPresence(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> station = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station st = DATA.getStationById(term.longValue());
                object.setStation(st);
                return true;
            }
        };
    }

    {
        final TeamPresenceSetter teamPresenceSetter = new TeamPresenceSetter();
        addPattern("fact_team_presence(id(TeamId), fact_time(PresenceTime),station(StId))",teamPresenceSetter,
                   "TeamId", teamPresenceSetter.teamId ,"PresenceTime", teamPresenceSetter.timeOfPresence,"StId",
                teamPresenceSetter.station);
    }

    //////// Ограничения по длине поездов //////////////////////////////////////
    private class TrainLengthLimitSetter extends TrivialAdder<Link> {
        private Long stationFrom;

        Wrapper setDefault = new TrivialAdder<Boolean>() {
            public void add(Boolean noObject) {
                stationFrom = -0xDFL;
            }
        };

        Matcher<NumberTerm> trackFrom = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                stationFrom = term.longValue();
                return true;
            }
        };

        Matcher<NumberTerm> trackTo = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Long stationTo = term.longValue();
                StationPair stp = new StationPair(stationFrom, stationTo);
                object = DATA.getLinkByStationPair(stp);
                return true;
            }
        };

        Matcher<NumberTerm> shortMax = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Integer shortMax = term.intValue();
                DATA.primaryMessageCount++;
                if (stationFrom.equals(-0xDFL)) {
                    Link.setDefaultLengthLimit(shortMax);
                } else if (object != null) {
                    object.setLengthLimit(shortMax);
                }
                return true;
            }
        };
    }

    {
        final TrainLengthLimitSetter trainLengthLimit = new TrainLengthLimitSetter();
        addPattern("train_length_limit(track(station(From), station(To)),short_max(N))",trainLengthLimit,
                   "From", trainLengthLimit.trackFrom,"To", trainLengthLimit.trackTo,"N", trainLengthLimit.shortMax);

        final TrainLengthLimitSetter defaultTrainLengthLimit = new TrainLengthLimitSetter();
        addPattern("train_length_limit(default, short_max(N))",defaultTrainLengthLimit,"default",
                defaultTrainLengthLimit.setDefault,"N", defaultTrainLengthLimit.shortMax);
    }
    
    //////// Приоритеты планирования поездов по категориям //////////////////////////////////////
    private class TrainCategorySetter extends Setter<TrainCategory> {
    	public void open() { object = new TrainCategory(); }
        
    	public void add(TrainCategory category) {
            DATA.addTrainCategory(category);
        }
        public void del(TrainCategory category) {
        	DATA.delTrainCategory(category);
        }

        Matcher<NumberTerm> catId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                DATA.primaryMessageCount++;
                DATA.trainCategoryCount++;
                object.setCatId(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> priority = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setPriority(term.intValue());
                return true;
            }
        };
       
    }

    {
        final TrainCategorySetter trainCategory = new TrainCategorySetter();
        addPattern("train_category(id(CatId), attributes([priority(PrId)]))",trainCategory,
                   "CatId", trainCategory.catId,
                   "PrId", trainCategory.priority);

    }

    //////// Произвольная конфигурация /////////////////////////////////////////

    {
        final TrivialAdder<String> config = new TrivialAdder<String>() {};
        addPattern("config(Prefix, Params)", config,
                   "Prefix", new Matcher<Term>() {
                       public Boolean match(Term term) {
                           DATA.primaryMessageCount++;
                           if (term instanceof StringTerm) {
                               config.object = ((StringTerm) term).value;
                               return true;
                           } else if (term instanceof CompoundTerm) {
                               CompoundTerm compound = (CompoundTerm) term;
                               if (compound.arity() == 0) {
                                   config.object = compound.functor;
                                   return true;
                               }
                           }
                           return false;
                       }
                   },
                   "Params", new Matcher<Term>() {
                       public Boolean match(Term term) {
                           Boolean ret = match(term, config.object);
                           return ret;
                       }
                       private Boolean match(Term term, String prefix) {
                           if (term instanceof CompoundTerm) {
                               CompoundTerm compound = (CompoundTerm) term;
                               prefix += "/" + compound.functor;
                               for (int i = 0; i < compound.arity(); ++i) {
                                   if (! match(compound.argument(i), prefix))
                                       return false;
                               }
                               return true;
                           } else if (term instanceof ListTerm) {
                               ListTerm list = (ListTerm) term;
                               for (int i = 0; i < list.length(); ++i) {
                                   if (! match(list.nth(i), prefix))
                                       return false;
                               }
                               return true;
                           } else if (term instanceof NumberTerm) {
                               Number v = ((NumberTerm) term).value;
                               DATA.setConfigParam(prefix, v);
                               return true;
                           } else {
                               return false;
                           }
                       }
                   });
    }

    ////////////////////// Корректировки ////////////////////////////////
    private class TeamPassSetter extends Setter<TeamPass> {
        public void add(TeamPass pass) { DATA.addTeamPass(pass); }
        public void del(TeamPass pass) { DATA.delTeamPass(pass); }

        Matcher<NumberTerm> time = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTime(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> interval = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setInterval(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> station = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station station = DATA.getStationById(term.longValue());
                object.setStation(station);
                return true;
            }
        };

        Matcher<NumberTerm> direction = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setDirection(term.intValue());
                return true;
            }
        };

        Matcher<NumberTerm> depot = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                Station depot = DATA.getStationById(term.longValue());
                object.setDepot(depot);
                return true;
            }
        };

        Matcher<NumberTerm> quantity = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setQuantityToSend(term.intValue());
                return true;
            }
        };

        Matcher<NumberTerm> region = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTeamServiceRegionId(term.longValue());
                return true;
            }
        };
    }

    {
        final TeamPassSetter teamPass = new TeamPassSetter();
        addPattern("+team_pass(interval(Time, Interval), station(StId), team_service_region(RegId), direction(Dir), " +
                        "depot(DepotId), number(N))", teamPass,
                        "Time", teamPass.time, "Interval", teamPass.interval, "StId", teamPass.station, "Dir",
                        teamPass.direction, "RegId", teamPass.region,
                        "DepotId", teamPass.depot, "N", teamPass.quantity);

        final TeamPassSetter anotherTeamPass = new TeamPassSetter();
        //+team_pass(interval(1441196659,3600),station(2000035112),direction(0),depot(2001890435),number(1))
        addPattern("+team_pass(interval(Time,Interval),station(StId),direction(Dir),depot(DepotId),number(N))",
                anotherTeamPass, "Time", anotherTeamPass.time, "Interval", anotherTeamPass.interval,
                "StId", anotherTeamPass.station, "Dir", anotherTeamPass.direction, "DepotId",
                anotherTeamPass.depot, "N", anotherTeamPass.quantity);
    }

    private class PinnedTeamSetter extends Setter<PinnedTeam> {
        public void add(PinnedTeam pinnedTeam) { DATA.addPinnedTeam(pinnedTeam); }
        public void del(PinnedTeam pinnedTeam) { DATA.delPinnedTeam(pinnedTeam); }

        Matcher<NumberTerm> teamId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTeamId(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> stationId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setStationId(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> trainId  = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setTrainId(term.longValue());
                return true;
            }
        };

        Matcher<NumberTerm> locoId = new Matcher<NumberTerm>() {
            public Boolean match(NumberTerm term) {
                object.setLocoId(term.longValue());
                return true;
            }
        };
    }

    {
        final PinnedTeamSetter pinnedTeam = new PinnedTeamSetter();
        addPattern("+pinned_team(id(TeamId), station(StId), train(TrainId), loco(LocoId))", pinnedTeam,
                   "TeamId", pinnedTeam.teamId, "StId", pinnedTeam.stationId, "TrainId", pinnedTeam.trainId, "LocoId",
                    pinnedTeam.locoId);
    }
}
