package ru.programpark.entity.util;

import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;

import java.util.*;

public class ShortestPath {
    private Map<Station, Map<Station, Link>> links = new HashMap<>();

    public ShortestPath() {}

    public ShortestPath(Collection<Link> links) {
        addLinks(links);
    }

    public void addLink(Link link) {
        Map<Station, Link> siblings = this.links.get(link.getFrom());
        if (siblings == null) {
            siblings = new HashMap<>();
            this.links.put(link.getFrom(), siblings);
        }
        siblings.put(link.getTo(), link);
    }

    public void addLinks(Collection<Link> links) {
        for (Link link : links) addLink(link);
    }

    public void delLink(Link link) {
        Map<Station, Link> siblings = this.links.get(link.getFrom());
        if (siblings != null) {
            siblings.remove(link.getTo());
            if (siblings.isEmpty()) links.remove(link.getFrom());
        }
    }

    public void delLinks(Collection<Link> links) {
        for (Link link : links) delLink(link);
    }

    private class Metric implements Comparable<Metric> {
        Station station;
        Long value;

        public Metric(Station station, Long value) {
            this.station = station;
            this.value = value;
        }

        @Override public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Metric) {
                Metric that = (Metric) o;
                return this.station.equals(that.station);
            } else {
                return false;
            }
        }

        @Override public int compareTo(Metric that) {
            int vCmp = Long.compare(this.value, that.value);
            return (vCmp == 0)
                ? Long.compare(this.station.getId(), that.station.getId())
                : vCmp;
        }

        @Override public String toString() {
            return "Metric{" + station.getId() + ", " + value + "}";
        }
    }

    private Collection<Link> outgoingLinks(Station st) {
        Map<Station, Link> linkMap = links.get(st);
        return (linkMap == null)
            ? Collections.<Link>emptyList()
            : linkMap.values();
    }

    private void reifyDynamicStation(Station dyn, Station real) {
        dyn.setId(real.getId());
        dyn.setRegions(real.getRegions());
        dyn.setServiceAvailable(real.getServiceAvailable());
        dyn.setProcessTime(real.getProcessTime());
    }

    private void writeRoute(Deque<Link> route, Station end,
                            Map<Station, Station> comeFrom) {
        route.clear();
        Station st;
        while ((st = comeFrom.get(end)) != null) {
            route.addFirst(links.get(st).get(end));
            end = st;
        }
    }

    private Long dijkstra(Station from, Station to,
                          boolean byDuration, Deque<Link> route,
                          Class<? extends FilterIterator<Link>> filterClass) {
        Map<Station, Metric> metricsMap = new HashMap<>();
        Set<Metric> metricsVisited = new TreeSet<>();
        SortedSet<Metric> metricsUnvisited = new TreeSet<>();
        Map<Station, Station> comeFrom = (route == null) ? null :
            new HashMap<Station, Station>();
        Metric m = new Metric(from, 0L);
        metricsUnvisited.add(m);
        metricsMap.put(from, m);
        m = null;
        while (! metricsUnvisited.isEmpty()) {
            // System.err.println("@@@ dijkstra # while(! " +
            //                    metricsUnvisited + ".isEmpty()" + ")");
            Metric mMin = metricsUnvisited.first();
            metricsUnvisited.remove(mMin);
            metricsVisited.add(mMin);
            Station st = mMin.station;
            if (to.equals(st)) {
                m = mMin;
                break;
            }
            Iterator<Link> iter = outgoingLinks(st).iterator();
            if (filterClass != null) {
                try {
                    iter = filterClass.getConstructor(Iterator.class)
                                      .newInstance(iter);
                } catch (Exception e) {
                    return Long.MAX_VALUE;
                }
            }
            while (iter.hasNext()) {
                Link link = iter.next();
                Station neighb = link.getTo();
                long v = mMin.value +
                    (byDuration ? link.getDefaultDuration() : link.getDistance());
                Metric mOld = metricsMap.get(neighb);
                // System.err.println("@@@ dijkstra # while(...) # for(link " +
                //                    st.getId() + " -> " + neighb.getId() + ") : " +
                //                    "v = " + v + "; mOld = " + mOld);
                if (mOld == null || v < mOld.value) {
                    Metric mNew = new Metric(neighb, v);
                    if (mOld != null &&  metricsVisited.remove(mOld)) {
                        // System.err.println("@@@ dijkstra # while(...) # for(...) : " +
                        //                    "metricsVisited.add(" + mNew + ")");
                        metricsVisited.add(mNew);
                    } else {
                        if (mOld != null) metricsUnvisited.remove(mOld);
                        // System.err.println("@@@ dijkstra # while(...) # for(...) : " +
                        //                    "metricsUnvisited.add(" + mNew + ")");
                        metricsUnvisited.add(mNew);
                    }
                    // System.err.println("@@@ dijkstra # while(...) # for(...) : " +
                    //                    "metricsMap.put(" + neighb.getId() + ", " + mNew + ")");
                    metricsMap.put(neighb, mNew);
                    if (comeFrom != null) comeFrom.put(neighb, st);
                }
            }
        }
        if (m == null) {
            return Long.MAX_VALUE;
        } else {
            Station st = m.station;
            if (to.getClass() != st.getClass()) reifyDynamicStation(to, st);
            if (route != null) writeRoute(route, st, comeFrom);
            return m.value;
        }
    }

    /**
     * Return the distance along the shortest path between the given
     * stations.
     *
     * @param  from the origin station
     * @param  to   the destination station; may specify the destination
     *              indirectly, by extending Station with a custom equals()
     *              method; and if the actual station is of a different
     *              type than the argument, its data are copied over to
     *              the argument
     * @return      the distance from origin to destination
     */
    public Long findDistance(Station from, Station to) {
        return dijkstra(from, to, false, null, null);
    }

    public Long findDistance(Station from, Station to,
                             Class<? extends FilterIterator<Link>> filterClass) {
        return dijkstra(from, to, false, null, filterClass);
    }

    /**
     * Return the duration along the shortest path between the given
     * stations.
     *
     * @param  from the origin station
     * @param  to   the destination station; may specify the destination
     *              indirectly, by extending Station with a custom equals()
     *              method; and if the actual station is of a different
     *              type than the argument, its data are copied over to
     *              the argument
     * @return      the distance from origin to destination
     */
    public Long findDuration(Station from, Station to) {
        return dijkstra(from, to, true, null, null);
    }

    public Long findDuration(Station from, Station to,
                             Class<? extends FilterIterator<Link>> filterClass) {
        return dijkstra(from, to, true, null, filterClass);
    }

    /**
     * Return the route along the shortest-distance path between the given
     * stations.
     *
     * @param  from the origin station
     * @param  to   the destination station; may specify the destination
     *              indirectly, by extending Station with a custom equals()
     *              method; and if the actual station is of a different
     *              type than the argument, its data are copied over to
     *              the argument
     * @return      the distance from origin to destination
     */
    public List<Link> findRouteByDistance(Station from, Station to) {
        return findRouteByDistance(from, to, null);
    }

    public List<Link>
    findRouteByDistance(Station from, Station to,
                        Class<? extends FilterIterator<Link>> filterClass) {
        LinkedList<Link> route = new LinkedList<>();
        dijkstra(from, to, false, (Deque<Link>) route, filterClass);
        return (List<Link>) route;
    }

    /**
     * Return the route along the shortest-duration path between the given
     * stations.
     *
     * @param  from the origin station
     * @param  to   the destination station; may specify the destination
     *              indirectly, by extending Station with a custom equals()
     *              method; and if the actual station is of a different
     *              type than the argument, its data are copied over to
     *              the argument
     * @return      the distance from origin to destination
     */
    public List<Link> findRouteByDuration(Station from, Station to) {
        return findRouteByDuration(from, to, null);
    }

    public List<Link>
    findRouteByDuration(Station from, Station to,
                        Class<? extends FilterIterator<Link>> filterClass) {
        LinkedList<Link> route = new LinkedList<>();
        dijkstra(from, to, true, (Deque<Link>) route, filterClass);
        return (List<Link>) route;
    }
}
