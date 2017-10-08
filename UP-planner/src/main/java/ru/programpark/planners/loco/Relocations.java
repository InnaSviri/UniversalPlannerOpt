package ru.programpark.planners.loco;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.*;
import ru.programpark.entity.loco.BaseLocoTrack.State;
import ru.programpark.planners.common.*;
import ru.programpark.entity.util.FilterIterator;
import ru.programpark.entity.util.LoggingAssistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

public class Relocations {

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(Relocations.class);
        return logger;
    }

    private void CHECK_INTERRUPTED() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            LOGGER().warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    private InputData input;
    private SchedulingData data;

    public Relocations() {
        this.input = SchedulingData.getInputData();
        this.data = SchedulingData.getFrameData();
    }

    private boolean relocationLike(Loco loco, LocoRelocation reloc) {
        Link linkFrom = reloc.getLinkFrom(),
             linkTo = reloc.getLinkTo();
        Long timeStart = reloc.getTime(),
             timeEnd = timeStart + reloc.getInterval();
        for (Loco.Event evt : loco.allEvents()) {
            if (evt instanceof LocoAssignEvent) {
                LocoAssignEvent lEvt = (LocoAssignEvent) evt;
                Train train = data.getTrain(lEvt.getTrainId());
                Integer startIndex = lEvt.getStartIndex(),
                        endIndex = lEvt.getEndIndex();
                AssignEvent aEvt;
                Train.EventContainer track1 = train.getRoute().get(startIndex),
                    track2 = train.getRoute().get(endIndex);
                if (! lEvt.isRelocation()) {
                    aEvt = (AssignEvent) (track1.lastEvent(AssignEvent.class));
                    if (aEvt.getTimeStart() < timeStart) continue;
                    aEvt = (AssignEvent) (track2.lastEvent(AssignEvent.class));
                    if (aEvt.getTimeStart() >= timeEnd) continue;
                }
                for (int i = startIndex + 1; i <= endIndex; ++i) {
                    track2 = train.getRoute().get(i);
                    aEvt = (AssignEvent) (track2.lastEvent(AssignEvent.class));
                    Long time = aEvt.getTimeStart();
                    if (track1.getLink().equals(linkFrom) &&
                            track2.getLink().equals(linkTo) &&
                            aEvt.getLocoState().equals(State.RESERVE) &&
                            (lEvt.isRelocation() ||
                                (time >= timeStart && time < timeEnd)))
                        return true;
                    track1 = track2;
                }
            }
        }
        return false;
    }

    private boolean even(LocoRelocation reloc) {
        return (reloc.getLinkTo().getDirection() % 2 == 0);
    }

    private class LocoFinder {
        LinkedList<Link> route;
        List<LocoRegion> regions;
        Long duration;

        // TBD: поиск по >1 веткам сети

        private Link link0;
        private Long timeFloor, timeCeil;
        private Iterator<Loco> locoIter;
        private Iterable<Loco> allLocos;
        private boolean haveExtension;

        LocoFinder(LocoRelocation reloc) {
            this.link0 = reloc.getLinkFrom();
            this.timeFloor = Math.max(input.getCurrentTime(), reloc.getTime());
            this.timeCeil = reloc.getTime() + reloc.getInterval();
            this.locoIter = null;
            this.allLocos = data.getLocos();
            this.haveExtension = true;
            this.regions = new ArrayList<>(link0.getTo().getRegions());
            this.route = new LinkedList<>();
            this.duration = 0L;
            extendRouteToTeamChange(reloc.getLinkTo());
        }

        private void extendRouteToTeamChange(Link link0) {
            extend: do {
                route.addLast(link0);
                duration += link0.getDefaultDuration().longValue();
                regions.retainAll(link0.getTo().getRegions());
                for (Link link : link0.getTo().getLinks()) {
                    if (link.getDirection() == link0.getDirection()) {
                        link0 = link;
                        continue extend;
                    }
                }
                break extend;   // Конец линии
            } while (link0.getFrom().getNormTime() <= 0 && ! regions.isEmpty());
        }

        private boolean maybeExtendRoute() {
            if (! haveExtension) return false;
            while (locoIter == null || ! locoIter.hasNext()) {
                if (link0 == null || regions.isEmpty())
                    return (haveExtension = false);
                final Station stTo = link0.getTo();
                final Station stFrom = link0.getFrom();
                final Long tCeil = timeCeil - duration;
                if (tCeil <= timeFloor)
                    return (haveExtension = false);
                // LOGGER().debug("Поиск свободных локомотивов на станции " +
                //                    stTo.getId());
                locoIter = new FilterIterator<Loco>(allLocos) {
                    @Override public boolean test(Loco loco) {
                        Station st = loco.getStation();
                        Long time = loco.lastEvent().getLocoReadyTime();
                        return stFrom.equals(st) &&
                            time >= timeFloor && time < tCeil &&
                            locoRouteRegion(loco) != null;
                    }
                };
                route.addFirst(link0);
                duration += link0.getDuration(tCeil);
                link0 = continuation(stFrom, link0.getDirection());
                if (link0 != null)
                    regions.retainAll(link0.getTo().getRegions());
            }
            return true;
        }

        private Link continuation(Station stFrom, Integer direction) {
            for (Link link : stFrom.getLinks()) {
                if (link.getDirection() != direction) {
                    for (Link rLink : link.getTo().getLinks()) {
                        if (rLink.getDirection() == direction &&
                                rLink.getTo().equals(stFrom)) {
                            return rLink;
                        }
                    }
                }
            }
            return null;
        }

        LocoRegion locoRouteRegion(Loco loco) {
            for (LocoRegion reg : loco.getLocoRegions()) {
                if (regions.contains(reg)) return reg;
            }
            return null;
        }

        Loco next() {
            return maybeExtendRoute() ? locoIter.next() : null;
        }
    }

    private Train planReserveTrain(Long locoId, List<Link> route, Long timeStart) {
        return LocoPlanner.regions().planReserveTrain(locoId, route, timeStart);
    }

    private boolean oneRelocation(LocoRelocation reloc, LocoFinder finder)
    throws InterruptedException {
        Loco loco;
        while ((loco = finder.next()) != null) {
            CHECK_INTERRUPTED();
            // LOGGER().debug("Найден свободный локомотив: " + loco);
            Long timeStart = loco.lastEvent().getLocoReadyTime();
            Train train =
                planReserveTrain(loco.getId(), finder.route, timeStart);
            if (train != null && inRelocInterval(train, reloc) &&
                    inTimeForService(train, loco)) {
                LocoRegion reg = finder.locoRouteRegion(loco);
                LocoRegionPriority.initPriorities(train, reg);
                SchedulingFrame frame = SchedulingData.getCurrentFrame();
                LocoAssignEvent evt =
                    new LocoAssignEvent(train.getTrainId(),
                                        0, finder.route.size() - 1, true);
                loco.addEvent(evt);
                data.putTrain(train);
                return true;
            }
        }
        return false;
    }

    private boolean inRelocInterval(Train train, LocoRelocation reloc) {
        Long time = train.lastEvent(StationPair.specifier(reloc.getLinkFrom()))
                         .getEventTime();
        Long timeFloor = reloc.getTime();
        Long timeCeil = timeFloor + reloc.getInterval();
        return (time >= timeFloor) && (time < timeCeil);
    }

    private boolean inTimeForService(Train train, Loco loco) {
        List<Train.EventContainer> route = train.getRoute();
        Train.EventContainer lastTrack = route.get(route.size() - 1);
        Station stEnd = lastTrack.getLink().getTo();
        Long servType = loco.lastServiceEvent().getServiceType();
        Station stServ = new StationWithService(servType);
        Long tts = loco.lastServiceEvent().getTimeToService(),
             rtts = input.getShortestPath().findDuration(stEnd, stServ);
        return tts >= rtts;
    }

    void process() throws InterruptedException {
        relocations: for (LocoRelocation reloc : input.getRelocations()) {
            CHECK_INTERRUPTED();
            int nToRelocate = reloc.getNumber();
            for (Loco loco : data.getLocos()) {
                if (relocationLike(loco, reloc)) {
                    if (--nToRelocate == 0) break;
                }
            }
            String relocDesc = reloc.getTime().toString() +
                " в " + (even(reloc) ? "чётном" : "нечётном") +
                " направлении по станции " + reloc.getLinkTo().getFrom().getId();
            if (nToRelocate > 0) {
                LOGGER().info("По регулировочному заданию " + relocDesc +
                                  " необходима доп. передача резервом " +
                                  LoggingAssistant.countingForm(nToRelocate,
                                       "локомотива", "локомотивов"));
                while (nToRelocate > 0) {
                    CHECK_INTERRUPTED();
                    LocoFinder finder = new LocoFinder(reloc);
                    if (oneRelocation(reloc, finder)) {
                        --nToRelocate;
                    } else {
                        LOGGER().error("Недостаточно ресурсов для выполнения " +
                                           "регулировочного задания " + relocDesc);
                        continue relocations;
                    }
                }
            } else {
                LOGGER().info("Регулировочное задание " + relocDesc +
                                  " выполнено по фактическим назначениям");
            }
        }
    }

}
