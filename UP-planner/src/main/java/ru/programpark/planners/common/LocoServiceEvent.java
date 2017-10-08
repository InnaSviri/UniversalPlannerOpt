package ru.programpark.planners.common;

import lombok.Getter;

public class LocoServiceEvent extends StationEvent implements Loco.Event {
    @Getter private Long duration;
    private Long timeToService;
    @Getter private Long distToService;
    @Getter private Long serviceType;
    @Getter private Loco.Event priorEvent;

    public LocoServiceEvent(Integer runIndex,
                            Integer locoFrameIndex, Integer teamFrameIndex,
                            Long stationId, Long duration,
                            Long timeToService, Long distToService,
                            Long serviceType, Loco.Event priorEvent) {
        super(runIndex, locoFrameIndex, teamFrameIndex, stationId,
              priorEvent.getEventTime());
        this.duration = duration;
        this.timeToService = timeToService;
        this.distToService = distToService;
        this.serviceType = serviceType;
        this.priorEvent = priorEvent;
    }

    public LocoServiceEvent(Long stationId, Long duration,
                            Long timeToService, Long distToService,
                            Long serviceType, Loco.Event priorEvent) {
        this(SchedulingData.getCurrentFrame().runIndex,
             SchedulingData.getCurrentFrame().locoFrameIndex,
             SchedulingData.getCurrentFrame().teamFrameIndex,
             stationId, duration, timeToService, distToService,
             serviceType, priorEvent);
    }

    @Override public Long getLocoReadyTime() {
        if (duration > 0) {
            return priorEvent.getLocoReserveReadyTime() + duration +
                getStation().getProcessTime() / 2;
        } else {
            return priorEvent.getLocoReadyTime();
        }
    }

    @Override public Long getLocoReserveReadyTime() {
        if (duration > 0) {
            return priorEvent.getLocoReserveReadyTime() + duration;
        } else {
            return priorEvent.getLocoReserveReadyTime();
        }
    }

    public Long getTimeToService() {
        // В поле time записывается время совершения события priorEvent, как
        // оно вычислялось при создании объекта.  Если впоследствии по
        // ссылке priorEvent произошёл сдвиг, timeToService уменьшается на
        // его значение.
        return timeToService - (priorEvent.getEventTime() - getTime());
    }

    public Long getStartTime() {
        return priorEvent.getLocoReserveReadyTime();
    }

    public Long getEndTime() {
        return priorEvent.getLocoReserveReadyTime() + duration;
    }

    @Override public String toString() {
        return toString(null);
    }

    protected String toString(String subFields) {
        String dur = (duration > 0L) ? (duration.toString() + ", ") : "",
            type = "type " + serviceType + ", ",
            rel = (priorEvent == null) ? "" : ("rel. to " + priorEvent + ", "),
            tts = "TTS=" + timeToService + ", ",
            dts = "DTS=" + distToService,
            sub = (subFields == null) ? "" : (", " + subFields);
        return super.toString(dur + type + tts + dts + sub);
    }
}
