package ru.programpark.planners.team;

/**
 * Created by oracle on 08.12.2015.
 */
public class TeamPassDataWrapper {
    public long whenArrives;
    public long whenDeparts;

    public TeamPassDataWrapper(long whenArrives, long whenDeparts) {
        this.whenArrives = whenArrives;
        this.whenDeparts = whenDeparts;
    }
}
