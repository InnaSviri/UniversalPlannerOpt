import ru.programpark.entity.data.*;
import ru.programpark.entity.fixed.*;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.common.*;

import org.json.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Draw extends Main {

    private static class Stretch {
        SchedulingData data = SchedulingData.getFrameData();
        Set<Train> trains;
        Set<Loco> locos;
        Set<Team> teams;
        List<Link> route;

        private Stretch(Set<Train> trains, List<Link> route) {
            this.trains = trains;
            this.route = route;
            this.locos = new HashSet<>();
            this.teams = new HashSet<>();
            for (Train train : trains) {
                for (Train.EventContainer events : train.getRoute()) {
                    AssignEvent aEvt = events.lastEvent(AssignEvent.class);
                    if (aEvt != null) {
                        if(aEvt.getLocoId() != null)
                            locos.add(data.getLoco(aEvt.getLocoId()));
                        if(aEvt.getTeamId() != null)
                            teams.add(data.getTeam(aEvt.getTeamId()));
                    }
                }
            }
        }

        private static Link revLink(Link link) {
            StationPair rstp = new StationPair(link.getTo().getId(),
                    link.getFrom().getId());
            return SchedulingData.getLink(rstp);
        }

        private static List<Link> evenRoute(Train train) {
            List<Link> route = new ArrayList<>();
            for (Train.EventContainer track : train.getRoute())
                route.add(track.getLink());
            if (! route.isEmpty() && (route.get(0).getDirection() % 2) != 0) {
                for (int i = 0; i < route.size(); ++i) {
                    route.set(i, revLink(route.get(i)));
                }
                Collections.reverse(route);
            }
            return route;
        }

        private static boolean
        isRoutePrefix(List<Link> route1, int start1,
                      List<Link> route2, int start2) {
            for (int i1 = start1, i2 = start2; ; ++i1, ++i2) {
                if (i1 >= route1.size()) {
                    return true;
                } else if (i2 >= route2.size() ||
                               ! route1.get(i1).equals(route2.get(i2))) {
                    return false;
                } else {
                    continue;
                }
            }
        }

        private static List<Link>
        extendedRoute(List<Link> route1, List<Link> route2) {
            if (route2.isEmpty()) return route1;
            else if (route1.isEmpty()) return route2;
            Link l1 = route1.get(route1.size() - 1),
                 l2 = route2.get(0);
            int i = route1.indexOf(l2);
            if (i < 0) {
                if (l1.getTo().equals(l2.getFrom()) &&
                        l1.getDirection().equals(l2.getDirection())) {
                    List<Link> extRoute = new ArrayList<>(route1);
                    extRoute.addAll(route2);
                    return extRoute;
                } else {
                    return null;
                }
            } else if (isRoutePrefix(route2, 0, route1, i)) {
                return route1;
            } else if (isRoutePrefix(route1, i, route2, 0)) {
                List<Link> extRoute = new ArrayList<>(route1.subList(0, i));
                extRoute.addAll(route2);
                return extRoute;
            } else {
                return null;
            }
        }

        private static Stretch getNext(SortedSet<Train> trains) {
            if (trains.isEmpty()) return null;
            Train train0 = trains.first();
            SortedSet<Train> sTrains = new TreeSet<>(trains.comparator());
            sTrains.add(train0);
            List<Link> sRoute = evenRoute(train0);
            for (Train train : trains) {
                if (train == train0) continue;
                List<Link> route = evenRoute(train);
                List<Link> extRoute = extendedRoute(sRoute, route);
                if (extRoute == null) extRoute = extendedRoute(route, sRoute);
                if (extRoute != null) {
                    sRoute = extRoute;
                    sTrains.add(train);
                }
            }
            return new Stretch(sTrains, sRoute);
        }

        static List<Stretch> getAll() {
            SortedSet<Train> trains =
                new TreeSet<>(new Comparator<Train>() {
                    public int compare(Train trn1, Train trn2) {
                        int c = - Integer.compare(trn1.getRoute().size(),
                                                  trn2.getRoute().size());
                        return (c != 0) ? c :
                            Long.compare(trn1.getTrainId(), trn2.getTrainId());
                    }
                    public boolean equals(Object other) {
                        return false;
                    }
                });
            trains.addAll(SchedulingData.getFrameData().getTrains());
            List<Stretch> stretches = new ArrayList<>();
            Stretch stretch;
            while ((stretch = getNext(trains)) != null) {
                stretches.add(stretch);
                trains.removeAll(stretch.trains);
            }
            return stretches;
        }

        JSONObject stationJSON(Station station) {
            JSONObject joStation = new JSONObject();
            joStation.put("id", station.getId());
            joStation.put("name", station.hasName() ? station.getName() :
                                      station.getId().toString());
            if (station.getProcessTime() > 0) joStation.put("isLocoExch", true);
            if (station.getNormTime() > 0) joStation.put("isTeamExch", true);
            return joStation;
        }

        JSONArray routeJSON() {
            JSONArray jaRoute = new JSONArray();
            Station prev = null;
            for (Link link : route) {
                JSONObject joLink = new JSONObject();
                Station from = link.getFrom();
                if (! from.equals(prev))
                    joLink.put("from", stationJSON(from));
                joLink.put("to", stationJSON((prev = link.getTo())));
                joLink.put("dist", link.getDistance());
                jaRoute.put(joLink);
            }
            return jaRoute;
        }

        JSONArray trainsJSON() {
            JSONArray jaTrains = new JSONArray();
            for (Train train : trains) {
                JSONObject joTrain = new JSONObject();
                joTrain.put("id", train.getTrainId());
                if (train.getAttributes() != null) {
                    joTrain.put("num", train.getTrainNum());
                    joTrain.put("cat", train.getCategory());
                }
                joTrain.put("t0", train.getStartTime());
                Link link0 = train.getRoute().get(0).getLink();
                int i0 = route.indexOf(link0);
                if (i0 < 0) {
                    i0 = route.indexOf(revLink(link0));
                    joTrain.put("rev", true);
                }
                joTrain.put("i0", i0);
                JSONArray jaRoute = new JSONArray();
                for (int i = -1; i < train.getRoute().size(); ++i) {
                    Train.Event evt;
                    String fType = null;
                    if ((evt = train.lastEvent(i, TrainDepartEvent.class)) != null) {
                        fType = "depart";
                    } else if ((evt = train.lastEvent(i, TrainArriveEvent.class)) != null) {
                        fType = "arrive";
                    } else if ((evt = train.lastEvent(i, TrainReadyEvent.class)) != null) {
                        fType = "ready";
                    }
                    if (evt != null) {
                        JSONObject joFEvt = new JSONObject();
                        joFEvt.put("t", evt.getEventTime());
                        joFEvt.put("i", i);
                        joTrain.put(fType, joFEvt);
                    }
                    if (i >= 0) {
                        if ((evt = train.lastEvent(i, AssignEvent.class)) != null) {
                            JSONArray jaTrack = new JSONArray();
                            AssignEvent aEvt = (AssignEvent) evt;
                            jaTrack.put(aEvt.getLocoFrameIndex());
                            jaTrack.put(aEvt.getTimeStart());
                            jaTrack.put(aEvt.getTimeEnd());
                            jaTrack.put(aEvt.getLocoId());
                            jaTrack.put(aEvt.getLocoState());
                            jaTrack.put(aEvt.getTeamId());
                            jaTrack.put(aEvt.getTeamState());
                            jaRoute.put(jaTrack);
                        } else {
                            jaRoute.put((Object) null);
                        }
                    }
                }
                joTrain.put("route", jaRoute);
                jaTrains.put(joTrain);
            }
            return jaTrains;
        }

        JSONArray locosJSON() {
            JSONArray jaLocos = new JSONArray();

            return jaLocos;
        }

        JSONArray teamsJSON() {
            JSONArray jaTeams = new JSONArray();
            return jaTeams;
        }

        JSONObject rangeJSON() {
            JSONObject joRange = new JSONObject();
            SchedulingFrame frame = SchedulingData.getCurrentFrame();
            joRange.put("start", frame.rangeStart);
            joRange.put("end", frame.rangeEnd);
            joRange.put("locoFrame", frame.locoFrame);
            joRange.put("teamFrame", frame.teamFrame);
            return joRange;
        }

        JSONObject toJSON() {
            JSONObject joStretch = new JSONObject();
            joStretch.put("stretch", hashCode());
            joStretch.put("route", routeJSON());
            joStretch.put("trains", trainsJSON());
            joStretch.put("locos", locosJSON());
            joStretch.put("teams", teamsJSON());
            joStretch.put("range", rangeJSON());
            return joStretch;
        }

        String routeTitle() {
            if (route.isEmpty()) {
                return "?";
            } else {
                Station first = route.get(0).getFrom();
                Station last = route.get(route.size() - 1).getTo();
                return (first.hasName() && last.hasName())
                    ? (first.getName() + " — " + last.getName())
                    : ("" + first.getId() + " — " + last.getId());
            }
        }
    }

    /*
    private static String drawSchedule() {
        List<Stretch> stretches = Stretch.getAll();
        int width = MARGIN, height = MARGIN + PADDING;
        for (Stretch stretch : stretches) {
            width = Math.max(width, stretch.width + (MARGIN + PADDING) * 2);
            height += stretch.height + MARGIN + PADDING * 3;
        }
        SVGGraphics2D svg = new SVGGraphics2D(width, height);
        int hOffset = MARGIN, vOffset = MARGIN;
        for (Stretch stretch : stretches)
            vOffset = stretch.draw(svg, hOffset, vOffset);
        return svg.getSVGDocument();
    }
    */

    private static String getResourceAsString(String path) {
        try {
            String rsrc;
            InputStream in = null;
            try {
                in = Draw.class.getResourceAsStream(path);
                rsrc = new Scanner(in).useDelimiter("\\Z").next();
            } finally {
                in.close();
            }
            return rsrc;
        } catch (Exception e) {
            return "";
        }
    }

    private static void
    appendTemplateForeach(StringBuffer ret, String template,
                          List<Stretch> stretches) {
        Pattern fmt = Pattern.compile("~(.)");
        for (int i = 1; i <= stretches.size(); ++i) {
            Stretch stretch = stretches.get(i - 1);
            Matcher m = fmt.matcher(template);
            String repl = "";
            match_fmt: while (m.find()) {
                switch (m.group(1)) {
                    case "H":
                        repl = Integer.toString(stretch.hashCode()); break;
                    case "J":
                        repl = stretch.toJSON().toString(); break;
                    case "R":
                        repl = stretch.routeTitle(); break;
                    case "#":
                        repl = Integer.toString(i); break;
                    case "^":
                        if (i == stretches.size()) {
                            m.appendReplacement(ret, "");
                            repl = null; break match_fmt;
                        } else {
                            repl = ""; break;
                        }
                    default:
                        repl = m.group(1);
                }
                if (repl != null) m.appendReplacement(ret, repl);
            }
            if (repl != null) m.appendTail(ret);
        }
    }

    private static String
    transformTemplate(String template, Integer runIndex, List<Stretch> stretches) {
        StringBuffer ret = new StringBuffer();
        Pattern pi = Pattern.compile("<\\?(?:" +
                                       "(runIndex)|" +
                                       "(stretches(?:\\.(foreach) (.+?))?)|" +
                                       "(include (.+?))" +
                                     ")\\?>",
                                     Pattern.DOTALL);
        Matcher m = pi.matcher(template);
        while (m.find()) {
            if (m.group(1) != null) {
                m.appendReplacement(ret, runIndex.toString());
            } else if (m.group(2) != null) {
                String ins = m.group(3), sub = m.group(4);
                if (ins == null) {
                    JSONArray jaStretches = new JSONArray();
                    for (Stretch stretch : stretches) {
                        jaStretches.put(stretch.toJSON());
                    }
                    m.appendReplacement(ret, jaStretches.toString());
                } else if (ins.equals("foreach")) {
                    m.appendReplacement(ret, "");
                    appendTemplateForeach(ret, sub, stretches);
                } else {
                    m.appendReplacement(ret, m.group());
                }
            } else if (m.group(5) != null) {
                String incRsrc = m.group(6);
                String inc = getResourceAsString(incRsrc);
                if (inc.isEmpty()) {
                    m.appendReplacement(ret, m.group());
                } else {
                    m.appendReplacement(ret, "");
                    ret.append(inc);
                }
            } else {
                m.appendReplacement(ret, m.group());
            }
        }
        m.appendTail(ret);
        return ret.toString();
    }

    private static String drawSchedule(int runIndex) {
        List<Stretch> stretches = Stretch.getAll();
        String template = getResourceAsString("schedule.html");
        return transformTemplate(template, runIndex, stretches);
    }

    private int runIndex = 0;

    public static void main(String... args) {
        Main.main(new Draw(), args);
    }

    @Override public void oneRun(Percepts percepts, boolean summary) {
        super.oneRun(percepts, summary);
        PrintWriter svgWriter =
            LoggingAssistant.openWriter("schedule" + runIndex + ".html");
        svgWriter.write(drawSchedule(runIndex));
        svgWriter.close();
        ++runIndex;
    }

}
