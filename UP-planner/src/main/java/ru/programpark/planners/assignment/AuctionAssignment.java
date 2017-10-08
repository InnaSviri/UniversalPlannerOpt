package ru.programpark.planners.assignment;

// 2D only

import ru.programpark.planners.util.MatrixOperationsUtil;

import java.util.*;

public class AuctionAssignment implements AssignmentProblem {//, Runnable {
    public static AssignmentParams DEFAULT_PARAMS = new AssignmentParams() {{
        auction_ɛ = 0.05;
        auction_λ = 0.0;//0.09;
    }};

    double E_coef, LAMBDA;
    { setParams(DEFAULT_PARAMS); }

    public void setParams(AssignmentParams params) {
        E_coef = params.auction_ɛ;
        LAMBDA = params.auction_λ;
    }

    double priorPrice[];
    double price[];
    double profit[];
    double matrix[][];
    int assignment[];
    int maxCalc = 0;
    PartialAssignmentMap utils;

    double maxSumUtils = Double.NEGATIVE_INFINITY;
    int betterAssignment[];
    //Map<Double, int[]> maxStackOne = new TreeMap<>();
    //List<int[]> maxStackTwo = new ArrayList<>();

    @Override
    public PartialAssignmentMap decision(PartialAssignmentMap utils) {
        this.utils = utils;
        maxSumUtils = Double.NEGATIVE_INFINITY;

        double [][] u = MatrixOperationsUtil.assignmentArrayTo2DMatrix(utils);
        List<int[]> result = decisions(u, false);
        return vectorToAssignmentArray(result.get(0));
    }

    @Override
    public List<PartialAssignmentMap> decisions(PartialAssignmentMap utils) {
        List<PartialAssignmentMap> res = new ArrayList<>();
        maxSumUtils = Double.NEGATIVE_INFINITY;

        this.utils = utils;
        double [][] u = MatrixOperationsUtil.assignmentArrayTo2DMatrix(utils);
        List<int[]> result = decisions(u, false);
        PartialAssignmentMap arr = null;

        for (int[] oneResult : result) {
            //printArray(oneResult);
            arr = vectorToAssignmentArray(oneResult);
            res.add(0, arr);
        }
        /*if (result.size() > 0) {
            arr = vectorToAssignmentArray(result.get(result.size() - 1));
            resPrime.add(0, arr);
        }*/
        /*System.out.println("-------------------------------");
        int counter = 0;
        for (PartialAssignmentMap oneRes : resPrime) {
            double sum = oneRes.getSumUtility();
            System.out.println(counter + " : " + sum);
            counter++;
        }
        System.out.println("-------------------------------");*/
        return res;
    }



    private PartialAssignmentMap vectorToAssignmentArray(int[] result){
        PartialAssignmentMap assignment =
                new PartialAssignmentMap(utils.getDimensions());
        for (PartialAssignment p : utils.partialAssignments()) {
            int i = p.getIndices().get(0), j = p.getIndices().get(1);
            if (i < result.length && result[i] == j) {
                assignment.addPartialAssignment(p);
            }
        }
        return assignment;
    }

