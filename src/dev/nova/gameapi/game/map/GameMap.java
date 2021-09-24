package dev.nova.gameapi.game.map;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
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
import java.util.Arrays;
import java.util.List;

public class GameMap {

    private final World world;
    private final GameBase gameBase;
    private final String codeName;
    private final String displayName;
    private final ArrayList<Map> maps;
    private final int playerLimit;
    private ConfigurationSection rawData;
    private final File file;

    public GameMap(GameBase gameBase,File configFile,ConfigurationSection data){
        this.file = configFile;
        this.world = Bukkit.getWorld(data.getString("world"));
        if(world == null){
            loadWorld(data.getString("world"));
        }

        this.displayName = data.getString("display-name");
        this.maps = new ArrayList<>();
        this.codeName = data.getString("code-name");
        this.rawData = data;
        this.playerLimit = data.getInt("player-limit");

        this.gameBase = gameBase;
    }

    public static void loadWorld(String name) {
        World world = new WorldCreator(name).createWorld();

        world.setAutoSave(false);
    }



    public File getFile() {
        return file;
    }

    public String getFileName() {
        return file.getName().replaceFirst(".yml","");
    }

    public World getWorld() {
        return world;
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
                for (String injectionClassPath : (List<String>) rawData.getList("injections")) {
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
            this.bukkitWorld = cloneWorld();
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

                option = new GameOption(key,optionType,optionConfig.get("value"),this);
                this.options.add(option);
            }
            if(option == null && required){
                Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Missing option: §b"+key);
                gameInstance.getUtils().sendMessage("§cSorry, an unexpected error happened!");
                gameInstance.postEnd();
            }

            return option;
        }

        private World cloneWorld() throws UnableToCloneException{
            GameLogger.log(new GameLog(base.gameBase, LogLevel.INFO,"Starting clone of map: "+base.codeName+" for instance with id: "+gameInstance.getGameID(),true));
            File copiedFile = new File(Bukkit.getWorldContainer(), base.gameBase.getCodeName()+"-"+gameInstance.getGameID()+"-"+base.getCodeName());
            String worldName = base.gameBase.getCodeName()+"-"+gameInstance.getGameID()+"-"+base.getCodeName();
            World oldWorld = Bukkit.getServer().getWorld(worldName);

            if(oldWorld != null){
                Bukkit.getServer().unloadWorld(oldWorld,false);
                deleteWorld(oldWorld.getWorldFolder());
            }

            copiedFile.mkdir();
            copyFileStructure(base.world.getWorldFolder(), copiedFile);
            GameLogger.log(new GameLog(base.gameBase, LogLevel.INFO,"Clone completed successfully.",true));

            World newWorld = new WorldCreator(base.gameBase.getCodeName()+"-"+gameInstance.getGameID()+"-"+base.getCodeName()).createWorld();

            newWorld.setAutoSave(false);
            newWorld.setGameRuleValue("doDaylightCycle", "false");
            newWorld.setGameRuleValue("doWeatherCycle", "false");
            newWorld.setDifficulty(Difficulty.EASY);
            newWorld.setTime(1000);

            return newWorld;
        }

        private boolean deleteWorld(File path) {
            if(path.exists()) {
                File files[] = path.listFiles();
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteWorld(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
            return(path.delete());
        }

        private static void copyFileStructure(File source, File target) throws UnableToCloneException{
            try {
                ArrayList<String> ignore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));
                if (!ignore.contains(source.getName())) {
                    if (source.isDirectory()) {
                        if (!target.exists())
                            if (!target.mkdirs())
                                throw new IOException("Couldn't create world directory!");
                        String files[] = source.list();
                        for (String file : files) {
                            File srcFile = new File(source, file);
                            File destFile = new File(target, file);
                            copyFileStructure(srcFile, destFile);
                        }
                    } else {
                        InputStream in = new FileInputStream(source);
                        OutputStream out = new FileOutputStream(target);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0)
                            out.write(buffer, 0, length);
                        in.close();
                        out.close();
                    }
                }
            } catch (IOException e) {
                throw new UnableToCloneException(e);
            }
        }

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



    public static class BadMapExcpetion extends Exception{
        public BadMapExcpetion(GameInstance gameInstance){
        }
    }
}
