package ru.programpark.entity.train;

import java.io.Serializable;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 13.04.15
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */

public class Task extends Artifact implements Serializable, GeneralizedTask {

    public Task(Long id, Long startTime, Long duration, List<Route> routes, Long weight, Integer trainQuantity) {
        super(id, startTime, duration, routes, weight, trainQuantity);
    }

    public Task(){
        super();
    }

    @Override
    public int hashCode() {
        Long id = getId();
        return (id == null) ? 0 : id.hashCode();
    }
}
