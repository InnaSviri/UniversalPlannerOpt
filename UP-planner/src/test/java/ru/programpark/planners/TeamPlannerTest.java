package ru.programpark.planners;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

//import ru.programpark.planners.team.TeamPlanner;

public class TeamPlannerTest extends TestEnvironment {

    @Test
    public void simpleTest() throws InterruptedException {

        /*inputData.getFactTeams().put(simpleFactTeam.getId(), simpleFactTeam);
        outputData.getSlotLocos().put(simpleLoco.getRealLocoId(), simpleLoco);

        TeamPlanner planner = TeamPlanner.instance();
        //TeamPlanner planner = new TeamPlanner(inputData, outputData);

        planner.plan(inputData, outputData);
        List<RealTeam> realTeams = new ArrayList<>();
        realTeams.addAll(outputData.getSlotTeams().values());

                assertNotNull(realTeams);
        assertEquals(realTeams.size(), 1);
        RealTeam checkingTeam = realTeams.get(0);

        //Проверка правильности запланированного бригадного слота
        assertNotNull(checkingTeam.getRoute());
        assertTrue(checkingTeam.getRoute().size() == 1);
        assertEquals(checkingTeam.getRoute().get(0).getLink(), linkOneTwo);

        //Проверка правильности использованной бригады
        assertEquals(checkingTeam.getId(), simpleFactTeam.getId());

        //Проверка пост условий: использованная бригада оказалась на нужной станции
        //assertEquals(readyTeams.get(simpleFactTeam.getId()).station, stationTwo);*/
    }

    @Test
    public void simpleWithTwoLocoTest() throws InterruptedException {
        /*inputData.getFactTeams().put(simpleFactTeamOne.getId(), simpleFactTeamOne);
        inputData.getFactTeams().put(simpleFactTeamTwo.getId(), simpleFactTeamTwo);

        outputData.getSlotLocos().put(simpleLocoWithTwoLink.getRealLocoId(), simpleLocoWithTwoLink);

        TeamPlanner planner = TeamPlanner.instance();//new TeamPlanner(inputData, outputData);
        planner.plan(inputData, outputData);
        List<RealTeam> realTeams = new ArrayList<>();
        realTeams.addAll(outputData.getSlotTeams().values());

        assertNotNull(realTeams);
        assertEquals(realTeams.size(), 2);
        RealTeam checkingTeamOne = realTeams.get(0);
        assertNotNull(checkingTeamOne.getRoute());
        assertEquals(checkingTeamOne.getRoute().size(), 1);
        assertEquals(checkingTeamOne.getRoute().get(0).getLink(), linkOneTwo);
        assertEquals(checkingTeamOne.getId(), simpleFactTeamOne.getId());
        //assertEquals(readyTeams.get(checkingTeamOne.getId()).station, stationTwo);

        RealTeam checkingTeamTwo = realTeams.get(1);
        assertNotNull(checkingTeamTwo.getRoute());
        assertEquals(checkingTeamTwo.getRoute().size(), 1);
        assertEquals(checkingTeamTwo.getRoute().get(0).getLink(), linkTwoThree);
        assertEquals(checkingTeamTwo.getId(), simpleFactTeamTwo.getId());
        //assertEquals(readyTeams.get(checkingTeamTwo.getId()).station, stationThree); */
    }

    @Test
    @Ignore
    /**
     * loco: St1 -> St2 -> St3
     * teams: t1 = St2
     */
    public void testTeamWithPassenger() throws InterruptedException {
        /*inputData.getFactTeams().put(simpleFactTeamForPassenger.getId(), simpleFactTeamForPassenger);

        outputData.getSlotLocos().put(simpleLocoWithTwoLink.getRealLocoId(), simpleLocoWithTwoLink);

        TeamPlanner planner = TeamPlanner.instance();//new TeamPlanner(inputData, outputData);
        planner.plan(inputData, outputData);
        List<RealTeam> realTeams = new ArrayList<>();
        realTeams.addAll(outputData.getSlotTeams().values());
        //проверяем, что:
        //1) маршрут запланирован на обеих участках с бригадой
        //2) есть маршрут, по которому бригада приехала на станцию St1
        assertNotNull(realTeams);
        assertEquals(realTeams.size(), 1);
        RealTeam team = realTeams.get(0);
        assertNotNull(team);
        assertNotNull(team.getRoute());
        assertEquals(team.getRoute().size() , 3);
        assertEquals(team.getRoute().get(0).getLink().getFrom(), stationTwo);
        assertEquals(team.getRoute().get(0).getLink().getTo(), stationOne);
        assertEquals(team.getRoute().get(0).getState(), BaseTeamTrack.State.PASSENGER);

        assertEquals(team.getRoute().get(1).getLink().getFrom(), stationOne);
        assertEquals(team.getRoute().get(1).getLink().getTo(), stationTwo);
        assertEquals(team.getRoute().get(1).getState(), BaseTeamTrack.State.AT_WORK);

        assertEquals(team.getRoute().get(2).getLink().getFrom(), stationTwo);
        assertEquals(team.getRoute().get(2).getLink().getTo(), stationThree);
        assertEquals(team.getRoute().get(2).getState(), BaseTeamTrack.State.AT_WORK);*/

    }

