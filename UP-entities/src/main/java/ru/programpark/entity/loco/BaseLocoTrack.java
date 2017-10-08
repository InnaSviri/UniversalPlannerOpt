package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Link;

import java.io.Serializable;

/**
 * User: oracle
 * Date: 21.05.14
 * base class for FactLocoTrack, VirtualLocoTrack and RealLocoTrack.
 */
public abstract class BaseLocoTrack implements Serializable {
    private Link link;
    private Long timeStart;
    private Long durationStart;
    private Long timeEnd;
    private Long durationEnd;
    private Long realTrainId = -1L;

    public enum State {RESERVE, WITH_TRAIN, PUSH, UNUSED, TECH, NA};
    /*
        public class LocoState{
        State locoState;

        public LocoState(State locoState) {
            this.locoState = locoState;
        }

        public String getValue() {
            String locoState = "в рабочем режиме";
            case State:
            RESERVE:

            if (fLoco.getTrack().getState().equals(BaseLocoTrack.State.RESERVE)){
                locoState = "резервом";
            } else if (fLoco.getTrack().getState().equals(BaseLocoTrack.State.TECH)){
                locoState = "в ТО режиме";
            } else if (fLoco.getTrack().getState().equals(BaseLocoTrack.State.PUSH)){
                locoState = "в подталкивающем режиме";
            }
        }

        @Override
        public String toString() {
            return this.getValue();
        }

    };*/
    private State state = State.NA;
    private Long trainId;//real

    public BaseLocoTrack() {
    }

    public BaseLocoTrack(Link link, State state, Long trainId) {
        this.link = link;
        this.state = state;
        this.trainId = trainId;
    }

    public BaseLocoTrack(Link link, Long trainId) {
        this.link = link;
        this.trainId = trainId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseLocoTrack that = (BaseLocoTrack) o;

        if (link != null ? !link.equals(that.link) : that.link != null) return false;
        if (state != that.state) return false;
        if (trainId != null ? !trainId.equals(that.trainId) : that.trainId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = link != null ? link.hashCode() : 0;
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (trainId != null ? trainId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BaseLocoTrack");
        sb.append("{link=").append(link);
        sb.append(", locoState=").append(state);
        sb.append(", trainId=").append(trainId);
        sb.append('}');
        return sb.toString();
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public Long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Long timeStart) {
        this.timeStart = timeStart;
    }

    public Long getDurationStart() {
        return durationStart;
    }

    public void setDurationStart(Long durationStart) {
        this.durationStart = durationStart;
    }

    public Long getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public Long getDurationEnd() {
        return durationEnd;
    }

    public void setDurationEnd(Long durationEnd) {
        this.durationEnd = durationEnd;
    }

    public Long getRealTrainId() {
        return realTrainId;
    }

    public void setRealTrainId(Long realTrainId) {
        this.realTrainId = realTrainId;
    }
}
