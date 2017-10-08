package ru.programpark.entity.train;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 13.04.15
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for task and fact
 * Either task, either fact
 */
public class Artifact implements Serializable {
    private Long Id;
    private Long startTime;
    private Long duration;
    public List<Route> routes = new ArrayList<>();
    private Long weight;
    private Integer trainQuantity;
    public Integer routeIndex = 0;

    public Artifact() {
    }

    public Artifact(Long id, Long startTime, Long duration, List<Route> routes, Long weight, Integer trainQuantity) {
        Id = id;
        this.startTime = startTime;
        this.duration = duration;
        this.routes = routes;
        this.weight = weight;
        this.trainQuantity = trainQuantity;
    }

    public boolean checkRoutes(){
        if (routes == null || routes.size() == 0 || getMainRoute() == null)//случается если в маршруте только одна станция, например, такие факты игнорируем
            return false;
        return true;
    }

    public Artifact(Long id){
        Id = id;
    }

    public Integer getRouteIndex() {
        return routeIndex;
    }

    public void setRouteIndex(Integer routeIndex) {
        this.routeIndex = routeIndex;
    }

    public Route getMainRoute(){
        if (routes == null || routes.size() <= routeIndex || routes.size() == 0 || routes.get(0) == null)
            return null;
        else
            return routes.get(routeIndex);
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public void addRoute(Route r) {
        routes.add(r);
    }

    public void delRoute(Route r) {
        routes.remove(r);
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    public Integer getTrainQuantity() {
        return trainQuantity;
    }

    public void setTrainQuantity(Integer trainQuantity) {
        this.trainQuantity = trainQuantity;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Artifact");
        sb.append("{Id=").append(Id);
        sb.append(", startTime=").append(startTime);
        sb.append(", duration=").append(duration);
        sb.append(", routes=").append(routes);
        sb.append(", weight=").append(weight);
        sb.append(", trainQuantity=").append(trainQuantity);
        sb.append('}');
        return sb.toString();
    }
}
