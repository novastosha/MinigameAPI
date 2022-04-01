package dev.nova.gameapi.game.map;

import com.grinderwolf.swm.api.world.SlimeWorld;
import dev.nova.gameapi.GameAPI;
import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.options.GameOption;
import dev.nova.gameapi.game.map.options.OptionType;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public final class GameMap implements Map{

    private final GameBase gameBase;
    private final String codeName;
    private final String displayName;
    private final ArrayList<Map> maps;
    private final int playerLimit;
    private final String instanceCode;
    private ConfigurationSection rawData;
    private final File file;

    public GameMap(GameBase gameBase,File configFile,ConfigurationSection data,String instanceCode){
        this.file = configFile;

        this.instanceCode = instanceCode;

        this.displayName = data.getString("display-name");
        this.maps = new ArrayList<>();
        this.codeName = data.getString("code-name");
        this.rawData = data;
        this.playerLimit = data.getInt("player-limit");

        this.gameBase = gameBase;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public static void loadWorld(String name) {
        World world = new WorldCreator(name).createWorld();

        world.setAutoSave(false);
    }

    public ConfigurationSection getRawData() {
        return rawData;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return file.getName().replaceFirst(".yml","");
    }


    public GameBase getGameBase() {
        return gameBase;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    private void updateRawData() {
        YamlConfiguration configuration = new YamlConfiguration();
        try{
            configuration.load(file);
        }catch (IOException | InvalidConfigurationException e){
            return;
        }
        rawData =configuration.getConfigurationSection("data");

    }

    @SuppressWarnings("unchecked")
    public Map newMap(GameInstance gameInstance) {

        Map map = null;
        try {
            map = new Map(this,gameInstance);


        } catch (Map.UnableToCloneException e) {

            GameLogger.log(new GameLog(gameBase,LogLevel.ERROR,"Unable to clone world of map: "+codeName,true));
        }
        if(map != null){
            maps.add(map);

            if(rawData.getList("injections") != null) {
                for (String injectionClassPath : rawData.getStringList("injections")) {
                    Class<? extends MapInjection> injectionClass = null;
                    try {
                        injectionClass = (Class<? extends MapInjection>) Class.forName(injectionClassPath);
                    } catch (ClassNotFoundException ignored) {
                        GameLogger.log(new GameLog(gameBase, LogLevel.ERROR, "Tried to inject custom injection into map with name: " + codeName + " but failed because class path does not exist!", true));
                        continue;
                    }

                    try {
                        Constructor<? extends MapInjection> injectionClassCons = injectionClass.getConstructor(Map.class);

                        map.customInjections.add(injectionClassCons.newInstance(map));
                    } catch (InstantiationException | IllegalAccessException e) {
                        GameLogger.log(new GameLog(gameBase, LogLevel.ERROR, "Tried to inject custom injection into map with name: " + codeName + " but failed while creating the instance!", true));

                    } catch (InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return map;
    }



    public ArrayList<Map> getMaps() {
        return maps;
    }

    public int getPlayerLimit() {
        return playerLimit;
    }

    public void onTick(Map map){
        for(MapInjection injection : map.customInjections){
            injection.tick();
        }
    }

    public ConfigurationSection getOptionsSection() {
        return rawData.getConfigurationSection("options") == null ? rawData.createSection("options") : rawData.getConfigurationSection("options");
    }

    public void updateOptions(ConfigurationSection temporaryConfig) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            rawData.set("options",temporaryConfig);
            configuration.set("data",rawData);
            configuration.save(file);
            updateRawData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Map {

        private final GameMap base;
        private final GameInstance gameInstance;
        private final ArrayList<MapInjection> customInjections;
        private final World bukkitWorld;
        private final ArrayList<GameOption> options;

        public Map(GameMap map, GameInstance gameInstance) throws UnableToCloneException {

            this.base = map;
            this.customInjections = new ArrayList<>();
            this.gameInstance = gameInstance;
            SlimeWorld slimeWorld = GameManager.SLIME_WORLD_MAPS.get(map);
            SlimeWorld slimeWorldClone = slimeWorld.clone("game-"+map.getGameBase().getCodeName()+"-"+map.getInstanceCode()+"-"+map.getCodeName().toLowerCase() + "-" + gameInstance.getGameID());
            GameAPI.getSlime().generateWorld(slimeWorldClone);

            this.bukkitWorld = Bukkit.getWorld("game-"+map.getGameBase().getCodeName()+"-"+map.getInstanceCode()+"-"+map.getCodeName().toLowerCase() +"-"+gameInstance.getGameID());

            bukkitWorld.setAutoSave(false);
            bukkitWorld.setGameRuleValue("doDaylightCycle", "false");
            bukkitWorld.setGameRuleValue("doWeatherCycle", "false");
            bukkitWorld.setDifficulty(Difficulty.PEACEFUL);
            bukkitWorld.setTime(1000);
            bukkitWorld.setGameRuleValue("announceAdvancements","false");
            bukkitWorld.setThundering(false);
            bukkitWorld.setStorm(false);
            bukkitWorld.setWeatherDuration(-1);
            
            this.options = new ArrayList<GameOption>();

        }

        public ArrayList<GameOption> getOptions() {
            return options;
        }

        public GameOption getOption(String codeName){
            for(GameOption option: options){
                if(option.getKey().equalsIgnoreCase(codeName)) return option;
            }
            return null;
        }

        public World getBukkitWorld() {
            return bukkitWorld;
        }

        public GameOption loadOption(String key,OptionType type,boolean required){
            this.base.updateRawData();
            GameOption option = null;
            if(this.base.rawData.getConfigurationSection("options") != null){
                ConfigurationSection options = this.base.rawData.getConfigurationSection("options");

                ConfigurationSection optionConfig = options.getConfigurationSection(key);

                if(optionConfig == null) {
                    return null;
                }
                OptionType optionType;

                try {
                    optionType =  OptionType.valueOf(optionConfig.getString("type").toUpperCase());

                }catch (IllegalArgumentException e){
                    return null;
                }

                if(!optionType.equals(type)){
                    return null;
                }

                if(optionType.equals(OptionType.ARRAY) && optionConfig.getConfigurationSection("value") == null){
                    return null;
                }

                option = new GameOption(key,optionType,optionConfig.get("value"),this);

                this.options.add(option);
            }

            if(option == null && required){
                gameInstance.end("option: "+key+", of type: "+type.name()+" is missing on map: "+gameInstance.getGameMap().getCodeName());
            }

            return option;
        }

        /*
                    bukkitWorld.setAutoSave(false);
            bukkitWorld.setGameRuleValue("doDaylightCycle", "false");
            bukkitWorld.setGameRuleValue("doWeatherCycle", "false");
            bukkitWorld.setDifficulty(Difficulty.PEACEFUL);
            bukkitWorld.setTime(1000);
            bukkitWorld.setGameRuleValue("announceAdvancements","false");
            bukkitWorld.setThundering(false);
            bukkitWorld.setStorm(false);
            bukkitWorld.setWeatherDuration(-1);
         */

        public GameMap getBase() {
            return base;
        }

        public GameInstance getGameInstance() {
            return gameInstance;
        }

        public ArrayList<MapInjection> getCustomInjections() {
            return customInjections;
        }


        public static class UnableToCloneException extends Exception {

            public UnableToCloneException(IOException msg){
                super(msg);
            }

        }
    }
}
