package dev.nova.gameapi.utils;

import dev.nova.gameapi.GAPIPlugin;
import sun.net.ftp.FtpClient;

import java.io.File;

public enum Files {
    LOG_FOLDER(new File(GAPIPlugin.getPlugin(GAPIPlugin.class).getDataFolder(),"logs"),true,false),
    PLAYERS(new File(GAPIPlugin.getPlugin(GAPIPlugin.class).getDataFolder(),"players"),true,false),
    GAMES_FOLDER(new File(GAPIPlugin.getPlugin(GAPIPlugin.class).getDataFolder(),"games"),true,false),
    GAME_FOLDER(new File(GAMES_FOLDER.getFile(),"NAME"),false,false),
    GAME_MAPS_FOLDER(new File(GAME_FOLDER.getFile(),"maps"),false,false),
    CONFIG(new File(GAPIPlugin.getPlugin(GAPIPlugin.class).getDataFolder(),"config.yml"),true,true),
    PLUGINS(new File("./plugins"),false,false);

    private final File file;
    private final boolean isFile;
    private final boolean create;

    Files(File file,boolean create, boolean isFile) {
        this.file = file;
        this.create = create;
        this.isFile = isFile;
    }

    public boolean isFile() {
        return isFile;
    }

    public boolean isToCreate() {
        return create;
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
