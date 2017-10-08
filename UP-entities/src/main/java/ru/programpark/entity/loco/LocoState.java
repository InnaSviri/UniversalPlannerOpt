package ru.programpark.entity.loco;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 03.10.14
 * Time: 17:54
 * To change this template use File | Settings | File Templates.
 */
public abstract class LocoState {
    private Long id;
    private Long time;

    public LocoState(Long id, Long time) {
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
