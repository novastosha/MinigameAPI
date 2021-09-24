/**
 *
 * Copyright (c) 2021 novastosha
 *
 */
package dev.nova.gameapi;

import dev.nova.gameapi.game.base.GameBase;
import dev.nova.gameapi.game.base.instance.GameInstance;
import dev.nova.gameapi.game.base.instance.eventListener.GlobalListener;
import dev.nova.gameapi.game.base.tasks.GameTicker;
import dev.nova.gameapi.game.base.tasks.ScoreboardTicker;
import dev.nova.gameapi.game.manager.GameManager;
import dev.nova.gameapi.game.map.command.MapEditCommand;
import dev.nova.gameapi.game.map.options.command.OptionCommands;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.game.queue.command.QueueCommand;
import dev.nova.gameapi.utils.Files;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class GAPIPlugin extends JavaPlugin {

    private GlobalListener listener;

    @Override
    public void onEnable() {



        getCommand("queue").setExecutor(new QueueCommand());
        OptionCommands optionCommands = new OptionCommands();
        getCommand("setOption").setExecutor(optionCommands);
        getCommand("map").setExecutor(new MapEditCommand());

        for(Files files : Files.values()) {
            if (files.isToCreate()) {
                if(!files.isFile()) {
                    if (!files.getFile().exists()) files.getFile().mkdirs();
                }else{
                    if(!files.getFile().exists()) {
                        try {
                            files.getFile().createNewFile();
                        } catch (IOException ignored) {

                        }
                    }
                }
            }
        }

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, GameTicker.instance(), 0L, 0L);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, ScoreboardTicker.instance(), 0L, 0L);

        GameManager.loadGames();

        this.listener = new GlobalListener(this,true){

            @Override
            protected void onEvent(Event event) {
                if(event instanceof ChunkLoadEvent || event instanceof ChunkUnloadEvent || event instanceof EntityAirChangeEvent){
                    return;
                }


                for(GameBase base : GameManager.GAMES){
                    for(ArrayList<GameInstance> instances : base.getRunningInstances().values()) {

                        for (GameInstance instance : instances) {
                            if (event instanceof PlayerDeathEvent) {
                                PlayerDeathEvent deathEvent = (PlayerDeathEvent) event;

                                GamePlayer victim = GamePlayer.getPlayer(deathEvent.getEntity());
                                GamePlayer killer = GamePlayer.getPlayer(deathEvent.getEntity().getKiller());

                                if(killer == null) {
                                    if (instance.getPlayers().contains(victim)) {
                                        instance.onEvent(event);
                                    }
                                }else{
                                    if(instance.getPlayers().contains(victim) && instance.getPlayers().contains(killer)){
                                        instance.onEvent(event);
                                    }
                                }
                            }

                            try {
                                World world = (World) event.getClass().getMethod("getWorld").invoke(event);

                                if (world != null) {
                                    if (instance.getMap() != null && world == instance.getMap().getBukkitWorld()) {
                                        instance.onEvent(event);
                                        continue;
                                    }
                                }
                                Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);

                                if (player != null) {
                                    GamePlayer gamePlayer = GamePlayer.getPlayer(player);

                                    if (instance.getPlayers().contains(gamePlayer)) {
                                        instance.onEvent(event);
                                        continue;
                                    }
                                }

                                Entity entity = (Entity) event.getClass().getMethod("getEntity").invoke(event);

                                if (entity != null) {
                                    if(!(entity instanceof Player)) {
                                        if (instance.getMap() != null && instance.getMap().getBukkitWorld().getEntities().contains(entity)) {
                                            instance.onEvent(event);
                                            continue;
                                        }
                                    }else{
                                        GamePlayer player1 = GamePlayer.getPlayer((Player) entity);
                                        if(instance.getPlayers().contains(player1)){
                                            instance.onEvent(event);
                                        }
                                    }
                                }
                            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
                                instance.onEvent(event);
                            }
                        }
                    }
                }
            }
        };
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

    @Override
    public void onDisable() {
        Bukkit.getServer().getScheduler().cancelTasks(this);
        for(GameBase base : GameManager.GAMES){
            for(ArrayList<GameInstance> instances : base.getRunningInstances().values()){
                for(GameInstance instance : instances){
                    if(instance.getMap() != null){
                        if(instance.getMap().getBukkitWorld().getPlayers().size() != 0){
                            for(Player player : instance.getMap().getBukkitWorld().getPlayers()){
                                player.kickPlayer("§cGameAPI shutting down");
                                //Move to fallback server...
                            }
                            Bukkit.unloadWorld(instance.getMap().getBukkitWorld(),false);
                            deleteWorld(instance.getMap().getBukkitWorld().getWorldFolder());
                        }
                    }
                }
            }
        }
    }
}
