package ru.programpark.entity.loco;

import ru.programpark.entity.fixed.Station;
import java.util.ArrayList;

// Фактический локомотив в аспекте справочных данных
public interface LocoAttributes {
    SeriesPair getSeriesPair();
    Integer getSeries();
    Integer getNSections();
    ArrayList<LocoRegion> getLocoRegions();
    Station getDepotStation();
}
