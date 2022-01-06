package dev.nova.gameapi.game.manager;

import dev.nova.gameapi.GAPIPlugin;
import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class GameManager {

    public static final ArrayList<GameBase> GAMES = new ArrayList<GameBase>();

    public static void loadGames() {
        for (File file : Objects.requireNonNull(Files.PLUGINS.getFile().listFiles())) {
            if (file.getName().endsWith(".jar") && file.isFile()) {
                loadGame(file);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadGame(File file) {
        try {
            JarFile jar = new JarFile(file);

            JarEntry pluginEntry = jar.getJarEntry("plugin.yml");
            JarEntry gameEntry = jar.getJarEntry("gameAPI.yml");

            if (pluginEntry == null || gameEntry == null) return;

            YamlConfiguration gameyml = new YamlConfiguration();


            try {
                gameyml.load(new InputStreamReader(jar.getInputStream(gameEntry)));
            } catch (IOException | InvalidConfigurationException ignored) {
                ignored.printStackTrace();
                return;
            }

            String classPath = gameyml.getString("game-base-class");

            Class<? extends GameBase> gameClass = null;
            try {
                gameClass = (Class<? extends GameBase>) Class.forName(classPath);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }

            Constructor<? extends GameBase> constructor = null;
            try {
                constructor = gameClass.getConstructor();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return;
            }

            try {

                GameBase gameBase = constructor.newInstance();

                if (GAPIPlugin.isCoinsSystemHooked()) {

                }

                Files.getGameFolder(gameBase.getCodeName()).mkdirs();
                if (gameBase.onInitialize()) {
                    GAMES.add(gameBase);

                    GameLogger.log(new GameLog(gameBase, LogLevel.INFO, "Loading maps...", false));
                    Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Loading maps for game: " + gameBase.getCodeName());


                    for (String mode : gameBase.getInstances().keySet()) {

                        File gameMaps = Files.getGameMapsFolder(gameBase.getCodeName(),mode);

                        if (!gameMaps.exists()) {
                            gameMaps.mkdirs();
                        }

                        for (File mapYml : Objects.requireNonNull(gameMaps.listFiles())) {
                            if (mapYml.isFile() && !mapYml.getName().startsWith("-") && mapYml.getName().endsWith(".yml")) {

                                String fileName = mapYml.getName().replaceFirst(".yml", "");

                                File world = new File(Bukkit.getWorldContainer(), fileName);
                                if (world.exists()) {
                                    GameMap.loadWorld(fileName);
                                } else {
                                    Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Unable to load map named: " + mapYml.getName() + " because the file name does not match the name of any world!");
                                    continue;
                                }

                                YamlConfiguration configuration = new YamlConfiguration();

                                try {
                                    configuration.load(mapYml);
                                } catch (IOException | InvalidConfigurationException e) {
                                    GameLogger.log(new GameLog(gameBase, LogLevel.INFO, "Unable to load map with file name: " + mapYml.getName(), false));
                                    continue;
                                }

                                ConfigurationSection data = configuration.getConfigurationSection("data");

                                if (data == null) {
                                    continue;
                                }

                                if (data.contains("world") && data.contains("display-name")
                                        && data.contains("code-name") && data.contains("player-limit") && data.contains("world")) {
                                    gameBase.addMap(new GameMap(gameBase, mapYml, data),mode);
                                    GameLogger.log(new GameLog(gameBase, LogLevel.INFO, "Loaded map: " + data.getString("code-name"), false));
                                    Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Map: " + data.getString("code-name") + " has been loaded successfully");
                                } else {
                                    GameLogger.log(new GameLog(gameBase, LogLevel.INFO, "Unable to load map with file name: " + mapYml.getName() + " because it does not contain necessary data.", false));
                                    continue;
                                }
                            }
                        }
                    }
                } else {
                    GameLogger.log(new GameLog(gameBase, LogLevel.ERROR, "Game did not initialize properly!", false));
                    Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Game: §b" + gameBase.getCodeName() + "§7 did not initialize properly!");
                    return;
                }

                GameLogger.log(new GameLog(gameBase, LogLevel.INFO, "Game initialized successfully.", false));
                Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Game: " + gameBase.getCodeName() + " has initialized successfully!");
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                return;
            }

        } catch (Exception ignored) {
        }
    }

    public static GameBase getGame(String name) {
        for (GameBase base : GAMES) {
            if (base.getCodeName().equals(name)) return base;
        }
        return null;
    }
}
