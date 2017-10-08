package ru.programpark.entity.fixed;

/**
 * User: oracle
 * Date: 02.07.15
 * Станционный путь передается сообщением:
 * +line(id(LineId), station(StationId), length(Length))
 */
public class Line {
    private Long id;
    private Station station;
    private Long length;

    public Line(Long id, Station station, Long length) {
        this.id = id;
        this.station = station;
        this.length = length;
    }

    public Line() {
    }

    public Line(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Line line = (Line) o;

        if (id != null ? !id.equals(line.id) : line.id != null) return false;
        if (length != null ? !length.equals(line.length) : line.length != null) return false;
        if (station != null ? !station.equals(line.station) : line.station != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (station != null ? station.hashCode() : 0);
        result = 31 * result + (length != null ? length.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Line");
        sb.append("{id=").append(id);
        sb.append(", station=").append(station);
        sb.append(", length=").append(length);
        sb.append('}');
        return sb.toString();
    }
}
