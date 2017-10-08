package ru.programpark.planners.train;

import ru.programpark.entity.train.RealTrain;
import ru.programpark.entity.train.RealTrainTrack;
import ru.programpark.planners.TestEnvironment;

import static org.junit.Assert.assertFalse;

public class CalcCapacityTest extends TestEnvironment {

    //TrainPlanner planner = new TrainPlanner();

    /*@Test
    public void calcCapacityTest() {
        System.out.println("cacl capacity test started");

        Map<StationPair, Link> links = planner.calcCapacity(inputData);
        Link link = links.get(new StationPair(stationOne.getId(), stationTwo.getId()));

        for (long i = 0L; i < 7L; i++) {
            Capacity capacity = link.getCapacities().get(i * 600L);

            int capacityQuantity = capacity == null ? 0 : capacity.getCapacity();
            switch ((int)i) {
                case 0:
                    assertTrue(capacityQuantity == 1);
                    break;
                case 1:
                    assertTrue(capacityQuantity == 0);
                    break;
                case 2:
                    assertTrue(capacityQuantity == 1);
                    break;
                case 3:
                    assertTrue(capacityQuantity == 0);
                    break;
                case 4:
                    assertTrue(capacityQuantity == 1);
                    break;
                case 5:
                    assertTrue(capacityQuantity == 0);
                    break;
                case 6:
                    assertTrue(capacityQuantity == 1);
                    break;
            }
        }
    }*/

    /*@Test
    @Ignore
    public void TestTrivialTrainTask() {

        planner.calcCapacity(inputData);

        OneTask trainOne = new OneTask(3001L, 300L, 3600L,
                //маршрут
                new ArrayList<List<OneTaskTrack>>() {{
                    add(new ArrayList<OneTaskTrack>() {{
                        add(new OneTaskTrack(linkOneTwo, -1L, -1L, -1L));
                    }});
                }}
                , 1L, OneTask.OneTaskType.TASK);

        OneTask trainTwo = new OneTask(3002L, 420L, 3600L,
                //маршрут
                new ArrayList<List<OneTaskTrack>>() {{
                    add(new ArrayList<OneTaskTrack>() {{
                        add(new OneTaskTrack(linkOneTwo, -1L, -1L, -1L));
                    }});
                }}
                , 1L, OneTask.OneTaskType.TASK);
        inputData.getOneTasks().put(trainOne.getId(), trainOne);
        inputData.getOneTasks().put(trainTwo.getId(), trainTwo);

        planner.plan(inputData, outputData);
        Map<Long, RealTrain> realTrains = outputData.getSlotTrains();
        assertNotNull(realTrains);
        assertTrue(realTrains.size() == 2);
        assertTrain(realTrains.get(trainOne.getId()));
        assertTrain(realTrains.get(trainTwo.getId()));
        assertTrue(realTrains.get(trainOne.getId()).getRoute().size() > 0);
        assertTrue(realTrains.get(trainTwo.getId()).getRoute().size() > 0);
        assertFalse(realTrains.get(trainOne.getId()).getRoute().get(0).getTimeStart().equals(
                realTrains.get(trainTwo.getId()).getRoute().get(0).getTimeStart()
        ));
    }*/

    /*@Test
    @Ignore
    public void testTrainWithLocoChangeAndWithoutTimeInterval() {
        planner.calcCapacity(inputData);

        OneTask trainOne = new OneTask(3001L, 7195L, 3600L,
                new ArrayList<List<OneTaskTrack>>() {{
                    add(new ArrayList<OneTaskTrack>(){{
                        add(new OneTaskTrack(linkOneTwo, -1L, -1L, -1L));
                        add(new OneTaskTrack(linkTwoThree, -1L, -1L, -1L));
                        add(new OneTaskTrack(linkThreeFour, -1L, -1L, -1L));
                    }});
                }}
                , 1L, OneTask.OneTaskType.TASK);

        inputData.getOneTasks().put(trainOne.getId(), trainOne);

        planner.plan(inputData, outputData);
        Map<Long, RealTrain> realTrains = outputData.getSlotTrains();
        assertNotNull(realTrains);
        assertTrue(realTrains.size() == 1);
        assertTrue(realTrains.get(trainOne.getId()).getRoute().size() == 3);

        assertTrain(realTrains.get(trainOne.getId()));
    }*/

