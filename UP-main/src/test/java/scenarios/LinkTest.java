package scenarios;

import org.junit.Test;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.slot.AveDurationSlotWrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by oracle on 06.11.2015.
 */
public class LinkTest {
    @Test
    public void testLinkDuration1() throws Exception {
        Link link = new Link();
        Map<Integer, HashSet<AveDurationSlotWrapper>> durationMap = new HashMap<>(); // ключом является время начала 3-хчасового интервала: 0, 3, 6, 9, 12, 15, 18, 21
        HashSet<AveDurationSlotWrapper> set1 = new HashSet<>();
        set1.add(new AveDurationSlotWrapper(2L, null));
        set1.add(new AveDurationSlotWrapper(4L, null));
        HashSet<AveDurationSlotWrapper> set2 = new HashSet<>();
        set2.add(new AveDurationSlotWrapper(5L, null));

        durationMap.put(0, set1);
        durationMap.put(3, set2);
        link.setDefaultDuration(0L);
        link.setDurationMap(durationMap);
        Long duration = link.getDuration(0L);
        assertTrue(duration.equals(3L));
    }

    @Test
    public void testLinkDuration2() throws Exception {
        Link link = new Link();
        Map<Integer, HashSet<AveDurationSlotWrapper>> durationMap = new HashMap<>(); // ключом является время начала 3-хчасового интервала: 0, 3, 6, 9, 12, 15, 18, 21
        HashSet<AveDurationSlotWrapper> set1 = new HashSet<>();
        set1.add(new AveDurationSlotWrapper(4L, null));
        set1.add(new AveDurationSlotWrapper(6L, null));
        HashSet<AveDurationSlotWrapper> set2 = new HashSet<>();
        set2.add(new AveDurationSlotWrapper(1L, null));

        durationMap.put(0, set1);
        durationMap.put(3, set2);
        link.setDefaultDuration(0L);
        link.setDurationMap(durationMap);
        Long duration = link.getDuration(0L);
        assertTrue(!duration.equals(3L));
    }

    //тест на учет технологической стоянки
    //более детальный тест на костыль
}



