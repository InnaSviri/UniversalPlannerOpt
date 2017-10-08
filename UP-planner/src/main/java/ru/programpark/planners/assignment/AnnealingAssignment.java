package ru.programpark.planners.assignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * User: oracle
 * Date: 11.06.15
 * MultiD
 */
public class AnnealingAssignment implements AssignmentProblem {//, Runnable {
    Random generator = new Random(1L);

    private static Logger logger = null;

    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(AnnealingAssignment.class);
        return logger;
    }

    public static AssignmentParams DEFAULT_PARAMS = new AssignmentParams() {{
        annealing_R = 0.3;
        annealing_T = 10.0;
        annealing_T_threshold = 0.1;
        annealing_N = 1.0;
        annealing_accept = 0.5;
    }};

    double R, T, T_threshold, NUMcoef, perceptOfAcceptableSolution;

    {
        setParams(DEFAULT_PARAMS);
    }

    public void setParams(AssignmentParams params) {
        R = params.annealing_R;
        T = params.annealing_T;
        T_threshold = params.annealing_T_threshold;
        NUMcoef = params.annealing_N;
        perceptOfAcceptableSolution = params.annealing_accept;
    }

    public PartialAssignmentMap decision(PartialAssignmentMap utils) {
        List<PartialAssignmentMap> res = decisions(utils);
        return res.isEmpty() ? null : res.get(0);
    }

    public List<PartialAssignmentMap> decisions(PartialAssignmentMap utils) {
        T = 10;
        List<PartialAssignmentMap> res = new ArrayList<>();
        PartialAssignmentMap currentAssignment = initiateAssignment(utils);
        PartialAssignmentMap newAssignment = currentAssignment;
        int i, j1 = 0, j2 = 0, k = 0;

        res.add(newAssignment);
        LOGGER().trace(" Размерность задачи о назначениях методом имитации отжига: " + newAssignment.getDimensions());
        while (T > T_threshold) { //(5)
            long N = utils.getMaxDimension();
            long NUMmax = Math.round(NUMcoef * N * (N - 1) / 2);
            int NUM = 0;
            while (NUM < NUMmax) {
                i = generator.nextInt(utils.getDimensions().size());  //(1) Randomly select dimension
                int Ni = utils.getDimensions().get(i);
                boolean stop = false;
                int rep = 0;
                while (!stop && rep < 10) {//(2) Randomly select 2 elements in dimension
                    j1 = generator.nextInt(Ni);
                    j2 = generator.nextInt(Ni);
                    if (j1 != j2) stop = true;
                    rep++;
                }
                newAssignment = exchangeAssignment(utils, currentAssignment, i, j1, j2);  //(3) Exchange currentAssignment for those two elements
                double delta = newAssignment.getSumUtility() - currentAssignment.getSumUtility();
                if (delta > 0 | (delta <= 0 & generator.nextDouble() <= Math.exp(-delta / T))) //(4) Accept or reject Snew
                    currentAssignment = newAssignment;
                if (!res.contains(newAssignment))
                    res.add(newAssignment);
                NUM++;
                k++;
            }
            T *= R;
        }
        res = leaveTopPercent(res);
        LOGGER().trace("Общее число итераций у отжига: " + k);

        return res;
    }

    private List<PartialAssignmentMap> leaveTopPercent(List<PartialAssignmentMap> res) {
        Collections.sort(res);
        Iterator<PartialAssignmentMap> iter = res.iterator();
        int i = 0;
        int length = (int) (res.size()* perceptOfAcceptableSolution);
        while (iter.hasNext()) {
            PartialAssignmentMap p = iter.next();
            p.trim();

            if (p.getIndexToUtil() == null || p.getIndexToUtil().size() == 0 || p.getSumUtility().equals(AssignmentProblem.RESTRICT_VALUE))
                iter.remove();
            else {
                if (i >= length) {
                    iter.remove();
                } else {
                    i++;
                }
            }
        }

        return res;
    }

    private PartialAssignmentMap initiateAssignment(PartialAssignmentMap utils) {  // 1 - 1, 2 - 2, ...
        PartialAssignmentMap currentAssignment = new PartialAssignmentMap(utils.getDimensions());
        for (int i = 0; i < utils.getMinDimension(); i++) {
            List<Integer> indices = new ArrayList<>();
            Integer numDim = utils.getDimensions().size();
            //indices.add(utilsPrime.getMinDimension() -1 - i);
            for (int j = 0; j < numDim; j++) {
                indices.add(i);
            }
            PartialAssignment p = new PartialAssignment(indices, utils.getUtility(indices));
            currentAssignment.addPartialAssignment(p);
        }

        return currentAssignment;
    }

    private PartialAssignmentMap exchangeAssignment(PartialAssignmentMap utils, PartialAssignmentMap currentAssignment,
                                                    int col, int i, int j) {
        PartialAssignmentMap newAssignment = new PartialAssignmentMap(currentAssignment.getDimensions(), currentAssignment.getIndexToUtil());
        PartialAssignment p1 = newAssignment.getPartialAssignment(col, i);
        PartialAssignment p2 = newAssignment.getPartialAssignment(col, j);
        int colNew, iNew, jNew;
        if (col != utils.getDimensions().size() - 1) {
            colNew = col + 1;
        } else {
            colNew = col - 1;
        }

        if (p1 == null && p2 == null) { // если так то ничего не делаем
            return newAssignment;
        } else if (p1 == null && p2 != null) {
            PartialAssignment p2Prime = new PartialAssignment(p2.getIndices(), p2.getUtil());
            if (i < utils.getDimensions().get(col)) {
                p2Prime.getIndices().set(col, i);
                p2Prime.setUtil(utils.getUtility(p2Prime.getIndices()));
                newAssignment.removePartialAssignment(p2);
                newAssignment.addPartialAssignment(p2Prime);
            }
        } else if (p1 != null && p2 == null) {
            PartialAssignment p1Prime = new PartialAssignment(p1.getIndices(), p1.getUtil());
            if (j < utils.getDimensions().get(col)) {
                p1Prime.getIndices().set(col, j);
                p1Prime.setUtil(utils.getUtility(p1Prime.getIndices()));
                newAssignment.removePartialAssignment(p1);
                newAssignment.addPartialAssignment(p1Prime);
            }
        } else {
            iNew = p1.getIndices().get(colNew);
            jNew = p2.getIndices().get(colNew);

            PartialAssignment p1Prime = new PartialAssignment(p1.getIndices(), p1.getUtil());
            if (jNew < utils.getDimensions().get(colNew)) {
                p1Prime.getIndices().set(colNew, jNew);
                p1Prime.setUtil(utils.getUtility(p1Prime.getIndices()));
                newAssignment.removePartialAssignment(p1);
                newAssignment.addPartialAssignment(p1Prime);
            }
            PartialAssignment p2Prime = new PartialAssignment(p2.getIndices(), p2.getUtil());
            if (iNew < utils.getDimensions().get(colNew)) {
                p2Prime.getIndices().set(colNew, iNew);
                p2Prime.setUtil(utils.getUtility(p2Prime.getIndices()));
                newAssignment.removePartialAssignment(p2);
                newAssignment.addPartialAssignment(p2Prime);
            }
        }

        return newAssignment;
    }
}


