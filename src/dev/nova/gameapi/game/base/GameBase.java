package dev.nova.gameapi.game.base;

import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import dev.nova.gameapi.GameAPI;
import dev.nova.gameapi.game.base.init.InitResultInfo;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.lobby.Lobby;
import dev.nova.gameapi.game.base.lobby.LobbyBase;
import dev.nova.gameapi.game.base.lobby.mode.LobbyInfo;
import dev.nova.gameapi.game.base.lobby.mode.LobbyMode;
import dev.nova.gameapi.game.base.mysql.DatabaseConnection;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.map.options.instance.GameOptionInfo;
import dev.nova.gameapi.game.map.swm.SlimeFileLoader;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

import static dev.nova.gameapi.game.manager.GameManager.createMissingOptionsLog;

public abstract class GameBase {

    private static final Random RANDOM = new Random();

    protected final String displayName;
    protected final String codeName;
    protected final Map<String, ArrayList<GameInstance>> runningInstances;
    protected final Map<String,ArrayList<GameMap>> gameMaps;
    protected final Map<String,ArrayList<GameMap>> editingGameMaps;
    protected final ChatColor gameTheme;
    protected final Map<String, Class<? extends GameInstance>> instances;
    protected final Plugin plugin;
    protected final String[] databasesRequired;
    protected final DatabaseConnection[] databases;
    private Class<? extends Lobby> lobbyImplementation;
    private boolean lobbySupport;
    private final LobbyManager lobbyManager;

    public static class GameLayoutManager {

        private final Class<? extends GameInstance> instance;

        private GameLayoutManager(Class<? extends GameInstance> instance){
            this.instance = instance;
        }

        public static class Layout {

            public static class Item {
                public final Slot slotType;
                public final int slot;
                public final ItemStack item;

                public enum Slot {
                    HELMET,
                    CHESTPLATE,
                    LEGGINGS,
                    BOOTS,
                    SLOT
                }

                private Item(Slot slot, int slotI,ItemStack stack){
                    this.slotType = slot;
                    this.slot = slotI;
                    this.item = stack;
                }

                public Item(Slot slot, ItemStack stack){
                    this(slot,-1,stack);
                    if(slot.equals(Slot.SLOT)){
                        throw new UnsupportedOperationException("Use Item("+int.class+","+ItemStack.class+") instead.");
                    }
                }

                public Item(int slot, ItemStack stack){
                    this(Slot.SLOT,slot,stack);
                }
            }

            private final ArrayList<Item> items;
            private final String name;

            public Item getItem(int slot) {
                for(Item item :items){
                    if(item.slotType != Item.Slot.SLOT) continue;

                    if(item.slot == slot){
                        return item;
                    }
                }
                return null;
            }

            public Item getItem(Item.Slot slot) {
                if(slot.equals(Item.Slot.SLOT)) return null;

                for(Item item : items){
                    if(item.slotType == Item.Slot.SLOT) continue;

                    if(slot.equals(item.slotType)) return item;
                }

                return null;
            }

            public Layout(String name){
                this.name = name;
                this.items = new ArrayList<Item>();
            }

            public String getName() {
                return name;
            }


            public void write() {

            }
        }

        public Class<? extends GameInstance> getInstance() {
            return instance;
        }
    }


