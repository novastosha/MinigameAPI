package dev.nova.gameapi.game.base;

import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.player.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public abstract class GameBase {

    private static final Random RANDOM = new Random();

    private final Class<? extends GameInstance> instanceClass;
    private final String displayName;
    private final String codeName;
    private final ArrayList<GameInstance> runningInstances;
    private final ArrayList<GameMap> gameMaps;
    private final ChatColor gameTheme;

    public GameBase(String codeName, String displayName, Class<? extends GameInstance> gameInstanceClass, ChatColor gameTheme){
        this.instanceClass = gameInstanceClass;
        this.runningInstances = new ArrayList<>();
        this.codeName = codeName;
        this.displayName = displayName;
        this.gameMaps = new ArrayList<>();
        this.gameTheme = gameTheme;
    }

    public ChatColor getGameTheme() {
        return gameTheme;
    }

    public Class<? extends GameInstance> getInstanceClass() {
        return instanceClass;
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
    public GameInstance newInstance(@Nullable GameMap map, GamePlayer... initPlayers) {
        try {
            GameInstance instance = instanceClass.getConstructor(String.class, GameMap.class).newInstance(this.getCodeName(),map);
            runningInstances.add(instance);

            for(GamePlayer player : initPlayers){
                instance.join(player);
            }

            GameLogger.log(new GameLog(this,LogLevel.INFO,"Created new instance of game: "+getCodeName()+" with id: "+instance.getGameID(),true));
            return instance;
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

