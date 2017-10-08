package ru.programpark.planners.loco;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.loco.BaseLocoTrack.State;
import ru.programpark.entity.loco.LocoRegion;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.planners.common.*;

import java.util.Iterator;
import java.util.List;

public class CurrentRunners {

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(CurrentRunners.class);
        return logger;
    }

    private void CHECK_INTERRUPTED() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            LOGGER().warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    private boolean isServiceReachable(Loco runner, Train train,
                                       Integer startIndex, Integer endIndex) {
        LocoServiceEvent sEvt = runner.lastServiceEvent();

        if (sEvt != null && runner.allEvents().size() == 2 && runner.allEvents().get(0) instanceof LocoDepartEvent)
            return true;

        return sEvt != null &&
            sEvt.getTimeToService() >= train.duration(startIndex, endIndex) &&
            sEvt.getDistToService() >= train.distance(startIndex, endIndex);
    }

    private Integer reachableServiceIndex(Loco runner, Train train,
                                          Integer startIndex, Integer endIndex) {
        Integer indexServ = null, indexProcess = null, indexNorm = null,
            indexEnough = null;
        Loco.Event nEvt = runner.lastNonServiceEvent();
        LocoServiceEvent sEvt = runner.lastServiceEvent();
        if (! (nEvt instanceof LocoDepartEvent) || sEvt == null)
            return null;
        else if (nEvt instanceof LocoDepartEvent) {
            return endIndex; //случай когда локо уже выехал по факту с поездом на последний трек маршрута
        }

        while (--endIndex >= startIndex) {
            if (indexEnough == null) {
                if (isServiceReachable(runner, train, startIndex, endIndex)) {
                    indexEnough = endIndex;
                } else {
                    continue;
                }
            }
            Station st = train.getStationTo(endIndex);
            if (indexServ == null &&
                    st.getServiceAvailable()
                      .containsKey(sEvt.getServiceType())) {
                indexServ = endIndex;
                break;
            }
            if (indexProcess == null && st.getProcessTime() > 0L) {
                indexProcess = endIndex;
                if (indexServ != null) break;
            }
            if (indexNorm == null && st.getNormTime() > 0L) {
                indexNorm = endIndex;
                if (indexServ != null && indexProcess != null) break;
            }
        }
        return (indexServ != null) ? indexServ :
            (indexProcess != null) ? indexProcess :
            (indexNorm != null) ? indexNorm : indexEnough;
    }

    private boolean assignOneLoco(Loco runner, ReferenceEvent event,
                                  SchedulingFrame frame, Boolean preplanning) {
        List<LocoRegion> regs = runner.getAttributes().getLocoRegions();
        Train train = frame.data.getTrain(event.getTrainId());
        Integer startIndex = Math.max(event.getStartIndex(), 0),
            endIndex = LocoRegionPriority.endOfSlot(train, startIndex, regs);
        if (endIndex >= startIndex) {
            if (! isServiceReachable(runner, train, startIndex, endIndex)) {
                endIndex =
                    reachableServiceIndex(runner, train, startIndex, endIndex);
                if (endIndex == null) {
                    LOGGER().warn("Поезд " + train.getTrainId() + " не может" +
                                  " следовать с локомотивом " + runner.getId() +
                                  " по недостатку времени до ТО");
                    return false;
                } else {
                    LOGGER().warn("Назначение на поезд " + train.getTrainId() +
                                  " локомотива " + runner.getId() +
                                  " укорочено до станции " +
                                      train.getStationTo(endIndex).getId() +
                                  " по недостатку времени до ТО");
                }
            }
            train.updateAssign(startIndex, endIndex,
                               runner.getId(), State.WITH_TRAIN, null, null);
            if (preplanning)
                LocoPlanner.regions()
                           .estimativeShift(train, startIndex, endIndex);
            train.setUnassignedLocoIndex(endIndex + 1);
            train.setUnassignedTeamIndex(startIndex);
            LocoAssignEvent lEvent =
                new LocoAssignEvent(train.getTrainId(), startIndex, endIndex);
            runner.addEvent(lEvent);
            runner.updateService(lEvent);
            return true;
        } else {
            return false;
        }
    }

    void assign(SchedulingFrame frame, Boolean preplanning)
    throws InterruptedException {
        CHECK_INTERRUPTED();
        int nAssigned = 0;
        Iterator<Loco> itr = frame.data.getLocos().iterator();
        while (itr.hasNext()) {
            Loco loco = itr.next();
            CHECK_INTERRUPTED();
            ReferenceEvent evt =
                (ReferenceEvent) (loco.lastEvent(ReferenceEvent.class));
            //if (evt != null && assignOneLoco(loco, evt, frame, preplanning))
            //    ++nAssigned;
            boolean assigned = false;
            if (evt != null) {
                assigned = assignOneLoco(loco, evt, frame, preplanning);
                if (assigned)
                    ++nAssigned;
                else if (loco.allEvents().size() == 2 && loco.allEvents().get(0) instanceof LocoDepartEvent){
                    itr.remove();
                    String m = "Временное решение. Локомотив " + loco.getId() + " удаляется из планирования из-за " +
                            " несовпадения локомотивных плеч у локомотива и трека в маршруте поезда.";
                    LOGGER().warn(m);
                }
            }
        }

        for (Train train: frame.data.getTrains()){
            if (! train.getPreRoute().hasEvents() &&
                    train.getUnassignedLocoIndex() == 0 &&
                    ! (train.lastEvent(0) instanceof HintEvent)) {
                train.setUnassignedLocoIndex(train.getRoute().size());
            }
        }
        LOGGER().info(LoggingAssistant.countingForm(nAssigned,
                              "Привязан %d локомотив, следующий",
                              "Привязаны %d локомотива, следующие",
                              "Привязано %d локомотивов, следующих") +
                          (nAssigned == 1 ? " с поездом" : " с поездами"));
    }

}
