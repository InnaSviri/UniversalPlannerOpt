package ru.programpark.entity.slot;

/**
 * Created with IntelliJ IDEA.
 * User: oracle
 * Date: 29.07.15
 * Time: 18:25
 * To change this template use File | Settings | File Templates.
 */
public class AveDurationSlotWrapper {
    public Long t;
    public Slot slot;

    public AveDurationSlotWrapper(Long t, Slot slot) {
        this.t = t;
        this.slot = slot;
    }
}
