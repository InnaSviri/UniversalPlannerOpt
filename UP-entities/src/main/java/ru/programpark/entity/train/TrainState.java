package ru.programpark.entity.train;

public abstract class TrainState {
    private Long id;
    private Long time;

    public TrainState() {
    }

    public TrainState(Long id, Long time) {
        super();
        this.id = id;
        this.time = time;
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

}
