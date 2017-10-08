package ru.programpark.planners.util;

import ru.programpark.planners.assignment.AssignmentProblem;
import ru.programpark.planners.assignment.PartialAssignment;
import ru.programpark.planners.assignment.PartialAssignmentMap;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 27.07.15
 * Time: 16:32
 * To change this template use File | Settings | File Templates.
 */
public class MatrixOperationsUtil {
    public static double[][] assignmentArrayTo2DMatrix(PartialAssignmentMap utils){
        if (utils.getDimensions().size() == 2) {
            int w = utils.getDimensions().get(0);
            int h = utils.getDimensions().get(1);
            double[][] matrix = new double[w][h];
            for (int i = 0; i < w; ++i) {
                Arrays.fill(matrix[i], AssignmentProblem.RESTRICT_VALUE);
            }
            for (PartialAssignment p: utils.partialAssignments()) {
                matrix[p.getIndices().get(0)][p.getIndices().get(1)] = p.getUtil();
            }
            return matrix;
        } else {
            throw new RuntimeException("Cannot run AuctionAssignment on a" +
                    " non-2D PartialAssignmentMap");
        }
    }

    public static void initializeToZeroes(Integer u[]){
        int m = u.length;

        for (int i = 0; i < m; i++ ){
            u[i] = 0;
        }
    }

    public static void initializeToZeroes(Integer u[][]){
        int m = u.length;
        int n = u[0].length;

        for (int i = 0; i < m; i++ ){
            for (int j = 0; j < n; j++ ){
                u[i][j] = 0;
            }
        }
    }

    public static double[][] removeSomeRowsAndColumns(double u[][], double uPrime[][], Map<Integer, Integer> rowFromPrime, Map<Integer, Integer> colFromPrime){
        int m = u.length;
        int n = u[0].length;
        int iPrime = 0, jPrime = 0;

        Comparator<Integer> c = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        };

        List<Integer> rowList = new ArrayList();
        rowList.addAll(rowFromPrime.keySet());
        Collections.sort(rowList, c);
        List<Integer> colList = new ArrayList();
        colList.addAll(colFromPrime.keySet());
        Collections.sort(colList, c);

        for (int i: rowList){
            Integer oldRowIndex = rowFromPrime.get(i);
            jPrime = 0;
            for (int j: colList){
                Integer oldColIndex = colFromPrime.get(j);
                uPrime[iPrime][jPrime] = u[oldRowIndex][oldColIndex];
                jPrime++;
            }
            iPrime++;
        }

        return uPrime;
    }

    public static double[][] copyArray(double from[][]){
        int m = from.length;
        int n = from[0].length;
        double[][] to = new double[m][n];

        for (int i = 0; i < m; i++ ){
            for (int j = 0; j < n; j++ ){
                to[i][j] = from[i][j];
            }
        }

        return to;
    }

    public static double [][] copyArray(double from[][], double to[][]){
        int n1 = from.length;
        int m1 = from[0].length;
        int n2 = to.length;
        int m2 = to[0].length;
        int n = Math.min(m1, m2);
        int m = Math.min(n1, n2);

        for (int i = 0; i < m; i++ ){
            for (int j = 0; j < n; j++ ){
                to[i][j] = from[i][j];
            }
        }

        return to;
    }
}
