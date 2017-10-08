package ru.programpark.entity.team;

import ru.programpark.entity.fixed.Station;
import java.util.List;

public interface TeamAttributes {
    List<TeamRegion> getTeamWorkRegions();
    Station getDepot();
    List<Long> getLocoSeries();
    List<Long> getWeightTypes();
    boolean isAllowedToWorkOnLongTrain();
    boolean isAllowedToWorkOnHeavyTrain();
}
