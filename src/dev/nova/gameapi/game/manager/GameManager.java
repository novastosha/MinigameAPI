package dev.nova.gameapi.game.manager;

import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import dev.nova.gameapi.GameAPI;
import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.lobby.LobbyBase;
import dev.nova.gameapi.game.logger.GameLog;
import dev.nova.gameapi.game.logger.GameLogger;
import dev.nova.gameapi.game.logger.LogLevel;
import dev.nova.gameapi.game.map.GameMap;
import dev.nova.gameapi.game.map.Map;
import dev.nova.gameapi.game.map.options.instance.GameOptionInfo;
import dev.nova.gameapi.game.map.swm.SlimeFileLoader;
import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class GameManager {

    public static final ArrayList<GameBase> GAMES = new ArrayList<GameBase>();
    public static final HashMap<GameMap, SlimeWorld> SLIME_WORLD_MAPS = new HashMap<>();
    public static final SlimePropertyMap GENERIC_PROPERTY_MAP;

    static {

        GENERIC_PROPERTY_MAP = new SlimePropertyMap();
        GENERIC_PROPERTY_MAP.setString(SlimeProperties.DIFFICULTY, "easy");
        GENERIC_PROPERTY_MAP.setBoolean(SlimeProperties.ALLOW_ANIMALS, false);
        GENERIC_PROPERTY_MAP.setBoolean(SlimeProperties.ALLOW_MONSTERS, false);
        GENERIC_PROPERTY_MAP.setBoolean(SlimeProperties.PVP, true);

        Files.MAPS_FOLDER.getFile().mkdirs();
        SLIME_FILE_LOADER = new SlimeFileLoader(Files.MAPS_FOLDER.getFile());
    }

    public static final SlimeFileLoader SLIME_FILE_LOADER;

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

                Files.getGameFolder(gameBase.getCodeName()).mkdirs();
                if (gameBase.onInitialize()) {
                    GAMES.add(gameBase);

                    GameLogger.log(new GameLog(gameBase, LogLevel.INFO, "Loading maps...", false));
                    Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Pre-Loading maps for game: " + gameBase.getCodeName());

                    for (String mode : gameBase.getInstances().keySet()) {

                        File gameMaps = Files.getGameMapsFolder(gameBase.getCodeName(), mode);

                        if (!gameMaps.exists()) {
                            gameMaps.mkdirs();
                        }

                        for (File mapYml : Objects.requireNonNull(gameMaps.listFiles())) {
                            if (mapYml.isFile() && !mapYml.getName().startsWith("-") && mapYml.getName().endsWith(".yml")) {

                                String fileName = mapYml.getName().replaceFirst(".yml", "");

                                File MC_world = new File(Bukkit.getWorldContainer(), fileName);
                                if (MC_world.exists()) {
                                    if (!SLIME_FILE_LOADER.worldExists(fileName)) {
                                        Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Importing: " + fileName + " into Slime Region Format World (SRF).");
                                        Bukkit.getServer().getScheduler().runTaskAsynchronously(GameAPI.getInstance(), new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    GameAPI.getSlime().importWorld(MC_world, fileName, SLIME_FILE_LOADER);
                                                } catch (WorldAlreadyExistsException | InvalidWorldException | WorldTooBigException | IOException | WorldLoadedException e) {
                                                    e.printStackTrace();
                                                }
                                                Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] " + fileName + " was successfully imported into a Slime Region Format World (SRF).");
                                                MC_world.delete();

                                            }
                                        });

                                    }
                                } else {
                                    if (!SLIME_FILE_LOADER.worldExists(fileName)) {
                                        Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Unable to load map named: " + mapYml.getName() + " because the file name does not match the name of any world (either in slime type or normal)!");
                                        continue;
                                    }
                                }

                                YamlConfiguration configuration = new YamlConfiguration();

                                try {
                                    configuration.load(mapYml);
                                } catch (IOException | InvalidConfigurationException e) {
                                    GameLogger.log(new GameLog(gameBase, LogLevel.INFO, "Unable to load map with file name: " + mapYml.getName(), false));
                                    e.printStackTrace();
                                    continue;
                                }

                                ConfigurationSection data = configuration.getConfigurationSection("data");

                                if (data == null) {
                                    continue;
                                }

                                if (data.contains("display-name")
                                        && data.contains("code-name") && data.contains("player-limit")) {

                                    final GameMap map = new GameMap(gameBase, mapYml, data, mode);

                                    Bukkit.getServer().getScheduler().runTaskAsynchronously(GameAPI.getInstance(), new Runnable() {
                                        @Override
                                        public void run() {
                                            GameLogger.log(new GameLog(gameBase, LogLevel.INFO, "Loaded map: " + data.getString("code-name"), false));
                                            try {
                                                SlimeWorld world = GameAPI.getSlime().loadWorld(SLIME_FILE_LOADER, fileName, false, GENERIC_PROPERTY_MAP);

                                                SLIME_WORLD_MAPS.put(map, world);

                                                boolean editingMode = false;

                                                GameOptionInfo[] infos = gameBase.getOptionInformation(mode);
                                                ConfigurationSection section = data.getConfigurationSection("options");
                                                ArrayList<GameOptionInfo> missingInformation = new ArrayList<>();

                                                if (infos.length != 0) {

                                                    if (section == null) {
                                                        createMissingOptionsLog(gameBase, mode, infos, null);
                                                        editingMode = true;
                                                    } else {

                                                        for (GameOptionInfo optionInfo : infos) {
                                                            GameOptionInfo.ValidationState valid = optionInfo.isValid(section, false);

                                                            if (!valid.equals(GameOptionInfo.ValidationState.VALID)) {
                                                                missingInformation.add(optionInfo);
                                                                editingMode = true;
                                                            }
                                                        }
                                                    }
                                                }

                                                if (!missingInformation.isEmpty()) {
                                                    createMissingOptionsLog(gameBase, mode, missingInformation.toArray(new GameOptionInfo[0]), section);
                                                }

                                                if (!editingMode) {
                                                    gameBase.addMap(map, mode);
                                                } else {
                                                    gameBase.addEditingMap(map, mode);
                                                }
                                            } catch (UnknownWorldException | CorruptedWorldException | NewerFormatException | WorldInUseException | IOException e) {
                                                e.printStackTrace();
                                            }
                                            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Map: " + data.getString("code-name") + " has been loaded successfully");
                                        }
                                    });


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

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void createMissingOptionsLog(GameBase gameBase, String mode, GameOptionInfo[] infos, ConfigurationSection section) {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(mode).append("]").append(" Missing options (").append(infos.length).append(" in-total) (Switching map to editing mode):\n");
        for (GameOptionInfo info : infos) {
            builder.append(info.getMissingDisplayName(0, section, false)).append("\n");
        }

        GameLogger.log(new GameLog(gameBase, LogLevel.ERROR, builder.toString(), true));
    }

    public static GameBase getGame(String name) {
        for (GameBase base : GAMES) {
            if (base.getCodeName().equals(name)) return base;
        }
        return null;
    }

    public static void reloadMap(SlimeWorld clone, Map map) {

        Bukkit.getServer().getScheduler().runTaskAsynchronously(GameAPI.getInstance(), new Runnable() {
            @Override
            public void run() {
                GameLogger.log(new GameLog(map.getGameBase(), LogLevel.INFO, "Reloading map: " + map.getCodeName(), true));

                if (clone != null) {
                    SlimeWorld world = null;
                    try {
                        world = GameAPI.getSlime().loadWorld(map instanceof GameMap ? SLIME_FILE_LOADER : map.getGameBase().getLobbyManager().SLIME_FILE_LOADER, map instanceof GameMap ? map.getCodeName() : map.getGameBase().getCodeName()+"-"+map.getCodeName(), false, GENERIC_PROPERTY_MAP);
                    } catch (UnknownWorldException | CorruptedWorldException | IOException | NewerFormatException | WorldInUseException e) {
                        e.printStackTrace();
                    }
                    if (map instanceof GameMap gameMap) {
                        SLIME_WORLD_MAPS.put(gameMap, world);
                    } else {
                        map.getGameBase().getLobbyManager().SLIME_WORLD_MAPS.put(((LobbyBase) map),world);
                    }
                }

                GameOptionInfo[] infos = map instanceof GameMap ? map.getGameBase().getOptionInformation(((GameMap) map).getInstanceCode()) : map.getGameBase().getLobbyManager().getLobbyOptionInformation(((LobbyBase) map));
                ArrayList<GameOptionInfo> missingInformation = new ArrayList<>();

                boolean editingMode = false;

                if (infos.length != 0) {

                    if (map.getOptionsSection() == null) {
                        createMissingOptionsLog(map.getGameBase(), map.getCodeName(), infos, null);
                        editingMode = true;
                    } else {

                        for (GameOptionInfo optionInfo : infos) {
                            GameOptionInfo.ValidationState valid = optionInfo.isValid(map.getOptionsSection(), false);

                            if (!valid.equals(GameOptionInfo.ValidationState.VALID)) {
                                missingInformation.add(optionInfo);
                                editingMode = true;
                            }
                        }
                    }
                }

                if (map instanceof GameMap gameMap) {

                    if (!missingInformation.isEmpty()) {
                        createMissingOptionsLog(map.getGameBase(), gameMap.getInstanceCode(), missingInformation.toArray(new GameOptionInfo[0]), map.getOptionsSection());
                    }

                    if (!editingMode) {
                        if (!map.getGameBase().getGameMaps().get(gameMap.getInstanceCode()).contains(map)) {
                            GameLogger.log(new GameLog(map.getGameBase(), LogLevel.INFO, "Loaded map: " + map.getCodeName(), true));
                            map.getGameBase().addMap(gameMap, gameMap.getInstanceCode());
                            map.getGameBase().getEditingGameMaps().get(gameMap.getInstanceCode()).remove(map);
                        }
                    } else {
                        if (!map.getGameBase().containsEditingMap(gameMap.getInstanceCode(), map.getCodeName())) {
                            GameLogger.log(new GameLog(map.getGameBase(), LogLevel.INFO, "Switched map: " + map.getCodeName() + " to editing mode.", true));
                            map.getGameBase().addEditingMap(map, gameMap.getInstanceCode());
                            map.getGameBase().getGameMaps().get(gameMap.getInstanceCode()).remove(map);
                        }
                    }
                } else {
                    if (!missingInformation.isEmpty()) {
                        createMissingOptionsLog(map.getGameBase(), map.getCodeName(), missingInformation.toArray(new GameOptionInfo[0]), map.getOptionsSection());
                    }

                    if (!editingMode) {
                        if (!map.getGameBase().getLobbyManager().editingLobbies.contains(map)) {
                            GameLogger.log(new GameLog(map.getGameBase(), LogLevel.INFO, "Loaded map: " + map.getCodeName(), true));
                            map.getGameBase().getLobbyManager().addLobby(((LobbyBase) map));
                            map.getGameBase().getLobbyManager().editingLobbies.remove(map);
                        }
                    } else {
                        if (!map.getGameBase().getLobbyManager().editingLobbies.contains(map)) {
                            GameLogger.log(new GameLog(map.getGameBase(), LogLevel.INFO, "Switched map: " + map.getCodeName() + " to editing mode.", true));
                            map.getGameBase().getLobbyManager().editingLobbies.add((LobbyBase) map);
                            map.getGameBase().getLobbyManager().shutDownBase(((LobbyBase) map),"Lobby is not ready!");
                            map.getGameBase().getLobbyManager().removeLobby(((LobbyBase) map));
                        }
                    }
                }
            }
        });

    }
}
