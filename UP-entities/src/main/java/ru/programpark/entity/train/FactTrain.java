package ru.programpark.entity.train;

import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;

import java.util.*;

//import ru.programpark.planners.util.LoggingAssistant;


/**
 * User: nina
 * Date: 10.06.14
 * Факт о прибытии, отправке или готовности к отправке поезда
 */

//+train_fact(id(Id), category(TrainCat),
//        weight(Tonnage), length(Length),
//        route([station(Id1), station(Id2),…]))
//Id – уникальный идентификатор поезда.
//TrainCat – категория поезда.
//Tonnage – вес поезда (в тоннах).
//Length – длина поезда (в условных вагонах).
//Id1, Id2,… – идентификаторы станций. В атрибуте route должны быть перечислены все станции, составляющие маршрут поезда, в нужном порядке.

public class FactTrain implements Cloneable, GeneralizedTask, TrainAttributes {
    private Long id;//real train id
    private boolean isDuplicateTrainInfo = false; // было ли дублирование
    private boolean isSetTrainInfo = false; // получено ли сообщение
    private Long trainNum = -1L;
    private Long category = -1L;
    private Long weight = -1L;
    private Long length = -1L;
    public List<Route> routes = new ArrayList();
    private int routeIndex = 0;
    private TrainState trainState;
    private Integer priority;

    public FactTrain() {
    }

    public FactTrain(Long id) {
        this.id = id;
    }

    public FactTrain(Long id, Long category, Long weight, Long length, List<Route> routes, int routeIndex, TrainState trainState) {
        this.id = id;
        this.category = category;
        this.weight = weight;
        this.length = length;
        this.routes = routes;
        this.routeIndex = routeIndex;
        this.trainState = trainState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FactTrain factTrain = (FactTrain) o;

        return id.equals(factTrain.id);

    }

