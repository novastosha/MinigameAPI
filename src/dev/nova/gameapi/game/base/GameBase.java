package dev.nova.gameapi.game.base;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
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

    public GameBase(Plugin plugin, String codeName, String displayName, Class<? extends GameInstance>[] gameInstances, ChatColor gameTheme) {
        this.runningInstances = new HashMap<>();
        this.codeName = codeName;
        this.plugin = plugin;
        this.displayName = displayName;
        this.gameMaps = new ArrayList<>();
        this.gameTheme = gameTheme;
        this.instances = new HashMap<>();

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
}

