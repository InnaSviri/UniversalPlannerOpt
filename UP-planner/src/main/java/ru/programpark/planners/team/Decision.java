package ru.programpark.planners.team;

public class Decision<K, V> {
    public K teamSlot;
    public V team;

    public Decision(K teamSlot, V team) {
        this.teamSlot = teamSlot;
        this.team = team;
    }
}
