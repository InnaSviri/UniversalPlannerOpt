package ru.programpark.planners.loco;

import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.util.FilterIterator;
import java.util.Map;
import java.util.Iterator;

public class StationWithService extends Station {
    private Long serviceType;

    public StationWithService(Long serviceType) {
        this.serviceType = serviceType;
    }

    @Override public boolean equals(Object o) {
        if (o instanceof Station) {
            Station st = (Station) o;
            Map<Long, Long> serviceAvail = st.getServiceAvailable();
            return (serviceAvail != null &&
                        serviceAvail.containsKey(serviceType));
        } else {
            return false;
        }
    }

    public Long shortestServiceType(Long hintType) {
        Map<Long, Long> serviceAvail = getServiceAvailable();
        Long curDuration = serviceAvail.get(hintType);
        for (Map.Entry<Long, Long> serviceDuration : serviceAvail.entrySet()) {
            if (serviceDuration.getValue() < curDuration) {
                curDuration = serviceDuration.getValue();
                return serviceDuration.getKey();
            }
        }
        return hintType;
    }

    public static class NoHaulingDirectionFilter
    extends FilterIterator<Link> {
        public NoHaulingDirectionFilter(Iterator<Link> iter) {
            super(iter);
        }
        @Override public boolean test(Link link) {
            return ! link.isHaulingDirection();
        }
    }
}
