package ru.programpark.planners.assignment;

import ru.programpark.planners.util.MatrixOperationsUtil;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 24.08.15
 * Time: 14:54
 * To change this template use File | Settings | File Templates.
 */
public class ShrinkUtilityMatrixWrapper {
    private PartialAssignmentMap utils, utilsPrime;//initialUtils, shrinkedUtils
    private List<PartialAssignmentMap> res, resPrime;
    private double [][] u, temp;// m x n
    private double [][] uPrime;// (m - N_row_shifts, n - N_col_shifts)
    private List<Integer> rowsToKeep = new ArrayList<>();
    private List<Integer> colsToKeep = new ArrayList<>();
    private int N_col_shifts, N_row_shifts,  n = 0 ,m = 0;
    private Map<Integer, Integer> rowFromPrime = new HashMap<>(), colFromPrime = new HashMap<>();// m, n
    private boolean transform = false;

    public ShrinkUtilityMatrixWrapper(PartialAssignmentMap utils) {
        this.utils = utils;
        this.transform = shrinkUtils();
    }

    boolean shrinkUtils(){
        boolean shrinked = false;
        Comparator<? super Integer> c = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        };

        //this.utils = utils;
        this.utilsPrime = new PartialAssignmentMap();
        if (utils.getDimensions().size() == 2){
            u = MatrixOperationsUtil.assignmentArrayTo2DMatrix(utils);
            temp = MatrixOperationsUtil.assignmentArrayTo2DMatrix(utils);
            m = u.length;
            n = u[0].length;

            for (int i = 0; i < m; i++){
                int count = 0;
                for (int j = 0; j < n; j++ ){
                    if (u[i][j] == AssignmentProblem.RESTRICT_VALUE){
                        count++;
                    }
                }
                if (count == n){
                    N_row_shifts++;
                } else {
                    rowsToKeep.add(i);
                }
            }

            int kNew = 0;
            for (Integer k: rowsToKeep){
                rowFromPrime.put(kNew, k);
                kNew++;
            }

            for (int j = 0; j < n; j++ ){
                int count = 0;
                for (int i = 0; i < m; i++ ){
                    if (u[i][j] == AssignmentProblem.RESTRICT_VALUE) {
                        count++;
                    }
                }
                if (count == m){
                    N_col_shifts++;
                }  else
                    colsToKeep.add(j);
            }

            kNew = 0;
            for (Integer k: colsToKeep){
                colFromPrime.put(kNew, k);
                kNew++;
            }

            if (N_row_shifts > 0 || N_col_shifts > 0){
                shrinked = true;
            }

            uPrime = new double[m - N_row_shifts][n - N_col_shifts];
            uPrime = MatrixOperationsUtil.removeSomeRowsAndColumns(temp, uPrime, rowFromPrime, colFromPrime);
            utilsPrime = new PartialAssignmentMap(uPrime);
        }
        return shrinked;
    }

    private List<PartialAssignmentMap> transformBackToInitialDimensions(){
        if (!transform) {
            return resPrime;
        } else {
            res = new ArrayList<>();
            for (PartialAssignmentMap pMap: resPrime){
                List<Integer> dimensions = new ArrayList<>();
                dimensions.add(m);
                dimensions.add(n);
                PartialAssignmentMap pMapNew = new PartialAssignmentMap(dimensions);
                for (PartialAssignment p: pMap.partialAssignments()){
                    List<Integer> indices = p.getIndices();
                    List<Integer> indicesNew = new ArrayList<>(indices.size());
                    indicesNew.addAll(indices);
                    indicesNew.set(0, rowFromPrime.get(indices.get(0)));
                    indicesNew.set(1, colFromPrime.get(indices.get(1)));
                    PartialAssignment pNew = new PartialAssignment(indicesNew, pMap.getUtility(p.getIndices()));
                    pMapNew.addPartialAssignment(pNew);
                }
                res.add(pMapNew);
            }
            return res;
        }
    }

    public PartialAssignmentMap getUtils() {
        return utils;
    }

    public void setUtils(PartialAssignmentMap utils) {
        this.utils = utils;
    }

    public PartialAssignmentMap getUtilsPrime() {
        if (transform)
            return utilsPrime;
        else
            return utils;
    }

    public void setUtilsPrime(PartialAssignmentMap utilsPrime) {
        this.utilsPrime = utilsPrime;
    }

    public List<PartialAssignmentMap> getRes(List<PartialAssignmentMap> resPrime) {
        this.resPrime = resPrime;

        res = transformBackToInitialDimensions();

        return res;
    }

    public void printAllUtils(){
        System.out.println("@@@ U:");
        System.out.println(Arrays.deepToString(u));
        System.out.println("UPrime:");
        System.out.println(Arrays.deepToString(uPrime));
    }
}
