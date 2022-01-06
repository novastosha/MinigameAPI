/**
 * Copyright (c) 2021 novastosha
 */
package dev.nova.gameapi;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.commands.LeaveCommand;
import dev.nova.gameapi.game.base.instance.commands.ReviveCommand;
import dev.nova.gameapi.game.base.instance.commands.SpectateCommand;
import dev.nova.gameapi.game.base.instance.commands.SpectatorCommand;
import dev.nova.gameapi.game.base.lobby.Lobby;
import dev.nova.gameapi.game.base.mysql.DatabaseConnection;
import dev.nova.gameapi.game.base.tasks.GameEventListener;
import dev.nova.gameapi.game.base.tasks.GameTicker;
import dev.nova.gameapi.game.base.tasks.LobbyScoreboardTicker;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.queue.command.ForceStartCommand;
import dev.nova.gameapi.game.queue.command.QueueCommand;
import dev.nova.gameapi.party.command.PartyCommand;
import dev.nova.gameapi.game.base.listener.ChatListener;
import dev.nova.gameapi.utils.Files;
import dev.nova.gameapi.utils.api.schematics.commands.SchematicsSaveCommand;
import dev.nova.gameapi.utils.api.schematics.commands.SchematicsWandCommand;
import dev.nova.gameapi.utils.api.schematics.commands.listener.SchematicsInteractEventListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;

public class GAPIPlugin extends JavaPlugin {

    private static Connection coinsConnection;

    public static Connection getCoinsConnection() {
        return coinsConnection;
    }

    public static boolean isCoinsSystemHooked() {
        return coinsConnection != null;
    }

    @Override
    public void onEnable() {

        new GameEventListener(this, true);
        Bukkit.getServer().getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new SchematicsInteractEventListener(), this);

        QueueCommand queueCommand = new QueueCommand();
        getCommand("queue").setExecutor(queueCommand);
        getCommand("queue").setTabCompleter(queueCommand);

        getCommand("leave").setExecutor(new LeaveCommand());

        getCommand("revive").setExecutor(new ReviveCommand());
        getCommand("spectator").setExecutor(new SpectatorCommand());

        SpectateCommand spectateCommand = new SpectateCommand();
        getCommand("spectate").setExecutor(spectateCommand);
        getCommand("spectate").setTabCompleter(spectateCommand);

        PartyCommand partyCommand = new PartyCommand();
        getCommand("party").setExecutor(partyCommand);

        getCommand("wand").setExecutor(new SchematicsWandCommand());
        getCommand("saveSchematic").setExecutor(new SchematicsSaveCommand());

        getCommand("forcestart").setExecutor(new ForceStartCommand());

        for (Files files : Files.values()) {
            if (files.isToCreate()) {
                if (!files.getFile().exists()) {
                    if (!files.isFile()) {
                        files.getFile().mkdirs();
                    } else {
                        try {
                            files.getFile().createNewFile();
                        } catch (IOException ignored) {

                        }

                    }
                    files.onCreate();
                }
            }
        }


        if (getConfig().getBoolean("coins-support.enabled")) {
            Bukkit.getServer().getConsoleSender().sendMessage("§7[§bGameAPI§7] Hooking coins system...");

            String host = getConfig().getString("coins-support.host"), username = getConfig().getString("coins-support.username"),
                    password = getConfig().getString("coins-support.password");

            int port = getConfig().getInt("coins-support.port");

            this.coinsConnection = new DatabaseConnection(host, port, username, password, "coins", "coins").connection;
        }

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, GameTicker.instance(), 0L, 0L);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, LobbyScoreboardTicker.instance(), 0L, 0L);

        GameManager.loadGames();
    }

    private boolean deleteWorld(File path) {
        if (path.exists()) {
            File files[] = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteWorld(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    @Override
    public void onDisable() {
        Bukkit.getServer().getScheduler().cancelTasks(this);
        for (GameBase base : GameManager.GAMES) {
            if (base.hasLobbySupport()) {
                for (Lobby lobby : base.getLobbyInstances()) {
                    lobby.delete();
                }
            }
            for (ArrayList<GameInstance> instances : base.getRunningInstances().values()) {
                for (GameInstance instance : instances) {
                    Bukkit.getServer().getScheduler().cancelTask(instance.eventsTaskID);
                    if (instance.getMap() != null) {
                        if (instance.getMap().getBukkitWorld().getPlayers().size() != 0) {
                            for (Player player : instance.getMap().getBukkitWorld().getPlayers()) {
                                player.kickPlayer("§cGameAPI shutting down");
                                //Move to fallback server...
                            }
                            Bukkit.unloadWorld(instance.getMap().getBukkitWorld(), false);
                            deleteWorld(instance.getMap().getBukkitWorld().getWorldFolder());
                        }
                    }
                }
            }
        }
    }
}