    /*
    @Test
    public void testInitReadyTeams() {
        Map<Long, ReadyTeam> readyTeams = initReadyTeams(new ArrayList<FactTeam>(){{
            add(simpleFactTeam);
        }});

        assertNotNull(readyTeams);
        assertEquals(readyTeams.size(), 1);
        ReadyTeam team = readyTeams.get(simpleFactTeam.getId());

        assertNotNull(team.teamId);
        assertNotNull(team.station);
        assertNotNull(team.depot);
        assertNotNull(team.timeOfPresence);
        assertNotNull(team.timeUntilRest);
        assertEquals(team.teamId, simpleFactTeam.getId());
        assertEquals(team.station, simpleFactTeam.getStation());
        assertEquals(team.depot, simpleFactTeam.getDepot());
        assertEquals(team.timeOfPresence, simpleFactTeam.getTimeOfPresence());
        assertEquals(team.timeUntilRest, simpleFactTeam.getTimeUntilRest());
    }
    */

    /*
    @Test
    public void testInitTeamSlots() {
        List<TeamSlot> teamSlots = initTeamSlot(new ArrayList<RealLoco>() {{
            add(simpleLoco);
        }});

        assertNotNull(teamSlots);
        assertEquals(teamSlots.size(), 1);
        TeamSlot teamSlot = teamSlots.get(0);

        assertNotNull(teamSlot.trainId);
        assertNotNull(teamSlot.locoId);
        assertNotNull(teamSlot.route);
        assertTrue(teamSlot.route.size() > 0);
        assertEquals(teamSlot.trainId, simpleLoco.getRoute().get(0).getTrainId());
        assertEquals(teamSlot.locoId, simpleLoco.getRealLocoId());
        assertEquals(teamSlot.route.size(), simpleLoco.getRoute().size());
        for (int i = 0; i < teamSlot.route.size(); i++) {
            SlotTrack slotTrack = teamSlot.route.get(i);
            RealLocoTrack locoTrack = simpleLoco.getRoute().get(i);

            assertNotNull(slotTrack.getLink());
            assertNotNull(slotTrack.getTimeOneDayBack());
            assertNotNull(slotTrack.getTimeEnd());
            assertNotNull(slotTrack.getSlotId());
            assertEquals(slotTrack.getLink(), locoTrack.getLink());
            assertEquals(slotTrack.getTimeOneDayBack(), locoTrack.getTimeOneDayBack());
            assertEquals(slotTrack.getTimeEnd(), locoTrack.getTimeEnd());
            assertEquals(slotTrack.getSlotId(), locoTrack.getSlotId());
        }
    }*/

    @Test
    @Ignore
    public void testGetNearestReadyTeam() {
        assertTrue(false);
    }


    /*private TeamSlot getTeamSlots(final RealLoco realLoco) {
        //ищем в списке существущих бригадных слотов созданный для этого локо
        //соответствие по locoId и 1-ой станции
        for (TeamSlot teamSlot : teamSlots) {
            assert teamSlot.locoId != null;
            assert realLoco.getRealLocoId() != null;

            boolean equalsId = teamSlot.locoId.equals(realLoco.getRealLocoId());
            boolean notEmptyRoutes = teamSlot.route != null && teamSlot.route.size() > 0 &&
                    realLoco.getRoute() != null && realLoco.getRoute().size() > 0;
            boolean equalsFirstLink = notEmptyRoutes && teamSlot.route.get(0).getLink().equals(realLoco.getRoute().get(0).getLink());

            if (equalsId && equalsFirstLink) {
                return teamSlot;
            }
        }
        return null;
    }*/

    /**
     * Возвращает список тригад для работы по конкретному тригадному слоту
     * @return
     */
    /*private List<ReadyTeam> getNearestReadyTeams(TeamSlot teamSlot) {
        List<ReadyTeam> readyTeams = new ArrayList<>();

        readyTeams.addAll(this.readyTeams.values());

        return readyTeams;
    }*/


    /**
     * Проверяет незапланированные бригады на уход на отдых
     */
    private void checkTeamRest() {

    }


}