package ru.programpark.planners.team;

import ru.programpark.entity.fixed.Station;

public class StationWithTeamChange extends Station {
    @Override
    public boolean equals(Object o) {
        if (o instanceof Station) {
            Station st = (Station) o;
            return st.getNormTime() > 0;
        } return false;
    }
}
