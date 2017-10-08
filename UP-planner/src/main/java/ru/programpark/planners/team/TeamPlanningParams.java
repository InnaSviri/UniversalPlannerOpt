package ru.programpark.planners.team;

import ru.programpark.entity.data.InputData;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 31.07.15
 * Time: 13:21
 * To change this template use File | Settings | File Templates.
 */
public class TeamPlanningParams {
    private long param(InputData input, String key, long mult) {
        Number val = (key.indexOf("/") >= 0)
                          ? input.getConfigParam(key).longValue()
                          : input.getConfigParam("team", key);
        return (long) (val.doubleValue() * mult);
    }
    private long param(InputData input, String key) {
        return param(input, key, 1L);
    }
    private final static long H = 3600L;

    public long maxTimeLocoWaitsForTeam; // Наибольшее время перемещения бригады к началу движения
    public long minRest;           // Наименьшее время отдыха бригады (4.3.9 п. 3a)
    public long technologicalTimeForLocoReturnBeforeRest;    // Время необхолимое на сдачу локомотива до ухода на отдых
    public long initTimeToRest;    // Время до отдыха, выставляемое после прохождения предыдущего
    public long inactThreshold;    // Время, которое бригада может находиться без работы, прежде чем будет
    // отправлена на отдых (4.3.9 п. 2b)
    public long teamSchedulingRange;   // Горизонт бригадного планирования
    public static long defaultStopTimeForTeam;
    public boolean bulkPlanning;
    TeamPlanningCoeffs coeff;
    Integer defaultTrainPriority;

    public TeamPlanningParams(InputData input) {
        maxTimeLocoWaitsForTeam = param(input, "max_duration", H);
        minRest = param(input, "min_rest", H);
        inactThreshold = param(input, "inactivity_threshold", H);
        initTimeToRest = param(input, "init_time_to_rest", H) +
                             param(input, "allowance/team_rest_additional", H);
        teamSchedulingRange = param(input, "range/team_scheduling", H);
        defaultStopTimeForTeam = param(input, "default_stop_time", H);
        bulkPlanning = input.getConfigParam("bulk_planning").intValue() == 1;
        coeff = new TeamPlanningCoeffs(input);
        defaultTrainPriority = input.getConfigParam("train/default_priority").intValue();
        technologicalTimeForLocoReturnBeforeRest = 1800L;
    }
}
