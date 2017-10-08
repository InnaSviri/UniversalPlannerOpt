package ru.programpark.planners.assignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mipt.optimization.algorithms.Algorithm;
import ru.mipt.optimization.algorithms.GradientDescent;
import ru.mipt.optimization.entity.Optimizator;
import ru.mipt.optimization.entity.inOut.Config;
import ru.mipt.optimization.entity.typeWrapper.TypeWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by Inna on 08.10.2017.
 */
public class OptimizationAssignment implements AssignmentProblem {

    private static Logger logger = null;

    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(AnnealingAssignment.class);
        return logger;
    }

    public static AssignmentParams DEFAULT_PARAMS = new AssignmentParams() {{
        optimization_accuracyOfDomainSearch = 0.1;
    }};

    double accuracyOfDomainSearch;

    List<Config> config = new ArrayList<>(); // list of different configurations to try
    Function<Integer, Double> toReal; // rule to convert indices to real numbers for differentiating argument of the cost func
    Function<Double, Integer> toIndices; // rule to convert reals back to indices


    {
        setParams(DEFAULT_PARAMS);
    }

    public OptimizationAssignment(List<Config> config,
                                  Function<Integer, Double> toReal, Function<Double, Integer> toIndices) {
        if (config == null || config.isEmpty()) this.config.add(new Config());
        else this.config = config;
        this.toReal = toReal;
        this.toIndices = toIndices;

    }


    @Override
    public void setParams(AssignmentParams params) {
        accuracyOfDomainSearch = params.optimization_accuracyOfDomainSearch;
    }

    @Override
    public PartialAssignmentMap decision(PartialAssignmentMap utils) {
        List<PartialAssignmentMap> res = decisions(utils);
        return res.isEmpty() ? null : res.get(0);
    }

    @Override
    public List<PartialAssignmentMap> decisions(PartialAssignmentMap utils) {
        //Optimizator<Integer> optimizator = new Optimizator<>(utils.)
        return null;
    }
}
