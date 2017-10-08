package ru.programpark.entity.train;

import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.fixed.Link;import java.lang.Long;import java.lang.Override;import java.lang.String;import java.lang.StringBuilder;

/**
 * User: oracle
 * Date: 10.07.14
 */
public class OneTaskTrack extends SlotTrack {

    public OneTaskTrack(Link link, Long timeStart, Long timeEnd, Long slotId) {
        super(link, timeStart, timeEnd, slotId);
    }

    public void reset(){
        this.setTimeStart(Long.MIN_VALUE);
        this.setTimeEnd(Long.MIN_VALUE);
        this.setSlotId(-1L);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("OneTaskTrack");
        sb.append("{slotId=").append(getSlotId());
        sb.append('}');
        return sb.toString();
    }
}
