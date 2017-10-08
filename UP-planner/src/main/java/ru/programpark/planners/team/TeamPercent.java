package ru.programpark.planners.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.data.OutputData;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.team.RealTeam;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.planners.assignment.PartialAssignment;
import ru.programpark.planners.assignment.PartialAssignmentMap;
import ru.programpark.planners.common.SchedulingData;
import ru.programpark.planners.common.Team;

import java.util.*;

public class TeamPercent {
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(TeamAssignment.class);
        return logger;
    }
    public static double utilDeviationCoef = 0.7, percentDeviationCoef = 1.0;

    public Map<Long, TeamRegion> getActualTeamPercent(InputData iData, OutputData oData) {
        Map<Long, TeamRegion> regionsWithActualPercent = new HashMap<>();

        for (TeamRegion region : iData.getTeamServiceRegions().values()) {
            List<RealTeam> teams = getSlotTeamsInRegion(region, oData.getSlotTeams());
            Map<Station, Integer> percent = getTeamPercentFroRegion(region, teams);
            TeamRegion regionForOData = new TeamRegion(region.getId(), region.getStationPairs(), 0L, 0L, percent);
            regionsWithActualPercent.put(regionForOData.getId(), regionForOData);
        }

        return regionsWithActualPercent;
    }

    public PartialAssignmentMap getDecisionToBeUsedAccordingToTeamPercent(List<PartialAssignmentMap> result,
                                                                          List<TeamSlot> teamSlots,
                                                                          List<Team> teamsInSolution) {
        InputData iData = SchedulingData.getInputData();
        Map<PartialAssignmentMap, Double> percentDeltaByDecision = new HashMap<>();
        Map<PartialAssignmentMap, Double> utilityDeltaByDecision = new HashMap<>();
        Map<PartialAssignmentMap, Double> finalDelta;
        PartialAssignmentMap bestDecision = result.get(0);
        Double maxUtil = Double.NEGATIVE_INFINITY;

        for (PartialAssignmentMap decision: result){
            utilityDeltaByDecision.put(decision, decision.getSumUtility());
            if (decision.getSumUtility() > maxUtil)
                maxUtil = decision.getSumUtility();
            List<PartialAssignment> allPartialAssignments = new ArrayList<>();
            allPartialAssignments.addAll(decision.partialAssignments());
            double finalDev = 0.0;
            for (TeamRegion region: iData.getTeamServiceRegions().values()) {
                List<PartialAssignment> teams = getAllWorkingTeams(region, allPartialAssignments, teamSlots);
                Double dev = getTeamPercentForRegion(region, teams, teamsInSolution);
                finalDev += dev;
            }
            percentDeltaByDecision.put(decision, finalDev/ iData.getTeamServiceRegions().values().size());
        }

        for (Map.Entry<PartialAssignmentMap, Double> entry: utilityDeltaByDecision.entrySet()){
            utilityDeltaByDecision.put(entry.getKey(), maxUtil - entry.getValue());
        }

        normalize(percentDeltaByDecision);
        normalize(utilityDeltaByDecision);
        finalDelta = calculateFinalDelta(percentDeltaByDecision, utilityDeltaByDecision);


        List<Map.Entry<PartialAssignmentMap, Double>> list = new ArrayList<>();
        list.addAll(finalDelta.entrySet());

        Comparator<Map.Entry<PartialAssignmentMap, Double>> c = new Comparator<Map.Entry<PartialAssignmentMap, Double>>() {
            @Override
            public int compare(Map.Entry<PartialAssignmentMap, Double> o1, Map.Entry<PartialAssignmentMap, Double> o2) {
                if (o1.getValue() != o2.getValue()) {
                    return Double.compare(o1.getValue(), o2.getValue());
                } else {
                    return ( (int) (o2.getKey().getSumUtility() - o1.getKey().getSumUtility()));
                }
            }
        };

        Collections.sort(list, c); //ascending
        bestDecision = list.get(0).getKey();
        List<PartialAssignment> allPartialAssignments = new ArrayList<>();
        allPartialAssignments.addAll(bestDecision.partialAssignments());
        for (TeamRegion region: iData.getTeamServiceRegions().values()) {
            List<PartialAssignment> teams = getAllWorkingTeams(region, allPartialAssignments, teamSlots);
            //Double percent = getTeamPercentFroRegion(region, teams, readyTeams, iData);
        }
        return bestDecision;
    }

    private TeamRegion getRegion(List<PartialAssignment> allPartialAssignments,  List<TeamSlot> teamSlots){
        InputData iData = SchedulingData.getInputData();
        //найти все возможные линки
        for (PartialAssignment p: allPartialAssignments) {
            TeamSlot teamSlot = teamSlots.get(p.getIndices().get(0));
            SlotTrack firstTrack = teamSlot.route.get(0);
            //Определить подходит ли
            for (TeamRegion region: iData.getTeamServiceRegions().values()){
                if (region != null && region.containsLink(firstTrack.getLink())) {
                    return region; // переделать на проверку вхождения всего маршрута в регион todo
                }
            }
        }

        return null;
    }

    private List<RealTeam> getSlotTeamsInRegion(TeamRegion region, Map<Long, RealTeam> slotTeams){
        List<RealTeam> res = new ArrayList<>();
        List<Station> depots = new ArrayList<>();
        depots.addAll(region.getPercentByDepot().keySet());

        for (RealTeam team: slotTeams.values()){
            if (depots.size() >= 2 && team.passesStation(depots.get(0).getId()) &&
                    team.passesStation(depots.get(1).getId()))
                res.add(team);
        }

        return res;
    }

    private List<PartialAssignment> getAllWorkingTeams(TeamRegion region, List<PartialAssignment> allPartialAssignments,
                                                       List<TeamSlot> teamSlots){
        List<PartialAssignment> res = new ArrayList<>();

        //найти все возможные линки
        for (PartialAssignment p: allPartialAssignments) {
            TeamSlot teamSlot = teamSlots.get(p.getIndices().get(0));
            SlotTrack firstTrack = teamSlot.route.get(0);
            //Определить подходит ли
            if (region != null && region.containsLink(firstTrack.getLink())) {
                res.add(p); // переделать на проверку вхождения всего маршрута в регион todo
            }
        }

        return res;
    }

    private Map<Station, Integer> getTeamPercentFroRegion(TeamRegion region, List<RealTeam> teams){
        List<Station> depots = new ArrayList<>();
        Map<Station, Integer> percentForDepot = new HashMap();
        Map<Station, Integer> sentByDepot = new HashMap();
        Integer allSent = teams.size();

        depots.addAll(region.getPercentByDepot().keySet());
        for (Station depot: depots){
            percentForDepot.put(depot, 0);
            sentByDepot.put(depot, 0);
        }

        if (allSent.equals(0))
            return percentForDepot;

        Map<Long, FactTeam> fTeams = SchedulingData.getInputData().getFactTeams();
        for (RealTeam rTeam: teams){
            FactTeam factTeam = fTeams.get(rTeam.getId());
            Station depot = factTeam.getDepot();
            if (sentByDepot.get(depot) != null){
                int temp = sentByDepot.get(depot);
                sentByDepot.put(depot, ++temp);
            }
        }

        return percentForDepot;
    }

    private Double getTeamPercentForRegion(TeamRegion region, List<PartialAssignment> teams, List<Team> readyTeams) {
        InputData iData = SchedulingData.getInputData();
        List<Station> depots = new ArrayList<>();
        Map<Station, Double> percentForDepot = new HashMap();
        Map<Station, Integer> sentByDepot = new HashMap();
        Integer allSent = teams.size();
        Double res = 0.0;

        if (allSent.equals(0))
            return 0.0;
        depots.addAll(region.getPercentByDepot().keySet());
        for (Station depot: depots){
            percentForDepot.put(depot, 0.0);
            sentByDepot.put(depot, 0);
        }

        int i = 0;
        for (PartialAssignment p: teams){
            Team readyTeam = readyTeams.get(p.getIndices().get(1));
            FactTeam factTeam = iData.getFactTeams().get(readyTeam.getId());
            Station depot = factTeam.getDepot();
            if (sentByDepot.get(depot) != null){
                int temp = sentByDepot.get(depot);
                sentByDepot.put(depot, ++temp);
            }
         }

        for (Station depot: depots){
            percentForDepot.put(depot, sentByDepot.get(depot)*100.0/allSent);
            res += Math.abs(percentForDepot.get(depot) - region.getPercentByDepot().get(depot));
        }

        res = res/depots.size();

        return res;
    }

    private Map<PartialAssignmentMap, Double> calculateFinalDelta(Map<PartialAssignmentMap, Double> deltaPercent,
                                                                  Map<PartialAssignmentMap, Double> deltaU){
        Map<PartialAssignmentMap, Double> delta = new HashMap<>();

        for (Map.Entry<PartialAssignmentMap, Double> entry: deltaPercent.entrySet()){
            Double d = percentDeviationCoef*entry.getValue() + utilDeviationCoef*deltaU.get(entry.getKey());
            delta.put(entry.getKey(), d);
        }

        return delta;
    }

    private void normalize( Map<PartialAssignmentMap, Double> totalDeviationByDecision){
        Double maxElem = Double.NEGATIVE_INFINITY;

        for (Map.Entry<PartialAssignmentMap, Double> entry: totalDeviationByDecision.entrySet()){
            if (entry.getValue() > maxElem)
                maxElem = entry.getValue();
        }

        for (Map.Entry<PartialAssignmentMap, Double> entry: totalDeviationByDecision.entrySet()){
            totalDeviationByDecision.put(entry.getKey(), entry.getValue()/maxElem);
        }
    }
}
