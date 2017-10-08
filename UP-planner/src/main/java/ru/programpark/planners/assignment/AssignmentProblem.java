package ru.programpark.planners.assignment;

import java.util.List;

/**
 * User: oracle
 * Date: 11.06.15
 * Новый интерфейс для решения задачи о назначениях
 */

public interface AssignmentProblem {
    //public static Double RESTRICT_VALUE = -1.7976931348623157E308D;
    public static Double RESTRICT_VALUE = -1000000000D;
    public void setParams(AssignmentParams params);
    public PartialAssignmentMap decision(PartialAssignmentMap utils);
    public List<PartialAssignmentMap> decisions(PartialAssignmentMap utils);
}
