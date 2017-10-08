package ru.programpark.planners.assignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * User: oracle
 * Date: 25.08.15
 * Гибрид аукциона и отжига
 */
public class HybridAssignment implements AssignmentProblem{
    AnnealingAssignment annealing = new AnnealingAssignment();
    AuctionAssignment auction = new AuctionAssignment();
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(AnnealingAssignment.class);
        return logger;
    }

    public void setParams(AssignmentParams params) {
        annealing.setParams(params);
        auction.setParams(params);
    }

    public PartialAssignmentMap decision(PartialAssignmentMap utils){
        List<PartialAssignmentMap> res = decisions(utils);
        if (res.size() != 0)
            return res.get(0);
        else
            return null;
    }

    public List<PartialAssignmentMap> decisions(PartialAssignmentMap utils){
        PartialAssignmentMap resAuction = auction.decision(utils);

        ShrinkUtilityMatrixWrapper shrinkedUtilityWrapper = new ShrinkUtilityMatrixWrapper(utils);
        PartialAssignmentMap shrinkedUtils = shrinkedUtilityWrapper.getUtilsPrime();
        List<PartialAssignmentMap> resultShrinked = annealing.decisions(shrinkedUtils);
        List<PartialAssignmentMap> resAnnealing = shrinkedUtilityWrapper.getRes(resultShrinked);

        if (resAuction != null)
            LOGGER().trace("Auction " + resAuction.getSumUtility());
        if (resAnnealing.size() != 0)
            LOGGER().trace("Annealing " + resAnnealing.get(0).getSumUtility());

        if (!resAnnealing.contains(resAuction)){
            for (int i = 0; i < resAnnealing.size(); i++){
                PartialAssignmentMap elemInList = resAnnealing.get(i);
                if (resAuction.getSumUtility() > elemInList.getSumUtility()){
                    resAnnealing.add(i, resAuction);
                    break;
                }
            }
            if (resAnnealing.size() == 0 && resAuction != null) resAnnealing.add(resAuction);
        }

        return resAnnealing;
        /*List<PartialAssignmentMap> result = new ArrayList<>();
        result.add(resAuction);
        return result; */
    }
}