    public GameBase(Plugin plugin, String codeName, String displayName, Class<? extends GameInstance>[] gameInstances, ChatColor gameTheme, boolean lobbySupport,String[] databases, Class<? extends Lobby> lobbyImplementation) {
        editingGameMaps = new HashMap<>();

        this.runningInstances = new HashMap<>();
        this.databasesRequired = databases;
        this.codeName = codeName;
        this.lobbySupport = lobbySupport;
        this.lobbyImplementation = lobbyImplementation;
        this.plugin = plugin;
        this.displayName = displayName;
        this.gameMaps = new HashMap<>();
        this.gameTheme = gameTheme;
        this.instances = new HashMap<>();
        this.databases = databases.length != 0 ? loadDatabases() : new DatabaseConnection[0];

        if (lobbySupport) {
            this.lobbyManager = new LobbyManager();

        }else{
            this.lobbyManager = null;
        }

        for (Class<? extends GameInstance> instance : gameInstances) {
            String code = getCode(instance);
            if (code != null) {

                try {
                    Method method = instance.getMethod("onInitialize",GameBase.class);

                    GameLogger.log(new GameLog(this,LogLevel.INFO,"Invoking onInitialize on game instance: "+code,true));
                    method.setAccessible(true);
                    Object obj = method.invoke(null,this);
                    if(obj instanceof InitResultInfo info){
                        if(info.getResult().equals(InitResultInfo.Result.FAILED)){
                            GameLogger.log(new GameLog(this,LogLevel.ERROR,"Invoking onInitialize failed on game instance: "+code,true));
                            if(info.getThrowable() != null) info.getThrowable().printStackTrace();
                            continue;
                        }
                    }
                    method.setAccessible(false);

                    Field field = instance.getField("layouts");

                    GameLogger.log(new GameLog(this,LogLevel.INFO,"Making a layout manager for game instance: "+code,true));

                    field.setAccessible(true);

                    GameLayoutManager layoutManager = new GameLayoutManager(instance);

                    field.setAccessible(false);

                } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException ignored) {

                }catch (InvocationTargetException e){
                    GameLogger.log(new GameLog(this,LogLevel.ERROR,"Invoking onInitialize failed on game instance: "+code+" "+e.getClass().getSimpleName(),true));
                    continue;
                }

                instances.put(code, instance);
                gameMaps.put(code,new ArrayList<>());
                editingGameMaps.put(code,new ArrayList<>());
                GameLogger.log(new GameLog(this, LogLevel.INFO, "Added game instance: " + code + " to the game!", true));
                continue;
            }
            GameLogger.log(new GameLog(this, LogLevel.ERROR, "Unable to load game instance: " + instance.getSimpleName() + " as it does not contain a public static final String field named 'code'", true));
        }
    }

    public GameOptionInfo[] getOptionInformation(String instanceName) {

        if(!instances.containsKey(instanceName)) {
            return new GameOptionInfo[0];
        }

        try {
            Field field = getStaticField(instances.get(instanceName),"options");

            field.setAccessible(true);
            Object got = field.get(null);

            if(!(got instanceof GameOptionInfo[])) {
                return new GameOptionInfo[0];
            }

            for(GameOptionInfo info : (GameOptionInfo[]) got) {
                if(info.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.ConfigurationSection configurationSection){
                    configurationSection.make(info);
                }
            }

            return (GameOptionInfo[]) got;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return new GameOptionInfo[0];
        }

    }

    private Field getStaticField(Class<?> aClass, String name) throws NoSuchFieldException {
        try {
            return aClass.getField(name);
        } catch (NoSuchFieldException e) {
            return aClass.getField(name.toUpperCase());
        }
    }

    /**
     *
     * Loads the databases from the databases file.
     *
     * @return The databases.
     */
    private DatabaseConnection[] loadDatabases() {
        if(databasesRequired == null) return new DatabaseConnection[0];
        File databaseFile = Files.getGameDatabasesFile(codeName,true);

        if(!databaseFile.exists()) return new DatabaseConnection[0];

        YamlConfiguration databasesConfiguration = new YamlConfiguration();
        try{
            databasesConfiguration.load(databaseFile);
        }catch (IOException | InvalidConfigurationException e) {
            return new DatabaseConnection[0];
        }

        ArrayList<DatabaseConnection> connections = new ArrayList<>();

        for(String db : databasesRequired){
            ConfigurationSection databaseConfig = databasesConfiguration.getConfigurationSection(db);

            if(databaseConfig == null){
                continue;
            }

            String host = databaseConfig.getString("host");
            int port = databaseConfig.contains("port") ? databaseConfig.getInt("port") : 3306;
            String username = databaseConfig.getString("username");
            String password = databaseConfig.getString("password");
            String database = databaseConfig.contains("database") ? databaseConfig.getString("database") : db;

            if(host == null || username == null || password == null){
                continue;
            }

            DatabaseConnection connection = new DatabaseConnection(host,port,username,password,database,db);

            if(connection.connection == null){
                continue;
            }

            connections.add(connection);
        }

        return connections.toArray(new DatabaseConnection[0]);
    }

    public DatabaseConnection getDatabase(String name){
        for(DatabaseConnection connection : databases){
            if(connection.name.equalsIgnoreCase(name)) return connection;
        }
        return null;
    }

    public GameBase(Plugin plugin, String codeName, String displayName, Class<? extends GameInstance>[] gameInstances, ChatColor gameTheme, boolean lobbySupport, Class<? extends Lobby> lobbyImplementation){
        this(plugin,codeName,displayName,gameInstances,gameTheme,lobbySupport,new String[0],lobbyImplementation);
    }


    public boolean onInitialize() {
        return true;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Map<String, Class<? extends GameInstance>> getInstances() {
        return instances;
    }

    public String getCode(Class<? extends GameInstance> instance) {

        try {
            Field field = instance.getField("code");

            field.setAccessible(true);
            return (String) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    public ChatColor getGameTheme() {
        return gameTheme;
    }

    public Map<String, ArrayList<GameMap>> getGameMaps() {
        return gameMaps;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCodeName() {
        return codeName;
    }

    public Map<String, ArrayList<GameInstance>> getRunningInstances() {
        return this.runningInstances;
    }

    public boolean instanceExists(String instanceCodeName) {
        Class<? extends GameInstance> instanceClass = instances.get(instanceCodeName);

        return instanceClass != null;
    }

    /**
     * @param mapName The name of the map to search for.
     * @return The map with the according name. (can be null if none found)
     */
    public GameMap getMap(String mode,String mapName) {
        for (GameMap map : gameMaps.get(mode)) {
            if (map.getCodeName().equalsIgnoreCase(mapName)) return map;
        }
        return null;
    }

    /**
     * @param mapName The name of the map to search for.
     * @return The map with the according name. (can be null if none found)
     */
    public GameMap getEditingMap(String mode,String mapName) {
        for (GameMap map : editingGameMaps.get(mode)) {
            if (map.getCodeName().equalsIgnoreCase(mapName)) return map;
        }
        return null;
    }

    /**
     *
     * @param mode What maps to choose from.
     * @return A random map for that mode.
     */
    public GameMap randomMap(String mode) {
        try {
            if (gameMaps.get(mode).size() == 1) return gameMaps.get(mode).get(0);
            return gameMaps.get(mode).get(RANDOM.nextInt(gameMaps.get(mode).size()));
        }catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @param map The map to create the instance on. (can be null)
     * @return A new game instance.
     */
    public GameInstance newInstance(String codeName, GameMap map, GamePlayer... initPlayers) {
        try {
            Class<? extends GameInstance> instanceClass = instances.get(codeName);

            if (instanceClass == null) {
                return null;
            }

            try {
                GameInstance instance = instanceClass.getConstructor(String.class, GameMap.class).newInstance(getCodeName(), map);

                ArrayList<GameInstance> instances = runningInstances.get(codeName) == null ? new ArrayList<>() : runningInstances.get(codeName);
                instances.add(instance);

                runningInstances.put(codeName, instances);

                for (GamePlayer player : initPlayers) {
                    instance.join(player);
                }

                GameLogger.log(new GameLog(GameBase.this, LogLevel.INFO, "Created new instance of game: " + getCodeName() + " with id: " + instance.getGameID(), true));
                return instance;
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                GameLogger.log(new GameLog(GameBase.this, LogLevel.ERROR, "Unable to create instance! " + e.getClass().getSimpleName(), true));
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            GameLogger.log(new GameLog(this, LogLevel.ERROR, "Unable to create instance! " + e.getClass().getSimpleName(), true));
            e.printStackTrace();
            return null;
        }
    }

    public void addMap(GameMap map,String mode) {
        gameMaps.get(mode).add(map);
    }

    public void removeInstance(GameInstance gameInstance) {
        ArrayList<GameInstance> instances = getRunningInstances().get(getCode(gameInstance.getClass()));
        instances.remove(gameInstance);
    }

    public boolean moveToLobby(GamePlayer player) {
        if (lobbySupport) {
            return lobbyManager.moveToLobby(player);
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Game: " + codeName + " called \"moveToLobby\" when lobby support was disabled!");
            player.getPlayer().sendMessage(ChatColor.RED+"Attempted to move you to a lobby but lobby support for game: " + codeName + " is disabled!");
            return false;
        }
    }

    public boolean hasLobbySupport(){
        return lobbySupport && lobbyManager != null;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }


    public Map<String, ArrayList<GameMap>> getEditingGameMaps() {
        return editingGameMaps;
    }

    public void addEditingMap(dev.nova.gameapi.game.map.Map map, String mode) {
        if(map instanceof GameMap gameMap){
            editingGameMaps.get(mode).add(gameMap);
        }else{
            lobbyManager.editingLobbies.add((LobbyBase) map);
        }
    }

    public void removeEditingMap(GameMap map, String mode) {
        editingGameMaps.get(mode).remove(map);
    }

    public boolean containsEditingMap(String instance, String map) {
        if(!editingGameMaps.containsKey(instance)) return false;

        for(GameMap map1 : editingGameMaps.get(instance)){
            if(map1.getCodeName().equals(map)) return true;
        }

        return false;
    }

    public GameOptionInfo[] getAdditionalOptionInformation(String instanceCode) {
        GameOptionInfo[] baseInfos = getOptionInformation(instanceCode);

        ArrayList<GameOptionInfo> infos = new ArrayList<>(Arrays.asList(baseInfos));

        for(GameOptionInfo info : baseInfos){
            if(info.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.ConfigurationSection section){
                infos.addAll(section.getAllOptionInfos());
            }
        }

        return infos.toArray(new GameOptionInfo[0]);
    }

    public class LobbyManager {


        public final SlimeLoader SLIME_FILE_LOADER;
        public final HashMap<LobbyBase, SlimeWorld> SLIME_WORLD_MAPS = new HashMap<>();
        public static final SlimePropertyMap GENERIC_PROPERTY_MAP;
        public static final Pattern DATE_PATTERN = Pattern.compile("([1-9]|1[0-2])-([1-9]|1[0-9]|2[0-9]|3[0-1])_([1-9]|1[0-2])-([1-9]|1[0-9]|2[0-9]|3[0-1])\b");

        static {

            GENERIC_PROPERTY_MAP = new SlimePropertyMap();
            GENERIC_PROPERTY_MAP.setString(SlimeProperties.DIFFICULTY, "easy");
            GENERIC_PROPERTY_MAP.setBoolean(SlimeProperties.ALLOW_ANIMALS,false);
            GENERIC_PROPERTY_MAP.setBoolean(SlimeProperties.ALLOW_MONSTERS,false);
            GENERIC_PROPERTY_MAP.setBoolean(SlimeProperties.PVP,false);

        }

        private final ArrayList<LobbyBase> lobbyBases;
        private final ArrayList<Lobby> runningLobbies;
        public final ArrayList<LobbyBase> editingLobbies;

        public ArrayList<LobbyBase> getLobbyBases() {
            return lobbyBases;
        }

        private LobbyBase genericLobby;
        private LobbyBase using;

        public LobbyBase getUsingLobbyBase() {
            return using;
        }

        public LobbyBase getGenericLobby() {
            return genericLobby;
        }

        private LobbyManager() {
            this.lobbyBases = new ArrayList<LobbyBase>();
            this.editingLobbies = new ArrayList<LobbyBase>();
            this.runningLobbies = new ArrayList<Lobby>();

            File lobbiesFolder = Files.getLobbiesFolder(codeName);
            File mapsFolder = Files.getLobbiesMapsFolder(codeName);

            mapsFolder.mkdirs();
            lobbiesFolder.mkdirs();

            this.SLIME_FILE_LOADER = new SlimeFileLoader(mapsFolder);


            GameLogger.log(new GameLog(GameBase.this,LogLevel.INFO,"Loading lobbies...", true));
            loadLobbies();
        }

        public Lobby createInstance() {

            int id = runningLobbies.size()+1;

            if (lobbyImplementation != null) {
                try {
                    Constructor<? extends Lobby> defaultConstructor = lobbyImplementation.getConstructor(LobbyManager.class,LobbyBase.class, World.class);

                    World world = cloneWorld(id);
                    Lobby lobby = defaultConstructor.newInstance(this,using,world);

                    runningLobbies.add(lobby);
                    return lobby;
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    lobbyImplementation = null;
                    GameLogger.log(new GameLog(getGame(),LogLevel.ERROR,"Unable to create lobby with a custom implementation! Using the default instead.",true));
                    return createInstance();
                }
            }

            World world = cloneWorld(id);
            Lobby lobby = new Lobby(this,using,world) {
                @Override
                public void tick() {
                    //Unused
                }
            };

            runningLobbies.add(lobby);
            return lobby;
        }

        private World cloneWorld(int id) {

            SlimeWorld slimeWorldClone = SLIME_WORLD_MAPS.get(using).clone("lobby-" +codeName+"-"+id);
            GameAPI.getSlime().generateWorld(slimeWorldClone);

            return Bukkit.getWorld("lobby-" +codeName+"-"+id);
        }

        public GameBase getGame() {
            return GameBase.this;
        }

        public void close(Lobby lobby) {
            runningLobbies.remove(lobby);
        }

        private void loadLobbies() {
            File lobbiesFolder = Files.getLobbiesFolder(codeName);
            File mapsFolder = Files.getLobbiesMapsFolder(codeName);

            boolean loadedGenericLobby = false;

            for (File file : Objects.requireNonNull(lobbiesFolder.listFiles())) {
                if(file.isFile() && file.getName().endsWith(".yml")) {
                    String fileName = file.getName().replaceFirst(".yml", "");

                    GameLogger.log(new GameLog(GameBase.this, LogLevel.INFO, "Loading lobby: " + fileName, true));

                    LobbyMode mode = LobbyMode.DATE_SPECIFIC;

                    if (fileName.equalsIgnoreCase("generic")) {
                        loadedGenericLobby = true;
                        mode = LobbyMode.GENERIC;
                    }

                    File MC_world = new File(Bukkit.getWorldContainer(), "lobby-" + codeName + "-" + fileName);
                    if (MC_world.exists()) {
                        try {
                            if (!SLIME_FILE_LOADER.worldExists(codeName+"-"+fileName)) {
                                Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Importing lobby: " + fileName + " into Slime Region Format World (SRF).");
                                Bukkit.getServer().getScheduler().runTaskAsynchronously(GameAPI.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            GameAPI.getSlime().importWorld(MC_world, codeName+"-"+fileName, SLIME_FILE_LOADER);
                                        } catch (WorldAlreadyExistsException | InvalidWorldException | WorldTooBigException | IOException | WorldLoadedException e) {
                                            e.printStackTrace();
                                        }
                                        Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] " + fileName + " was successfully imported into a Slime Region Format World (SRF).");
                                        MC_world.delete();

                                    }
                                });

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            if (!SLIME_FILE_LOADER.worldExists(codeName+"-"+fileName)) {
                                Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Unable to load lobby named: " + MC_world.getName() + " because the file name does not match the name of any world (either in slime type or normal)!");
                                continue;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    LobbyInfo info;

                    String name = mode == LobbyMode.GENERIC ? "generic" : fileName;

                    if (mode.equals(LobbyMode.DATE_SPECIFIC)) {
                        GameLogger.log(new GameLog(GameBase.this, LogLevel.INFO, "Attempting to extract date info from: "+fileName,true));

                        if(!DATE_PATTERN.asPredicate().test(fileName)){
                            GameLogger.log(new GameLog(GameBase.this, LogLevel.ERROR, "Unable to extract date information from: "+fileName+" (Malformed name!)",true));
                            continue;
                        }

                        String[] splitRaw = fileName.split("_");

                        String[] firstHalf = splitRaw[0].split("-");
                        String[] secondHalf = splitRaw[1].split("-");

                        LocalDate firstDate = LocalDate.of(LocalDate.now().getYear(), Integer.parseInt(firstHalf[0]),Integer.parseInt(firstHalf[1]));
                        LocalDate secondDate = LocalDate.of(LocalDate.now().getYear(), Integer.parseInt(secondHalf[0]),Integer.parseInt(secondHalf[1]));

                        info = new LobbyInfo(LobbyMode.DATE_SPECIFIC,firstDate,secondDate,name);
                    }else{
                        info = new LobbyInfo(LobbyMode.GENERIC,null,null,name);
                    }

                    YamlConfiguration configuration = new YamlConfiguration();

                    try {
                        configuration.load(file);
                    } catch (IOException | InvalidConfigurationException e) {
                        e.printStackTrace();
                        continue;
                    }

                    ConfigurationSection data = configuration.getConfigurationSection("data");

                    if(data == null) {
                        continue;
                    }

                    if(!data.contains("spawn")){
                        continue;
                    }

                    LobbyBase lobbyBase = new LobbyBase(this,info,data,file);

                    if(mode.equals(LobbyMode.GENERIC)){
                        genericLobby = lobbyBase;
                    }


                    Bukkit.getServer().getScheduler().runTaskAsynchronously(GameAPI.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            try {

                                SlimeWorld world = GameAPI.getSlime().loadWorld(SLIME_FILE_LOADER, codeName+"-"+fileName, false, GENERIC_PROPERTY_MAP);
                                SLIME_WORLD_MAPS.put(lobbyBase,world);

                                boolean editingMode = false;

                                GameOptionInfo[] infos = getLobbyOptionInformation(lobbyBase);

                                ConfigurationSection section = data.getConfigurationSection("options");
                                ArrayList<GameOptionInfo> missingInformation = new ArrayList<>();

                                if(infos.length != 0) {

                                    if(section == null) {
                                        createMissingOptionsLog(GameBase.this,"lobby-"+name,infos,null);
                                        editingMode = true;
                                    }else {

                                        for (GameOptionInfo optionInfo : infos) {
                                            GameOptionInfo.ValidationState valid = optionInfo.isValid(section,false);

                                            if (!valid.equals(GameOptionInfo.ValidationState.VALID)) {
                                                missingInformation.add(optionInfo);
                                                editingMode = true;
                                            }
                                        }
                                    }
                                }

                                if(!missingInformation.isEmpty()){
                                    createMissingOptionsLog(GameBase.this,"lobby-"+name,missingInformation.toArray(new GameOptionInfo[0]),section);
                                }

                                if(!editingMode) {

                                    GameLogger.log(new GameLog(GameBase.this, LogLevel.INFO, "Loaded lobby: " + fileName, false));
                                    lobbyBases.add(lobbyBase);
                                    if(using != null) {
                                        createInstance();
                                    }
                                }else{
                                    addEditingMap(lobbyBase,null);
                                }


                            } catch (UnknownWorldException | CorruptedWorldException | NewerFormatException | WorldInUseException | IOException e) {
                                e.printStackTrace();
                            }
                            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Lobby: " + fileName + " for game: "+codeName+" has been loaded successfully");
                        }
                    });
                }
            }

            if(!loadedGenericLobby){
                GameLogger.log(new GameLog(GameBase.this,LogLevel.ERROR,"Unable to load the generic lobby!",true));
                lobbySupport = false;
                return;
            }

            using = genericLobby;

            for(LobbyBase base : lobbyBases) {
                if(base.getInfo().mode().equals(LobbyMode.GENERIC)) continue;
                
                LocalDate now = LocalDate.now();
                
                if((now.isEqual(base.getInfo().start()) || now.isAfter(base.getInfo().start())) && (now.isEqual(base.getInfo().end()) || now.isBefore(base.getInfo().end()))) {
                    using = base;
                    break;
                }
            }
        }

        public GameOptionInfo[] getLobbyOptionInformation(LobbyBase lobbyBase) {

            if(!lobbyBases.contains(lobbyBase) || !editingLobbies.contains(lobbyBase)) {
                return new GameOptionInfo[0];
            }

            try {
                Field field = getStaticField(lobbyBase.getClass(),"options");

                field.setAccessible(true);
                Object got = field.get(null);

                if(!(got instanceof GameOptionInfo[])) {
                    return new GameOptionInfo[0];
                }

                for(GameOptionInfo info : (GameOptionInfo[]) got) {
                    if(info.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.ConfigurationSection configurationSection){
                        configurationSection.make(info);
                    }
                }

                return (GameOptionInfo[]) got;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                return new GameOptionInfo[0];
            }
        }

        public GameOptionInfo[] getAdditionalLobbyOptionInformation(LobbyBase base) {
            GameOptionInfo[] baseInfos = getLobbyOptionInformation(base);

            ArrayList<GameOptionInfo> infos = new ArrayList<>(Arrays.asList(baseInfos));

            for(GameOptionInfo info : baseInfos){
                if(info.getOptionSpecificInfo() instanceof GameOptionInfo.OptionSpecificInfo.ConfigurationSection section){
                    infos.addAll(section.getAllOptionInfos());
                }
            }

            return infos.toArray(new GameOptionInfo[0]);
        }

        public void shutDownBase(LobbyBase lobbyBase, String reason) {
            GameLogger.log(new GameLog(GameBase.this,LogLevel.ERROR,reason,true));

            if(lobbyBase == genericLobby) return;

            lobbyBases.remove(lobbyBase);

            for(Lobby lobby : runningLobbies) {
                if(lobby.getBase() == lobbyBase) {
                    lobby.close();
                }
            }
        }

        public Lobby getLobby(int placesNeeded) {
            GameAPI plugin = GameAPI.getPlugin(GameAPI.class);
            int slots = plugin.getConfig().contains("max-lobby-players") ? plugin.getConfig().getInt("max-lobby-players") : 120;
            if (hasLobbySupport()) {
                for (Lobby lobby : runningLobbies) {
                    lobby.syncPlayers();
                    if (lobby.getPlayers().size() + placesNeeded <= slots) {
                        return lobby;
                    }
                }
            } else {
                Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Game: " + codeName + " invoked \"getLobby\" when lobby support was disabled!");
            }
            return null;
        }

        public boolean moveToLobby(GamePlayer player) {
            Lobby lobby = getLobby(1);

            if(lobby == null) {
                try{
                    lobby = createInstance();
                }catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            lobby.onJoin(player);
            return true;
        }

        public Lobby[] getRunningLobbies() {
            return runningLobbies.toArray(new Lobby[0]);
        }

        public void addLobby(LobbyBase map) {
            lobbyBases.add(map);
        }

        public void removeLobby(LobbyBase map) {
            lobbyBases.remove(map);
        }

        public LobbyBase getLobbyBaseByName(String arg) {
            for(LobbyBase base : lobbyBases) {
                if(base.getInfo().name().equals(arg)) return base;
            }

            for(LobbyBase base : editingLobbies) {
                if(base.getInfo().name().equals(arg)) return base;
            }

            return null;
        }
    }
}

