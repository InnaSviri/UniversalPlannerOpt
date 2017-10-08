package ru.programpark.planners.team;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.planners.common.PassEvent;
import ru.programpark.planners.common.SchedulingData;
import ru.programpark.planners.common.Team;

import java.util.HashSet;
import java.util.Set;

public class TeamChange {
    public static Set<Station> findAllNearestTeamChangeStations(final Station station) {
        return findAllNearestTeamChangeStations(station,
                new HashSet<Station>() {{
                    add(station);
                }});
    }

    private static Set<Station> findAllNearestTeamChangeStations(Station station, Set<Station> checkedStations) {
        Set<Station> nearestTeamChangeStations = new HashSet<>();
        Set<Station> nearestStations = new HashSet<>();
        InputData iData = SchedulingData.getInputData();

        for (Link link : iData.getLinks().values())
            if (link.getFrom().equals(station) && !checkedStations.contains(link.getTo()))
                nearestStations.add(link.getTo());

        checkedStations.addAll(nearestStations);
        for (Station nearestStation : nearestStations)
            if (nearestStation.getNormTime() != 0)
                nearestTeamChangeStations.add(nearestStation);
            else
                nearestTeamChangeStations.addAll(findAllNearestTeamChangeStations(nearestStation, checkedStations));

        return nearestTeamChangeStations;
    }

    /**
     * Определяем станцию смены бригады
     */
    public static Station getTeamChangeStation(Team team, TeamSlot teamSlot, long whenWillBeRelocated, long whenDepartsAsPass, long shiftTime) {
        Long duration = whenWillBeRelocated - team.timeOfPresence();
        if ((duration < 0L) || (team.lastEvent() instanceof PassEvent) || (team.lastEvent().getStation() != null && team.lastEvent().getStation().equals(team.getDepot())
                && whenDepartsAsPass != -1L))//учет рабочего времени, когда бригада пересылается под локо.
            duration = 0L;//whenWillBeRelocated - whenDepartsAsPass;
        Station stationForChangeTeam = null;
        SlotTrack firstTrack = teamSlot.route.get(0);
        Long allTime = duration;
        Long timeStart = firstTrack.getTimeStart() + duration + shiftTime;
        Long trackTimeStart = timeStart;

        //расчитываем время, которое необходимо вычесть из оставшегося рабочего времени бригады,
        // если бригада готова раньше и ждет отправления
        long teamShift = 0L;
        if (!team.getDepot().equals(team.lastEvent().getStation()) || team.lastWorkTime() > 0L)
            if (team.lastEvent().getEventTime() < timeStart)
                teamShift = timeStart - team.lastEvent().getEventTime();

        for (SlotTrack track : teamSlot.route) {
            Long linkDuration = track.getLink().getDuration(trackTimeStart);
            allTime += linkDuration;
            trackTimeStart += linkDuration;
            if (!checkTrackForTeam(track, team))
                break;
            //тут основная проверка
            if (allTime > team.timeUntilRest() - teamShift)
                break;
            if (track.getLink().getTo().getNormTime() > 0)
                stationForChangeTeam = track.getLink().getTo();
            //Если на маршруте попадается станция приписки бригады, то ставим ее как станцию смены бригады
            if (!track.getLink().equals(firstTrack.getLink())
                    && track.getLink().getTo().equals(team.getDepot())) {
                stationForChangeTeam = track.getLink().getTo();
                break;
            }
        }

        return stationForChangeTeam;
    }

    /**
     * Проверяем, может ли бригада вести локо по данному треку (участки обкатки)
     */
    private static boolean checkTrackForTeam(SlotTrack track, Team team) {
        boolean canUseThisTrack = false;
        canUseThisTeam: for (TeamRegion teamRegion : team.getFactTeam().getTeamWorkRegions()) {
            for (StationPair pair : teamRegion.getStationPairs()) {
                if (track.getLink().getFrom().getId().equals(pair.stationFromId) &&
                        track.getLink().getTo().getId().equals(pair.stationToId)) {
                    canUseThisTrack = true;
                    continue canUseThisTeam;
                }
            }
        }

        return canUseThisTrack;
    }

    public static long getTimeBeforeChange(SlotTrack firstSlot, TeamSlot loco, Long shiftTime, Station changeSt){
        long timeBeforeChange = 0L;
        long timeStart = firstSlot.getTimeStart() + timeBeforeChange + shiftTime;

        for (SlotTrack slotTrack : loco.route) {
            timeBeforeChange += slotTrack.getLink().getDuration(timeStart);
            timeStart += timeBeforeChange;
            if (slotTrack.getLink().getTo().equals(changeSt))
                break;
        }

        return timeBeforeChange;
    }

    /**
     * определить, нужно ли сдвигать бригаду. Сдвиг не делается, если 2-х и более путная дорога на 90% пути
     */
    public static boolean needShiftForReserveLoco(TeamSlot reserve, Station changeTeamStation) {
        if (reserve.route.size() == 0) return false;
        //считаем, сколько путей на маршруте
        double oneLineDuration = 0.0;
        double someLineDuration = 0;
        for (SlotTrack track : reserve.route) {
            if (track.getLink().getLines() == 1) {
                oneLineDuration += track.getLink().getDistance().doubleValue();
            } else {
                someLineDuration += track.getLink().getDistance().doubleValue();
            }
            if (track.getLink().getTo().equals(changeTeamStation)) {
                break;
            }
        }

        return (oneLineDuration / someLineDuration) > 0.1;
    }
}