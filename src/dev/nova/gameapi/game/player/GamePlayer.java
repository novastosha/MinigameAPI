package dev.nova.gameapi.game.player;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.utils.Files;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class GamePlayer {


    private static final ArrayList<GamePlayer> PLAYERS = new ArrayList<>();

    private final YamlConfiguration yamlConfiguration;
    private final Player player;
    private final File configurationFile;
    private GameInstance game;

    public GamePlayer(Player player) throws UnableToLoadGamePlayerException {
        this.player = player;
        this.yamlConfiguration = new YamlConfiguration();

        this.configurationFile = new File(Files.PLAYERS.getFile(),player.getUniqueId().toString()+".yml");
            try{
                configurationFile.createNewFile();
                yamlConfiguration.load(configurationFile);
            }catch (IOException | InvalidConfigurationException e){
                throw new UnableToLoadGamePlayerException(e);
            }
    }

    public GameInstance getGame() {
        return game;
    }

    public void setGame(GameInstance game) {
        this.game = game;
    }

    public boolean isInGame(){
        return game != null;
    }

    public File getConfigurationFile() {
        return configurationFile;
    }

    public Player getPlayer() {
        return player;
    }

    public YamlConfiguration getConfiguration() {
        return yamlConfiguration;
    }

    public void saveConfiguration() throws UnableToSaveGamePlayerConfig {
        try {
            yamlConfiguration.save(configurationFile);
        } catch (IOException e) {
            throw new UnableToSaveGamePlayerConfig(e);
        }
    }

    public static class UnableToLoadGamePlayerException extends Exception {
        public UnableToLoadGamePlayerException(Exception e) {
            super(e);
        }
    }

    public static class UnableToSaveGamePlayerConfig extends Exception {
        public UnableToSaveGamePlayerConfig(Exception e){
            super(e);
        }
    }

    public static GamePlayer getPlayer(Player bukkitPlayer){

        for(GamePlayer player : PLAYERS){
            if(player.getPlayer().equals(bukkitPlayer)) return player;
        }
        return newPlayer(bukkitPlayer);
    }

    private static GamePlayer newPlayer(Player bukkitPlayer) {
        try {
            GamePlayer player = new GamePlayer(bukkitPlayer);
            PLAYERS.add(player);
            return player;
        } catch (UnableToLoadGamePlayerException e) {
            return null;
        }
    }

    public void setValue(GameBase gameBase,Class<? extends GameInstance> base, String path, Object value){
        yamlConfiguration.set(gameBase.getCodeName()+"."+gameBase.getCode(base)+"."+path,value);
        try {
            saveConfiguration();
        } catch (UnableToSaveGamePlayerConfig e) {
            e.printStackTrace();
        }
    }

    public Object getValue(GameBase gameBase,Class<? extends GameInstance> base, String path){
        return yamlConfiguration.get(gameBase.getCodeName()+"."+gameBase.getCode(base)+"."+path);
    }
}
