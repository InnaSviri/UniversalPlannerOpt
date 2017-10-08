package ru.programpark.planners.team;

import ru.programpark.entity.data.InputData;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 31.07.15
 * Time: 13:04
 * To change this template use File | Settings | File Templates.
 */
public class TeamPlanningCoeffs{
    InputData iData;
    double K1, K2, K3, K4, K5, K6, ACCOUNT_WAIT_TIME;
    private final static double POS = 1d;
    private final static double NEG = -1d;

    public TeamPlanningCoeffs(InputData iData) {
        this.iData = iData;
        K1 = coeffParam("K1_need_move_passenger", NEG);
        K2 = coeffParam("K2_loco_waiting_time", NEG);
        K3 = coeffParam("K3_team_waiting_time", POS);
        K4 = coeffParam("K4_work_time_left", NEG);
        K5 = coeffParam("K5_match_depot_direction", NEG);
        K6 = coeffParam("K6_team_after_rest", POS);
        ACCOUNT_WAIT_TIME = coeffParam("account_wait_time", POS);
    }

    public double coeffParam(String key, double signum) {
        double coeff = iData.getConfigParam("team/coeff", key).doubleValue();
        if (Math.signum(coeff) == signum) {
            return coeff;
        } else {
            throw new RuntimeException("Коэффициент " + key + " функции полезности назначения бригад задан" +
                    " с неправильным знаком");
        }
    }
}
