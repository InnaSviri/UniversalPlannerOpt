package ru.programpark.entity.loco;

/**
 * User: oracle
 * Date: 20.05.14
 *
 */
public class SeriesPair {
    private Integer series = 0;
    private Integer section = 0;

    public SeriesPair() {
    }

    public SeriesPair(Integer series, Integer section) {
        this.series = series;
        this.section = section;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SeriesPair that = (SeriesPair) o;

        if (section != null ? !section.equals(that.section) : that.section != null) return false;
        if (series != null ? !series.equals(that.series) : that.series != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = series != null ? series.hashCode() : 0;
        result = 31 * result + (section != null ? section.hashCode() : 0);
        return result;
    }

    public Integer getSeries() {
        return series;
    }

    public void setSeries(Integer series) {
        this.series = series;
    }

    public Integer getSection() {
        return section;
    }

    public void setSection(Integer section) {
        this.section = section;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SeriesPair");
        sb.append("{series=").append(series);
        sb.append(", section=").append(section);
        sb.append('}');
        return sb.toString();
    }
}
