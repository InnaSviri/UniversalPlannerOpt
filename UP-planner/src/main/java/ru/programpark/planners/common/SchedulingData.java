package ru.programpark.planners.common;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.data.OutputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.FactLoco;
import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.loco.RealLoco;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.team.RealTeam;
import ru.programpark.entity.train.GeneralizedTask;
import ru.programpark.entity.train.RealTrain;
import ru.programpark.entity.train.FactTrain;
import ru.programpark.entity.train.TrainReady;
import ru.programpark.entity.train.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class SchedulingData {
    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(SchedulingData.class);
        return logger;
    }

    private final Map<Long, Train> trains = new TreeMap<>();
    private final Map<Long, Loco> locos = new TreeMap<>();
    private final Map<Long, Team> teams = new TreeMap<>();

    public Collection<Train> getTrains() { return trains.values(); }
    public Train getTrain(Long id) { return trains.get(id); }

    public Collection<Loco> getLocos() {
        return locos.values();
    }
    public Loco getLoco(Long id) { return locos.get(id); }

    public Collection<Team> getTeams() { return teams.values(); }
    public Team getTeam(Long id) { return teams.get(id); }

    private void ensureFactTrain(GeneralizedTask gTask, Train train) {
        if (gTask instanceof FactTrain) return;
        // Добавляем фиктивный factTrain во входные данные, чтобы сверять
        // в тестах, все ли поезда запланированы и т.д.
        List<Link> linkList = new ArrayList<>();
        for (Train.EventContainer track: train.getRoute()) {
            linkList.add(track.getLink());
        }
        Route route= new Route(linkList);
        List<Route> routes = Collections.singletonList(route);
        Long trainId = train.getTrainId();
        TrainReady ready =
            new TrainReady(trainId, train.getStartTime(),
                           linkList.get(0).getFrom());
        FactTrain fictTrain =
            new FactTrain(trainId, 0L, 0L, 0L, routes, 0, ready);
        SchedulingData.getInputData().getFictitousFactTrainsFromTasks()
                      .put(trainId, fictTrain);
    }

    public Train newTrain(GeneralizedTask gTask) {
        return newTrain(gTask, 0L);
    }

    private Train newTrain(GeneralizedTask gTask, long idPrefix) {
        Train train = null;
        for (int i = 0; i < gTask.getTrainQuantity(); ++i) {
            train = new Train(gTask, i);
            long id = train.getTrainId();
            long m = (long) Math.pow(10d,
                                     Math.ceil(Math.log10(id + 1d)) -
                                         Math.ceil(Math.log10(idPrefix + 1d)));
            train.setTrainId(id % m + idPrefix * m);
            ensureFactTrain(gTask, train);
            if (train.hasEvents()) {
                trains.put(train.getTrainId(), train);
            } else {
                LOGGER().warn("Не удалось инициализировать поезд " +
                                  gTask.getId());
            }
        }
        return train;
    }

    public void putTrain(Train train) {
        trains.put(train.getTrainId(), train);
    }

    public void delTrain(Train train) {
        trains.remove(train.getTrainId());
    }

    private <T extends GeneralizedTask> void
    initTrains(Map<Long, T> gTasks, long idPrefix) {
        for (T gTask : gTasks.values()) newTrain(gTask, idPrefix);
    }

    public Loco newLoco(FactLoco fLoco) {
        Long id = fLoco.getId();
        Loco loco = new Loco(fLoco);
        if (loco.hasEvents()) {
            locos.put(id, loco);
        } else {
            LOGGER().warn("Не удалось инициализировать локомотив " + id);
        }
        return loco;
    }

    public void putLoco(Loco loco) {
        locos.put(loco.getId(), loco);
    }

    private void initLocos(Map<Long, FactLoco> fLocos) {
        for (FactLoco fLoco : fLocos.values()) {
            newLoco(fLoco);
        }
    }

    public Team newTeam(FactTeam fTeam) {
        Long id = fTeam.getId();
        Team team = new Team(fTeam);
        if (team.hasEvents()) {
            teams.put(id, team);
        } else {
            LOGGER().warn("Не удалось инициализировать бригаду " + id);
        }
        return team;
    }
    
    private void initTeams(Map<Long, FactTeam> fTeams) {
        for (FactTeam fTeam : fTeams.values()) {
            newTeam(fTeam);
        }
    }

    public OutputData dump() {
        OutputData oData = new OutputData();
        for (Train train : getTrains()) {
            RealTrain rTrain = train.toRealTrain();
            if (rTrain != null) oData.addSlotTrain(rTrain);
        }
        for (Loco loco : getLocos()) {
            RealLoco rLoco = loco.toRealLoco();
            if (rLoco != null) oData.addSlotLoco(rLoco);
        }
        for (Team team : getTeams()) {
            RealTeam rTeam = team.toRealTeam();
            if (rTeam != null) oData.addSlotTeam(rTeam);
        }
        return oData;
    }

    @Getter @Setter static SchedulingFrame currentFrame;
    @Getter @Setter static SchedulingData frameData;
    @Getter @Setter static InputData inputData;

    public static SchedulingData init(InputData iData) {
        SchedulingData data = new SchedulingData();
        boolean bulkPlanning =
            iData.getConfigParam("bulk_planning").intValue() > 0;
        long taskPrefix =
            iData.getConfigParam("train/task_prefix").longValue();
        setInputData(iData);
        setFrameData(data);
        data.initTrains(iData.getFactTrains(), 0L);
        if (bulkPlanning) data.initTrains(iData.getTasks(), taskPrefix);
        data.initLocos(iData.getFactLocos());
        data.initTeams(iData.getFactTeams());
        return data;
    }

    public static SchedulingData fini() {
        setCurrentFrame(null);
        setInputData(null);
        setFrameData(null);
        return null;
    }

    public static Station getStation(Long id) {
        return inputData.getStations().get(id);
    }
    public static Link getLink(StationPair stp) {
        return inputData.getLinks().get(stp);
    }
    public static LocoRegion getLocoRegion(Long id) {
        return inputData.getLocoRegions().get(id);
    }
}
