package dev.nova.gameapi.game.base;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.team.TeamGameInstance;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class GameBase {

    private static final Random RANDOM = new Random();

    private final String displayName;
    private final String codeName;
    private final ArrayList<GameInstance> runningInstances;
    private final ArrayList<GameMap> gameMaps;
    private final ChatColor gameTheme;
    private final Map<String,Class<? extends GameInstance>> instances;

    public GameBase(String codeName, String displayName, Class<? extends GameInstance>[] gameInstances, ChatColor gameTheme){
        this.runningInstances = new ArrayList<>();
        this.codeName = codeName;
        this.displayName = displayName;
        this.gameMaps = new ArrayList<>();
        this.gameTheme = gameTheme;
        this.instances = new HashMap<>();

        for(Class<? extends GameInstance> instance : gameInstances){
            String code = checkValidation(instance);
            if(code != null){
                instances.put(code,instance);
                GameLogger.log(new GameLog(this,LogLevel.INFO,"Added game instance: "+code+" to the game!",true));
                continue;
            }
            GameLogger.log(new GameLog(this,LogLevel.ERROR,"Unable to load game instance: "+instance.getSimpleName()+" as it does not contain a public static final String field named 'code'",true));
        }
    }

    public Map<String, Class<? extends GameInstance>> getInstances() {
        return instances;
    }

    private String checkValidation(Class<? extends GameInstance> instance) {

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

    public ArrayList<GameInstance> getRunningInstances(){
        return this.runningInstances;
    }

    public boolean instanceExists(String instanceCodeName){
        Class<? extends GameInstance> instanceClass = instances.get(codeName);

        return instanceClass != null;
    }

    /**
     *
     * @param mapName The name of the map to search for.
     * @return The map with the according name. (can be null if none found)
     */
    @Nullable
    public GameMap getMap(String mapName) {
        for(GameMap map : gameMaps){
            if(map.getCodeName().equalsIgnoreCase(mapName)) return map;
        }
        return null;
    }

    public GameMap random(){
        return gameMaps.get(RANDOM.nextInt(gameMaps.size()));
    }

    /**
     *
     * @param map The map to create the instnace on. (can be null)
     * @return A new game instance.
     */
    @OverridingMethodsMustInvokeSuper
    public GameInstance newInstance(String codeName,@Nullable GameMap map, GamePlayer... initPlayers) {
        try {
            Class<? extends GameInstance> instanceClass = instances.get(codeName);

            if(instanceClass == null){
                return null;
            }

            if(!instanceClass.isAssignableFrom(TeamGameInstance.class)) {
                GameInstance instance = instanceClass.getConstructor(String.class, GameMap.class).newInstance(this.getCodeName(), map);
                runningInstances.add(instance);

                for (GamePlayer player : initPlayers) {
                    instance.join(player);
                }

                GameLogger.log(new GameLog(this, LogLevel.INFO, "Created new instance of game: " + getCodeName() + " with id: " + instance.getGameID(), true));
                return instance;
            }else{
                GameInstance instance = instanceClass.getConstructor(String.class, GameMap.class).newInstance(this.getCodeName(), map);
                runningInstances.add(instance);

                for (GamePlayer player : initPlayers) {
                    instance.join(player);
                }

                GameLogger.log(new GameLog(this, LogLevel.INFO, "Created new instance of game: " + getCodeName() + " with id: " + instance.getGameID(), true));
                return instance;
            }
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            GameLogger.log(new GameLog(this, LogLevel.ERROR,"Unable to create instance! "+e.getClass().getSimpleName(),true));
            e.printStackTrace();
            return null;
        }
    }

    public void addMap(GameMap map) {
        gameMaps.add(map);
    }

}

