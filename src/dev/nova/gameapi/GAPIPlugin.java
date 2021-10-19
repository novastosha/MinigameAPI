/**
 * Copyright (c) 2021 novastosha
 */
package dev.nova.gameapi;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.commands.LeaveCommand;
import dev.nova.gameapi.game.base.instance.commands.SpectateCommand;
import dev.nova.gameapi.game.base.tasks.GameEventListener;
import dev.nova.gameapi.game.base.tasks.GameTicker;
import dev.nova.gameapi.game.base.tasks.ScoreboardTicker;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.command.MapEditCommand;
import dev.nova.gameapi.game.map.options.command.OptionCommands;
import dev.nova.gameapi.game.queue.command.QueueCommand;
import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class GAPIPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        new GameEventListener(this, true);


        QueueCommand queueCommand = new QueueCommand();
        getCommand("queue").setExecutor(queueCommand);
        getCommand("queue").setTabCompleter(queueCommand);

        OptionCommands optionCommands = new OptionCommands();
        getCommand("setOption").setExecutor(optionCommands);

        getCommand("map").setExecutor(new MapEditCommand());
        getCommand("leave").setExecutor(new LeaveCommand());

        SpectateCommand spectateCommand = new SpectateCommand();
        getCommand("spectate").setExecutor(spectateCommand);
        getCommand("spectate").setTabCompleter(spectateCommand);

        for (Files files : Files.values()) {
            if (files.isToCreate()) {
                if (!files.isFile()) {
                    if (!files.getFile().exists()) files.getFile().mkdirs();
                } else {
                    if (!files.getFile().exists()) {
                        try {
                            files.getFile().createNewFile();
                        } catch (IOException ignored) {

                        }
                    }
                }
            }
        }

        getConfig().options().copyDefaults(true);

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, GameTicker.instance(), 0L, 0L);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, ScoreboardTicker.instance(), 0L, 0L);

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
            for (ArrayList<GameInstance> instances : base.getRunningInstances().values()) {
                for (GameInstance instance : instances) {
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
