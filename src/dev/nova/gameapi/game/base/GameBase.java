package dev.nova.gameapi.game.base;

import dev.nova.gameapi.GAPIPlugin;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.lobby.Lobby;
import dev.nova.gameapi.game.base.lobby.core.LobbyBase;
import dev.nova.gameapi.game.base.mysql.DatabaseConnection;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class GameBase {

    private static final Random RANDOM = new Random();

    private final String displayName;
    private final String codeName;
    private final Map<String, ArrayList<GameInstance>> runningInstances;
    private final ArrayList<GameMap> gameMaps;
    private final ChatColor gameTheme;
    private final Map<String, Class<? extends GameInstance>> instances;
    private final Plugin plugin;
    private final YamlConfiguration lobbyConfiguration;
    private final ArrayList<Lobby> lobbyInstances;
    private final String[] databasesRequired;
    private final DatabaseConnection[] databases;
    private LobbyBase lobbyBase;
    private boolean lobbySupport;
    private final File lobbyFile;

    public GameBase(Plugin plugin, String codeName, String displayName, Class<? extends GameInstance>[] gameInstances, ChatColor gameTheme, boolean lobbySupport,String[] databases) {
        this.runningInstances = new HashMap<>();
        this.databasesRequired = databases;
        this.lobbySupport = lobbySupport;
        this.codeName = codeName;
        this.plugin = plugin;
        this.displayName = displayName;
        this.gameMaps = new ArrayList<>();
        this.lobbyInstances = new ArrayList<Lobby>();
        this.gameTheme = gameTheme;
        this.instances = new HashMap<>();
        this.lobbyFile = new File(Files.getGameFolder(codeName), "lobby.yml");
        this.databases = loadDatabases();
        this.lobbyConfiguration = new YamlConfiguration();

        if (lobbySupport) {
            if (!lobbyFile.exists()) {
                try {
                    lobbyFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    this.lobbySupport = false;
                }
            }
            try {
                lobbyConfiguration.load(lobbyFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
                this.lobbySupport = false;
            }
            World world = loadLobbyWorld();
            String[] spawnData = loadSpawnData();

            if (world != null && spawnData != null) {
                this.lobbyBase = new LobbyBase(this, world, spawnData);
            } else {
                this.lobbySupport = false;
            }
        }

        if (this.lobbySupport) {
            createLobbyInstance();
        }

        for (Class<? extends GameInstance> instance : gameInstances) {
            String code = getCode(instance);
            if (code != null) {
                instances.put(code, instance);
                GameLogger.log(new GameLog(this, LogLevel.INFO, "Added game instance: " + code + " to the game!", true));
                continue;
            }
            GameLogger.log(new GameLog(this, LogLevel.ERROR, "Unable to load game instance: " + instance.getSimpleName() + " as it does not contain a public static final String field named 'code'", true));
        }
    }

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

    public GameBase(Plugin plugin, String codeName, String displayName, Class<? extends GameInstance>[] gameInstances, ChatColor gameTheme, boolean lobbySupport){
        this(plugin,codeName,displayName,gameInstances,gameTheme,lobbySupport,new String[0]);
    }

    private String[] loadSpawnData() {
        ConfigurationSection spawnConfiguration = lobbyConfiguration.getConfigurationSection("spawn-data");

        if (spawnConfiguration == null) {
            this.lobbySupport = false;
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Spawn data configuration section is missing, for game: " + codeName + ", lobby support has been disabled!");
            return null;
        }

        String[] spawnData = new String[5];

        if (spawnConfiguration.contains("x") && spawnConfiguration.contains("y") && spawnConfiguration.contains("z")
                && spawnConfiguration.contains("yaw") && spawnConfiguration.contains("pitch")) {


            spawnData[0] = ((Double) spawnConfiguration.getDouble("x")).toString();
            spawnData[1] = ((Double) spawnConfiguration.getDouble("y")).toString();
            spawnData[2] = ((Double) spawnConfiguration.getDouble("z")).toString();

            spawnData[3] = String.valueOf(Float.parseFloat(((Double) spawnConfiguration.getDouble("yaw")).toString()));
            spawnData[4] = String.valueOf(Float.parseFloat(((Double) spawnConfiguration.getDouble("pitch")).toString()));
        }
        return spawnData;
    }

    public ArrayList<Lobby> getLobbyInstances() {
        return lobbyInstances;
    }

    public Lobby createLobbyInstance() {
        if (lobbySupport) {
            Lobby lobby = new Lobby(lobbyBase, lobbyInstances.size() + 1);
            lobbyInstances.add(lobby);
            return lobby;
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Game: " + codeName + " invoked \"createLobbyInstance\" when lobby support was disabled!");
            return null;
        }
    }

    public void moveToLobby(GamePlayer player) {
        if (lobbySupport) {
            if (player.isInGame()) {
                if (player.getGame().getPlayers().contains(player) || player.getGame().getSpectators().contains(player)) {
                    player.getGame().leave(player, false);
                }

                Lobby lobby = getLobby(1);

                if (lobby == null) {
                    player.getPlayer().sendMessage("§cCould not find any lobby!");
                    return;
                }

                lobby.move(player);
            }
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Game: " + codeName + " invoked \"moveToLobby\" when lobby support was disabled!");
            player.getPlayer().sendMessage("§cAttempted to move you to a lobby but lobby support for game: " + codeName + " is disabled!");
        }
    }

    public Lobby getLobby(int placesNeeded) {
        GAPIPlugin plugin = GAPIPlugin.getPlugin(GAPIPlugin.class);
        int slots = plugin.getConfig().contains("max-lobby-players") ? plugin.getConfig().getInt("max-lobby-players") : 120;
        if (lobbySupport) {
            for (Lobby lobby : lobbyInstances) {

                if (lobby.getWorld().getPlayers().size() + placesNeeded <= slots) {
                    return lobby;
                }
            }
            return null;
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Game: " + codeName + " invoked \"getLobby\" when lobby support was disabled!");
            return null;
        }
    }

    public File getLobbyFile() {
        return lobbyFile;
    }

    public LobbyBase getLobbyBase() {
        return lobbyBase;
    }

    public YamlConfiguration getLobbyConfiguration() {
        return lobbyConfiguration;
    }

    private World loadLobbyWorld() {
        String name = lobbyConfiguration.getString("world-folder");

        if (name == null) {
            lobbySupport = false;
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Bukkit world folder name is missing, for game: " + codeName + ", lobby support has been disabled!");
            return null;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), name);

        if (!worldFolder.exists()) {
            lobbySupport = false;
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Bukkit world folder does not exist, for game: " + codeName + ", lobby support has been disabled!");
            return null;
        }

        WorldCreator creator = new WorldCreator(name);
        World newWorld = creator.createWorld();

        newWorld.setAutoSave(true);
        newWorld.setGameRuleValue("doDaylightCycle", "false");
        newWorld.setGameRuleValue("doWeatherCycle", "false");
        newWorld.setDifficulty(Difficulty.EASY);
        newWorld.setTime(1000);
        newWorld.setGameRuleValue("announceAdvancements", "false");
        newWorld.setThundering(false);
        newWorld.setStorm(false);
        newWorld.setWeatherDuration(-1);

        return newWorld;
    }

    public boolean hasLobbySupport() {
        return lobbySupport;
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

    public ArrayList<GameMap> getGameMaps() {
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
    @Nullable
    public GameMap getMap(String mapName) {
        for (GameMap map : gameMaps) {
            if (map.getCodeName().equalsIgnoreCase(mapName)) return map;
        }
        return null;
    }

    public GameMap random() {
        return gameMaps.get(RANDOM.nextInt(gameMaps.size()));
    }

    /**
     * @param map The map to create the instnace on. (can be null)
     * @return A new game instance.
     */
    @OverridingMethodsMustInvokeSuper
    public GameInstance newInstance(String codeName, @Nullable GameMap map, GamePlayer... initPlayers) {
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

    public void addMap(GameMap map) {
        gameMaps.add(map);
    }

    public void removeInstance(GameInstance gameInstance) {
        ArrayList<GameInstance> instances = getRunningInstances().get(getCode(gameInstance.getClass()));
        instances.remove(gameInstance);
    }

    public void moveToLobby(GamePlayer player, boolean b) {
        if (lobbySupport) {

            if (b && !player.isInGame()) {
                return;
            }

            if (b) {
                if (player.getGame().getPlayers().contains(player) || player.getGame().getSpectators().contains(player)) {
                    player.getGame().leave(player, false);
                }
            }

            Lobby lobby = getLobby(1);

            if (lobby == null) {
                player.getPlayer().sendMessage("§cCould not find any lobby!");
                return;
            }

            lobby.move(player);
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Game: " + codeName + " invoked \"moveToLobby\" when lobby support was disabled!");
            player.getPlayer().sendMessage("§cAttempted to move you to a lobby but lobby support for game: " + codeName + " is disabled!");
        }
    }
}