    private List<int[]> decisions(double[][] matrix, boolean getAll) {

        boolean transpose = false;
        if (matrix.length > matrix[0].length) {
            transpose = true;
            double[][] buffer = new double[matrix[0].length][matrix.length];
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    buffer[j][i] = matrix[i][j];
                }
            }
            matrix = buffer;
        }

        List<int[]> decisions = new ArrayList<>();
        this.matrix = matrix;
        priorPrice = new double[matrix[0].length];
        price = new double[matrix[0].length];
        profit = new double[matrix[0].length];
        assignment = new int[matrix.length];
        betterAssignment = new int[matrix.length];
        maxCalc = matrix[0].length * 10;//matrix[0].length;
        for (int i = 0; i < assignment.length; i++) {
            assignment[i] = -1;
        }

        while (checkAssignment()) {
            for (int i = 0; i < assignment.length; i++) {
                if (assignment[i] == -1) {
                    getBetterSlot(i);
                }
            }
            if (checkStop()) {
                break;
            }
            priorPrice = Arrays.copyOf(price, price.length);
            //printArray(profit);
            //printArray(assignment);
            //PartialAssignmentMap arr = vectorToAssignmentArray(assignment);

            if (getAll) {
                //System.out.println(sum + " " + arr.getSumUtility());
                int [] buffer = Arrays.copyOf(assignment, assignment.length);
                decisions.add(buffer);
            }
            double sum = calcSumUtils();
            if (maxSumUtils < sum) {
                maxSumUtils = sum;
                betterAssignment = Arrays.copyOf(assignment, assignment.length);
            }
            //printArray(assignment);
        }

        if (!getAll) {
            double sum = calcSumUtils();
            if (sum <= maxSumUtils) {
                decisions.add(betterAssignment);
            } else {
                decisions.add(assignment);
            }
        }

        if (transpose) {
            List<int[]> transposeDecisions = new ArrayList<>();
            for (int[] decision : decisions) {
                int[] buffer = new int[matrix[0].length];
                for (int i = 0; i < buffer.length; i++) buffer[i] = -1;
                for (int i = 0; i < decision.length; i++) {
                    if (decision[i] != -1) {
                        buffer[decision[i]] = i;
                    }
                }
                transposeDecisions.add(buffer);
            }
            return transposeDecisions;
        } else {
            return decisions;
        }
    }

    public static void printArray(int[] array) {
        for (int d : array) {
            System.out.print(d + "; ");
        }
        System.out.print("\n");
    }

    private void printArray(double[] array) {
        for (double d : array) {
            System.out.print(d + "; ");
        }
        System.out.print("\n");
    }

    /**
     * Возвращает true, если в решении есть не назначенные пункты
     * @return
     */
    private Boolean checkAssignment() {
        Boolean b = false;
        for (int i = 0; i < assignment.length; i++) {
            if (assignment[i] == -1) {
                b = true;
                break;
            }
        }
        return b;
    }

    private void getBetterSlot(int entityIndex) {

        if (assignment[entityIndex] != -1) return;

        for (int i = 0; i < profit.length; i++) {
            /*if (matrix[entityIndex][i] == AssignmentProblem.RESTRICT_VALUE) {
                profit[i] = AssignmentProblem.RESTRICT_VALUE;
            } else {
                profit[i] = matrix[entityIndex][i] - price[i];
            }*/
            //profit[i] = matrix[entityIndex][i] - price[i];
            /*if (matrix[entityIndex][i] - price[i] > 0.0) {
                profit[i] = matrix[entityIndex][i] - price[i];
            } else {
                profit[i] = AssignmentProblem.RESTRICT_VALUE;
            }*/
            profit[i] = matrix[entityIndex][i] - price[i];
        }

        int betterIndex = getBetterElementFromArray(profit);
        int priorBetterIndex = betterIndex != -1 ? getBetterElementFromArray(profit, betterIndex) : -1;

        double w = AssignmentProblem.RESTRICT_VALUE;
        if (priorBetterIndex != -1) {
            w = profit[priorBetterIndex];
        }

        double newPrice = matrix[entityIndex][betterIndex] - w + E_coef;
        if (newPrice /*- price[betterIndex]*/ > LAMBDA) {
            price[betterIndex] = Math.max(newPrice, LAMBDA);
            int oldEntity = getEntityBySlot(betterIndex);
            if (oldEntity != -1) {

                //тогда мы снимаем предыдущее назначение
                assignment[oldEntity] = -1;
            }

            assignment[entityIndex] = betterIndex;
        }
    }


    private int getEntityBySlot(int slot) {
        for (int i = 0; i < assignment.length; i++) {
            if (assignment[i] == slot) {
                return i;
            }
        }
        return -1;
    }


    private int getBetterElementFromArray(double[] array, int withoutIndex) {
        double better = Double.NEGATIVE_INFINITY;
        int betterIndex = -1;
        for (int i = 0; i < array.length; i++) {
            if (i != withoutIndex && /*array[i] != AssignmentProblem.RESTRICT_VALUE &&*/ better < array[i]) {
                better = array[i];
                betterIndex = i;
            }
        }
        return betterIndex;
    }

    private int getBetterElementFromArray(double[] array) {
        int betterIndex = getBetterElementFromArray(array, -1);
        return betterIndex;
    }

    private Boolean checkStop() {

        //System.out.println(calcSumUtils());

        if (maxCalc == 0) {
            return true;
        } else {
            maxCalc--;
        }

        /*double sum = calcSumUtils();
        if (sum < maxSumUtils) {
            maxStackOne.put(sum, assignment);
        } else if (sum > maxSumUtils) {
            maxSumUtils = sum;
            maxStackOne.clear();
            maxStackOne.put(sum, assignment);
        } else if (sum == maxSumUtils) {

            //проверим будет ли полное совпадение в стеке
            Map.Entry<Double, int[]> priorEntry = null;
            boolean isFirst = true;
            boolean isSecond = false;
            boolean checked = false;
            boolean error = false;
            for (Map.Entry<Double, int[]> entry : maxStackOne.entrySet()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    if (entry.getKey().equals(priorEntry.getKey())) {
                        checked = true;
                    } else {
                        error = true;
                        break;
                    }
                    isFirst = true;
                }
                priorEntry = entry;
            }
            if (!error) {
                System.out.println("bingo" + error);
            }
        }*/

        return false;

        /*Boolean isDifferent = false;
        for (int i = 0; i < price.length; i++) {
            if (price[i] != priorPrice[i]) {
                isDifferent = true;
                break;
            }
        }

        return !isDifferent;*/
    }

    private double calcSumUtils() {
        double sum = 0.0;
        for (int i = 0; i < assignment.length; i++) {
            if (assignment[i] != -1) {
                sum += matrix[i][assignment[i]];
            }
        }
        return sum;
    }

    /*@Override
    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
        System.out.println("Auctions thread's id: " + Thread.currentThread().getId());
    }*/
}
