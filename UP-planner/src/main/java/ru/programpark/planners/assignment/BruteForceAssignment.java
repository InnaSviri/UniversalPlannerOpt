package ru.programpark.planners.assignment;

        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.util.*;

/**
 * User: oracle
 * Date: 11.06.15
 * MultiD
 */
public class BruteForceAssignment implements AssignmentProblem {//, Runnable {
    Random generator = new Random(1L);
    private PartialAssignmentMap initialAssignment = null;
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(BruteForceAssignment.class);
        return logger;
    }

    public static AssignmentParams DEFAULT_PARAMS = new AssignmentParams() {{
        annealing_R = 0.1;
        annealing_T = 10.0;
        annealing_T_threshold = 5;
        annealing_N = 1.0;
        annealing_accept = 0.3;
    }};

    public void setInitialAssignment(PartialAssignmentMap initialAssignment) {
        this.initialAssignment = initialAssignment;
    }

    double coolingRate, To, T_threshold, NUMcoef, perceptOfAcceptableSolution;
    { setParams(DEFAULT_PARAMS); }

    public void setParams(AssignmentParams params) {
        coolingRate = params.annealing_R;
        To = params.annealing_T;
        T_threshold = params.annealing_T_threshold;
        NUMcoef = params.annealing_N;
        perceptOfAcceptableSolution = params.annealing_accept;
    }

    public PartialAssignmentMap decision(PartialAssignmentMap utils) {
        List<PartialAssignmentMap> res = decisions(utils);
        return res.isEmpty() ? null : res.get(0);
    }

    public List<PartialAssignmentMap> decisions(PartialAssignmentMap utils) {
        To = 10;
        List<PartialAssignmentMap> res = new ArrayList<>();
        if (initialAssignment == null)
            initialAssignment = initiateAssignment(utils);
        PartialAssignmentMap currentAssignment = initialAssignment;
        PartialAssignmentMap newAssignment = initialAssignment;
        int iterations = 0;

        res.add(initialAssignment);
        LOGGER().trace(" Размерность задачи о назначениях методом имитации отжига: " + newAssignment.getDimensions());
        long N = utils.getMaxDimension();
        int NUM = 0;
        while (NUM < 10*N) {
            int i = utils.getIndexOfMaxDimension();//generator.nextInt(utils.getDimensions().size());  //(1) Randomly select dimension
            int D = utils.getDimensions().get(i);
            int j = generator.nextInt(D);  //(1) Randomly select dimension
            int pIndex = generator.nextInt(currentAssignment.getIndexToUtil().size());// index of partial assignment in current assignment
            newAssignment = exchangeAssignment(utils, currentAssignment, i, pIndex, j);  //(3) Exchange currentAssignment for those two elements
            //if (!newAssignment.equals(currentAssignment))
            //    newAssignment.prinAllIndicesInPartialAssignments();
            double delta = newAssignment.getSumUtility() - currentAssignment.getSumUtility();
            if (delta > 0 || (delta <= 0 & generator.nextDouble() <= Math.exp(-delta/ To))) //(4) Accept or reject Snew
                currentAssignment = newAssignment;
            if (!res.contains(newAssignment))
                res.add(newAssignment);
            NUM++;
            iterations++;
        }
        res = leaveTopPercent(res);
        initialAssignment = null;
        LOGGER().trace("Общее число итераций у отжига: " + iterations);

        return res;
    }

    private List<PartialAssignmentMap> leaveTopPercent(List<PartialAssignmentMap> res){
        Iterator<PartialAssignmentMap> iter = res.iterator();
        double maxElem = res.get(0).getSumUtility();//сверху всегда лежит результат аукциона
        while (iter.hasNext()){
            PartialAssignmentMap p = iter.next();
            //p.trim();
            if (p.getIndexToUtil() == null || p.getIndexToUtil().size() == 0 || p.getSumUtility().equals(AssignmentProblem.RESTRICT_VALUE))
                iter.remove();
            else {
                if (maxElem > 0) {
                    if (p.getSumUtility() < perceptOfAcceptableSolution*maxElem)
                        iter.remove();
                } else {
                    if (p.getSumUtility() > perceptOfAcceptableSolution*maxElem)
                        iter.remove();
                }
            }
        }
        //Collections.sort(res);

        return res;
    }

    private PartialAssignmentMap initiateAssignment(PartialAssignmentMap utils) {  // 1 - 1, 2 - 2, ...
        PartialAssignmentMap currentAssignment = new PartialAssignmentMap(utils.getDimensions());
        for (int i = 0; i < utils.getMinDimension(); i++){
            List<Integer> indices = new ArrayList<>();
            Integer numDim = utils.getDimensions().size();
            //indices.add(utilsPrime.getMinDimension() -1 - i);
            for (int j = 0; j < numDim; j ++){
                indices.add(i);
            }
            PartialAssignment p = new PartialAssignment(indices, utils.getUtility(indices));
            currentAssignment.addPartialAssignment(p);
        }

        return currentAssignment;
    }

    private PartialAssignmentMap exchangeAssignment(PartialAssignmentMap utils, PartialAssignmentMap currentAssignment, int col, int pIndex, int j){
        PartialAssignmentMap newAssignment = new PartialAssignmentMap(currentAssignment.getDimensions(), currentAssignment.getIndexToUtil());
        List<PartialAssignment> pList = new ArrayList<>();
        pList.addAll(newAssignment.getIndexToUtil().values());
        PartialAssignment p = pList.get(pIndex);

        PartialAssignment pPrime = new PartialAssignment(p.getIndices(), p.getUtil());
        List<Integer> indices = new ArrayList<>();
        indices.addAll(p.getIndices());
        indices.set(col, j);
        if (!utils.getUtility(indices).equals(AssignmentProblem.RESTRICT_VALUE)){
            pPrime.getIndices().set(col, j);
            pPrime.setUtil(utils.getUtility(pPrime.getIndices()));
            newAssignment.removePartialAssignment(p);
            newAssignment.addPartialAssignment(pPrime);
            if (!RepsInIndicesInCol(newAssignment, col))
                return newAssignment;
        }

        return currentAssignment;
    }

    private boolean RepsInIndicesInCol(PartialAssignmentMap assignmentMap, int col){
        Set<Integer> set = new HashSet<>();
        List<PartialAssignment> pList = new ArrayList<>();
        pList.addAll(assignmentMap.getIndexToUtil().values());
        for (PartialAssignment p: pList){
            set.add(p.getIndices().get(col));
        }

        if (set.size() == pList.size())
            return false;
        else
            return true;
    }
}