    /*@Test
    @Ignore
    public void testTrainWithLocoChangeAndFreeTimeInterval() {
        planner.calcCapacity(inputData);

        OneTask trainOne = new OneTask(3001L, 0L, 3600L,
                new ArrayList<List<OneTaskTrack>>() {{
                    add(new ArrayList<OneTaskTrack>(){{
                        add(new OneTaskTrack(linkOneTwo, -1L, -1L, -1L));
                        add(new OneTaskTrack(linkTwoThree, -1L, -1L, -1L));
                        add(new OneTaskTrack(linkThreeFour, -1L, -1L, -1L));
                    }});
                }}
                , 1L, OneTask.OneTaskType.TASK);

        inputData.getOneTasks().put(trainOne.getId(), trainOne);

        planner.plan(inputData, outputData);
        Map<Long, RealTrain> realTrains = outputData.getSlotTrains();
        assertNotNull(realTrains);
        assertTrue(realTrains.size() == 1);
        assertTrain(realTrains.get(trainOne.getId()));
        assertTrue(realTrains.get(trainOne.getId()).getRoute().get(2).getTimeStart() == 75600L);
    }*/

    /*@Test
    @Ignore
    public void testTrainWithLocoChangeAndWastedTimeInterval() {
        planner.calcCapacity(inputData);

        OneTask trainOne = new OneTask(3001L, 3000L, 3600L,
                new ArrayList<List<OneTaskTrack>>() {{
                    add(new ArrayList<OneTaskTrack>(){{
                        add(new OneTaskTrack(linkOneTwo, -1L, -1L, -1L));
                        add(new OneTaskTrack(linkTwoThree, -1L, -1L, -1L));
                        add(new OneTaskTrack(linkThreeFour, -1L, -1L, -1L));
                    }});
                }}
                , 1L, OneTask.OneTaskType.TASK);

        inputData.getOneTasks().put(trainOne.getId(), trainOne);

        //воруем последний интервал
        Link stealLink = inputData.getLinks().get(new StationPair(linkThreeFour));
        Iterator<Capacity> it = stealLink.getCapacities().values().iterator();
        while (it.hasNext()) {
            Capacity stealCapacity = it.next();
            if (!it.hasNext()) {
                //последний интервал
                stealCapacity.setCapacity(0);
            }
        }


        planner.plan(inputData, outputData);
        Map<Long, RealTrain> realTrains = outputData.getSlotTrains();
        assertNotNull(realTrains);
        assertTrue(realTrains.size() == 1);
        assertTrain(realTrains.get(trainOne.getId()));
        assertTrue(realTrains.get(trainOne.getId()).getRoute().size() == 2);
    }*/

    public void assertTrain(RealTrain train) {
        boolean error = false;
        //все времена должны отличаться от -1;
        for (RealTrainTrack track : train.getRoute()) {
            if (track.getTimeStart() < 0L || track.getTimeEnd() < 0L) {
                error = true;
            }
        }
        assertFalse(error);
        error = false;
        //все времена должны последовательно увеличиваться по мере движения поезда
        long currentTime = 0L;
        for (RealTrainTrack track : train.getRoute()) {
            if (track.getTimeStart() >= currentTime) {
                currentTime = track.getTimeStart();
            } else {
                error = true;
            }
            if (track.getTimeEnd() > currentTime) {
                currentTime = track.getTimeEnd();
            } else {
                error = true;
            }
        }

        assertFalse(error);
    }


}
