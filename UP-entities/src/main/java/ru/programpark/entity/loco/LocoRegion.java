package ru.programpark.entity.loco;

import java.util.ArrayList;

/**
 * User: oracle
 * Date: 19.05.14
 * Тяговое плечо
 * +loco_region(id(RegId), series([series(id(S1), sections(N1)), series(id(S2), sections(N2)), …]), depot([station(St1), station(St2),…]))
 * +loco_region(id(32654121), series([1234, 455, 4234]), depot([station(123132), station(1233333)]))

 RegId – идентификатор тягового плеча.
 S1, S2, – идентификаторы заводских серий локомотивов, которые могут работать на данном тяговом плече. В атрибуте должны быть перечислены все такие серии.
 N1, N2 – количество секций для соответствующей серии.
 St1, St2,… – идентификаторы станций, которые являются депо внутри данного тягового плеча.

 */
public class LocoRegion implements Comparable {
    private Long id;//RegId
    private ArrayList<SeriesPair> seriesPairs = new ArrayList<>();
    private ArrayList<Long> depots = new ArrayList<>();

    public LocoRegion(Long id) {
        this.id = id;
    }

    public LocoRegion() {
    }

    public LocoRegion(Long id, ArrayList<SeriesPair> seriesPairs, ArrayList<Long> depots) {
        this.id = id;
        this.seriesPairs = seriesPairs;
        this.depots = depots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocoRegion that = (LocoRegion) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (depots != null ? !depots.equals(that.depots) : that.depots != null) return false;
        if (seriesPairs != null ? !seriesPairs.equals(that.seriesPairs) : that.seriesPairs != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (seriesPairs != null ? seriesPairs.hashCode() : 0);
        result = 31 * result + (depots != null ? depots.hashCode() : 0);
        return result;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ArrayList<SeriesPair> getSeriesPairs() {
        return seriesPairs;
    }

    public void setSeriesPairs(ArrayList<SeriesPair> seriesPairs) {
        this.seriesPairs = seriesPairs;
    }

    public void addSeriesPair(SeriesPair seriesPair) {
        if (seriesPairs == null) {
            seriesPairs = new ArrayList<SeriesPair>();
        }
        seriesPairs.add(seriesPair);
    }

    public ArrayList<Long> getDepots() {
        return depots;
    }

    public void addDepot(Long depot) {
        if (depots == null) {
            depots = new ArrayList<Long>();
        }
        if (!depots.contains(depot)){
            depots.add(depot);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LocoRegion");
        sb.append("{id = ").append(id);
        sb.append(", seriesPairs=").append(seriesPairs);
        sb.append(", depots=").append(depots);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(Object o) {
        LocoRegion lr = (LocoRegion) o;
        return (int) (this.getId() - lr.getId());
    }
}