    @Override
    public int hashCode() {
        return (id == null) ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FactTrain{");
        sb.append("id=").append(id);
        sb.append(", trainNum=").append(trainNum);
        sb.append(", category=").append(category);
        sb.append(", weight=").append(weight);
        sb.append(", length=").append(length);
        sb.append(", routeIndex=").append(routeIndex);
        sb.append(", trainState=").append(trainState);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Object clone() {
        FactTrain copy = new FactTrain();
        copy.id = this.id;
        copy.trainNum = this.trainNum;
        copy.category = this.category;
        copy.weight = this.weight;
        copy.length = this.length;
        copy.routes = this.routes;
        copy.routeIndex = this.routeIndex;
        copy.trainState = this.trainState;
        return (Object) copy;
    }

    public Long getCategory() {
        return category;
    }

    public void setCategory(Long category) {
        this.category = category;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getCatId() {
        return category;
    }
    public void setCatId(Long category) {
        this.category = category;
    }
    public Long getWeight() {
        return weight;
    }
    public void setWeight(Long weight) {
        this.weight = weight;
    }
    public Long getLength() {
        return length;
    }
    public void setLength(Long length) {
        this.length = length;
    }
    public Route getMainRoute() {
        if (routes != null && routeIndex >= 0 && routeIndex < routes.size())
            return routes.get(routeIndex);
        else
            return null;
    }

    public void markTrainInfoSet() {
        if(isSetTrainInfo)
            isDuplicateTrainInfo = true;
        else
            isSetTrainInfo = true;
    }

    public int checkCorrectTrainInfo() {
        if (isDuplicateTrainInfo) return -1;
        if (!isSetTrainInfo) return -2;
        return 0;
    }


    public List<Route> getRoutes() {
        return routes;
    }

    public void addRoute(Route route){
        routes.add(route);
    }

    public TrainState getTrainState() {
        return trainState;
    }

    public void setTrainState(TrainState trainState) {
        this.trainState = trainState;
    }

    public boolean isEmpty(Route route) {
        return (route == null || route.getLinkList().isEmpty());
    }

    public boolean allEmptyRoutes() {
        if (routes == null || routes.size() == 0) return true;
        while (routeIndex < routes.size()) {
            Route rte = routes.get(routeIndex);
            if (isEmpty(rte)) ++routeIndex;
            else return false;
        }
        return true;
    }

    private boolean isCircular(Route route) {
        Set<Station> traversed = new HashSet<>();
        Station prevTo = null;
        for (Link link : route.getLinkList()) {
            Station from = link.getFrom(), to = link.getTo();
            if ((! from.equals(prevTo) && traversed.contains(from)) ||
                    traversed.contains(to) ||
                    from.equals(to)) {
                return true;
            } else {
                traversed.add(from);
                traversed.add(to);
                prevTo = to;
            }
        }
        return false;
    }

    public boolean circularRoutes() {
        if (routes == null || routes.size() == 0) return true;
        while (routeIndex < routes.size()) {
            Route rte = routes.get(routeIndex);
            if (isEmpty(rte) || isCircular(rte)) ++routeIndex;
            else return false;
        }
        return true;
    }

    public boolean checkRoutesContainsLink(Link link) {
        Iterator<Route> it = routes.iterator();
        while (it.hasNext()) {
            Route r = it.next();
            boolean deleteRoute = !checkRouteContainsLink(r, link);
            if (deleteRoute) {
                it.remove();
            }
        }

        return !allEmptyRoutes();
    }

    public boolean checkRouteContainsLink(Route r, Link link){
        if (r.containsLink(link)){
            return true;
        }

        return false;
    }

    public boolean checkRoutesContainsStation(Station s){
        Iterator<Route> it = routes.iterator();
        while(it.hasNext()){
            Route r = it.next();
            boolean deleteRoute = !checkRouteContainsStation(r, s);
            if (deleteRoute){
                it.remove();
            }
        }

        return !allEmptyRoutes();
    }

    public boolean checkRouteContainsStation(Route r, Station s){
        if (r.containsStation(s)){
            return true;
        }
        return false;
    }

    public boolean checkLastStationInRoute(Route r, Station s){
        //удаляем поезда уже приехавшие на станцию назначения, а сообщение train_ready приходит
        int routeSize = r.getLinkList().size();
        Station lastStation = r.getLinkList().get(routeSize - 1).getTo();

        if (s.equals(lastStation)){
            return true; //плохой маршрут - удаляем его из списка машрутов
        } else {
            return false;
        }
    }

    public boolean checkLastStationInRoutes(Station s){
        //удаляем поезда уже приехавшие на станцию назначения, а сообщение train_ready приходит
        Iterator<Route> it = routes.iterator();
        while(it.hasNext()){
            Route r = it.next();
            boolean deleteRoute = checkLastStationInRoute(r, s);
            if (deleteRoute){
                it.remove();
            }
        }

        return !allEmptyRoutes();
    }

    public boolean checkTime(Long currentTime, Link link){
        if (trainState instanceof TrainArrive){
            // Планировать только те поезда, у которых время прибытия меньше текущего.
            if (trainState.getTime() > currentTime)//иначе - отсеиваем
                return false;
        } else if (trainState instanceof TrainDepart){
            //  Условие отсеивания: currentTime - factTime >  2*link.duration() или factTime < currentTime - 2*link.duration()
            Long thresholdTime = currentTime - 2*link.getDefaultDuration();
            //Вычитать из текущего времени это нормативное время хода (link.duration).
            // Получать таким образом некое "граничное время корректных фактов".
            // Eсли время отправления, указанное в train_depart, находится в интервале между этим граничным и
            // текущим временами, то факт считать корректным и планировать поезд.
            if (trainState.getTime() < thresholdTime || trainState.getTime() > currentTime ) {
                // Если время отправления вне этого интервала, считать факт некорректным и не планировать поезд.
                return false;
            }
            //READY со временм в будущем не надо отсеивать: Поезд надо просто планировать не раньше этого времени
        }

        return true;
    }

    public String checkForExclusion(Map<StationPair, Link> links){
        try{
            Station s = null;
            Link link = null;
            if (trainState instanceof TrainReady)
                s = ((TrainReady) trainState).getStation();
            if (trainState instanceof TrainArrive) {
                link = ((TrainArrive) trainState).getLink();
                s = link.getTo();
            }
            if (trainState instanceof TrainDepart) {
                link = ((TrainDepart) trainState).getLink();
                Link actualLink = links.get(new StationPair(link.getFrom().getId(), link.getTo().getId()));
                if (actualLink == null || actualLink.getDefaultDuration().equals(0L) ||
                        actualLink.getDistance().equals(0L))
                    return "в маршруте поезда обнаружен несуществующий перегон, для него не было сообщения +link";
            }

            if (trainState instanceof TrainArrive && link.getTo().equals(getMainRoute().getLinkList().get(getMainRoute().getLinkList().size() - 1).getTo()))
                return "прибыл на конечную станцию маршрута";

            if (link == null && !(trainState instanceof TrainReady)) {
                return "находится не на перегоне, но объявлен прибывшим либо отправленным";
            } else if (s == null && trainState instanceof TrainReady) {
                return "находится не на станции, но объявлен готовым к движению";
            } else if ((trainState instanceof TrainArrive || trainState instanceof TrainReady) && !checkRoutesContainsStation(s)) {
                return "прибыл или стоит на станции, которой нет в маршруте";
            } else if (trainState instanceof TrainDepart && !checkRoutesContainsLink(link)) {
                return "уехал на перегон, которого нет в маршруте";
            /*} else if (!checkTime(currentTime, link)) {
                return "либо в будущем, либо слишком в прошлом"; */
            } else if (trainState instanceof TrainReady && !checkLastStationInRoutes(s)) {
                return "объявлен готовым к движению, хотя прибыл на последнюю станцию своего маршрута";
            } else if (allEmptyRoutes()) {
                return "пустой маршрут";
            } else if (circularRoutes()) {
                return "циклический маршрут";
            }
        } catch (Exception e){
            e.printStackTrace();
            return "ошибка при проверке";
        }
        return null;
    }

    public void trimRoutes(){//delete everything in all routes before station firstStation
        Station firstStation = null;
        Station s = null;
        Link link = null;
        if (trainState instanceof TrainReady)
            s = ((TrainReady) trainState).getStation();
        if (trainState instanceof TrainArrive)
            link = ((TrainArrive) trainState).getLink();
        if (trainState instanceof TrainDepart)
            link = ((TrainDepart) trainState).getLink();

        if (trainState instanceof TrainReady){
                firstStation = s;
        } else if (trainState instanceof TrainArrive){
                firstStation = link.getTo();
        } else if (trainState instanceof TrainDepart){
                firstStation = link.getFrom();
        }

        Iterator<Route> routeIterator = routes.iterator();
        while (routeIterator.hasNext()) {
                Route route = routeIterator.next();
                Iterator<Link> iter = route.getLinkList().iterator();
                while (iter.hasNext()){
                    Link track = iter.next();
                    if (!track.getFrom().equals(firstStation))
                        iter.remove();
                    else
                        break;
                }
                if (route.getLinkList().size() == 0) {
                    routeIterator.remove();
                }
        }
    }

    public Long getTrainNum() {
        return trainNum;
    }

    public void setTrainNum(Long trainNum) {
        this.trainNum = trainNum;
    }

    public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Integer getTrainQuantity() {
        return 1;
    }

    public Long getFirstStationId(){
        return trainState.getId();
    }
}
