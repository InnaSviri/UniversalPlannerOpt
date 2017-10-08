package ru.programpark.planners;

import org.junit.Before;
import ru.programpark.entity.data.InputData;
import ru.programpark.entity.data.OutputData;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.BaseLocoTrack;
import ru.programpark.entity.loco.RealLoco;
import ru.programpark.entity.loco.RealLocoTrack;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.util.ShortestPath;


import java.util.ArrayList;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertTrue;
import static ru.programpark.planners.utils.CapacityUtils.createSlot;

public class TestEnvironment {

    protected InputData inputData;
    protected OutputData outputData;
    protected Station stationOne;
    protected Station stationTwo;
    protected Station stationThree;
    protected Station stationFour;
    protected Link linkOneTwo;
    protected Link linkTwoThree;
    protected Link linkThreeFour;

    protected Link linkFourThree;
    protected Link linkThreeTwo;
    protected Link linkTwoOne;

    protected RealLoco simpleLoco;
    protected FactTeam simpleFactTeam;

    protected ShortestPath path;

    protected RealLoco simpleLocoWithTwoLink;
    protected FactTeam simpleFactTeamOne;
    protected FactTeam simpleFactTeamTwo;
    protected FactTeam simpleFactTeamForPassenger;

    @Before
    public void setUp() {
        inputData = new InputData();
        outputData = new OutputData();
        stationOne = new Station(1001L);
        stationTwo = new Station(1002L);
        //станция смены бригад, время обработки 1 час
        stationThree = new Station(1003L);
        stationThree.setNormTime(3600L);
        stationFour = new Station(1004L);
        inputData.getStations().put(stationOne.getId(), stationOne);
        inputData.getStations().put(stationTwo.getId(), stationTwo);
        inputData.getStations().put(stationThree.getId(), stationThree);
        inputData.getStations().put(stationFour.getId(), stationFour);

        linkOneTwo = new Link(stationOne, stationTwo, 36000L, 100, false);
        linkTwoThree = new Link(stationTwo, stationThree, 36000L, 100, false);
        linkThreeFour = new Link(stationThree, stationFour, 36000L, 100, false);

        linkFourThree = new Link(stationFour, stationThree, 36000L, 100, false);
        linkThreeTwo = new Link(stationThree, stationTwo, 36000L, 100, false);
        linkTwoOne = new Link(stationTwo, stationOne, 36000L, 100, false);

        inputData.getLinks().put(new StationPair(linkOneTwo), linkOneTwo);
        inputData.getLinks().put(new StationPair(linkTwoThree), linkTwoThree);
        inputData.getLinks().put(new StationPair(linkThreeFour), linkThreeFour);
        inputData.getLinks().put(new StationPair(linkFourThree), linkFourThree);
        inputData.getLinks().put(new StationPair(linkThreeTwo), linkThreeTwo);
        inputData.getLinks().put(new StationPair(linkTwoOne), linkTwoOne);

        for (long i = 1L; i <= 7L; i++) {
            Slot slot = createSlot(new LinkedHashSet<Link>() {{
                add(linkOneTwo);
                add(linkTwoThree);
                add(linkThreeFour);
            }}, 2000L + i, (i - 1L) * 1200L + 15L);

            inputData.getSlots().put(slot.getSlotId(), slot);
        }

        /*path = new ShortestPath(new ArrayList<Link>(){{
            add(linkOneTwo);
            add(linkTwoThree);
            add(linkThreeFour);
            add(linkFourThree);
            add(linkThreeTwo);
            add(linkTwoOne);
        }});*/

        simpleLoco = new RealLoco();
        simpleLoco.setRealLocoId(4001L);
        simpleLoco.setRoute(new ArrayList<RealLocoTrack>() {{
            add(new RealLocoTrack(linkOneTwo, BaseLocoTrack.State.WITH_TRAIN, 3001L, 1000L, 37000L, -1L));
        }});

        //Добавляем список тригадных фактов (машинист, помошник машиниста, кочегар)
        simpleFactTeam = new FactTeam(6001L, 0L, stationOne, null);
        simpleFactTeam.setDepot(stationFour);
        simpleFactTeam.setTimeOfPresence(simpleFactTeam.getTimeOfFact());
        simpleFactTeam.setTimeUntilRest(128800L);
        //inputData.getFactTeams().put(simpleFactTeam.getId(), simpleFactTeam);

        simpleLocoWithTwoLink = new RealLoco();
        simpleLocoWithTwoLink.setRealLocoId(4001L);
        simpleLocoWithTwoLink.setRoute(new ArrayList<RealLocoTrack>() {{
            add(new RealLocoTrack(linkOneTwo, BaseLocoTrack.State.WITH_TRAIN, 3001L, 1000L, 37000L, -1L));
            add(new RealLocoTrack(linkTwoThree, BaseLocoTrack.State.WITH_TRAIN, 3001L, 38000L, 74000L, -1L));
        }});

        simpleFactTeamOne = new FactTeam(6001L, 0L, stationOne, null);
        simpleFactTeamOne.setDepot(stationFour);
        simpleFactTeamOne.setTimeOfPresence(simpleFactTeamOne.getTimeOfFact());
        simpleFactTeamOne.setTimeUntilRest(40000L);
        //inputData.getFactTeams().put(simpleFactTeamOne.getId(), simpleFactTeamOne);

        simpleFactTeamTwo = new FactTeam(6002L, 35000L, stationTwo, null);
        simpleFactTeamTwo.setDepot(stationFour);
        simpleFactTeamTwo.setTimeOfPresence(simpleFactTeamTwo.getTimeOfFact());
        simpleFactTeamTwo.setTimeUntilRest(128800L);
        //inputData.getFactTeams().put(simpleFactTeamTwo.getId(), simpleFactTeamTwo);

        simpleFactTeamForPassenger = new FactTeam(6002L, -37000L, stationTwo, null);
        simpleFactTeamForPassenger.setDepot(stationFour);
        simpleFactTeamForPassenger.setTimeOfPresence(simpleFactTeamForPassenger.getTimeOfFact());
        simpleFactTeamForPassenger.setTimeUntilRest(128800L);
        //inputData.getFactTeams().put(simpleFactTeamForPassenger.getId(), simpleFactTeamForPassenger);
    }

}
