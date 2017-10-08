package ru.programpark.planners.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 15.06.15
 * Time: 10:48
 * To change this template use File | Settings | File Templates.
 */
public class PartialAssignment {
        private List<Integer> indices = new ArrayList<>();
        private Double util;

    public PartialAssignment(Integer[] indices, Double util) {
        this.indices = new ArrayList(Arrays.asList(indices));
        this.util = util;
    }

    public PartialAssignment(List<Integer> indices, Double util) {
        for (Integer k: indices){
            this.getIndices().add(k);
        }
        this.util = util;
    }

    public PartialAssignment(Integer[] indices) {
        this.indices = new ArrayList(Arrays.asList(indices));
    }

    public PartialAssignment(List<Integer> indices) {
        for (Integer k: indices){
            this.getIndices().add(k);
        }
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public Double getUtil() {
        return util;
    }

    public void setUtil(Double util) {
        this.util = util;
    }

    public boolean setOfIndicesIsTheSame(Integer[] ind){
        List<Integer> a1 = new ArrayList(Arrays.asList(indices));
        List<Integer> a2 = new ArrayList(Arrays.asList(ind));

        return a1.equals(a2);
    }

    @Override
    public String toString() {
        String str = String.format("%.6f", util);
        for (int i = 0; i < indices.size(); ++i) {
            str += (i == 0 ? "@" : ",") + indices.get(i);
        }
        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PartialAssignment that = (PartialAssignment) o;

        if (!(this.indices).equals(((PartialAssignment) o).getIndices())) return false;
        //if (util != null ? !util.equals(that.util) : that.util != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = indices != null ? indices.hashCode() : 0;
        return result;
    }
}
