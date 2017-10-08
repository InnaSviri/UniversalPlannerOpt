package ru.programpark.entity.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.programpark.entity.fixed.Line;
import ru.programpark.entity.fixed.Link;
import ru.programpark.entity.fixed.Station;
import ru.programpark.entity.fixed.StationPair;
import ru.programpark.entity.loco.*;
import ru.programpark.entity.slot.AveDurationSlotWrapper;
import ru.programpark.entity.slot.Slot;
import ru.programpark.entity.slot.SlotTrack;
import ru.programpark.entity.team.FactTeam;
import ru.programpark.entity.team.PinnedTeam;
import ru.programpark.entity.team.TeamPass;
import ru.programpark.entity.team.TeamRegion;
import ru.programpark.entity.train.*;
import ru.programpark.entity.util.LoggingAssistant;
import ru.programpark.entity.util.ShortestPath;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * User: oracle
 * Date: 22.05.14
 */
public class InputData {
    private TeamLocoSummaryData eData;//вспомогательная инфа для анализатора
    public int trainReadyCount = 0;
    public int trainArriveCount = 0;
    public int trainDepartCount = 0;
    public int primaryMessageCount = 0;
    public int stationCount = 0;//+station
    public int linkCount = 0;//+link
    public int locoRegionCount = 0;//+loco_region
    public int lineCount = 0;//+line
    public int trainInfoCount = 0; //+train_info
    public int locoAttrCount = 0;//+loco_attributes
    public int fLocoCount = 0;//+fact_loco
    public int fLocoNextServiceCount = 0;//+fact_loco_next_servcie
    public int teamAttrCount = 0;//+team_attributes
    public int fTeamNextRestCount = 0;//+fact_team_next_rest
    public int trainCategoryCount = 0;//+train_category
    public Set<Long> uniqueStations = new HashSet<Long>();
    public boolean fLocoDup = false, fTeamDup = false, fTrainDup = false;
    private Long currentTime = System.currentTimeMillis() / 1000L; //время начала планирования
    private Long currentIdTime = currentTime;
    private Long currentIdOrd = 0L;
    private Map<Long, LocoRegion> locoRegions = new HashMap<>();
    private Map<Long, Station> stations = new HashMap<>();
    private Map<Long, Station> priorityStations = new HashMap<>();
    private Map<Station, Map<Long,Line>> linesByStation = new HashMap<>();
    private Map<String, Long> stationNameToIdMap = new HashMap();
    private Map<StationPair, Link> links = new HashMap<>();
    private Map<Long, Slot> slots = new HashMap<>();
    private Map<Long, Slot> slots_pass = new HashMap();
    private Map<Long, Task> tasks = new HashMap<>();
    private Map<Long, FactTrain> factTrains = new HashMap<>();
    private Map<Long, FactTrain> fictitousFactTrainsFromTasks = new HashMap<>();
    private Map<Long, TrainState> trainStates = new HashMap<>();
    private Map<Long, TrainCategory> trainCategories = new HashMap<>();
    private Map<Long, FactLoco> factLocos = new HashMap<>();
    private Map<Long, FactTeam> factTeams = new HashMap();
    private Map<Long, TeamRegion> teamWorkRegions = new HashMap();
    private Map<Long, TeamRegion> teamServiceRegions = new HashMap();
    private List<LocoRelocation> relocations = new ArrayList<>();
    private Set<WeightType> weightTypes = new HashSet<>();
    //дополнительные данные, формируемые внутри ОПТР
    private Map<Long, OneTask> oneTasks = new HashMap<>();//заполняется из trainFact и task в бывшей ОПП части
    private ShortestPath shortestPath = new ShortestPath();
    //конфигурационные данные, не приходят из вектора (приходит только bulk_planning из Вектора)
    private Map<String, Number> config = new HashMap<>();
    private Map<String, Number> configFromVector = new HashMap<>();
    private boolean filtered, tasksProcessed;
    private List<TeamPass> teamPassList = new ArrayList<>();
    private List<PinnedTeam> pinnedTeamList = new ArrayList<>();

    private static Logger logger = null;
    private static Logger LOGGER() {
        if (logger == null) logger = LoggerFactory.getLogger(InputData.class);
        return logger;
    }

