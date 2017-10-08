package ru.programpark.planners.team;

import ru.programpark.entity.fixed.Capacity;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.planners.common.Team;

import java.util.*;

public class BulkTeamAssign {

    CapacityManager capacityManager = new CapacityManager();

    public List<Decision<TeamSlot, Team>> getDecisions(Station station,
            final List<TeamSlot> teamSlots, final List<Team> teams, long timeEndPlanningIteration) {

        List<Team> bufferTeams = new ArrayList<>(teams);

        List<TeamSlot> bufferSlots = new ArrayList<>(teamSlots);
        Collections.sort(bufferSlots, ComparatorBuilder.teamSlotTimeStartComparator());

        List<Decision<TeamSlot, Team>> result = new ArrayList<>();

        //Выделяем оборотные бригады
        List<Team> returningTeam = new ArrayList<>();
        for (Team team : bufferTeams) {
            if (station.equals(team.lastEvent().getStation()) && !station.equals(team.getDepot())) {
                returningTeam.add(team);
            }
        }
        Collections.sort(returningTeam, ComparatorBuilder.teamTimeOfPresenceComparator());
        for (Team team : returningTeam) {
            Iterator<TeamSlot> itr = bufferSlots.iterator();
            while (itr.hasNext()) {
                TeamSlot teamSlot = itr.next();

                boolean checkResult = new TeamSlotAndTeamChecker(teamSlot, team)
                        .basicCheck()
                        .checkDepotDirection()
                        .getCheckResult();

                if (checkResult) {
                    result.add(new Decision<>(teamSlot, team));
                    bufferTeams.remove(team);
                    itr.remove();
                    break;
                }
            }
        }

        //Выделяем местные бригады
        List<Team> localTeams = new ArrayList<>();
        for (Team team : bufferTeams) {
            if (station.equals(team.lastEvent().getStation()) && station.equals(team.getDepot())) {
                localTeams.add(team);
            }
        }
        Collections.sort(localTeams, ComparatorBuilder.teamTimeOfPresenceComparator());
        for (Team team : localTeams) {
            Iterator<TeamSlot> itr = bufferSlots.iterator();
            while (itr.hasNext()) {
                TeamSlot teamSlot = itr.next();
                boolean checkResult = new TeamSlotAndTeamChecker(teamSlot, team)
                        .basicCheck()
                        .checkTime()
                        .getCheckResult();

                if (checkResult) {
                    result.add(new Decision<>(teamSlot, team));
                    bufferTeams.remove(team);
                    itr.remove();
                    break;
                }
            }
        }

        if (bufferSlots.size() > 0) {
            List<Team> foreignTeam = new ArrayList<>();
            List<Station> stations = new ArrayList<>(getNearestStationWithTeamChange(station));
            for (Team team : bufferTeams) {
                if (stations.contains(team.lastEvent().getStation())) {
                    foreignTeam.add(team);
                }
            }

            Collections.sort(foreignTeam, ComparatorBuilder.teamTimeOfPresenceComparator());

            for (Team team : foreignTeam) {
                Iterator<TeamSlot> itr = bufferSlots.iterator();
                while (itr.hasNext()) {
                    TeamSlot teamSlot = itr.next();

                    boolean checkResult = new TeamSlotAndTeamChecker(teamSlot, team)
                            .basicCheck()
                            //.checkTime()
                            .getCheckResult();

                    if (checkResult) {
                        result.add(new Decision<>(teamSlot, team));
                        bufferTeams.remove(team);
                        itr.remove();
                        break;
                    }
                }
            }
        }

        return result;
    }

    private Set<Station> getNearestStationWithTeamChange(Station station) {
        Set<Station> result = getNearestStationWithTeamChange(station, new ArrayList<Link>());
        result.remove(station);
        return result;
    }

    private Set<Station> getNearestStationWithTeamChange(Station station, List<Link> usedLinks) {
        Set<Station> result = new HashSet<>();
        List<Link> outgoingLinks = station.getLinks();
        for (Link link : outgoingLinks) {
            if (!usedLinks.contains(link)) {
                usedLinks.add(link);
                if (link.getTo().getNormTime() > 0) {
                    result.add(link.getTo());
                } else {
                    result.addAll(getNearestStationWithTeamChange(link.getTo(), usedLinks));
                }
            }
        }
        return result;
    }

    private class TeamSlotAndTeamChecker {

        boolean all = true;
        TeamSlot teamSlot;
        Team team;

        public TeamSlotAndTeamChecker(TeamSlot teamSlot, Team team) {
            this.teamSlot = teamSlot;
            this.team = team;
        }

        private TeamSlotAndTeamChecker checkDepotDirection() {
            if (!all) return this;
            for (SlotTrack track : teamSlot.route) {
                if (track.getLink().getTo().equals(team.getDepot())) {
                    all = true;
                    return this;
                }
            }
            all = false;
            return this;
        }
        private TeamSlotAndTeamChecker basicCheck() {
            if (!all) return this;
            SlotTrack firstTrack = teamSlot.route.get(0);
            boolean isNeedMovePassanger = !team.lastEvent().getStation().equals(firstTrack.getLink().getFrom());
            long shiftTime = 0L;
            if (isNeedMovePassanger) {
                Capacity capacity = capacityManager.foundCapacity(firstTrack, false, teamSlot, team.timeOfPresence());
                if (capacity != null) {
                    shiftTime = capacity.getStartTime() - firstTrack.getTimeStart();
                    if (shiftTime < 0L) shiftTime = 0L;
                }
            }

            Station station = TeamChange.getTeamChangeStation(team, teamSlot, 0L, 0L, 0L);
            if (station != null) {
                all = true;
                return this;
            }
            all = false;
            return this;
        }
        private TeamSlotAndTeamChecker checkTime() {
            if (!all) return this;
            if (teamSlot.route.size() == 0) {
                all = false;
                return this;
            }
            SlotTrack firstTrack = teamSlot.route.get(0);
            if (team.timeOfPresence() >= firstTrack.getTimeStart()) {
                all = false;
            }
            return this;
        }

        private boolean getCheckResult() {
            return all;
        }
    }

    static class ComparatorBuilder {
        static BulkTeamAssign bulkTeamAssign = new BulkTeamAssign();
        static TeamTimeOfPresenceComparator teamTimeOfPresenceComparator() {
            return bulkTeamAssign.new TeamTimeOfPresenceComparator();
        }
        static TeamSlotTimeStartComparator teamSlotTimeStartComparator() {
            return bulkTeamAssign.new TeamSlotTimeStartComparator();
        }
    }

    private class TeamTimeOfPresenceComparator implements Comparator<Team> {
        @Override
        public int compare(Team o1, Team o2) {
            return o1.timeOfPresence().compareTo(o2.timeOfPresence());
        }
    }

    private class TeamSlotTimeStartComparator implements  Comparator<TeamSlot> {
        @Override
        public int compare(TeamSlot o1, TeamSlot o2) {
            Long firstTime1 = o1.route.size() > 0 ? o1.route.get(0).getTimeStart() : 0;
            Long firstTime2 = o2.route.size() > 0 ? o2.route.get(0).getTimeStart() : 0;
            return firstTime1.compareTo(firstTime2);
        }
    }
}