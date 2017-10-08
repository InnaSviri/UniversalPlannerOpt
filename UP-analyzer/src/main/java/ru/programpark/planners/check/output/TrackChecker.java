package ru.programpark.planners.check.output;

import ru.programpark.entity.data.InputData;
import ru.programpark.entity.raw_entities.*;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.team.BaseTeamTrack;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.Time;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TrackChecker {
    private static PrintWriter writer = null;

    private static void log(String message) {
        if (writer == null)
            writer = LoggingAssistant.getResultsTestDetailsWriter();
        writer.println(message);
        //System.out.println("@@@" + message);
    }

    public static boolean checkCorrectTimeOnTrack_3_1_1(Collection<SlotTrain> trains, Collection<SlotLoco> locos, Collection<SlotTeam> teams) { //3.1.1
        /*
        Для всех поездов, локомотивов и бригад время прибытия на конечную станцию каждого трека больше времени отправления.
        В случае ошибки выводить тип сущности (поезд, локомотив, бригада), его id и участок маршрута, на котором произошла ошибка. Если для одной сущности случается несколько ошибок (на нескольких участках), то выводить все участки.
         */
        boolean pass = true;
        String testName = "Тест 3.1.1 (Для всех поездов, локомотивов и бригад время прибытия на конечную станцию каждого трека больше времени отправления)";

        for (SlotTrain train : trains) {
            if (!correctTimeOnTrack(train, train.route, pass, testName)) {
                pass = false;
            }
        }
        for (SlotLoco loco : locos) {
            if (!correctTimeOnTrack(loco, loco.route, pass, testName)) {
                pass = false;
            }
        }
        for (SlotTeam team : teams) {
            if (!correctTimeOnTrack(team, team.route, pass, testName)) {
                pass = false;
            }
        }
        return pass;
    }
    private static boolean correctTimeOnTrack(BaseSlot slot, List<? extends BaseTrack> tracks, boolean pass, String testName) {
        boolean result = true;

        for (BaseTrack track : tracks) {
            if (track.timeEnd <= track.timeStart) {
                result = false;
                if (slot instanceof SlotTrain) {
                    if (pass)
                        log(testName + "не пройден");
                    log("Поезда " + slot.id + " на перегоне " + track.stationFromId + " - " + track.stationToId + " уехал в " +
                            new Time(track.timeStart).getTimeStamp() + ", а приехал в " + new Time(track.timeEnd).getTimeStamp());
                }
                if (slot instanceof SlotLoco) {
                    if (pass)
                        log(testName + "не пройден");
                    log("Локомотив " + slot.id + " на перегоне " + track.stationFromId + " - " + track.stationToId + " уехал в " +
                            new Time(track.timeStart).getTimeStamp() + ", а приехал в " + new Time(track.timeEnd).getTimeStamp());
                }
                if (slot instanceof SlotTeam) {
                    if (((SlotTeam.Track) track).state == (BaseTeamTrack.State.AT_WORK.ordinal())) {
                        if (pass)
                            log(testName + "не пройден");
                        log("Бригада " + slot.id + " на перегоне " + track.stationFromId + " - " + track.stationToId + " уехала в " +
                                new Time(track.timeStart).getTimeStamp() + ", а приехала в " + new Time(track.timeEnd).getTimeStamp());
                    } else {
                        result = true;
                    }
                }

            }
        }

        return result;
    }

    public static boolean checkCorrectTimeOnStay_3_1_2(Collection<SlotTrain> trains, Collection<SlotLoco> locos, Collection<SlotTeam> teams) { //3.1.2
        boolean pass = true;
        String testName = "Тест 3.1.2 (Отсутствие скачков во времени на стоянках)";

        for (SlotTrain train : trains) {
            if (!correctTimeOnStay(train, train.route, pass, testName)) {
                pass = false;
            }
        }
        for (SlotLoco loco : locos) {
            if (!correctTimeOnStay(loco,  loco.route, pass, testName)) {
                pass = false;
            }
        }
        for (SlotTeam team : teams) {
            if (!correctTimeOnStay(team, team.route, pass, testName)) {
                pass = false;
            }
        }

        return pass;
    }

    private static boolean correctTimeOnStay(BaseSlot slot, List<? extends BaseTrack> tracks, boolean pass, String testName) {
        boolean result = true;
        BaseTrack priorTrack = null;

        for (BaseTrack track : tracks) {
            if (priorTrack != null) {
                if (priorTrack.timeEnd > track.timeStart) {
                    result = false;
                    if (slot instanceof SlotTrain) {
                        if (pass)
                            log(testName + "не пройден");
                        pass = false;
                        log("Поезд " + slot.id + " приехал на станцию " + priorTrack.stationToId + " в " + new Time(priorTrack.timeEnd).getTimeStamp()+
                                ", a  уехал в " + new Time(track.timeStart).getTimeStamp());
                    }
                    if (slot instanceof SlotLoco) {
                        if (pass)
                            log(testName + "не пройден");
                        pass = false;
                        log("Локомотив " + slot.id + " приехал на станцию " + priorTrack.stationToId + " в " + new Time(priorTrack.timeEnd).getTimeStamp()+
                                ", a  уехал в " + new Time(track.timeStart).getTimeStamp());
                    }
                    if (slot instanceof SlotTeam) {
                        if (pass)
                            log(testName + "не пройден");
                        pass = false;
                        log("Бригада " + slot.id + " приехала на станцию " + priorTrack.stationToId + " в " + new Time(priorTrack.timeEnd).getTimeStamp()+
                                ", a  уехала в " + new Time(track.timeStart).getTimeStamp());
                    }
                }
            }
            priorTrack = track;
        }

        return result;
    }

    public static boolean checkEqualsTimeInTrainAndLoco_3_1_3(Map<Long, SlotTrain> trains, Collection<SlotLoco> locos) { //3.1.3   Совпадение времен у поездов и локомотивов
        boolean pass = true;
        String testName = "Тест 3.1.3 (Совпадение времен у поездов и локомотивов)";

        for (SlotLoco loco : locos) {
            for (SlotLoco.Track track : loco.route) {
                SlotTrain train = trains.get(track.trainId);
                if (train == null)
                    continue;

                if (!checkEqualsTimeTrain(train, track, train.route, null, pass, testName)) {
                    pass = false;
                }
            }
        }
        return pass;
    }

    public static SlotTeam getSlotTeamLocoTrack(SlotTrain train, SlotLoco.Track track, SlotLoco loco, Collection<SlotTeam> teams){
        for (SlotTeam team : teams) {
            for (SlotTeam.Track teamTrack : team.route) {
                try {
                    if (track.stationFromId.equals(teamTrack.stationFromId) && track.stationToId.equals(teamTrack.stationToId) && teamTrack.locoId.equals(loco.id) && track.trainId.equals(train.id)) {
                        return team;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LoggingAssistant.logException(e);
                }
            }
        }

        return null;
    }


    public static boolean checkEqualsTimeInLocoAndTeam_3_1_4(InputData iData, Map<Long, SlotLoco> locos, Collection<SlotTeam> teams) {//3.1.4 (Совпадение времен у локомотивов и бригад)
        boolean pass = true;
        String testName = "Тест 3.1.4 (Совпадение времен у локомотивов и бригад)";

        for (SlotTeam team: teams) {
            for (SlotTeam.Track track : team.route) {
                if (!track.state.equals(BaseTeamTrack.State.PASSENGER.ordinal()) && !track.state.equals(BaseTeamTrack.State.AT_WORK.ordinal())) {
                //сверяем времена только у бригад в работе или пересылкой пассажиром
                continue;
                }
                SlotLoco loco = locos.get(track.locoId);
                if (loco == null) {
                    if (track.locoId != -1L) { // бригада следует пассажиром на пасс нитке
                        if (pass) {
                            log(testName + " не пройден");
                        }
                        pass = false;
                        log("Бригада " + team.id + " на участке " + track.stationFromId + " - " + track.stationToId + " ссылается на локо " + track.locoId +
                                ", для которого нет slotLoco");
                    } else {
                        Slot slot = iData.getSlots_pass().get(track.slotId);
                        if (slot == null) {
                            if (pass) {
                                log(testName + " не пройден");
                            }
                            pass = false;
                            log("Бригада, " + team.id + " на участке " + track.stationFromId + " - " + track.stationToId + " ссылается на  пасс. нитку " + track.slotId +
                                    ", для которого нет slotPass");
                        } else {
                            if (!slot.containsTeamTrack(track)){
                                if (pass) {
                                    log(testName + " не пройден");
                                }
                                pass = false;
                                log("Бригада, " + team.id + " на участке " + track.stationFromId + " - " + track.stationToId + " ссылается на пасс. нитку " + track.slotId +
                                        ", но нитка не содержит такого участка");
                            }
                        }
                    }
                } else {
                    if (!loco.containsTeamTrack(track)) {
                        if (pass){
                            log(testName + " не пройден");
                        }
                        pass = false;
                        log("Бригадный участок " + track.stationFromId + " - " + track.stationToId + " в " + new Time(track.timeStart).getTimeStamp() + " - " +
                                new Time(track.timeEnd).getTimeStamp() + " не находится в локо " + loco.id);
                    }
                }
            }
        }
        return pass;
    }

    private static boolean checkEqualsTimeTeam(Long locoId, BaseTrack locoTrack, List<SlotTeam.Track> teamRoute, String location, boolean pass, String testName) {
        boolean result = true;

        for (SlotTeam.Track teamTrack : teamRoute) {
            boolean trackFound = teamTrack.stationFromId.equals(locoTrack.stationFromId) && teamTrack.stationToId.equals(locoTrack.stationToId) && teamTrack.locoId.equals(locoId);
            if (trackFound) {
                if (!teamTrack.timeStart.equals(locoTrack.timeStart) || !teamTrack.timeEnd.equals(locoTrack.timeEnd)) {
                    result = false;
                    if (location != null) {
                        if (pass)
                            log(testName + " не пройден");
                        log("Разные времена хода " + location + ": времена бригады " + new Time(locoTrack.timeStart).getTimeStamp() + " - " + new Time(locoTrack.timeEnd).getTimeStamp() +
                                ", времена локомотива " + new Time(teamTrack.timeStart).getTimeStamp() + " - " + new Time(teamTrack.timeEnd).getTimeStamp());
                        break;
                    }
                }
            }
        }

        return result;
    }

    private static boolean checkEqualsTimeTrain(SlotTrain train, SlotLoco.Track locoTrack, List<SlotTrain.Track> trainRoute, String location, boolean pass, String testName) {
        boolean result = true;

        for (SlotTrain.Track trainTrack : trainRoute) {
            boolean trackFound = trainTrack.stationFromId.equals(locoTrack.stationFromId) && trainTrack.stationToId.equals(locoTrack.stationToId) && locoTrack.trainId.equals(train.id);
            if (trackFound) {
                if (!trainTrack.timeStart.equals(locoTrack.timeStart) || !trainTrack.timeEnd.equals(locoTrack.timeEnd)) {
                    result = false;
                    if (location != null) {
                        if (pass)
                            log(testName + " не пройден");
                        log("Разные времена хода " + location + ": времена бригады " + new Time(locoTrack.timeStart).getTimeStamp() + "-" + new Time(locoTrack.timeEnd).getTimeStamp() +
                                ", a времена локомотива " + new Time(trainTrack.timeStart).getTimeStamp() + "-" + new Time(trainTrack.timeEnd).getTimeStamp());
                        break;
                    }
                }
            }
        }

        return result;
    }
}
