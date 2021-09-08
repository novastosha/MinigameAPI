package dev.nova.gameapi.utils;

import dev.nova.gameapi.GAPIPlugin;
import sun.net.ftp.FtpClient;

import java.io.File;

public enum Files {
    LOG_FOLDER(new File(GAPIPlugin.getPlugin(GAPIPlugin.class).getDataFolder(),"logs")),
    PLAYERS(new File(GAPIPlugin.getPlugin(GAPIPlugin.class).getDataFolder(),"players")),
    GAMES_FOLDER(new File(GAPIPlugin.getPlugin(GAPIPlugin.class).getDataFolder(),"games")),
    GAME_FOLDER(new File(GAMES_FOLDER.getFile(),"NAME")),
    GAME_MAPS_FOLDER(new File(GAME_FOLDER.getFile(),"maps")),
    CONFIG(new File(GAPIPlugin.getPlugin(GAPIPlugin.class).getDataFolder(),"config.yml")),
    PLUGINS(new File("./plugins"));

    private final File file;

    Files(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public static File getGameFolder(String gameName){
        return new File(GAME_FOLDER.getFile().getPath().replaceAll("NAME",gameName));
    }

    public static File getGameMapsFolder(String gameName){
        return new File(GAME_MAPS_FOLDER.getFile().getPath().replaceAll("NAME",gameName));
    }
}
