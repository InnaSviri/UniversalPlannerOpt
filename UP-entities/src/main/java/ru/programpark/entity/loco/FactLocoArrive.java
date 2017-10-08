package ru.programpark.entity.loco;

import ru.programpark.entity.train.TrainState;

public class FactLocoArrive extends TrainState {
    private Long id;
    private Long time;
    private BaseLocoTrack.State locoState;

    public FactLocoArrive() {
        super(0L, System.currentTimeMillis() / 1000L);
    }

    public FactLocoArrive(Long id, Long time, BaseLocoTrack.State locoState) {
        this.id = id;
        this.time = time;
        this.locoState = locoState;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public BaseLocoTrack.State getLocoState() {
        return locoState;
    }

    public void setLocoState(BaseLocoTrack.State locoState) {
        this.locoState = locoState;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FactLocoArrive");
        sb.append("{id=").append(id);
        sb.append(", time=").append(time);
        sb.append(", locoState=").append(locoState);
        sb.append('}');
        return sb.toString();
    }
}
