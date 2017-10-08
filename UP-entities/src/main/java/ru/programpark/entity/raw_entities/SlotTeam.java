package ru.programpark.entity.raw_entities;

import ru.programpark.entity.team.BaseTeamTrack;

import java.util.ArrayList;
import java.util.List;

public class SlotTeam extends BaseSlot {
    public List<Track> route = new ArrayList<>();

    public class Track extends BaseTrack {
        public Long state;
        public Long locoId = -1L;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Track track = (Track) o;

            if (state != null ? !state.equals(track.state) : track.state != null) return false;
            return !(locoId != null ? !locoId.equals(track.locoId) : track.locoId != null);

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (state != null ? state.hashCode() : 0);
            result = 31 * result + (locoId != null ? locoId.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Track{");
            sb.append("state=").append(state);
            sb.append(", locoId=").append(locoId);
            sb.append('}');
            return super.toString() + sb.toString();
        }
    }

    public boolean passesStation(Long stId){
        for (Track track:route){
            if (track.stationFromId.equals(stId))
                return true;
        }

        return false;
    }
}
