package dev.nova.gameapi;

import dev.nova.gameapi.game.base.tasks.GameTicker;
import dev.nova.gameapi.game.base.tasks.ScoreboardTicker;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.command.MapEditCommand;
import dev.nova.gameapi.game.map.options.command.OptionCommands;
import dev.nova.gameapi.game.queue.command.QueueCommand;
import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class GAPIPlugin extends JavaPlugin {

    @Override
    @SuppressWarnings("unchecked")
    public void onEnable() {

        getCommand("queue").setExecutor(new QueueCommand());
        OptionCommands optionCommands = new OptionCommands();
        getCommand("setOption").setExecutor(optionCommands);
        getCommand("map").setExecutor(new MapEditCommand());

        for(Files files : Files.values()){
            if(!files.getFile().exists()) files.getFile().mkdirs();
        }


        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, GameTicker.instance(), 0L, 0L);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, ScoreboardTicker.instance(), 0L, 0L);

        GameManager.loadGames();

    }

    @Override
    public void onDisable() {
        Bukkit.getServer().getScheduler().cancelTasks(this);
    }
}
