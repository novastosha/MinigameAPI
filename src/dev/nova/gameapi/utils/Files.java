package dev.nova.gameapi.utils;

import dev.nova.gameapi.GameAPI;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public enum Files {
    LOG_FOLDER(new File(GameAPI.getPlugin(GameAPI.class).getDataFolder(),"logs"),true,false){
        @Override
        public void onCreate() {

        }
    },
    PLAYERS(new File(GameAPI.getPlugin(GameAPI.class).getDataFolder(),"players"),true,false){
        @Override
        public void onCreate() {

        }
    },
    GAMES_FOLDER(new File(GameAPI.getPlugin(GameAPI.class).getDataFolder(),"games"),true,false){
        @Override
        public void onCreate() {

        }
    },
    GAME_FOLDER(new File(GAMES_FOLDER.getFile(),"NAME"),false,false){
        @Override
        public void onCreate() {

        }
    },
    LOBBIES_FOLDER(new File(GAME_FOLDER.getFile(),"lobbies"),false,false){
        @Override
        public void onCreate() {

        }
    },
    LOBBIES_MAPS_FOLDER(new File(LOBBIES_FOLDER.getFile(),"maps"),false,false){
        @Override
        public void onCreate() {

        }
    },
    GAME_MAPS_FOLDER(new File(new File(GAME_FOLDER.getFile(),"maps"),"MODE"),false,false){
        @Override
        public void onCreate() {

        }
    },
    GAME_SCHEMATICS_FOLDER(new File(new File(GAME_FOLDER.getFile(),"schematics"),"MODE"),false,false){
        @Override
        public void onCreate() {

        }
    },
    MAPS_FOLDER(new File(GameAPI.getInstance().getDataFolder(), "maps"),false,false){
        @Override
        public void onCreate() {

        }
    },
    CONFIG(new File(GameAPI.getPlugin(GameAPI.class).getDataFolder(),"config.yml"),true,true){
        @Override
        public void onCreate() {
            try {
                FileWriter fileWriter = new FileWriter(getFile());

                BufferedReader reader = new BufferedReader(new InputStreamReader(GameAPI.getPlugin(GameAPI.class).getResource("config.yml")));

                reader.lines().forEach(s -> {
                    try {
                        fileWriter.write(s+"\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                reader.close();
                fileWriter.flush();
                fileWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    },
    PLUGINS(new File("./plugins"),false,false){
        @Override
        public void onCreate() {

        }
    };

    private final File file;
    private final boolean isFile;
    private final boolean create;

    Files(File file,boolean create, boolean isFile) {
        this.file = file;
        this.create = create;
        this.isFile = isFile;
    }

    public abstract void onCreate();

    public boolean isFile() {
        return isFile;
    }

    public boolean isToCreate() {
        return create;
    }

    public File getFile() {
        return file;
    }

    public static File getLobbiesFolder(String gameName) {
        return new File(LOBBIES_FOLDER.getFile().getPath().replaceAll("NAME",gameName));
    }

    public static File getLobbiesMapsFolder(String gameName) {
        return new File(LOBBIES_MAPS_FOLDER.getFile().getPath().replaceAll("NAME",gameName));
    }

    public static File getGameFolder(String gameName){
        return new File(GAME_FOLDER.getFile().getPath().replaceAll("NAME",gameName));
    }

    public static File getGameDatabasesFile(String gameName,boolean create){
        File f = new File(GAME_FOLDER.getFile().getPath().replaceAll("NAME",gameName));
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = new File(f,"database.yml");

        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    public static File getGameMapsFolder(String gameName,String modeName){
        return new File(GAME_MAPS_FOLDER.getFile().getPath().replaceAll("NAME",gameName).replaceAll("MODE",modeName));
    }

    public static File getGameSchematicsFolder(String gameName,String modeName){
        return new File(GAME_SCHEMATICS_FOLDER.getFile().getPath().replaceAll("NAME",gameName).replaceAll("MODE",modeName));
    }
}
