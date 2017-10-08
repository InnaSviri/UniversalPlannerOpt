package ru.programpark.planners.assignment;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 15.06.15
 * Time: 10:58
 * To change this template use File | Settings | File Templates.
 */
public class PartialAssignmentMap implements Comparable<PartialAssignmentMap>{
    private List<Integer> dimensions = new ArrayList<>();
    private Map<List<Integer>, PartialAssignment> indexToUtil = new HashMap();

    public PartialAssignmentMap(List<Integer> dimensions, Map<List<Integer>, PartialAssignment> map) {
        this.dimensions = dimensions;

        for (PartialAssignment p:  map.values()){
            PartialAssignment newP = new PartialAssignment(p.getIndices(), p.getUtil());
            this.indexToUtil.put(newP.getIndices(), newP);
        }
    }

    public PartialAssignmentMap(List<Integer> dimensions) {//random utilsPrime
        this.dimensions = dimensions;
    }

    public PartialAssignmentMap() {
    }

    public PartialAssignmentMap(double[][] utils) {
        dimensions.add(utils.length);
        if (utils.length > 0) {
            dimensions.add(utils[0].length);
            for (int i = 0; i < dimensions.get(0); i++){
                for (int j = 0; j < dimensions.get(1); j++){
                    Integer[] indices = new Integer[2];
                    indices[0] = i;
                    indices[1] = j;
                    PartialAssignment p = new PartialAssignment(indices, utils[i][j]);
                    indexToUtil.put(p.getIndices(), p);
                }
            }
        } else {
            dimensions.add(0);
        }
    }

    public void addPartialAssignment(PartialAssignment p){
        if (!indexToUtil.containsKey(p.getIndices()))
            indexToUtil.put(p.getIndices(), p);
    }

    public void removePartialAssignment(PartialAssignment p){
        if (indexToUtil.containsKey(p.getIndices()))
            indexToUtil.remove(p.getIndices());
    }

    public Collection<PartialAssignment> partialAssignments() {
        return indexToUtil.values();
    }

    public boolean isEmpty() {
        return indexToUtil.isEmpty();
    }

    public Double getUtility(List<Integer> indices){
        PartialAssignment p = indexToUtil.get(indices);
        if (p == null)
            return AssignmentProblem.RESTRICT_VALUE;
        else
            return p.getUtil();
    }

    public PartialAssignment getPartialAssignment(int dim, int i1){
        for (PartialAssignment a: indexToUtil.values()){
            if (a.getIndices().get(dim) == i1){
                return a;
            }
        }
        return null;
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public Map<List<Integer>, PartialAssignment> getIndexToUtil() {
        return indexToUtil;
    }

    public void setIndexToUtil(Map<List<Integer>, PartialAssignment> indexToUtil) {
        this.indexToUtil = indexToUtil;
    }

    public int getMinDimension(){
        return Collections.min(dimensions);
    }

    public int getMaxDimension(){
        return Collections.max(dimensions);
    }

    public int getIndexOfMaxDimension(){
        int maxDim = Collections.max(dimensions);
        int j = 0;
        for (Integer dim: dimensions){
            if (dim.equals(maxDim))
                return j;
            j++;
        }
        return -1;
    }

    public long getAveDimension(){
        int sum = 0;
        for (int d : dimensions) sum += d;
        double ave = 1.0*sum/dimensions.size();

        return Math.round(ave);
    }

    public Double getSumUtility(){
        Double res = 0.0;
        PartialAssignmentMap pMap = new PartialAssignmentMap(this.dimensions, this.getIndexToUtil());
        pMap.trim();
        for (PartialAssignment p: pMap.getIndexToUtil().values()){
            if (!p.getUtil().equals(AssignmentProblem.RESTRICT_VALUE)){
                res += p.getUtil();
            }
        }

        if (res == null){
            res = AssignmentProblem.RESTRICT_VALUE;
        }

        return res;
    }

    public Double getMaxUtility(){
        Double max = Double.NEGATIVE_INFINITY;
        for (PartialAssignment p: partialAssignments()){
            if (p.getUtil() > max){
                max = p.getUtil();
            }
        }

        return max;
    }

    public List<Integer> getIndicesNotUsed(int dim){
        List<Integer> res = new ArrayList();

        int d = dimensions.get(dim);

        for (int i=0; i< d; i++){
            res.add(i);
        }

        for (PartialAssignment p: indexToUtil.values()){
            res.remove(p.getIndices().get(dim));
        }

        return res;
    }

    public void trim(){
        Iterator<PartialAssignment> iterator = indexToUtil.values().iterator();
        while (iterator.hasNext()){
            PartialAssignment p = iterator.next();
            if (p.getUtil().equals(AssignmentProblem.RESTRICT_VALUE)){
                iterator.remove();
            }
        }
    }

    @Override
    public String toString() {
        String str = null;
        for (PartialAssignment pa : indexToUtil.values()) {
            if (str == null) str = ""; else str += "; ";
            str += pa.toString();
        }
        return "arr = " + str;
    }

    public String printMap() {
        //p = new PartialAssignment(new Integer[]{0,11}, 1.2100220000000004); utilities.put(p.getIndices(), p);
        StringBuffer sb = new StringBuffer();
        for (PartialAssignment pa : indexToUtil.values()) {
            sb.append("p = new PartialAssignment(new Integer[]{" + pa.getIndices().get(0) + "," + pa.getIndices().get(1) + "}, " + pa.getUtil() + "); utilities.put(p.getIndices(), p);\n");
        }
        return sb.toString();
    }

    @Override
    public int compareTo(PartialAssignmentMap o) {   // -1 if o is greater than this
        if (o.getSumUtility() >  this.getSumUtility()) {
            return 1;
        } else  if (o.getSumUtility() <  this.getSumUtility()) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PartialAssignmentMap that = (PartialAssignmentMap) o;

        if (dimensions != null ? !dimensions.equals(that.dimensions) : that.dimensions != null) return false;

        if (!this.getSumUtility().equals(that.getSumUtility())){
            return false;
        } else {
            if (indexToUtil != null ? !this.indexToUtil.equals(that.getIndexToUtil()) : that.indexToUtil != null) return false;

        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = dimensions != null ? dimensions.hashCode() : 0;
        result = 31 * result + (indexToUtil != null ? indexToUtil.hashCode() : 0);
        return result;
    }
}
