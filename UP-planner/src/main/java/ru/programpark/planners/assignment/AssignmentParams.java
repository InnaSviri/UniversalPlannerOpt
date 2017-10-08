package ru.programpark.planners.assignment;

import ru.programpark.entity.data.InputData;

public class AssignmentParams {
    double auction_ɛ;
    double auction_λ;
    double annealing_R;
    double annealing_T;
    double annealing_T_threshold;
    double annealing_N;
    double annealing_accept;

    double optimization_accuracyOfDomainSearch;

    public AssignmentParams() {
    }

    public AssignmentParams(InputData iData) {
        AssignmentParams dflt;

        dflt = AuctionAssignment.DEFAULT_PARAMS;
        auction_ɛ =
            coeffParam(iData, "auction_forward/ɛ", dflt.auction_ɛ);
        auction_λ =
            coeffParam(iData, "auction_forward/λ", dflt.auction_λ);

        dflt = AnnealingAssignment.DEFAULT_PARAMS;
        annealing_R =
            coeffParam(iData, "annealing/R", dflt.annealing_R);
        annealing_T =
            coeffParam(iData, "annealing/T", dflt.annealing_T);
        annealing_T_threshold =
            coeffParam(iData, "annealing/T_threshold", dflt.annealing_T_threshold);
        annealing_N =
            coeffParam(iData, "annealing/N", dflt.annealing_N);
        annealing_accept =
            coeffParam(iData, "annealing/accept", dflt.annealing_accept);

        dflt = OptimizationAssignment.DEFAULT_PARAMS;
        optimization_accuracyOfDomainSearch =
                coeffParam(iData, "optimization/accuracyOfDomainSearch", dflt.optimization_accuracyOfDomainSearch);
    }

    private double coeffParam(InputData iData, String key, double dflt) {
        double coeff;
        try {
            coeff = iData.getConfigParam(key).doubleValue();
        } catch (RuntimeException e) {
            coeff = dflt;
        }
        if (Math.signum(coeff) == Math.signum(dflt)) {
            return coeff;
        } else {
            throw new RuntimeException("Коэффициент " + key +
                                       " задан с неправильным знаком");
        }
    }
}