    private void CHECK_INTERRUPTED() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            LOGGER().warn("Принудительная остановка!");
            throw new InterruptedException();
        }
    }

    private static final long seed = 358810191994695139L;
    private static Random random = new Random(seed);

    public void addAllReferenceData(InputData priorData) {
        this.stations.putAll(priorData.getStations());
        this.links.putAll(priorData.getLinks());
        this.locoRegions.putAll(priorData.getLocoRegions());
        this.teamWorkRegions.putAll(priorData.getTeamWorkRegions());
        this.teamServiceRegions.putAll(priorData.getTeamServiceRegions());
    }

    public void preprocess(InputData defaultData) throws InterruptedException {
        fTrainDup = (trainInfoCount == factTrains.size());
        fLocoDup = (locoAttrCount == factLocos.size());
        fTeamDup = (teamAttrCount == factTeams.size());

        if (!filtered) {
            filtered = filterFactTrainsAndTrimRoutes();
            filtered = filterFactLocos() && filtered;
            filtered = filterFactTeams() && filtered;
            LoggingAssistant.getFilterWriter().close();
        }

        for (FactTeam factTeam : getFactTeams().values()) {
            factTeam.setTimeUntilRest(factTeam.getTimeUntilRest() +
                                      (long) (15 * 60));
        }

        if (!tasksProcessed) { // внутри в DATA.oneTasks() кладутся OneTasks сформированные из FactTrain и Task
            OneTasksHolder oth = new OneTasksHolder(this);
            oth.processTasks();
            oth.processTrainFacts();
            tasksProcessed = true;
        }

        if (defaultData != null) { // значения по умолчанию
            if (getPriorityStations().isEmpty()) {
                for (Station st : defaultData.getPriorityStations().values()) {
                    addPriorityStation(getStationById(st.getId()));
                }
            }
            if (relocations.isEmpty()) {
                for (LocoRelocation reloc : defaultData.getRelocations()) {
                    relocations.add(internRelocation(reloc));
                }
            }
            if (weightTypes.isEmpty()) {
                weightTypes.addAll(defaultData.getWeightTypes());
            }
            for (Station dSt : defaultData.getStations().values()) {
                Station st;
                if (dSt.hasName() && (st = stations.get(dSt.getId())) != null) {
                    st.setProcessTime(dSt.getProcessTime());
                    st.setNormTime(dSt.getNormTime());
                }
            }

            for (Map.Entry<String, Number> pv : config.entrySet()){
                configFromVector.put(pv.getKey(), pv.getValue());
            }

            for (Map.Entry<String, Number> pv : defaultData.getConfig().entrySet()) {
                if (! config.containsKey(pv.getKey())) {
                    config.put(pv.getKey(), pv.getValue());
                }
            }

            //if (teamServiceRegions.isEmpty()) {
            teamServiceRegions.clear();//пока приходят кривые данные поэтому их не используем todo
                for (TeamRegion region : defaultData.getTeamServiceRegions().values()) {
                    teamServiceRegions.put(region.getId(), region);
                }
            //}
        }

        calculateAveLinkDuration();
    }

    private boolean filterFactTrainsAndTrimRoutes() throws InterruptedException {
        Iterator<FactTrain> iter = factTrains.values().iterator();
        boolean res = false;

        while (iter.hasNext()){
            CHECK_INTERRUPTED();
            FactTrain train = iter.next();
            String cause;
            if ((cause = train.checkForExclusion(links)) != null) {
                iter.remove();
                LoggingAssistant.getFilterWriter().println("Поезд " + train.getId() + " отсеян: " + cause);
                res = true;
            } else if (train.getMainRoute() != null) {
                train.trimRoutes();
                try {
                    if (train.getMainRoute() == null || train.getMainRoute().getLinkList() == null ||
                            train.getMainRoute().getLinkList().size() == 0) {
                        iter.remove();
                        cause = " ни одного маршрута";
                        res = true;
                        LoggingAssistant.getFilterWriter().println("Поезд " + train.getId() + " отсеян: " + cause);
                    }
                } catch (Exception e) {
                    LoggingAssistant.logException(e);
                }
            }
        }

        return res;
    }

    private boolean filterFactLocos() throws InterruptedException {
        Iterator<FactLoco> iter = getFactLocos().values().iterator();
        boolean res = false;

        while (iter.hasNext()){
            CHECK_INTERRUPTED();
            FactLoco loco = iter.next();
            String cause;
            if ((cause = loco.checkForExclusion()) != null) {
                iter.remove();
                res = true;
                LoggingAssistant.getFilterWriter().println("Локомотив " + loco.getId() + " отсеян: " + cause);
            }
        }

        return res;
    }

    private boolean filterFactTeams() throws InterruptedException {
        boolean res = false;
        Iterator<FactTeam> iter = getFactTeams().values().iterator();

        while (iter.hasNext()){
            CHECK_INTERRUPTED();
            FactTeam team = iter.next();
            String cause;
            if ((cause = team.checkForExclusion()) != null) {
                iter.remove();
                res = true;
                LoggingAssistant.getFilterWriter().println("Бригада " + team.getId() + " отсеяна: " + cause);
            }
        }

        return res;
    }

    private LocoRelocation internRelocation(LocoRelocation reloc) {
        Link linkFrom =
                getLinkByStationPair(StationPair.specifier(reloc.getLinkFrom()));
        Link linkTo =
            getLinkByStationPair(StationPair.specifier(reloc.getLinkTo()));
        return new LocoRelocation(linkFrom, linkTo, reloc.getTime(),
                                  reloc.getInterval(), reloc.getNumber());
    }

    public void updateForCorrections(InputData newData) {
        String ignored = "";
        if (newData.stationCount > 0) ignored += ", станций";
        if (newData.linkCount > 0) ignored += ", перегонов";
        if (newData.locoRegionCount > 0) ignored += ", тяговых плеч";
        if (newData.lineCount > 0) ignored += ", станционных путей";
        if (newData.trainInfoCount > 0) ignored += ", аттрибутов поездов";
        if (newData.locoAttrCount > 0) ignored += ", аттрибутов локомотивов";
        if (newData.teamAttrCount > 0) ignored += ", аттрибутов бригад";
        if (newData.fLocoCount > 0) ignored += ", локомотивных фактов";
        if (newData.fLocoNextServiceCount > 0) ignored += ", фактов о ТО";
        if (newData.fTeamNextRestCount > 0) ignored += ", фактов об отдыхе";
        if (newData.primaryMessageCount > 0) {
            if (! ignored.equals(""))
                ignored = " (в т. ч." + ignored.substring(1) + ")";
            LOGGER().warn("Переданные определения" + ignored +
                          " не могут быть учтены при перепланировании");
        }

        this.teamPassList = newData.getTeamPassList();
        this.pinnedTeamList = newData.getPinnedTeamList();
    }

    public Long getCurrentTime() {
        return currentTime;
    }

    public Long getCurrentIdTime() {
        return currentIdTime;
    }

    public Long getCurrentIdOrd() {
        return currentIdOrd;
    }

    public Map<Long, LocoRegion> getLocoRegions() {
        return locoRegions;
    }

    public Map<Long, Station> getStations() {
        return stations;
    }

    public Map<Long, Station> getPriorityStations() {
        return priorityStations;
    }

    public Map<String, Long> getStationNameToIdMap() {
        return stationNameToIdMap;
    }

    public Map<StationPair, Link> getLinks() {
        return links;
    }

    public Map<Long, Slot> getSlots() {
        return slots;
    }

    public Map<Long, Slot> getSlots_pass() {
        return slots_pass;
    }

    public Map<Long, Task> getTasks() {
        return tasks;
    }

    public Map<Long, FactTrain> getFactTrains() {
        return factTrains;
    }

    public Map<Long, TrainState> getTrainStates() {
        return trainStates;
    }

    public Map<Long, TrainCategory> getTrainCategories() {
		return trainCategories;
	}

	public Map<Long, FactLoco> getFactLocos() {
        return factLocos;
    }

    public Map<Long, FactTeam> getFactTeams() {
        return factTeams;
    }

    public Map<Long, TeamRegion> getTeamWorkRegions() {
        return teamWorkRegions;
    }

    public Map<Long, OneTask> getOneTasks() {
        return oneTasks;
    }

    public ShortestPath getShortestPath() {
        return shortestPath;
    }

    public Map<String, Number> getConfig() {
        return config;
    }

    public void setCurrentTime(Long currentTime) {
        this.currentTime = currentTime;
    }

    public void setCurrentIdTime(Long currentIdTime) {
        this.currentIdTime = currentIdTime;
    }

    public void setCurrentIdOrd(Long currentIdOrd) {
        this.currentIdOrd = currentIdOrd;
    }

    // by ID
    public final Station getStationById(Long id) {
        Station station = stations.get(id);
        if (station == null) {
            ArrayList<LocoRegion> regions = new ArrayList<>();
            HashMap<Long, Long> service = new HashMap<>();
            station = new Station(id, regions, service);
            stations.put(id, station);
        }
        return station;
    }

    // by ID
    public final FactTrain getFactTrainById(Long id) {
        FactTrain tf = factTrains.get(id);
        if (tf == null) {
            tf = new FactTrain(id);
            factTrains.put(id, tf);
        }
        return tf;
    }

    public final FactLoco findLocoWithTrain(Long trainId) {
        for (FactLoco loco : factLocos.values()) {
            if (trainId.equals(loco.getTrainId())) return loco;
        }
        return null;
    }

    public final void addStation(Station station) {
        Long id = station.getId();
        Station extant = stations.get(id);
        if (extant == null) {
            stations.put(id, station);
        } else {
            List<LocoRegion> regions = station.getRegions();
            List<LocoRegion> extantRegions = extant.getRegions();
            assert regions != null && extantRegions != null;
            for (LocoRegion region : regions) {
                if (! extantRegions.contains(region)) {
                    extantRegions.add(region);
                }
            }
            HashMap<Long, Long> service = station.getServiceAvailable();
            HashMap<Long, Long> extantService = extant.getServiceAvailable();
            if (service != null) {
                if (extantService == null) {
                    extant.setServiceAvailable(service);
                } else {
                    extantService.putAll(service);
                }
            }
        }
    }

    public final void delStation(Station station) {
        Station extant = stations.get(station.getId());
        if (extant != null) {
            List<LocoRegion> regions = station.getRegions();
            List<LocoRegion> extantRegions = extant.getRegions();
            assert regions != null && extantRegions != null;
            for (LocoRegion region : regions) {
                extantRegions.remove(region);
            }
            if (extantRegions.isEmpty()) {
                stations.remove(extant);
            }
            HashMap<Long, Long> service = station.getServiceAvailable();
            HashMap<Long, Long> extantService = extant.getServiceAvailable();
            if (service != null && extantService != null) {
                for (Long type : service.keySet()) extantService.remove(type);
            }
        }
    }

    public final void mapStationName(Station station, String name) {
        station.setName(name);
        stationNameToIdMap.put(name, station.getId());
    }

    public final Link getLinkByStationPair(StationPair stp) {
        Link link = links.get(stp);
        if (link == null) {
            int dist = random.nextInt(100);
            int spd = random.nextInt(36) + 36;
            link = new Link(getStationById(stp.getStationFromId()),
                            getStationById(stp.getStationToId()),
                            (3600L * dist) / spd, dist, false);
            links.put(stp, link);
            shortestPath.addLink(link);
        }
        return link;
    }

    private Map<LocoRegion, List<Link>> regionLinks = null;

    private Map<LocoRegion, List<Link>> ensureRegionLinks() {
        if (regionLinks == null) {
            regionLinks = new HashMap<>();
            for (Link link : links.values()) {
                List<LocoRegion> fromRegions = link.getFrom().getRegions();
                List<LocoRegion> toRegions = link.getTo().getRegions();
                assert fromRegions != null && toRegions != null;
                for (LocoRegion region : fromRegions) {
                    if (toRegions.contains(region)) {
                        List<Link> siblings = regionLinks.get(region);
                        if (siblings == null) {
                            siblings = new ArrayList<>();
                            regionLinks.put(region, siblings);
                        }
                        siblings.add(link);
                    }
                }
            }
        }
        return regionLinks;
    }

    public final List<Link> getLinksByLocoRegion(LocoRegion region) {
        return ensureRegionLinks().get(region);
    }

    public final void addLink(Link link) {
        StationPair stp = StationPair.specifier(link);
        Link extant = links.get(stp);
        if (extant == null) {
            links.put(stp, link);
            shortestPath.addLink(link);
        } else {
            extant.setFrom(link.getFrom());
            extant.setTo(link.getTo());
            extant.setDirection(link.getDirection());
            extant.setDefaultDuration(link.getDefaultDuration());
            extant.setDistance(link.getDistance());
            extant.setPush(link.isPush());
            shortestPath.addLink(extant);
        }
    }

    public final void delLink(Link link) {
        links.remove(StationPair.specifier(link));
        shortestPath.delLink(link);
    }


    public final LocoRegion getLocoRegionById(Long id) {
        LocoRegion region = locoRegions.get(id);
        if (region == null) {
            region = new LocoRegion(id,
                    new ArrayList<SeriesPair>(),
                    new ArrayList<Long>());
            locoRegions.put(id, region);
        }
        return region;
    }

    public final TeamRegion getTeamWorkRegionById(Long id) {
        TeamRegion region = teamWorkRegions.get(id);
        if (region == null) {
            region = new TeamRegion(id, new ArrayList<StationPair>(), -1L, -1L);
            teamWorkRegions.put(id, region);
        }
        return region;
    }

    public final TeamRegion getTeamServiceRegionById(Long id) {
        TeamRegion region = teamServiceRegions.get(id);
        if (region == null) {
            region = new TeamRegion(id, new ArrayList<StationPair>(), -1L, -1L);
            teamServiceRegions.put(id, region);
        }
        return region;
    }
    
    public final TrainCategory getTrainCategoryById(Long id) {
    	TrainCategory category = trainCategories.get(id);
        if (category == null) {
        	category = new TrainCategory(id);
        	trainCategories.put(id, category);
        }
        return category;
    }

    public final void addSlot(Slot slot) {
        slots.put(slot.getSlotId(), slot);
    }

    public final void delSlot(Slot slot) {
        slots.remove(slot.getSlotId());
    }

    public final void addPassSlot(Slot slot) {
        slots_pass.put(slot.getSlotId(), slot);
    }

    public final void delPassSlot(Slot slot) {
        slots_pass.remove(slot.getSlotId());
    }

    public final void addLocoTonnage(LocoTonnage tonnage) {
        Link link = getLinkByStationPair(StationPair.specifier(tonnage));
        link.addLocoTonnage(tonnage);
    }

    public final void delLocoTonnage(LocoTonnage tonnage) {
        Link link = links.get(StationPair.specifier(tonnage));
        if (link != null) link.delLocoTonnage(tonnage);
    }

    public final void addFactLoco(FactLoco loco) {
        factLocos.put(loco.getId(), loco);
    }

    public final void delFactLoco(FactLoco loco) {
        factLocos.remove(loco.getId());
    }

    public final void addFactTrain(FactTrain train) {
        factTrains.put(train.getId(), train);
    }

    public final void addFictitousFactTrain(FactTrain train) {
        fictitousFactTrainsFromTasks.put(train.getId(), train);
    }

    public final void delFactTrain(FactTrain train) {
        factTrains.remove(train.getId());
    }

    public final void addTrainState(TrainState state) {
        trainStates.put(state.getId(), state);
    }

    public final void delTrainState(TrainState state) {
        trainStates.remove(state.getId());
    }
    
    public final void addTrainCategory(TrainCategory category) {
    	trainCategories.put(category.getCatId(), category);
    }
    
    public final void delTrainCategory(TrainCategory category) {
    	trainCategories.remove(category.getCatId());
    }

    public final FactLoco getFactLocoById(Long id) {
        FactLoco loco = factLocos.get(id);
        if (loco == null) {
            loco = new FactLoco(id, currentTime, null, null);
            factLocos.put(id, loco);
        }
        return loco;
    }

    public final void addPriorityStation(Station s){
        priorityStations.put(s.getId(), s);
    }

    public final void delPriorityStation(Station s){
        priorityStations.remove(s.getId());
    }

    public final void addTask(Task t){
        tasks.put(t.getId(), t);
    }

    public final void delTask(Task t){
        tasks.remove(t);
    }

    public final void addTrainFact(FactTrain t){
       factTrains.put(t.getId(), t);
    }

    public final void delTrainFact(FactTrain t){
        factTrains.remove(t);
    }

    public void setOneTasks(Map<Long, OneTask> oneTasks) {
        this.oneTasks = oneTasks;
    }

    public void addOneTask(OneTask task){
        this.oneTasks.put(task.getId(), task);
    }

    public FactTeam getFactTeamById(Long id){
        FactTeam team = factTeams.get(id);
        if (team == null) {
            team = new FactTeam(id, currentTime, null, null);
            factTeams.put(id, team);
        }
        return team;
    }

    public final void addFactTeam(FactTeam team) {
        factTeams.put(team.getId(), team);
    }

    public final void delFactTeam(FactTeam team) {
        factTeams.remove(team.getId());
    }

    public final void setConfigParam(String key, Number value) {
        config.put(key, value);
    }

    public final void setConfigParam(String prefix, String key, Number value) {
        setConfigParam(prefix + "/" + key, value);
    }

    public final Number getConfigParam(String key) {
        Number value = config.get(key);
        if (value == null) {
            throw new RuntimeException("Несуществующий параметр конфигурации: " +
                                           key);
        } else {
            return value;
        }
    }

    public final Number getConfigParam(String prefix, String key) {
        return getConfigParam(prefix + "/" + key);
    }

    public Map<Long, TeamRegion> getTeamServiceRegions() {
        return teamServiceRegions;
    }

    public void setTeamServiceRegions(Map<Long, TeamRegion> teamServiceRegions) {
        this.teamServiceRegions = teamServiceRegions;
    }

    public Map<Station, Map<Long, Line>> getLinesByStation() {
        return linesByStation;
    }

    public void setLinesByStation(Map<Station, Map<Long, Line>> linesByStation) {
        this.linesByStation = linesByStation;
    }

    public void addLine(Line line){
        Station st = line.getStation();
        Map<Long, Line> lines = new HashMap<>();
        if (this.linesByStation.get(st) == null){
            lines.put(line.getId(), line);
            this.linesByStation.put(st, lines);
        } else {
            this.linesByStation.get(st).put(line.getId(), line);
        }
    }

    public void delLine(Line line){
        Station st = line.getStation();
        if (this.linesByStation.get(st) != null){
            if (this.linesByStation.get(st).containsKey(line.getId()))
                this.linesByStation.get(st).remove(line);
        }
    }

    public void addTeamWorkRegion(TeamRegion tr){
        this.teamWorkRegions.put(tr.getId(), tr);
    }

    public void delTeamWorkRegion(TeamRegion tr){
        if (this.teamWorkRegions.containsKey(tr.getId()))
            this.teamWorkRegions.remove(tr);
    }

    public void addTeamServiceRegion(TeamRegion tr){
        this.teamServiceRegions.put(tr.getId(), tr);
    }

    public void delTeamServiceRegion(TeamRegion tr){
        if (this.teamServiceRegions.containsKey(tr.getId()))
            this.teamServiceRegions.remove(tr);
    }

    public List<LocoRelocation> getRelocations() {
        return relocations;
    }

    public void setRelocations(List<LocoRelocation> relocations) {
        this.relocations = relocations;
    }

    public void addRelocation(LocoRelocation r) {
        this.relocations.add(r);
    }

    public Set<WeightType> getWeightTypes() {
        return weightTypes;
    }

    public void setWeightTypes(Set<WeightType> weightTypes) {
        this.weightTypes = weightTypes;
    }

    public void addWeightType(WeightType wt) {
        this.weightTypes.add(wt);
    }

    public void delWeightType(WeightType wt) {
        this.weightTypes.remove(wt);
    }

    public WeightType getWeightTypeById(Long id) {
        for (WeightType wt : weightTypes) {
            if (id.equals(wt.id)) return wt;
        }
        return null;
    }

    public WeightType getWeightTypeByTonnage(Long tonnage) {
        for (WeightType wt : weightTypes) {
            if (tonnage >= wt.minWeight && tonnage < wt.maxWeight) return wt;
        }
        return null;
    }

    public WeightType getMaxWeightType() {
        WeightType wtMax = null;
        for (WeightType wt : weightTypes) {
            if (wtMax == null || wt.minWeight > wtMax.minWeight)
                wtMax = wt;
        }
        return wtMax;
    }

    public long[] countFactTrains(){//считаем сколько пришло на вход arrive, ready, depart
        Long arr = 0L;
        Long ready = 0L;
        Long dep = 0L;

        for (FactTrain train: factTrains.values()){
            if (train.getTrainState() instanceof TrainArrive){
                arr++;
            } else if (train.getTrainState() instanceof TrainReady){
                ready++;
            } else if (train.getTrainState() instanceof TrainDepart){
                dep++;
            }
        }

        long res[] = {arr, ready, dep};
        return res;
    }

    public void calculateAveLinkDuration(){
        for (Slot slot: slots.values()){
            for (SlotTrack track: slot.getRoute().values()){
                Long toId = track.getLink().getTo().getId();
                Long fromId = track.getLink().getFrom().getId();
                Link link = getLinkByStationPair(new StationPair(fromId, toId));
                Long duration = track.getTimeEnd() - track.getTimeStart();
                Date today = new Date(track.getTimeStart() * 1000L);
                SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("hh");
                Integer hour = Integer.valueOf(DATE_FORMAT.format(today));
                hour = (hour/3)*3;
                //Если станция прибытия не является станцией смены бригады (processTime == 0) и время отправления не равно времени прибытия,
                //то учитывать время стоянки во времени хода по линку
                SlotTrack nextTrack = slot.getRoute().get(link.getTo());
                if (link.getTo().getProcessTime().equals(0L) || link.getTo().getNormTime().equals(0L)){   //не является станцией смены бригады или локомотива
                    if (nextTrack != null)
                        if (nextTrack.getTimeStart() != track.getTimeEnd())// но присутствует технологическая для разъезда стоянка
                            duration +=  nextTrack.getTimeStart() - track.getTimeEnd();
                }
                link.getDurationMap().get(hour).add(new AveDurationSlotWrapper(duration, slot));
            }
        }
    }

    public List<TeamPass> getTeamPassList() {
        return teamPassList;
    }

    public void setTeamPassList(List<TeamPass> teamPassList) {
        this.teamPassList = teamPassList;
    }

    public void addTeamPass(TeamPass teamPass){
        this.teamPassList.add(teamPass);
    }

    public void delTeamPass(TeamPass teamPass){
        if (this.teamPassList.contains(teamPass))
            this.teamPassList.remove(teamPass);
    }

    public List<PinnedTeam> getPinnedTeamList() {
        return pinnedTeamList;
    }

    public void setPinnedTeamList(List<PinnedTeam> pinnedTeamList) {
        this.pinnedTeamList = pinnedTeamList;
    }

    public void addPinnedTeam(PinnedTeam pinnedTeam){
        this.pinnedTeamList.add(pinnedTeam);
    }

    public void delPinnedTeam(PinnedTeam pinnedTeam){
        if (this.pinnedTeamList.contains(pinnedTeam))
            this.pinnedTeamList.remove(pinnedTeam);
    }

    public Map<String, Number> getConfigFromVector() {
        return configFromVector;
    }

    public TeamLocoSummaryData geteData() {
        return eData;
    }

    public int getTrainDepartWitMissingLocoCount(){
        /*
        c.	Количество сообщений train_depart, для которых вообще нет сообщения fact_loco о
        соответствующем локомотиве (см. раздел 2.2, п.8, подпункт 1).
         */
        int n = 0;

        for (FactTrain fTrain: getFactTrains().values()){
            if (fTrain.getTrainState() instanceof TrainDepart){
                boolean found = false;
                for(FactLoco fLoco : getFactLocos().values()) {
                    try {
                        if (fTrain.getId().equals(fLoco.getTrainId())) {
                            found = true;
                            break;
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

                if (!found) {
                    n++;
                }
            }
        }

        return n;
    }

    public int getCorruptedTrainDepartCount(){
        /*
        d.	Количество сообщений train_depart, для которых есть сообщение fact_loco, но факт о локомотиве или
        другого типа, или с другим местоположением, или с другим временем операции (см. раздел 2.2, п.8, подпункты 2-4)
         */
        int n = 0;

        for (FactTrain fTrain: getFactTrains().values()){
            if (fTrain.getTrainState() instanceof TrainDepart){
                for(FactLoco fLoco : getFactLocos().values()) {
                    try {
                        if (fTrain.getId().equals(fLoco.getTrainId())) {
                            if (fLoco.getTrack() == null) //не совпадает тип фактов
                                n++;
                            else {
                                if (!fLoco.getTrack().getTimeDepart().equals(fTrain.getTrainState().getTime())) {
                                    //не совпадает время фактов
                                    n++;
                                }
                                Link link1 = ((TrainDepart) fTrain.getTrainState()).getLink();
                                Link link2 = fLoco.getTrack().getLink();
                                if (!link1.equals(link2)) {
                                    n++;
                                }
                                break;
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        return n;
    }

    public int getFactLocoWithTrainCount(){
        /*
        f.	Количество сообщений fact_loco о локомотивах в пути и локомотивах на станции с поездом,
         у которых заполнен атрибут train.
         */
        int n = 0;

        for (FactLoco fLoco: factLocos.values()){
            if (fLoco.getTrack() != null)
                n++;
            if (fLoco.getLocoArrive()!= null)
                n++;
        }

        return n;
    }

    public int getFactLocosWithMissingTrainCount(){
        /*
        g.	Количество сообщений fact_loco о локомотивах в пути и локомотивах на станции с поездом,
        у которых заполнен атрибут train, но нет сообщения train_depart, train_arrive или train_ready
        об операции с этим поездом (см. раздел 2.3, п.6, подпункт 1 и п.7, подпункт 1).
         */
        int n = 0;

        for (FactLoco fLoco: getFactLocos().values()){
            if (fLoco.getTrack() != null){ //локомотив находится на перегоне
                Long trainId = fLoco.getTrack().getTrainId();
                if (trainId == null)
                    continue;
                FactTrain fTrain = getFactTrains().get(trainId);
                if (fTrain == null) {
                    n++;
                }
            }
        }

        return n;
    }

    public int getCorruptedFactLocosCount(){
        /*
        h.	Количество сообщений fact_loco о локомотивах в пути и локомотивах на станции с поездом,
        у которых заполнен атрибут train и есть сообщение train_depart, train_arrive или train_ready
        об этом поезде, но не совпадают либо типы фактов, либо местоположения локомотива и поезда,
        либо время операции (см. раздел 2.3, п.6, подпункты 2-4 и п.7, подпункты 2-3).
         */
        int n = 0;

        for (FactLoco fLoco: getFactLocos().values()){
            if (fLoco.getTrack() != null){ //локомотив находится на перегоне
                Long trainId = fLoco.getTrack().getTrainId();
                if (trainId == null)
                    continue;
                FactTrain fTrain = getFactTrains().get(trainId);
                if (fTrain != null) {
                    Long locoTime = fLoco.getTrack().getTimeDepart();
                    Long trainTime = fTrain.getTrainState().getTime();
                    TrainState state = fTrain.getTrainState();

                    if (!(state instanceof TrainDepart)){
                        n++;
                    } else {
                        TrainDepart trainDepart = (TrainDepart) state;
                        if (!trainDepart.getLink().equals(fLoco.getTrack().getLink())){
                            n++;
                        }
                        if (!locoTime.equals(trainTime)) {
                            n++;
                        }
                    }
                }
            }
        }

        return n;
    }

    public int getFactLocosOnTrackCount(){
        /*
        i.	Количество сообщений fact_loco, для которых передано местоположение – на перегоне (см. раздел 2.3, п.8).
         */
        int n = 0;

        for (FactLoco fLoco: getFactLocos().values()){
            if (fLoco.getTrack() != null){ //локомотив находится на перегоне
                n++;
            }
        }

        return n;
    }

    public int getFactLocosWithMissingTeamsCount(){
        /*
        j.	Количество сообщений fact_loco, для которых передано местоположение «на перегоне» и нет сообщения
        fact_team о бригаде со ссылкой на этот локомотив (см. раздел 2.3, п.8, подпункт 1).
         */
        int n = 0;

        for (FactLoco factLoco: factLocos.values()){
            boolean found = false;
            if (factLoco.getTrack() != null) {
                for (FactTeam factTeam: factTeams.values()) {
                    if (factTeam.getTrack() != null && factTeam.getTrack().getLocoId().equals(factLoco.getId())){
                        found = true;
                        break;
                    }
                }
                if (!found)
                    n++;
            }
        }

        return n;
    }

    public int getFactLocosWithIncorrectTeamsCount(){
        /*
        k.	Количество сообщений fact_loco, для которых передано местоположение «на перегоне», есть сообщение
        fact_team о бригаде со ссылкой на этот локомотив, но местоположение бригады отличается
        по типу, участку планирования или времени операции (см. раздел 2.3, п.8, подпункты 2-4).
         */
        int n = 0;

        for (FactLoco fLoco: factLocos.values()){
            if (fLoco.getTrack() != null) {
                for (FactTeam fTeam: factTeams.values()) {
                    if (fTeam.getTrack() != null && fTeam.getTrack().getLocoId().equals(fLoco.getId())){
                        Long teamTime = fTeam.getTrack().getDepartTime();
                        if (!fLoco.getTrack().getLink().equals(fTeam.getTrack().getLink()))
                                n++;
                        Long locoTime = fLoco.getTrack().getTimeDepart();
                        if (!locoTime.equals(teamTime))
                                n++;
                        break;
                    }
                }
            }
        }

        return n;
    }

    public int getFactTeamsWithLocoCount(){
        /*
        m.	Количество сообщений fact_team, для которых есть ссылка на локомотив
        (местоположение бригады – на перегоне или на станции с локомотивом).
         */
        int n = 0;

        for (FactTeam fTeam: factTeams.values()){
            if (fTeam.getTrack() != null || fTeam.getTeamArrive() != null)
                n++;
        }

        return  n;
    }

    public int getFactTeamsWithMissingLocosCount(){
        /*
        n.	Количество сообщений fact_team, для которых есть ссылка на локомотив, но вообще нет сообщения
        fact_loco для этого локомотива (см. раздел 2.4, п.6, подпункт 1 и п.7, подпункт 1).
         */
        int n = 0;

        for (FactTeam fTeam: factTeams.values()){
            Long locoId = 0L;
            if (fTeam.getTrack() != null)
                locoId = fTeam.getTrack().getLocoId();
            if (fTeam.getTeamArrive() != null)
                locoId = fTeam.getTeamArrive().getId();
            if (!locoId.equals(0L) && factLocos.get(locoId) == null)
                n++;
        }

        return n;
    }

    public int getFactTeamsWithIncorrectLocosCount(){
        /*
        o.	Количество сообщений fact_team, для которых есть ссылка на локомотив и есть сообщение
        fact_loco о соответствующем локомотиве, но оно отличается по типу операции, местоположению
        или времени операции (см. раздел 2.4, п.6, подпункты 2-4 и п.7, подпункты 2-3).
         */
        int n = 0;

        for (FactTeam fTeam: getFactTeams().values()){
            if (fTeam.getTrack() != null){ //бригада находится на перегоне
                Long locoId = fTeam.getTrack().getLocoId();
                FactLoco fLoco = getFactLocos().get(locoId);
                if (fLoco != null) {
                    Long teamTime = fTeam.getTrack().getDepartTime();
                    if (fLoco.getTrack() == null){
                        n++;
                    } else {
                        if (!fLoco.getTrack().getLink().equals(fTeam.getTrack().getLink())){
                            n++;
                        }
                        Long locoTime = fLoco.getTrack().getTimeDepart();
                        if (!locoTime.equals(teamTime)) {
                            n++;
                        }
                    }
                }
            }
        }

        return n;
    }

    public Map<Long, FactTrain> getFictitousFactTrainsFromTasks() {
        return fictitousFactTrainsFromTasks;
    }
}
